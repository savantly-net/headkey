package ai.headkey.memory.langchain4j.dto;

import java.util.Objects;

/**
 * Data Transfer Object for category extraction response results.
 * 
 * This class represents the result of a category extraction operation from AI services,
 * containing the extracted category classification and associated confidence score.
 * 
 * Used by BeliefExtractionAiService implementations to return structured
 * category classification results from Large Language Models.
 * 
 * @since 1.0
 */
public class CategoryExtractionResponse {
    
    /**
     * The extracted category classification.
     * Common categories: preference, fact, relationship, location, opinion, general.
     */
    private String category;
    
    /**
     * Confidence score for the category classification (0.0 to 1.0).
     * Higher values indicate greater confidence in the categorization accuracy.
     */
    private double confidence;
    
    /**
     * Default constructor.
     */
    public CategoryExtractionResponse() {
        this.confidence = 0.0;
    }
    
    /**
     * Constructor with category and confidence.
     * 
     * @param category The extracted category
     * @param confidence The confidence score (0.0 to 1.0)
     */
    public CategoryExtractionResponse(String category, double confidence) {
        this.category = category;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence)); // Clamp to valid range
    }
    
    /**
     * Constructor with category only (default confidence).
     * 
     * @param category The extracted category
     */
    public CategoryExtractionResponse(String category) {
        this(category, 0.5); // Default confidence
    }
    
    /**
     * Gets the extracted category.
     * 
     * @return The category classification
     */
    public String getCategory() {
        return category;
    }
    
    /**
     * Sets the extracted category.
     * 
     * @param category The category to set
     */
    public void setCategory(String category) {
        this.category = category;
    }
    
    /**
     * Gets the confidence score.
     * 
     * @return The confidence (0.0 to 1.0)
     */
    public double getConfidence() {
        return confidence;
    }
    
    /**
     * Sets the confidence score.
     * 
     * @param confidence The confidence to set (will be clamped to 0.0-1.0)
     */
    public void setConfidence(double confidence) {
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }
    
    /**
     * Checks if this is a high-confidence categorization.
     * 
     * @param threshold The confidence threshold
     * @return true if confidence is above threshold
     */
    public boolean isHighConfidence(double threshold) {
        return confidence >= threshold;
    }
    
    /**
     * Checks if this is a high-confidence categorization using default threshold of 0.8.
     * 
     * @return true if confidence is above 0.8
     */
    public boolean isHighConfidence() {
        return isHighConfidence(0.8);
    }
    
    /**
     * Checks if a category was successfully extracted.
     * 
     * @return true if category is not null and not empty
     */
    public boolean hasCategory() {
        return category != null && !category.trim().isEmpty();
    }
    
    /**
     * Checks if this is a general/unknown category.
     * 
     * @return true if category is "general" or "unknown"
     */
    public boolean isGeneralCategory() {
        return "general".equals(category) || "unknown".equals(category);
    }
    
    /**
     * Creates a fallback response for unknown categorization.
     * 
     * @return CategoryExtractionResponse with "general" category and low confidence
     */
    public static CategoryExtractionResponse unknown() {
        return new CategoryExtractionResponse("general", 0.1);
    }
    
    /**
     * Creates a fallback response for unknown categorization with custom confidence.
     * 
     * @param confidence The confidence score to use
     * @return CategoryExtractionResponse with "general" category and specified confidence
     */
    public static CategoryExtractionResponse unknown(double confidence) {
        return new CategoryExtractionResponse("general", confidence);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryExtractionResponse that = (CategoryExtractionResponse) o;
        return Double.compare(that.confidence, confidence) == 0 &&
                Objects.equals(category, that.category);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(category, confidence);
    }
    
    @Override
    public String toString() {
        return "CategoryExtractionResponse{" +
                "category='" + category + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}