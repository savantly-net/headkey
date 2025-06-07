package ai.headkey.memory.dto;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Data Transfer Object representing filter options for memory search operations.
 * Used by the Retrieval & Response Engine (RRE) to narrow down search results
 * based on various criteria such as agent, category, date range, and custom filters.
 */
public class FilterOptions {
    
    /**
     * Agent ID to filter by (optional).
     * If specified, only memories belonging to this agent will be returned.
     */
    private String agentId;
    
    /**
     * Category to filter by (optional).
     * If specified, only memories in this category will be returned.
     */
    private String category;
    
    /**
     * Start date for temporal filtering (optional).
     * Only memories created on or after this date will be returned.
     */
    private Instant since;
    
    /**
     * End date for temporal filtering (optional).
     * Only memories created on or before this date will be returned.
     */
    private Instant until;
    
    /**
     * Source filter (optional).
     * Only memories from the specified source will be returned.
     */
    private String source;
    
    /**
     * Minimum relevance score threshold (optional).
     * Only memories with relevance scores above this threshold will be returned.
     */
    private Double minRelevanceScore;
    
    /**
     * Maximum relevance score threshold (optional).
     * Only memories with relevance scores below this threshold will be returned.
     */
    private Double maxRelevanceScore;
    
    /**
     * Set of tags to filter by (optional).
     * Only memories containing at least one of these tags will be returned.
     */
    private Set<String> tags;
    
    /**
     * Flag to include only active memories (optional).
     * If true, only active/non-deprecated memories will be returned.
     */
    private Boolean activeOnly;
    
    /**
     * Minimum confidence threshold for categorization (optional).
     * Only memories with categorization confidence above this threshold will be returned.
     */
    private Double minCategoryConfidence;
    
    /**
     * Custom filter parameters for extensible filtering.
     * Allows for additional filter criteria not covered by standard fields.
     */
    private Map<String, Object> customFilters;
    
    /**
     * Flag to exclude memories with conflicts (optional).
     * If true, memories involved in unresolved conflicts will be excluded.
     */
    private Boolean excludeConflicted;
    
    /**
     * Minimum access count threshold (optional).
     * Only memories accessed at least this many times will be returned.
     */
    private Integer minAccessCount;
    
    /**
     * Maximum age in seconds (optional).
     * Only memories newer than this age will be returned.
     */
    private Long maxAgeSeconds;
    
    /**
     * Default constructor.
     */
    public FilterOptions() {
        this.customFilters = new HashMap<>();
        this.activeOnly = true; // Default to active memories only
    }
    
    /**
     * Constructor with agent ID.
     * 
     * @param agentId The agent ID to filter by
     */
    public FilterOptions(String agentId) {
        this();
        this.agentId = agentId;
    }
    
    /**
     * Constructor with agent ID and category.
     * 
     * @param agentId The agent ID to filter by
     * @param category The category to filter by
     */
    public FilterOptions(String agentId, String category) {
        this(agentId);
        this.category = category;
    }
    
    /**
     * Creates a filter for a specific agent.
     * 
     * @param agentId The agent ID
     * @return FilterOptions configured for the agent
     */
    public static FilterOptions forAgent(String agentId) {
        return new FilterOptions(agentId);
    }
    
    /**
     * Creates a filter for a specific category.
     * 
     * @param category The category
     * @return FilterOptions configured for the category
     */
    public static FilterOptions forCategory(String category) {
        FilterOptions options = new FilterOptions();
        options.setCategory(category);
        return options;
    }
    
    /**
     * Creates a filter for recent memories.
     * 
     * @param since The start date
     * @return FilterOptions configured for recent memories
     */
    public static FilterOptions since(Instant since) {
        FilterOptions options = new FilterOptions();
        options.setSince(since);
        return options;
    }
    
    /**
     * Creates a filter for memories within a date range.
     * 
     * @param since Start date
     * @param until End date
     * @return FilterOptions configured for the date range
     */
    public static FilterOptions between(Instant since, Instant until) {
        FilterOptions options = new FilterOptions();
        options.setSince(since);
        options.setUntil(until);
        return options;
    }
    
    /**
     * Sets the agent filter and returns this instance for method chaining.
     * 
     * @param agentId The agent ID to filter by
     * @return This FilterOptions instance
     */
    public FilterOptions withAgent(String agentId) {
        this.agentId = agentId;
        return this;
    }
    
    /**
     * Sets the category filter and returns this instance for method chaining.
     * 
     * @param category The category to filter by
     * @return This FilterOptions instance
     */
    public FilterOptions withCategory(String category) {
        this.category = category;
        return this;
    }
    
    /**
     * Sets the source filter and returns this instance for method chaining.
     * 
     * @param source The source to filter by
     * @return This FilterOptions instance
     */
    public FilterOptions withSource(String source) {
        this.source = source;
        return this;
    }
    
