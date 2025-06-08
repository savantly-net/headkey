# HeadKey Persistence PostgreSQL Module

This module provides PostgreSQL-based persistence implementations for the HeadKey memory system, specifically focusing on the Belief Reinforcement & Conflict Analyzer (BRCA) storage layer.

## Overview

The persistence-postgres module implements the `BeliefStorageService` interface using JPA/Hibernate with PostgreSQL as the backend database. It provides a production-ready, scalable, and performant storage solution for beliefs and conflicts.

## Key Features

- **JPA/Hibernate Integration**: Full JPA 3.1 compliance with Hibernate as the persistence provider
- **PostgreSQL Optimization**: Database-specific optimizations and indexing strategies
- **AI Integration**: Integration with LangChain4J for enhanced similarity search and semantic operations
- **Transaction Management**: Full ACID transaction support
- **Connection Pooling**: HikariCP integration for high-performance connection management
- **Batch Operations**: Optimized bulk operations for better performance
- **Comprehensive Indexing**: Strategic indexes for fast queries and searches
- **TestContainers Integration**: Full integration testing with real PostgreSQL instances

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Client Code                          │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│            BeliefStorageService Interface               │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│           JpaBeliefStorageService                       │
│  ┌─────────────────┬───────────────────┬───────────────┐│
│  │                 │                   │               ││
│  ▼                 ▼                   ▼               ││
│ BeliefRepository   │ ConflictRepository │ Mappers       ││
│ (JPA Impl)         │ (JPA Impl)         │ (DTO<->Entity)││
└─────────────────────┬───────────────────┬───────────────┘
                      │                   │
        ┌─────────────▼─────────────┐   ┌▼─────────────────┐
        │     BeliefEntity          │   │ BeliefConflictEntity │
        │     (JPA Entity)          │   │ (JPA Entity)         │
        └─────────────┬─────────────┘   └┬─────────────────┘
                      │                   │
            ┌─────────▼───────────────────▼─────────┐
            │        PostgreSQL Database            │
            └───────────────────────────────────────┘
```

## Components

### Core Classes

- **`JpaBeliefStorageService`**: Main implementation of BeliefStorageService
- **`BeliefEntity`**: JPA entity for beliefs with comprehensive mapping
- **`BeliefConflictEntity`**: JPA entity for conflicts with full lifecycle support
- **`BeliefMapper`**: Converts between DTOs and JPA entities
- **`BeliefConflictMapper`**: Converts between conflict DTOs and entities

### Repositories

- **`BeliefRepository`**: Interface defining belief data access operations
- **`JpaBeliefRepository`**: JPA implementation with optimized queries
- **`BeliefConflictRepository`**: Interface defining conflict data access operations
- **`JpaBeliefConflictRepository`**: JPA implementation with conflict management

### Factory

- **`JpaBeliefStorageServiceFactory`**: Factory for creating configured instances

## Database Schema

### Tables

- **`beliefs`**: Core belief storage with full indexing
- **`belief_evidence_memories`**: Evidence memory references
- **`belief_tags`**: Belief tagging system
- **`belief_conflicts`**: Conflict tracking and resolution
- **`conflict_belief_ids`**: Conflict-to-belief relationships

### Key Indexes

- Agent-based lookups: `idx_belief_agent_id`
- Category filtering: `idx_belief_category`
- Confidence queries: `idx_belief_confidence`
- Full-text search: `idx_belief_statement_gin`
- Composite indexes for common query patterns

## Usage

### Basic Setup

```java
// Simple setup
BeliefStorageService storage = JpaBeliefStorageServiceFactory.createDefault();

// With custom DataSource
DataSource dataSource = createPostgreSQLDataSource();
BeliefStorageService storage = JpaBeliefStorageServiceFactory.create(dataSource);

// Production configuration
BeliefStorageService storage = JpaBeliefStorageServiceFactory.createForProduction(dataSource);
```

### Builder Pattern

```java
BeliefStorageService storage = JpaBeliefStorageServiceFactory.builder()
    .withDataSource(dataSource)
    .withJpaProperty("hibernate.show_sql", "true")
    .withAutoCreateSchema(true)
    .withStatistics(true)
    .build();
```

### Basic Operations

```java
// Store a belief
Belief belief = new Belief("belief-1", "user-123", "I love pizza", 0.8);
belief.setCategory("preference");
Belief stored = storage.storeBelief(belief);

// Retrieve beliefs
Optional<Belief> retrieved = storage.getBeliefById("belief-1");
List<Belief> userBeliefs = storage.getBeliefsForAgent("user-123", false);
List<Belief> preferences = storage.getBeliefsInCategory("preference", null, false);

// Search beliefs
List<Belief> searchResults = storage.searchBeliefs("pizza", null, 10);
List<SimilarBelief> similar = storage.findSimilarBeliefs("I really love pizza", "user-123", 0.7, 5);

// Manage conflicts
BeliefConflict conflict = new BeliefConflict();
conflict.setId("conflict-1");
conflict.setAgentId("user-123");
conflict.setConflictingBeliefIds(Arrays.asList("belief-1", "belief-2"));
BeliefConflict stored = storage.storeConflict(conflict);

