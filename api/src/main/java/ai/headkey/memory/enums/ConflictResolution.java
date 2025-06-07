package ai.headkey.memory.enums;

/**
 * Enumeration of different conflict resolution strategies that can be applied
 * when the Belief Reinforcement & Conflict Analyzer (BRCA) detects conflicts
 * between memories and existing beliefs.
 */
public enum ConflictResolution {
    /**
     * Accept the new information and update/replace the conflicting belief.
     * The new memory takes precedence over the existing belief.
     */
    TAKE_NEW,
    
    /**
     * Keep the existing belief and mark the new memory as potentially incorrect.
     * The existing belief takes precedence over the new memory.
     */
    KEEP_OLD,
    
    /**
     * Mark both the belief and memory as uncertain due to the conflict.
     * Reduces confidence levels for both conflicting pieces of information.
     */
    MARK_UNCERTAIN,
    
    /**
     * Flag the conflict for manual review by a human operator or higher-level system.
     * No automatic resolution is applied; requires external intervention.
     */
    REQUIRE_MANUAL_REVIEW,
    
    /**
     * Attempt to merge or synthesize the conflicting information.
     * Creates a combined belief that incorporates aspects of both sources.
     */
    MERGE,
    
    /**
     * Archive the older information while keeping the newer information active.
     * Maintains historical context while prioritizing recent data.
     */
    ARCHIVE_OLD
}