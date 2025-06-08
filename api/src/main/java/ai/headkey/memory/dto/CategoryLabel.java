package ai.headkey.memory.dto;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a category label assigned by the Contextual Categorization Engine (CCE).
 * Contains the primary category, optional secondary category, semantic tags,
 * and confidence score for the categorization decision.
 */
public class CategoryLabel {
    
    /**
     * Primary category assigned to the content.
     * This is the main classification (e.g., "UserProfile", "WorldFact", "PersonalData").
     */
    private String primary;
    
    /**
     * Optional secondary category for more granular classification.
     * Provides subcategorization within the primary category (e.g., "Biography", "Preferences").
     */
    private String secondary;
    
    /**
     * Set of semantic tags or key annotations extracted from the content.
     * These are fine-grained descriptors that provide additional context
     * (e.g., "person:John Doe", "birthYear:1990", "location:Paris").
     */
    private Set<String> tags;
    
    /**
     * Confidence score for the categorization decision (0.0 to 1.0).
     * Higher values indicate greater confidence in the assigned category.
     */
    private double confidence;
    
    /**
     * Default constructor required by JPA and Jackson.
     */
    public CategoryLabel() {
        this.tags = new HashSet<>();
        this.confidence = 0.0;
    }
    
    /**
     * Constructor with primary category only.
     * 
     * @param primary The primary category
     */
    public CategoryLabel(String primary) {
        this();
        this.primary = primary;
    }
    
    /**
     * Constructor with primary and secondary categories.
     * 
     * @param primary The primary category
     * @param secondary The secondary category
     */
    public CategoryLabel(String primary, String secondary) {
        this(primary);
        this.secondary = secondary;
    }
    
    /**
     * Full constructor with all fields.
     * 
     * @param primary The primary category
     * @param secondary The secondary category
     * @param tags Set of semantic tags
     * @param confidence Confidence score (0.0 to 1.0)
     */
    public CategoryLabel(String primary, String secondary, Set<String> tags, Double confidence) {
        this.primary = primary;
        this.secondary = secondary;
        this.tags = new HashSet<>(tags != null ? tags : new HashSet<>());
        this.confidence = confidence != null ? Math.max(0.0, Math.min(1.0, confidence)) : 0.0; // Clamp to [0.0, 1.0]
    }
    
    /**
     * Adds a tag to the category label.
     * 
     * @param tag The tag to add
     */
    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            if (tags == null) {
                tags = new HashSet<>();
            }
            tags.add(tag.trim());
        }
    }
    
    /**
     * Removes a tag from the category label.
     * 
     * @param tag The tag to remove
     * @return true if the tag was removed, false if it wasn't present
     */
    public boolean removeTag(String tag) {
        return tags != null && tags.remove(tag);
    }
    
    /**
     * Checks if a specific tag is present.
     * 
     * @param tag The tag to check for
     * @return true if the tag is present
     */
    public boolean hasTag(String tag) {
        return tags != null && tags.contains(tag);
    }
    
    /**
     * Gets the full category path as a string.
     * 
     * @return A string representation of the category hierarchy
     * @deprecated This method should not be serialized as a JSON property
     */
    public String getFullCategory() {
        if (primary == null) {
            return "";
        }
        if (secondary == null || secondary.trim().isEmpty()) {
            return primary;
        }
        return primary + "/" + secondary;
    }
    
    /**
     * Checks if this is a high-confidence categorization.
     * 
     * @param threshold The confidence threshold (default 0.8 if not specified)
     * @return true if confidence is above the threshold
     * @deprecated This method should not be serialized as a JSON property
     */
    public boolean isHighConfidence(double threshold) {
        return confidence >= threshold;
    }
    
    /**
     * Checks if this is a high-confidence categorization using default threshold of 0.8.
     * 
     * @return true if confidence is above 0.8
     * @deprecated This method should not be serialized as a JSON property
     */
    public boolean isHighConfidence() {
        return isHighConfidence(0.8);
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
    
    public Set<String> getTags() {
        return tags != null ? new HashSet<>(tags) : new HashSet<>();
    }
    
    public void setTags(Set<String> tags) {
        this.tags = new HashSet<>(tags != null ? tags : new HashSet<>());
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = Math.max(0.0, Math.min(1.0, confidence)); // Clamp to [0.0, 1.0]
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryLabel that = (CategoryLabel) o;
        return Double.compare(that.confidence, confidence) == 0 &&
                Objects.equals(primary, that.primary) &&
                Objects.equals(secondary, that.secondary) &&
                Objects.equals(tags, that.tags);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(primary, secondary, tags, confidence);
    }
    
    @Override
    public String toString() {
        return "CategoryLabel{" +
                "primary='" + primary + '\'' +
                ", secondary='" + secondary + '\'' +
                ", tags=" + tags +
                ", confidence=" + confidence +
                '}';
    }
}