package ai.headkey.persistence.elastic.documents;

import ai.headkey.memory.enums.RelationshipType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Elasticsearch document representing a relationship between beliefs in the knowledge graph.
 * This document maps to the BeliefRelationship DTO for persistence in Elasticsearch.
 *
 * The document is optimized for:
 * - Efficient graph traversal queries
 * - Filtering by relationship type, strength, and temporal constraints
 * - Agent-based relationship isolation
 * - Analytics and aggregations for relationship statistics
 * - Support for temporal relationships with effective periods
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BeliefRelationshipDocument {

    /**
     * Unique identifier for this relationship document.
     * Maps to Elasticsearch _id field.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Agent identifier for relationship isolation.
     * Indexed for efficient agent-based filtering.
     */
    @JsonProperty("agent_id")
    private String agentId;

    /**
     * ID of the source belief in the relationship.
     * Keyword field for exact matching and graph traversal.
     */
    @JsonProperty("source_belief_id")
    private String sourceBeliefId;

    /**
     * ID of the target belief in the relationship.
     * Keyword field for exact matching and graph traversal.
     */
    @JsonProperty("target_belief_id")
    private String targetBeliefId;

    /**
     * Type of relationship between the beliefs.
     * Keyword field for relationship type filtering and aggregations.
     */
    @JsonProperty("relationship_type")
    private String relationshipType;

    /**
     * Strength of the relationship (0.0 to 1.0).
     * Numeric field for strength-based filtering and scoring.
     */
    @JsonProperty("strength")
    private Double strength;

    /**
     * Flag indicating whether this relationship is currently active.
     * Boolean field for active/inactive filtering.
     */
    @JsonProperty("active")
    private Boolean active;

    /**
     * Optional reason for the relationship.
     * Text field for descriptive information.
     */
    @JsonProperty("reason")
    private String reason;

    /**
     * When the relationship becomes effective (for temporal relationships).
     * Date field for temporal queries.
     */
    @JsonProperty("effective_from")
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant effectiveFrom;

    /**
     * When the relationship expires (for temporal relationships).
     * Date field for temporal queries.
     */
    @JsonProperty("effective_until")
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant effectiveUntil;

    /**
     * Additional metadata stored as nested object.
     * Allows for flexible metadata without schema changes.
     */
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    /**
     * Timestamp when relationship was created.
     * Date field for temporal queries and sorting.
     */
    @JsonProperty("created_at")
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant createdAt;

    /**
     * Timestamp when relationship was last updated.
     * Date field for modification tracking.
     */
    @JsonProperty("last_updated")
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant lastUpdated;

    /**
     * Document version for optimistic locking.
     * Numeric field for version control.
     */
    @JsonProperty("version")
    private Long version;

    /**
     * Cached category of the relationship type for efficient filtering.
     * Keyword field derived from RelationshipType.getCategory().
     */
    @JsonProperty("relationship_category")
    private String relationshipCategory;

    /**
     * Flag indicating if this is a temporal relationship.
     * Boolean field derived from RelationshipType.isTemporal().
     */
    @JsonProperty("is_temporal")
    private Boolean isTemporal;

    /**
     * Flag indicating if this relationship is bidirectional.
     * Boolean field derived from RelationshipType.isBidirectional().
     */
    @JsonProperty("is_bidirectional")
    private Boolean isBidirectional;

    /**
     * Default constructor.
     */
    public BeliefRelationshipDocument() {
        this.active = true;
        this.version = 1L;
        this.strength = 0.5;
        this.createdAt = Instant.now();
        this.lastUpdated = Instant.now();
    }

    /**
     * Constructor with required fields.
     */
    public BeliefRelationshipDocument(String id, String agentId, String sourceBeliefId,
                                    String targetBeliefId, RelationshipType relationshipType) {
        this();
        this.id = id;
        this.agentId = agentId;
        this.sourceBeliefId = sourceBeliefId;
        this.targetBeliefId = targetBeliefId;
        setRelationshipTypeEnum(relationshipType);
    }

    /**
     * Constructor with core fields including strength.
     */
    public BeliefRelationshipDocument(String id, String agentId, String sourceBeliefId,
                                    String targetBeliefId, RelationshipType relationshipType,
                                    double strength) {
        this(id, agentId, sourceBeliefId, targetBeliefId, relationshipType);
        this.strength = Math.max(0.0, Math.min(1.0, strength));
    }

    /**
     * Sets the relationship type using the enum and updates derived fields.
     */
    public void setRelationshipTypeEnum(RelationshipType relationshipType) {
        if (relationshipType != null) {
            this.relationshipType = relationshipType.getCode();
            this.relationshipCategory = relationshipType.getCategory();
            this.isTemporal = relationshipType.isTemporal();
            this.isBidirectional = relationshipType.isBidirectional();
        }
    }

    /**
     * Gets the relationship type as enum.
     */
    public RelationshipType getRelationshipTypeEnum() {
        return RelationshipType.fromCode(this.relationshipType);
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
     * Updates the last updated timestamp.
     */
    public void updateLastUpdated() {
        this.lastUpdated = Instant.now();
    }

    /**
     * Checks if this relationship is currently effective (for temporal relationships).
     */
    public boolean isCurrentlyEffective() {
        Instant now = Instant.now();
        boolean afterStart = effectiveFrom == null || !now.isBefore(effectiveFrom);
        boolean beforeEnd = effectiveUntil == null || now.isBefore(effectiveUntil);
        return afterStart && beforeEnd;
    }

    /**
     * Checks if this relationship has high strength.
     */
    public boolean isHighStrength(double threshold) {
        return strength != null && strength >= threshold;
    }

    /**
     * Checks if this relationship has high strength using default threshold of 0.7.
     */
    public boolean isHighStrength() {
        return isHighStrength(0.7);
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

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
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

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
        // Update derived fields when setting relationship type directly
        RelationshipType type = RelationshipType.fromCode(relationshipType);
        if (type != null) {
            this.relationshipCategory = type.getCategory();
            this.isTemporal = type.isTemporal();
            this.isBidirectional = type.isBidirectional();
        }
    }

    public Double getStrength() {
        return strength;
    }

    public void setStrength(Double strength) {
        this.strength = strength != null ? Math.max(0.0, Math.min(1.0, strength)) : 0.5;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getRelationshipCategory() {
        return relationshipCategory;
    }

    public void setRelationshipCategory(String relationshipCategory) {
        this.relationshipCategory = relationshipCategory;
    }

    public Boolean getIsTemporal() {
        return isTemporal;
    }

    public void setIsTemporal(Boolean isTemporal) {
        this.isTemporal = isTemporal;
    }

    public Boolean getIsBidirectional() {
        return isBidirectional;
    }

    public void setIsBidirectional(Boolean isBidirectional) {
        this.isBidirectional = isBidirectional;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeliefRelationshipDocument that = (BeliefRelationshipDocument) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(agentId, that.agentId) &&
               Objects.equals(sourceBeliefId, that.sourceBeliefId) &&
               Objects.equals(targetBeliefId, that.targetBeliefId) &&
               Objects.equals(relationshipType, that.relationshipType) &&
               Objects.equals(strength, that.strength) &&
               Objects.equals(active, that.active) &&
               Objects.equals(reason, that.reason) &&
               Objects.equals(effectiveFrom, that.effectiveFrom) &&
               Objects.equals(effectiveUntil, that.effectiveUntil) &&
               Objects.equals(metadata, that.metadata) &&
               Objects.equals(createdAt, that.createdAt) &&
               Objects.equals(lastUpdated, that.lastUpdated) &&
               Objects.equals(version, that.version) &&
               Objects.equals(relationshipCategory, that.relationshipCategory) &&
               Objects.equals(isTemporal, that.isTemporal) &&
               Objects.equals(isBidirectional, that.isBidirectional);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, agentId, sourceBeliefId, targetBeliefId, relationshipType,
                          strength, active, reason, effectiveFrom, effectiveUntil, metadata,
                          createdAt, lastUpdated, version, relationshipCategory, isTemporal,
                          isBidirectional);
    }

    @Override
    public String toString() {
        return "BeliefRelationshipDocument{" +
               "id='" + id + '\'' +
               ", agentId='" + agentId + '\'' +
               ", sourceBeliefId='" + sourceBeliefId + '\'' +
               ", targetBeliefId='" + targetBeliefId + '\'' +
               ", relationshipType='" + relationshipType + '\'' +
               ", strength=" + strength +
               ", active=" + active +
               ", reason='" + reason + '\'' +
               ", effectiveFrom=" + effectiveFrom +
               ", effectiveUntil=" + effectiveUntil +
               ", metadata=" + metadata +
               ", createdAt=" + createdAt +
               ", lastUpdated=" + lastUpdated +
               ", version=" + version +
               ", relationshipCategory='" + relationshipCategory + '\'' +
               ", isTemporal=" + isTemporal +
               ", isBidirectional=" + isBidirectional +
               '}';
    }
}
