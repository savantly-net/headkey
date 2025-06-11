package ai.headkey.memory.interfaces;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefRelationship;
import ai.headkey.memory.dto.BeliefKnowledgeGraph;
import ai.headkey.memory.enums.RelationshipType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Service interface for managing relationships between beliefs in the knowledge graph.
 * 
 * This interface provides comprehensive functionality for creating, querying, and managing
 * relationships between beliefs, enabling rich knowledge graph operations including
 * temporal deprecation, semantic connections, and graph traversal capabilities.
 * 
 * @since 1.0
 */
public interface BeliefRelationshipService {
    
    /**
     * Creates a new relationship between two beliefs.
     * 
     * @param sourceBeliefId The ID of the source belief
     * @param targetBeliefId The ID of the target belief
     * @param relationshipType The type of relationship
     * @param strength The strength of the relationship (0.0 to 1.0)
     * @param agentId The agent ID
     * @return The created relationship
     * @throws IllegalArgumentException if beliefs don't exist or parameters are invalid
     */
    BeliefRelationship createRelationship(String sourceBeliefId, String targetBeliefId, 
                                        RelationshipType relationshipType, double strength, String agentId);
    
    /**
     * Creates a new relationship with metadata.
     * 
     * @param sourceBeliefId The ID of the source belief
     * @param targetBeliefId The ID of the target belief
     * @param relationshipType The type of relationship
     * @param strength The strength of the relationship
     * @param agentId The agent ID
     * @param metadata Additional metadata for the relationship
     * @return The created relationship
     */
    BeliefRelationship createRelationshipWithMetadata(String sourceBeliefId, String targetBeliefId, 
                                                     RelationshipType relationshipType, double strength, 
                                                     String agentId, Map<String, Object> metadata);
    
    /**
     * Creates a temporal relationship with effective period.
     * 
     * @param sourceBeliefId The ID of the source belief
     * @param targetBeliefId The ID of the target belief
     * @param relationshipType The type of relationship
     * @param strength The strength of the relationship
     * @param agentId The agent ID
     * @param effectiveFrom When the relationship becomes effective
     * @param effectiveUntil When the relationship expires
     * @return The created relationship
     */
    BeliefRelationship createTemporalRelationship(String sourceBeliefId, String targetBeliefId, 
                                                 RelationshipType relationshipType, double strength, 
                                                 String agentId, Instant effectiveFrom, Instant effectiveUntil);
    
    /**
     * Creates a temporal deprecation relationship where a new belief supersedes an old one.
     * This is a specialized method for the common use case of belief evolution.
     * 
     * @param oldBeliefId The ID of the belief being deprecated
     * @param newBeliefId The ID of the new belief that supersedes the old one
     * @param reason The reason for deprecation
     * @param agentId The agent ID
     * @return The created supersession relationship
     */
    BeliefRelationship deprecateBeliefWith(String oldBeliefId, String newBeliefId, 
                                         String reason, String agentId);
    
    /**
     * Updates an existing relationship.
     * 
     * @param relationshipId The relationship ID
     * @param strength The new strength value
     * @param metadata Updated metadata
     * @return The updated relationship
     * @throws IllegalArgumentException if relationship doesn't exist
     */
    BeliefRelationship updateRelationship(String relationshipId, double strength, 
                                         Map<String, Object> metadata);
    
    /**
     * Deactivates a relationship.
     * 
     * @param relationshipId The relationship ID
     * @param agentId The agent ID
     * @return true if the relationship was deactivated
     */
    boolean deactivateRelationship(String relationshipId, String agentId);
    
    /**
     * Reactivates a relationship.
     * 
     * @param relationshipId The relationship ID
     * @param agentId The agent ID
     * @return true if the relationship was reactivated
     */
    boolean reactivateRelationship(String relationshipId, String agentId);
    
    /**
     * Deletes a relationship permanently.
     * 
     * @param relationshipId The relationship ID
     * @param agentId The agent ID
     * @return true if the relationship was deleted
     */
    boolean deleteRelationship(String relationshipId, String agentId);
    
    /**
     * Finds a relationship by ID.
     * 
     * @param relationshipId The relationship ID
     * @param agentId The agent ID
     * @return The relationship if found
     */
    Optional<BeliefRelationship> findRelationshipById(String relationshipId, String agentId);
    
    /**
     * Finds all relationships for a belief (both incoming and outgoing).
     * 
     * @param beliefId The belief ID
     * @param agentId The agent ID
     * @return List of relationships connected to the belief
     */
    List<BeliefRelationship> findRelationshipsForBelief(String beliefId, String agentId);
    