    /**
     * Sets the relevance score range and returns this instance for method chaining.
     * 
     * @param minScore Minimum relevance score
     * @param maxScore Maximum relevance score
     * @return This FilterOptions instance
     */
    public FilterOptions withRelevanceRange(double minScore, double maxScore) {
        this.minRelevanceScore = minScore;
        this.maxRelevanceScore = maxScore;
        return this;
    }
    
    /**
     * Sets the minimum relevance score and returns this instance for method chaining.
     * 
     * @param minScore Minimum relevance score
     * @return This FilterOptions instance
     */
    public FilterOptions withMinRelevance(double minScore) {
        this.minRelevanceScore = minScore;
        return this;
    }
    
    /**
     * Sets the tags filter and returns this instance for method chaining.
     * 
     * @param tags Tags to filter by
     * @return This FilterOptions instance
     */
    public FilterOptions withTags(Set<String> tags) {
        this.tags = tags;
        return this;
    }
    
    /**
     * Sets the active only filter and returns this instance for method chaining.
     * 
     * @param activeOnly Whether to include only active memories
     * @return This FilterOptions instance
     */
    public FilterOptions withActiveOnly(boolean activeOnly) {
        this.activeOnly = activeOnly;
        return this;
    }
    
    /**
     * Sets the exclude conflicted filter and returns this instance for method chaining.
     * 
     * @param excludeConflicted Whether to exclude conflicted memories
     * @return This FilterOptions instance
     */
    public FilterOptions withExcludeConflicted(boolean excludeConflicted) {
        this.excludeConflicted = excludeConflicted;
        return this;
    }
    
    /**
     * Sets the maximum age filter and returns this instance for method chaining.
     * 
     * @param maxAgeSeconds Maximum age in seconds
     * @return This FilterOptions instance
     */
    public FilterOptions withMaxAge(long maxAgeSeconds) {
        this.maxAgeSeconds = maxAgeSeconds;
        return this;
    }
    
    /**
     * Adds a custom filter parameter and returns this instance for method chaining.
     * 
     * @param key The filter parameter key
     * @param value The filter parameter value
     * @return This FilterOptions instance
     */
    public FilterOptions withCustomFilter(String key, Object value) {
        if (customFilters == null) {
            customFilters = new HashMap<>();
        }
        customFilters.put(key, value);
        return this;
    }
    
    /**
     * Checks if any filters are applied.
     * 
     * @return true if at least one filter is set
     */
    public boolean hasFilters() {
        return agentId != null || category != null || since != null || until != null ||
               source != null || minRelevanceScore != null || maxRelevanceScore != null ||
               (tags != null && !tags.isEmpty()) || activeOnly != null || 
               minCategoryConfidence != null || (customFilters != null && !customFilters.isEmpty()) ||
               excludeConflicted != null || minAccessCount != null || maxAgeSeconds != null;
    }
    
    /**
     * Checks if temporal filtering is applied.
     * 
     * @return true if since or until dates are set
     */
    public boolean hasTemporalFilter() {
        return since != null || until != null;
    }
    
    /**
     * Checks if relevance score filtering is applied.
     * 
     * @return true if min or max relevance scores are set
     */
    public boolean hasRelevanceFilter() {
        return minRelevanceScore != null || maxRelevanceScore != null;
    }
    
    /**
     * Gets a custom filter value.
     * 
     * @param key The filter parameter key
     * @return The filter value, or null if not found
     */
    public Object getCustomFilter(String key) {
        return customFilters != null ? customFilters.get(key) : null;
    }
    
    /**
     * Gets a custom filter value with type casting.
     * 
     * @param key The filter parameter key
     * @param type The expected type
     * @param <T> The type parameter
     * @return The filter value cast to the specified type, or null if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T> T getCustomFilter(String key, Class<T> type) {
        Object value = getCustomFilter(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Creates a description of the active filters for logging or display.
     * 
     * @return A human-readable description of the filters
     */
    public String getFilterDescription() {
        if (!hasFilters()) {
            return "No filters applied";
        }
        
        StringBuilder desc = new StringBuilder("Filters: ");
        boolean first = true;
        
        if (agentId != null) {
            desc.append("agent=").append(agentId);
            first = false;
        }
        
        if (category != null) {
            if (!first) desc.append(", ");
            desc.append("category=").append(category);
            first = false;
        }
        
        if (source != null) {
            if (!first) desc.append(", ");
            desc.append("source=").append(source);
            first = false;
        }
        
        if (hasTemporalFilter()) {
            if (!first) desc.append(", ");
            desc.append("temporal=");
            if (since != null && until != null) {
                desc.append(since).append(" to ").append(until);
            } else if (since != null) {
                desc.append("since ").append(since);
            } else {
                desc.append("until ").append(until);
            }
            first = false;
        }
        
        if (hasRelevanceFilter()) {
            if (!first) desc.append(", ");
            desc.append("relevance=");
            if (minRelevanceScore != null && maxRelevanceScore != null) {
                desc.append(minRelevanceScore).append("-").append(maxRelevanceScore);
            } else if (minRelevanceScore != null) {
                desc.append("≥").append(minRelevanceScore);
            } else {
                desc.append("≤").append(maxRelevanceScore);
            }
            first = false;
        }
        
        if (tags != null && !tags.isEmpty()) {
            if (!first) desc.append(", ");
            desc.append("tags=").append(tags);
            first = false;
        }
        
        if (activeOnly != null && !activeOnly) {
            if (!first) desc.append(", ");
            desc.append("includeInactive=true");
            first = false;
        }
        
        if (customFilters != null && !customFilters.isEmpty()) {
            if (!first) desc.append(", ");
            desc.append("custom=").append(customFilters.size()).append(" filters");
        }
        
        return desc.toString();
    }
    
