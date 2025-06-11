# Belief Relationships Implementation Guide

## Overview

This document provides a comprehensive guide to the Belief Relationships feature implemented in the HeadKey Memory System. This feature enables beliefs to function as nodes in a knowledge graph with rich, typed relationships between them, including temporal deprecation capabilities.

## 🎯 Core Features Implemented

### 1. Rich Relationship Types
- **Temporal**: `SUPERSEDES`, `UPDATES`, `DEPRECATES`, `REPLACES`
- **Logical**: `SUPPORTS`, `CONTRADICTS`, `IMPLIES`, `REINFORCES`, `WEAKENS`
- **Semantic**: `RELATES_TO`, `SPECIALIZES`, `GENERALIZES`, `EXTENDS`, `DERIVES_FROM`
- **Causal**: `CAUSES`, `CAUSED_BY`, `ENABLES`, `PREVENTS`
- **Contextual**: `DEPENDS_ON`, `PRECEDES`, `FOLLOWS`, `CONTEXT_FOR`
- **Evidence**: `EVIDENCED_BY`, `PROVIDES_EVIDENCE_FOR`, `CONFLICTS_WITH`
- **Similarity**: `SIMILAR_TO`, `ANALOGOUS_TO`, `CONTRASTS_WITH`
- **Custom**: `CUSTOM` for extensibility

### 2. Temporal Relationship Support
- Effective from/until timestamps for time-bound relationships
- Automatic deprecation handling with reasons
- Evolution chains for belief updates
- Temporal validity checks

### 3. Knowledge Graph Operations
- Graph traversal with configurable depth
- Shortest path finding between beliefs
- Strongly connected component detection
- Similarity analysis based on relationship patterns
- Conflict detection and reporting

### 4. Advanced Analytics
- Graph statistics and metrics
- Belief clustering by relationship strength
- Deprecation chain analysis
- Validation and health checks

## 🏗️ Architecture Components

### Core Data Models

#### BeliefRelationship DTO
```java
public class BeliefRelationship {
    private String id;
    private String sourceBeliefId;
    private String targetBeliefId;
    private String agentId;
    private RelationshipType relationshipType;
    private double strength; // 0.0 to 1.0
    private Map<String, Object> metadata;
    private Instant effectiveFrom;
    private Instant effectiveUntil;
    private String deprecationReason;
    // ... additional fields and methods
}
```

#### BeliefKnowledgeGraph DTO
```java
public class BeliefKnowledgeGraph {
    private String agentId;
    private Map<String, Belief> beliefs;
    private Map<String, BeliefRelationship> relationships;
    private Map<String, List<BeliefRelationship>> outgoingRelationships;
    private Map<String, List<BeliefRelationship>> incomingRelationships;
    // ... graph operations and analytics methods
}
```

### Service Layer

#### BeliefRelationshipService Interface
Core service interface providing all relationship management capabilities:

```java
public interface BeliefRelationshipService {
    // Relationship CRUD operations
    BeliefRelationship createRelationship(String sourceId, String targetId, RelationshipType type, double strength, String agentId);
    BeliefRelationship deprecateBeliefWith(String oldId, String newId, String reason, String agentId);
    
    // Query operations
    List<BeliefRelationship> findRelationshipsForBelief(String beliefId, String agentId);
    List<String> findDeprecatedBeliefs(String agentId);
    Set<String> findRelatedBeliefs(String beliefId, String agentId, int maxDepth);
    
    // Graph operations
    BeliefKnowledgeGraph getKnowledgeGraph(String agentId);
    List<BeliefRelationship> findShortestPath(String sourceId, String targetId, String agentId);
    Map<String, Set<String>> findBeliefClusters(String agentId, double strengthThreshold);
    
    // Analytics and maintenance
    Map<String, Object> getKnowledgeGraphStatistics(String agentId);
    List<String> validateKnowledgeGraph(String agentId);
    int cleanupKnowledgeGraph(String agentId, int olderThanDays);
}
```

### Persistence Layer

#### Database Schema
- **belief_relationships**: Main relationship table with comprehensive indexing
- **belief_relationship_metadata**: Flexible key-value metadata storage
- Optimized indexes for graph traversal and temporal queries
- Database functions for complex graph operations

#### JPA Entity
```java
@Entity
@Table(name = "belief_relationships")
public class BeliefRelationshipEntity {
    @Id
    private String id;
    
    @Column(name = "source_belief_id")
    private String sourceBeliefId;
    
    @Column(name = "target_belief_id")
    private String targetBeliefId;
    
    @Enumerated(EnumType.STRING)
    private RelationshipType relationshipType;
    
    // Temporal fields
    private Instant effectiveFrom;
    private Instant effectiveUntil;
    private String deprecationReason;
    
    // Metadata as element collection
    @ElementCollection
    private Map<String, String> metadata;
    
    // Standard audit fields...
}
```

