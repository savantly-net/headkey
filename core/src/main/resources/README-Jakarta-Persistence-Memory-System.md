# Jakarta Persistence Memory Encoding System

This document provides a comprehensive guide to the Jakarta Persistence (JPA) implementation of the MemoryEncodingSystem interface.

## Overview

The `JpaMemoryEncodingSystem` is a production-ready implementation that provides persistent storage for the Headkey memory system using Jakarta Persistence API. It extends the `AbstractMemoryEncodingSystem` to leverage common functionality while implementing database-specific persistence operations.

## Key Features

- **Persistent Storage**: Uses JPA entities to store memories in relational databases
- **Vector Embeddings**: Stores and retrieves vector embeddings for similarity search
- **Transaction Management**: Ensures data consistency with proper transaction handling
- **Multiple Database Support**: Works with PostgreSQL, MySQL, H2, and HSQLDB
- **Connection Pooling**: Uses HikariCP for efficient database connection management
- **Optimized Queries**: Leverages JPA Criteria API and named queries for performance
- **JSON Storage**: Complex objects stored as JSON using custom AttributeConverters

## Architecture

### Abstract Base Class Pattern

The implementation follows a clean architecture pattern:

```
MemoryEncodingSystem (Interface)
    ↓
AbstractMemoryEncodingSystem (Abstract Class)
    ↓
JpaMemoryEncodingSystem (Concrete Implementation)
```

**Benefits:**
- Separates persistence concerns from business logic
- Shares common functionality (validation, statistics, embedding generation)
- Makes it easy to add new persistence implementations
- Follows SOLID principles

### Entity Mapping

The `MemoryEntity` class maps `MemoryRecord` DTOs to database tables:

- **Primary Storage**: Core fields like ID, content, agent ID
- **JSON Storage**: Complex objects (CategoryLabel, Metadata) stored as JSON
- **Vector Storage**: Embeddings stored with precomputed magnitudes for optimization
- **Indexing**: Strategic indexes for performance on common queries

## Database Configuration

### Supported Databases

1. **PostgreSQL** (Recommended for Production)
2. **MySQL/MariaDB** (Alternative for Production)
3. **HSQLDB** (Development)
4. **H2** (Testing)

### Persistence Units

The system includes preconfigured persistence units in `persistence.xml`:

- `headkey-memory-postgresql`: Production PostgreSQL setup
- `headkey-memory-mysql`: Production MySQL setup  
- `headkey-memory-hsqldb-dev`: Development HSQLDB setup
- `headkey-memory-h2-test`: Testing H2 setup

### Database Schema

The system automatically creates the following table structure:

```sql
CREATE TABLE memory_records (
    id VARCHAR(50) PRIMARY KEY,
    agent_id VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    category TEXT,
    metadata TEXT,
    embedding TEXT,
    embedding_magnitude DOUBLE,
    relevance_score DOUBLE,
    created_at TIMESTAMP NOT NULL,
    last_accessed TIMESTAMP,
    version BIGINT
);

-- Indexes for performance
CREATE INDEX idx_memory_agent_id ON memory_records(agent_id);
CREATE INDEX idx_memory_created_at ON memory_records(created_at);
CREATE INDEX idx_memory_last_accessed ON memory_records(last_accessed);
CREATE INDEX idx_memory_relevance_score ON memory_records(relevance_score);
CREATE INDEX idx_memory_agent_created ON memory_records(agent_id, created_at);
CREATE INDEX idx_memory_agent_accessed ON memory_records(agent_id, last_accessed);
```

## Usage Examples

### Basic Setup

```java
// Create EntityManagerFactory
EntityManagerFactory emf = Persistence.createEntityManagerFactory("headkey-memory-postgresql");

// Create memory system with embedding generator
VectorEmbeddingGenerator embeddingGen = content -> {
    // Your embedding generation logic here
    return generateEmbedding(content);
};

JpaMemoryEncodingSystem memorySystem = new JpaMemoryEncodingSystem(emf, embeddingGen);
```

