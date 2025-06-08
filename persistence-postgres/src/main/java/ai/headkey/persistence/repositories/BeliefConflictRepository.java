package ai.headkey.persistence.repositories;

import ai.headkey.persistence.entities.BeliefConflictEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for BeliefConflictEntity operations.
 * 
 * This repository provides data access methods for BeliefConflictEntity objects,
 * including CRUD operations, custom queries for conflict management, and
 * performance-optimized batch operations. It uses JPA EntityManager for
 * database interactions and leverages named queries for better performance.
 * 
 * The repository is designed to be used with dependency injection
 * and transaction management provided by the container or framework.
 * 
 * @since 1.0
 */
public interface BeliefConflictRepository {

    // ========== Basic CRUD Operations ==========

    /**
     * Saves a belief conflict entity to the database.
     * 
     * @param conflict The belief conflict entity to save
     * @return The saved conflict entity (with generated/updated fields)
     */
    BeliefConflictEntity save(BeliefConflictEntity conflict);

    /**
     * Saves multiple belief conflict entities in a batch operation.
     * 
     * @param conflicts The conflict entities to save
     * @return List of saved conflict entities
     */
    List<BeliefConflictEntity> saveAll(List<BeliefConflictEntity> conflicts);

    /**
     * Finds a belief conflict entity by its ID.
     * 
     * @param id The conflict ID
     * @return Optional containing the conflict if found
     */
    Optional<BeliefConflictEntity> findById(String id);

    /**
     * Finds multiple belief conflict entities by their IDs.
     * 
     * @param ids Set of conflict IDs
     * @return List of found conflict entities
     */
    List<BeliefConflictEntity> findByIds(Set<String> ids);

    /**
     * Deletes a belief conflict entity by its ID.
     * 
     * @param id The conflict ID
     * @return true if the conflict was deleted, false if it didn't exist
     */
    boolean deleteById(String id);

    /**
     * Deletes a belief conflict entity.
     * 
     * @param conflict The conflict entity to delete
     */
    void delete(BeliefConflictEntity conflict);

    /**
     * Checks if a conflict exists by its ID.
     * 
     * @param id The conflict ID
     * @return true if the conflict exists
     */
    boolean existsById(String id);

    // ========== Query Operations ==========

    /**
     * Finds all unresolved conflicts.
     * 
     * @param agentId Optional agent filter (null for all agents)
     * @return List of unresolved conflicts ordered by detection time
     */
    List<BeliefConflictEntity> findUnresolved(String agentId);

    /**
     * Finds all conflicts for a specific agent.
     * 
     * @param agentId The agent ID
     * @return List of conflicts for the agent
     */
    List<BeliefConflictEntity> findByAgent(String agentId);

    /**
     * Finds conflicts by resolution strategy.
     * 
     * @param strategy The resolution strategy
     * @return List of conflicts using the specified strategy
     */
    List<BeliefConflictEntity> findByResolutionStrategy(String strategy);

    /**
     * Finds conflicts involving specific belief IDs.
     * 
     * @param beliefIds Set of belief IDs to search for
     * @return List of conflicts involving any of the specified beliefs
     */
    List<BeliefConflictEntity> findByBeliefIds(Set<String> beliefIds);

    /**
     * Finds conflicts by type.
     * 
     * @param conflictType The type of conflict
     * @param agentId Optional agent filter (null for all agents)
     * @return List of conflicts of the specified type
     */
    List<BeliefConflictEntity> findByType(String conflictType, String agentId);

    /**
     * Finds conflicts by severity level.
     * 
     * @param severity The severity level
     * @param agentId Optional agent filter (null for all agents)
     * @return List of conflicts with the specified severity
     */
    List<BeliefConflictEntity> findBySeverity(String severity, String agentId);

    /**
     * Finds conflicts detected within a time range.
     * 
     * @param startTime The start time (inclusive)
     * @param endTime The end time (inclusive)
     * @param agentId Optional agent filter (null for all agents)
     * @return List of conflicts detected within the time range
     */
    List<BeliefConflictEntity> findByDetectionTimeRange(Instant startTime, Instant endTime, String agentId);

    /**
     * Finds auto-resolvable conflicts.
     * 
     * @param agentId Optional agent filter (null for all agents)
     * @return List of conflicts that can be automatically resolved
     */
    List<BeliefConflictEntity> findAutoResolvable(String agentId);

    /**
     * Finds conflicts requiring manual review.
     * 
     * @param agentId Optional agent filter (null for all agents)
     * @return List of conflicts requiring manual intervention
     */
    List<BeliefConflictEntity> findRequiringManualReview(String agentId);

