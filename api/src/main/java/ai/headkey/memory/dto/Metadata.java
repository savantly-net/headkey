package ai.headkey.memory.dto;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * General-purpose metadata container for memory system operations.
 * Provides a flexible way to attach additional information to memories,
 * inputs, and other system entities.
 */
public class Metadata {
    
    /**
     * Free-form key-value pairs for custom metadata.
     */
    private Map<String, Object> properties;
    
    /**
     * Importance level of the associated data (0.0 to 1.0).
     */
    private Double importance;
    
    /**
     * Source or origin of the information.
     */
    private String source;
    
    /**
     * Set of tags associated with the data.
     */
    private Set<String> tags;
    
    /**
     * Timestamp when this metadata was last accessed.
     */
    private Instant lastAccessed;
    
    /**
     * Confidence score for the associated data (0.0 to 1.0).
     */
    private Double confidence;
    
    /**
     * Number of times the associated data has been accessed.
     */
    private Integer accessCount;
    
    /**
     * Default constructor required by JPA and Jackson.
     */
    public Metadata() {
        this.properties = new HashMap<>();
        this.accessCount = 0;
    }
    
    /**
     * Constructor with properties map.
     * 
     * @param properties Initial properties map
     */
    public Metadata(Map<String, Object> properties) {
        this.properties = new HashMap<>(properties != null ? properties : new HashMap<>());
        this.accessCount = 0;
    }
    
    /**
     * Gets a property value by key.
     * 
     * @param key The property key
     * @return The property value, or null if not found
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    /**
     * Sets a property value.
     * 
     * @param key The property key
     * @param value The property value
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    /**
     * Removes a property.
     * 
     * @param key The property key to remove
     * @return The removed value, or null if not found
     */
    public Object removeProperty(String key) {
        return properties.remove(key);
    }
    
    /**
     * Checks if a property exists.
     * 
     * @param key The property key
     * @return true if the property exists
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }
    
    /**
     * Gets all property keys.
     * 
     * @return Set of all property keys
     */
    public Set<String> getPropertyKeys() {
        return properties.keySet();
    }
    
    // Getters and Setters
    
    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }
    
    public void setProperties(Map<String, Object> properties) {
        this.properties = new HashMap<>(properties != null ? properties : new HashMap<>());
    }
    
    public Double getImportance() {
        return importance;
    }
    
    public void setImportance(Double importance) {
        this.importance = importance;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public Set<String> getTags() {
        return tags;
    }
    
    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
    
    public Instant getLastAccessed() {
        return lastAccessed;
    }
    
    public void setLastAccessed(Instant lastAccessed) {
        this.lastAccessed = lastAccessed;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    public Integer getAccessCount() {
        return accessCount;
    }
    
    public void setAccessCount(Integer accessCount) {
        this.accessCount = accessCount;
    }
    
    /**
     * Increments the access count.
     */
    public void incrementAccessCount() {
        if (accessCount == null) {
            accessCount = 0;
        }
        accessCount++;
        lastAccessed = Instant.now();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Metadata metadata = (Metadata) o;
        return Objects.equals(properties, metadata.properties) &&
                Objects.equals(importance, metadata.importance) &&
                Objects.equals(source, metadata.source) &&
                Objects.equals(tags, metadata.tags) &&
                Objects.equals(lastAccessed, metadata.lastAccessed) &&
                Objects.equals(confidence, metadata.confidence) &&
                Objects.equals(accessCount, metadata.accessCount);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(properties, importance, source, tags, lastAccessed, confidence, accessCount);
    }
    
    @Override
    public String toString() {
        return "Metadata{" +
                "properties=" + properties +
                ", importance=" + importance +
                ", source='" + source + '\'' +
                ", tags=" + tags +
                ", lastAccessed=" + lastAccessed +
                ", confidence=" + confidence +
                ", accessCount=" + accessCount +
                '}';
    }
}