package ai.headkey.memory.interfaces;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefRelationship;
import ai.headkey.memory.enums.RelationshipType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Efficient lazy-loading interface for belief knowledge graph queries.
 * 
 * This interface is designed to avoid loading complete knowledge graphs into memory,
 * instead providing targeted query methods that fetch only the required data.
 * All methods are designed to be efficient and work with large-scale belief graphs.
 * 
 * Key principles:
 * - Lazy evaluation: Only fetch data when needed
 * - Streaming: Support for processing large result sets
 * - Targeted queries: Specific methods for common use cases
 * - Pagination: Built-in support for large result sets
 * - Caching hints: Methods indicate cache-friendly operations
 * 
 * @since 1.0
 */
public interface BeliefGraphQueryService {
    
    // ========================================
    // EFFICIENT GRAPH STATISTICS
    // ========================================
    
    /**
     * Gets lightweight graph statistics without loading full graph.
     * This method should execute efficiently using database aggregations.
     * 
     * @param agentId The agent ID
     * @return Map containing statistical information
     */
    Map<String, Long> getGraphStatistics(String agentId);
    
    /**
     * Gets relationship type distribution for an agent.
     * 
     * @param agentId The agent ID
     * @return Map of relationship types to counts
     */
    Map<RelationshipType, Long> getRelationshipTypeDistribution(String agentId);
    
    /**
     * Gets the count of beliefs for an agent.
     * 
     * @param agentId The agent ID
     * @param includeInactive Whether to include inactive beliefs
     * @return The count of beliefs
     */
    long getBeliefsCount(String agentId, boolean includeInactive);
    
    /**
     * Gets the count of relationships for an agent.
     * 
     * @param agentId The agent ID
     * @param includeInactive Whether to include inactive relationships
     * @return The count of relationships
     */
    long getRelationshipsCount(String agentId, boolean includeInactive);
    
    /**
     * Gets the count of deprecated beliefs for an agent.
     * 
     * @param agentId The agent ID
     * @return The count of deprecated beliefs
     */
    long getDeprecatedBeliefsCount(String agentId);
    
    // ========================================
    // TARGETED BELIEF QUERIES
    // ========================================
    
    /**
     * Streams beliefs for an agent without loading all into memory.
     * Supports filtering and pagination for large datasets.
     * 
     * @param agentId The agent ID
     * @param includeInactive Whether to include inactive beliefs
     * @param pageSize Number of beliefs per page (0 = no pagination)
     * @return Stream of beliefs
     */
    Stream<Belief> streamBeliefs(String agentId, boolean includeInactive, int pageSize);
    
    /**
     * Gets beliefs by category efficiently.
     * 
     * @param agentId The agent ID
     * @param category The belief category
     * @param limit Maximum number of results (0 = no limit)
     * @return List of beliefs in the category
     */
    List<Belief> getBeliefsByCategory(String agentId, String category, int limit);
    
    /**
     * Gets beliefs with confidence above threshold.
     * 
     * @param agentId The agent ID
     * @param confidenceThreshold Minimum confidence level
     * @param limit Maximum number of results (0 = no limit)
     * @return List of high-confidence beliefs
     */
    List<Belief> getHighConfidenceBeliefs(String agentId, double confidenceThreshold, int limit);
    
    /**
     * Gets recently created beliefs.
     * 
     * @param agentId The agent ID
     * @param limit Maximum number of results
     * @return List of recent beliefs
     */
    List<Belief> getRecentBeliefs(String agentId, int limit);
    
    /**
     * Checks if a belief exists without loading it.
     * 
     * @param beliefId The belief ID
     * @param agentId The agent ID
     * @return true if the belief exists
     */
    boolean beliefExists(String beliefId, String agentId);
    
    // ========================================
    // EFFICIENT RELATIONSHIP QUERIES
    // ========================================
    
    /**
     * Streams relationships for an agent without loading all into memory.
     * 
     * @param agentId The agent ID
     * @param includeInactive Whether to include inactive relationships
     * @param pageSize Number of relationships per page (0 = no pagination)
     * @return Stream of relationships
     */
    Stream<BeliefRelationship> streamRelationships(String agentId, boolean includeInactive, int pageSize);
    
