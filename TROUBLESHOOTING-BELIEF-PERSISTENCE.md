# Troubleshooting Belief Persistence Issues

## Problem Description

You're running the Quarkus application in production mode with PostgreSQL and seeing logs indicating beliefs are being extracted and processed, but they're not appearing in the database. You're also seeing the Hibernate warning `HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory`.

## Quick Diagnosis

### 1. Check Basic Health
```bash
# Check if the diagnostic endpoints are available
curl http://localhost:8080/api/v1/diagnostics/beliefs/health

# Generate comprehensive diagnostic report
curl http://localhost:8080/api/v1/diagnostics/beliefs/report
```

### 2. Verify Database Connection
```bash
# Check system health
curl http://localhost:8080/api/v1/system/health

# Verify PostgreSQL connection directly
psql -h localhost -U headkey -d headkey -c "SELECT COUNT(*) FROM beliefs;"
```

### 3. Test Isolated Persistence
```bash
# Test belief persistence in isolation
curl -X POST http://localhost:8080/api/v1/diagnostics/beliefs/test-persistence
```

## Common Root Causes

### 1. Transaction Rollback Issues

**Symptoms:**
- Beliefs appear to be created in logs
- No beliefs in database
- No error messages

**Investigation:**
```bash
# Enable transaction debugging
export QUARKUS_LOG_CATEGORY__IO_QUARKUS_NARAYANA_JTA__LEVEL=DEBUG
export QUARKUS_LOG_CATEGORY__COM_ARJUNA_ATS__LEVEL=DEBUG
```

**Common Causes:**
- Uncaught exceptions causing transaction rollback
- Transaction timeout
- Database connection issues during commit

**Solution:**
1. Add explicit `@Transactional` annotations to belief analyzer methods
2. Increase transaction timeout if needed
3. Add proper exception handling in belief processing

### 2. EntityManager Lifecycle Issues

**Symptoms:**
- Entities created but not persisted
- `EntityManager` errors in logs

**Investigation:**
```java
// Check if EntityManager is being properly injected
@Inject
EntityManager entityManager;

// Verify transaction state
boolean isActive = entityManager.getTransaction().isActive();
```

**Solution:**
Ensure `JpaBeliefRepository` properly manages EntityManager lifecycle:

```java
@Override
public BeliefEntity save(BeliefEntity belief) {
    EntityManager em = entityManagerFactory.createEntityManager();
    try {
        em.getTransaction().begin();
        BeliefEntity result = em.merge(belief);
        em.getTransaction().commit();
        return result;
    } catch (Exception e) {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
        throw e;
    } finally {
        em.close();
    }
}
```

### 3. Collection Fetch Warning (HHH90003004)

**Root Cause:**
The `BeliefEntity` has `@ElementCollection` fields (`evidenceMemoryIds` and `tags`) with lazy loading. When queries use pagination (`setMaxResults`), Hibernate must load collections in memory.

**Impact:**
- Performance degradation
- Potential memory issues
- May indicate underlying persistence problems

**Solutions:**

#### Option 1: Use JOIN FETCH for specific queries
```java
@NamedQuery(
    name = "BeliefEntity.findSimilarWithCollections",
    query = "SELECT DISTINCT b FROM BeliefEntity b " +
            "LEFT JOIN FETCH b.evidenceMemoryIds " +
            "LEFT JOIN FETCH b.tags " +
            "WHERE b.agentId = :agentId AND b.active = true"
)
```

#### Option 2: Create projection DTOs for queries with pagination
```java
@NamedQuery(
    name = "BeliefEntity.findSimilarProjection",
    query = "SELECT NEW ai.headkey.persistence.dto.BeliefSummaryDto(" +
            "b.id, b.agentId, b.statement, b.confidence) " +
            "FROM BeliefEntity b WHERE b.agentId = :agentId"
)
```

#### Option 3: Separate collection loading
```java
// First get beliefs without collections
List<BeliefEntity> beliefs = findBeliefsWithoutCollections(agentId, limit);

// Then load collections separately if needed
for (BeliefEntity belief : beliefs) {
    Hibernate.initialize(belief.getEvidenceMemoryIds());
    Hibernate.initialize(belief.getTags());
}
```

### 4. Database Schema Issues

**Investigation:**
```sql
-- Check if tables exist
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN ('beliefs', 'belief_evidence_memories', 'belief_tags');

-- Check table structure
\d beliefs
\d belief_evidence_memories
\d belief_tags
```

**Common Issues:**
- Hibernate not creating tables
- Wrong database being used
- Schema permissions

