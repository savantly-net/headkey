package ai.headkey.memory.dto;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Data Transfer Object representing an individual belief held by the system.
 * Beliefs are distilled knowledge derived from memories and represent
 * the agent's understanding of facts, preferences, or relationships.
 */
public class Belief {
    
    /**
     * Unique identifier for this belief.
     */
    private String id;
    
    /**
     * The agent who holds this belief.
     */
    private String agentId;
    
    /**
     * Textual statement representing the belief.
     * Examples: "User's favorite color is blue", "Paris is the capital of France"
     */
    private String statement;
    
    /**
     * Confidence level in this belief (0.0 to 1.0).
     * Higher values indicate stronger confidence in the truth of the belief.
     */
    private double confidence;
    
    /**
     * Set of memory IDs that provide evidence supporting this belief.
     * Links the belief back to the specific memories that contributed to it.
     */
    private Set<String> evidenceMemoryIds;
    
    /**
     * Category or domain this belief belongs to.
     * Examples: "UserPreference", "WorldKnowledge", "PersonalData"
     */
    private String category;
    
    /**
     * Timestamp when this belief was first created.
     */
    private Instant createdAt;
    
    /**
     * Timestamp when this belief was last updated or reinforced.
     */
    private Instant lastUpdated;
    
    /**
     * Number of times this belief has been reinforced by new evidence.
     */
    private int reinforcementCount;
    
    /**
     * Flag indicating whether this belief is currently active.
     * Inactive beliefs may be deprecated or conflicted.
     */
    private boolean active;
    
    /**
     * Optional tags or keywords associated with this belief.
     */
    private Set<String> tags;
    
    /**
     * Default constructor.
     */
    public Belief() {
        this.evidenceMemoryIds = new HashSet<>();
        this.tags = new HashSet<>();
        this.createdAt = Instant.now();
        this.lastUpdated = Instant.now();
        this.reinforcementCount = 0;
        this.active = true;
        this.confidence = 0.0;
    }
    
    /**
     * Constructor with required fields.
     * 
     * @param id The belief identifier
     * @param agentId The agent identifier
     * @param statement The belief statement
     */
    public Belief(String id, String agentId, String statement) {
        this();
        this.id = id;
        this.agentId = agentId;
        this.statement = statement;
    }
    
    /**
     * Constructor with core fields.
     * 
     * @param id The belief identifier
     * @param agentId The agent identifier
     * @param statement The belief statement
     * @param confidence The confidence level
     */
    public Belief(String id, String agentId, String statement, double confidence) {
        this(id, agentId, statement);
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }
    
    /**
     * Full constructor.
     * 
     * @param id The belief identifier
     * @param agentId The agent identifier
     * @param statement The belief statement
     * @param confidence The confidence level
     * @param category The belief category
     * @param evidenceMemoryIds Supporting evidence memory IDs
     */
    public Belief(String id, String agentId, String statement, double confidence, 
                 String category, Set<String> evidenceMemoryIds) {
        this(id, agentId, statement, confidence);
        this.category = category;
        this.evidenceMemoryIds = new HashSet<>(evidenceMemoryIds != null ? evidenceMemoryIds : new HashSet<>());
    }
    
    /**
     * Adds evidence (memory ID) supporting this belief.
     * 
     * @param memoryId The ID of the memory that supports this belief
     */
    public void addEvidence(String memoryId) {
        if (memoryId != null && !memoryId.trim().isEmpty()) {
            if (evidenceMemoryIds == null) {
                evidenceMemoryIds = new HashSet<>();
            }
            evidenceMemoryIds.add(memoryId);
        }
    }
    
    /**
     * Removes evidence (memory ID) from this belief.
     * 
     * @param memoryId The memory ID to remove
     * @return true if the evidence was removed
     */
    public boolean removeEvidence(String memoryId) {
        return evidenceMemoryIds != null && evidenceMemoryIds.remove(memoryId);
    }
    
    /**
     * Reinforces this belief by increasing confidence and updating timestamp.
     * 
     * @param additionalConfidence Additional confidence to add (clamped to max 1.0)
     */
    public void reinforce(double additionalConfidence) {
        this.confidence = Math.min(1.0, this.confidence + additionalConfidence);
        this.reinforcementCount++;
        this.lastUpdated = Instant.now();
    }
    
    /**
     * Weakens this belief by decreasing confidence.
     * 
     * @param confidenceReduction Amount to reduce confidence (clamped to min 0.0)
     */
    public void weaken(double confidenceReduction) {
        this.confidence = Math.max(0.0, this.confidence - confidenceReduction);
        this.lastUpdated = Instant.now();
    }
    
    /**
     * Marks this belief as inactive (deprecated or conflicted).
     */
    public void deactivate() {
        this.active = false;
        this.lastUpdated = Instant.now();
    }
    
    /**
     * Reactivates this belief.
     */
    public void reactivate() {
        this.active = true;
        this.lastUpdated = Instant.now();
    }
    
