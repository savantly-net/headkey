# Belief Reinforcement Conflict Analyzer - Architecture Guide

## Overview

This document describes the refactored architecture of the Belief Reinforcement & Conflict Analyzer (BRCA) system. The new design separates business logic from persistence and content analysis concerns, enabling pluggable implementations and future AI integration.

## Architecture Principles

The refactored system follows these key principles:

- **Separation of Concerns**: Business logic, persistence, and content analysis are separated
- **SOLID Principles**: Single responsibility, open/closed, dependency inversion
- **Strategy Pattern**: Pluggable algorithms for extraction and storage
- **Template Method**: Common business logic with customizable components
- **Dependency Injection**: Services are injected rather than hard-coded

## Component Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Code                              │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│            BeliefReinforcementConflictAnalyzer              │
│                      (Interface)                           │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│       AbstractBeliefReinforcementConflictAnalyzer          │
│                  (Business Logic)                          │
│  ┌─────────────────┬───────────────────┬─────────────────┐  │
│  │                 │                   │                 │  │
│  ▼                 ▼                   ▼                 │  │
│ analyzeNewMemory() │ reviewAllBeliefs() │ resolveConflict() │  │
│ analyzeBatch()     │ findRelatedBeliefs() │ getStatistics() │  │
└─────────────────────┬───────────────────┬─────────────────┘
                      │                   │
        ┌─────────────▼─────────────┐   ┌▼─────────────────────┐
        │  BeliefExtractionService  │   │  BeliefStorageService │
        │       (Interface)         │   │      (Interface)      │
        └─────────────┬─────────────┘   └┬─────────────────────┘
                      │                   │
         ┌────────────▼────────────┐    ┌▼──────────────────────┐
         │ SimplePatternBelief...  │    │ InMemoryBeliefStorage │
         │ LangChain4JBelief...    │    │ JpaBeliefStorage      │
         │ (Content Analysis)      │    │ RedisBeliefStorage    │
         └─────────────────────────┘    └───────────────────────┘
```

## Core Components

### 1. Abstract Base Class

**`AbstractBeliefReinforcementConflictAnalyzer`**
- Contains all business logic for belief management
- Implements the template method pattern
- Delegates to service provider interfaces
- Ensures consistent behavior across implementations

**Key responsibilities:**
- Memory analysis workflow
- Conflict detection and resolution
- Belief statistics and reporting
- Batch processing optimization

### 2. Service Provider Interfaces

**`BeliefExtractionService`**
- Responsible for content analysis and belief extraction
- Supports different extraction strategies (patterns, NLP, AI)
- Provides similarity calculation and conflict detection
- Enables confidence scoring and categorization

**`BeliefStorageService`**
- Handles persistence and retrieval of beliefs and conflicts
- Supports different storage backends (memory, database, cache)
- Provides query capabilities and indexing
- Manages data integrity and consistency

### 3. Concrete Implementations

**Current Implementations:**
- `SimplePatternBeliefExtractionService` - Pattern-based extraction
- `InMemoryBeliefStorageService` - Memory-based storage
- `InMemoryBeliefReinforcementConflictAnalyzer` - Production implementation

**Future Implementations:**
- `LangChain4JBeliefExtractionService` - AI-powered extraction
- `JpaBeliefStorageService` - Database persistence
- `VectorBeliefStorageService` - Vector database for semantic search

## Factory Pattern

The `BeliefReinforcementConflictAnalyzerFactory` provides convenient creation methods:

```java
// Simple in-memory implementation
BeliefReinforcementConflictAnalyzer analyzer = 
    BeliefReinforcementConflictAnalyzerFactory.createInMemory();

// Custom service configuration
BeliefReinforcementConflictAnalyzer analyzer = 
    BeliefReinforcementConflictAnalyzerFactory.builder()
        .withExtractionService(new LangChain4JBeliefExtractionService())
        .withStorageService(new JpaBeliefStorageService())
        .withResolutionStrategy("preference", "newer_wins")
        .build();

// Pre-configured setups
BeliefReinforcementConflictAnalyzer devAnalyzer = 
    BeliefReinforcementConflictAnalyzerFactory.createForDevelopment();
BeliefReinforcementConflictAnalyzer prodAnalyzer = 
    BeliefReinforcementConflictAnalyzerFactory.createForProduction();
```

## Configuration Types

| Configuration | Description | Extraction Service | Storage Service | Use Case |
|---------------|-------------|-------------------|-----------------|----------|
| Development | Fast, simple | SimplePattern | InMemory | Development, Testing |
| Testing | Predictable | SimplePattern | InMemory | Unit Tests |
| Production | Robust | SimplePattern* | InMemory* | Production (current) |
| AI-Powered | Intelligent | LangChain4J** | Vector** | Future AI integration |

*Will be upgraded to persistent storage in production
**Not yet implemented

## Service Provider Interface Details

### BeliefExtractionService

```java
public interface BeliefExtractionService {
    List<ExtractedBelief> extractBeliefs(String content, String agentId, CategoryLabel category);
    double calculateSimilarity(String statement1, String statement2);
    boolean areConflicting(String statement1, String statement2, String category1, String category2);
    String extractCategory(String statement);
    double calculateConfidence(String content, String statement, ExtractionContext context);
    boolean isHealthy();
    Map<String, Object> getServiceInfo();
}
```

### BeliefStorageService

```java
public interface BeliefStorageService {
    // CRUD Operations
    Belief storeBelief(Belief belief);
    Optional<Belief> getBeliefById(String beliefId);
    boolean deleteBelief(String beliefId);
    
