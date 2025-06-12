package ai.headkey.persistence.repositories;

import ai.headkey.memory.enums.RelationshipType;
import ai.headkey.persistence.entities.BeliefRelationshipEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for BeliefRelationshipEntity operations.
 * 
 * This repository provides data access methods for BeliefRelationshipEntity objects,
 * including CRUD operations, custom queries, and performance-optimized
 * batch operations. It uses JPA EntityManager for database interactions
 * and leverages named queries for better performance.
 * 
 * The repository is designed to be used with dependency injection
 * and transaction management provided by the container or framework.
 * 
 * @since 1.0
 */
public interface BeliefRelationshipRepository {

    // ========== Basic CRUD Operations ==========

    /**
     * Saves a belief relationship entity to the database.
     * 
     * @param relationship The relationship entity to save
     * @return The saved relationship entity (with generated/updated fields)
     */
    BeliefRelationshipEntity save(BeliefRelationshipEntity relationship);

    /**
     * Saves multiple belief relationship entities in a batch operation.
     * 
     * @param relationships The relationship entities to save
     * @return List of saved relationship entities
     */
    List<BeliefRelationshipEntity> saveAll(List<BeliefRelationshipEntity> relationships);

    /**
     * Finds a belief relationship entity by its ID.
     * 
     * @param id The relationship ID
     * @return Optional containing the relationship if found
     */
    Optional<BeliefRelationshipEntity> findById(String id);

    /**
     * Finds multiple belief relationship entities by their IDs.
     * 
     * @param ids The relationship IDs
     * @return List of found relationship entities
     */
    List<BeliefRelationshipEntity> findByIds(Set<String> ids);

    /**
     * Deletes a belief relationship entity by its ID.
     * 
     * @param id The relationship ID
     * @return true if the relationship was deleted, false if not found
     */
    boolean deleteById(String id);

    /**
     * Checks if a belief relationship exists with the given ID.
     * 
     * @param id The relationship ID
     * @return true if the relationship exists
     */
    boolean existsById(String id);

    // ========== Query Operations ==========

    /**
     * Finds all relationships for a specific agent.
     * 
     * @param agentId The agent ID
     * @param includeInactive Whether to include inactive relationships
     * @return List of relationship entities
     */
    List<BeliefRelationshipEntity> findByAgent(String agentId, boolean includeInactive);

    /**
     * Finds all outgoing relationships from a specific belief.
     * 
     * @param sourceBeliefId The source belief ID
     * @param agentId The agent ID (null for all agents)
     * @param includeInactive Whether to include inactive relationships
     * @return List of outgoing relationship entities
     */
    List<BeliefRelationshipEntity> findBySourceBelief(String sourceBeliefId, String agentId, boolean includeInactive);

    /**
     * Finds all incoming relationships to a specific belief.
     * 
     * @param targetBeliefId The target belief ID
     * @param agentId The agent ID (null for all agents)
     * @param includeInactive Whether to include inactive relationships
     * @return List of incoming relationship entities
     */
    List<BeliefRelationshipEntity> findByTargetBelief(String targetBeliefId, String agentId, boolean includeInactive);

    /**
     * Finds all relationships connected to a specific belief (both incoming and outgoing).
     * 
     * @param beliefId The belief ID
     * @param agentId The agent ID (null for all agents)
     * @param includeInactive Whether to include inactive relationships
     * @return List of relationship entities
     */
    List<BeliefRelationshipEntity> findByBelief(String beliefId, String agentId, boolean includeInactive);

    /**
     * Finds relationships by type.
     * 
     * @param relationshipType The relationship type
     * @param agentId The agent ID (null for all agents)
     * @param includeInactive Whether to include inactive relationships
     * @return List of relationship entities
     */
    List<BeliefRelationshipEntity> findByType(RelationshipType relationshipType, String agentId, boolean includeInactive);

    /**
     * Finds relationships between two specific beliefs.
     * 
     * @param sourceBeliefId The source belief ID
     * @param targetBeliefId The target belief ID
     * @param agentId The agent ID (null for all agents)
     * @param includeInactive Whether to include inactive relationships
     * @return List of relationship entities
     */
    List<BeliefRelationshipEntity> findBetweenBeliefs(String sourceBeliefId, String targetBeliefId, String agentId, boolean includeInactive);

    /**
     * Finds relationships that are deprecating (supersedes, updates, deprecates, replaces).
     * 
     * @param agentId The agent ID (null for all agents)
     * @return List of deprecating relationship entities
     */
    List<BeliefRelationshipEntity> findDeprecating(String agentId);

    /**
     * Finds relationships that are currently effective.
     * 
     * @param agentId The agent ID (null for all agents)
     * @return List of currently effective relationship entities
     */
    List<BeliefRelationshipEntity> findCurrentlyEffective(String agentId);

    /**
     * Finds relationships with strength above a threshold.
     * 
     * @param threshold The minimum strength threshold
     * @param agentId The agent ID (null for all agents)
     * @return List of high-strength relationship entities
     */
    List<BeliefRelationshipEntity> findByHighStrength(double threshold, String agentId);

