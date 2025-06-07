package ai.headkey.memory.enums;

/**
 * Enumeration of different forgetting strategies that can be applied
 * by the Relevance Evaluation & Forgetting Agent (REFA).
 */
public enum ForgettingStrategyType {
    /**
     * Remove memories older than a specified age threshold.
     * Uses time-based criteria for forgetting.
     */
    AGE,
    
    /**
     * Remove memories that have been accessed least frequently.
     * Retains the most frequently accessed memories up to a specified count.
     */
    LEAST_USED,
    
    /**
     * Remove memories with relevance scores below a specified threshold.
     * Uses computed relevance scores for forgetting decisions.
     */
    LOW_SCORE,
    
    /**
     * Apply a custom forgetting strategy.
     * Allows for extensible, user-defined forgetting logic.
     */
    CUSTOM
}