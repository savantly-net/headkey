package ai.headkey.persistence.entities;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.persistence.entities.converters.CategoryLabelConverter;
import ai.headkey.persistence.entities.converters.MetadataConverter;
import ai.headkey.persistence.entities.converters.VectorEmbeddingConverter;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

/**
 * JPA Entity representing a stored memory record in the database.
 * 
 * This entity maps the MemoryRecord DTO to database tables and handles
 * the persistence of memory data including vector embeddings for similarity search.
 * 
 * The entity uses JSON storage for complex fields like CategoryLabel and Metadata
 * to maintain flexibility while leveraging JPA for basic CRUD operations.
 * 
 * Vector embeddings are stored as a separate column to enable efficient
 * similarity search operations using database-specific vector extensions.
 * 
 * @since 1.0
 */
@Entity
@Table(name = "memory_records", indexes = {
    @Index(name = "idx_memory_agent_id", columnList = "agent_id"),
    @Index(name = "idx_memory_created_at", columnList = "created_at"),
    @Index(name = "idx_memory_last_accessed", columnList = "last_accessed"),
    @Index(name = "idx_memory_relevance_score", columnList = "relevance_score"),
    @Index(name = "idx_memory_agent_created", columnList = "agent_id, created_at"),
    @Index(name = "idx_memory_agent_accessed", columnList = "agent_id, last_accessed")
})
@NamedQueries({
    @NamedQuery(
        name = "MemoryEntity.findByAgentId",
        query = "SELECT m FROM MemoryEntity m WHERE m.agentId = :agentId ORDER BY m.createdAt DESC"
    ),
    @NamedQuery(
        name = "MemoryEntity.findByAgentIdAndLimit",
        query = "SELECT m FROM MemoryEntity m WHERE m.agentId = :agentId ORDER BY m.createdAt DESC"
    ),
    @NamedQuery(
        name = "MemoryEntity.findOldMemories",
        query = "SELECT m FROM MemoryEntity m WHERE m.createdAt < :threshold ORDER BY m.createdAt ASC"
    ),
    @NamedQuery(
        name = "MemoryEntity.findOldMemoriesByAgent",
        query = "SELECT m FROM MemoryEntity m WHERE m.agentId = :agentId AND m.createdAt < :threshold ORDER BY m.createdAt ASC"
    ),
    @NamedQuery(
        name = "MemoryEntity.countByAgent",
        query = "SELECT COUNT(m) FROM MemoryEntity m WHERE m.agentId = :agentId"
    ),
    @NamedQuery(
        name = "MemoryEntity.countTotal",
        query = "SELECT COUNT(m) FROM MemoryEntity m"
    )
})
public class MemoryEntity {
    
    @Id
    @Column(name = "id", length = 50)
    private String id;
    
    @Column(name = "agent_id", nullable = false, length = 100)
    private String agentId;
    
    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    /**
     * CategoryLabel stored as JSON.
     * Uses @Convert annotation with custom AttributeConverter for JSON serialization.
     */
    @Convert(converter = CategoryLabelConverter.class)
    @Column(name = "category", columnDefinition = "TEXT")
    private CategoryLabel category;
    
