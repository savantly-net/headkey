package ai.headkey.memory.langchain4j.dto;

import java.util.Objects;

/**
 * Data Transfer Object for conflict detection response results.
 * 
 * This class represents the result of a conflict detection operation from AI services,
 * containing information about whether two statements conflict, the confidence in that
 * assessment, the type of conflict detected, and the reasoning behind the determination.
 * 
 * Used by ConflictDetectionAiService implementations to return structured
 * conflict analysis results from Large Language Models.
 * 
 * @since 1.0
 */
public class ConflictDetectionResponse {
    
    /**
     * Whether the two statements are conflicting.
     * True indicates a conflict was detected, false indicates no conflict.
     */
    private boolean conflicting;
    
    /**
     * Confidence score for the conflict detection (0.0 to 1.0).
     * Higher values indicate greater confidence in the conflict assessment.
     */
    private double confidence;
    
    /**
     * The type of conflict detected, if any.
     * Common types: direct_contradiction, mutual_exclusion, logical_inconsistency, temporal_conflict, none.
     */
    private String conflictType;
    
    /**
     * AI-generated reasoning explaining the conflict detection decision.
     * Provides transparency into the conflict analysis process.
     */
    private String reasoning;
    
    /**
     * Default constructor.
     */
    public ConflictDetectionResponse() {
        this.conflicting = false;
        this.confidence = 0.0;
        this.conflictType = "none";
    }
    
