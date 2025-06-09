package ai.headkey.memory.langchain4j.services;

import ai.headkey.memory.langchain4j.dto.TagResponse;

/**
 * Service interface for AI-powered semantic tag and entity extraction.
 * 
 * This interface defines the contract for extracting semantic tags, named entities,
 * and other meaningful information from content using AI/ML models. Implementations
 * should provide intelligent analysis to identify key concepts, entities, and
 * relationships within the provided content.
 * 
 * Following the Interface Segregation Principle (ISP), this interface is focused
 * solely on tag and entity extraction responsibilities, keeping it lean and focused.
 * 
 * @since 1.0
 */
public interface TagExtractionService {
    
    /**
     * Extracts semantic tags and entities from the given content.
     * 
     * This method analyzes the provided content to identify and extract:
     * - Named entities (people, places, organizations)
     * - Key concepts and topics
     * - Temporal expressions
     * - Important keywords
     * - Relationships and connections
     * 
     * The extraction process uses semantic understanding to provide meaningful
     * tags that capture the essence and important elements of the content.
     * 
     * @param content The content to analyze for tag extraction (required)
     * @return TagResponse containing extracted tags and categorized entities
     * @throws IllegalArgumentException if content is null or empty
     * @throws TagExtractionException if extraction fails due to service issues
     */
    TagResponse extractTags(String content);
    
    /**
     * Extracts tags with additional context information.
     * 
     * This method allows for more sophisticated tag extraction by providing
     * additional context that can help the AI model better understand the
     * content and extract more relevant tags.
     * 
     * @param content The content to analyze for tag extraction (required)
     * @param contextMetadata Additional context information as JSON string (optional)
     * @return TagResponse containing extracted tags and categorized entities
     * @throws IllegalArgumentException if content is null or empty
     * @throws TagExtractionException if extraction fails due to service issues
     */
    TagResponse extractTags(String content, String contextMetadata);
    
    /**
     * Extracts tags focusing on specific entity types.
     * 
     * This method allows for targeted extraction by specifying which types
     * of entities should be prioritized during the extraction process.
     * 
     * @param content The content to analyze for tag extraction (required)
     * @param entityTypes Comma-separated list of entity types to focus on (optional)
     * @return TagResponse containing extracted tags and categorized entities
     * @throws IllegalArgumentException if content is null or empty
     * @throws TagExtractionException if extraction fails due to service issues
     */
    TagResponse extractTagsWithFocus(String content, String entityTypes);
    
    /**
     * Checks if the tag extraction service is available and functioning.
     * 
     * This method can be used for health checks and monitoring to ensure
     * the underlying AI service is responsive and operational.
     * 
     * @return true if the service is healthy and ready to process requests
     */
    boolean isHealthy();
    
    /**
     * Gets the name or identifier of the tag extraction service implementation.
     * 
     * This can be useful for logging, monitoring, and debugging purposes
     * to identify which specific implementation is being used.
     * 
     * @return A descriptive name of the service implementation
     */
    String getServiceName();
    
    /**
     * Gets the supported entity types that this service can extract.
     * 
     * Different implementations may support different sets of entity types.
     * This method allows clients to discover what types of entities can be
     * extracted by the current implementation.
     * 
     * @return Array of supported entity type names
     */
    String[] getSupportedEntityTypes();
    
    /**
     * Exception thrown when tag extraction operations fail.
     */
    class TagExtractionException extends RuntimeException {
        
        public TagExtractionException(String message) {
            super(message);
        }
        
        public TagExtractionException(String message, Throwable cause) {
            super(message, cause);
        }
        
        public TagExtractionException(Throwable cause) {
            super(cause);
        }
    }
}