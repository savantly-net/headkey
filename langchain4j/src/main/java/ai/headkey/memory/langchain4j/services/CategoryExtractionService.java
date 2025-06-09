package ai.headkey.memory.langchain4j.services;

import ai.headkey.memory.langchain4j.dto.CategoryResponse;

/**
 * Service interface for AI-powered content categorization.
 * 
 * This interface defines the contract for categorizing content using AI/ML models.
 * Implementations should provide intelligent categorization based on content analysis,
 * context understanding, and semantic meaning.
 * 
 * Following the Interface Segregation Principle (ISP), this interface is focused
 * solely on categorization responsibilities, keeping it lean and focused.
 * 
 * @since 1.0
 */
public interface CategoryExtractionService {
    
    /**
     * Categorizes the given content based on available categories and context.
     * 
     * This method analyzes the provided content and assigns it to the most
     * appropriate category from the available options. The categorization
     * takes into account the content's semantic meaning, context metadata,
     * and the predefined category schema.
     * 
     * @param content The content to categorize (required)
     * @param availableCategories Comma-separated list of available categories (required)
     * @param contextMetadata Additional context information as JSON string (optional)
     * @return CategoryResponse containing primary/secondary categories, confidence, and reasoning
     * @throws IllegalArgumentException if content or availableCategories is null/empty
     * @throws CategoryExtractionException if categorization fails due to service issues
     */
    CategoryResponse categorizeContent(String content, String availableCategories, String contextMetadata);
    
    /**
     * Categorizes content using default metadata.
     * 
     * Convenience method that calls the full categorization method with empty metadata.
     * 
     * @param content The content to categorize (required)
     * @param availableCategories Comma-separated list of available categories (required)
     * @return CategoryResponse containing categorization results
     * @throws IllegalArgumentException if content or availableCategories is null/empty
     * @throws CategoryExtractionException if categorization fails due to service issues
     */
    default CategoryResponse categorizeContent(String content, String availableCategories) {
        return categorizeContent(content, availableCategories, "{}");
    }
    
    /**
     * Checks if the categorization service is available and functioning.
     * 
     * This method can be used for health checks and monitoring to ensure
     * the underlying AI service is responsive and operational.
     * 
     * @return true if the service is healthy and ready to process requests
     */
    boolean isHealthy();
    
    /**
     * Gets the name or identifier of the categorization service implementation.
     * 
     * This can be useful for logging, monitoring, and debugging purposes
     * to identify which specific implementation is being used.
     * 
     * @return A descriptive name of the service implementation
     */
    String getServiceName();
    
    /**
     * Exception thrown when category extraction operations fail.
     */
    class CategoryExtractionException extends RuntimeException {
        
        public CategoryExtractionException(String message) {
            super(message);
        }
        
        public CategoryExtractionException(String message, Throwable cause) {
            super(message, cause);
        }
        
        public CategoryExtractionException(Throwable cause) {
            super(cause);
        }
    }
}