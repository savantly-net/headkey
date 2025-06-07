package ai.headkey.memory.dto;

import java.time.Instant;
import java.util.Objects;

/**
 * Data Transfer Object representing input for the memory ingestion process.
 * Used by the Information Ingestion Module (IIM) to capture all necessary
 * information for storing a new memory in the system.
 */
public class MemoryInput {
    
    /**
     * Identifier for the agent or context under which this memory is stored.
     * Supports multi-agent or multi-user scenarios by providing isolation
     * between different agents' memory spaces.
     */
    private String agentId;
    
    /**
     * The raw information or knowledge to store.
     * This could be a user utterance, fact, event description, or any textual content
     * that needs to be remembered by the system.
     */
    private String content;
    
    /**
     * Source or type of the information (optional).
     * Examples: "conversation", "sensor", "knowledge_base", "user_input", "system_event".
     * This field can guide the categorization process.
     */
    private String source;
    
    /**
     * Timestamp when this information was generated or received (optional).
     * Used for temporal context and forgetting decisions.
     * If not provided, the system will use the current timestamp.
     */
    private Instant timestamp;
    
    /**
     * Additional contextual metadata or hints (optional).
     * Could include explicit category tags, importance level, confidence scores,
     * or other relevant flags that can assist in processing the memory.
     */
    private Metadata metadata;
    
    /**
     * Default constructor.
     */
    public MemoryInput() {
    }
    
    /**
     * Constructor with required fields.
     * 
     * @param agentId The agent identifier
     * @param content The content to store
     */
    public MemoryInput(String agentId, String content) {
        this.agentId = agentId;
        this.content = content;
        this.timestamp = Instant.now();
    }
    
    /**
     * Constructor with agent, content, and source.
     * 
     * @param agentId The agent identifier
     * @param content The content to store
     * @param source The source of the information
     */
    public MemoryInput(String agentId, String content, String source) {
        this(agentId, content);
        this.source = source;
    }
    
    /**
     * Full constructor with all fields.
     * 
     * @param agentId The agent identifier
     * @param content The content to store
     * @param source The source of the information
     * @param timestamp When the information was generated
     * @param metadata Additional metadata
     */
    public MemoryInput(String agentId, String content, String source, Instant timestamp, Metadata metadata) {
        this.agentId = agentId;
        this.content = content;
        this.source = source;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.metadata = metadata;
    }
    
    /**
     * Validates that the input contains the minimum required information.
     * 
     * @return true if the input is valid for processing
     */
    public boolean isValid() {
        return agentId != null && !agentId.trim().isEmpty() &&
               content != null && !content.trim().isEmpty();
    }
    
    /**
     * Gets the effective timestamp, using current time if not set.
     * 
     * @return The timestamp to use for this memory
     */
    public Instant getEffectiveTimestamp() {
        return timestamp != null ? timestamp : Instant.now();
    }
    
    /**
     * Checks if metadata is present.
     * 
     * @return true if metadata is not null
     */
    public boolean hasMetadata() {
        return metadata != null;
    }
    
    /**
     * Gets metadata, creating an empty instance if null.
     * 
     * @return The metadata instance (never null)
     */
    public Metadata getOrCreateMetadata() {
        if (metadata == null) {
            metadata = new Metadata();
        }
        return metadata;
    }
    
    // Getters and Setters
    
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
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public Metadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemoryInput that = (MemoryInput) o;
        return Objects.equals(agentId, that.agentId) &&
                Objects.equals(content, that.content) &&
                Objects.equals(source, that.source) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(metadata, that.metadata);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(agentId, content, source, timestamp, metadata);
    }
    
    @Override
    public String toString() {
        return "MemoryInput{" +
                "agentId='" + agentId + '\'' +
                ", content='" + content + '\'' +
                ", source='" + source + '\'' +
                ", timestamp=" + timestamp +
                ", metadata=" + metadata +
                '}';
    }
}