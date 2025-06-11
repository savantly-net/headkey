package ai.headkey.memory.enums;

/**
 * Defines the types of relationships that can exist between beliefs in the knowledge graph.
 * 
 * This enum categorizes different semantic and temporal relationships that can be established
 * between beliefs, enabling rich knowledge graph functionality with support for belief
 * evolution, deprecation, and semantic connections.
 * 
 * @since 1.0
 */
public enum RelationshipType {
    
    // Temporal relationships - for belief evolution and deprecation
    SUPERSEDES("supersedes", "Source belief supersedes target belief", true, "Temporal"),
    UPDATES("updates", "Source belief updates target belief", true, "Temporal"),
    DEPRECATES("deprecates", "Source belief deprecates target belief", true, "Temporal"),
    REPLACES("replaces", "Source belief replaces target belief", true, "Temporal"),
    
    // Logical relationships - for reasoning and inference
    SUPPORTS("supports", "Source belief provides evidence for target belief", false, "Logical"),
    CONTRADICTS("contradicts", "Source belief contradicts target belief", false, "Logical"),
    IMPLIES("implies", "Source belief logically implies target belief", false, "Logical"),
    REINFORCES("reinforces", "Source belief strengthens target belief", false, "Logical"),
    WEAKENS("weakens", "Source belief reduces confidence in target belief", false, "Logical"),
    
    // Semantic relationships - for content organization
    RELATES_TO("relates_to", "Source belief is semantically related to target belief", false, "Semantic"),
    SPECIALIZES("specializes", "Source belief is a more specific case of target belief", false, "Semantic"),
    GENERALIZES("generalizes", "Source belief is a more general case of target belief", false, "Semantic"),
    EXTENDS("extends", "Source belief extends or builds upon target belief", false, "Semantic"),
    DERIVES_FROM("derives_from", "Source belief is derived from target belief", false, "Semantic"),
    
    // Causal relationships - for cause and effect
    CAUSES("causes", "Source belief describes a cause of target belief", false, "Causal"),
    CAUSED_BY("caused_by", "Source belief is caused by target belief", false, "Causal"),
    ENABLES("enables", "Source belief enables or makes possible target belief", false, "Causal"),
    PREVENTS("prevents", "Source belief prevents target belief", false, "Causal"),
    
    // Contextual relationships - for situational connections
    DEPENDS_ON("depends_on", "Source belief depends on target belief", false, "Contextual"),
    PRECEDES("precedes", "Source belief temporally precedes target belief", false, "Contextual"),
    FOLLOWS("follows", "Source belief temporally follows target belief", false, "Contextual"),
    CONTEXT_FOR("context_for", "Source belief provides context for target belief", false, "Contextual"),
    
    // Evidence relationships - for supporting information
    EVIDENCED_BY("evidenced_by", "Source belief is evidenced by target belief", false, "Evidence"),
    PROVIDES_EVIDENCE_FOR("provides_evidence_for", "Source belief provides evidence for target belief", false, "Evidence"),
    CONFLICTS_WITH("conflicts_with", "Source belief conflicts with target belief", false, "Evidence"),
    
    // Similarity relationships - for analogies and comparisons
    SIMILAR_TO("similar_to", "Source belief is similar to target belief", false, "Similarity"),
    ANALOGOUS_TO("analogous_to", "Source belief is analogous to target belief", false, "Similarity"),
    CONTRASTS_WITH("contrasts_with", "Source belief contrasts with target belief", false, "Similarity"),
    
    // Custom relationship for extensibility
    CUSTOM("custom", "Custom relationship type defined by metadata", false, "Custom");
    
    private final String code;
    private final String description;
    private final boolean isTemporal;
    private final String category;
    
    /**
     * Constructor for RelationshipType enum.
     * 
     * @param code The string code representing this relationship type
     * @param description Human-readable description of the relationship
     * @param isTemporal Whether this relationship type is temporal in nature
     * @param category The category this relationship belongs to
     */
    RelationshipType(String code, String description, boolean isTemporal, String category) {
        this.code = code;
        this.description = description;
        this.isTemporal = isTemporal;
        this.category = category;
    }
    
