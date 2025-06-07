package ai.headkey.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

/**
 * REST API DTO for memory ingestion requests.
 * 
 * This class represents the JSON payload sent by clients when requesting
 * memory ingestion through the REST API. It provides validation and
 * conversion capabilities to the internal MemoryInput DTO.
 */
@RegisterForReflection
public class MemoryIngestionRequest {
    
    /**
     * Identifier for the agent or context under which this memory should be stored.
     * Required field that supports multi-agent scenarios.
     */
    @NotNull(message = "Agent ID cannot be null")
    @NotBlank(message = "Agent ID cannot be blank")
    @Size(min = 1, max = 100, message = "Agent ID must be between 1 and 100 characters")
    @JsonProperty("agent_id")
    private String agentId;
    
    /**
     * The raw information or knowledge to store.
     * This is the main content that will be processed and stored as a memory.
     */
    @NotNull(message = "Content cannot be null")
    @NotBlank(message = "Content cannot be blank")
    @Size(min = 1, max = 10000, message = "Content must be between 1 and 10000 characters")
    @JsonProperty("content")
    private String content;
    
    /**
     * Source or type of the information (optional).
     * Examples: "conversation", "sensor", "knowledge_base", "user_input", "system_event".
     */
    @JsonProperty("source")
    private String source;
    
    /**
     * Timestamp when this information was generated or received (optional).
     * If not provided, the server will use the current timestamp.
     * Expected format: ISO 8601 (e.g., "2023-12-01T10:30:00Z")
     */
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    /**
     * Additional contextual metadata or hints (optional).
     * Can include explicit category tags, importance level, confidence scores,
     * or other relevant flags that can assist in processing the memory.
     */
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    /**
     * Flag to indicate if this should be a dry run (preview only, no actual storage).
     * Defaults to false.
     */
    @JsonProperty("dry_run")
    private Boolean dryRun = false;
    
    /**
     * Default constructor for JSON deserialization.
     */
    public MemoryIngestionRequest() {
    }
    
    /**
     * Constructor with required fields.
     * 
     * @param agentId The agent identifier
     * @param content The content to store
     */
    public MemoryIngestionRequest(String agentId, String content) {
        this.agentId = agentId;
        this.content = content;
    }
    
    /**
     * Full constructor.
     * 
     * @param agentId The agent identifier
     * @param content The content to store
     * @param source The source of the information
     * @param timestamp When the information was generated
     * @param metadata Additional metadata
     * @param dryRun Whether this is a dry run
     */
    public MemoryIngestionRequest(String agentId, String content, String source, 
                                 Instant timestamp, Map<String, Object> metadata, 
                                 Boolean dryRun) {
        this.agentId = agentId;
        this.content = content;
        this.source = source;
        this.timestamp = timestamp;
        this.metadata = metadata;
        this.dryRun = dryRun != null ? dryRun : false;
    }
    
    /**
     * Validates the request has all required fields.
     * 
     * @return true if the request is valid
     */
    public boolean isValid() {
        return agentId != null && !agentId.trim().isEmpty() &&
               content != null && !content.trim().isEmpty();
    }
    
    /**
     * Gets the effective dry run flag, defaulting to false if null.
     * 
     * @return true if this should be a dry run
     */
    public boolean isDryRunEffective() {
        return dryRun != null && dryRun;
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
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public Boolean getDryRun() {
        return dryRun;
    }
    
    public void setDryRun(Boolean dryRun) {
        this.dryRun = dryRun;
    }
    
    @Override
    public String toString() {
        return "MemoryIngestionRequest{" +
                "agentId='" + agentId + '\'' +
                ", content='" + content + '\'' +
                ", source='" + source + '\'' +
                ", timestamp=" + timestamp +
                ", metadata=" + metadata +
                ", dryRun=" + dryRun +
                '}';
    }
}