    /**
     * Finds outgoing relationships from a belief.
     * 
     * @param beliefId The source belief ID
     * @param agentId The agent ID
     * @return List of outgoing relationships
     */
    List<BeliefRelationship> findOutgoingRelationships(String beliefId, String agentId);
    
    /**
     * Finds incoming relationships to a belief.
     * 
     * @param beliefId The target belief ID
     * @param agentId The agent ID
     * @return List of incoming relationships
     */
    List<BeliefRelationship> findIncomingRelationships(String beliefId, String agentId);
    
    /**
     * Finds relationships by type.
     * 
     * @param relationshipType The relationship type to filter by
     * @param agentId The agent ID
     * @return List of relationships of the specified type
     */
    List<BeliefRelationship> findRelationshipsByType(RelationshipType relationshipType, String agentId);
    
    /**
     * Finds relationships between two specific beliefs.
     * 
     * @param sourceBeliefId The source belief ID
     * @param targetBeliefId The target belief ID
     * @param agentId The agent ID
     * @return List of relationships between the beliefs
     */
    List<BeliefRelationship> findRelationshipsBetween(String sourceBeliefId, String targetBeliefId, String agentId);
    
    /**
     * Finds beliefs that are deprecated by temporal relationships.
     * 
     * @param agentId The agent ID
     * @return List of deprecated belief IDs
     */
    List<String> findDeprecatedBeliefs(String agentId);
    
    /**
     * Finds beliefs that supersede a given belief.
     * 
     * @param beliefId The deprecated belief ID
     * @param agentId The agent ID
     * @return List of beliefs that supersede the given belief
     */
    List<Belief> findSupersedingBeliefs(String beliefId, String agentId);
    
    /**
     * Finds the complete deprecation chain for a belief.
     * 
     * @param beliefId The belief ID
     * @param agentId The agent ID
     * @return List of beliefs in the deprecation chain
     */
    List<Belief> findDeprecationChain(String beliefId, String agentId);
    
    /**
     * Finds beliefs that are related to a given belief within a certain depth.
     * 
     * @param beliefId The belief ID
     * @param agentId The agent ID
     * @param maxDepth Maximum traversal depth
     * @return Set of related belief IDs
     */
    Set<String> findRelatedBeliefs(String beliefId, String agentId, int maxDepth);
    
    /**
     * Finds beliefs that are semantically similar based on relationship patterns.
     * 
     * @param beliefId The belief ID
     * @param agentId The agent ID
     * @param similarityThreshold Minimum similarity threshold
     * @return List of similar beliefs with similarity scores
     */
    List<Map<String, Object>> findSimilarBeliefs(String beliefId, String agentId, double similarityThreshold);
    
    /**
     * Gets the complete knowledge graph for an agent.
     * 
     * @deprecated For large graphs, use createSnapshotGraph() or BeliefGraphQueryService methods instead.
     * This method loads the complete graph into memory which can be inefficient.
     * 
     * @param agentId The agent ID
     * @return The belief knowledge graph
     */
    @Deprecated
    BeliefKnowledgeGraph getKnowledgeGraph(String agentId);

    /**
     * Gets a filtered knowledge graph containing only active relationships.
     * 
     * @deprecated For large graphs, use createSnapshotGraph(agentId, false) or BeliefGraphQueryService methods instead.
     * This method loads the complete active graph into memory which can be inefficient.
     * 
     * @param agentId The agent ID
     * @return The filtered knowledge graph
     */
    @Deprecated
    BeliefKnowledgeGraph getActiveKnowledgeGraph(String agentId);
    
    /**
     * Gets knowledge graph statistics for an agent.
     * 
     * @deprecated Use BeliefGraphQueryService.getComprehensiveGraphStatistics() for efficient database-level statistics.
     * This method loads the complete graph into memory which is inefficient for large datasets.
     * 
     * @param agentId The agent ID
     * @return Map containing graph statistics
     */
    @Deprecated
    Map<String, Object> getKnowledgeGraphStatistics(String agentId);
    
    /**
     * Validates the knowledge graph structure for consistency.
     * 
     * @deprecated Use BeliefGraphQueryService.validateGraphStructure() for efficient database-level validation.
     * This method loads the complete graph into memory which is inefficient for large datasets.
     * 
     * @param agentId The agent ID
     * @return List of validation issues
     */
    @Deprecated
    List<String> validateKnowledgeGraph(String agentId);
    