    /**
     * Constructor with all fields.
     * 
     * @param conflicting Whether statements conflict
     * @param confidence The confidence score (0.0 to 1.0)
     * @param conflictType The type of conflict
     * @param reasoning The reasoning for the assessment
     */
    public ConflictDetectionResponse(boolean conflicting, double confidence, String conflictType, String reasoning) {
        this.conflicting = conflicting;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence)); // Clamp to valid range
        this.conflictType = conflictType != null ? conflictType : "none";
        this.reasoning = reasoning;
    }
    
    /**
     * Constructor for conflict detection with default type.
     * 
     * @param conflicting Whether statements conflict
     * @param confidence The confidence score (0.0 to 1.0)
     * @param reasoning The reasoning for the assessment
     */
    public ConflictDetectionResponse(boolean conflicting, double confidence, String reasoning) {
        this(conflicting, confidence, conflicting ? "unknown" : "none", reasoning);
    }
    
    /**
     * Checks if the statements are conflicting.
     * 
     * @return true if statements conflict
     */
    public boolean isConflicting() {
        return conflicting;
    }
    
    /**
     * Sets whether the statements are conflicting.
     * 
     * @param conflicting true if statements conflict
     */
    public void setConflicting(boolean conflicting) {
        this.conflicting = conflicting;
    }
    
    /**
     * Gets the confidence score.
     * 
     * @return The confidence (0.0 to 1.0)
     */
    public double getConfidence() {
        return confidence;
    }
    
    /**
     * Sets the confidence score.
     * 
     * @param confidence The confidence to set (will be clamped to 0.0-1.0)
     */
    public void setConfidence(double confidence) {
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }
    
    /**
     * Gets the conflict type.
     * 
     * @return The conflict type
     */
    public String getConflictType() {
        return conflictType;
    }
    
    /**
     * Sets the conflict type.
     * 
     * @param conflictType The conflict type to set
     */
    public void setConflictType(String conflictType) {
        this.conflictType = conflictType != null ? conflictType : "none";
    }
    
    /**
     * Gets the reasoning for the conflict assessment.
     * 
     * @return The reasoning text
     */
    public String getReasoning() {
        return reasoning;
    }
    
    /**
     * Sets the reasoning for the conflict assessment.
     * 
     * @param reasoning The reasoning to set
     */
    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
    
    /**
     * Checks if this is a high-confidence assessment.
     * 
     * @param threshold The confidence threshold
     * @return true if confidence is above threshold
     */
    public boolean isHighConfidence(double threshold) {
        return confidence >= threshold;
    }
    
    /**
     * Checks if this is a high-confidence assessment using default threshold of 0.8.
     * 
     * @return true if confidence is above 0.8
     */
    public boolean isHighConfidence() {
        return isHighConfidence(0.8);
    }
    
    /**
     * Checks if this is a low-confidence assessment.
     * 
     * @param threshold The confidence threshold
     * @return true if confidence is below threshold
     */
    public boolean isLowConfidence(double threshold) {
        return confidence < threshold;
    }
    
    /**
     * Checks if this is a low-confidence assessment using default threshold of 0.3.
     * 
     * @return true if confidence is below 0.3
     */
    public boolean isLowConfidence() {
        return isLowConfidence(0.3);
    }
    
    /**
     * Checks if reasoning is provided.
     * 
     * @return true if reasoning is not null and not empty
     */
    public boolean hasReasoning() {
        return reasoning != null && !reasoning.trim().isEmpty();
    }
    
    /**
     * Checks if a specific conflict type was detected.
     * 
     * @return true if conflict type is not "none"
     */
    public boolean hasConflictType() {
        return conflictType != null && !"none".equals(conflictType);
    }
    
    /**
     * Checks if this is a direct contradiction conflict.
     * 
     * @return true if conflict type is direct_contradiction
     */
    public boolean isDirectContradiction() {
        return "direct_contradiction".equals(conflictType);
    }
    
    /**
     * Checks if this is a mutual exclusion conflict.
     * 
     * @return true if conflict type is mutual_exclusion
     */
    public boolean isMutualExclusion() {
        return "mutual_exclusion".equals(conflictType);
    }
    
    /**
     * Checks if this is a logical inconsistency conflict.
     * 
     * @return true if conflict type is logical_inconsistency
     */
    public boolean isLogicalInconsistency() {
        return "logical_inconsistency".equals(conflictType);
    }
    
    /**
     * Checks if this is a temporal conflict.
     * 
     * @return true if conflict type is temporal_conflict
     */
    public boolean isTemporalConflict() {
        return "temporal_conflict".equals(conflictType);
    }
    
    /**
     * Gets a descriptive conflict assessment.
     * 
     * @return A string describing the conflict assessment
     */
    public String getConflictAssessment() {
        if (!conflicting) {
            return "No Conflict";
        }
        
        String baseAssessment = switch (conflictType) {
            case "direct_contradiction" -> "Direct Contradiction";
            case "mutual_exclusion" -> "Mutual Exclusion";
            case "logical_inconsistency" -> "Logical Inconsistency";
            case "temporal_conflict" -> "Temporal Conflict";
            default -> "Conflict Detected";
        };
        
        String confidenceLevel = confidence >= 0.8 ? " (High Confidence)" :
                                confidence >= 0.5 ? " (Medium Confidence)" :
                                " (Low Confidence)";
        
        return baseAssessment + confidenceLevel;
    }
    
    /**
     * Creates a response indicating no conflict.
     * 
     * @param reasoning The reasoning for no conflict
     * @return ConflictDetectionResponse with no conflict
     */
    public static ConflictDetectionResponse noConflict(String reasoning) {
        return new ConflictDetectionResponse(false, 0.9, "none", reasoning);
    }
    
    /**
     * Creates a response indicating a direct contradiction.
     * 
     * @param confidence The confidence in the assessment
     * @param reasoning The reasoning for the contradiction
     * @return ConflictDetectionResponse with direct contradiction
     */
    public static ConflictDetectionResponse directContradiction(double confidence, String reasoning) {
        return new ConflictDetectionResponse(true, confidence, "direct_contradiction", reasoning);
    }
    
    /**
     * Creates a response indicating mutual exclusion.
     * 
     * @param confidence The confidence in the assessment
     * @param reasoning The reasoning for the mutual exclusion
     * @return ConflictDetectionResponse with mutual exclusion
     */
    public static ConflictDetectionResponse mutualExclusion(double confidence, String reasoning) {
        return new ConflictDetectionResponse(true, confidence, "mutual_exclusion", reasoning);
    }
    
    /**
     * Creates a response indicating logical inconsistency.
     * 
     * @param confidence The confidence in the assessment
     * @param reasoning The reasoning for the inconsistency
     * @return ConflictDetectionResponse with logical inconsistency
     */
    public static ConflictDetectionResponse logicalInconsistency(double confidence, String reasoning) {
        return new ConflictDetectionResponse(true, confidence, "logical_inconsistency", reasoning);
    }
    
    /**
     * Creates a response indicating temporal conflict.
     * 
     * @param confidence The confidence in the assessment
     * @param reasoning The reasoning for the temporal conflict
     * @return ConflictDetectionResponse with temporal conflict
     */
    public static ConflictDetectionResponse temporalConflict(double confidence, String reasoning) {
        return new ConflictDetectionResponse(true, confidence, "temporal_conflict", reasoning);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConflictDetectionResponse that = (ConflictDetectionResponse) o;
        return conflicting == that.conflicting &&
                Double.compare(that.confidence, confidence) == 0 &&
                Objects.equals(conflictType, that.conflictType) &&
                Objects.equals(reasoning, that.reasoning);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(conflicting, confidence, conflictType, reasoning);
    }
    
    @Override
    public String toString() {
        return "ConflictDetectionResponse{" +
                "conflicting=" + conflicting +
                ", confidence=" + confidence +
                ", conflictType='" + conflictType + '\'' +
                ", reasoning='" + reasoning + '\'' +
                ", assessment='" + getConflictAssessment() + '\'' +
                '}';
    }
}