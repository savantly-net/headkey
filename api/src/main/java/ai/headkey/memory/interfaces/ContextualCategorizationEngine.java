package ai.headkey.memory.interfaces;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.Metadata;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface for the Contextual Categorization Engine (CCE).
 * 
 * The CCE provides classification of content into contextual categories.
 * It analyzes textual content and metadata to determine appropriate
 * categorization labels, subcategories, and semantic tags.
 * 
 * This interface encapsulates whatever algorithm or model is used for
 * categorization (rules-based, ML models, ontologies, etc.) and provides
 * a clean abstraction for the categorization process.
 * 
 * The CCE supports both primary categorization and fine-grained semantic
 * tag extraction to enrich the memory metadata with contextual information.
 * 
 * @since 1.0
 */
public interface ContextualCategorizationEngine {
    
    /**
     * Categorizes a piece of information in context.
     * 
     * Analyzes the provided content and metadata to assign an appropriate
     * category label. The categorization process may use:
     * - Natural Language Processing (NLP) models
     * - Rule-based classification systems
     * - Ontology matching
     * - Machine learning classifiers
     * - Hybrid approaches combining multiple techniques
     * 
     * The metadata parameter can provide hints or context that influence
     * the categorization decision, such as:
     * - Source of the information
     * - Agent context
     * - Explicitly provided category hints
     * - Importance levels
     * 
     * @param content The textual content or data to categorize
     * @param meta Optional metadata that can inform categorization decisions
     * @return A CategoryLabel containing the assigned category, subcategory,
     *         tags, and confidence score
     * @throws IllegalArgumentException if content is null or empty
     * 
     * @since 1.0
     */
    CategoryLabel categorize(String content, Metadata meta);
    
    /**
     * Extracts semantic tags or entities from content.
     * 
     * Analyzes the content to identify and extract fine-grained descriptors
     * such as:
     * - Named entities (people, places, organizations)
     * - Temporal expressions (dates, times)
     * - Numerical values and measurements
     * - Keywords and key phrases
     * - Relationship indicators
     * - Domain-specific terms
     * 
     * These tags provide additional context and can be used for:
     * - Enhanced search and retrieval
     * - Content relationships
     * - Filtering and organization
     * - Analytics and insights
     * 
     * @param content The content to analyze for tag extraction
     * @return A set of tags or annotations derived from the content
     * @throws IllegalArgumentException if content is null or empty
     * 
     * @since 1.0
     */
    Set<String> extractTags(String content);
    
    /**
     * Categorizes multiple pieces of content in batch.
     * 
     * Efficiently processes multiple content items in a single operation.
     * This method is optimized for batch processing scenarios where
     * overhead reduction and throughput are important.
     * 
     * @param contentItems Map of identifier to content for batch processing
     * @param commonMeta Optional metadata common to all items
     * @return Map of identifier to CategoryLabel for each processed item
     * @throws IllegalArgumentException if contentItems is null or empty
     * 
     * @since 1.0
     */
    Map<String, CategoryLabel> categorizeBatch(Map<String, String> contentItems, Metadata commonMeta);
    
    /**
     * Gets the confidence threshold for categorization decisions.
     * 
     * Returns the minimum confidence level required for the engine to
     * assign a category. Content that cannot be categorized with sufficient
     * confidence may be assigned to a default or "unknown" category.
     * 
     * @return The confidence threshold (0.0 to 1.0)
     * 
     * @since 1.0
     */
    double getConfidenceThreshold();
    
    /**
     * Sets the confidence threshold for categorization decisions.
     * 
     * Configures the minimum confidence level required for category
     * assignment. Higher thresholds result in more conservative
     * categorization but may increase the number of items assigned
     * to default categories.
     * 
     * @param threshold The confidence threshold (0.0 to 1.0)
     * @throws IllegalArgumentException if threshold is outside valid range
     * 
     * @since 1.0
     */
    void setConfidenceThreshold(double threshold);
    
    /**
     * Gets all available categories that this engine can assign.
     * 
     * Returns the complete set of primary categories that the
     * categorization engine is capable of identifying and assigning.
     * This is useful for:
     * - Configuration validation
     * - UI category selection
     * - Analytics and reporting
     * - Testing and debugging
     * 
     * @return List of available category names
     * 
     * @since 1.0
     */
    List<String> getAvailableCategories();
    
    /**
     * Gets subcategories available for a given primary category.
     * 
     * Returns the set of subcategories that can be assigned within
     * a specific primary category. This supports hierarchical
     * categorization schemes.
     * 
     * @param primaryCategory The primary category to get subcategories for
     * @return Set of subcategory names, or empty set if none available
     * @throws IllegalArgumentException if primaryCategory is null or not supported
     * 
     * @since 1.0
     */
    Set<String> getSubcategories(String primaryCategory);
    
    /**
     * Suggests alternative categories for borderline content.
     * 
     * For content that has moderate confidence scores across multiple
     * categories, this method returns alternative categorization options
     * ranked by confidence. This is useful for:
     * - Manual review processes
     * - Quality assurance
     * - Ambiguous content handling
     * - User-assisted categorization
     * 
     * @param content The content to analyze for alternatives
     * @param meta Optional metadata for context
     * @param maxSuggestions Maximum number of alternative suggestions to return
     * @return List of CategoryLabel alternatives ordered by confidence (highest first)
     * @throws IllegalArgumentException if content is null or maxSuggestions < 1
     * 
     * @since 1.0
     */
    List<CategoryLabel> suggestAlternativeCategories(String content, Metadata meta, int maxSuggestions);
    
    /**
     * Updates the categorization model with feedback.
     * 
     * Allows the categorization engine to learn from correction feedback
     * by providing the original content, the category that was assigned,
     * and the correct category that should have been assigned.
     * 
     * This method supports continuous improvement of categorization
     * accuracy through active learning approaches.
     * 
     * @param content The original content that was categorized
     * @param assignedCategory The category that was originally assigned
     * @param correctCategory The correct category that should have been assigned
     * @throws IllegalArgumentException if any parameter is null
     * 
     * @since 1.0
     */
    void provideFeedback(String content, CategoryLabel assignedCategory, CategoryLabel correctCategory);
    
    /**
     * Gets statistics about categorization performance.
     * 
     * Returns metrics and statistics about the categorization engine's
     * performance and operation history, including:
     * - Total number of categorizations performed
     * - Category distribution
     * - Average confidence scores
     * - Processing times
     * - Error rates
     * - Model performance metrics
     * 
     * @return A map containing various statistics and metrics
     * 
     * @since 1.0
     */
    Map<String, Object> getCategorizationStatistics();
    
    /**
     * Checks if the categorization engine is healthy and ready.
     * 
     * Performs a health check of the categorization engine and its
     * dependencies. This includes verifying:
     * - Model availability and loading status
     * - Required resources and configurations
     * - External service connectivity (if applicable)
     * - Memory and processing capacity
     * 
     * @return true if the engine is healthy and ready to process requests
     * 
     * @since 1.0
     */
    boolean isHealthy();
}