### REST API Layer

#### BeliefRelationshipResource
Comprehensive REST endpoints for all relationship operations:

```
POST   /api/v1/agents/{agentId}/belief-relationships           - Create relationship
POST   /api/v1/agents/{agentId}/belief-relationships/temporal  - Create temporal relationship
POST   /api/v1/agents/{agentId}/belief-relationships/deprecate - Deprecate belief
GET    /api/v1/agents/{agentId}/belief-relationships/{id}      - Get relationship
PUT    /api/v1/agents/{agentId}/belief-relationships/{id}      - Update relationship
DELETE /api/v1/agents/{agentId}/belief-relationships/{id}      - Delete relationship

# Query endpoints
GET    /api/v1/agents/{agentId}/belief-relationships/belief/{beliefId}          - All relationships for belief
GET    /api/v1/agents/{agentId}/belief-relationships/type/{relationshipType}    - Relationships by type
GET    /api/v1/agents/{agentId}/belief-relationships/deprecated                 - Deprecated beliefs

# Graph operations
GET    /api/v1/agents/{agentId}/belief-relationships/knowledge-graph           - Full knowledge graph
GET    /api/v1/agents/{agentId}/belief-relationships/statistics                - Graph statistics
GET    /api/v1/agents/{agentId}/belief-relationships/clusters                  - Belief clusters
GET    /api/v1/agents/{agentId}/belief-relationships/conflicts                 - Potential conflicts
GET    /api/v1/agents/{agentId}/belief-relationships/path/{sourceId}/{targetId} - Shortest path

# Maintenance
GET    /api/v1/agents/{agentId}/belief-relationships/validate                  - Validate graph
DELETE /api/v1/agents/{agentId}/belief-relationships/cleanup                  - Cleanup old relationships
GET    /api/v1/agents/{agentId}/belief-relationships/export                   - Export graph
```

## 🚀 Usage Examples

### Basic Relationship Creation
```java
// Create supporting relationship
BeliefRelationship support = relationshipService.createRelationship(
    "belief-001", "belief-002", RelationshipType.SUPPORTS, 0.85, "agent-001"
);

// Create relationship with metadata
Map<String, Object> metadata = Map.of(
    "source", "user_conversation",
    "confidence", 0.92,
    "evidence_count", 3
);
BeliefRelationship rich = relationshipService.createRelationshipWithMetadata(
    "belief-003", "belief-004", RelationshipType.RELATES_TO, 0.8, "agent-001", metadata
);
```

### Temporal Deprecation
```java
// Deprecate old belief with new one
BeliefRelationship deprecation = relationshipService.deprecateBeliefWith(
    "old-belief-123", 
    "new-belief-456", 
    "Updated information from recent conversation", 
    "agent-001"
);

// Create temporal relationship with effective period
BeliefRelationship temporal = relationshipService.createTemporalRelationship(
    "belief-new", "belief-old", RelationshipType.UPDATES, 1.0, "agent-001",
    Instant.now(), Instant.now().plusSeconds(86400) // Valid for 24 hours
);
```

### Graph Traversal and Analysis
```java
// Find all relationships for a belief
List<BeliefRelationship> allRels = relationshipService.findRelationshipsForBelief("belief-001", "agent-001");

// Find deprecated beliefs
List<String> deprecated = relationshipService.findDeprecatedBeliefs("agent-001");

// Find related beliefs within depth
Set<String> related = relationshipService.findRelatedBeliefs("belief-001", "agent-001", 3);

// Find shortest path between beliefs
List<BeliefRelationship> path = relationshipService.findShortestPath("belief-001", "belief-010", "agent-001");

// Get complete knowledge graph
BeliefKnowledgeGraph graph = relationshipService.getKnowledgeGraph("agent-001");

// Find belief clusters
Map<String, Set<String>> clusters = relationshipService.findBeliefClusters("agent-001", 0.8);
```

### Analytics and Maintenance
```java
// Get graph statistics
Map<String, Object> stats = relationshipService.getKnowledgeGraphStatistics("agent-001");

// Validate graph structure
List<String> issues = relationshipService.validateKnowledgeGraph("agent-001");

// Find potential conflicts
List<Map<String, Object>> conflicts = relationshipService.findPotentialConflicts("agent-001");

// Cleanup old inactive relationships
int cleanedUp = relationshipService.cleanupKnowledgeGraph("agent-001", 365);
```

## 🔧 Configuration and Setup

### Database Setup
1. Run the belief relationships schema script:
   ```sql
   -- Execute belief-relationships-schema.sql
   ```

2. For testing, use the updated test schema that includes relationship tables.

### Service Configuration
The system provides multiple implementation strategies:

#### In-Memory Implementation (Development/Testing)
```java
BeliefRelationshipService service = new InMemoryBeliefRelationshipService();
```