    /**
     * Gets the string code for this relationship type.
     * 
     * @return The relationship type code
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Gets the human-readable description of this relationship.
     * 
     * @return The relationship description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if this relationship type is temporal in nature.
     * Temporal relationships typically involve belief evolution, deprecation, or time-based changes.
     * 
     * @return true if this is a temporal relationship type
     */
    public boolean isTemporal() {
        return isTemporal;
    }
    
    /**
     * Gets the category this relationship type belongs to.
     * 
     * @return The relationship category
     */
    public String getCategory() {
        return category;
    }
    
    /**
     * Checks if this relationship type indicates deprecation or supersession.
     * 
     * @return true if this relationship type deprecates the target belief
     */
    public boolean isDeprecating() {
        return this == SUPERSEDES || this == UPDATES || this == DEPRECATES || this == REPLACES;
    }
    
    /**
     * Checks if this relationship type is bidirectional by nature.
     * Some relationships like SIMILAR_TO are naturally bidirectional.
     * 
     * @return true if this relationship type is typically bidirectional
     */
    public boolean isBidirectional() {
        return this == SIMILAR_TO || this == ANALOGOUS_TO || this == RELATES_TO;
    }
    
    /**
     * Gets the inverse relationship type if one exists.
     * For example, CAUSES has inverse CAUSED_BY.
     * 
     * @return The inverse relationship type, or null if no natural inverse exists
     */
    public RelationshipType getInverse() {
        switch (this) {
            case CAUSES: return CAUSED_BY;
            case CAUSED_BY: return CAUSES;
            case SPECIALIZES: return GENERALIZES;
            case GENERALIZES: return SPECIALIZES;
            case PRECEDES: return FOLLOWS;
            case FOLLOWS: return PRECEDES;
            case EVIDENCED_BY: return PROVIDES_EVIDENCE_FOR;
            case PROVIDES_EVIDENCE_FOR: return EVIDENCED_BY;
            case DERIVES_FROM: return EXTENDS; // Approximate inverse
            case SUPPORTS: return REINFORCES; // Approximate inverse
            default: return null;
        }
    }
    
    /**
     * Parses a string code to get the corresponding RelationshipType.
     * 
     * @param code The string code to parse
     * @return The corresponding RelationshipType, or null if not found
     */
    public static RelationshipType fromCode(String code) {
        if (code == null) {
            return null;
        }
        
        for (RelationshipType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * Gets all temporal relationship types.
     * 
     * @return Array of temporal relationship types
     */
    public static RelationshipType[] getTemporalTypes() {
        return new RelationshipType[]{SUPERSEDES, UPDATES, DEPRECATES, REPLACES};
    }
    
    /**
     * Gets all logical relationship types.
     * 
     * @return Array of logical relationship types
     */
    public static RelationshipType[] getLogicalTypes() {
        return new RelationshipType[]{SUPPORTS, CONTRADICTS, IMPLIES, REINFORCES, WEAKENS};
    }
    
    /**
     * Gets all semantic relationship types.
     * 
     * @return Array of semantic relationship types
     */
    public static RelationshipType[] getSemanticTypes() {
        return new RelationshipType[]{RELATES_TO, SPECIALIZES, GENERALIZES, EXTENDS, DERIVES_FROM};
    }
    
    /**
     * Gets relationship types by category.
     * 
     * @param category The category to filter by
     * @return Array of relationship types in the specified category
     */
    public static RelationshipType[] getByCategory(String category) {
        if (category == null) {
            return new RelationshipType[0];
        }
        
        return java.util.Arrays.stream(values())
                .filter(type -> category.equalsIgnoreCase(type.category))
                .toArray(RelationshipType[]::new);
    }
    
    @Override
    public String toString() {
        return code + " (" + description + ")";
    }
}