    /**
     * Gets the count of relationships connected to a specific belief.
     * 
     * @param beliefId The belief ID
     * @param agentId The agent ID
     * @param direction "incoming", "outgoing", or "both"
     * @return Count of connected relationships
     */
    long getRelationshipCount(String beliefId, String agentId, String direction);
    
    /**
     * Gets relationship IDs (not full objects) for a belief efficiently.
     * Useful for checking connectivity without loading relationship details.
     * 
     * @param beliefId The belief ID
     * @param agentId The agent ID
     * @param direction "incoming", "outgoing", or "both"
     * @param limit Maximum number of results (0 = no limit)
     * @return List of relationship IDs
     */
    List<String> getRelationshipIds(String beliefId, String agentId, String direction, int limit);
    
    /**
     * Gets connected belief IDs (not full belief objects) efficiently.
     * 
     * @param beliefId The source belief ID
     * @param agentId The agent ID
     * @param direction "incoming", "outgoing", or "both"
     * @param relationshipTypes Filter by relationship types (null = all types)
     * @param limit Maximum number of results (0 = no limit)
     * @return List of connected belief IDs
     */
    List<String> getConnectedBeliefIds(String beliefId, String agentId, String direction, 
                                      Set<RelationshipType> relationshipTypes, int limit);
    
    /**
     * Checks if two beliefs are directly connected.
     * 
     * @param sourceBeliefId The source belief ID
     * @param targetBeliefId The target belief ID
     * @param agentId The agent ID
     * @param relationshipTypes Filter by relationship types (null = any type)
     * @return true if directly connected
     */
    boolean areBeliefsDirectlyConnected(String sourceBeliefId, String targetBeliefId, 
                                       String agentId, Set<RelationshipType> relationshipTypes);
    
    // ========================================
    // EFFICIENT DEPRECATION QUERIES
    // ========================================
    
    /**
     * Gets deprecated belief IDs efficiently.
     * 
     * @param agentId The agent ID
     * @param limit Maximum number of results (0 = no limit)
     * @return List of deprecated belief IDs
     */
    List<String> getDeprecatedBeliefIds(String agentId, int limit);
    
    /**
     * Gets belief IDs that supersede a given belief.
     * 
     * @param beliefId The deprecated belief ID
     * @param agentId The agent ID
     * @return List of superseding belief IDs
     */
    List<String> getSupersedingBeliefIds(String beliefId, String agentId);
    
    /**
     * Checks if a belief is deprecated.
     * 
     * @param beliefId The belief ID
     * @param agentId The agent ID
     * @return true if the belief is deprecated
     */
    boolean isBeliefDeprecated(String beliefId, String agentId);
    
    /**
     * Gets the deprecation chain for a belief (belief IDs only).
     * 
     * @param beliefId The starting belief ID
     * @param agentId The agent ID
     * @param maxDepth Maximum chain depth
     * @return List of belief IDs in deprecation order
     */
    List<String> getDeprecationChainIds(String beliefId, String agentId, int maxDepth);
    
    // ========================================
    // EFFICIENT GRAPH TRAVERSAL
    // ========================================
    
    /**
     * Gets belief IDs reachable from a starting belief within specified depth.
     * This method should use efficient graph traversal algorithms.
     * 
     * @param startBeliefId The starting belief ID
     * @param agentId The agent ID
     * @param maxDepth Maximum traversal depth
     * @param relationshipTypes Filter by relationship types (null = all types)
     * @return Set of reachable belief IDs
     */
    Set<String> getReachableBeliefIds(String startBeliefId, String agentId, int maxDepth, 
                                     Set<RelationshipType> relationshipTypes);
    
    /**
     * Finds the shortest path between two beliefs (relationship IDs only).
     * 
     * @param sourceBeliefId The source belief ID
     * @param targetBeliefId The target belief ID
     * @param agentId The agent ID
     * @param maxDepth Maximum search depth
     * @return List of relationship IDs forming the shortest path (empty if no path)
     */
    List<String> getShortestPathIds(String sourceBeliefId, String targetBeliefId, 
                                   String agentId, int maxDepth);
    
    /**
     * Gets belief IDs in strongly connected components above strength threshold.
     * 
     * @param agentId The agent ID
     * @param strengthThreshold Minimum relationship strength
     * @param minClusterSize Minimum cluster size
     * @return Map of cluster names to belief ID sets
     */
    Map<String, Set<String>> getBeliefClusterIds(String agentId, double strengthThreshold, int minClusterSize);
    
