package ai.headkey.memory.langchain4j.dto;

import java.util.Objects;

/**
 * Data Transfer Object for similarity calculation response results.
 * 
 * This class represents the result of a semantic similarity calculation operation
 * from AI services, containing the calculated similarity score between two statements
 * and the reasoning behind the assessment.
 * 
 * Used by SimilarityAiService implementations to return structured
 * similarity assessment results from Large Language Models.
 * 
 * @since 1.0
 */
public class SimilarityResponse {
    
    /**
     * The calculated similarity score (0.0 to 1.0).
     * 0.0 indicates completely different statements, 1.0 indicates identical meaning.
     */
    private double similarityScore;
    
    /**
     * AI-generated reasoning explaining how the similarity score was determined.
     * Provides transparency into the similarity calculation process.
     */
    private String reasoning;
    
    /**
     * Default constructor.
     */
    public SimilarityResponse() {
        this.similarityScore = 0.0;
    }
    
    /**
     * Constructor with similarity score and reasoning.
     * 
     * @param similarityScore The similarity score (0.0 to 1.0)
     * @param reasoning The reasoning for the similarity assessment
     */
    public SimilarityResponse(double similarityScore, String reasoning) {
        this.similarityScore = Math.max(0.0, Math.min(1.0, similarityScore)); // Clamp to valid range
        this.reasoning = reasoning;
    }
    
    /**
     * Constructor with similarity score only.
     * 
     * @param similarityScore The similarity score (0.0 to 1.0)
     */
    public SimilarityResponse(double similarityScore) {
        this(similarityScore, null);
    }
    
    /**
     * Gets the similarity score.
     * 
     * @return The similarity score (0.0 to 1.0)
     */
    public double getSimilarityScore() {
        return similarityScore;
    }
    
    /**
     * Sets the similarity score.
     * 
     * @param similarityScore The similarity score to set (will be clamped to 0.0-1.0)
     */
    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = Math.max(0.0, Math.min(1.0, similarityScore));
    }
    
    /**
     * Gets the reasoning for the similarity assessment.
     * 
     * @return The reasoning text
     */
    public String getReasoning() {
        return reasoning;
    }
    
    /**
     * Sets the reasoning for the similarity assessment.
     * 
     * @param reasoning The reasoning to set
     */
    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
    
    /**
     * Checks if this indicates high similarity.
     * 
     * @param threshold The similarity threshold
     * @return true if similarity is above threshold
     */
    public boolean isHighSimilarity(double threshold) {
        return similarityScore >= threshold;
    }
    
    /**
     * Checks if this indicates high similarity using default threshold of 0.8.
     * 
     * @return true if similarity is above 0.8
     */
    public boolean isHighSimilarity() {
        return isHighSimilarity(0.8);
    }
    
    /**
     * Checks if this indicates low similarity.
     * 
     * @param threshold The similarity threshold
     * @return true if similarity is below threshold
     */
    public boolean isLowSimilarity(double threshold) {
        return similarityScore < threshold;
    }
    
    /**
     * Checks if this indicates low similarity using default threshold of 0.3.
     * 
     * @return true if similarity is below 0.3
     */
    public boolean isLowSimilarity() {
        return isLowSimilarity(0.3);
    }
    
    /**
     * Checks if this indicates the statements are essentially identical.
     * 
     * @return true if similarity is above 0.95
     */
    public boolean isNearlyIdentical() {
        return similarityScore >= 0.95;
    }
    
    /**
     * Checks if this indicates the statements are completely different.
     * 
     * @return true if similarity is below 0.05
     */
    public boolean isCompletelyDifferent() {
        return similarityScore < 0.05;
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
     * Gets a descriptive similarity level.
     * 
     * @return A string describing the similarity level
     */
    public String getSimilarityLevel() {
        if (similarityScore >= 0.95) return "Nearly Identical";
        if (similarityScore >= 0.8) return "Very Similar";
        if (similarityScore >= 0.6) return "Similar";
        if (similarityScore >= 0.4) return "Somewhat Similar";
        if (similarityScore >= 0.2) return "Slightly Similar";
        return "Not Similar";
    }
    
    /**
     * Creates a response indicating no similarity.
     * 
     * @param reasoning The reasoning for no similarity
     * @return SimilarityResponse with 0.0 similarity
     */
    public static SimilarityResponse noSimilarity(String reasoning) {
        return new SimilarityResponse(0.0, reasoning);
    }
    
    /**
     * Creates a response indicating perfect similarity.
     * 
     * @param reasoning The reasoning for perfect similarity
     * @return SimilarityResponse with 1.0 similarity
     */
    public static SimilarityResponse perfectSimilarity(String reasoning) {
        return new SimilarityResponse(1.0, reasoning);
    }
    
    /**
     * Creates a response indicating moderate similarity.
     * 
     * @param reasoning The reasoning for moderate similarity
     * @return SimilarityResponse with 0.5 similarity
     */
    public static SimilarityResponse moderateSimilarity(String reasoning) {
        return new SimilarityResponse(0.5, reasoning);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimilarityResponse that = (SimilarityResponse) o;
        return Double.compare(that.similarityScore, similarityScore) == 0 &&
                Objects.equals(reasoning, that.reasoning);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(similarityScore, reasoning);
    }
    
    @Override
    public String toString() {
        return "SimilarityResponse{" +
                "similarityScore=" + similarityScore +
                ", similarityLevel='" + getSimilarityLevel() + '\'' +
                ", reasoning='" + reasoning + '\'' +
                '}';
    }
}