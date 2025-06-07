package ai.headkey.memory.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Data Transfer Object representing the results of a forgetting operation
 * performed by the Relevance Evaluation & Forgetting Agent (REFA).
 * Contains information about which memories were removed, retained, and
 * the strategy that was applied.
 */
public class ForgettingReport {
    
    /**
     * Number of memories that were removed during the forgetting operation.
     */
    private int removedCount;
    
    /**
     * List of memory IDs that were removed.
     * Useful for logging, auditing, or potential recovery operations.
     */
    private List<String> removedIds;
    
    /**
     * Number of memories that were retained after the forgetting operation.
     */
    private int remainingCount;
    
    /**
     * The forgetting strategy that was applied during this operation.
     */
    private ForgettingStrategy appliedStrategy;
    
    /**
     * Agent ID for which the forgetting operation was performed.
     */
    private String agentId;
    
    /**
     * Timestamp when the forgetting operation was executed.
     */
    private Instant executionTimestamp;
    
    /**
     * Processing time for the forgetting operation in milliseconds.
     */
    private Long processingTimeMs;
    
    /**
     * Total number of memories that were evaluated for potential removal.
     */
    private int totalEvaluated;
    
    /**
     * Number of memories that were candidates for removal but were retained
     * due to various protection criteria or thresholds.
     */
    private int protectedCount;
    
    /**
     * Flag indicating whether this was a dry run (no actual deletion performed).
     */
    private boolean dryRun;
    
    /**
     * Optional error message if the forgetting operation encountered issues.
     */
    private String errorMessage;
    
    /**
     * List of categories affected by the forgetting operation.
     */
    private List<String> affectedCategories;
    
    /**
     * Amount of storage space freed by the operation (in bytes, if available).
     */
    private Long bytesFreed;
    
    /**
     * Default constructor.
     */
    public ForgettingReport() {
        this.removedIds = new ArrayList<>();
        this.affectedCategories = new ArrayList<>();
        this.executionTimestamp = Instant.now();
        this.dryRun = false;
        this.removedCount = 0;
        this.remainingCount = 0;
        this.totalEvaluated = 0;
        this.protectedCount = 0;
    }
    
    /**
     * Constructor with basic counts.
     * 
     * @param removedCount Number of memories removed
     * @param remainingCount Number of memories remaining
     */
    public ForgettingReport(int removedCount, int remainingCount) {
        this();
        this.removedCount = removedCount;
        this.remainingCount = remainingCount;
    }
    
    /**
     * Constructor with strategy and counts.
     * 
     * @param appliedStrategy The strategy that was applied
     * @param removedCount Number of memories removed
     * @param remainingCount Number of memories remaining
     */
    public ForgettingReport(ForgettingStrategy appliedStrategy, int removedCount, int remainingCount) {
        this(removedCount, remainingCount);
        this.appliedStrategy = appliedStrategy;
        this.dryRun = appliedStrategy != null && appliedStrategy.isDryRun();
    }
    
    /**
     * Full constructor.
     * 
     * @param removedCount Number of memories removed
     * @param removedIds List of removed memory IDs
     * @param remainingCount Number of memories remaining
     * @param appliedStrategy The applied strategy
     * @param agentId The agent ID
     */
    public ForgettingReport(int removedCount, List<String> removedIds, int remainingCount, 
                           ForgettingStrategy appliedStrategy, String agentId) {
        this(appliedStrategy, removedCount, remainingCount);
        this.removedIds = new ArrayList<>(removedIds != null ? removedIds : new ArrayList<>());
        this.agentId = agentId;
    }
    
    /**
     * Adds a removed memory ID to the report.
     * 
     * @param memoryId The ID of the removed memory
     */
    public void addRemovedId(String memoryId) {
        if (memoryId != null && !memoryId.trim().isEmpty()) {
            if (removedIds == null) {
                removedIds = new ArrayList<>();
            }
            removedIds.add(memoryId);
            removedCount = removedIds.size(); // Keep count in sync
        }
    }
    