    // ========================================
    // CONFLICT AND VALIDATION QUERIES
    // ========================================
    
    /**
     * Gets relationship IDs that represent conflicts.
     * 
     * @param agentId The agent ID
     * @param limit Maximum number of results (0 = no limit)
     * @return List of conflicting relationship IDs
     */
    List<String> getConflictingRelationshipIds(String agentId, int limit);
    
    /**
     * Gets pairs of belief IDs that have contradictory relationships.
     * 
     * @param agentId The agent ID
     * @param limit Maximum number of results (0 = no limit)
     * @return List of maps containing "sourceBeliefId" and "targetBeliefId"
     */
    List<Map<String, String>> getContradictoryBeliefPairs(String agentId, int limit);
    
    /**
     * Validates graph structure efficiently and returns validation issues.
     * This method should identify structural problems without loading the full graph.
     * 
     * @param agentId The agent ID
     * @return List of validation issue descriptions
     */
    List<String> validateGraphStructure(String agentId);
    
    // ========================================
    // BATCH AND UTILITY OPERATIONS
    // ========================================
    
    /**
     * Gets multiple beliefs by IDs efficiently (batch operation).
     * 
     * @param beliefIds Set of belief IDs to retrieve
     * @param agentId The agent ID
     * @return Map of belief ID to belief object
     */
    Map<String, Belief> getBeliefsById(Set<String> beliefIds, String agentId);
    
    /**
     * Gets multiple relationships by IDs efficiently (batch operation).
     * 
     * @param relationshipIds Set of relationship IDs to retrieve
     * @param agentId The agent ID
     * @return Map of relationship ID to relationship object
     */
    Map<String, BeliefRelationship> getRelationshipsById(Set<String> relationshipIds, String agentId);
    
    /**
     * Checks existence of multiple beliefs efficiently.
     * 
     * @param beliefIds Set of belief IDs to check
     * @param agentId The agent ID
     * @return Map of belief ID to existence boolean
     */
    Map<String, Boolean> checkBeliefsExist(Set<String> beliefIds, String agentId);
    
    /**
     * Gets the degree (number of connections) for multiple beliefs.
     * 
     * @param beliefIds Set of belief IDs
     * @param agentId The agent ID
     * @param direction "incoming", "outgoing", or "both"
     * @return Map of belief ID to degree count
     */
    Map<String, Long> getBeliefDegrees(Set<String> beliefIds, String agentId, String direction);
    
    // ========================================
    // EFFICIENT SEARCH AND FILTERING
    // ========================================
    
    /**
     * Searches for beliefs by statement content efficiently.
     * 
     * @param agentId The agent ID
     * @param searchText Text to search for
     * @param limit Maximum number of results
     * @return List of matching beliefs
     */
    List<Belief> searchBeliefsByContent(String agentId, String searchText, int limit);
    
    /**
     * Gets beliefs similar to a given belief based on relationship patterns.
     * Returns only belief IDs for efficiency.
     * 
     * @param beliefId The reference belief ID
     * @param agentId The agent ID
     * @param similarityThreshold Minimum similarity score
     * @param limit Maximum number of results
     * @return List of maps containing "beliefId" and "similarity" score
     */
    List<Map<String, Object>> getSimilarBeliefIds(String beliefId, String agentId, 
                                                  double similarityThreshold, int limit);
    
    // ========================================
    // HEALTH AND MAINTENANCE
    // ========================================
    
    /**
     * Gets service health information efficiently.
     * 
     * @return Map containing health metrics and status
     */
    Map<String, Object> getServiceHealth();
    
    /**
     * Gets performance metrics for the graph query service.
     * 
     * @return Map containing performance metrics
     */
    Map<String, Object> getPerformanceMetrics();
    
    /**
     * Estimates the memory usage for loading a complete graph.
     * Useful for deciding whether to use full graph operations.
     * 
     * @param agentId The agent ID
     * @return Estimated memory usage in bytes
     */
    long estimateGraphMemoryUsage(String agentId);
    
    // ========================================
    // COMPREHENSIVE GRAPH STATISTICS
    // ========================================
    
