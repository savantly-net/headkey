package ai.headkey.memory.langchain4j.dto;

import java.util.Objects;

/**
 * Data Transfer Object for categorization response results.
 * 
 * This class represents the result of a content categorization operation,
 * containing the primary category, optional subcategory, confidence score,
 * and reasoning for the categorization decision.
 * 
 * Used by CategoryExtractionService implementations to return structured
 * categorization results with confidence metrics and explanatory reasoning.
 * 
 * @since 1.0
 */
public class CategoryResponse {
    
    /**
     * The primary category assigned to the content.
     * Should be one of the available categories in the system.
     */
    private String primary;
    
    /**
     * Optional secondary category or subcategory.
     * Provides more granular classification within the primary category.
     */
    private String secondary;
    
    /**
     * Confidence score for the categorization decision (0.0 to 1.0).
     * Higher values indicate greater confidence in the categorization.
     */
    private double confidence;
    
    /**
     * Brief explanation for why this categorization was chosen.
     * Provides transparency and context for the AI's decision-making process.
     */
    private String reasoning;
    
    /**
     * Default constructor.
     */
    public CategoryResponse() {
        this.confidence = 0.0;
    }
    
    /**
     * Constructor with all fields.
     * 
     * @param primary The primary category
     * @param secondary The secondary category (can be null)
     * @param confidence The confidence score (0.0 to 1.0)
     * @param reasoning The reasoning for the categorization
     */
    public CategoryResponse(String primary, String secondary, double confidence, String reasoning) {
        this.primary = primary;
        this.secondary = secondary;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence)); // Clamp to valid range
        this.reasoning = reasoning;
    }
    
    /**
     * Constructor with primary category and confidence.
     * 
     * @param primary The primary category
     * @param confidence The confidence score (0.0 to 1.0)
     */
    public CategoryResponse(String primary, double confidence) {
        this(primary, null, confidence, null);
    }
    
    /**
     * Checks if this response has a high confidence score.
     * 
     * @param threshold The confidence threshold to check against
     * @return true if confidence is above the threshold
     */
    public boolean isHighConfidence(double threshold) {
        return confidence >= threshold;
    }
    
    /**
     * Checks if this response has a high confidence score using default threshold (0.7).
     * 
     * @return true if confidence is above 0.7
     */
    public boolean isHighConfidence() {
        return isHighConfidence(0.7);
    }
    
    /**
     * Checks if this response has a secondary category.
     * 
     * @return true if secondary category is not null and not empty
     */
    public boolean hasSecondaryCategory() {
        return secondary != null && !secondary.trim().isEmpty();
    }
    
    /**
     * Checks if this response includes reasoning.
     * 
     * @return true if reasoning is not null and not empty
     */
    public boolean hasReasoning() {
        return reasoning != null && !reasoning.trim().isEmpty();
    }
    
    /**
     * Creates a fallback response for unknown categorization.
     * 
     * @param reasoning Optional reasoning for the unknown categorization
     * @return CategoryResponse with "Unknown" category and low confidence
     */
    public static CategoryResponse unknown(String reasoning) {
        return new CategoryResponse("Unknown", null, 0.1, reasoning);
    }
    
    /**
     * Creates a fallback response for unknown categorization with default reasoning.
     * 
     * @return CategoryResponse with "Unknown" category and low confidence
     */
    public static CategoryResponse unknown() {
        return unknown("Content could not be categorized with sufficient confidence");
    }
    
    // Getters and Setters
    
    public String getPrimary() {
        return primary;
    }
    
    public void setPrimary(String primary) {
        this.primary = primary;
    }
    
    public String getSecondary() {
        return secondary;
    }
    
    public void setSecondary(String secondary) {
        this.secondary = secondary;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = Math.max(0.0, Math.min(1.0, confidence)); // Clamp to valid range
    }
    
    public String getReasoning() {
        return reasoning;
    }
    
    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryResponse that = (CategoryResponse) o;
        return Double.compare(that.confidence, confidence) == 0 &&
                Objects.equals(primary, that.primary) &&
                Objects.equals(secondary, that.secondary) &&
                Objects.equals(reasoning, that.reasoning);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(primary, secondary, confidence, reasoning);
    }
    
    @Override
    public String toString() {
        return "CategoryResponse{" +
                "primary='" + primary + '\'' +
                ", secondary='" + secondary + '\'' +
                ", confidence=" + confidence +
                ", reasoning='" + reasoning + '\'' +
                '}';
    }
}