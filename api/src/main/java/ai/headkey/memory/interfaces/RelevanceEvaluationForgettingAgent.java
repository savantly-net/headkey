package ai.headkey.memory.interfaces;

import ai.headkey.memory.dto.ForgettingReport;
import ai.headkey.memory.dto.ForgettingStrategy;
import ai.headkey.memory.dto.MemoryRecord;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface for the Relevance Evaluation & Forgetting Agent (REFA).
 * 
 * The REFA encapsulates logic for deciding what to forget and carrying out
 * the forgetting process. It manages memory lifecycle by evaluating relevance
 * and implementing various forgetting strategies to prevent memory overload
 * and maintain system performance.
 * 
 * Intelligent forgetting is vital for preventing memory overload and focusing
 * the agent on what matters. The interface supports multiple forgetting
 * strategies from simple time-based expiration to complex semantic compression.
 * 
 * The design allows for pluggable strategies and provides both automatic
 * and manual forgetting capabilities, ensuring that crucial knowledge is
 * retained while irrelevant data is pruned.
 * 
 * @since 1.0
 */
public interface RelevanceEvaluationForgettingAgent {
    
    /**
     * Evaluates and assigns a relevance score to a memory item.
     * 
     * Computes a relevance score for the given memory based on various factors:
     * - Recency of creation and last access
     * - Frequency of access and retrieval
     * - Importance flags and metadata
     * - Association with current active topics
     * - Belief reinforcement strength
     * - User-defined importance markers
     * 
     * The score ranges from 0.0 (completely irrelevant) to 1.0 (highly relevant).
     * This score is used internally by forgetting strategies and can also be
     * used by retrieval systems for ranking results.
     * 
     * @param memory The memory record to evaluate
     * @return A relevance score between 0.0 and 1.0 indicating importance
     * @throws IllegalArgumentException if memory is null
     * @throws RelevanceEvaluationException if evaluation process fails
     * 
     * @since 1.0
     */
    double evaluateRelevance(MemoryRecord memory);
    
    /**
     * Evaluates relevance for multiple memories in batch.
     * 
     * Efficiently computes relevance scores for multiple memory records
     * in a single operation, optimizing for scenarios where bulk evaluation
     * is needed (e.g., during scheduled forgetting cycles).
     * 
     * @param memories List of memory records to evaluate
     * @return Map of memory ID to relevance score for each memory
     * @throws IllegalArgumentException if memories is null or empty
     * @throws RelevanceEvaluationException if batch evaluation fails
     * 
     * @since 1.0
     */
    Map<String, Double> evaluateRelevanceBatch(List<MemoryRecord> memories);
    
    /**
     * Performs a forgetting cycle based on a given strategy.
     * 
     * Executes the complete forgetting process:
     * 1. Identifies memories that meet the forgetting criteria
     * 2. Evaluates each candidate against protection rules
     * 3. Removes qualifying memories from storage
     * 4. Updates relevance statistics and metrics
     * 5. Optionally archives removed items for recovery
     * 
     * The process respects the strategy configuration including:
     * - Dry run mode (identification without deletion)
     * - Maximum removal limits
     * - Confidence thresholds
     * - Agent and category filters
     * 
     * @param strategy A specification of the forgetting strategy to apply
     * @return ForgettingReport summarizing removed and remaining memories
     * @throws IllegalArgumentException if strategy is null or invalid
     * @throws ForgettingException if the forgetting process fails
     * 
     * @since 1.0
     */
    ForgettingReport performForgetting(ForgettingStrategy strategy);
    
    /**
     * Identifies memory candidates for removal without actually deleting them.
     * 
     * Performs the identification phase of forgetting to determine which
     * memories would be removed by a given strategy, but doesn't perform
     * the actual deletion. Useful for:
     * - Preview and validation before forgetting
     * - Testing forgetting strategies
     * - Generating removal estimates
     * - Manual review workflows
     * 
     * @param strategy The forgetting strategy to simulate
     * @return List of MemoryRecord objects that would be removed
     * @throws IllegalArgumentException if strategy is null or invalid
     * @throws RelevanceEvaluationException if candidate identification fails
     * 
     * @since 1.0
     */
    List<MemoryRecord> identifyForgettingCandidates(ForgettingStrategy strategy);
    
