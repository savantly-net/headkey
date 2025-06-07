package ai.headkey.memory.interfaces;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefConflict;
import ai.headkey.memory.dto.BeliefUpdateResult;
import ai.headkey.memory.dto.MemoryRecord;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface for the Belief Reinforcement & Conflict Analyzer (BRCA).
 * 
 * The BRCA handles the logic of keeping the agent's belief model in sync with
 * the memories. It manages the creation, reinforcement, and conflict resolution
 * of beliefs based on new information entering the system.
 * 
 * This interface distinguishes between raw memory (which may contain redundancies
 * or errors) and structured beliefs (which are distilled knowledge). It ensures
 * that frequently confirmed information is noted (belief reinforced) and
 * contradictory information is flagged for resolution.
 * 
 * The interface allows different implementations ranging from simple key-value
 * fact storage with occurrence counting to complex logical inference systems
 * or knowledge graph integration.
 * 
 * @since 1.0
 */
public interface BeliefReinforcementConflictAnalyzer {
    
    /**
     * Analyzes a newly stored memory and updates beliefs accordingly.
     * 
     * This method is invoked whenever a new MemoryRecord is added to the system
     * to update the belief system incrementally. The analysis process includes:
     * 1. Extracting factual information from the memory
     * 2. Checking for existing beliefs that relate to this information
     * 3. Reinforcing existing beliefs with supporting evidence
     * 4. Creating new beliefs for novel information
     * 5. Detecting and handling conflicts with existing beliefs
     * 
     * For example, if a new memory states "User's favorite color is blue",
     * the analyzer might update a belief like FavoriteColor(user) = blue,
     * reinforcing it if it existed or creating it anew.
     * 
     * @param newMemory The MemoryRecord that was recently added to storage
     * @return BeliefUpdateResult detailing belief changes, reinforcements, and conflicts
     * @throws IllegalArgumentException if newMemory is null
     * @throws BeliefAnalysisException if the analysis process fails
     * 
     * @since 1.0
     */
    BeliefUpdateResult analyzeNewMemory(MemoryRecord newMemory);
    
    /**
     * Performs a comprehensive review of the belief base for consistency and reinforcement.
     * 
     * This method provides a comprehensive scan of the entire belief system,
     * cross-checking for contradictions among all memories and beliefs.
     * It can be run periodically as a maintenance operation or on-demand
     * via the explicit belief-update API.
     * 
     * The review process includes:
     * - Analyzing all stored beliefs for internal consistency
     * - Cross-referencing beliefs with supporting evidence
     * - Identifying contradictions between beliefs
     * - Updating confidence levels based on evidence strength
     * - Marking outdated or unsupported beliefs for review
     * 
     * @return List of conflict reports detailing any inconsistencies found,
     *         or an empty list if all beliefs are consistent
     * @throws BeliefAnalysisException if the review process fails
     * 
     * @since 1.0
     */
    List<BeliefConflict> reviewAllBeliefs();
    
    /**
     * Reviews beliefs for a specific agent.
     * 
     * Performs a targeted belief consistency review limited to a particular
     * agent's knowledge base. This is useful for multi-tenant scenarios
     * where belief isolation is required.
     * 
     * @param agentId The agent whose beliefs should be reviewed
     * @return List of conflict reports for the specified agent
     * @throws IllegalArgumentException if agentId is null or empty
     * @throws BeliefAnalysisException if the review process fails
     * 
     * @since 1.0
     */
    List<BeliefConflict> reviewBeliefsForAgent(String agentId);
    
    /**
     * Analyzes multiple memories in batch for belief updates.
     * 
     * Efficiently processes multiple memory records in a single operation,
     * optimizing for scenarios where many memories need to be analyzed
     * simultaneously (e.g., during bulk import or periodic batch processing).
     * 
     * @param memories List of MemoryRecords to analyze
     * @return BeliefUpdateResult consolidating all belief changes from the batch
     * @throws IllegalArgumentException if memories is null or empty
     * @throws BeliefAnalysisException if the batch analysis fails
     * 
     * @since 1.0
     */
    BeliefUpdateResult analyzeBatch(List<MemoryRecord> memories);
    
    /**
     * Resolves a specific belief conflict.
     * 
     * Applies resolution logic to a detected conflict between beliefs or
     * between a belief and memory. The resolution strategy may involve:
     * - Taking the newer information
     * - Keeping the older belief
     * - Merging conflicting information
     * - Marking both as uncertain
     * - Flagging for manual review
     * 
     * @param conflict The conflict to resolve
     * @return Updated BeliefConflict with resolution details applied
     * @throws IllegalArgumentException if conflict is null
     * @throws BeliefAnalysisException if resolution fails
     * 
     * @since 1.0
     */
    BeliefConflict resolveConflict(BeliefConflict conflict);
    
    /**
     * Gets all active beliefs for a specific agent.
     * 
     * Retrieves the current set of beliefs held by the specified agent,
     * filtered to include only active (non-deprecated) beliefs.
     * 
     * @param agentId The agent whose beliefs to retrieve
     * @return List of active Belief objects for the agent
     * @throws IllegalArgumentException if agentId is null or empty
     * 
     * @since 1.0
     */
    List<Belief> getBeliefsForAgent(String agentId);
    
