package ai.headkey.memory.langchain4j.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Data Transfer Object representing an individual belief extracted from content.
 * 
 * This class encapsulates all the information about a single belief including
 * the statement text, category classification, confidence score, polarity,
 * reasoning for extraction, and associated tags.
 * 
 * Used within BeliefExtractionResponse to represent structured belief data
 * returned from AI-powered belief extraction services.
 * 
 * @since 1.0
 */
public class BeliefData {
    
    /**
     * The extracted belief statement text.
     * This is the core belief expressed in natural language.
     */
    private String statement;
    
    /**
     * The semantic category of this belief.
     * Common categories: preference, fact, relationship, location, opinion, general.
     */
    private String category;
    
    /**
     * Confidence score for this belief extraction (0.0 to 1.0).
     * Higher values indicate greater confidence in the extraction accuracy.
     */
    private double confidence;
    
    /**
     * Whether this belief represents a positive/affirmative statement (true)
     * or a negative/denial statement (false).
     */
    private boolean positive;
    
    /**
     * AI-generated reasoning explaining why this belief was extracted
     * and how the confidence was determined.
     */
    private String reasoning;
    
    /**
     * List of semantic tags associated with this belief.
     * Tags provide additional categorization and context.
     */
    private List<String> tags;
    
    /**
     * Default constructor.
     */
    public BeliefData() {
        this.confidence = 0.0;
        this.positive = true;
        this.tags = new ArrayList<>();
    }
    
    /**
     * Constructor with core fields.
     * 
     * @param statement The belief statement
     * @param category The belief category
     * @param confidence The confidence score (0.0 to 1.0)
     * @param positive Whether the belief is positive
     */
    public BeliefData(String statement, String category, double confidence, boolean positive) {
        this();
        this.statement = statement;
        this.category = category;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence)); // Clamp to valid range
        this.positive = positive;
    }
    
    /**
     * Full constructor with all fields.
     * 
     * @param statement The belief statement
     * @param category The belief category
     * @param confidence The confidence score (0.0 to 1.0)
     * @param positive Whether the belief is positive
     * @param reasoning The extraction reasoning
     * @param tags The associated tags
     */
    public BeliefData(String statement, String category, double confidence, boolean positive, 
                     String reasoning, List<String> tags) {
        this(statement, category, confidence, positive);
        this.reasoning = reasoning;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }
    
    /**
     * Gets the belief statement.
     * 
     * @return The statement text
     */
    public String getStatement() {
        return statement;
    }
    
    /**
     * Sets the belief statement.
     * 
     * @param statement The statement to set
     */
    public void setStatement(String statement) {
        this.statement = statement;
    }
    
    /**
     * Gets the belief category.
     * 
     * @return The category
     */
    public String getCategory() {
        return category;
    }
    
    /**
     * Sets the belief category.
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
     * Checks if this is a positive belief.
     * 
     * @return true if positive, false if negative
     */
    public boolean isPositive() {
        return positive;
    }
    
    /**
     * Sets the polarity of this belief.
     * 
     * @param positive true for positive, false for negative
     */
    public void setPositive(boolean positive) {
        this.positive = positive;
    }
    
    /**
     * Gets the extraction reasoning.
     * 
     * @return The reasoning text
     */
    public String getReasoning() {
        return reasoning;
    }
    
    /**
     * Sets the extraction reasoning.
     * 
     * @param reasoning The reasoning to set
     */
    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
    
    /**
     * Gets the list of tags.
     * 
     * @return A copy of the tags list
     */
    public List<String> getTags() {
        return tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }
    
    /**
     * Sets the list of tags.
     * 
     * @param tags The tags to set
     */
    public void setTags(List<String> tags) {
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }
    
    /**
     * Adds a tag to this belief.
     * 
     * @param tag The tag to add
     */
    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            if (this.tags == null) {
                this.tags = new ArrayList<>();
            }
            String trimmedTag = tag.trim();
            if (!this.tags.contains(trimmedTag)) {
                this.tags.add(trimmedTag);
            }
        }
    }
    
    /**
     * Removes a tag from this belief.
     * 
     * @param tag The tag to remove
     * @return true if the tag was removed
     */
    public boolean removeTag(String tag) {
        return tags != null && tags.remove(tag);
    }
    
    /**
     * Checks if this belief has a specific tag.
     * 
     * @param tag The tag to check for
     * @return true if the tag exists
     */
    public boolean hasTag(String tag) {
        return tags != null && tags.contains(tag);
    }
    
    /**
     * Checks if this is a high-confidence belief.
     * 
     * @param threshold The confidence threshold
     * @return true if confidence is above threshold
     */
    public boolean isHighConfidence(double threshold) {
        return confidence >= threshold;
    }
    
    /**
     * Checks if this is a high-confidence belief using default threshold of 0.8.
     * 
     * @return true if confidence is above 0.8
     */
    public boolean isHighConfidence() {
        return isHighConfidence(0.8);
    }
    
    /**
     * Checks if this belief has reasoning information.
     * 
     * @return true if reasoning is provided
     */
    public boolean hasReasoning() {
        return reasoning != null && !reasoning.trim().isEmpty();
    }
    
    /**
     * Checks if this belief has any tags.
     * 
     * @return true if tags are present
     */
    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }
    
    /**
     * Gets the number of tags associated with this belief.
     * 
     * @return The tag count
     */
    public int getTagCount() {
        return tags != null ? tags.size() : 0;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeliefData that = (BeliefData) o;
        return Double.compare(that.confidence, confidence) == 0 &&
                positive == that.positive &&
                Objects.equals(statement, that.statement) &&
                Objects.equals(category, that.category) &&
                Objects.equals(reasoning, that.reasoning) &&
                Objects.equals(tags, that.tags);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(statement, category, confidence, positive, reasoning, tags);
    }
    
    @Override
    public String toString() {
        return "BeliefData{" +
                "statement='" + statement + '\'' +
                ", category='" + category + '\'' +
                ", confidence=" + confidence +
                ", positive=" + positive +
                ", reasoning='" + reasoning + '\'' +
                ", tags=" + tags +
                '}';
    }
}