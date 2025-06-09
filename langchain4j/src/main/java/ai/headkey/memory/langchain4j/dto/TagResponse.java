package ai.headkey.memory.langchain4j.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Data Transfer Object for tag extraction response results.
 * 
 * This class represents the result of a semantic tag extraction operation,
 * containing extracted tags and categorized entities from content analysis.
 * 
 * Used by TagExtractionService implementations to return structured
 * tag and entity extraction results with semantic categorization.
 * 
 * @since 1.0
 */
public class TagResponse {
    
    /**
     * List of extracted tags and entities from the content.
     * These are general keywords, phrases, and entities identified in the text.
     */
    private List<String> tags;
    
    /**
     * Categorized entities organized by type.
     * Common categories include: person, place, organization, technology, etc.
     */
    private Map<String, List<String>> entities;
    
    /**
     * Default constructor.
     */
    public TagResponse() {
        this.tags = new ArrayList<>();
        this.entities = new HashMap<>();
    }
    
    /**
     * Constructor with tags and entities.
     * 
     * @param tags List of extracted tags
     * @param entities Map of categorized entities
     */
    public TagResponse(List<String> tags, Map<String, List<String>> entities) {
        this.tags = new ArrayList<>(tags != null ? tags : new ArrayList<>());
        this.entities = new HashMap<>(entities != null ? entities : new HashMap<>());
    }
    
    /**
     * Constructor with only tags.
     * 
     * @param tags List of extracted tags
     */
    public TagResponse(List<String> tags) {
        this(tags, new HashMap<>());
    }
    
    /**
     * Adds a tag to the response.
     * 
     * @param tag The tag to add
     */
    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            if (tags == null) {
                tags = new ArrayList<>();
            }
            if (!tags.contains(tag.trim())) {
                tags.add(tag.trim());
            }
        }
    }
    
    /**
     * Adds multiple tags to the response.
     * 
     * @param tags The tags to add
     */
    public void addTags(List<String> tags) {
        if (tags != null) {
            for (String tag : tags) {
                addTag(tag);
            }
        }
    }
    
    /**
     * Adds an entity to a specific category.
     * 
     * @param category The entity category (e.g., "person", "place")
     * @param entity The entity value
     */
    public void addEntity(String category, String entity) {
        if (category != null && !category.trim().isEmpty() && 
            entity != null && !entity.trim().isEmpty()) {
            
            if (entities == null) {
                entities = new HashMap<>();
            }
            
            entities.computeIfAbsent(category.trim().toLowerCase(), k -> new ArrayList<>())
                    .add(entity.trim());
        }
    }
    
    /**
     * Adds multiple entities to a specific category.
     * 
     * @param category The entity category
     * @param entities The entities to add
     */
    public void addEntities(String category, List<String> entities) {
        if (entities != null) {
            for (String entity : entities) {
                addEntity(category, entity);
            }
        }
    }
    
    /**
     * Gets entities for a specific category.
     * 
     * @param category The entity category
     * @return List of entities in that category, or empty list if none
     */
    public List<String> getEntitiesForCategory(String category) {
        if (entities == null || category == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(entities.getOrDefault(category.toLowerCase(), new ArrayList<>()));
    }
    
    /**
     * Gets all entity categories.
     * 
     * @return Set of entity category names
     */
    public Set<String> getEntityCategories() {
        if (entities == null) {
            return Set.of();
        }
        return entities.keySet();
    }
    
    /**
     * Checks if any tags were extracted.
     * 
     * @return true if tags list is not empty
     */
    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }
    
    /**
     * Checks if any entities were extracted.
     * 
     * @return true if entities map is not empty
     */
    public boolean hasEntities() {
        return entities != null && !entities.isEmpty();
    }
    
    /**
     * Checks if the response contains any extracted data.
     * 
     * @return true if either tags or entities are present
     */
    public boolean hasData() {
        return hasTags() || hasEntities();
    }
    
    /**
     * Gets the total count of all extracted tags.
     * 
     * @return Total number of tags
     */
    public int getTagCount() {
        return tags != null ? tags.size() : 0;
    }
    
    /**
     * Gets the total count of all extracted entities.
     * 
     * @return Total number of entities across all categories
     */
    public int getEntityCount() {
        if (entities == null) {
            return 0;
        }
        return entities.values().stream()
                .mapToInt(List::size)
                .sum();
    }
    
    /**
     * Creates an empty TagResponse.
     * 
     * @return Empty TagResponse with no tags or entities
     */
    public static TagResponse empty() {
        return new TagResponse();
    }
    
    /**
     * Merges another TagResponse into this one.
     * 
     * @param other The TagResponse to merge
     */
    public void merge(TagResponse other) {
        if (other != null) {
            if (other.tags != null) {
                addTags(other.tags);
            }
            if (other.entities != null) {
                for (Map.Entry<String, List<String>> entry : other.entities.entrySet()) {
                    addEntities(entry.getKey(), entry.getValue());
                }
            }
        }
    }
    
    // Getters and Setters
    
    public List<String> getTags() {
        return tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }
    
    public void setTags(List<String> tags) {
        this.tags = new ArrayList<>(tags != null ? tags : new ArrayList<>());
    }
    
    public Map<String, List<String>> getEntities() {
        if (entities == null) {
            return new HashMap<>();
        }
        // Return a deep copy to prevent external modification
        Map<String, List<String>> copy = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : entities.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }
    
    public void setEntities(Map<String, List<String>> entities) {
        if (entities == null) {
            this.entities = new HashMap<>();
        } else {
            // Create a deep copy
            this.entities = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : entities.entrySet()) {
                this.entities.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TagResponse that = (TagResponse) o;
        return Objects.equals(tags, that.tags) &&
                Objects.equals(entities, that.entities);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(tags, entities);
    }
    
    @Override
    public String toString() {
        return "TagResponse{" +
                "tags=" + (tags != null ? tags.size() : 0) + " items" +
                ", entities=" + (entities != null ? entities.size() : 0) + " categories" +
                '}';
    }
}