    /**
     * Sets protection rules for memories that should never be forgotten.
     * 
     * Configures criteria that protect certain memories from being removed
     * regardless of their relevance scores or age. Protection rules might
     * include:
     * - Memories with specific importance tags
     * - Core beliefs or fundamental knowledge
     * - Recently accessed critical information
     * - User-flagged important memories
     * - Memories supporting active beliefs
     * 
     * @param protectionRules Map of rule names to rule criteria
     * @throws IllegalArgumentException if protectionRules is null
     * 
     * @since 1.0
     */
    void setProtectionRules(Map<String, String> protectionRules);
    
    /**
     * Gets the current protection rules configuration.
     * 
     * Returns the set of rules that protect memories from being forgotten,
     * useful for configuration validation and debugging.
     * 
     * @return Map of protection rule names to their criteria
     * 
     * @since 1.0
     */
    Map<String, String> getProtectionRules();
    
    /**
     * Checks if a specific memory is protected from forgetting.
     * 
     * Evaluates whether the given memory matches any of the configured
     * protection rules and should be excluded from forgetting operations.
     * 
     * @param memory The memory to check for protection
     * @return true if the memory is protected from forgetting
     * @throws IllegalArgumentException if memory is null
     * 
     * @since 1.0
     */
    boolean isProtected(MemoryRecord memory);
    
    /**
     * Updates relevance scores for all memories of a specific agent.
     * 
     * Recalculates relevance scores for all memories belonging to the
     * specified agent, useful for maintaining up-to-date scoring after
     * significant changes in access patterns or importance criteria.
     * 
     * @param agentId The agent whose memory relevance should be updated
     * @return Number of memories that had their scores updated
     * @throws IllegalArgumentException if agentId is null or empty
     * @throws RelevanceEvaluationException if update process fails
     * 
     * @since 1.0
     */
    int updateRelevanceScores(String agentId);
    
    /**
     * Gets memories with low relevance scores.
     * 
     * Identifies memories that have relevance scores below a specified
     * threshold, indicating they are candidates for forgetting.
     * 
     * @param scoreThreshold The relevance score threshold
     * @param agentId Optional agent ID filter (null for all agents)
     * @param limit Maximum number of memories to return (0 for no limit)
     * @return List of low-relevance memories ordered by score (lowest first)
     * @throws IllegalArgumentException if scoreThreshold is outside valid range
     * 
     * @since 1.0
     */
    List<MemoryRecord> getLowRelevanceMemories(double scoreThreshold, String agentId, int limit);
    
    /**
     * Gets memories that haven't been accessed recently.
     * 
     * Finds memories that haven't been retrieved or accessed within a
     * specified time period, useful for usage-based forgetting strategies.
     * 
     * @param daysUnaccessed Number of days without access
     * @param agentId Optional agent ID filter (null for all agents)
     * @param limit Maximum number of memories to return (0 for no limit)
     * @return List of unaccessed memories ordered by last access time
     * @throws IllegalArgumentException if daysUnaccessed < 0
     * 
     * @since 1.0
     */
    List<MemoryRecord> getUnaccessedMemories(int daysUnaccessed, String agentId, int limit);
    
    /**
     * Archives memories instead of permanently deleting them.
     * 
     * Moves the specified memories to an archive storage where they
     * are preserved but not included in active memory operations.
     * Archived memories can potentially be restored later.
     * 
     * @param memoryIds Set of memory IDs to archive
     * @param archiveReason Reason for archiving (for audit purposes)
     * @return Set of memory IDs that were successfully archived
     * @throws IllegalArgumentException if memoryIds is null or empty
     * @throws ForgettingException if archiving fails
     * 
     * @since 1.0
     */
    Set<String> archiveMemories(Set<String> memoryIds, String archiveReason);
    