**Solution:**
```properties
# Ensure schema generation is enabled
quarkus.hibernate-orm.database.generation=update
quarkus.hibernate-orm.packages=ai.headkey.persistence.entities

# For debugging, enable DDL logging
%dev.quarkus.hibernate-orm.log.sql=true
%dev.quarkus.hibernate-orm.log.format-sql=true
```

### 5. Multiple EntityManager/DataSource Issues

**Symptoms:**
- Beliefs created in one context but not visible in another
- Inconsistent behavior

**Investigation:**
```bash
# Check if multiple persistence units are configured
grep -r "persistence-unit" src/main/resources/
grep -r "EntityManagerFactory" src/main/java/
```

**Solution:**
Ensure single EntityManager configuration in production:

```java
@Produces
@Singleton
public BeliefStorageService beliefStorageService() {
    // Use the same EntityManagerFactory everywhere
    BeliefRepository repository = new JpaBeliefRepository(entityManager.getEntityManagerFactory());
    BeliefConflictRepository conflictRepository = new JpaBeliefConflictRepository(entityManager.getEntityManagerFactory());
    return new JpaBeliefStorageService(repository, conflictRepository);
}
```

## Step-by-Step Debugging Process

### Step 1: Enable Comprehensive Logging

Add to `application.properties`:
```properties
# Enable SQL and transaction logging
quarkus.hibernate-orm.log.sql=true
quarkus.hibernate-orm.log.format-sql=true
quarkus.hibernate-orm.log.bind-parameters=true

# Enable belief system debugging
quarkus.log.category."ai.headkey.memory.abstracts.AbstractBeliefReinforcementConflictAnalyzer".level=FINEST
quarkus.log.category."ai.headkey.persistence.services.JpaBeliefStorageService".level=DEBUG
quarkus.log.category."ai.headkey.persistence.repositories".level=DEBUG

# Enable transaction debugging
quarkus.log.category."org.hibernate.engine.transaction".level=DEBUG
quarkus.log.category."io.quarkus.narayana.jta".level=DEBUG
```

### Step 2: Monitor Real-Time Activity

```bash
# Start monitoring belief creation
curl -X POST "http://localhost:8080/api/v1/diagnostics/beliefs/monitor/start?durationMinutes=5"

# Note the monitorId from response, then check after 5 minutes
curl http://localhost:8080/api/v1/diagnostics/beliefs/monitor/{monitorId}
```

### Step 3: Test Isolated Components

```bash
# Test just the persistence layer
curl -X POST http://localhost:8080/api/v1/diagnostics/beliefs/test-persistence

# Check if the test belief appears in database
psql -h localhost -U headkey -d headkey -c "SELECT * FROM beliefs WHERE agent_id = 'test-agent-diagnostic';"
```

### Step 4: Verify Belief Extraction vs Persistence

1. **Check if beliefs are being extracted:**
   - Look for `"Received X extracted beliefs from extraction service"` in logs
   - Should see `"Processing extracted belief: ..."` messages

2. **Check if beliefs reach storage service:**
   - Look for JpaBeliefStorageService debug messages
   - Should see SQL INSERT statements

3. **Check if transactions commit:**
   - Look for transaction commit/rollback messages
   - Check for any exceptions during commit

### Step 5: Database-Level Verification

```sql
-- Check for any beliefs
SELECT COUNT(*) FROM beliefs;

-- Check recent beliefs
SELECT id, agent_id, statement, created_at 
FROM beliefs 
ORDER BY created_at DESC 
LIMIT 10;

-- Check for evidence and tags
SELECT b.id, b.statement, 
       array_agg(DISTINCT bem.memory_id) as evidence,
       array_agg(DISTINCT bt.tag) as tags
FROM beliefs b
LEFT JOIN belief_evidence_memories bem ON b.id = bem.belief_id
LEFT JOIN belief_tags bt ON b.id = bt.belief_id
GROUP BY b.id, b.statement
ORDER BY b.created_at DESC
LIMIT 5;
```

## Environment-Specific Fixes

### Development Environment
```properties
# Use H2 for simpler debugging
%dev.quarkus.datasource.db-kind=h2
%dev.quarkus.datasource.jdbc.url=jdbc:h2:mem:headkey;DB_CLOSE_DELAY=-1
%dev.quarkus.hibernate-orm.database.generation=drop-and-create
```

### Production Environment
```properties
# Ensure proper PostgreSQL configuration
%prod.quarkus.datasource.db-kind=postgresql
%prod.quarkus.datasource.jdbc.url=${DATABASE_URL}
%prod.quarkus.datasource.username=${DATABASE_USER}
%prod.quarkus.datasource.password=${DATABASE_PASSWORD}
%prod.quarkus.hibernate-orm.database.generation=update

# Production-safe logging
%prod.quarkus.hibernate-orm.log.sql=false
%prod.quarkus.log.category."ai.headkey".level=INFO
```

