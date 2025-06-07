package ai.headkey.memory.dto;

import ai.headkey.memory.enums.ConflictResolution;
import java.time.Instant;
import java.util.Objects;

/**
 * Data Transfer Object representing a conflict between beliefs and memories
 * detected by the Belief Reinforcement & Conflict Analyzer (BRCA).
 * Contains information about the conflicting entities and the resolution applied.
 */
public class BeliefConflict {
    
    /**
     * Unique identifier for this conflict.
     */
    private String conflictId;
    
    /**
     * ID of the belief involved in the conflict.
     */
    private String beliefId;
    
    /**
     * ID of the memory that conflicts with the belief.
     * May be null if the conflict is between two beliefs.
     */
    private String memoryId;
    
    /**
     * ID of another belief if the conflict is between two beliefs.
     * May be null if the conflict is between a belief and a memory.
     */
    private String conflictingBeliefId;
    
    /**
     * Agent ID associated with this conflict.
     */
    private String agentId;
    
    /**
     * Human-readable description of the conflict.
     * Explains what conflicting information was detected and why it's problematic.
     */
    private String description;
    
    /**
     * The resolution strategy applied to handle this conflict.
     */
    private ConflictResolution resolution;
    
    /**
     * Detailed explanation of how the conflict was resolved.
     */
    private String resolutionDetails;
    
    /**
     * Confidence score for the resolution decision (0.0 to 1.0).
     * Higher values indicate more confidence in the resolution choice.
     */
    private double resolutionConfidence;
    
    /**
     * Timestamp when this conflict was detected.
     */
    private Instant detectedAt;
    
    /**
     * Timestamp when this conflict was resolved.
     * May be null if the conflict is still pending resolution.
     */
    private Instant resolvedAt;
    
    /**
     * Flag indicating whether this conflict has been resolved.
     */
    private boolean resolved;
    
    /**
     * Severity level of the conflict (e.g., "LOW", "MEDIUM", "HIGH", "CRITICAL").
     */
    private String severity;
    
    /**
     * Default constructor.
     */
    public BeliefConflict() {
        this.detectedAt = Instant.now();
        this.resolved = false;
        this.resolutionConfidence = 0.0;
        this.severity = "MEDIUM";
    }
    
    /**
     * Constructor for belief-memory conflict.
     * 
     * @param beliefId The conflicting belief ID
     * @param memoryId The conflicting memory ID
     * @param description Description of the conflict
     */
    public BeliefConflict(String beliefId, String memoryId, String description) {
        this();
        this.beliefId = beliefId;
        this.memoryId = memoryId;
        this.description = description;
    }
    
    /**
     * Constructor for belief-belief conflict.
     * 
     * @param beliefId The first belief ID
     * @param conflictingBeliefId The second belief ID
     * @param description Description of the conflict
     */
    public BeliefConflict(String beliefId, String conflictingBeliefId, String description, boolean isBeliefsConflict) {
        this();
        this.beliefId = beliefId;
        this.conflictingBeliefId = conflictingBeliefId;
        this.description = description;
    }
    
    /**
     * Full constructor.
     * 
     * @param conflictId Unique conflict identifier
     * @param beliefId The belief ID
     * @param memoryId The memory ID (optional)
     * @param conflictingBeliefId The conflicting belief ID (optional)
     * @param agentId The agent ID
     * @param description Conflict description
     * @param resolution The resolution strategy
     */
    public BeliefConflict(String conflictId, String beliefId, String memoryId, String conflictingBeliefId,
                         String agentId, String description, ConflictResolution resolution) {
        this();
        this.conflictId = conflictId;
        this.beliefId = beliefId;
        this.memoryId = memoryId;
        this.conflictingBeliefId = conflictingBeliefId;
        this.agentId = agentId;
        this.description = description;
        this.resolution = resolution;
    }
    
    /**
     * Marks this conflict as resolved with the given resolution strategy.
     * 
     * @param resolution The resolution strategy applied
     * @param resolutionDetails Details about how the conflict was resolved
     * @param confidence Confidence in the resolution (0.0 to 1.0)
     */
    public void markResolved(ConflictResolution resolution, String resolutionDetails, double confidence) {
        this.resolution = resolution;
        this.resolutionDetails = resolutionDetails;
        this.resolutionConfidence = Math.max(0.0, Math.min(1.0, confidence));
        this.resolvedAt = Instant.now();
        this.resolved = true;
    }
    
    /**
     * Marks this conflict as resolved with the given resolution strategy.
     * 
     * @param resolution The resolution strategy applied
     * @param resolutionDetails Details about how the conflict was resolved
     */
    public void markResolved(ConflictResolution resolution, String resolutionDetails) {
        markResolved(resolution, resolutionDetails, 1.0);
    }
    
    /**
     * Checks if this conflict involves a memory (vs. only beliefs).
     * 
     * @return true if memoryId is not null
     */
    public boolean involvesMemory() {
        return memoryId != null;
    }
    