    /**
     * Adds an affected category to the report.
     * 
     * @param category The category that was affected
     */
    public void addAffectedCategory(String category) {
        if (category != null && !category.trim().isEmpty()) {
            if (affectedCategories == null) {
                affectedCategories = new ArrayList<>();
            }
            if (!affectedCategories.contains(category)) {
                affectedCategories.add(category);
            }
        }
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
     * Calculates the removal rate as a percentage of evaluated memories.
     * 
     * @return Removal rate (0.0 to 100.0), or 0.0 if no memories were evaluated
     */
    public double getRemovalRate() {
        if (totalEvaluated <= 0) {
            return 0.0;
        }
        return (double) removedCount / totalEvaluated * 100.0;
    }
    
    /**
     * Calculates the protection rate as a percentage of candidates.
     * 
     * @return Protection rate (0.0 to 100.0), or 0.0 if no memories were candidates
     */
    public double getProtectionRate() {
        int candidates = removedCount + protectedCount;
        if (candidates <= 0) {
            return 0.0;
        }
        return (double) protectedCount / candidates * 100.0;
    }
    
    /**
     * Checks if the forgetting operation was successful.
     * 
     * @return true if no errors occurred
     */
    public boolean isSuccessful() {
        return errorMessage == null || errorMessage.trim().isEmpty();
    }
    
    /**
     * Checks if any memories were actually removed.
     * 
     * @return true if at least one memory was removed
     */
    public boolean hasRemovedMemories() {
        return removedCount > 0;
    }
    
    /**
     * Gets the total number of memories before the operation.
     * 
     * @return Total memories before forgetting
     */
    public int getTotalBeforeOperation() {
        return remainingCount + removedCount;
    }
    
    /**
     * Creates a summary string of the forgetting operation.
     * 
     * @return A human-readable summary
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Forgetting Report: ");
        
        if (dryRun) {
            summary.append("DRY RUN - ");
        }
        
        summary.append(String.format("%d removed, %d remaining", removedCount, remainingCount));
        
        if (totalEvaluated > 0) {
            summary.append(String.format(" (%.1f%% removal rate)", getRemovalRate()));
        }
        
        if (protectedCount > 0) {
            summary.append(String.format(", %d protected", protectedCount));
        }
        
        if (processingTimeMs != null) {
            summary.append(String.format(", processed in %dms", processingTimeMs));
        }
        
        if (!isSuccessful()) {
            summary.append(" - ERROR: ").append(errorMessage);
        }
        
        return summary.toString();
    }
    
    /**
     * Creates a detailed report string for logging or debugging.
     * 
     * @return Detailed report information
     */
    public String getDetailedReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Forgetting Operation Report ===\n");
        report.append("Agent ID: ").append(agentId != null ? agentId : "ALL").append("\n");
        report.append("Execution Time: ").append(executionTimestamp).append("\n");
        report.append("Processing Duration: ").append(processingTimeMs != null ? processingTimeMs + "ms" : "unknown").append("\n");
        report.append("Dry Run: ").append(dryRun).append("\n");
        report.append("\nStrategy Applied: ").append(appliedStrategy != null ? appliedStrategy.getDescription() : "unknown").append("\n");
        report.append("\nResults:\n");
        report.append("  - Total Evaluated: ").append(totalEvaluated).append("\n");
        report.append("  - Removed: ").append(removedCount).append("\n");
        report.append("  - Remaining: ").append(remainingCount).append("\n");
        report.append("  - Protected: ").append(protectedCount).append("\n");
        
        if (hasRemovedMemories()) {
            report.append("  - Removal Rate: ").append(String.format("%.2f%%", getRemovalRate())).append("\n");
        }
        
        if (bytesFreed != null && bytesFreed > 0) {
            report.append("  - Storage Freed: ").append(formatBytes(bytesFreed)).append("\n");
        }
        
        if (affectedCategories != null && !affectedCategories.isEmpty()) {
            report.append("  - Affected Categories: ").append(affectedCategories).append("\n");
        }
        
        if (!isSuccessful()) {
            report.append("\nError: ").append(errorMessage).append("\n");
        }
        
        return report.toString();
    }
    
