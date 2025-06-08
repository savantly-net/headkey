package ai.headkey.persistence.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity for storing belief conflicts in PostgreSQL database.
 * 
 * This entity maps to the 'belief_conflicts' table and represents the persistent
 * form of conflicts between beliefs in the system. It includes all necessary fields
 * for tracking conflict detection, resolution, and audit trail.
 * 
 * Table structure optimized for:
 * - Fast lookups by ID and agent
 * - Efficient filtering by resolution status
 * - Tracking conflict resolution workflow
 * - Proper indexing for performance
 * 
 * @since 1.0
 */
@Entity
@Table(name = "belief_conflicts", indexes = {
    @Index(name = "idx_conflict_agent_id", columnList = "agent_id"),
    @Index(name = "idx_conflict_resolved", columnList = "resolved"),
    @Index(name = "idx_conflict_detected_at", columnList = "detected_at"),
    @Index(name = "idx_conflict_agent_resolved", columnList = "agent_id, resolved"),
    @Index(name = "idx_conflict_resolution_strategy", columnList = "resolution_strategy")
})
@NamedQueries({
    @NamedQuery(
        name = "BeliefConflictEntity.findUnresolved",
        query = "SELECT c FROM BeliefConflictEntity c WHERE c.resolved = false AND (:agentId IS NULL OR c.agentId = :agentId) ORDER BY c.detectedAt DESC"
    ),
    @NamedQuery(
        name = "BeliefConflictEntity.findByAgent",
        query = "SELECT c FROM BeliefConflictEntity c WHERE c.agentId = :agentId ORDER BY c.detectedAt DESC"
    ),
    @NamedQuery(
        name = "BeliefConflictEntity.countUnresolved",
        query = "SELECT COUNT(c) FROM BeliefConflictEntity c WHERE c.resolved = false AND (:agentId IS NULL OR c.agentId = :agentId)"
    ),
    @NamedQuery(
        name = "BeliefConflictEntity.findByResolutionStrategy",
        query = "SELECT c FROM BeliefConflictEntity c WHERE c.resolutionStrategy = :strategy ORDER BY c.detectedAt DESC"
    )
})
public class BeliefConflictEntity {

    @Id
    @Column(name = "id", length = 100, nullable = false)
    @NotBlank(message = "Conflict ID cannot be blank")
    private String id;

