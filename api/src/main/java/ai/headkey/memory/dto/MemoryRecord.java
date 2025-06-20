package ai.headkey.memory.dto;

import java.time.Instant;
import java.util.Objects;

/**
 * Data Transfer Object representing a stored memory entry in the system.
 * This is the core unit of storage that contains all information about
 * a persisted memory, including its content, categorization, and metadata.
 */
public class MemoryRecord {
    
    /**
     * Unique identifier for this memory record.
     * Generated by the Memory Encoding System (MES) when the memory is stored.
     */
    private String id;
    
    /**
     * Identifier for the agent or context that owns this memory.
     * Provides isolation between different agents' memory spaces.
     */
    private String agentId;
    
    /**
     * The original content that was stored.
     * This is the raw information or knowledge that the system remembered.
     */
    private String content;
    
    /**
     * The category label assigned by the Contextual Categorization Engine (CCE).
     * Contains the primary/secondary categories, tags, and confidence score.
     */
    private CategoryLabel category;
    
    /**
     * Additional metadata associated with this memory.
     * Includes source information, importance, tags, access statistics, etc.
     */
    private Metadata metadata;
    
    /**
     * Timestamp when this memory was created and stored in the system.
     */
    private Instant createdAt;
    
    /**
     * Timestamp when this memory was last accessed or retrieved.
     * Used by the Relevance Evaluation & Forgetting Agent (REFA) for usage-based forgetting.
     */
    private Instant lastAccessed;
    
    /**
     * Current relevance score for this memory (0.0 to 1.0).
     * Computed by REFA based on various factors like recency, frequency, importance.
     */
    private Double relevanceScore;
    
    /**
     * Version number for this memory record.
     * Can be used for optimistic locking or tracking changes.
     */
    private Long version;
    
    /**
     * Default constructor.
     */
    public MemoryRecord() {
        this.createdAt = Instant.now();
        this.lastAccessed = Instant.now();
        this.version = 1L;
    }
    
    /**
     * Constructor with required fields.
     * 
     * @param id The unique identifier
     * @param agentId The agent identifier
     * @param content The memory content
     */
    public MemoryRecord(String id, String agentId, String content) {
        this();
        this.id = id;
        this.agentId = agentId;
        this.content = content;
    }
    
    /**
     * Constructor with core fields including category.
     * 
     * @param id The unique identifier
     * @param agentId The agent identifier
     * @param content The memory content
     * @param category The category label
     */
    public MemoryRecord(String id, String agentId, String content, CategoryLabel category) {
        this(id, agentId, content);
        this.category = category;
    }
    
    /**
     * Full constructor with all fields.
     * 
     * @param id The unique identifier
     * @param agentId The agent identifier
     * @param content The memory content
     * @param category The category label
     * @param metadata Additional metadata
     * @param createdAt Creation timestamp
     */
    public MemoryRecord(String id, String agentId, String content, CategoryLabel category, 
                       Metadata metadata, Instant createdAt) {
        this.id = id;
        this.agentId = agentId;
        this.content = content;
        this.category = category;
        this.metadata = metadata;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.lastAccessed = this.createdAt;
        this.version = 1L;
    }
    
    /**
     * Updates the last accessed timestamp to the current time.
     * Should be called whenever this memory is retrieved or accessed.
     */
    public void updateLastAccessed() {
        this.lastAccessed = Instant.now();
        if (metadata != null) {
            metadata.incrementAccessCount();
        }
    }
    
    /**
     * Checks if this memory is recent based on a time threshold.
     * 
     * @param threshold The time threshold to compare against
     * @return true if the memory was created after the threshold
     */
    public boolean isRecentlyCreated(Instant threshold) {
        return createdAt != null && createdAt.isAfter(threshold);
    }
    
    /**
     * Checks if this memory was recently accessed based on a time threshold.
     * 
     * @param threshold The time threshold to compare against
     * @return true if the memory was accessed after the threshold
     */
    public boolean isRecentlyAccessed(Instant threshold) {
        return lastAccessed != null && lastAccessed.isAfter(threshold);
    }
    
    /**
     * Gets the age of this memory in seconds.
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
     * Checks if this memory has a high relevance score.
     * 
     * @param threshold The relevance threshold (default 0.7 if not specified)
     * @return true if relevance score is above the threshold
     */
    public boolean isHighlyRelevant(double threshold) {
        return relevanceScore != null && relevanceScore >= threshold;
    }
    
    /**
     * Checks if this memory has a high relevance score using default threshold of 0.7.
     * 
     * @return true if relevance score is above 0.7
     */
    public boolean isHighlyRelevant() {
        return isHighlyRelevant(0.7);
    }
    
    /**
     * Gets the access count from metadata, or 0 if not available.
     * 
     * @return The number of times this memory has been accessed
     */
    public int getAccessCount() {
        if (metadata != null && metadata.getAccessCount() != null) {
            return metadata.getAccessCount();
        }
        return 0;
    }
    
    /**
     * Creates a copy of this memory record with a new ID.
     * Useful for creating derived or archived versions.
     * 
     * @param newId The new identifier for the copy
     * @return A new MemoryRecord instance with the same content but different ID
     */
    public MemoryRecord copyWithNewId(String newId) {
        MemoryRecord copy = new MemoryRecord(newId, agentId, content, category, metadata, createdAt);
        copy.setLastAccessed(lastAccessed);
        copy.setRelevanceScore(relevanceScore);
        copy.setVersion(1L); // Reset version for the copy
        return copy;
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
    
    public CategoryLabel getCategory() {
        return category;
    }
    
    public void setCategory(CategoryLabel category) {
        this.category = category;
    }
    
    public Metadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
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
    
    public Double getRelevanceScore() {
        return relevanceScore;
    }
    
    public void setRelevanceScore(Double relevanceScore) {
        this.relevanceScore = relevanceScore;
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
        MemoryRecord that = (MemoryRecord) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(agentId, that.agentId) &&
                Objects.equals(content, that.content) &&
                Objects.equals(category, that.category) &&
                Objects.equals(metadata, that.metadata) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(lastAccessed, that.lastAccessed) &&
                Objects.equals(relevanceScore, that.relevanceScore) &&
                Objects.equals(version, that.version);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, agentId, content, category, metadata, createdAt, 
                          lastAccessed, relevanceScore, version);
    }
    
    @Override
    public String toString() {
        return "MemoryRecord{" +
                "id='" + id + '\'' +
                ", agentId='" + agentId + '\'' +
                ", content='" + content + '\'' +
                ", category=" + category +
                ", metadata=" + metadata +
                ", createdAt=" + createdAt +
                ", lastAccessed=" + lastAccessed +
                ", relevanceScore=" + relevanceScore +
                ", version=" + version +
                '}';
    }
}