List<BeliefConflict> unresolved = storage.getUnresolvedConflicts("user-123");
```

## Configuration

### Persistence Units

The module provides three pre-configured persistence units:

1. **`headkey-beliefs-postgresql`**: Production configuration
2. **`headkey-beliefs-development`**: Development with verbose logging
3. **`headkey-beliefs-test`**: Testing with H2 in-memory database

### Environment-Specific Configuration

#### Production
```java
BeliefStorageService storage = JpaBeliefStorageServiceFactory.createForProduction(dataSource);
```

Features:
- Connection pooling optimization
- Performance monitoring enabled
- Schema validation (no auto-updates)
- Batch processing optimization

#### Development
```java
BeliefStorageService storage = JpaBeliefStorageServiceFactory.createForDevelopment(dataSource);
```

Features:
- SQL logging enabled
- Auto schema updates
- Smaller connection pools
- Development-friendly settings

#### Testing
```java
BeliefStorageService storage = JpaBeliefStorageServiceFactory.createForTesting();
```

Features:
- H2 in-memory database
- Schema auto-creation
- Fast execution settings
- No statistics overhead

### Database Connection

#### Using JDBC URL
```java
String jdbcUrl = "jdbc:postgresql://localhost:5432/headkey";
BeliefStorageService storage = JpaBeliefStorageServiceFactory.createWithUrl(
    jdbcUrl, "username", "password"
);
```

#### Using DataSource
```java
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:postgresql://localhost:5432/headkey");
config.setUsername("headkey_user");
config.setPassword("headkey_password");
config.setMaximumPoolSize(20);
config.setMinimumIdle(5);

HikariDataSource dataSource = new HikariDataSource(config);
BeliefStorageService storage = JpaBeliefStorageServiceFactory.create(dataSource);
```

## Performance Optimization

### Batch Operations

```java
// Batch store
List<Belief> beliefs = Arrays.asList(belief1, belief2, belief3);
List<Belief> stored = storage.storeBeliefs(beliefs);

// Batch retrieval
Set<String> ids = Set.of("belief-1", "belief-2", "belief-3");
List<Belief> retrieved = storage.getBeliefsById(ids);
```

### Indexing Strategy

The module uses comprehensive indexing:
- **Primary lookups**: ID-based access
- **Agent filtering**: Fast agent-specific queries
- **Category filtering**: Efficient category-based searches
- **Full-text search**: GIN indexes for statement content
- **Composite indexes**: Multi-column indexes for common patterns

### Connection Pooling

HikariCP configuration for production:
```java
properties.put("hibernate.hikari.minimumIdle", "5");
properties.put("hibernate.hikari.maximumPoolSize", "20");
properties.put("hibernate.hikari.idleTimeout", "300000");
properties.put("hibernate.hikari.connectionTimeout", "30000");
properties.put("hibernate.hikari.leakDetectionThreshold", "60000");
```

## Testing

### Unit Tests

```bash
./gradlew :persistence-postgres:test
```

### Integration Tests with TestContainers

```bash
./gradlew :persistence-postgres:integrationTest
```

The integration tests use TestContainers to spin up real PostgreSQL instances:

```java
@Testcontainers
class JpaBeliefStorageServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("headkey_test")
            .withUsername("test_user")
            .withPassword("test_password");
    
    @Test
    void testCompleteWorkflow() {
        // Test against real PostgreSQL
    }
}
```

## LangChain4J Integration

The module integrates with the LangChain4J module for AI-powered operations:

- **Semantic Similarity**: Enhanced similarity calculations using embeddings
- **Intelligent Search**: AI-powered belief discovery
- **Conflict Detection**: Advanced conflict identification using NLP

```java
// AI-enhanced similarity search (when LangChain4J is available)
List<SimilarBelief> similar = storage.findSimilarBeliefs(
    "I really enjoy Italian cuisine", 
    "user-123", 
    0.8, 
    10
);
```

## Monitoring and Health

### Health Checks

```java
boolean healthy = storage.isHealthy();
Map<String, Object> healthInfo = storage.getHealthInfo();
```

### Statistics

```java
Map<String, Object> stats = storage.getStorageStatistics();
Map<String, Long> categoryDist = storage.getBeliefDistributionByCategory(null);
Map<String, Long> confidenceDist = storage.getBeliefDistributionByConfidence(null);
```

### Maintenance Operations

```java
// Optimize storage
Map<String, Object> results = storage.optimizeStorage();

// Validate integrity
Map<String, Object> validation = storage.validateIntegrity();

// Create backup (external tools required)
Map<String, Object> backup = storage.createBackup("backup-20231201", options);
```

## Dependencies

### Core Dependencies
- Jakarta Persistence API 3.1.0
- Hibernate Core 6.4.4.Final
- PostgreSQL JDBC Driver 42.7.2
- HikariCP 5.0.1

### Test Dependencies
- TestContainers (PostgreSQL, JUnit Jupiter)
- H2 Database (for unit tests)
- Mockito
- JUnit Jupiter

### LangChain4J Integration
- LangChain4J Core
- LangChain4J BOM for version management

## Future Enhancements

### Planned Features
1. **Vector Similarity Search**: Integration with pgvector for semantic search
2. **Advanced Analytics**: Time-series analysis of belief evolution
3. **Partitioning**: Table partitioning for large-scale deployments
4. **Read Replicas**: Support for read-only replicas
5. **Caching Layer**: Redis integration for performance boost

### AI Integration Roadmap
1. **Embedding Storage**: Store and index belief embeddings
2. **Semantic Clustering**: AI-powered belief categorization
3. **Conflict Prediction**: Proactive conflict detection
4. **Belief Summarization**: AI-generated belief summaries

## Contributing

1. Follow the existing code patterns and conventions
2. Add comprehensive tests for new features
3. Update documentation for API changes
4. Ensure backward compatibility when possible
5. Run integration tests before submitting PRs

## License

This module is part of the HeadKey project and follows the same licensing terms.