    // ========== Statistics and Analytics ==========

    /**
     * Counts total conflicts for an agent.
     * 
     * @param agentId The agent ID
     * @return Total conflict count for the agent
     */
    long countByAgent(String agentId);

    /**
     * Counts unresolved conflicts.
     * 
     * @param agentId Optional agent filter (null for all agents)
     * @return Count of unresolved conflicts
     */
    long countUnresolved(String agentId);

    /**
     * Counts all conflicts in the system.
     * 
     * @return Total conflict count
     */
    long count();

    /**
     * Gets conflict distribution by type.
     * 
     * @param agentId Optional agent filter (null for all agents)
     * @return List of conflict type distribution results
     */
    List<ConflictTypeDistribution> getConflictDistributionByType(String agentId);

    /**
     * Gets conflict distribution by severity.
     * 
     * @param agentId Optional agent filter (null for all agents)
     * @return List of conflict severity distribution results
     */
    List<ConflictSeverityDistribution> getConflictDistributionBySeverity(String agentId);

    /**
     * Gets conflict distribution by resolution status.
     * 
     * @param agentId Optional agent filter (null for all agents)
     * @return List of conflict resolution status distribution results
     */
    List<ConflictResolutionDistribution> getConflictDistributionByResolution(String agentId);

    /**
     * Gets average resolution time for resolved conflicts.
     * 
     * @param agentId Optional agent filter (null for all agents)
     * @return Average resolution time in seconds
     */
    double getAverageResolutionTime(String agentId);

    // ========== Batch Operations ==========

    /**
     * Updates conflicts in batch for better performance.
     * 
     * @param conflicts The conflicts to update
     * @return Number of updated conflicts
     */
    int updateBatch(List<BeliefConflictEntity> conflicts);

    /**
     * Marks conflicts as resolved in batch.
     * 
     * @param conflictIds The IDs of conflicts to mark as resolved
     * @param resolutionStrategy The resolution strategy used
     * @param resolutionNotes Optional notes about the resolution
     * @return Number of resolved conflicts
     */
    int markResolvedBatch(List<String> conflictIds, String resolutionStrategy, String resolutionNotes);

    /**
     * Deletes conflicts in batch.
     * 
     * @param conflictIds The IDs of conflicts to delete
     * @return Number of deleted conflicts
     */
    int deleteBatch(List<String> conflictIds);

    /**
     * Deletes old resolved conflicts (cleanup operation).
     * 
     * @param olderThan Delete conflicts resolved before this time
     * @return Number of deleted conflicts
     */
    int deleteOldResolvedConflicts(Instant olderThan);

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
     * @param conflict The conflict entity to detach
     */
    void detach(BeliefConflictEntity conflict);

    /**
     * Refreshes an entity from the database.
     * 
     * @param conflict The conflict entity to refresh
     */
    void refresh(BeliefConflictEntity conflict);

    // ========== Helper Classes ==========

    /**
     * Result class for conflict type distribution queries.
     */
    class ConflictTypeDistribution {
        private final String conflictType;
        private final long count;

        public ConflictTypeDistribution(String conflictType, long count) {
            this.conflictType = conflictType;
            this.count = count;
        }

        public String getConflictType() {
            return conflictType;
        }

        public long getCount() {
            return count;
        }

        @Override
        public String toString() {
            return "ConflictTypeDistribution{conflictType='" + conflictType + "', count=" + count + '}';
        }
    }

    /**
     * Result class for conflict severity distribution queries.
     */
    class ConflictSeverityDistribution {
        private final String severity;
        private final long count;

        public ConflictSeverityDistribution(String severity, long count) {
            this.severity = severity;
            this.count = count;
        }

        public String getSeverity() {
            return severity;
        }

        public long getCount() {
            return count;
        }

        @Override
        public String toString() {
            return "ConflictSeverityDistribution{severity='" + severity + "', count=" + count + '}';
        }
    }

    /**
     * Result class for conflict resolution distribution queries.
     */
    class ConflictResolutionDistribution {
        private final String resolutionStatus;
        private final long count;

        public ConflictResolutionDistribution(String resolutionStatus, long count) {
            this.resolutionStatus = resolutionStatus;
            this.count = count;
        }

        public String getResolutionStatus() {
            return resolutionStatus;
        }

        public long getCount() {
            return count;
        }

        @Override
        public String toString() {
            return "ConflictResolutionDistribution{resolutionStatus='" + resolutionStatus + "', count=" + count + '}';
        }
    }
}