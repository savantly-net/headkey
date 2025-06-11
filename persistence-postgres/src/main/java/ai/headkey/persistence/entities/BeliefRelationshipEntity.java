package ai.headkey.persistence.entities;

import ai.headkey.memory.enums.RelationshipType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * JPA Entity for storing belief relationships in PostgreSQL database.
 * 
 * This entity represents the edges in the belief knowledge graph, enabling
 * rich relationships between beliefs with support for temporal constraints,
 * metadata, and various relationship types. The table is optimized for
 * graph traversal queries and relationship analysis.
 * 
 * Table structure optimized for:
 * - Fast graph traversal queries
 * - Efficient filtering by relationship type and agent
 * - Temporal relationship queries
 * - Bidirectional relationship lookups
 * 
 * @since 1.0
 */
@Entity
@Table(name = "belief_relationships", indexes = {
    @Index(name = "idx_rel_source_belief", columnList = "source_belief_id"),
    @Index(name = "idx_rel_target_belief", columnList = "target_belief_id"),
    @Index(name = "idx_rel_agent_id", columnList = "agent_id"),
    @Index(name = "idx_rel_type", columnList = "relationship_type"),
    @Index(name = "idx_rel_active", columnList = "active"),
    @Index(name = "idx_rel_strength", columnList = "strength"),
    @Index(name = "idx_rel_temporal", columnList = "effective_from, effective_until"),
    @Index(name = "idx_rel_agent_active", columnList = "agent_id, active"),
    @Index(name = "idx_rel_agent_type", columnList = "agent_id, relationship_type"),
    @Index(name = "idx_rel_source_type", columnList = "source_belief_id, relationship_type"),
    @Index(name = "idx_rel_target_type", columnList = "target_belief_id, relationship_type"),
    @Index(name = "idx_rel_deprecation", columnList = "relationship_type, active, effective_until"),
    @Index(name = "idx_rel_created_at", columnList = "created_at")
}, uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_rel_unique_active", 
        columnNames = {"source_belief_id", "target_belief_id", "relationship_type", "agent_id", "active"}
    )
})
@NamedQueries({
    @NamedQuery(
        name = "BeliefRelationshipEntity.findByAgent",
        query = "SELECT r FROM BeliefRelationshipEntity r WHERE r.agentId = :agentId AND (:includeInactive = true OR r.active = true) ORDER BY r.createdAt DESC"
    ),
    @NamedQuery(
        name = "BeliefRelationshipEntity.findBySourceBelief",
        query = "SELECT r FROM BeliefRelationshipEntity r WHERE r.sourceBeliefId = :sourceBeliefId AND (:agentId IS NULL OR r.agentId = :agentId) AND (:includeInactive = true OR r.active = true) ORDER BY r.strength DESC"
    ),
    @NamedQuery(
        name = "BeliefRelationshipEntity.findByTargetBelief",
        query = "SELECT r FROM BeliefRelationshipEntity r WHERE r.targetBeliefId = :targetBeliefId AND (:agentId IS NULL OR r.agentId = :agentId) AND (:includeInactive = true OR r.active = true) ORDER BY r.strength DESC"
    ),
    @NamedQuery(
        name = "BeliefRelationshipEntity.findByBelief",
        query = "SELECT r FROM BeliefRelationshipEntity r WHERE (r.sourceBeliefId = :beliefId OR r.targetBeliefId = :beliefId) AND (:agentId IS NULL OR r.agentId = :agentId) AND (:includeInactive = true OR r.active = true) ORDER BY r.strength DESC"
    ),
    @NamedQuery(
        name = "BeliefRelationshipEntity.findByType",
        query = "SELECT r FROM BeliefRelationshipEntity r WHERE r.relationshipType = :relationshipType AND (:agentId IS NULL OR r.agentId = :agentId) AND (:includeInactive = true OR r.active = true) ORDER BY r.strength DESC"
    ),
    @NamedQuery(
        name = "BeliefRelationshipEntity.findBetweenBeliefs",
        query = "SELECT r FROM BeliefRelationshipEntity r WHERE r.sourceBeliefId = :sourceBeliefId AND r.targetBeliefId = :targetBeliefId AND (:agentId IS NULL OR r.agentId = :agentId) AND (:includeInactive = true OR r.active = true) ORDER BY r.strength DESC"
    ),
    @NamedQuery(
        name = "BeliefRelationshipEntity.findDeprecating",
        query = "SELECT r FROM BeliefRelationshipEntity r WHERE r.relationshipType IN (ai.headkey.memory.enums.RelationshipType.SUPERSEDES, ai.headkey.memory.enums.RelationshipType.UPDATES, ai.headkey.memory.enums.RelationshipType.DEPRECATES, ai.headkey.memory.enums.RelationshipType.REPLACES) AND (:agentId IS NULL OR r.agentId = :agentId) AND r.active = true AND (r.effectiveUntil IS NULL OR r.effectiveUntil > CURRENT_TIMESTAMP) ORDER BY r.createdAt DESC"
    ),
    @NamedQuery(
        name = "BeliefRelationshipEntity.findCurrentlyEffective",
        query = "SELECT r FROM BeliefRelationshipEntity r WHERE r.active = true AND (:agentId IS NULL OR r.agentId = :agentId) AND (r.effectiveFrom IS NULL OR r.effectiveFrom <= CURRENT_TIMESTAMP) AND (r.effectiveUntil IS NULL OR r.effectiveUntil > CURRENT_TIMESTAMP) ORDER BY r.strength DESC"
    ),
    @NamedQuery(
        name = "BeliefRelationshipEntity.findHighStrength",
        query = "SELECT r FROM BeliefRelationshipEntity r WHERE r.strength >= :threshold AND (:agentId IS NULL OR r.agentId = :agentId) AND r.active = true ORDER BY r.strength DESC"
    ),
    @NamedQuery(
        name = "BeliefRelationshipEntity.countByAgent",
        query = "SELECT COUNT(r) FROM BeliefRelationshipEntity r WHERE r.agentId = :agentId AND (:includeInactive = true OR r.active = true)"
    )
})
public class BeliefRelationshipEntity {