    /**
     * Finds strongly connected belief clusters.
     * 
     * @param agentId The agent ID
     * @param strengthThreshold Minimum relationship strength threshold
     * @return Map of cluster names to belief sets
     */
    Map<String, Set<String>> findBeliefClusters(String agentId, double strengthThreshold);
    
    /**
     * Finds potential conflicts between beliefs based on contradictory relationships.
     * 
     * @param agentId The agent ID
     * @return List of potential belief conflicts
     */
    List<Map<String, Object>> findPotentialConflicts(String agentId);

    // ========================================
    // EFFICIENT GRAPH OPERATIONS
    // ========================================

    /**
     * Gets comprehensive graph statistics using efficient database operations.
     * This replaces the deprecated getKnowledgeGraphStatistics method.
     * 
     * @param agentId The agent ID
     * @return Map containing comprehensive graph statistics
     */
    Map<String, Object> getEfficientGraphStatistics(String agentId);

    /**
     * Validates graph structure using efficient database queries.
     * This replaces the deprecated validateKnowledgeGraph method.
     * 
     * @param agentId The agent ID
     * @return List of validation issues
     */
    List<String> performEfficientGraphValidation(String agentId);

    /**
     * Creates a lightweight snapshot graph for small datasets.
     * Recommended for graphs with < 1000 beliefs.
     * 
     * @param agentId The agent ID
     * @param includeInactive Whether to include inactive beliefs/relationships
     * @return Lightweight BeliefKnowledgeGraph snapshot
     */
    BeliefKnowledgeGraph createSnapshotGraph(String agentId, boolean includeInactive);

    /**
     * Creates a filtered snapshot graph based on specific criteria.
     * 
     * @param agentId The agent ID
     * @param beliefIds Specific belief IDs to include (null = all)
     * @param relationshipTypes Relationship types to include (null = all)
     * @param maxBeliefs Maximum number of beliefs to include
     * @return Filtered BeliefKnowledgeGraph snapshot
     */
    BeliefKnowledgeGraph createFilteredSnapshot(String agentId, 
                                              Set<String> beliefIds,
                                              Set<RelationshipType> relationshipTypes,
                                              int maxBeliefs);

    /**
     * Creates a graph snapshot optimized for export operations.
     * 
     * @param agentId The agent ID
     * @param format Export format ("json", "dot", etc.)
     * @return BeliefKnowledgeGraph optimized for export
     */
    BeliefKnowledgeGraph createExportGraph(String agentId, String format);

    /**
     * Finds the shortest path between two beliefs in the knowledge graph.
     * 
     * @param sourceBeliefId The source belief ID
     * @param targetBeliefId The target belief ID
     * @param agentId The agent ID
     * @return List of belief relationships forming the shortest path
     */
    List<BeliefRelationship> findShortestPath(String sourceBeliefId, String targetBeliefId, String agentId);
    
    /**
     * Suggests new relationships based on existing patterns and belief content.
     * 
     * @param agentId The agent ID
     * @param maxSuggestions Maximum number of suggestions to return
     * @return List of suggested relationships
     */
    List<Map<String, Object>> suggestRelationships(String agentId, int maxSuggestions);
    
    /**
     * Bulk creates multiple relationships efficiently.
     * 
     * @param relationships List of relationships to create
     * @param agentId The agent ID
     * @return List of created relationships
     */
    List<BeliefRelationship> createRelationshipsBulk(List<BeliefRelationship> relationships, String agentId);
    
    /**
     * Exports the knowledge graph in a specific format.
     * 
     * @param agentId The agent ID
     * @param format The export format (e.g., "json", "graphml", "dot")
     * @return The exported graph data
     */
    String exportKnowledgeGraph(String agentId, String format);
    
    /**
     * Imports relationships from external data.
     * 
     * @param data The relationship data to import
     * @param format The data format
     * @param agentId The agent ID
     * @return Number of relationships imported
     */
    int importRelationships(String data, String format, String agentId);
    
    /**
     * Performs cleanup operations on the knowledge graph.
     * This includes removing orphaned relationships, inactive relationships older than a threshold, etc.
     * 
     * @param agentId The agent ID
     * @param olderThanDays Remove inactive relationships older than this many days
     * @return Number of relationships cleaned up
     */
    int cleanupKnowledgeGraph(String agentId, int olderThanDays);
    
    /**
     * Gets service health information.
     * 
     * @return Map containing health metrics
     */
    Map<String, Object> getHealthInfo();
}