    /**
     * Restores archived memories back to active storage.
     * 
     * Moves previously archived memories back to the active memory store,
     * making them available for normal memory operations again.
     * 
     * @param memoryIds Set of archived memory IDs to restore
     * @return Set of memory IDs that were successfully restored
     * @throws IllegalArgumentException if memoryIds is null or empty
     * @throws ForgettingException if restoration fails
     * 
     * @since 1.0
     */
    Set<String> restoreArchivedMemories(Set<String> memoryIds);
    
    /**
     * Gets a list of available archive entries.
     * 
     * Returns information about memories that have been archived,
     * useful for archive management and selective restoration.
     * 
     * @param agentId Optional agent ID filter (null for all agents)
     * @param limit Maximum number of entries to return (0 for no limit)
     * @return List of archived memory information
     * 
     * @since 1.0
     */
    List<ArchiveEntry> getArchivedMemories(String agentId, int limit);
    
    /**
     * Gets forgetting statistics and metrics.
     * 
     * Returns comprehensive statistics about forgetting operations including:
     * - Total memories removed over time
     * - Forgetting strategy effectiveness
     * - Average relevance scores
     * - Protection rule hit rates
     * - Archive utilization
     * - Performance metrics
     * 
     * @return Map containing various forgetting statistics
     * 
     * @since 1.0
     */
    Map<String, Object> getForgettingStatistics();
    
    /**
     * Gets relevance evaluation statistics.
     * 
     * Returns metrics about the relevance evaluation process including:
     * - Score distribution across memories
     * - Evaluation processing times
     * - Factor weighting effectiveness
     * - Score accuracy measures
     * 
     * @return Map containing relevance evaluation statistics
     * 
     * @since 1.0
     */
    Map<String, Object> getRelevanceStatistics();
    
    /**
     * Configures relevance evaluation parameters.
     * 
     * Sets up weights and parameters that influence how relevance scores
     * are calculated, allowing customization of the evaluation algorithm.
     * 
     * @param parameters Map of parameter names to values
     * @throws IllegalArgumentException if parameters is null
     * 
     * @since 1.0
     */
    void configureRelevanceParameters(Map<String, Object> parameters);
    
    /**
     * Checks if the forgetting agent is healthy and ready.
     * 
     * Performs a health check of the forgetting system including:
     * - Storage system connectivity
     * - Archive system availability
     * - Configuration validity
     * - Resource availability
     * 
     * @return true if the system is healthy and ready to process requests
     * 
     * @since 1.0
     */
    boolean isHealthy();
    
    /**
     * Data class representing an archived memory entry.
     */
    public static class ArchiveEntry {
        private final String memoryId;
        private final String agentId;
        private final String archivedAt;
        private final String reason;
        private final String summary;
        
        public ArchiveEntry(String memoryId, String agentId, String archivedAt, String reason, String summary) {
            this.memoryId = memoryId;
            this.agentId = agentId;
            this.archivedAt = archivedAt;
            this.reason = reason;
            this.summary = summary;
        }
        
        public String getMemoryId() { return memoryId; }
        public String getAgentId() { return agentId; }
        public String getArchivedAt() { return archivedAt; }
        public String getReason() { return reason; }
        public String getSummary() { return summary; }
    }
    
    /**
     * Exception thrown when relevance evaluation operations fail.
     */
    public static class RelevanceEvaluationException extends RuntimeException {
        public RelevanceEvaluationException(String message) {
            super(message);
        }
        
        public RelevanceEvaluationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Exception thrown when forgetting operations fail.
     */
    public static class ForgettingException extends RuntimeException {
        public ForgettingException(String message) {
            super(message);
        }
        
        public ForgettingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}