package ai.headkey.memory.dto;

import ai.headkey.memory.enums.RelationshipType;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Data Transfer Object representing a directed relationship between two beliefs in the knowledge graph.
 * 
 * This class models the edges in the belief knowledge graph, supporting rich metadata,
 * temporal relationships for belief deprecation, and various semantic connections.
 * Relationships are directed from source to target belief and can include strength
 * measurements, validity periods, and custom metadata.
 * 
 * @since 1.0
 */
public class BeliefRelationship {
    
    /**
     * Unique identifier for this relationship.
     */
    private String id;
    
    /**
     * ID of the source belief (the relationship originates from this belief).
     */
    private String sourceBeliefId;
    
    /**
     * ID of the target belief (the relationship points to this belief).
     */
    private String targetBeliefId;
    
    /**
     * ID of the agent who owns this relationship.
     */
    private String agentId;
    
    /**
     * The type of relationship between the beliefs.
     */
    private RelationshipType relationshipType;
    
    /**
     * Strength or confidence of this relationship (0.0 to 1.0).
     * Higher values indicate stronger or more confident relationships.
     */
    private double strength;
    
    /**
     * Flexible metadata for storing custom relationship properties.
     * Can include contextual information, source references, confidence metrics, etc.
     */
    private Map<String, Object> metadata;
    
    /**
     * Timestamp when this relationship was created.
     */
    private Instant createdAt;
    
    /**
     * Timestamp when this relationship was last updated.
     */
    private Instant lastUpdated;
    
    /**
     * Flag indicating whether this relationship is currently active.
     */
    private boolean active;
    
    // Temporal-specific fields for time-bound relationships
    
    /**
     * Timestamp when this relationship becomes effective.
     * Null means effective immediately.
     */
    private Instant effectiveFrom;
    
    /**
     * Timestamp when this relationship expires or becomes invalid.
     * Null means no expiration.
     */
    private Instant effectiveUntil;
    
    /**
     * Human-readable reason for deprecation (used with temporal relationships).
     */
    private String deprecationReason;
    
    /**
     * Priority or weight of this relationship compared to others of the same type.
     * Higher values indicate higher priority.
     */
    private Integer priority;
    
    /**
     * Default constructor.
     */
    public BeliefRelationship() {
        this.metadata = new HashMap<>();
        this.createdAt = Instant.now();
        this.lastUpdated = Instant.now();
        this.active = true;
        this.strength = 1.0;
        this.priority = 0;
    }
    
    /**
     * Constructor with required fields.
     * 
     * @param sourceBeliefId The source belief ID
     * @param targetBeliefId The target belief ID
     * @param relationshipType The type of relationship
     * @param agentId The agent ID
     */
    public BeliefRelationship(String sourceBeliefId, String targetBeliefId, 
                             RelationshipType relationshipType, String agentId) {
        this();
        this.sourceBeliefId = sourceBeliefId;
        this.targetBeliefId = targetBeliefId;
        this.relationshipType = relationshipType;
        this.agentId = agentId;
    }
    
    /**
     * Constructor with core fields.
     * 
     * @param sourceBeliefId The source belief ID
     * @param targetBeliefId The target belief ID
     * @param relationshipType The type of relationship
     * @param strength The relationship strength
     * @param agentId The agent ID
     */
    public BeliefRelationship(String sourceBeliefId, String targetBeliefId, 
                             RelationshipType relationshipType, double strength, String agentId) {
        this(sourceBeliefId, targetBeliefId, relationshipType, agentId);
        this.strength = Math.max(0.0, Math.min(1.0, strength));
    }
    
    /**
     * Full constructor for temporal relationships.
     * 
     * @param id The relationship ID
     * @param sourceBeliefId The source belief ID
     * @param targetBeliefId The target belief ID
     * @param relationshipType The type of relationship
     * @param strength The relationship strength
     * @param agentId The agent ID
     * @param effectiveFrom When the relationship becomes effective
     * @param effectiveUntil When the relationship expires
     * @param deprecationReason Reason for deprecation
     */
    public BeliefRelationship(String id, String sourceBeliefId, String targetBeliefId, 
                             RelationshipType relationshipType, double strength, String agentId,
                             Instant effectiveFrom, Instant effectiveUntil, String deprecationReason) {
        this(sourceBeliefId, targetBeliefId, relationshipType, strength, agentId);
        this.id = id;
        this.effectiveFrom = effectiveFrom;
        this.effectiveUntil = effectiveUntil;
        this.deprecationReason = deprecationReason;
    }
    
    /**
     * Adds metadata to this relationship.
     * 
     * @param key The metadata key
     * @param value The metadata value
     */
    public void addMetadata(String key, Object value) {
        if (key != null && value != null) {
            if (metadata == null) {
                metadata = new HashMap<>();
            }
            metadata.put(key, value);
        }
    }
    
    /**
     * Gets metadata value by key.
     * 
     * @param key The metadata key
     * @return The metadata value, or null if not found
     */
    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
    
