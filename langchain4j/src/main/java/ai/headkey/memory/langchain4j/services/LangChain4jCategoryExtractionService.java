package ai.headkey.memory.langchain4j.services;

import java.util.Objects;

import ai.headkey.memory.langchain4j.dto.CategoryResponse;
import ai.headkey.memory.langchain4j.services.ai.LangChain4jCategoryAiService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

/**
 * LangChain4j implementation of CategoryExtractionService.
 * 
 * This implementation uses LangChain4j's AI services to perform intelligent
 * content categorization using Large Language Models. It leverages the power
 * of LLMs to understand semantic meaning and context for accurate categorization.
 * 
 * The service is designed following SOLID principles:
 * - Single Responsibility: Focuses solely on category extraction
 * - Open/Closed: Can be extended without modification
 * - Dependency Inversion: Depends on ChatLanguageModel abstraction
 * 
 * @since 1.0
 */
public class LangChain4jCategoryExtractionService implements CategoryExtractionService {
    
    private final LangChain4jCategoryAiService aiService;
    private final String serviceName;
    
    /**
     * Constructor with ChatLanguageModel dependency.
     * 
     * @param chatModel The LangChain4j ChatModel to use for AI operations
     * @throws IllegalArgumentException if chatModel is null
     */
    public LangChain4jCategoryExtractionService(ChatModel chatModel) {
        Objects.requireNonNull(chatModel, "ChatModel cannot be null");
        this.aiService = AiServices.create(LangChain4jCategoryAiService.class, chatModel);
        this.serviceName = "LangChain4j-" + chatModel.getClass().getSimpleName();
    }

    /**
     * Constructor with ChatLanguageModel and service.
     * 
     * @param chatModel The LangChain4j ChatModel to use for AI operations
     * @param service The Service instance to use for AI operations
     * @param serviceName The name of the service
     * @throws IllegalArgumentException if chatModel, service, or serviceName is null
     */
    public LangChain4jCategoryExtractionService(ChatModel chatModel, LangChain4jCategoryAiService service, String serviceName) {
        Objects.requireNonNull(chatModel, "ChatModel cannot be null");
        this.aiService = Objects.requireNonNull(service, "AI service cannot be null");
        this.serviceName = Objects.requireNonNull(serviceName, "Service name cannot be null");
    }
    
    @Override
    public CategoryResponse categorizeContent(String content, String availableCategories, String contextMetadata) {
        validateInputs(content, availableCategories);
        
        try {
            // Ensure metadata is not null
            String metadata = contextMetadata != null ? contextMetadata : "{}";
            
            // Call the AI service for categorization
            CategoryResponse response = aiService.categorizeContent(content, availableCategories, metadata);
            
            // Validate and return response
            return validateAndNormalizeResponse(response, availableCategories);
            
        } catch (Exception e) {
            throw new CategoryExtractionException("Failed to categorize content", e);
        }
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Perform a simple health check by trying a minimal categorization
            CategoryResponse response = categorizeContent(
                "test content", 
                "Test,Unknown"
            );
            return response != null && response.getPrimary() != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getServiceName() {
        return serviceName;
    }
    
    /**
     * Validates input parameters for categorization.
     * 
     * @param content The content to validate
     * @param availableCategories The categories to validate
     * @throws IllegalArgumentException if inputs are invalid
     */
    private void validateInputs(String content, String availableCategories) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        if (availableCategories == null || availableCategories.trim().isEmpty()) {
            throw new IllegalArgumentException("Available categories cannot be null or empty");
        }
    }
    
    /**
     * Validates and normalizes the AI service response.
     * 
     * @param response The response from the AI service
     * @param availableCategories The available categories for validation
     * @return Validated and normalized CategoryResponse
     */
    private CategoryResponse validateAndNormalizeResponse(CategoryResponse response, String availableCategories) {
        if (response == null) {
            return CategoryResponse.unknown("AI service returned null response");
        }
        
        // Ensure primary category is set
        if (response.getPrimary() == null || response.getPrimary().trim().isEmpty()) {
            response.setPrimary("Unknown");
            response.setConfidence(0.1);
            response.setReasoning("AI service did not provide a primary category");
        }
        
        // Validate confidence is in proper range
        if (response.getConfidence() < 0.0 || response.getConfidence() > 1.0) {
            response.setConfidence(Math.max(0.0, Math.min(1.0, response.getConfidence())));
        }
        
        // Set default reasoning if missing
        if (response.getReasoning() == null || response.getReasoning().trim().isEmpty()) {
            response.setReasoning("Categorized as " + response.getPrimary());
        }
        
        return response;
    }
    
}