    // Getters and Setters
    
    public String getAgentId() {
        return agentId;
    }
    
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public Instant getSince() {
        return since;
    }
    
    public void setSince(Instant since) {
        this.since = since;
    }
    
    public Instant getUntil() {
        return until;
    }
    
    public void setUntil(Instant until) {
        this.until = until;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public Double getMinRelevanceScore() {
        return minRelevanceScore;
    }
    
    public void setMinRelevanceScore(Double minRelevanceScore) {
        this.minRelevanceScore = minRelevanceScore;
    }
    
    public Double getMaxRelevanceScore() {
        return maxRelevanceScore;
    }
    
    public void setMaxRelevanceScore(Double maxRelevanceScore) {
        this.maxRelevanceScore = maxRelevanceScore;
    }
    
    public Set<String> getTags() {
        return tags;
    }
    
    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
    
    public Boolean getActiveOnly() {
        return activeOnly;
    }
    
    public void setActiveOnly(Boolean activeOnly) {
        this.activeOnly = activeOnly;
    }
    
    public Double getMinCategoryConfidence() {
        return minCategoryConfidence;
    }
    
    public void setMinCategoryConfidence(Double minCategoryConfidence) {
        this.minCategoryConfidence = minCategoryConfidence;
    }
    
    public Map<String, Object> getCustomFilters() {
        return customFilters != null ? new HashMap<>(customFilters) : new HashMap<>();
    }
    
    public void setCustomFilters(Map<String, Object> customFilters) {
        this.customFilters = new HashMap<>(customFilters != null ? customFilters : new HashMap<>());
    }
    
    public Boolean getExcludeConflicted() {
        return excludeConflicted;
    }
    
    public void setExcludeConflicted(Boolean excludeConflicted) {
        this.excludeConflicted = excludeConflicted;
    }
    
    public Integer getMinAccessCount() {
        return minAccessCount;
    }
    
    public void setMinAccessCount(Integer minAccessCount) {
        this.minAccessCount = minAccessCount;
    }
    
    public Long getMaxAgeSeconds() {
        return maxAgeSeconds;
    }
    
    public void setMaxAgeSeconds(Long maxAgeSeconds) {
        this.maxAgeSeconds = maxAgeSeconds;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilterOptions that = (FilterOptions) o;
        return Objects.equals(agentId, that.agentId) &&
                Objects.equals(category, that.category) &&
                Objects.equals(since, that.since) &&
                Objects.equals(until, that.until) &&
                Objects.equals(source, that.source) &&
                Objects.equals(minRelevanceScore, that.minRelevanceScore) &&
                Objects.equals(maxRelevanceScore, that.maxRelevanceScore) &&
                Objects.equals(tags, that.tags) &&
                Objects.equals(activeOnly, that.activeOnly) &&
                Objects.equals(minCategoryConfidence, that.minCategoryConfidence) &&
                Objects.equals(customFilters, that.customFilters) &&
                Objects.equals(excludeConflicted, that.excludeConflicted) &&
                Objects.equals(minAccessCount, that.minAccessCount) &&
                Objects.equals(maxAgeSeconds, that.maxAgeSeconds);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(agentId, category, since, until, source, minRelevanceScore, 
                          maxRelevanceScore, tags, activeOnly, minCategoryConfidence, 
                          customFilters, excludeConflicted, minAccessCount, maxAgeSeconds);
    }
    
    @Override
    public String toString() {
        return "FilterOptions{" +
                "agentId='" + agentId + '\'' +
                ", category='" + category + '\'' +
                ", since=" + since +
                ", until=" + until +
                ", source='" + source + '\'' +
                ", minRelevanceScore=" + minRelevanceScore +
                ", maxRelevanceScore=" + maxRelevanceScore +
                ", tags=" + tags +
                ", activeOnly=" + activeOnly +
                ", minCategoryConfidence=" + minCategoryConfidence +
                ", customFilters=" + customFilters +
                ", excludeConflicted=" + excludeConflicted +
                ", minAccessCount=" + minAccessCount +
                ", maxAgeSeconds=" + maxAgeSeconds +
                '}';
    }
}