    @Column(name = "agent_id", length = 100, nullable = false)
    @NotBlank(message = "Agent ID cannot be blank")
    private String agentId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "conflict_belief_ids",
        joinColumns = @JoinColumn(name = "conflict_id"),
        indexes = @Index(name = "idx_conflict_beliefs", columnList = "conflict_id")
    )
    @Column(name = "belief_id", length = 100)
    private List<String> conflictingBeliefIds = new ArrayList<>();

    @Column(name = "new_evidence_memory_id", length = 100)
    private String newEvidenceMemoryId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "conflict_type", length = 50)
    private String conflictType;

    @Column(name = "detected_at", nullable = false)
    @NotNull(message = "Detected timestamp cannot be null")
    private Instant detectedAt;

    @Column(name = "resolved", nullable = false)
    @NotNull(message = "Resolved flag cannot be null")
    private Boolean resolved = false;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_strategy", length = 100)
    private String resolutionStrategy;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "severity", length = 20)
    private String severity = "MEDIUM";

    @Column(name = "auto_resolvable", nullable = false)
    @NotNull(message = "Auto resolvable flag cannot be null")
    private Boolean autoResolvable = true;

    // Audit fields
    @Column(name = "version")
    @Version
    private Long version;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created timestamp cannot be null")
    private Instant createdAt;

    @Column(name = "last_updated", nullable = false)
    @NotNull(message = "Last updated timestamp cannot be null")
    private Instant lastUpdated;

    /**
     * Default constructor for JPA.
     */
    public BeliefConflictEntity() {
        this.detectedAt = Instant.now();
        this.createdAt = Instant.now();
        this.lastUpdated = Instant.now();
        this.resolved = false;
        this.autoResolvable = true;
        this.severity = "MEDIUM";
        this.conflictingBeliefIds = new ArrayList<>();
    }

    /**
     * Constructor with required fields.
     */
    public BeliefConflictEntity(String id, String agentId) {
        this();
        this.id = id;
        this.agentId = agentId;
    }

    /**
     * Constructor with main fields.
     */
    public BeliefConflictEntity(String id, String agentId, List<String> conflictingBeliefIds, String description) {
        this(id, agentId);
        this.conflictingBeliefIds = new ArrayList<>(conflictingBeliefIds != null ? conflictingBeliefIds : new ArrayList<>());
        this.description = description;
    }

    // JPA lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (detectedAt == null) {
            detectedAt = Instant.now();
        }
        lastUpdated = Instant.now();
        if (resolved == null) {
            resolved = false;
        }
        if (autoResolvable == null) {
            autoResolvable = true;
        }
        if (severity == null) {
            severity = "MEDIUM";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Instant.now();
    }

    // Business methods
    public void markResolved(String strategy, String notes) {
        this.resolved = true;
        this.resolvedAt = Instant.now();
        this.resolutionStrategy = strategy;
        this.resolutionNotes = notes;
        this.lastUpdated = Instant.now();
    }

    public void markUnresolved() {
        this.resolved = false;
        this.resolvedAt = null;
        this.resolutionStrategy = null;
        this.resolutionNotes = null;
        this.lastUpdated = Instant.now();
    }

    public void addConflictingBeliefId(String beliefId) {
        if (beliefId != null && !beliefId.trim().isEmpty()) {
            if (conflictingBeliefIds == null) {
                conflictingBeliefIds = new ArrayList<>();
            }
            if (!conflictingBeliefIds.contains(beliefId)) {
                conflictingBeliefIds.add(beliefId);
            }
        }
    }

    public boolean removeConflictingBeliefId(String beliefId) {
        return conflictingBeliefIds != null && conflictingBeliefIds.remove(beliefId);
    }

    public boolean isHighSeverity() {
        return "HIGH".equalsIgnoreCase(severity) || "CRITICAL".equalsIgnoreCase(severity);
    }

    public long getAgeInSeconds() {
        if (detectedAt == null) {
            return 0;
        }
        return Instant.now().getEpochSecond() - detectedAt.getEpochSecond();
    }

    public long getResolutionTimeInSeconds() {
        if (detectedAt == null || resolvedAt == null) {
            return 0;
        }
        return resolvedAt.getEpochSecond() - detectedAt.getEpochSecond();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public List<String> getConflictingBeliefIds() {
        return conflictingBeliefIds != null ? new ArrayList<>(conflictingBeliefIds) : new ArrayList<>();
    }

    public void setConflictingBeliefIds(List<String> conflictingBeliefIds) {
        this.conflictingBeliefIds = new ArrayList<>(conflictingBeliefIds != null ? conflictingBeliefIds : new ArrayList<>());
    }

    public String getNewEvidenceMemoryId() {
        return newEvidenceMemoryId;
    }

    public void setNewEvidenceMemoryId(String newEvidenceMemoryId) {
        this.newEvidenceMemoryId = newEvidenceMemoryId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConflictType() {
        return conflictType;
    }

    public void setConflictType(String conflictType) {
        this.conflictType = conflictType;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public void setResolved(Boolean resolved) {
        this.resolved = resolved;
    }

    public boolean isResolved() {
        return Boolean.TRUE.equals(resolved);
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolutionStrategy() {
        return resolutionStrategy;
    }

    public void setResolutionStrategy(String resolutionStrategy) {
        this.resolutionStrategy = resolutionStrategy;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Boolean getAutoResolvable() {
        return autoResolvable;
    }

    public void setAutoResolvable(Boolean autoResolvable) {
        this.autoResolvable = autoResolvable;
    }

    public boolean isAutoResolvable() {
        return Boolean.TRUE.equals(autoResolvable);
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeliefConflictEntity that = (BeliefConflictEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "BeliefConflictEntity{" +
                "id='" + id + '\'' +
                ", agentId='" + agentId + '\'' +
                ", conflictingBeliefIds=" + conflictingBeliefIds +
                ", conflictType='" + conflictType + '\'' +
                ", resolved=" + resolved +
                ", severity='" + severity + '\'' +
                ", detectedAt=" + detectedAt +
                ", resolvedAt=" + resolvedAt +
                '}';
    }
}