    // Query Operations
    List<Belief> getBeliefsForAgent(String agentId, boolean includeInactive);
    List<Belief> searchBeliefs(String searchText, String agentId, int limit);
    List<SimilarBelief> findSimilarBeliefs(String statement, String agentId, double threshold, int limit);
    
    // Conflict Management
    BeliefConflict storeConflict(BeliefConflict conflict);
    List<BeliefConflict> getUnresolvedConflicts(String agentId);
    
    // Statistics and Monitoring
    Map<String, Object> getStorageStatistics();
    boolean isHealthy();
}
```

## Implementation Guidelines

### Creating Custom Extraction Services

```java
public class MyCustomExtractionService implements BeliefExtractionService {
    @Override
    public List<ExtractedBelief> extractBeliefs(String content, String agentId, CategoryLabel category) {
        // Your custom extraction logic
        // Could use NLP libraries, ML models, or rule engines
        return extractedBeliefs;
    }
    
    @Override
    public double calculateSimilarity(String statement1, String statement2) {
        // Your similarity algorithm
        // Could use embeddings, edit distance, or semantic analysis
        return similarity;
    }
    
    // Implement other methods...
}
```

### Creating Custom Storage Services

```java
public class MyDatabaseStorageService implements BeliefStorageService {
    private final EntityManager entityManager;
    
    @Override
    public Belief storeBelief(Belief belief) {
        // Your persistence logic
        // Could use JPA, MongoDB, Redis, etc.
        return entityManager.merge(belief);
    }
    
    // Implement other methods...
}
```

## Migration from Original Implementation

### Before (Monolithic)
```java
// Everything was coupled together
InMemoryBeliefReinforcementConflictAnalyzer analyzer = 
    new InMemoryBeliefReinforcementConflictAnalyzer();
```

### After (Modular)
```java
// Services are separated and configurable
BeliefExtractionService extractor = new SimplePatternBeliefExtractionService();
BeliefStorageService storage = new InMemoryBeliefStorageService();
BeliefReinforcementConflictAnalyzer analyzer = 
    new InMemoryBeliefReinforcementConflictAnalyzer(extractor, storage);

// Or use factory for convenience
BeliefReinforcementConflictAnalyzer analyzer = 
    BeliefReinforcementConflictAnalyzerFactory.createInMemory();
```

## Testing Strategies

### Unit Testing with Mocks
```java
@Test
public void testBeliefAnalysis() {
    BeliefExtractionService mockExtractor = mock(BeliefExtractionService.class);
    BeliefStorageService mockStorage = mock(BeliefStorageService.class);
    
    BeliefReinforcementConflictAnalyzer analyzer = 
        BeliefReinforcementConflictAnalyzerFactory.create(mockExtractor, mockStorage);
    
    // Test business logic in isolation
}
```

### Integration Testing
```java
@Test
public void testEndToEndAnalysis() {
    BeliefReinforcementConflictAnalyzer analyzer = 
        BeliefReinforcementConflictAnalyzerFactory.createForTesting();
    
    // Test complete workflow with real implementations
}
```

## Performance Considerations

### Extraction Service Performance
- Pattern-based: Very fast, low CPU usage
- NLP-based: Moderate speed, higher CPU usage
- AI-based: Slower, requires network calls, higher cost

### Storage Service Performance
- In-memory: Fastest, limited by RAM
- Database: Good for large datasets, persistent
- Vector DB: Best for semantic search, specialized

### Optimization Strategies
- Caching frequently accessed beliefs
- Batch processing for multiple memories
- Async processing for non-critical operations
- Connection pooling for database services

## Future Enhancements

### AI Integration Roadmap
1. **Phase 1**: LangChain4J integration for extraction
2. **Phase 2**: Vector database for semantic storage
3. **Phase 3**: Advanced reasoning for conflict resolution
4. **Phase 4**: Learning and adaptation capabilities

### Planned Implementations
- `LangChain4JBeliefExtractionService` - Using GPT/Claude for extraction
- `VectorBeliefStorageService` - Semantic similarity search
- `JpaBeliefStorageService` - PostgreSQL persistence
- `RedisBeliefStorageService` - High-performance caching

## Best Practices

### Service Implementation
- Always implement health checks
- Provide detailed service information
- Handle errors gracefully with fallbacks
- Log important operations for debugging

### Configuration Management
- Use factory methods for common configurations
- Support environment-specific settings
- Validate service health before use
- Document configuration requirements

### Error Handling
- Use specific exception types
- Provide meaningful error messages
- Implement retry logic for transient failures
- Fall back to simpler implementations when possible

## Examples

See `BeliefAnalyzerExample.java` for comprehensive usage examples including:
- Simple factory usage
- Custom service configuration
- Different pre-built configurations
- Service provider pattern benefits
- Future AI integration approach

## Troubleshooting

### Common Issues

**Service Not Healthy**
- Check service dependencies (database connections, API keys)
- Verify configuration parameters
- Review service logs for specific errors

**Poor Extraction Quality**
- Try different extraction services
- Adjust confidence thresholds
- Review and improve pattern matching rules

**Performance Issues**
- Enable caching in storage services
- Use batch processing for multiple memories
- Consider async processing for heavy operations

### Debug Configuration
```java
BeliefReinforcementConflictAnalyzer analyzer = 
    BeliefReinforcementConflictAnalyzerFactory.builder()
        .withConfig("verboseLogging", true)
        .withConfig("debugMode", true)
        .build();
```

## Contributing

When adding new service implementations:
1. Implement the appropriate service interface
2. Add comprehensive unit tests
3. Include health check functionality
4. Document configuration requirements
5. Add factory methods for common configurations
6. Update this README with usage examples

For questions or contributions, see the main project documentation.