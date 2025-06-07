# JDBC Memory Encoding System

A production-ready, database-backed implementation of the HeadKey Memory Encoding System (MES) with vector similarity search capabilities.

## Overview

The JDBC Memory Encoding System provides persistent storage for memory records using SQL databases with support for:

- **Vector embeddings** for semantic similarity search
- **Pluggable similarity strategies** for different database backends
- **HSQLDB support** for testing and development
- **PostgreSQL support** with pgvector for production
- **Connection pooling** with HikariCP for performance
- **Transaction support** for data consistency
- **Comprehensive monitoring** and statistics

## Features

### Core Capabilities
- ✅ Store and retrieve memory records with metadata
- ✅ Vector embedding storage and similarity search
- ✅ Category-based memory organization
- ✅ Agent-specific memory isolation
- ✅ Time-based memory retrieval (age-based queries)
- ✅ Batch operations for performance
- ✅ Concurrent access with thread safety

### Database Support
- ✅ **HSQLDB** - In-memory and file-based for testing
- ✅ **PostgreSQL** - Production-ready with pgvector extension
- 🔄 **Extensible** - Easy to add support for other databases

### Similarity Search
- ✅ **Vector similarity** - Dot product, cosine similarity (PostgreSQL)
- ✅ **Text-based fallback** - Full-text and pattern matching (HSQLDB)
- ✅ **Hybrid scoring** - Combines vector, text, and relevance scores
- ✅ **Configurable strategies** - Pluggable similarity implementations

## Quick Start

### 1. Add Dependencies

Add to your `build.gradle`:

```gradle
dependencies {
    implementation project(':headkey-core')
    
    // Database drivers (choose based on your needs)
    implementation 'org.hsqldb:hsqldb:2.7.2'           // For testing
    implementation 'org.postgresql:postgresql:42.7.1'  // For production
    implementation 'com.zaxxer:HikariCP:5.1.0'         // Connection pooling
}
```

### 2. Basic Usage (HSQLDB for Testing)

```java
import ai.headkey.memory.implementations.JdbcMemorySystemFactory;
import ai.headkey.memory.interfaces.MemoryEncodingSystem;

// Create in-memory test system
MemoryEncodingSystem memorySystem = JdbcMemorySystemFactory.createInMemoryTestSystem();

// Store a memory
CategoryLabel category = new CategoryLabel();
category.setPrimary("technology");
category.setSecondary("ai");

Metadata metadata = new Metadata();
metadata.setAgentId("my-agent");
metadata.setSource("user-input");

MemoryRecord memory = memorySystem.encodeAndStore(
    "Machine learning is transforming software development",
    category,
    metadata
);

// Search for similar memories
List<MemoryRecord> results = memorySystem.searchSimilar("artificial intelligence", 5);
```

### 3. Production Setup (PostgreSQL)

```java
// First, ensure PostgreSQL has pgvector extension
// CREATE EXTENSION vector;

// Create embedding generator (integrate with your embedding service)
VectorEmbeddingGenerator embeddingGenerator = content -> {
    // Call OpenAI API, HuggingFace, or local model
    return openAiClient.createEmbedding(content).getData().get(0).getEmbedding();
};

// Create production system
MemoryEncodingSystem memorySystem = JdbcMemorySystemFactory.createPostgreSQLSystem(
    "localhost", 5432, "headkey_db", "username", "password",
    embeddingGenerator
);
```

### 4. Advanced Configuration

```java
MemoryEncodingSystem memorySystem = JdbcMemorySystemFactory.builder()
    .jdbcUrl("jdbc:postgresql://localhost:5432/headkey")
    .credentials("user", "password")
    .poolSize(20, 5)  // max 20, min 5 connections
    .embeddingGenerator(myEmbeddingGenerator)
    .build();
```

## Database Setup

### PostgreSQL Setup

1. **Install PostgreSQL** (version 11+)

2. **Install pgvector extension**:
   ```sql
   CREATE EXTENSION vector;
   ```

3. **Create database**:
   ```sql
   CREATE DATABASE headkey_memory;
   ```

4. **Run the schema** (automatically created by the system):
   ```bash
   # The schema is automatically applied on first run
   # See: core/src/main/resources/sql/postgresql-schema.sql
   ```

### HSQLDB Setup

HSQLDB requires no setup - it's automatically configured for testing:

```java
// In-memory (data lost on shutdown)
MemoryEncodingSystem testSystem = JdbcMemorySystemFactory.createInMemoryTestSystem();

// File-based (data persisted)
MemoryEncodingSystem fileSystem = JdbcMemorySystemFactory.createHSQLDBSystem(
    "file:/path/to/database", embeddingGenerator
);
```

## Schema Overview

