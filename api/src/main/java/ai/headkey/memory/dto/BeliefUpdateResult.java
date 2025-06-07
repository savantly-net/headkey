package ai.headkey.memory.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Data Transfer Object representing the result of a belief update operation
 * performed by the Belief Reinforcement & Conflict Analyzer (BRCA).
 * Contains information about beliefs that were reinforced, conflicts detected,
 * and new beliefs created during the analysis.
 */
public class BeliefUpdateResult {
    
    /**
     * List of beliefs that were reinforced or strengthened during the analysis.
     * These are existing beliefs that received additional supporting evidence.
     */
    private List<Belief> reinforcedBeliefs;
    
    /**
     * List of conflicts detected between memories and existing beliefs.
     * Each conflict includes details about the conflicting entities and resolution.
     */
    private List<BeliefConflict> conflicts;
    
    /**
     * List of entirely new beliefs that were created during the analysis.
     * These beliefs were formed from new information that didn't conflict
     * with existing knowledge.
     */
    private List<Belief> newBeliefs;
    
    /**
     * List of beliefs that were weakened or had their confidence reduced.
     * This can happen when conflicting information is encountered.
     */
    private List<Belief> weakenedBeliefs;
    
    /**
     * Agent ID for which this belief update was performed.
     */
    private String agentId;
    
    /**
     * Timestamp when the belief analysis was performed.
     */
    private Instant analysisTimestamp;
    
    /**
     * Processing time for the belief analysis in milliseconds.
     */
    private Long processingTimeMs;
    
    /**
     * Total number of beliefs examined during the analysis.
     */
    private int totalBeliefsExamined;
    
    /**
     * Number of memories that were considered in the analysis.
     */
    private int memoriesAnalyzed;
    
    /**
     * Overall confidence score for the analysis results (0.0 to 1.0).
     */
    private double overallConfidence;
    
    /**
     * Default constructor.
     */
    public BeliefUpdateResult() {
        this.reinforcedBeliefs = new ArrayList<>();
        this.conflicts = new ArrayList<>();
        this.newBeliefs = new ArrayList<>();
        this.weakenedBeliefs = new ArrayList<>();
        this.analysisTimestamp = Instant.now();
        this.overallConfidence = 1.0;
        this.totalBeliefsExamined = 0;
        this.memoriesAnalyzed = 0;
    }
    
    /**
     * Constructor with agent ID.
     * 
     * @param agentId The agent for which beliefs were updated
     */
    public BeliefUpdateResult(String agentId) {
        this();
        this.agentId = agentId;
    }
    
    /**
     * Full constructor.
     * 
     * @param agentId The agent ID
     * @param reinforcedBeliefs List of reinforced beliefs
     * @param conflicts List of detected conflicts
     * @param newBeliefs List of new beliefs created
     */
    public BeliefUpdateResult(String agentId, List<Belief> reinforcedBeliefs, 
                             List<BeliefConflict> conflicts, List<Belief> newBeliefs) {
        this(agentId);
        this.reinforcedBeliefs = new ArrayList<>(reinforcedBeliefs != null ? reinforcedBeliefs : new ArrayList<>());
        this.conflicts = new ArrayList<>(conflicts != null ? conflicts : new ArrayList<>());
        this.newBeliefs = new ArrayList<>(newBeliefs != null ? newBeliefs : new ArrayList<>());
    }
    
    /**
     * Adds a reinforced belief to the result.
     * 
     * @param belief The belief that was reinforced
     */
    public void addReinforcedBelief(Belief belief) {
        if (belief != null) {
            if (reinforcedBeliefs == null) {
                reinforcedBeliefs = new ArrayList<>();
            }
            reinforcedBeliefs.add(belief);
        }
    }
    
    /**
     * Adds a conflict to the result.
     * 
     * @param conflict The conflict that was detected
     */
    public void addConflict(BeliefConflict conflict) {
        if (conflict != null) {
            if (conflicts == null) {
                conflicts = new ArrayList<>();
            }
            conflicts.add(conflict);
        }
    }
    