    /**
     * Adds a tag to this belief.
     * 
     * @param tag The tag to add
     */
    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            if (tags == null) {
                tags = new HashSet<>();
            }
            tags.add(tag.trim());
        }
    }
    
    /**
     * Checks if this belief has high confidence.
     * 
     * @param threshold The confidence threshold
     * @return true if confidence is above the threshold
     */
    public boolean isHighConfidence(double threshold) {
        return confidence >= threshold;
    }
    
    /**
     * Checks if this belief has high confidence using default threshold of 0.8.
     * 
     * @return true if confidence is above 0.8
     */
    public boolean isHighConfidence() {
        return isHighConfidence(0.8);
    }
    
    /**
     * Gets the number of evidence items supporting this belief.
     * 
     * @return The count of supporting evidence
     */
    public int getEvidenceCount() {
        return evidenceMemoryIds != null ? evidenceMemoryIds.size() : 0;
    }
    
    /**
     * Checks if this belief is well-supported by evidence.
     * 
     * @param minEvidenceCount Minimum number of evidence items required
     * @return true if evidence count meets or exceeds the minimum
     */
    public boolean isWellSupported(int minEvidenceCount) {
        return getEvidenceCount() >= minEvidenceCount;
    }
    
    /**
     * Gets the age of this belief in seconds.
     * 
     * @return Age in seconds since creation
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
    
    public String getStatement() {
        return statement;
    }
    
    public void setStatement(String statement) {
        this.statement = statement;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }
    
    public Set<String> getEvidenceMemoryIds() {
        return evidenceMemoryIds != null ? new HashSet<>(evidenceMemoryIds) : new HashSet<>();
    }
    
    public void setEvidenceMemoryIds(Set<String> evidenceMemoryIds) {
        this.evidenceMemoryIds = new HashSet<>(evidenceMemoryIds != null ? evidenceMemoryIds : new HashSet<>());
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
    
    public int getReinforcementCount() {
        return reinforcementCount;
    }
    
    public void setReinforcementCount(int reinforcementCount) {
        this.reinforcementCount = reinforcementCount;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public Set<String> getTags() {
        return tags != null ? new HashSet<>(tags) : new HashSet<>();
    }
    
    public void setTags(Set<String> tags) {
        this.tags = new HashSet<>(tags != null ? tags : new HashSet<>());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Belief belief = (Belief) o;
        return Double.compare(belief.confidence, confidence) == 0 &&
                reinforcementCount == belief.reinforcementCount &&
                active == belief.active &&
                Objects.equals(id, belief.id) &&
                Objects.equals(agentId, belief.agentId) &&
                Objects.equals(statement, belief.statement) &&
                Objects.equals(evidenceMemoryIds, belief.evidenceMemoryIds) &&
                Objects.equals(category, belief.category) &&
                Objects.equals(createdAt, belief.createdAt) &&
                Objects.equals(lastUpdated, belief.lastUpdated) &&
                Objects.equals(tags, belief.tags);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, agentId, statement, confidence, evidenceMemoryIds, category, 
                          createdAt, lastUpdated, reinforcementCount, active, tags);
    }
    
    @Override
    public String toString() {
        return "Belief{" +
                "id='" + id + '\'' +
                ", agentId='" + agentId + '\'' +
                ", statement='" + statement + '\'' +
                ", confidence=" + confidence +
                ", evidenceMemoryIds=" + evidenceMemoryIds +
                ", category='" + category + '\'' +
                ", createdAt=" + createdAt +
                ", lastUpdated=" + lastUpdated +
                ", reinforcementCount=" + reinforcementCount +
                ", active=" + active +
                ", tags=" + tags +
                '}';
    }

    static public class Builder {

        private Belief belief;

        /**
         * Default constructor for the Belief Builder.
         * Initializes a new Belief instance with default values.
         */
        public Builder() {
            belief = new Belief();
        }

        /**
         * Constructor for the Belief Builder with required fields.
         * 
         * @param id The belief identifier
         * @param agentId The agent identifier
         * @param statement The belief statement
         */
        public Builder(String id, String agentId, String statement) {
            belief = new Belief(id, agentId, statement);
        }

        public Builder id(String id) {
            belief.setId(id);
            return this;
        }
        public Builder agentId(String agentId) {
            belief.setAgentId(agentId);
            return this;
        }
        public Builder statement(String statement) {
            belief.setStatement(statement);
            return this;
        }

        public Builder confidence(double confidence) {
            belief.setConfidence(confidence);
            return this;
        }

        public Builder category(String category) {
            belief.setCategory(category);
            return this;
        }

        public Builder evidenceMemoryIds(Set<String> evidenceMemoryIds) {
            belief.setEvidenceMemoryIds(evidenceMemoryIds);
            return this;
        }
        public Builder tags(Set<String> tags) {
            belief.setTags(tags);
            return this;
        }

        public Builder createdAt(Instant now) {
            belief.setCreatedAt(now);
            return this;
        }
        public Builder lastUpdated(Instant now) {
            belief.setLastUpdated(now);
            return this;
        }
        public Builder reinforcementCount(int count) {
            belief.setReinforcementCount(count);
            return this;
        }
        public Builder active(boolean active) {
            belief.setActive(active);
            return this;
        }
        public Builder evidenceMemoryIds(String memoryId) {
            belief.addEvidence(memoryId);
            return this;
        }
        public Builder addEvidence(String memoryId) {
            belief.addEvidence(memoryId);
            return this;
        }
        public Builder addTag(String tag) {
            belief.addTag(tag);
            return this;
        }

        public Belief build() {
            if (belief.getConfidence() < 0.0 || belief.getConfidence() > 1.0) {
                throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
            }
            return belief;
        }
    }
}