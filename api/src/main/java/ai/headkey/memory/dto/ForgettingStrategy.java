package ai.headkey.memory.dto;

import ai.headkey.memory.enums.ForgettingStrategyType;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Data Transfer Object representing a forgetting strategy specification.
 * Used by the Relevance Evaluation & Forgetting Agent (REFA) to determine
 * which memories should be removed from the system based on various criteria.
 */
public class ForgettingStrategy {
    
    /**
     * The type of forgetting strategy to apply.
     */
    private ForgettingStrategyType type;
    
    /**
     * Maximum age for memories (used with AGE strategy).
     * Memories older than this duration will be candidates for removal.
     */
    private Duration maxAge;
    
    /**
     * Number of memories to retain (used with LEAST_USED strategy).
     * Keeps the N most frequently accessed memories.
     */
    private Integer retainCount;
    
    /**
     * Relevance score threshold (used with LOW_SCORE strategy).
     * Memories with scores below this threshold will be removed.
     */
    private Double scoreThreshold;
    
    /**
     * Agent ID to limit the forgetting scope (optional).
     * If specified, only memories belonging to this agent will be considered.
     */
    private String agentId;
    
    /**
     * Category filter (optional).
     * If specified, only memories in this category will be considered for forgetting.
     */
    private String category;
    
    /**
     * Custom parameters for extensible strategies.
     * Allows for additional configuration options specific to custom implementations.
     */
    private Map<String, Object> customParameters;
    
    /**
     * Flag indicating whether to perform a dry run.
     * If true, the strategy will identify candidates but not actually remove them.
     */
    private boolean dryRun;
    
    /**
     * Maximum number of memories to remove in a single operation.
     * Helps prevent excessive deletion and allows for gradual forgetting.
     */
    private Integer maxRemovalCount;
    
    /**
     * Minimum confidence threshold for removal decisions.
     * Only remove memories if the forgetting algorithm is confident in the decision.
     */
    private Double minConfidenceThreshold;
    
    /**
     * Default constructor.
     */
    public ForgettingStrategy() {
        this.customParameters = new HashMap<>();
        this.dryRun = false;
        this.minConfidenceThreshold = 0.7;
    }
    
    /**
     * Constructor with strategy type.
     * 
     * @param type The forgetting strategy type
     */
    public ForgettingStrategy(ForgettingStrategyType type) {
        this();
        this.type = type;
    }
    
    /**
     * Creates an age-based forgetting strategy.
     * 
     * @param maxAge Maximum age for memories to retain
     * @return ForgettingStrategy configured for age-based forgetting
     */
    public static ForgettingStrategy byAge(Duration maxAge) {
        ForgettingStrategy strategy = new ForgettingStrategy(ForgettingStrategyType.AGE);
        strategy.setMaxAge(maxAge);
        return strategy;
    }
    
    /**
     * Creates a usage-based forgetting strategy.
     * 
     * @param retainCount Number of most-used memories to retain
     * @return ForgettingStrategy configured for usage-based forgetting
     */
    public static ForgettingStrategy byUsage(int retainCount) {
        ForgettingStrategy strategy = new ForgettingStrategy(ForgettingStrategyType.LEAST_USED);
        strategy.setRetainCount(retainCount);
        return strategy;
    }
    
    /**
     * Creates a score-based forgetting strategy.
     * 
     * @param scoreThreshold Minimum relevance score threshold
     * @return ForgettingStrategy configured for score-based forgetting
     */
    public static ForgettingStrategy byScore(double scoreThreshold) {
        ForgettingStrategy strategy = new ForgettingStrategy(ForgettingStrategyType.LOW_SCORE);
        strategy.setScoreThreshold(scoreThreshold);
        return strategy;
    }
    
    /**
     * Creates a custom forgetting strategy.
     * 
     * @param customParameters Custom configuration parameters
     * @return ForgettingStrategy configured for custom forgetting
     */
    public static ForgettingStrategy custom(Map<String, Object> customParameters) {
        ForgettingStrategy strategy = new ForgettingStrategy(ForgettingStrategyType.CUSTOM);
        strategy.setCustomParameters(customParameters);
        return strategy;
    }
    
    /**
     * Sets the strategy to dry run mode.
     * 
     * @return This strategy instance for method chaining
     */
    public ForgettingStrategy asDryRun() {
        this.dryRun = true;
        return this;
    }
    
    /**
     * Limits the strategy to a specific agent.
     * 
     * @param agentId The agent ID to limit forgetting to
     * @return This strategy instance for method chaining
     */
    public ForgettingStrategy forAgent(String agentId) {
        this.agentId = agentId;
        return this;
    }
    
    /**
     * Limits the strategy to a specific category.
     * 
     * @param category The category to limit forgetting to
     * @return This strategy instance for method chaining
     */
    public ForgettingStrategy inCategory(String category) {
        this.category = category;
        return this;
    }
    
    /**
     * Sets the maximum number of memories to remove.
     * 
     * @param maxCount Maximum removal count
     * @return This strategy instance for method chaining
     */
    public ForgettingStrategy withMaxRemovalCount(int maxCount) {
        this.maxRemovalCount = maxCount;
        return this;
    }
    
    /**
     * Sets the minimum confidence threshold for removal decisions.
     * 
     * @param threshold Minimum confidence threshold (0.0 to 1.0)
     * @return This strategy instance for method chaining
     */
    public ForgettingStrategy withMinConfidence(double threshold) {
        this.minConfidenceThreshold = Math.max(0.0, Math.min(1.0, threshold));
        return this;
    }
    
