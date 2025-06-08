package ai.headkey.memory.spi;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefConflict;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Service Provider Interface for belief storage and persistence.
 * 
 * This interface defines the contract for storing, retrieving, and managing
 * beliefs in various persistence backends. Implementations can range from
 * simple in-memory storage for testing to sophisticated distributed databases
 * for production use.
 * 
 * The interface supports different storage strategies:
 * - In-memory storage (ConcurrentHashMap, Chronicle Map)
 * - Relational databases (PostgreSQL, MySQL, H2)
 * - NoSQL databases (MongoDB, Cassandra, DynamoDB)
 * - Graph databases (Neo4j, Amazon Neptune)
 * - Vector databases (Pinecone, Weaviate, Chroma)
 * - Distributed caches (Redis, Hazelcast)
 * 
 * @since 1.0
 */
public interface BeliefStorageService {

    // ========== Basic CRUD Operations ==========

    /**
     * Stores a new belief or updates an existing one.
     * 
     * If a belief with the same ID already exists, it should be updated.
     * If no belief exists with the given ID, a new one should be created.
     * 
     * @param belief The belief to store
     * @return The stored belief (may include generated fields like timestamps)
     * @throws BeliefStorageException if storage operation fails
     */
    Belief storeBelief(Belief belief);

    /**
     * Stores multiple beliefs in a batch operation.
     * 
     * This method should be optimized for bulk operations and provide
     * better performance than storing beliefs individually.
     * 
     * @param beliefs The beliefs to store
     * @return List of stored beliefs
     * @throws BeliefStorageException if storage operation fails
     */
    List<Belief> storeBeliefs(List<Belief> beliefs);

    /**
     * Retrieves a belief by its unique identifier.
     * 
     * @param beliefId The unique identifier of the belief
     * @return Optional containing the belief if found, empty otherwise
     * @throws BeliefStorageException if retrieval operation fails
     */
    Optional<Belief> getBeliefById(String beliefId);

    /**
     * Retrieves multiple beliefs by their identifiers.
     * 
     * @param beliefIds Set of belief identifiers
     * @return List of found beliefs (may be fewer than requested if some don't exist)
     * @throws BeliefStorageException if retrieval operation fails
     */
    List<Belief> getBeliefsById(Set<String> beliefIds);

    /**
     * Deletes a belief permanently from storage.
     * 
     * Note: In most cases, beliefs should be deactivated rather than deleted
     * to maintain audit trails and data integrity.
     * 
     * @param beliefId The unique identifier of the belief to delete
     * @return true if the belief was deleted, false if it didn't exist
     * @throws BeliefStorageException if deletion operation fails
     */
    boolean deleteBelief(String beliefId);

    // ========== Query Operations ==========

    /**
     * Gets all beliefs for a specific agent.
     * 
     * @param agentId The agent identifier
     * @param includeInactive Whether to include inactive beliefs
     * @return List of beliefs for the agent
     * @throws BeliefStorageException if query operation fails
     */
    List<Belief> getBeliefsForAgent(String agentId, boolean includeInactive);

    /**
     * Gets beliefs in a specific category.
     * 
     * @param category The belief category
     * @param agentId Optional agent filter (null for all agents)
     * @param includeInactive Whether to include inactive beliefs
     * @return List of beliefs in the category
     * @throws BeliefStorageException if query operation fails
     */
    List<Belief> getBeliefsInCategory(String category, String agentId, boolean includeInactive);

    /**
     * Gets all active beliefs in the system.
     * 
     * @return List of all active beliefs
     * @throws BeliefStorageException if query operation fails
     */
    List<Belief> getAllActiveBeliefs();

    /**
     * Gets all beliefs in the system (active and inactive).
     * 
     * @return List of all beliefs
     * @throws BeliefStorageException if query operation fails
     */
    List<Belief> getAllBeliefs();

    /**
     * Finds beliefs with confidence below a threshold.
     * 
     * @param confidenceThreshold The confidence threshold (0.0 to 1.0)
     * @param agentId Optional agent filter (null for all agents)
     * @return List of low-confidence beliefs
     * @throws BeliefStorageException if query operation fails
     */
    List<Belief> getLowConfidenceBeliefs(double confidenceThreshold, String agentId);

    /**
     * Searches for beliefs containing specific text.
     * 
     * This method performs a text search on belief statements and should
     * support fuzzy matching and relevance scoring where possible.
     * 
     * @param searchText The text to search for
     * @param agentId Optional agent filter (null for all agents)
     * @param limit Maximum number of results to return
     * @return List of matching beliefs ordered by relevance
     * @throws BeliefStorageException if search operation fails
     */
    List<Belief> searchBeliefs(String searchText, String agentId, int limit);

    /**
     * Finds beliefs similar to a given statement.
     * 
     * This method should use semantic similarity rather than just text matching.
     * Implementation depends on the storage backend capabilities.
     * 
     * @param statement The statement to find similar beliefs for
     * @param agentId The agent to search within
     * @param similarityThreshold Minimum similarity score (0.0 to 1.0)
     * @param limit Maximum number of results to return
     * @return List of similar beliefs with similarity scores
     * @throws BeliefStorageException if search operation fails
     */
    List<SimilarBelief> findSimilarBeliefs(String statement, String agentId, 
                                          double similarityThreshold, int limit);