### Advanced Configuration

```java
JpaMemoryEncodingSystem memorySystem = new JpaMemoryEncodingSystem(
    entityManagerFactory,
    embeddingGenerator,
    100,    // batch size for bulk operations
    true,   // enable second-level cache
    1000,   // max similarity search results
    0.1     // similarity threshold
);
```

### Storing Memories

```java
// Create category
CategoryLabel category = new CategoryLabel("knowledge", "ai");
category.setConfidence(0.9);
Set<String> tags = new HashSet<>();
tags.add("machine-learning");
tags.add("artificial-intelligence");
category.setTags(tags);

// Create metadata
Metadata metadata = new Metadata();
metadata.setProperty("agentId", "agent-001");
metadata.setImportance(0.8);
metadata.setSource("research-paper");

// Store memory
MemoryRecord stored = memorySystem.encodeAndStore(
    "Artificial intelligence is transforming technology...",
    category,
    metadata
);
```

### Similarity Search

```java
// Search for similar memories
List<MemoryRecord> similar = memorySystem.searchSimilar(
    "machine learning algorithms", 
    5  // limit to 5 results
);

// Results are ordered by similarity (highest first)
similar.forEach(memory -> {
    System.out.println("Content: " + memory.getContent());
    System.out.println("Relevance: " + memory.getRelevanceScore());
});
```

### Querying Memories

```java
// Get memories for specific agent
List<MemoryRecord> agentMemories = memorySystem.getMemoriesForAgent("agent-001", 10);

// Get memories in category
List<MemoryRecord> categoryMemories = memorySystem.getMemoriesInCategory("knowledge", "agent-001", 5);

// Get old memories for cleanup
List<MemoryRecord> oldMemories = memorySystem.getOldMemories(30 * 24 * 3600, "agent-001", 50); // 30 days
```

## Performance Considerations

### Vector Similarity Search

The current implementation uses in-memory similarity calculations after database retrieval. For large datasets, consider:

1. **Database-specific Vector Extensions**: 
   - PostgreSQL: Use pgvector extension
   - MySQL: Use vector search plugins
   
2. **Hybrid Approach**: 
   - Pre-filter using metadata/text search
   - Apply vector similarity on smaller result sets

3. **Caching**: 
   - Enable second-level cache for frequently accessed memories
   - Use application-level caching for embedding computations

### Optimization Settings

```java
// Enable batch processing
Map<String, Object> props = new HashMap<>();
props.put("hibernate.jdbc.batch_size", "100");
props.put("hibernate.order_inserts", "true");
props.put("hibernate.order_updates", "true");

// Connection pooling
props.put("hibernate.hikari.maximumPoolSize", "20");
props.put("hibernate.hikari.minimumIdle", "5");
```

## JSON Storage and Converters

### AttributeConverters

The system uses custom AttributeConverters for complex object storage:

- `CategoryLabelConverter`: Handles CategoryLabel serialization
- `MetadataConverter`: Handles Metadata serialization  
- `VectorEmbeddingConverter`: Handles vector embedding arrays

### JSON Configuration

Jackson ObjectMapper is configured to handle:
- Time zones and instant formatting
- Unknown properties gracefully
- Collection serialization edge cases

## Transaction Management

### Automatic Transactions

The implementation uses resource-local transactions:
- Each operation runs in its own transaction
- Automatic rollback on exceptions
- Proper resource cleanup in finally blocks

### Bulk Operations

Batch operations are optimized for performance:
- Configurable batch sizes
- Reduced transaction overhead
- Efficient memory usage

## Error Handling

### Exception Hierarchy

```
StorageException (Runtime)
├── Persistence errors
├── Database connectivity issues
├── JSON serialization problems
└── Constraint violations
```

### Retry and Recovery