#### JPA Implementation (Production)
```java
// Configure with CDI injection
@Inject
BeliefRelationshipService relationshipService;
```

### REST API Integration
Add the `BeliefRelationshipResource` to your Quarkus application and ensure proper CDI configuration.

## 📊 Performance Considerations

### Database Optimization
- Comprehensive indexing strategy for graph traversal queries
- Specialized indexes for temporal relationship queries
- Unique constraints to prevent duplicate relationships
- Optimized foreign key relationships

### Query Performance
- Efficient adjacency list management
- Depth-limited graph traversal to prevent infinite loops
- Cached relationship lookups in memory implementation
- Batch operations for bulk relationship creation

### Scalability
- Agent-scoped operations to enable horizontal scaling
- Configurable depth limits for graph operations
- Efficient cleanup mechanisms for old data
- Export/import capabilities for data migration

## 🧪 Testing

### Unit Tests
Comprehensive test suite covering:
- Basic relationship CRUD operations
- Temporal relationship functionality
- Graph traversal algorithms
- Analytics and statistics
- Error handling and edge cases

### Integration Tests
- Database persistence and retrieval
- REST API endpoint testing
- Cross-module integration validation

### Example Test
```java
@Test
void testTemporalDeprecation() {
    BeliefRelationship deprecation = service.deprecateBeliefWith(
        "old-belief", "new-belief", "Updated preference", "agent-001"
    );
    
    assertEquals(RelationshipType.SUPERSEDES, deprecation.getRelationshipType());
    assertTrue(deprecation.isDeprecating());
    assertTrue(deprecation.isCurrentlyEffective());
    
    List<String> deprecated = service.findDeprecatedBeliefs("agent-001");
    assertTrue(deprecated.contains("old-belief"));
}
```

## 🔍 Monitoring and Observability

### Health Checks
```java
Map<String, Object> health = relationshipService.getHealthInfo();
// Returns status, implementation details, and metrics
```

### Statistics and Metrics
- Total relationships and beliefs
- Active vs. inactive relationship counts
- Relationship type distribution
- Average relationship strength
- Deprecation statistics

### Graph Validation
```java
List<String> issues = relationshipService.validateKnowledgeGraph("agent-001");
// Returns validation issues like orphaned relationships, cycles, etc.
```

## 🚦 Best Practices

### Relationship Design
1. Use appropriate relationship types for semantic clarity
2. Set meaningful strength values (0.0-1.0)
3. Include rich metadata for context and provenance
4. Use temporal relationships for belief evolution

### Performance
1. Limit graph traversal depth to reasonable values (typically 3-5)
2. Use bulk operations for creating multiple relationships
3. Regular cleanup of inactive relationships
4. Monitor graph size and complexity

### Data Quality
1. Validate relationship parameters before creation
2. Prevent self-referential relationships
3. Use consistent belief ID naming conventions
4. Regular validation of graph structure

## 🔮 Future Enhancements

### Planned Features
1. **Advanced Graph Algorithms**: PageRank, centrality measures, community detection
2. **Machine Learning Integration**: Automatic relationship suggestion, strength prediction
3. **Visualization Support**: Graph rendering and interactive exploration
4. **Conflict Resolution**: Automated conflict detection and resolution strategies
5. **Event Sourcing**: Full audit trail of relationship changes
6. **Graph Snapshots**: Versioned knowledge graph states

### Extension Points
1. **Custom Relationship Types**: Domain-specific relationship extensions
2. **Plugin Architecture**: Pluggable graph analysis algorithms
3. **External Integrations**: Knowledge base imports, ontology mapping
4. **Real-time Updates**: Event-driven relationship updates

## 📚 Related Documentation

- [HeadKey Memory System Overview](NOTES.md)
- [API Documentation](rest/README.md)
- [Database Schema](persistence-postgres/src/main/resources/sql/)
- [Unit Tests](core/src/test/java/ai/headkey/memory/implementations/)
- [Demo Examples](core/src/main/java/ai/headkey/memory/examples/BeliefRelationshipDemo.java)

## 🤝 Contributing

When extending the belief relationship system:

1. Follow existing patterns and naming conventions
2. Add comprehensive unit tests for new functionality
3. Update this documentation for significant changes
4. Consider performance implications of new features
5. Maintain backward compatibility where possible

## 📝 Change Log

### v1.0.0 - Initial Implementation
- Core relationship types and operations
- Temporal deprecation support
- Graph traversal and analytics
- REST API endpoints
- Database schema and persistence
- Comprehensive testing suite
- Documentation and examples

---

*This implementation provides a solid foundation for building sophisticated knowledge graphs with beliefs as nodes and rich relationships as edges, enabling advanced AI agent memory capabilities with temporal awareness and semantic understanding.*