    @Id
    @Column(name = "id", length = 100, nullable = false)
    @NotBlank(message = "Relationship ID cannot be blank")
    private String id;

    @Column(name = "source_belief_id", length = 100, nullable = false)
    @NotBlank(message = "Source belief ID cannot be blank")
    private String sourceBeliefId;

    @Column(name = "target_belief_id", length = 100, nullable = false)
    @NotBlank(message = "Target belief ID cannot be blank")
    private String targetBeliefId;

    @Column(name = "agent_id", length = 100, nullable = false)
    @NotBlank(message = "Agent ID cannot be blank")
    private String agentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", length = 50, nullable = false)
    @NotNull(message = "Relationship type cannot be null")
    private RelationshipType relationshipType;

    @Column(name = "strength", nullable = false)
    @DecimalMin(value = "0.0", message = "Strength must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Strength must be at most 1.0")
    @NotNull(message = "Strength cannot be null")
    private Double strength;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_until")
    private Instant effectiveUntil;

    @Column(name = "deprecation_reason", columnDefinition = "TEXT")
    private String deprecationReason;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created timestamp cannot be null")
    private Instant createdAt;

    @Column(name = "last_updated", nullable = false)
    @NotNull(message = "Last updated timestamp cannot be null")
    private Instant lastUpdated;

    @Column(name = "active", nullable = false)
    @NotNull(message = "Active flag cannot be null")
    private Boolean active = true;

