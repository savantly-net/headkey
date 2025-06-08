package ai.headkey.persistence.repositories;

import ai.headkey.persistence.entities.BeliefEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for BeliefEntity operations.
 * 
 * This repository provides data access methods for BeliefEntity objects,
 * including CRUD operations, custom queries, and performance-optimized
 * batch operations. It uses JPA EntityManager for database interactions
 * and leverages named queries for better performance.
 * 
 * The repository is designed to be used with dependency injection
 * and transaction management provided by the container or framework.
 * 
 * @since 1.0
 */
public interface BeliefRepository {

    // ========== Basic CRUD Operations ==========

    /**
     * Saves a belief entity to the database.
     * 
     * @param belief The belief entity to save
     * @return The saved belief entity (with generated/updated fields)
     */
    BeliefEntity save(BeliefEntity belief);

    /**
     * Saves multiple belief entities in a batch operation.
     * 
     * @param beliefs The belief entities to save
     * @return List of saved belief entities
     */
    List<BeliefEntity> saveAll(List<BeliefEntity> beliefs);

    /**
     * Finds a belief entity by its ID.
     * 
     * @param id The belief ID
     * @return Optional containing the belief if found
     */
    Optional<BeliefEntity> findById(String id);

    /**
     * Finds multiple belief entities by their IDs.
     * 
     * @param ids Set of belief IDs
     * @return List of found belief entities
     */
    List<BeliefEntity> findByIds(Set<String> ids);

    /**
     * Deletes a belief entity by its ID.
     * 
     * @param id The belief ID
     * @return true if the belief was deleted, false if it didn't exist
     */
    boolean deleteById(String id);

    /**
     * Deletes a belief entity.
     * 
     * @param belief The belief entity to delete
     */
    void delete(BeliefEntity belief);

    /**
     * Checks if a belief exists by its ID.
     * 
     * @param id The belief ID
     * @return true if the belief exists
     */
    boolean existsById(String id);

    // ========== Query Operations ==========

    /**
     * Finds all beliefs for a specific agent.
     * 
     * @param agentId The agent ID
     * @param includeInactive Whether to include inactive beliefs
     * @return List of beliefs for the agent
     */
    List<BeliefEntity> findByAgent(String agentId, boolean includeInactive);

    /**
     * Finds beliefs in a specific category.
     * 
     * @param category The belief category
     * @param agentId Optional agent filter (null for all agents)
     * @param includeInactive Whether to include inactive beliefs
     * @return List of beliefs in the category
     */
    List<BeliefEntity> findByCategory(String category, String agentId, boolean includeInactive);

    /**
     * Finds all active beliefs in the system.
     * 
     * @return List of all active beliefs
     */
    List<BeliefEntity> findAllActive();

    /**
     * Finds all beliefs in the system (active and inactive).
     * 
     * @return List of all beliefs
     */
    List<BeliefEntity> findAll();

    /**
     * Finds beliefs with confidence below a threshold.
     * 
     * @param threshold The confidence threshold (0.0 to 1.0)
     * @param agentId Optional agent filter (null for all agents)
     * @return List of low-confidence beliefs
     */
    List<BeliefEntity> findLowConfidenceBeliefs(double threshold, String agentId);

    /**
     * Searches for beliefs containing specific text.
     * 
     * @param searchText The text to search for
     * @param agentId Optional agent filter (null for all agents)
     * @param limit Maximum number of results to return
     * @return List of matching beliefs ordered by relevance
     */
    List<BeliefEntity> searchByText(String searchText, String agentId, int limit);

    /**
     * Finds beliefs similar to a given statement using text similarity.
     * 
     * @param statement The statement to find similar beliefs for
     * @param agentId The agent to search within
     * @param similarityThreshold Minimum similarity score (0.0 to 1.0)
     * @param limit Maximum number of results to return
     * @return List of similar beliefs
     */
    List<BeliefEntity> findSimilarBeliefs(String statement, String agentId, double similarityThreshold, int limit);

    // ========== Statistics and Analytics ==========

    /**
     * Counts total beliefs for an agent.
     * 
     * @param agentId The agent ID
     * @param includeInactive Whether to include inactive beliefs
     * @return Total belief count for the agent
     */
    long countByAgent(String agentId, boolean includeInactive);

    /**
     * Counts all beliefs in the system.
     * 
     * @return Total belief count
     */
    long count();

    /**
     * Counts active beliefs in the system.
     * 
     * @return Active belief count
     */
    long countActive();

    /**
     * Gets belief distribution by category.
     * 
     * @param agentId Optional agent filter (null for all agents)
     * @return List of category distribution results
     */
    List<CategoryDistribution> getBeliefDistributionByCategory(String agentId);

    /**
     * Gets belief distribution by confidence level.
     * 
     * @param agentId Optional agent filter (null for all agents)
     * @return List of confidence distribution results
     */
    List<ConfidenceDistribution> getBeliefDistributionByConfidence(String agentId);

    // ========== Batch Operations ==========

    /**
     * Updates beliefs in batch for better performance.
     * 
     * @param beliefs The beliefs to update
     * @return Number of updated beliefs
     */
    int updateBatch(List<BeliefEntity> beliefs);

    /**
     * Deactivates beliefs in batch.
     * 
     * @param beliefIds The IDs of beliefs to deactivate
     * @return Number of deactivated beliefs
     */
    int deactivateBatch(List<String> beliefIds);

    /**
     * Deletes beliefs in batch.
     * 
     * @param beliefIds The IDs of beliefs to delete
     * @return Number of deleted beliefs
     */
    int deleteBatch(List<String> beliefIds);

    // ========== Maintenance Operations ==========

    /**
     * Flushes pending changes to the database.
     */
    void flush();

    /**
     * Clears the persistence context.
     */
    void clear();

    /**
     * Detaches an entity from the persistence context.
     * 
     * @param belief The belief entity to detach
     */
    void detach(BeliefEntity belief);

    /**
     * Refreshes an entity from the database.
     * 
     * @param belief The belief entity to refresh
     */
    void refresh(BeliefEntity belief);

    // ========== Helper Classes ==========

    /**
     * Result class for category distribution queries.
     */
    class CategoryDistribution {
        private final String category;
        private final long count;

        public CategoryDistribution(String category, long count) {
            this.category = category;
            this.count = count;
        }

        public String getCategory() {
            return category;
        }

        public long getCount() {
            return count;
        }

        @Override
        public String toString() {
            return "CategoryDistribution{category='" + category + "', count=" + count + '}';
        }
    }

    /**
     * Result class for confidence distribution queries.
     */
    class ConfidenceDistribution {
        private final String confidenceBucket;
        private final long count;

        public ConfidenceDistribution(String confidenceBucket, long count) {
            this.confidenceBucket = confidenceBucket;
            this.count = count;
        }

        public String getConfidenceBucket() {
            return confidenceBucket;
        }

        public long getCount() {
            return count;
        }

        @Override
        public String toString() {
            return "ConfidenceDistribution{confidenceBucket='" + confidenceBucket + "', count=" + count + '}';
        }
    }
}