    /**
     * Gets metadata value as string.
     * 
     * @param key The metadata key
     * @return The metadata value as string, or null if not found
     */
    public String getMetadataAsString(String key) {
        Object value = getMetadata(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Checks if this relationship is currently effective based on temporal constraints.
     * 
     * @return true if the relationship is currently effective
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
     * 
     * @param timestamp The timestamp to check
     * @return true if the relationship was effective at the given time
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
     * Checks if this is a temporal relationship type.
     * 
     * @return true if this relationship type is temporal
     */
    public boolean isTemporal() {
        return relationshipType != null && relationshipType.isTemporal();
    }
    
    /**
     * Checks if this relationship deprecates the target belief.
     * 
     * @return true if this relationship deprecates the target belief
     */
    public boolean isDeprecating() {
        return relationshipType != null && relationshipType.isDeprecating();
    }
    
    /**
     * Checks if this relationship is bidirectional by nature.
     * 
     * @return true if this relationship type is typically bidirectional
     */
    public boolean isBidirectional() {
        return relationshipType != null && relationshipType.isBidirectional();
    }
    
    /**
     * Updates the relationship strength.
     * 
     * @param newStrength The new strength value (clamped to 0.0-1.0)
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
     * 
     * @param from When the relationship becomes effective
     * @param until When the relationship expires
     */
    public void setEffectivePeriod(Instant from, Instant until) {
        this.effectiveFrom = from;
        this.effectiveUntil = until;
        this.lastUpdated = Instant.now();
    }
    
    /**
     * Gets the age of this relationship in seconds.
     * 
     * @return Age in seconds since creation
     */
    public long getAgeInSeconds() {
        if (createdAt == null) {
            return 0;
        }
        return Instant.now().getEpochSecond() - createdAt.getEpochSecond();
    }
    
    /**
     * Checks if this relationship has high strength.
     * 
     * @param threshold The strength threshold
     * @return true if strength is above the threshold
     */
    public boolean isHighStrength(double threshold) {
        return strength >= threshold;
    }
    
    /**
     * Checks if this relationship has high strength using default threshold of 0.8.
     * 
     * @return true if strength is above 0.8
     */
    public boolean isHighStrength() {
        return isHighStrength(0.8);
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
    
    public double getStrength() {
        return strength;
    }
    
    public void setStrength(double strength) {
        this.strength = Math.max(0.0, Math.min(1.0, strength));
    }
    
    public Map<String, Object> getMetadata() {
        return metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = new HashMap<>(metadata != null ? metadata : new HashMap<>());
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
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
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
    
    // Fluent setters
    public BeliefRelationship withId(String id) {
        this.id = id;
        return this;
    }
    public BeliefRelationship withSourceBeliefId(String sourceBeliefId) {
        this.sourceBeliefId = sourceBeliefId;
        return this;
    }
    public BeliefRelationship withTargetBeliefId(String targetBeliefId) {
        this.targetBeliefId = targetBeliefId;
        return this;
    }
    public BeliefRelationship withAgentId(String agentId) {
        this.agentId = agentId;
        return this;
    }
    public BeliefRelationship withRelationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
        return this;
    }
    public BeliefRelationship withStrength(double strength) {
        this.strength = Math.max(0.0, Math.min(1.0, strength));
        return this;
    }
    public BeliefRelationship withMetadata(Map<String, Object> metadata) {
        this.metadata = new HashMap<>(metadata != null ? metadata : new HashMap<>());
        return this;
    }
    public BeliefRelationship withCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }
    public BeliefRelationship withLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }
    public BeliefRelationship withActive(boolean active) {
        this.active = active;
        return this;
    }
    public BeliefRelationship withEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
        return this;
    }
    public BeliefRelationship withEffectiveUntil(Instant effectiveUntil) {
        this.effectiveUntil = effectiveUntil;
        return this;
    }
    public BeliefRelationship withDeprecationReason(String deprecationReason) {
        this.deprecationReason = deprecationReason;
        return this;
    }
    public BeliefRelationship withPriority(Integer priority) {
        this.priority = priority;
        return this;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeliefRelationship that = (BeliefRelationship) o;
        return Double.compare(that.strength, strength) == 0 &&
                active == that.active &&
                Objects.equals(id, that.id) &&
                Objects.equals(sourceBeliefId, that.sourceBeliefId) &&
                Objects.equals(targetBeliefId, that.targetBeliefId) &&
                Objects.equals(agentId, that.agentId) &&
                relationshipType == that.relationshipType &&
                Objects.equals(metadata, that.metadata) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(lastUpdated, that.lastUpdated) &&
                Objects.equals(effectiveFrom, that.effectiveFrom) &&
                Objects.equals(effectiveUntil, that.effectiveUntil) &&
                Objects.equals(deprecationReason, that.deprecationReason) &&
                Objects.equals(priority, that.priority);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, sourceBeliefId, targetBeliefId, agentId, relationshipType, 
                          strength, metadata, createdAt, lastUpdated, active, effectiveFrom, 
                          effectiveUntil, deprecationReason, priority);
    }
    
    @Override
    public String toString() {
        return "BeliefRelationship{" +
                "id='" + id + '\'' +
                ", sourceBeliefId='" + sourceBeliefId + '\'' +
                ", targetBeliefId='" + targetBeliefId + '\'' +
                ", agentId='" + agentId + '\'' +
                ", relationshipType=" + relationshipType +
                ", strength=" + strength +
                ", active=" + active +
                ", effectiveFrom=" + effectiveFrom +
                ", effectiveUntil=" + effectiveUntil +
                ", deprecationReason='" + deprecationReason + '\'' +
                ", priority=" + priority +
                ", createdAt=" + createdAt +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}