    /**
     * Adds a custom parameter to the strategy.
     * 
     * @param key Parameter key
     * @param value Parameter value
     * @return This strategy instance for method chaining
     */
    public ForgettingStrategy withParameter(String key, Object value) {
        if (customParameters == null) {
            customParameters = new HashMap<>();
        }
        customParameters.put(key, value);
        return this;
    }
    
    /**
     * Validates that the strategy configuration is complete and valid.
     * 
     * @return true if the strategy is valid for execution
     */
    public boolean isValid() {
        if (type == null) {
            return false;
        }
        
        switch (type) {
            case AGE:
                return maxAge != null && !maxAge.isNegative();
            case LEAST_USED:
                return retainCount != null && retainCount >= 0;
            case LOW_SCORE:
                return scoreThreshold != null && scoreThreshold >= 0.0 && scoreThreshold <= 1.0;
            case CUSTOM:
                return customParameters != null && !customParameters.isEmpty();
            default:
                return false;
        }
    }
    
    /**
     * Gets a custom parameter value.
     * 
     * @param key The parameter key
     * @return The parameter value, or null if not found
     */
    public Object getCustomParameter(String key) {
        return customParameters != null ? customParameters.get(key) : null;
    }
    
    /**
     * Gets a custom parameter value with type casting.
     * 
     * @param key The parameter key
     * @param type The expected type
     * @param <T> The type parameter
     * @return The parameter value cast to the specified type, or null if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T> T getCustomParameter(String key, Class<T> type) {
        Object value = getCustomParameter(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Creates a description of this strategy for logging or display purposes.
     * 
     * @return A human-readable description of the strategy
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("ForgettingStrategy[").append(type);
        
        switch (type) {
            case AGE:
                desc.append(", maxAge=").append(maxAge);
                break;
            case LEAST_USED:
                desc.append(", retainCount=").append(retainCount);
                break;
            case LOW_SCORE:
                desc.append(", scoreThreshold=").append(scoreThreshold);
                break;
            case CUSTOM:
                desc.append(", customParams=").append(customParameters.size()).append(" items");
                break;
        }
        
        if (agentId != null) {
            desc.append(", agentId=").append(agentId);
        }
        if (category != null) {
            desc.append(", category=").append(category);
        }
        if (maxRemovalCount != null) {
            desc.append(", maxRemoval=").append(maxRemovalCount);
        }
        if (dryRun) {
            desc.append(", dryRun=true");
        }
        
        desc.append("]");
        return desc.toString();
    }
    
    // Getters and Setters
    
    public ForgettingStrategyType getType() {
        return type;
    }
    
    public void setType(ForgettingStrategyType type) {
        this.type = type;
    }
    
    public Duration getMaxAge() {
        return maxAge;
    }
    
    public void setMaxAge(Duration maxAge) {
        this.maxAge = maxAge;
    }
    
    public Integer getRetainCount() {
        return retainCount;
    }
    
    public void setRetainCount(Integer retainCount) {
        this.retainCount = retainCount;
    }
    
    public Double getScoreThreshold() {
        return scoreThreshold;
    }
    
    public void setScoreThreshold(Double scoreThreshold) {
        this.scoreThreshold = scoreThreshold;
    }
    
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
    
    public Map<String, Object> getCustomParameters() {
        return customParameters != null ? new HashMap<>(customParameters) : new HashMap<>();
    }
    
    public void setCustomParameters(Map<String, Object> customParameters) {
        this.customParameters = new HashMap<>(customParameters != null ? customParameters : new HashMap<>());
    }
    
    public boolean isDryRun() {
        return dryRun;
    }
    
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
    
    public Integer getMaxRemovalCount() {
        return maxRemovalCount;
    }
    
    public void setMaxRemovalCount(Integer maxRemovalCount) {
        this.maxRemovalCount = maxRemovalCount;
    }
    
    public Double getMinConfidenceThreshold() {
        return minConfidenceThreshold;
    }
    
    public void setMinConfidenceThreshold(Double minConfidenceThreshold) {
        this.minConfidenceThreshold = minConfidenceThreshold != null ? 
            Math.max(0.0, Math.min(1.0, minConfidenceThreshold)) : null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForgettingStrategy that = (ForgettingStrategy) o;
        return dryRun == that.dryRun &&
                type == that.type &&
                Objects.equals(maxAge, that.maxAge) &&
                Objects.equals(retainCount, that.retainCount) &&
                Objects.equals(scoreThreshold, that.scoreThreshold) &&
                Objects.equals(agentId, that.agentId) &&
                Objects.equals(category, that.category) &&
                Objects.equals(customParameters, that.customParameters) &&
                Objects.equals(maxRemovalCount, that.maxRemovalCount) &&
                Objects.equals(minConfidenceThreshold, that.minConfidenceThreshold);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, maxAge, retainCount, scoreThreshold, agentId, category, 
                          customParameters, dryRun, maxRemovalCount, minConfidenceThreshold);
    }
    
    @Override
    public String toString() {
        return "ForgettingStrategy{" +
                "type=" + type +
                ", maxAge=" + maxAge +
                ", retainCount=" + retainCount +
                ", scoreThreshold=" + scoreThreshold +
                ", agentId='" + agentId + '\'' +
                ", category='" + category + '\'' +
                ", customParameters=" + customParameters +
                ", dryRun=" + dryRun +
                ", maxRemovalCount=" + maxRemovalCount +
                ", minConfidenceThreshold=" + minConfidenceThreshold +
                '}';
    }
}