    /**
     * Finds relationships effective at a specific time.
     * 
     * @param timestamp The timestamp to check
     * @param agentId The agent ID (null for all agents)
     * @return List of relationship entities effective at the given time
     */
    List<BeliefRelationshipEntity> findEffectiveAt(Instant timestamp, String agentId);

    /**
     * Finds relationships that expired before a specific time.
     * 
     * @param timestamp The timestamp to check
     * @param agentId The agent ID (null for all agents)
     * @return List of expired relationship entities
     */
    List<BeliefRelationshipEntity> findExpiredBefore(Instant timestamp, String agentId);

    // ========== Batch Operations ==========

    /**
     * Updates the strength of multiple relationships.
     * 
     * @param relationshipIds The relationship IDs to update
     * @param newStrength The new strength value
     * @return Number of relationships updated
     */
    int updateStrengthBatch(Set<String> relationshipIds, double newStrength);

    /**
     * Deactivates multiple relationships.
     * 
     * @param relationshipIds The relationship IDs to deactivate
     * @return Number of relationships deactivated
     */
    int deactivateBatch(Set<String> relationshipIds);

    /**
     * Reactivates multiple relationships.
     * 
     * @param relationshipIds The relationship IDs to reactivate
     * @return Number of relationships reactivated
     */
    int reactivateBatch(Set<String> relationshipIds);

    /**
     * Deletes multiple relationships.
     * 
     * @param relationshipIds The relationship IDs to delete
     * @return Number of relationships deleted
     */
    int deleteBatch(Set<String> relationshipIds);

    /**
     * Deletes inactive relationships older than the specified number of days.
     * 
     * @param agentId The agent ID
     * @param olderThanDays Number of days
     * @return Number of relationships deleted
     */
    int deleteOldInactiveRelationships(String agentId, int olderThanDays);

    // ========== Count Operations ==========

    /**
     * Counts the total number of relationships for an agent.
     * 
     * @param agentId The agent ID
     * @param includeInactive Whether to include inactive relationships
     * @return The count of relationships
     */
    long countByAgent(String agentId, boolean includeInactive);

    /**
     * Counts relationships by type for an agent.
     * 
     * @param agentId The agent ID
     * @param relationshipType The relationship type
     * @param includeInactive Whether to include inactive relationships
     * @return The count of relationships
     */
    long countByAgentAndType(String agentId, RelationshipType relationshipType, boolean includeInactive);

    /**
     * Counts relationships connected to a specific belief.
     * 
     * @param beliefId The belief ID
     * @param agentId The agent ID
     * @param includeInactive Whether to include inactive relationships
     * @return The count of relationships
     */
    long countByBelief(String beliefId, String agentId, boolean includeInactive);

    // ========== Statistics Operations ==========

    /**
     * Gets relationship type distribution for an agent.
     * 
     * @param agentId The agent ID
     * @param includeInactive Whether to include inactive relationships
     * @return Map of relationship type to count
     */
    List<Object[]> getRelationshipTypeDistribution(String agentId, boolean includeInactive);

    /**
     * Gets average relationship strength by type for an agent.
     * 
     * @param agentId The agent ID
     * @return Map of relationship type to average strength
     */
    List<Object[]> getAverageStrengthByType(String agentId);

    /**
     * Gets relationship statistics for an agent.
     * 
     * @param agentId The agent ID
     * @return Array containing [totalCount, activeCount, averageStrength]
     */
    Object[] getRelationshipStatistics(String agentId);

    // ========== Graph Operations ==========

    /**
     * Finds beliefs connected to a belief within a specific depth.
     * 
     * @param beliefId The starting belief ID
     * @param agentId The agent ID
     * @param maxDepth Maximum traversal depth
     * @param includeInactive Whether to include inactive relationships
     * @return List of connected belief IDs
     */
    List<String> findConnectedBeliefs(String beliefId, String agentId, int maxDepth, boolean includeInactive);

    /**
     * Finds strongly connected belief clusters.
     * 
     * @param agentId The agent ID
     * @param strengthThreshold Minimum relationship strength
     * @return List of belief clusters (each cluster is a list of belief IDs)
     */
    List<List<String>> findStronglyConnectedClusters(String agentId, double strengthThreshold);

    /**
     * Finds the shortest path between two beliefs.
     * 
     * @param sourceBeliefId The source belief ID
     * @param targetBeliefId The target belief ID
     * @param agentId The agent ID
     * @return List of relationship entities forming the shortest path
     */
    List<BeliefRelationshipEntity> findShortestPath(String sourceBeliefId, String targetBeliefId, String agentId);

    /**
     * Finds orphaned relationships (relationships to non-existent beliefs).
     * 
     * @param agentId The agent ID
     * @return List of orphaned relationship entities
     */
    List<BeliefRelationshipEntity> findOrphanedRelationships(String agentId);

    /**
     * Finds self-referencing relationships.
     * 
     * @param agentId The agent ID
     * @return List of self-referencing relationship entities
     */
    List<BeliefRelationshipEntity> findSelfReferencingRelationships(String agentId);

    /**
     * Validates temporal constraints for relationships.
     * 
     * @param agentId The agent ID
     * @return List of relationship entities with temporal issues
     */
    List<BeliefRelationshipEntity> findTemporallyInvalidRelationships(String agentId);
}