    /**
     * Gets beliefs in a specific category.
     * 
     * Retrieves beliefs that belong to a particular category or domain,
     * optionally filtered by agent.
     * 
     * @param category The belief category to filter by
     * @param agentId Optional agent ID filter (null for all agents)
     * @return List of beliefs in the specified category
     * @throws IllegalArgumentException if category is null or empty
     * 
     * @since 1.0
     */
    List<Belief> getBeliefsInCategory(String category, String agentId);
    
    /**
     * Finds beliefs related to specific content or query.
     * 
     * Searches for beliefs that are semantically related to the given
     * query content, useful for contextual belief retrieval.
     * 
     * @param queryContent The content to find related beliefs for
     * @param agentId Optional agent ID filter (null for all agents)
     * @param limit Maximum number of beliefs to return
     * @return List of related beliefs ordered by relevance
     * @throws IllegalArgumentException if queryContent is null or limit < 1
     * 
     * @since 1.0
     */
    List<Belief> findRelatedBeliefs(String queryContent, String agentId, int limit);
    
    /**
     * Gets all unresolved conflicts in the system.
     * 
     * Retrieves conflicts that have been detected but not yet resolved,
     * useful for monitoring and manual intervention workflows.
     * 
     * @param agentId Optional agent ID filter (null for all agents)
     * @return List of unresolved BeliefConflict objects
     * 
     * @since 1.0
     */
    List<BeliefConflict> getUnresolvedConflicts(String agentId);
    
    /**
     * Gets beliefs with low confidence scores.
     * 
     * Identifies beliefs that have confidence levels below a specified
     * threshold, indicating they may need additional evidence or review.
     * 
     * @param confidenceThreshold The confidence threshold (0.0 to 1.0)
     * @param agentId Optional agent ID filter (null for all agents)
     * @return List of low-confidence beliefs
     * @throws IllegalArgumentException if confidenceThreshold is outside valid range
     * 
     * @since 1.0
     */
    List<Belief> getLowConfidenceBeliefs(double confidenceThreshold, String agentId);
    
    /**
     * Updates the confidence level of a specific belief.
     * 
     * Manually adjusts the confidence score of a belief, useful for
     * incorporating external validation or correction feedback.
     * 
     * @param beliefId The ID of the belief to update
     * @param newConfidence The new confidence level (0.0 to 1.0)
     * @param reason Optional reason for the confidence change
     * @return The updated Belief object
     * @throws IllegalArgumentException if beliefId is null or confidence is invalid
     * @throws BeliefNotFoundException if the belief doesn't exist
     * 
     * @since 1.0
     */
    Belief updateBeliefConfidence(String beliefId, double newConfidence, String reason);
    
    /**
     * Deactivates a belief (marks it as deprecated).
     * 
     * Marks a belief as inactive without deleting it, preserving it for
     * audit purposes while removing it from active consideration.
     * 
     * @param beliefId The ID of the belief to deactivate
     * @param reason Optional reason for deactivation
     * @return The updated Belief object with active flag set to false
     * @throws IllegalArgumentException if beliefId is null
     * @throws BeliefNotFoundException if the belief doesn't exist
     * 
     * @since 1.0
     */
    Belief deactivateBelief(String beliefId, String reason);
    
    /**
     * Gets belief analysis statistics.
     * 
     * Returns comprehensive statistics about belief analysis operations
     * including:
     * - Total number of beliefs managed
     * - Belief confidence distribution
     * - Conflict detection rates
     * - Resolution success rates
     * - Category distribution
     * - Processing performance metrics
     * 
     * @return Map containing various belief analysis statistics
     * 
     * @since 1.0
     */
    Map<String, Object> getBeliefStatistics();
    
    /**
     * Gets statistics for a specific agent's beliefs.
     * 
     * Returns agent-specific belief statistics including:
     * - Total beliefs for the agent
     * - Confidence score distribution
     * - Category breakdown
     * - Conflict history
     * - Recent belief activity
     * 
     * @param agentId The agent ID to get statistics for
     * @return Map containing agent-specific belief statistics
     * @throws IllegalArgumentException if agentId is null or empty
     * 
     * @since 1.0
     */
    Map<String, Object> getAgentBeliefStatistics(String agentId);
    
    /**
     * Configures conflict resolution strategies.
     * 
     * Sets up rules and preferences for how different types of conflicts
     * should be automatically resolved. This allows customization of
     * the conflict resolution behavior.
     * 
     * @param strategies Map of conflict types to resolution strategies
     * @throws IllegalArgumentException if strategies is null
     * 
     * @since 1.0
     */
    void configureResolutionStrategies(Map<String, String> strategies);
    
    /**
     * Checks if the belief analysis system is healthy and ready.
     * 
     * Performs a health check of the belief analysis system including:
     * - Belief storage connectivity
     * - Analysis engine availability
     * - Memory and processing capacity
     * - Configuration validity
     * 
     * @return true if the system is healthy and ready to process requests
     * 
     * @since 1.0
     */
    boolean isHealthy();
    
    /**
     * Exception thrown when belief analysis operations fail.
     */
    public static class BeliefAnalysisException extends RuntimeException {
        public BeliefAnalysisException(String message) {
            super(message);
        }
        
        public BeliefAnalysisException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Exception thrown when a requested belief cannot be found.
     */
    public static class BeliefNotFoundException extends Exception {
        private final String beliefId;
        
        public BeliefNotFoundException(String message, String beliefId) {
            super(message);
            this.beliefId = beliefId;
        }
        
        public String getBeliefId() {
            return beliefId;
        }
    }
}