    /**
     * Metadata stored as JSON.
     * Uses @Convert annotation with custom AttributeConverter for JSON serialization.
     */
    @Convert(converter = MetadataConverter.class)
    @Column(name = "metadata", columnDefinition = "TEXT")
    private Metadata metadata;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "last_accessed")
    private Instant lastAccessed;
    
    @Column(name = "relevance_score")
    private Double relevanceScore;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    /**
     * Vector embedding stored as binary data or JSON array.
     * The storage format depends on database capabilities:
     * - PostgreSQL: Can use vector extension or bytea
     * - Other databases: JSON array or binary storage
     */
    @Convert(converter = VectorEmbeddingConverter.class)
    @Column(name = "embedding", columnDefinition = "TEXT")
    private double[] embedding;
    
    /**
     * Precomputed embedding magnitude for similarity calculations.
     * This optimization avoids recalculating vector norms during similarity search.
     */
    @Column(name = "embedding_magnitude")
    private Double embeddingMagnitude;
    
    /**
     * Default constructor required by JPA.
     */
    public MemoryEntity() {
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
    public MemoryEntity(String id, String agentId, String content) {
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
    public MemoryEntity(String id, String agentId, String content, CategoryLabel category) {
        this(id, agentId, content);
        this.category = category;
    }
    
    /**
     * Converts this entity to a MemoryRecord DTO.
     * 
     * @return MemoryRecord representation of this entity
     */
    public MemoryRecord toMemoryRecord() {
        MemoryRecord record = new MemoryRecord(id, agentId, content, category, metadata, createdAt);
        record.setLastAccessed(lastAccessed);
        record.setRelevanceScore(relevanceScore);
        record.setVersion(version);
        return record;
    }
    
    /**
     * Creates a MemoryEntity from a MemoryRecord DTO.
     * 
     * @param memoryRecord The MemoryRecord to convert
     * @return MemoryEntity representation
     */
    public static MemoryEntity fromMemoryRecord(MemoryRecord memoryRecord) {
        MemoryEntity entity = new MemoryEntity();
        entity.setId(memoryRecord.getId());
        entity.setAgentId(memoryRecord.getAgentId());
        entity.setContent(memoryRecord.getContent());
        entity.setCategory(memoryRecord.getCategory());
        entity.setMetadata(memoryRecord.getMetadata());
        entity.setCreatedAt(memoryRecord.getCreatedAt());
        entity.setLastAccessed(memoryRecord.getLastAccessed());
        entity.setRelevanceScore(memoryRecord.getRelevanceScore());
        entity.setVersion(memoryRecord.getVersion());
        return entity;
    }
    
    /**
     * Updates the last accessed timestamp to the current time.
     */
    public void updateLastAccessed() {
        this.lastAccessed = Instant.now();
        if (metadata != null) {
            metadata.incrementAccessCount();
        }
    }
    
    /**
     * Sets the vector embedding and calculates its magnitude.
     * 
     * @param embedding The vector embedding array
     */
    public void setEmbedding(double[] embedding) {
        this.embedding = embedding;
        if (embedding != null) {
            // Calculate and store magnitude for optimization
            double magnitude = 0.0;
            for (double value : embedding) {
                magnitude += value * value;
            }
            this.embeddingMagnitude = Math.sqrt(magnitude);
        } else {
            this.embeddingMagnitude = null;
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
     * @param threshold The relevance threshold
     * @return true if relevance score is above the threshold
     */
    public boolean isHighlyRelevant(double threshold) {
        return relevanceScore != null && relevanceScore >= threshold;
    }
    
    // JPA lifecycle callbacks
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (lastAccessed == null) {
            lastAccessed = createdAt;
        }
        if (version == null) {
            version = 1L;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        // Version is automatically handled by @Version annotation
        // Could add additional update logic here if needed
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
    
    public double[] getEmbedding() {
        return embedding;
    }
    
    public Double getEmbeddingMagnitude() {
        return embeddingMagnitude;
    }
    
    public void setEmbeddingMagnitude(Double embeddingMagnitude) {
        this.embeddingMagnitude = embeddingMagnitude;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemoryEntity that = (MemoryEntity) o;
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
        return "MemoryEntity{" +
                "id='" + id + '\'' +
                ", agentId='" + agentId + '\'' +
                ", content='" + content + '\'' +
                ", category=" + category +
                ", metadata=" + metadata +
                ", createdAt=" + createdAt +
                ", lastAccessed=" + lastAccessed +
                ", relevanceScore=" + relevanceScore +
                ", version=" + version +
                ", embeddingMagnitude=" + embeddingMagnitude +
                '}';
    }
}