    /**
     * Adds a new belief to the result.
     * 
     * @param belief The new belief that was created
     */
    public void addNewBelief(Belief belief) {
        if (belief != null) {
            if (newBeliefs == null) {
                newBeliefs = new ArrayList<>();
            }
            newBeliefs.add(belief);
        }
    }
    
    /**
     * Adds a weakened belief to the result.
     * 
     * @param belief The belief that was weakened
     */
    public void addWeakenedBelief(Belief belief) {
        if (belief != null) {
            if (weakenedBeliefs == null) {
                weakenedBeliefs = new ArrayList<>();
            }
            weakenedBeliefs.add(belief);
        }
    }
    
    /**
     * Checks if any beliefs were modified (reinforced, weakened, or created).
     * 
     * @return true if any beliefs were changed
     */
    public boolean hasBeliefsChanged() {
        return !getReinforced().isEmpty() || !getNewBeliefsInternal().isEmpty() || !getWeakenedBeliefsInternal().isEmpty();
    }
    
    /**
     * Checks if any conflicts were detected.
     * 
     * @return true if conflicts were found
     */
    public boolean hasConflicts() {
        return conflicts != null && !conflicts.isEmpty();
    }
    
    /**
     * Gets the total number of belief changes (reinforced + new + weakened).
     * 
     * @return Total count of belief modifications
     */
    public int getTotalBeliefChanges() {
        return getReinforced().size() + getNewBeliefsInternal().size() + getWeakenedBeliefsInternal().size();
    }
    
    /**
     * Gets the count of unresolved conflicts.
     * 
     * @return Number of conflicts that still require resolution
     */
    public int getUnresolvedConflictCount() {
        if (getConflictsInternal() == null) {
            return 0;
        }
        return (int) getConflictsInternal().stream().filter(c -> !c.isResolved()).count();
    }
    
    /**
     * Gets the count of high-severity conflicts.
     * 
     * @return Number of high-severity conflicts
     */
    public int getHighSeverityConflictCount() {
        if (getConflictsInternal() == null) {
            return 0;
        }
        return (int) getConflictsInternal().stream().filter(BeliefConflict::isHighSeverity).count();
    }
    
    /**
     * Sets the processing time based on start time.
     * 
     * @param startTime The time when processing began
     */
    public void setProcessingTime(Instant startTime) {
        if (startTime != null) {
            this.processingTimeMs = Instant.now().toEpochMilli() - startTime.toEpochMilli();
        }
    }
    
    /**
     * Creates a summary string of the analysis results.
     * 
     * @return A human-readable summary
     */
    public String getSummary() {
        return String.format(
            "Belief Analysis Summary: %d reinforced, %d new, %d weakened, %d conflicts detected (%d unresolved)",
            getReinforced().size(),
            getNewBeliefsInternal().size(), 
            getWeakenedBeliefsInternal().size(),
            getConflictsInternal().size(),
            getUnresolvedConflictCount()
        );
    }
    
    // Helper methods to safely get lists
    private List<Belief> getReinforced() {
        return reinforcedBeliefs != null ? reinforcedBeliefs : new ArrayList<>();
    }
    
    private List<Belief> getNewBeliefsInternal() {
        return newBeliefs != null ? newBeliefs : new ArrayList<>();
    }
    
    private List<Belief> getWeakenedBeliefsInternal() {
        return weakenedBeliefs != null ? weakenedBeliefs : new ArrayList<>();
    }
    
    private List<BeliefConflict> getConflictsInternal() {
        return conflicts != null ? conflicts : new ArrayList<>();
    }
    
    // Getters and Setters
    
    public List<Belief> getReinforcedBeliefs() {
        return reinforcedBeliefs != null ? new ArrayList<>(reinforcedBeliefs) : new ArrayList<>();
    }
    
    public void setReinforcedBeliefs(List<Belief> reinforcedBeliefs) {
        this.reinforcedBeliefs = new ArrayList<>(reinforcedBeliefs != null ? reinforcedBeliefs : new ArrayList<>());
    }
    
