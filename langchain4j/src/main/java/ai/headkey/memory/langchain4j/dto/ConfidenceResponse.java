package ai.headkey.memory.langchain4j.dto;

import java.util.Objects;

/**
 * Data Transfer Object for confidence calculation response results.
 * 
 * This class represents the result of a confidence calculation operation from AI services,
 * containing the calculated confidence score and the reasoning behind the assessment.
 * 
 * Used by BeliefExtractionAiService implementations to return structured
 * confidence assessment results from Large Language Models.
 * 
 * @since 1.0
 */
public class ConfidenceResponse {
    
    /**
     * The calculated confidence score (0.0 to 1.0).
     * Higher values indicate greater confidence in the belief extraction or statement.
     */
    private double confidence;
    
    /**
     * AI-generated reasoning explaining how the confidence score was determined.
     * Provides transparency into the confidence calculation process.
     */
    private String reasoning;
    
    /**
     * Default constructor.
     */
    public ConfidenceResponse() {
        this.confidence = 0.5; // Default neutral confidence
    }
    
    /**
     * Constructor with confidence and reasoning.
     * 
     * @param confidence The confidence score (0.0 to 1.0)
     * @param reasoning The reasoning for the confidence assessment
     */
    public ConfidenceResponse(double confidence, String reasoning) {
        this.confidence = Math.max(0.0, Math.min(1.0, confidence)); // Clamp to valid range
        this.reasoning = reasoning;
    }
    
    /**
     * Constructor with confidence only.
     * 
     * @param confidence The confidence score (0.0 to 1.0)
     */
    public ConfidenceResponse(double confidence) {
        this(confidence, null);
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
     * Gets the reasoning for the confidence assessment.
     * 
     * @return The reasoning text
     */
    public String getReasoning() {
        return reasoning;
    }
    
    /**
     * Sets the reasoning for the confidence assessment.
     * 
     * @param reasoning The reasoning to set
     */
    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
    
    /**
     * Checks if this is a high-confidence assessment.
     * 
     * @param threshold The confidence threshold
     * @return true if confidence is above threshold
     */
    public boolean isHighConfidence(double threshold) {
        return confidence >= threshold;
    }
    
    /**
     * Checks if this is a high-confidence assessment using default threshold of 0.8.
     * 
     * @return true if confidence is above 0.8
     */
    public boolean isHighConfidence() {
        return isHighConfidence(0.8);
    }
    
    /**
     * Checks if this is a low-confidence assessment.
     * 
     * @param threshold The confidence threshold
     * @return true if confidence is below threshold
     */
    public boolean isLowConfidence(double threshold) {
        return confidence < threshold;
    }
    
    /**
     * Checks if this is a low-confidence assessment using default threshold of 0.3.
     * 
     * @return true if confidence is below 0.3
     */
    public boolean isLowConfidence() {
        return isLowConfidence(0.3);
    }
    
    /**
     * Checks if reasoning is provided.
     * 
     * @return true if reasoning is not null and not empty
     */
    public boolean hasReasoning() {
        return reasoning != null && !reasoning.trim().isEmpty();
    }
    
    /**
     * Gets a descriptive confidence level.
     * 
     * @return A string describing the confidence level
     */
    public String getConfidenceLevel() {
        if (confidence >= 0.9) return "Very High";
        if (confidence >= 0.7) return "High";
        if (confidence >= 0.5) return "Medium";
        if (confidence >= 0.3) return "Low";
        return "Very Low";
    }
    
    /**
     * Creates a default confidence response with neutral confidence.
     * 
     * @return ConfidenceResponse with 0.5 confidence
     */
    public static ConfidenceResponse neutral() {
        return new ConfidenceResponse(0.5, "Default neutral confidence");
    }
    
    /**
     * Creates a high confidence response.
     * 
     * @param reasoning The reasoning for high confidence
     * @return ConfidenceResponse with high confidence
     */
    public static ConfidenceResponse high(String reasoning) {
        return new ConfidenceResponse(0.9, reasoning);
    }
    
    /**
     * Creates a low confidence response.
     * 
     * @param reasoning The reasoning for low confidence
     * @return ConfidenceResponse with low confidence
     */
    public static ConfidenceResponse low(String reasoning) {
        return new ConfidenceResponse(0.2, reasoning);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfidenceResponse that = (ConfidenceResponse) o;
        return Double.compare(that.confidence, confidence) == 0 &&
                Objects.equals(reasoning, that.reasoning);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(confidence, reasoning);
    }
    
    @Override
    public String toString() {
        return "ConfidenceResponse{" +
                "confidence=" + confidence +
                ", confidenceLevel='" + getConfidenceLevel() + '\'' +
                ", reasoning='" + reasoning + '\'' +
                '}';
    }
}