    /**
     * Gets comprehensive graph statistics using efficient database aggregations.
     * This replaces the heavy statistics computation in BeliefKnowledgeGraph.
     * 
     * @param agentId The agent ID
     * @return Map containing comprehensive graph metrics
     */
    Map<String, Object> getComprehensiveGraphStatistics(String agentId);
    
    /**
     * Gets average relationship strength for an agent.
     * 
     * @param agentId The agent ID
     * @param includeInactive Whether to include inactive relationships
     * @return Average relationship strength
     */
    double getAverageRelationshipStrength(String agentId, boolean includeInactive);
    
    // ========================================
    // GRAPH VALIDATION
    // ========================================
    
    /**
     * Finds orphaned relationships (relationships pointing to non-existent beliefs).
     * 
     * @param agentId The agent ID
     * @return List of orphaned relationship IDs
     */
    List<String> findOrphanedRelationships(String agentId);
    
    /**
     * Finds self-referential relationships.
     * 
     * @param agentId The agent ID
     * @return List of self-referential relationship IDs
     */
    List<String> findSelfReferencingRelationships(String agentId);
    
    /**
     * Finds relationships with invalid temporal periods.
     * 
     * @param agentId The agent ID
     * @return List of relationship IDs with temporal inconsistencies
     */
    List<String> findTemporallyInvalidRelationships(String agentId);
    
    // ========================================
    // ADVANCED GRAPH ANALYSIS
    // ========================================
    
    /**
     * Finds deprecated beliefs efficiently using database queries.
     * 
     * @param agentId The agent ID
     * @return List of deprecated belief IDs
     */
    List<String> findDeprecatedBeliefIds(String agentId);
    
    /**
     * Finds beliefs that supersede a given belief.
     * 
     * @param beliefId The belief ID
     * @param agentId The agent ID
     * @return List of superseding belief IDs
     */
    List<String> findSupersedingBeliefIds(String beliefId, String agentId);
    
    /**
     * Finds the complete deprecation chain for a belief.
     * 
     * @param beliefId The starting belief ID
     * @param agentId The agent ID
     * @return List of belief IDs in deprecation order
     */
    List<String> findDeprecationChain(String beliefId, String agentId);
    
    /**
     * Finds related beliefs within a specified depth using efficient traversal.
     * 
     * @param beliefId The starting belief ID
     * @param agentId The agent ID
     * @param maxDepth Maximum traversal depth
     * @return Set of related belief IDs
     */
    Set<String> findRelatedBeliefIds(String beliefId, String agentId, int maxDepth);
    
    /**
     * Finds strongly connected belief clusters using database-optimized algorithms.
     * 
     * @param agentId The agent ID
     * @param strengthThreshold Minimum relationship strength
     * @return Map of cluster ID to belief IDs in that cluster
     */
    Map<String, Set<String>> findStronglyConnectedBeliefClusters(String agentId, double strengthThreshold);
    
    // ========================================
    // GRAPH SNAPSHOT CREATION
    // ========================================
    
    /**
     * Creates a lightweight snapshot graph for small datasets.
     * Only use this for small graphs (recommended < 1000 beliefs).
     * 
     * @param agentId The agent ID
     * @param includeInactive Whether to include inactive beliefs/relationships
     * @return BeliefKnowledgeGraph snapshot
     */
    ai.headkey.memory.dto.BeliefKnowledgeGraph createSnapshotGraph(String agentId, boolean includeInactive);
    
    /**
     * Creates a filtered snapshot graph based on criteria.
     * 
     * @param agentId The agent ID
     * @param beliefIds Specific belief IDs to include (null = all)
     * @param relationshipTypes Relationship types to include (null = all)
     * @param maxBeliefs Maximum number of beliefs to include
     * @return Filtered BeliefKnowledgeGraph snapshot
     */
    ai.headkey.memory.dto.BeliefKnowledgeGraph createFilteredSnapshotGraph(String agentId, 
                                                                          Set<String> beliefIds,
                                                                          Set<ai.headkey.memory.enums.RelationshipType> relationshipTypes,
                                                                          int maxBeliefs);
    
    /**
     * Creates a graph snapshot suitable for export operations.
     * 
     * @param agentId The agent ID
     * @param format Export format ("json", "dot", etc.)
     * @return BeliefKnowledgeGraph optimized for export
     */
    ai.headkey.memory.dto.BeliefKnowledgeGraph createExportGraph(String agentId, String format);
}