### Core Table: `memories`

| Column | Type | Description |
|--------|------|-------------|
| `id` | VARCHAR(255) | Unique memory identifier |
| `agent_id` | VARCHAR(255) | Agent/context identifier |
| `content` | TEXT | Original memory content |
| `category_primary` | VARCHAR(255) | Primary category |
| `category_secondary` | VARCHAR(255) | Secondary category |
| `category_tags` | TEXT | Comma-separated tags |
| `category_confidence` | DOUBLE | Category confidence score |
| `metadata_json` | JSONB/TEXT | Flexible metadata storage |
| `created_at` | TIMESTAMP | Creation timestamp |
| `last_accessed` | TIMESTAMP | Last access timestamp |
| `relevance_score` | DOUBLE | Current relevance score |
| `version` | BIGINT | Version for optimistic locking |
| `vector_embedding` | VECTOR/TEXT | Vector embedding for similarity |

### Indexes

**PostgreSQL:**
- Vector similarity indexes (IVFFlat for cosine, dot product, L2)
- Full-text search indexes (GIN)
- Composite indexes for filtered queries

**HSQLDB:**
- B-tree indexes on frequently queried columns
- Text-based search optimization

## Similarity Search Strategies

### PostgreSQL Strategy (Production)

Uses native vector operations with hybrid scoring:

```sql
-- Vector similarity (cosine distance)
SELECT * FROM memories 
ORDER BY vector_embedding <=> $query_vector 
LIMIT 10;

-- Hybrid scoring (vector + text + relevance)
SELECT *, (
    vector_similarity_score + 
    text_match_score + 
    relevance_boost + 
    recency_boost
) AS total_score
FROM memories
ORDER BY total_score DESC;
```

### HSQLDB Strategy (Testing)

Uses text-based similarity with pattern matching:

```sql
-- Text similarity with scoring
SELECT *, (
    CASE WHEN LOWER(content) LIKE LOWER('%query%') THEN 100.0
         WHEN LOWER(content) LIKE LOWER('%word1%word2%') THEN 75.0
         ELSE 0.0 END +
    category_match_score +
    relevance_score * 10
) AS similarity_score
FROM memories
ORDER BY similarity_score DESC;
```

## Performance Optimization

### Connection Pooling

The system uses HikariCP for efficient connection management:

```java
// Optimize pool settings for your workload
JdbcMemorySystemFactory.builder()
    .poolSize(20, 5)  // max 20, min 5 connections
    .timeouts(30000, 600000, 1800000)  // connection, idle, max lifetime
    .build();
```

### Indexing Strategy

**For high-write workloads:**
- Use fewer indexes to reduce write overhead
- Optimize for batch operations

**For high-read workloads:**
- Create indexes on frequently queried fields
- Use partial indexes for common filters

**For vector similarity:**
- Tune IVFFlat list count based on data size
- Consider HNSW indexes for very large datasets

### Batch Operations

```java
// Efficient batch storage
List<MemoryRecord> memories = // ... prepare memories
Set<String> ids = memories.stream().map(MemoryRecord::getId).collect(toSet());

// Batch retrieval
Map<String, MemoryRecord> retrieved = memorySystem.getMemories(ids);

// Batch deletion
Set<String> deleted = memorySystem.removeMemories(idsToDelete);
```

## Monitoring and Maintenance

### System Statistics

```java
Map<String, Object> stats = memorySystem.getStorageStatistics();
System.out.println("Total memories: " + stats.get("totalMemories"));
System.out.println("Operations/sec: " + stats.get("operationsPerSecond"));
System.out.println("Strategy: " + stats.get("strategyName"));
```

### Agent-Specific Statistics

```java
Map<String, Object> agentStats = memorySystem.getAgentStatistics("my-agent");
System.out.println("Agent memories: " + agentStats.get("totalMemories"));
System.out.println("Categories: " + agentStats.get("categoryBreakdown"));
```

### Health Checks

```java
boolean healthy = memorySystem.isHealthy();
if (!healthy) {
    // Handle unhealthy system
    logger.error("Memory system is unhealthy");
}
```

### System Optimization

```java
// Regular maintenance
Map<String, Object> results = memorySystem.optimize(true);  // with vacuum
System.out.println("Optimization took: " + results.get("optimizationDurationMs") + "ms");
```

## Configuration Examples

### Development Configuration

```java
// Fast startup, in-memory, no persistence
MemoryEncodingSystem devSystem = JdbcMemorySystemFactory.builder()
    .jdbcUrl("jdbc:hsqldb:mem:devdb")
    .credentials("SA", "")
    .poolSize(5, 1)
    .embeddingGenerator(JdbcMemorySystemFactory.createMockEmbeddingGenerator())
    .build();
```

