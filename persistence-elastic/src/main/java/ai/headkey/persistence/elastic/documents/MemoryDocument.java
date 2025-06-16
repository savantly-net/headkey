package ai.headkey.persistence.elastic.documents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Elasticsearch document representing a stored memory record.
 * This document maps to the MemoryRecord DTO for persistence in Elasticsearch.
 *
 * The document is optimized for:
 * - Full-text search on content
 * - Vector similarity search using dense_vector fields
 * - Efficient filtering by agent, category, and temporal ranges
 * - Analytics and aggregations for memory statistics
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MemoryDocument {

    /**
     * Unique identifier for this memory document.
     * Maps to Elasticsearch _id field.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Agent identifier for memory isolation.
     * Indexed for efficient agent-based filtering.
     */
    @JsonProperty("agent_id")
    private String agentId;

    /**
     * The memory content for full-text search.
     * Analyzed field for text search capabilities.
     */
    @JsonProperty("content")
    private String content;

    /**
     * Vector embedding of the content for semantic similarity search.
     * Stored as dense_vector field in Elasticsearch.
     */
    @JsonProperty("content_embedding")
    private List<Double> contentEmbedding;

    /**
     * Primary category assigned by the categorization engine.
     * Keyword field for exact matching and aggregations.
     */
    @JsonProperty("primary_category")
    private String primaryCategory;

    /**
     * Secondary category for additional classification.
     * Keyword field for filtering and aggregations.
     */
    @JsonProperty("secondary_category")
    private String secondaryCategory;

    /**
     * Tags associated with this memory.
     * Keyword array for multi-tag filtering.
     */
    @JsonProperty("tags")
    private List<String> tags;

    /**
     * Category confidence score.
     * Numeric field for scoring and filtering.
     */
    @JsonProperty("category_confidence")
    private Double categoryConfidence;

    /**
     * Memory relevance score.
     * Numeric field for relevance-based filtering and sorting.
     */
    @JsonProperty("relevance_score")
    private Double relevanceScore;

    /**
     * Memory importance score from metadata.
     * Numeric field for importance-based operations.
     */
    @JsonProperty("importance_score")
    private Double importanceScore;

    /**
     * Source of the memory (e.g., "chat", "document", "api").
     * Keyword field for source-based filtering.
     */
    @JsonProperty("source")
    private String source;

    /**
     * Access count for usage-based forgetting.
     * Numeric field for frequency analysis.
     */
    @JsonProperty("access_count")
    private Integer accessCount;

    /**
     * Timestamp when memory was created.
     * Date field for temporal queries and sorting.
     */
    @JsonProperty("created_at")
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant createdAt;

    /**
     * Timestamp when memory was last accessed.
     * Date field for recency-based operations.
     */
    @JsonProperty("last_accessed")
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant lastAccessed;

    /**
     * Timestamp when memory was last updated.
     * Date field for modification tracking.
     */
    @JsonProperty("last_updated")
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant lastUpdated;

    /**
     * Additional metadata stored as nested object.
     * Allows for flexible metadata without schema changes.
     */
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    /**
     * Document version for optimistic locking.
     * Numeric field for version control.
     */
    @JsonProperty("version")
    private Long version;

    /**
     * Flag indicating if memory is active.
     * Boolean field for active/inactive filtering.
     */
    @JsonProperty("active")
    private Boolean active;

    /**
     * Default constructor.
     */
    public MemoryDocument() {
        this.active = true;
        this.version = 1L;
        this.accessCount = 0;
        this.createdAt = Instant.now();
        this.lastAccessed = Instant.now();
        this.lastUpdated = Instant.now();
    }

    /**
     * Constructor with required fields.
     */
    public MemoryDocument(String id, String agentId, String content) {
        this();
        this.id = id;
        this.agentId = agentId;
        this.content = content;
    }

    /**
     * Updates the last accessed timestamp and increments access count.
     */
    public void updateLastAccessed() {
        this.lastAccessed = Instant.now();
        this.accessCount = (this.accessCount != null ? this.accessCount : 0) + 1;
    }

    /**
     * Updates the last updated timestamp.
     */
    public void updateLastUpdated() {
        this.lastUpdated = Instant.now();
    }

    /**
     * Checks if this memory is recent based on a time threshold.
     */
    public boolean isRecentlyCreated(Instant threshold) {
        return createdAt != null && createdAt.isAfter(threshold);
    }

    /**
     * Checks if this memory was recently accessed.
     */
    public boolean isRecentlyAccessed(Instant threshold) {
        return lastAccessed != null && lastAccessed.isAfter(threshold);
    }

    /**
     * Gets the age of this memory in seconds.
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<Double> getContentEmbedding() {
        return contentEmbedding;
    }

    public void setContentEmbedding(List<Double> contentEmbedding) {
        this.contentEmbedding = contentEmbedding;
    }

    public String getPrimaryCategory() {
        return primaryCategory;
    }

    public void setPrimaryCategory(String primaryCategory) {
        this.primaryCategory = primaryCategory;
    }

    public String getSecondaryCategory() {
        return secondaryCategory;
    }

    public void setSecondaryCategory(String secondaryCategory) {
        this.secondaryCategory = secondaryCategory;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Double getCategoryConfidence() {
        return categoryConfidence;
    }

    public void setCategoryConfidence(Double categoryConfidence) {
        this.categoryConfidence = categoryConfidence;
    }

    public Double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(Double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public Double getImportanceScore() {
        return importanceScore;
    }

    public void setImportanceScore(Double importanceScore) {
        this.importanceScore = importanceScore;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(Integer accessCount) {
        this.accessCount = accessCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(Instant lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemoryDocument that = (MemoryDocument) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(agentId, that.agentId) &&
               Objects.equals(content, that.content) &&
               Objects.equals(contentEmbedding, that.contentEmbedding) &&
               Objects.equals(primaryCategory, that.primaryCategory) &&
               Objects.equals(secondaryCategory, that.secondaryCategory) &&
               Objects.equals(tags, that.tags) &&
               Objects.equals(categoryConfidence, that.categoryConfidence) &&
               Objects.equals(relevanceScore, that.relevanceScore) &&
               Objects.equals(importanceScore, that.importanceScore) &&
               Objects.equals(source, that.source) &&
               Objects.equals(accessCount, that.accessCount) &&
               Objects.equals(createdAt, that.createdAt) &&
               Objects.equals(lastAccessed, that.lastAccessed) &&
               Objects.equals(lastUpdated, that.lastUpdated) &&
               Objects.equals(metadata, that.metadata) &&
               Objects.equals(version, that.version) &&
               Objects.equals(active, that.active);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, agentId, content, contentEmbedding, primaryCategory,
                          secondaryCategory, tags, categoryConfidence, relevanceScore,
                          importanceScore, source, accessCount, createdAt, lastAccessed,
                          lastUpdated, metadata, version, active);
    }

    @Override
    public String toString() {
        return "MemoryDocument{" +
               "id='" + id + '\'' +
               ", agentId='" + agentId + '\'' +
               ", content='" + content + '\'' +
               ", primaryCategory='" + primaryCategory + '\'' +
               ", secondaryCategory='" + secondaryCategory + '\'' +
               ", tags=" + tags +
               ", categoryConfidence=" + categoryConfidence +
               ", relevanceScore=" + relevanceScore +
               ", importanceScore=" + importanceScore +
               ", source='" + source + '\'' +
               ", accessCount=" + accessCount +
               ", createdAt=" + createdAt +
               ", lastAccessed=" + lastAccessed +
               ", lastUpdated=" + lastUpdated +
               ", version=" + version +
               ", active=" + active +
               '}';
    }
}
