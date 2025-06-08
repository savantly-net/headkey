package ai.headkey.persistence.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA Entity for storing beliefs in PostgreSQL database.
 * 
 * This entity maps to the 'beliefs' table and represents the persistent
 * form of a belief in the system. It includes all necessary fields for
 * storing belief information with proper indexing and constraints.
 * 
 * Table structure optimized for:
 * - Fast lookups by ID and agent
 * - Efficient filtering by category and confidence
 * - Full-text search on statement content
 * - Proper indexing for performance
 * 
 * @since 1.0
 */
@Entity
@Table(name = "beliefs", indexes = {
    @Index(name = "idx_belief_agent_id", columnList = "agent_id"),
    @Index(name = "idx_belief_category", columnList = "category"),
    @Index(name = "idx_belief_confidence", columnList = "confidence"),
    @Index(name = "idx_belief_active", columnList = "active"),
    @Index(name = "idx_belief_created_at", columnList = "created_at"),
    @Index(name = "idx_belief_agent_category", columnList = "agent_id, category"),
    @Index(name = "idx_belief_agent_active", columnList = "agent_id, active")
})
@NamedQueries({
    @NamedQuery(
        name = "BeliefEntity.findByAgent",
        query = "SELECT b FROM BeliefEntity b WHERE b.agentId = :agentId AND (:includeInactive = true OR b.active = true) ORDER BY b.lastUpdated DESC"
    ),
    @NamedQuery(
        name = "BeliefEntity.findByCategory",
        query = "SELECT b FROM BeliefEntity b WHERE b.category = :category AND (:agentId IS NULL OR b.agentId = :agentId) AND (:includeInactive = true OR b.active = true) ORDER BY b.lastUpdated DESC"
    ),
    @NamedQuery(
        name = "BeliefEntity.findLowConfidence",
        query = "SELECT b FROM BeliefEntity b WHERE b.confidence < :threshold AND (:agentId IS NULL OR b.agentId = :agentId) AND b.active = true ORDER BY b.confidence ASC"
    ),
    @NamedQuery(
        name = "BeliefEntity.countByAgent",
        query = "SELECT COUNT(b) FROM BeliefEntity b WHERE b.agentId = :agentId AND (:includeInactive = true OR b.active = true)"
    ),
    @NamedQuery(
        name = "BeliefEntity.findAllActive",
        query = "SELECT b FROM BeliefEntity b WHERE b.active = true ORDER BY b.lastUpdated DESC"
    ),
    @NamedQuery(
        name = "BeliefEntity.searchByText",
        query = "SELECT b FROM BeliefEntity b WHERE LOWER(b.statement) LIKE LOWER(CONCAT('%', :searchText, '%')) AND (:agentId IS NULL OR b.agentId = :agentId) AND b.active = true ORDER BY b.confidence DESC"
    )
})
public class BeliefEntity {

    @Id
    @Column(name = "id", length = 100, nullable = false)
    @NotBlank(message = "Belief ID cannot be blank")
    private String id;

    @Column(name = "agent_id", length = 100, nullable = false)
    @NotBlank(message = "Agent ID cannot be blank")
    private String agentId;

    @Column(name = "statement", columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = "Statement cannot be blank")
    private String statement;

    @Column(name = "confidence", nullable = false)
    @DecimalMin(value = "0.0", message = "Confidence must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Confidence must be at most 1.0")
    @NotNull(message = "Confidence cannot be null")
    private Double confidence;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created timestamp cannot be null")
    private Instant createdAt;

    @Column(name = "last_updated", nullable = false)
    @NotNull(message = "Last updated timestamp cannot be null")
    private Instant lastUpdated;

    @Column(name = "reinforcement_count", nullable = false)
    @NotNull(message = "Reinforcement count cannot be null")
    private Integer reinforcementCount = 0;

