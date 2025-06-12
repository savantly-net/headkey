package ai.headkey.rest.dto;

import ai.headkey.memory.dto.BeliefUpdateResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST API DTO for memory ingestion responses.
 * 
 * This class represents the JSON response returned to clients after
 * processing a memory ingestion request through the REST API.
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemoryIngestionResponse {
    
    /**
     * Indicates whether the operation was successful.
     */
    @JsonProperty("success")
    private boolean success;
    
    /**
     * Unique identifier assigned to the stored memory record.
     * Only present for successful operations that actually stored data.
     */
    @JsonProperty("memory_id")
    private String memoryId;
    
    /**
     * The category assigned to the content by the system.
     */
    @JsonProperty("category")
    private CategoryResponse category;
    
    /**
     * Agent ID associated with this memory.
     */
    @JsonProperty("agent_id")
    private String agentId;
    
    /**
     * Whether the memory was successfully encoded and stored.
     */
    @JsonProperty("encoded")
    private Boolean encoded;
    
    /**
     * Whether this was a dry run operation.
     */
    @JsonProperty("dry_run")
    private Boolean dryRun;
    
    /**
     * Preview data for dry run operations.
     */
    @JsonProperty("preview_data")
    private Map<String, Object> previewData;
    
    /**
     * List of belief IDs that were updated during ingestion.
     */
    @JsonProperty("updated_beliefs")
    private List<String> updatedBeliefs;
    
    /**
     * Processing time in milliseconds.
     */
    @JsonProperty("processing_time_ms")
    private Long processingTimeMs;
    
    /**
     * Timestamp when the operation was processed.
     */
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    /**
     * Error message if the operation failed.
     */
    @JsonProperty("error_message")
    private String errorMessage;
    
    /**
     * Detailed error information for debugging.
     */
    @JsonProperty("error_details")
    private Map<String, Object> errorDetails;
    
    /**
     * Result of belief analysis performed during ingestion.
     */
    @JsonProperty("belief_update_result")
    private BeliefUpdateResult beliefUpdateResult;
    
    /**
     * Default constructor.
     */
    public MemoryIngestionResponse() {
    }
    
    /**
     * Constructor for successful response.
     * 
     * @param memoryId The generated memory ID
     * @param category The assigned category
     * @param encoded Whether the memory was encoded
     */
    public MemoryIngestionResponse(String memoryId, CategoryResponse category, boolean encoded) {
        this.success = true;
        this.memoryId = memoryId;
        this.category = category;
        this.encoded = encoded;
        this.timestamp = Instant.now();
    }
    
    /**
     * Constructor for error response.
     * 
     * @param errorMessage The error message
     */
    public MemoryIngestionResponse(String errorMessage) {
        this.success = false;
        this.errorMessage = errorMessage;
        this.timestamp = Instant.now();
    }
    
    /**
     * Creates a successful response.
     * 
     * @param memoryId The memory ID
     * @param category The category
     * @return A successful response
     */
    public static MemoryIngestionResponse success(String memoryId, CategoryResponse category) {
        return new MemoryIngestionResponse(memoryId, category, true);
    }
    
    /**
     * Creates an error response.
     * 
     * @param errorMessage The error message
     * @return An error response
     */
    public static MemoryIngestionResponse error(String errorMessage) {
        return new MemoryIngestionResponse(errorMessage);
    }
    
    /**
     * Creates a dry run response.
     * 
     * @param category The predicted category
     * @param previewData The preview data
     * @return A dry run response
     */
    public static MemoryIngestionResponse dryRun(CategoryResponse category, Map<String, Object> previewData) {
        MemoryIngestionResponse response = new MemoryIngestionResponse();
        response.success = true;
        response.dryRun = true;
        response.category = category;
        response.previewData = previewData;
        response.encoded = false;
        response.timestamp = Instant.now();
        return response;
    }
    
    // Getters and Setters
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMemoryId() {
        return memoryId;
    }
    
    public void setMemoryId(String memoryId) {
        this.memoryId = memoryId;
    }
    
    public CategoryResponse getCategory() {
        return category;
    }
    
    public void setCategory(CategoryResponse category) {
        this.category = category;
    }
    
    public String getAgentId() {
        return agentId;
    }
    
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }
    
    public Boolean getEncoded() {
        return encoded;
    }
    
    public void setEncoded(Boolean encoded) {
        this.encoded = encoded;
    }
    
    public Boolean getDryRun() {
        return dryRun;
    }
    
    public void setDryRun(Boolean dryRun) {
        this.dryRun = dryRun;
    }
    
    public Map<String, Object> getPreviewData() {
        return previewData;
    }
    
    public void setPreviewData(Map<String, Object> previewData) {
        this.previewData = previewData;
    }
    
    public List<String> getUpdatedBeliefs() {
        return updatedBeliefs;
    }
    
    public void setUpdatedBeliefs(List<String> updatedBeliefs) {
        this.updatedBeliefs = updatedBeliefs;
    }
    
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Map<String, Object> getErrorDetails() {
        return errorDetails;
    }
    
    public void setErrorDetails(Map<String, Object> errorDetails) {
        this.errorDetails = errorDetails;
    }
    
    public BeliefUpdateResult getBeliefUpdateResult() {
        return beliefUpdateResult;
    }
    
    public void setBeliefUpdateResult(BeliefUpdateResult beliefUpdateResult) {
        this.beliefUpdateResult = beliefUpdateResult;
    }
    
    @Override
    public String toString() {
        return "MemoryIngestionResponse{" +
                "success=" + success +
                ", memoryId='" + memoryId + '\'' +
                ", category=" + category +
                ", agentId='" + agentId + '\'' +
                ", encoded=" + encoded +
                ", dryRun=" + dryRun +
                ", previewData=" + previewData +
                ", updatedBeliefs=" + updatedBeliefs +
                ", processingTimeMs=" + processingTimeMs +
                ", timestamp=" + timestamp +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorDetails=" + errorDetails +
                ", beliefUpdateResult=" + beliefUpdateResult +
                '}';
    }
    
    /**
     * Inner class representing category information in the response.
     */
    @RegisterForReflection
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CategoryResponse {
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("confidence")
        private Double confidence;
        
        @JsonProperty("tags")
        private List<String> tags;
        
        public CategoryResponse() {
        }
        
        public CategoryResponse(String name, Double confidence, List<String> tags) {
            this.name = name;
            this.confidence = confidence;
            this.tags = tags;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public Double getConfidence() {
            return confidence;
        }
        
        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }
        
        public List<String> getTags() {
            return tags;
        }
        
        public void setTags(List<String> tags) {
            this.tags = tags;
        }
        
        @Override
        public String toString() {
            return "CategoryResponse{" +
                    "name='" + name + '\'' +
                    ", confidence=" + confidence +
                    ", tags=" + tags +
                    '}';
        }
    }
}