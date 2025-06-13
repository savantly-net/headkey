package ai.headkey.rest.dto;

import ai.headkey.memory.dto.MemoryRecord;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for memory search responses.
 *
 * This DTO encapsulates the results of a memory similarity search operation,
 * including the matching memories, relevance scores, and metadata about the
 * search operation itself.
 *
 * Example JSON response:
 * {
 *   "results": [
 *     {
 *       "memory_id": "mem-123",
 *       "content": "Machine learning is a subset of AI...",
 *       "relevance_score": 0.95,
 *       "category": "technology",
 *       "metadata": {...},
 *       "created_at": "2025-01-15T10:30:00Z"
 *     }
 *   ],
 *   "total_count": 1,
 *   "query": "machine learning concepts",
 *   "agent_id": "user-123",
 *   "processing_time_ms": 45,
 *   "timestamp": "2025-01-15T14:22:30Z"
 * }
 */
public class MemorySearchResponse {

    @JsonProperty("results")
    private List<SearchResult> results = new ArrayList<>();

    @JsonProperty("total_count")
    private int totalCount;

    @JsonProperty("query")
    private String query;

    @JsonProperty("agent_id")
    private String agentId;

    @JsonProperty("processing_time_ms")
    private Long processingTimeMs;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("success")
    private boolean success = true;

    @JsonProperty("message")
    private String message;

    @JsonProperty("error")
    private String error;

    @JsonProperty("search_metadata")
    private Map<String, Object> searchMetadata;

    /**
     * Default constructor.
     */
    public MemorySearchResponse() {
        this.timestamp = Instant.now();
    }

    /**
     * Constructor for successful search.
     */
    public MemorySearchResponse(
        List<SearchResult> results,
        String query,
        String agentId
    ) {
        this();
        this.results = results != null ? results : new ArrayList<>();
        this.totalCount = this.results.size();
        this.query = query;
        this.agentId = agentId;
        this.success = true;
    }

    /**
     * Constructor for error response.
     */
    public MemorySearchResponse(String error, String query, String agentId) {
        this();
        this.success = false;
        this.error = error;
        this.query = query;
        this.agentId = agentId;
        this.results = new ArrayList<>();
        this.totalCount = 0;
    }

    /**
     * Nested class representing a single search result.
     */
    public static class SearchResult {

        @JsonProperty("memory_id")
        private String memoryId;

        @JsonProperty("content")
        private String content;

        @JsonProperty("relevance_score")
        private double relevanceScore;

        @JsonProperty("category")
        private String category;

        @JsonProperty("metadata")
        private Map<String, Object> metadata;

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("source")
        private String source;

        @JsonProperty("tags")
        private List<String> tags;

        @JsonProperty("agent_id")
        private String agentId;

        /**
         * Default constructor.
         */
        public SearchResult() {}

        /**
         * Constructor from MemoryRecord.
         */
        public SearchResult(MemoryRecord memoryRecord, double relevanceScore) {
            this.memoryId = memoryRecord.getId();
            this.content = memoryRecord.getContent();
            this.relevanceScore = relevanceScore;
            this.category = memoryRecord.getCategory() != null
                ? memoryRecord.getCategory().getPrimary()
                : null;
            this.metadata = memoryRecord.getMetadata() != null
                ? memoryRecord.getMetadata().getProperties()
                : null;
            this.createdAt = memoryRecord.getCreatedAt() != null
                ? memoryRecord.getCreatedAt().toString()
                : null;
            this.source = memoryRecord.getMetadata() != null
                ? memoryRecord.getMetadata().getSource()
                : null;
            this.tags = memoryRecord.getCategory() != null
                ? new java.util.ArrayList<>(
                    memoryRecord.getCategory().getTags()
                )
                : null;
            this.agentId = memoryRecord.getAgentId();
        }

        // Getters and Setters for SearchResult
        public String getMemoryId() {
            return memoryId;
        }

        public void setMemoryId(String memoryId) {
            this.memoryId = memoryId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public double getRelevanceScore() {
            return relevanceScore;
        }

        public void setRelevanceScore(double relevanceScore) {
            this.relevanceScore = relevanceScore;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        @Override
        public String toString() {
            return (
                "SearchResult{" +
                "memoryId='" +
                memoryId +
                '\'' +
                ", content='" +
                content +
                '\'' +
                ", relevanceScore=" +
                relevanceScore +
                ", category='" +
                category +
                '\'' +
                ", createdAt='" +
                createdAt +
                '\'' +
                '}'
            );
        }
    }

    // Main class getters and setters
    public List<SearchResult> getResults() {
        return results;
    }

    public void setResults(List<SearchResult> results) {
        this.results = results != null ? results : new ArrayList<>();
        this.totalCount = this.results.size();
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
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

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
        if (error != null) {
            this.success = false;
        }
    }

    public Map<String, Object> getSearchMetadata() {
        return searchMetadata;
    }

    public void setSearchMetadata(Map<String, Object> searchMetadata) {
        this.searchMetadata = searchMetadata;
    }

    /**
     * Adds processing time based on start time.
     */
    public void setProcessingTime(Instant startTime) {
        if (startTime != null) {
            this.processingTimeMs =
                Instant.now().toEpochMilli() - startTime.toEpochMilli();
        }
    }

    /**
     * Convenience method to add a search result.
     */
    public void addResult(SearchResult result) {
        if (this.results == null) {
            this.results = new ArrayList<>();
        }
        this.results.add(result);
        this.totalCount = this.results.size();
    }

    /**
     * Convenience method to add a search result from MemoryRecord.
     */
    public void addResult(MemoryRecord memoryRecord, double relevanceScore) {
        addResult(new SearchResult(memoryRecord, relevanceScore));
    }

    @Override
    public String toString() {
        return (
            "MemorySearchResponse{" +
            "totalCount=" +
            totalCount +
            ", query='" +
            query +
            '\'' +
            ", agentId='" +
            agentId +
            '\'' +
            ", processingTimeMs=" +
            processingTimeMs +
            ", success=" +
            success +
            ", timestamp=" +
            timestamp +
            '}'
        );
    }
}
