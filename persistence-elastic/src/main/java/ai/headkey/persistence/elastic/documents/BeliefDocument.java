package ai.headkey.persistence.elastic.documents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Elasticsearch document representing a belief in the knowledge graph.
 * This document maps to the Belief DTO for persistence in Elasticsearch.
 *
 * The document is optimized for:
 * - Full-text search on belief statements
 * - Vector similarity search using dense_vector fields
 * - Efficient filtering by agent, category, and confidence
 * - Graph traversal queries for belief relationships
 * - Analytics and aggregations for belief statistics
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BeliefDocument {

    /**
     * Unique identifier for this belief document.
     * Maps to Elasticsearch _id field.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Agent identifier for belief isolation.
     * Indexed for efficient agent-based filtering.
     */
    @JsonProperty("agent_id")
    private String agentId;

    /**
     * The belief statement for full-text search.
     * Analyzed field for text search capabilities.
     */
    @JsonProperty("statement")
    private String statement;

    /**
     * Vector embedding of the statement for semantic similarity search.
     * Stored as dense_vector field in Elasticsearch.
     */
    @JsonProperty("statement_embedding")
    private List<Double> statementEmbedding;

    /**
     * Confidence level in this belief (0.0 to 1.0).
     * Numeric field for confidence-based filtering and sorting.
     */
    @JsonProperty("confidence")
    private Double confidence;

    /**
     * Set of memory IDs that provide evidence supporting this belief.
     * Keyword array for evidence-based queries.
     */
    @JsonProperty("evidence_memory_ids")
    private Set<String> evidenceMemoryIds;

    /**
     * Category or domain this belief belongs to.
     * Keyword field for category-based filtering and aggregations.
     */
    @JsonProperty("category")
    private String category;

    /**
     * Number of times this belief has been reinforced.
     * Numeric field for reinforcement analysis.
     */
    @JsonProperty("reinforcement_count")
    private Integer reinforcementCount;

    /**
     * Flag indicating whether this belief is currently active.
     * Boolean field for active/inactive filtering.
     */
    @JsonProperty("active")
    private Boolean active;

    /**
     * Optional tags or keywords associated with this belief.
     * Keyword array for tag-based filtering.
     */
    @JsonProperty("tags")
    private Set<String> tags;

    /**
     * Timestamp when belief was created.
     * Date field for temporal queries and sorting.
     */
    @JsonProperty("created_at")
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant createdAt;

    /**
     * Timestamp when belief was last updated or reinforced.
     * Date field for recency-based operations.
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
     * Default constructor.
     */
    public BeliefDocument() {
        this.confidence = 0.0;
        this.reinforcementCount = 0;
        this.active = true;
        this.version = 1L;
        this.createdAt = Instant.now();
        this.lastUpdated = Instant.now();
    }

    /**
     * Constructor with required fields.
     */
    public BeliefDocument(String id, String agentId, String statement) {
        this();
        this.id = id;
        this.agentId = agentId;
        this.statement = statement;
    }

    /**
     * Constructor with core fields.
     */
    public BeliefDocument(String id, String agentId, String statement, double confidence) {
        this(id, agentId, statement);
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    /**
     * Reinforces this belief by increasing confidence and updating timestamp.
     */
    public void reinforce(double additionalConfidence) {
        this.confidence = Math.min(1.0, this.confidence + additionalConfidence);
        this.reinforcementCount = (this.reinforcementCount != null ? this.reinforcementCount : 0) + 1;
        this.lastUpdated = Instant.now();
    }

    /**
     * Weakens this belief by decreasing confidence.
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
     * Updates the last updated timestamp.
     */
    public void updateLastUpdated() {
        this.lastUpdated = Instant.now();
    }

    /**
     * Checks if this belief has high confidence.
     */
    public boolean isHighConfidence(double threshold) {
        return confidence != null && confidence >= threshold;
    }

    /**
     * Checks if this belief has high confidence using default threshold of 0.8.
     */
    public boolean isHighConfidence() {
        return isHighConfidence(0.8);
    }

    /**
     * Gets the number of evidence items supporting this belief.
     */
    public int getEvidenceCount() {
        return evidenceMemoryIds != null ? evidenceMemoryIds.size() : 0;
    }

    /**
     * Checks if this belief is well-supported by evidence.
     */
    public boolean isWellSupported(int minEvidenceCount) {
        return getEvidenceCount() >= minEvidenceCount;
    }

    /**
     * Gets the age of this belief in seconds.
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

    public List<Double> getStatementEmbedding() {
        return statementEmbedding;
    }

    public void setStatementEmbedding(List<Double> statementEmbedding) {
        this.statementEmbedding = statementEmbedding;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence != null ? Math.max(0.0, Math.min(1.0, confidence)) : 0.0;
    }

    public Set<String> getEvidenceMemoryIds() {
        return evidenceMemoryIds;
    }

    public void setEvidenceMemoryIds(Set<String> evidenceMemoryIds) {
        this.evidenceMemoryIds = evidenceMemoryIds;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeliefDocument that = (BeliefDocument) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(agentId, that.agentId) &&
               Objects.equals(statement, that.statement) &&
               Objects.equals(statementEmbedding, that.statementEmbedding) &&
               Objects.equals(confidence, that.confidence) &&
               Objects.equals(evidenceMemoryIds, that.evidenceMemoryIds) &&
               Objects.equals(category, that.category) &&
               Objects.equals(reinforcementCount, that.reinforcementCount) &&
               Objects.equals(active, that.active) &&
               Objects.equals(tags, that.tags) &&
               Objects.equals(createdAt, that.createdAt) &&
               Objects.equals(lastUpdated, that.lastUpdated) &&
               Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, agentId, statement, statementEmbedding, confidence,
                          evidenceMemoryIds, category, reinforcementCount, active, tags,
                          createdAt, lastUpdated, version);
    }

    @Override
    public String toString() {
        return "BeliefDocument{" +
               "id='" + id + '\'' +
               ", agentId='" + agentId + '\'' +
               ", statement='" + statement + '\'' +
               ", confidence=" + confidence +
               ", evidenceMemoryIds=" + evidenceMemoryIds +
               ", category='" + category + '\'' +
               ", reinforcementCount=" + reinforcementCount +
               ", active=" + active +
               ", tags=" + tags +
               ", createdAt=" + createdAt +
               ", lastUpdated=" + lastUpdated +
               ", version=" + version +
               '}';
    }
}