    /**
     * Checks if this conflict is between two beliefs.
     * 
     * @return true if conflictingBeliefId is not null
     */
    public boolean isBeliefsConflict() {
        return conflictingBeliefId != null;
    }
    
    /**
     * Checks if this is a high-severity conflict.
     * 
     * @return true if severity is "HIGH" or "CRITICAL"
     */
    public boolean isHighSeverity() {
        return "HIGH".equalsIgnoreCase(severity) || "CRITICAL".equalsIgnoreCase(severity);
    }
    
    /**
     * Checks if the resolution requires manual intervention.
     * 
     * @return true if resolution is REQUIRE_MANUAL_REVIEW
     */
    public boolean requiresManualReview() {
        return resolution == ConflictResolution.REQUIRE_MANUAL_REVIEW;
    }
    
    /**
     * Gets the age of this conflict in seconds.
     * 
     * @return Age in seconds since detection
     */
    public long getAgeInSeconds() {
        if (detectedAt == null) {
            return 0;
        }
        return Instant.now().getEpochSecond() - detectedAt.getEpochSecond();
    }
    
    /**
     * Gets the time taken to resolve this conflict in seconds.
     * 
     * @return Resolution time in seconds, or -1 if not resolved
     */
    public long getResolutionTimeInSeconds() {
        if (detectedAt == null || resolvedAt == null) {
            return -1;
        }
        return resolvedAt.getEpochSecond() - detectedAt.getEpochSecond();
    }
    
    // Getters and Setters
    
    public String getConflictId() {
        return conflictId;
    }
    
    public void setConflictId(String conflictId) {
        this.conflictId = conflictId;
    }
    
    public String getBeliefId() {
        return beliefId;
    }
    
    public void setBeliefId(String beliefId) {
        this.beliefId = beliefId;
    }
    
    public String getMemoryId() {
        return memoryId;
    }
    
    public void setMemoryId(String memoryId) {
        this.memoryId = memoryId;
    }
    
    public String getConflictingBeliefId() {
        return conflictingBeliefId;
    }
    
    public void setConflictingBeliefId(String conflictingBeliefId) {
        this.conflictingBeliefId = conflictingBeliefId;
    }
    
    public String getAgentId() {
        return agentId;
    }
    
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public ConflictResolution getResolution() {
        return resolution;
    }
    
    public void setResolution(ConflictResolution resolution) {
        this.resolution = resolution;
    }
    
    public String getResolutionDetails() {
        return resolutionDetails;
    }
    
    public void setResolutionDetails(String resolutionDetails) {
        this.resolutionDetails = resolutionDetails;
    }
    
    public double getResolutionConfidence() {
        return resolutionConfidence;
    }
    
    public void setResolutionConfidence(double resolutionConfidence) {
        this.resolutionConfidence = Math.max(0.0, Math.min(1.0, resolutionConfidence));
    }
    
    public Instant getDetectedAt() {
        return detectedAt;
    }
    
    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }
    
    public Instant getResolvedAt() {
        return resolvedAt;
    }
    
    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
    
    public boolean isResolved() {
        return resolved;
    }
    
    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeliefConflict that = (BeliefConflict) o;
        return Double.compare(that.resolutionConfidence, resolutionConfidence) == 0 &&
                resolved == that.resolved &&
                Objects.equals(conflictId, that.conflictId) &&
                Objects.equals(beliefId, that.beliefId) &&
                Objects.equals(memoryId, that.memoryId) &&
                Objects.equals(conflictingBeliefId, that.conflictingBeliefId) &&
                Objects.equals(agentId, that.agentId) &&
                Objects.equals(description, that.description) &&
                resolution == that.resolution &&
                Objects.equals(resolutionDetails, that.resolutionDetails) &&
                Objects.equals(detectedAt, that.detectedAt) &&
                Objects.equals(resolvedAt, that.resolvedAt) &&
                Objects.equals(severity, that.severity);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(conflictId, beliefId, memoryId, conflictingBeliefId, agentId, description, 
                          resolution, resolutionDetails, resolutionConfidence, detectedAt, resolvedAt, 
                          resolved, severity);
    }
    
    @Override
    public String toString() {
        return "BeliefConflict{" +
                "conflictId='" + conflictId + '\'' +
                ", beliefId='" + beliefId + '\'' +
                ", memoryId='" + memoryId + '\'' +
                ", conflictingBeliefId='" + conflictingBeliefId + '\'' +
                ", agentId='" + agentId + '\'' +
                ", description='" + description + '\'' +
                ", resolution=" + resolution +
                ", resolutionDetails='" + resolutionDetails + '\'' +
                ", resolutionConfidence=" + resolutionConfidence +
                ", detectedAt=" + detectedAt +
                ", resolvedAt=" + resolvedAt +
                ", resolved=" + resolved +
                ", severity='" + severity + '\'' +
                '}';
    }
}