    public List<BeliefConflict> getConflicts() {
        return conflicts != null ? new ArrayList<>(conflicts) : new ArrayList<>();
    }
    
    public void setConflicts(List<BeliefConflict> conflicts) {
        this.conflicts = new ArrayList<>(conflicts != null ? conflicts : new ArrayList<>());
    }
    
    public List<Belief> getNewBeliefs() {
        return newBeliefs != null ? new ArrayList<>(newBeliefs) : new ArrayList<>();
    }
    
    public void setNewBeliefs(List<Belief> newBeliefs) {
        this.newBeliefs = new ArrayList<>(newBeliefs != null ? newBeliefs : new ArrayList<>());
    }
    
    public List<Belief> getWeakenedBeliefs() {
        return weakenedBeliefs != null ? new ArrayList<>(weakenedBeliefs) : new ArrayList<>();
    }
    
    public void setWeakenedBeliefs(List<Belief> weakenedBeliefs) {
        this.weakenedBeliefs = new ArrayList<>(weakenedBeliefs != null ? weakenedBeliefs : new ArrayList<>());
    }
    
    public String getAgentId() {
        return agentId;
    }
    
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }
    
    public Instant getAnalysisTimestamp() {
        return analysisTimestamp;
    }
    
    public void setAnalysisTimestamp(Instant analysisTimestamp) {
        this.analysisTimestamp = analysisTimestamp;
    }
    
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public int getTotalBeliefsExamined() {
        return totalBeliefsExamined;
    }
    
    public void setTotalBeliefsExamined(int totalBeliefsExamined) {
        this.totalBeliefsExamined = totalBeliefsExamined;
    }
    
    public int getMemoriesAnalyzed() {
        return memoriesAnalyzed;
    }
    
    public void setMemoriesAnalyzed(int memoriesAnalyzed) {
        this.memoriesAnalyzed = memoriesAnalyzed;
    }
    
    public double getOverallConfidence() {
        return overallConfidence;
    }
    
    public void setOverallConfidence(double overallConfidence) {
        this.overallConfidence = Math.max(0.0, Math.min(1.0, overallConfidence));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeliefUpdateResult that = (BeliefUpdateResult) o;
        return totalBeliefsExamined == that.totalBeliefsExamined &&
                memoriesAnalyzed == that.memoriesAnalyzed &&
                Double.compare(that.overallConfidence, overallConfidence) == 0 &&
                Objects.equals(reinforcedBeliefs, that.reinforcedBeliefs) &&
                Objects.equals(conflicts, that.conflicts) &&
                Objects.equals(newBeliefs, that.newBeliefs) &&
                Objects.equals(weakenedBeliefs, that.weakenedBeliefs) &&
                Objects.equals(agentId, that.agentId) &&
                Objects.equals(analysisTimestamp, that.analysisTimestamp) &&
                Objects.equals(processingTimeMs, that.processingTimeMs);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(reinforcedBeliefs, conflicts, newBeliefs, weakenedBeliefs, agentId, 
                          analysisTimestamp, processingTimeMs, totalBeliefsExamined, memoriesAnalyzed, 
                          overallConfidence);
    }
    
    @Override
    public String toString() {
        return "BeliefUpdateResult{" +
                "reinforcedBeliefs=" + (reinforcedBeliefs != null ? reinforcedBeliefs.size() : 0) + " items" +
                ", conflicts=" + (conflicts != null ? conflicts.size() : 0) + " items" +
                ", newBeliefs=" + (newBeliefs != null ? newBeliefs.size() : 0) + " items" +
                ", weakenedBeliefs=" + (weakenedBeliefs != null ? weakenedBeliefs.size() : 0) + " items" +
                ", agentId='" + agentId + '\'' +
                ", analysisTimestamp=" + analysisTimestamp +
                ", processingTimeMs=" + processingTimeMs +
                ", totalBeliefsExamined=" + totalBeliefsExamined +
                ", memoriesAnalyzed=" + memoriesAnalyzed +
                ", overallConfidence=" + overallConfidence +
                '}';
    }
}