### Testing Configuration

```java
// File-based HSQLDB for integration tests
MemoryEncodingSystem testSystem = JdbcMemorySystemFactory.builder()
    .jdbcUrl("jdbc:hsqldb:file:target/test-db/testdb")
    .credentials("SA", "")
    .poolSize(3, 1)
    .embeddingGenerator(null)  // No embeddings in tests
    .build();
```

### Production Configuration

```java
// PostgreSQL with connection pooling and monitoring
MemoryEncodingSystem prodSystem = JdbcMemorySystemFactory.builder()
    .jdbcUrl("jdbc:postgresql://db-host:5432/headkey_prod")
    .credentials(System.getenv("DB_USER"), System.getenv("DB_PASSWORD"))
    .poolSize(50, 10)
    .timeouts(30000, 600000, 1800000)
    .connectionProperty("ssl", "true")
    .connectionProperty("sslmode", "require")
    .embeddingGenerator(openAiEmbeddingGenerator)
    .build();
```

## Integration with Embedding Services

### OpenAI Integration

```java
VectorEmbeddingGenerator openAiGenerator = content -> {
    OpenAiService service = new OpenAiService(apiKey);
    EmbeddingRequest request = EmbeddingRequest.builder()
        .model("text-embedding-ada-002")
        .input(Arrays.asList(content))
        .build();
    
    EmbeddingResult result = service.createEmbedding(request);
    return result.getData().get(0).getEmbedding().stream()
        .mapToDouble(Double::doubleValue)
        .toArray();
};
```

### HuggingFace Integration

```java
VectorEmbeddingGenerator huggingFaceGenerator = content -> {
    // Use HuggingFace Transformers library
    SentenceTransformer model = new SentenceTransformer("all-MiniLM-L6-v2");
    return model.encode(content);
};
```

### Local Model Integration

```java
VectorEmbeddingGenerator localGenerator = content -> {
    // Use ONNX Runtime or TensorFlow Java
    OrtSession session = // ... load your model
    OnnxTensor inputTensor = // ... prepare input
    OrtSession.Result result = session.run(Collections.singletonMap("input", inputTensor));
    return result.get(0).getValue(); // Extract embedding vector
};
```

## Testing

### Unit Tests

```bash
# Run all JDBC memory system tests
./gradlew :core:test --tests "*JdbcMemory*"

# Run similarity strategy tests
./gradlew :core:test --tests "*SimilarityStrategy*"
```

### Integration Tests

```bash
# Run with PostgreSQL (requires Docker)
docker run --name postgres-test -e POSTGRES_PASSWORD=test -d postgres:13
docker exec postgres-test psql -U postgres -c "CREATE EXTENSION vector;"

# Run integration tests
./gradlew :core:integrationTest
```

### Example Usage

```bash
# Run the comprehensive example
./gradlew :core:test --tests "*JdbcMemorySystemExample*"
```

## Troubleshooting

### Common Issues

**PostgreSQL pgvector not found:**
```sql
-- Install pgvector extension
CREATE EXTENSION vector;
```

**Connection pool exhaustion:**
```java
// Increase pool size or check for connection leaks
.poolSize(50, 10)  // Increase max connections
```

**Slow vector similarity search:**
```sql
-- Check if vector indexes exist
SELECT indexname FROM pg_indexes WHERE tablename = 'memories';

-- Rebuild indexes if needed
REINDEX INDEX CONCURRENTLY idx_memories_vector_cosine;
```

**High memory usage:**
```java
// Enable connection leak detection
.connectionProperty("leakDetectionThreshold", "60000")
```

### Logging

Enable debug logging for troubleshooting:

```properties
# logback.xml or application.properties
logging.level.ai.headkey.memory=DEBUG
logging.level.com.zaxxer.hikari=DEBUG
```

## Performance Benchmarks

Typical performance characteristics:

| Operation | HSQLDB (in-memory) | PostgreSQL (local) | PostgreSQL (remote) |
|-----------|-------------------|-------------------|-------------------|
| Store memory | ~1ms | ~2-5ms | ~10-20ms |
| Retrieve by ID | ~0.1ms | ~1-2ms | ~5-10ms |
| Vector similarity search | ~5-10ms* | ~2-10ms | ~20-50ms |
| Text search | ~1-5ms | ~5-15ms | ~20-60ms |

*Text-based similarity for HSQLDB

## Contributing

To extend the JDBC Memory System:

1. **Add new database support**: Implement `SimilaritySearchStrategy`
2. **Optimize queries**: Modify strategy implementations
3. **Add new indexes**: Update schema files
4. **Improve monitoring**: Extend statistics collection

## License

This component is part of the HeadKey project and follows the same licensing terms.