    /**
     * Formats bytes into human-readable format.
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    // Getters and Setters
    
    public int getRemovedCount() {
        return removedCount;
    }
    
    public void setRemovedCount(int removedCount) {
        this.removedCount = removedCount;
    }
    
    public List<String> getRemovedIds() {
        return removedIds != null ? new ArrayList<>(removedIds) : new ArrayList<>();
    }
    
    public void setRemovedIds(List<String> removedIds) {
        this.removedIds = new ArrayList<>(removedIds != null ? removedIds : new ArrayList<>());
        this.removedCount = this.removedIds.size(); // Keep count in sync
    }
    
    public int getRemainingCount() {
        return remainingCount;
    }
    
    public void setRemainingCount(int remainingCount) {
        this.remainingCount = remainingCount;
    }
    
    public ForgettingStrategy getAppliedStrategy() {
        return appliedStrategy;
    }
    
    public void setAppliedStrategy(ForgettingStrategy appliedStrategy) {
        this.appliedStrategy = appliedStrategy;
    }
    
    public String getAgentId() {
        return agentId;
    }
    
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }
    
    public Instant getExecutionTimestamp() {
        return executionTimestamp;
    }
    
    public void setExecutionTimestamp(Instant executionTimestamp) {
        this.executionTimestamp = executionTimestamp;
    }
    
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public int getTotalEvaluated() {
        return totalEvaluated;
    }
    
    public void setTotalEvaluated(int totalEvaluated) {
        this.totalEvaluated = totalEvaluated;
    }
    
    public int getProtectedCount() {
        return protectedCount;
    }
    
    public void setProtectedCount(int protectedCount) {
        this.protectedCount = protectedCount;
    }
    
    public boolean isDryRun() {
        return dryRun;
    }
    
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public List<String> getAffectedCategories() {
        return affectedCategories != null ? new ArrayList<>(affectedCategories) : new ArrayList<>();
    }
    
    public void setAffectedCategories(List<String> affectedCategories) {
        this.affectedCategories = new ArrayList<>(affectedCategories != null ? affectedCategories : new ArrayList<>());
    }
    
    public Long getBytesFreed() {
        return bytesFreed;
    }
    
    public void setBytesFreed(Long bytesFreed) {
        this.bytesFreed = bytesFreed;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForgettingReport that = (ForgettingReport) o;
        return removedCount == that.removedCount &&
                remainingCount == that.remainingCount &&
                totalEvaluated == that.totalEvaluated &&
                protectedCount == that.protectedCount &&
                dryRun == that.dryRun &&
                Objects.equals(removedIds, that.removedIds) &&
                Objects.equals(appliedStrategy, that.appliedStrategy) &&
                Objects.equals(agentId, that.agentId) &&
                Objects.equals(executionTimestamp, that.executionTimestamp) &&
                Objects.equals(processingTimeMs, that.processingTimeMs) &&
                Objects.equals(errorMessage, that.errorMessage) &&
                Objects.equals(affectedCategories, that.affectedCategories) &&
                Objects.equals(bytesFreed, that.bytesFreed);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(removedCount, removedIds, remainingCount, appliedStrategy, agentId, 
                          executionTimestamp, processingTimeMs, totalEvaluated, protectedCount, 
                          dryRun, errorMessage, affectedCategories, bytesFreed);
    }
    
    @Override
    public String toString() {
        return "ForgettingReport{" +
                "removedCount=" + removedCount +
                ", removedIds=" + (removedIds != null ? removedIds.size() + " items" : "null") +
                ", remainingCount=" + remainingCount +
                ", appliedStrategy=" + (appliedStrategy != null ? appliedStrategy.getType() : "null") +
                ", agentId='" + agentId + '\'' +
                ", executionTimestamp=" + executionTimestamp +
                ", processingTimeMs=" + processingTimeMs +
                ", totalEvaluated=" + totalEvaluated +
                ", protectedCount=" + protectedCount +
                ", dryRun=" + dryRun +
                ", errorMessage='" + errorMessage + '\'' +
                ", affectedCategories=" + (affectedCategories != null ? affectedCategories.size() + " items" : "null") +
                ", bytesFreed=" + bytesFreed +
                '}';
    }
}