    // Metadata stored as key-value pairs in a separate table
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "belief_relationship_metadata",
        joinColumns = @JoinColumn(name = "relationship_id"),
        indexes = {
            @Index(name = "idx_rel_metadata_relationship", columnList = "relationship_id"),
            @Index(name = "idx_rel_metadata_key", columnList = "metadata_key")
        }
    )
    @MapKeyColumn(name = "metadata_key", length = 100)
    @Column(name = "metadata_value", columnDefinition = "TEXT")
    private Map<String, String> metadata = new HashMap<>();

    // Audit fields
    @Column(name = "version")
    @Version
    private Long version;

    /**
     * Default constructor for JPA.
     */
    public BeliefRelationshipEntity() {
        this.createdAt = Instant.now();
        this.lastUpdated = Instant.now();
        this.active = true;
        this.strength = 1.0;
        this.priority = 0;
        this.metadata = new HashMap<>();
    }

    /**
     * Constructor with required fields.
     */
    public BeliefRelationshipEntity(String id, String sourceBeliefId, String targetBeliefId, 
                                   RelationshipType relationshipType, String agentId) {
        this();
        this.id = id;
        this.sourceBeliefId = sourceBeliefId;
        this.targetBeliefId = targetBeliefId;
        this.relationshipType = relationshipType;
        this.agentId = agentId;
    }

    /**
     * Constructor with core fields.
     */
    public BeliefRelationshipEntity(String id, String sourceBeliefId, String targetBeliefId, 
                                   RelationshipType relationshipType, Double strength, String agentId) {
        this(id, sourceBeliefId, targetBeliefId, relationshipType, agentId);
        this.strength = strength;
    }

    /**
     * Full constructor for temporal relationships.
     */
    public BeliefRelationshipEntity(String id, String sourceBeliefId, String targetBeliefId, 
                                   RelationshipType relationshipType, Double strength, String agentId,
                                   Instant effectiveFrom, Instant effectiveUntil, String deprecationReason) {
        this(id, sourceBeliefId, targetBeliefId, relationshipType, strength, agentId);
        this.effectiveFrom = effectiveFrom;
        this.effectiveUntil = effectiveUntil;
        this.deprecationReason = deprecationReason;
    }

    // JPA lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        lastUpdated = Instant.now();
        if (active == null) {
            active = true;
        }
        if (strength == null) {
            strength = 1.0;
        }
        if (priority == null) {
            priority = 0;
        }
        
        // Validate temporal constraints
        validateTemporalConstraints();
        
        // Prevent self-referential relationships
        if (Objects.equals(sourceBeliefId, targetBeliefId)) {
            throw new IllegalArgumentException("Self-referential relationships are not allowed");
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Instant.now();
        validateTemporalConstraints();
    }

    /**
     * Validates temporal constraints for the relationship.
     */
    private void validateTemporalConstraints() {
        if (effectiveFrom != null && effectiveUntil != null) {
            if (effectiveFrom.isAfter(effectiveUntil)) {
                throw new IllegalArgumentException("effectiveFrom cannot be after effectiveUntil");
            }
        }
    }

    // Business methods

    /**
     * Checks if this relationship is currently effective based on temporal constraints.
     */
    public boolean isCurrentlyEffective() {
        if (!active) {
            return false;
        }
        
        Instant now = Instant.now();
        
        if (effectiveFrom != null && now.isBefore(effectiveFrom)) {
            return false;
        }
        
        if (effectiveUntil != null && now.isAfter(effectiveUntil)) {
            return false;
        }
        
        return true;
    }

    /**
     * Checks if this relationship is effective at a specific time.
     */
    public boolean isEffectiveAt(Instant timestamp) {
        if (!active || timestamp == null) {
            return false;
        }
        
        if (effectiveFrom != null && timestamp.isBefore(effectiveFrom)) {
            return false;
        }
        
        if (effectiveUntil != null && timestamp.isAfter(effectiveUntil)) {
            return false;
        }
        
        return true;
    }

    /**
     * Updates the relationship strength.
     */
    public void updateStrength(double newStrength) {
        this.strength = Math.max(0.0, Math.min(1.0, newStrength));
        this.lastUpdated = Instant.now();
    }

    /**
     * Deactivates this relationship.
     */
    public void deactivate() {
        this.active = false;
        this.lastUpdated = Instant.now();
    }

    /**
     * Reactivates this relationship.
     */
    public void reactivate() {
        this.active = true;
        this.lastUpdated = Instant.now();
    }

    /**
     * Sets the effective period for this relationship.
     */
    public void setEffectivePeriod(Instant from, Instant until) {
        this.effectiveFrom = from;
        this.effectiveUntil = until;
        this.lastUpdated = Instant.now();
        validateTemporalConstraints();
    }

    /**
     * Adds metadata to this relationship.
     */
    public void addMetadata(String key, String value) {
        if (key != null && value != null) {
            if (metadata == null) {
                metadata = new HashMap<>();
            }
            metadata.put(key, value);
        }
    }

    /**
     * Gets metadata value by key.
     */
    public String getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    /**
     * Checks if this is a temporal relationship type.
     */
    public boolean isTemporal() {
        return relationshipType != null && relationshipType.isTemporal();
    }

    /**
     * Checks if this relationship deprecates the target belief.
     */
    public boolean isDeprecating() {
        return relationshipType != null && relationshipType.isDeprecating();
    }

    /**
     * Checks if this relationship has high strength.
     */
    public boolean isHighStrength(double threshold) {
        return strength >= threshold;
    }

    /**
     * Checks if this relationship has high strength using default threshold.
     */
    public boolean isHighStrength() {
        return isHighStrength(0.8);
    }

    /**
     * Gets the age of this relationship in seconds.
     */
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

    public String getSourceBeliefId() {
        return sourceBeliefId;
    }

    public void setSourceBeliefId(String sourceBeliefId) {
        this.sourceBeliefId = sourceBeliefId;
    }

    public String getTargetBeliefId() {
        return targetBeliefId;
    }

    public void setTargetBeliefId(String targetBeliefId) {
        this.targetBeliefId = targetBeliefId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }

    public Double getStrength() {
        return strength;
    }

    public void setStrength(Double strength) {
        this.strength = Math.max(0.0, Math.min(1.0, strength));
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getEffectiveUntil() {
        return effectiveUntil;
    }

    public void setEffectiveUntil(Instant effectiveUntil) {
        this.effectiveUntil = effectiveUntil;
    }

    public String getDeprecationReason() {
        return deprecationReason;
    }

    public void setDeprecationReason(String deprecationReason) {
        this.deprecationReason = deprecationReason;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Map<String, String> getMetadata() {
        return metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = new HashMap<>(metadata != null ? metadata : new HashMap<>());
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
        BeliefRelationshipEntity that = (BeliefRelationshipEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "BeliefRelationshipEntity{" +
                "id='" + id + '\'' +
                ", sourceBeliefId='" + sourceBeliefId + '\'' +
                ", targetBeliefId='" + targetBeliefId + '\'' +
                ", agentId='" + agentId + '\'' +
                ", relationshipType=" + relationshipType +
                ", strength=" + strength +
                ", active=" + active +
                ", effectiveFrom=" + effectiveFrom +
                ", effectiveUntil=" + effectiveUntil +
                ", priority=" + priority +
                ", createdAt=" + createdAt +
                '}';
    }
}