    @Column(name = "active", nullable = false)
    @NotNull(message = "Active flag cannot be null")
    private Boolean active = true;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "belief_evidence_memories",
        joinColumns = @JoinColumn(name = "belief_id"),
        indexes = @Index(name = "idx_evidence_belief_id", columnList = "belief_id")
    )
    @Column(name = "memory_id", length = 100)
    private Set<String> evidenceMemoryIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "belief_tags",
        joinColumns = @JoinColumn(name = "belief_id"),
        indexes = @Index(name = "idx_tags_belief_id", columnList = "belief_id")
    )
    @Column(name = "tag", length = 100)
    private Set<String> tags = new HashSet<>();

    // Audit fields
    @Column(name = "version")
    @Version
    private Long version;

    /**
     * Default constructor for JPA.
     */
    public BeliefEntity() {
        this.createdAt = Instant.now();
        this.lastUpdated = Instant.now();
        this.reinforcementCount = 0;
        this.active = true;
        this.evidenceMemoryIds = new HashSet<>();
        this.tags = new HashSet<>();
    }

    /**
     * Constructor with required fields.
     */
    public BeliefEntity(String id, String agentId, String statement, Double confidence) {
        this();
        this.id = id;
        this.agentId = agentId;
        this.statement = statement;
        this.confidence = confidence;
    }

    /**
     * Constructor with all main fields.
     */
    public BeliefEntity(String id, String agentId, String statement, Double confidence, String category) {
        this(id, agentId, statement, confidence);
        this.category = category;
    }

    // JPA lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        lastUpdated = Instant.now();
        if (reinforcementCount == null) {
            reinforcementCount = 0;
        }
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Instant.now();
    }

    // Business methods
    public void reinforce(double additionalConfidence) {
        this.confidence = Math.min(1.0, this.confidence + additionalConfidence);
        this.reinforcementCount++;
        this.lastUpdated = Instant.now();
    }

    public void weaken(double confidenceReduction) {
        this.confidence = Math.max(0.0, this.confidence - confidenceReduction);
        this.lastUpdated = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.lastUpdated = Instant.now();
    }

    public void reactivate() {
        this.active = true;
        this.lastUpdated = Instant.now();
    }

    public void addEvidence(String memoryId) {
        if (memoryId != null && !memoryId.trim().isEmpty()) {
            if (evidenceMemoryIds == null) {
                evidenceMemoryIds = new HashSet<>();
            }
            evidenceMemoryIds.add(memoryId);
        }
    }

    public boolean removeEvidence(String memoryId) {
        return evidenceMemoryIds != null && evidenceMemoryIds.remove(memoryId);
    }

    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            if (tags == null) {
                tags = new HashSet<>();
            }
            tags.add(tag.trim());
        }
    }

    public int getEvidenceCount() {
        return evidenceMemoryIds != null ? evidenceMemoryIds.size() : 0;
    }

    public boolean isHighConfidence(double threshold) {
        return confidence >= threshold;
    }

    public boolean isHighConfidence() {
        return isHighConfidence(0.8);
    }

    public long getAgeInSeconds() {
        if (createdAt == null) {
            return 0;
        }
        return Instant.now().getEpochSecond() - createdAt.getEpochSecond();
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

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public Integer getReinforcementCount() {
        return reinforcementCount;
    }

    public void setReinforcementCount(Integer reinforcementCount) {
        this.reinforcementCount = reinforcementCount;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Set<String> getEvidenceMemoryIds() {
        return evidenceMemoryIds != null ? new HashSet<>(evidenceMemoryIds) : new HashSet<>();
    }

    public void setEvidenceMemoryIds(Set<String> evidenceMemoryIds) {
        this.evidenceMemoryIds = new HashSet<>(evidenceMemoryIds != null ? evidenceMemoryIds : new HashSet<>());
    }

    public Set<String> getTags() {
        return tags != null ? new HashSet<>(tags) : new HashSet<>();
    }

    public void setTags(Set<String> tags) {
        this.tags = new HashSet<>(tags != null ? tags : new HashSet<>());
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeliefEntity that = (BeliefEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "BeliefEntity{" +
                "id='" + id + '\'' +
                ", agentId='" + agentId + '\'' +
                ", statement='" + statement + '\'' +
                ", confidence=" + confidence +
                ", category='" + category + '\'' +
                ", active=" + active +
                ", reinforcementCount=" + reinforcementCount +
                ", createdAt=" + createdAt +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}