The system provides:
- Graceful degradation for non-critical failures
- Health checks for monitoring
- Detailed error messages for debugging

## Monitoring and Statistics

### Built-in Metrics

```java
// Get comprehensive statistics
Map<String, Object> stats = memorySystem.getStorageStatistics();
System.out.println("Total memories: " + stats.get("totalMemories"));
System.out.println("Total operations: " + stats.get("totalOperations"));

// Agent-specific statistics
Map<String, Object> agentStats = memorySystem.getAgentStatistics("agent-001");
```

### Health Checks

```java
// Check system health
boolean healthy = memorySystem.isHealthy();

// Get capacity information
Map<String, Object> capacity = memorySystem.getCapacityInfo();
```

## Best Practices

### 1. Connection Management

- Use connection pooling (HikariCP recommended)
- Configure appropriate pool sizes for your load
- Monitor connection usage and leaks

### 2. Transaction Boundaries

- Keep transactions as short as possible
- Batch related operations when feasible
- Handle exceptions properly to avoid resource leaks

### 3. Embedding Generation

- Use efficient embedding models
- Consider caching embeddings for duplicate content
- Implement retry logic for external embedding services

### 4. Data Lifecycle

- Implement regular cleanup of old memories
- Use relevance scores for memory prioritization
- Consider archiving strategies for historical data

### 5. Testing

- Use H2 for unit tests (fast, in-memory)
- Use test containers for integration tests
- Test transaction rollback scenarios

## Migration and Deployment

### Schema Evolution

The system uses Hibernate's `hibernate.hbm2ddl.auto=update` for development. For production:

1. Generate migration scripts
2. Test on staging environment
3. Apply during maintenance windows
4. Backup before major changes

### Production Deployment

1. **Database Setup**: Configure production database with appropriate resources
2. **Connection Pooling**: Tune HikariCP settings for your load
3. **Monitoring**: Set up application and database monitoring
4. **Backup Strategy**: Implement regular backups and recovery procedures

## Troubleshooting

### Common Issues

1. **JSON Serialization Errors**
   - Ensure DTOs use mutable collections
   - Check Jackson configuration
   - Validate object graph cycles

2. **Performance Issues**
   - Enable query logging for analysis
   - Check index usage
   - Monitor connection pool metrics

3. **Transaction Timeouts**
   - Reduce transaction scope
   - Optimize long-running queries
   - Check database locks

### Debug Configuration

```xml
<!-- Enable SQL logging -->
<property name="hibernate.show_sql" value="true"/>
<property name="hibernate.format_sql" value="true"/>
<property name="hibernate.generate_statistics" value="true"/>
```

## Integration with Existing Systems

### Factory Pattern

Use the factory pattern for easy integration:

```java
// Memory system factory
public class MemorySystemFactory {
    public static MemoryEncodingSystem createJpaSystem(String persistenceUnit) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
        return new JpaMemoryEncodingSystem(emf);
    }
}
```

### Dependency Injection

Compatible with CDI, Spring, and other DI frameworks:

```java
@ApplicationScoped
public class MemorySystemProducer {
    
    @Produces
    @ApplicationScoped
    public MemoryEncodingSystem createMemorySystem() {
        return new JpaMemoryEncodingSystem(entityManagerFactory);
    }
}
```

## Future Enhancements

### Planned Features

1. **Native Vector Search**: Integration with database vector extensions
2. **Distributed Caching**: Redis/Hazelcast integration
3. **Async Operations**: Non-blocking I/O for high-throughput scenarios
4. **Schema Versioning**: Automatic migration support
5. **Multi-tenancy**: Tenant isolation strategies

### Extension Points

The abstract base class design makes it easy to:
- Add new persistence backends
- Implement custom similarity algorithms
- Integrate with external vector databases
- Add specialized indexing strategies

---

This implementation provides a robust, scalable foundation for persistent memory storage in the Headkey system while maintaining clean separation of concerns and extensibility for future requirements.