## Performance Optimization

### Fix Collection Fetch Warning

Create optimized queries in `BeliefRepository`:

```java
@NamedQuery(
    name = "BeliefEntity.findSimilarOptimized",
    query = "SELECT b.id, b.agentId, b.statement, b.confidence " +
            "FROM BeliefEntity b " +
            "WHERE b.agentId = :agentId AND b.active = true " +
            "ORDER BY b.confidence DESC"
)
```

Use batch loading for collections:
```java
public List<BeliefEntity> findSimilarBeliefsOptimized(String agentId, int limit) {
    // First get belief IDs with pagination
    List<String> beliefIds = entityManager
        .createQuery("SELECT b.id FROM BeliefEntity b WHERE b.agentId = :agentId", String.class)
        .setParameter("agentId", agentId)
        .setMaxResults(limit)
        .getResultList();
    
    // Then fetch full entities with collections
    return entityManager
        .createQuery("SELECT DISTINCT b FROM BeliefEntity b " +
                    "LEFT JOIN FETCH b.evidenceMemoryIds " +
                    "LEFT JOIN FETCH b.tags " +
                    "WHERE b.id IN :ids", BeliefEntity.class)
        .setParameter("ids", beliefIds)
        .getResultList();
}
```

## Preventive Measures

### 1. Add Health Checks
```java
@ApplicationScoped
public class BeliefPersistenceHealthCheck implements HealthCheck {
    
    @Inject
    BeliefStorageService storageService;
    
    @Override
    public HealthCheckResponse call() {
        try {
            // Test basic persistence functionality
            boolean canStore = testBeliefStorage();
            return HealthCheckResponse.named("belief-persistence")
                .status(canStore)
                .withData("timestamp", Instant.now().toString())
                .build();
        } catch (Exception e) {
            return HealthCheckResponse.down("belief-persistence");
        }
    }
}
```

### 2. Add Metrics
```java
@ApplicationScoped
public class BeliefMetrics {
    
    @Counted(name = "beliefs_created", description = "Number of beliefs created")
    @Timed(name = "belief_creation_time", description = "Time to create a belief")
    public void recordBeliefCreation() {
        // Called after successful belief creation
    }
}
```

### 3. Add Scheduled Verification
```java
@ApplicationScoped
public class BeliefPersistenceVerification {
    
    @Scheduled(every = "5m")
    @Transactional
    public void verifyPersistence() {
        // Periodically verify beliefs are being persisted
        long recentCount = countBeliefsCreatedInLastMinutes(5);
        if (recentCount == 0) {
            LOG.warn("No beliefs created in the last 5 minutes");
        }
    }
}
```

## Emergency Recovery

If beliefs are completely missing from production:

### 1. Immediate Data Recovery
```sql
-- Check if beliefs exist in different schema/database
SELECT schemaname, tablename FROM pg_tables WHERE tablename = 'beliefs';

-- Check for table with different name
SELECT tablename FROM pg_tables WHERE tablename LIKE '%belief%';
```

### 2. Force Schema Recreation (CAUTION)
```properties
# Only for non-production or if you can lose existing data
quarkus.hibernate-orm.database.generation=drop-and-create
```

### 3. Manual Table Creation
```sql
-- If Hibernate isn't creating tables, create manually
CREATE TABLE beliefs (
    id VARCHAR(100) PRIMARY KEY,
    agent_id VARCHAR(100) NOT NULL,
    statement TEXT NOT NULL,
    confidence DECIMAL(3,2) NOT NULL,
    category VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    last_updated TIMESTAMP NOT NULL,
    reinforcement_count INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT true,
    version BIGINT
);

CREATE TABLE belief_evidence_memories (
    belief_id VARCHAR(100) NOT NULL,
    memory_id VARCHAR(100) NOT NULL,
    FOREIGN KEY (belief_id) REFERENCES beliefs(id)
);

CREATE TABLE belief_tags (
    belief_id VARCHAR(100) NOT NULL,
    tag VARCHAR(100) NOT NULL,
    FOREIGN KEY (belief_id) REFERENCES beliefs(id)
);
```

## Contact and Escalation

If none of these steps resolve the issue:

1. **Collect Logs:** Save complete application logs with debug logging enabled
2. **Database State:** Export current database schema and any existing data
3. **Configuration:** Document exact environment configuration
4. **Reproduction Steps:** Document exact steps that trigger the issue

The diagnostic endpoints created in this guide will provide comprehensive information for further analysis.