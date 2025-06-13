package ai.headkey.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object for memory search requests.
 *
 * This DTO encapsulates the parameters needed to perform a similarity search
 * across stored memories for a specific agent. It supports various search
 * options including similarity thresholds and result limits.
 *
 * Example JSON:
 * {
 *   "agent_id": "user-123",
 *   "query": "machine learning concepts",
 *   "limit": 10,
 *   "similarity_threshold": 0.7
 * }
 */
public class MemorySearchRequest {

    @NotNull(message = "Agent ID is required")
    @NotBlank(message = "Agent ID cannot be blank")
    @JsonProperty("agent_id")
    private String agentId;

    @NotNull(message = "Query is required")
    @NotBlank(message = "Query cannot be blank")
    @JsonProperty("query")
    private String query;

    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 1000, message = "Limit cannot exceed 1000")
    @JsonProperty("limit")
    private Integer limit = 20; // Default limit

    @Min(value = 0, message = "Similarity threshold must be between 0.0 and 1.0")
    @Max(value = 1, message = "Similarity threshold must be between 0.0 and 1.0")
    @JsonProperty("similarity_threshold")
    private Double similarityThreshold;

    @JsonProperty("category")
    private String category;

    @JsonProperty("include_metadata")
    private Boolean includeMetadata = true;

    /**
     * Default constructor for JSON deserialization.
     */
    public MemorySearchRequest() {
    }

    /**
     * Constructor with required fields.
     */
    public MemorySearchRequest(String agentId, String query) {
        this.agentId = agentId;
        this.query = query;
    }

    /**
     * Full constructor.
     */
    public MemorySearchRequest(String agentId, String query, Integer limit,
                              Double similarityThreshold, String category, Boolean includeMetadata) {
        this.agentId = agentId;
        this.query = query;
        this.limit = limit;
        this.similarityThreshold = similarityThreshold;
        this.category = category;
        this.includeMetadata = includeMetadata;
    }

    // Getters and Setters

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getLimit() {
        return limit != null ? limit : 20;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(Double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getIncludeMetadata() {
        return includeMetadata != null ? includeMetadata : true;
    }

    public void setIncludeMetadata(Boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
    }

    /**
     * Validates the request parameters.
     *
     * @return true if the request is valid
     */
    public boolean isValid() {
        return agentId != null && !agentId.trim().isEmpty() &&
               query != null && !query.trim().isEmpty() &&
               (limit == null || (limit >= 1 && limit <= 1000)) &&
               (similarityThreshold == null || (similarityThreshold >= 0.0 && similarityThreshold <= 1.0));
    }

    @Override
    public String toString() {
        return "MemorySearchRequest{" +
                "agentId='" + agentId + '\'' +
                ", query='" + query + '\'' +
                ", limit=" + limit +
                ", similarityThreshold=" + similarityThreshold +
                ", category='" + category + '\'' +
                ", includeMetadata=" + includeMetadata +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MemorySearchRequest that = (MemorySearchRequest) o;

        if (!agentId.equals(that.agentId)) return false;
        if (!query.equals(that.query)) return false;
        if (limit != null ? !limit.equals(that.limit) : that.limit != null) return false;
        if (similarityThreshold != null ? !similarityThreshold.equals(that.similarityThreshold) : that.similarityThreshold != null)
            return false;
        if (category != null ? !category.equals(that.category) : that.category != null) return false;
        return includeMetadata != null ? includeMetadata.equals(that.includeMetadata) : that.includeMetadata == null;
    }

    @Override
    public int hashCode() {
        int result = agentId.hashCode();
        result = 31 * result + query.hashCode();
        result = 31 * result + (limit != null ? limit.hashCode() : 0);
        result = 31 * result + (similarityThreshold != null ? similarityThreshold.hashCode() : 0);
        result = 31 * result + (category != null ? category.hashCode() : 0);
        result = 31 * result + (includeMetadata != null ? includeMetadata.hashCode() : 0);
        return result;
    }
}