    // ========== Conflict Management ==========

    /**
     * Stores an unresolved conflict.
     * 
     * @param conflict The conflict to store
     * @return The stored conflict
     * @throws BeliefStorageException if storage operation fails
     */
    BeliefConflict storeConflict(BeliefConflict conflict);

    /**
     * Retrieves a conflict by its identifier.
     * 
     * @param conflictId The conflict identifier
     * @return Optional containing the conflict if found, empty otherwise
     * @throws BeliefStorageException if retrieval operation fails
     */
    Optional<BeliefConflict> getConflictById(String conflictId);

    /**
     * Gets all unresolved conflicts.
     * 
     * @param agentId Optional agent filter (null for all agents)
     * @return List of unresolved conflicts
     * @throws BeliefStorageException if query operation fails
     */
    List<BeliefConflict> getUnresolvedConflicts(String agentId);

    /**
     * Removes a conflict from storage.
     * 
     * This is typically called when a conflict has been resolved.
     * 
     * @param conflictId The conflict identifier
     * @return true if the conflict was removed, false if it didn't exist
     * @throws BeliefStorageException if removal operation fails
     */
    boolean removeConflict(String conflictId);

    // ========== Statistics and Analytics ==========

    /**
     * Gets storage statistics and metrics.
     * 
     * Returns information about storage performance, capacity, and usage:
     * - Total number of beliefs stored
     * - Storage size and capacity utilization
     * - Query performance metrics
     * - Connection pool status
     * - Cache hit rates
     * 
     * @return Map containing storage statistics
     */
    Map<String, Object> getStorageStatistics();

    /**
     * Gets belief distribution by category.
     * 
     * @param agentId Optional agent filter (null for all agents)
     * @return Map of category name to belief count
     * @throws BeliefStorageException if query operation fails
     */
    Map<String, Long> getBeliefDistributionByCategory(String agentId);

    /**
     * Gets belief distribution by confidence level.
     * 
     * @param agentId Optional agent filter (null for all agents)
     * @return Map of confidence bucket to belief count
     * @throws BeliefStorageException if query operation fails
     */
    Map<String, Long> getBeliefDistributionByConfidence(String agentId);

    /**
     * Counts total beliefs for an agent.
     * 
     * @param agentId The agent identifier
     * @param includeInactive Whether to include inactive beliefs
     * @return Total belief count for the agent
     * @throws BeliefStorageException if query operation fails
     */
    long countBeliefsForAgent(String agentId, boolean includeInactive);

    // ========== Maintenance Operations ==========

    /**
     * Performs storage optimization and cleanup.
     * 
     * This method should:
     * - Compact storage if applicable
     * - Remove tombstoned records
     * - Update indexes and statistics
     * - Free unused memory/disk space
     * 
     * @return Map containing optimization results
     * @throws BeliefStorageException if optimization fails
     */
    Map<String, Object> optimizeStorage();

    /**
     * Validates storage integrity.
     * 
     * This method performs consistency checks and identifies any
     * data corruption or inconsistencies in the storage layer.
     * 
     * @return Map containing validation results
     * @throws BeliefStorageException if validation fails
     */
    Map<String, Object> validateIntegrity();

    /**
     * Backs up belief data.
     * 
     * Creates a backup of belief data that can be restored later.
     * The backup format and location depend on the implementation.
     * 
     * @param backupId Unique identifier for this backup
     * @param options Backup configuration options
     * @return Backup information and metadata
     * @throws BeliefStorageException if backup operation fails
     */
    Map<String, Object> createBackup(String backupId, Map<String, Object> options);

    // ========== Health and Monitoring ==========

    /**
     * Checks if the storage service is healthy and ready.
     * 
     * This method performs health checks specific to the storage implementation:
     * - Database connectivity
     * - Resource availability (disk space, memory)
     * - Service dependencies
     * - Performance thresholds
     * 
     * @return true if the storage service is healthy
     */
    boolean isHealthy();

    /**
     * Gets detailed health information.
     * 
     * @return Map containing detailed health status and diagnostics
     */
    Map<String, Object> getHealthInfo();

    /**
     * Gets information about the storage implementation.
     * 
     * @return Map containing storage service metadata
     */
    Map<String, Object> getServiceInfo();

    // ========== Helper Classes ==========

    /**
     * Represents a belief with its similarity score to a query.
     */
    class SimilarBelief {
        private final Belief belief;
        private final double similarityScore;

        public SimilarBelief(Belief belief, double similarityScore) {
            this.belief = belief;
            this.similarityScore = Math.max(0.0, Math.min(1.0, similarityScore));
        }

        public Belief getBelief() { return belief; }
        public double getSimilarityScore() { return similarityScore; }

        @Override
        public String toString() {
            return "SimilarBelief{" +
                    "belief=" + belief.getId() +
                    ", similarityScore=" + similarityScore +
                    '}';
        }
    }

    /**
     * Exception thrown when storage operations fail.
     */
    class BeliefStorageException extends RuntimeException {
        public BeliefStorageException(String message) {
            super(message);
        }

        public BeliefStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}