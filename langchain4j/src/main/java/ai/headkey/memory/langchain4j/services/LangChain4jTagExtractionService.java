package ai.headkey.memory.langchain4j.services;

import ai.headkey.memory.langchain4j.dto.TagResponse;
import ai.headkey.memory.langchain4j.services.ai.LangChain4jTagAiService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * LangChain4j implementation of TagExtractionService.
 * 
 * This implementation uses LangChain4j's AI services to perform intelligent
 * semantic tag and entity extraction using Large Language Models. It leverages
 * the power of LLMs to understand semantic meaning, context, and relationships
 * for accurate tag and entity identification.
 * 
 * The service is designed following SOLID principles:
 * - Single Responsibility: Focuses solely on tag and entity extraction
 * - Open/Closed: Can be extended without modification
 * - Dependency Inversion: Depends on ChatLanguageModel abstraction
 * 
 * @since 1.0
 */
public class LangChain4jTagExtractionService implements TagExtractionService {
    
    private final LangChain4jTagAiService aiService;
    private final String serviceName;
    
    // Pattern-based entity extraction patterns for fallback
    private final Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private final Pattern phonePattern = Pattern.compile("\\b\\d{3}-\\d{3}-\\d{4}\\b|\\b\\(\\d{3}\\)\\s*\\d{3}-\\d{4}\\b");
    private final Pattern urlPattern = Pattern.compile("https?://[\\w\\.-]+\\.[a-zA-Z]{2,}[\\w\\.-]*/?[\\w\\.-?=%&]*");
    private final Pattern datePattern = Pattern.compile("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b|\\b\\d{4}-\\d{2}-\\d{2}\\b");
    
    // Supported entity types
    private static final String[] SUPPORTED_ENTITY_TYPES = {
        "person", "place", "organization", "technology", "product", "event",
        "date", "time", "email", "phone", "url", "profession", "concept",
        "location", "company", "skill", "tool", "framework", "language"
    };
    
    /**
     * Constructor with ChatModel dependency.
     * 
     * @param chatModel The LangChain4j ChatModel to use for AI operations
     * @throws IllegalArgumentException if chatModel is null
     */
    public LangChain4jTagExtractionService(ChatModel chatModel) {
        Objects.requireNonNull(chatModel, "ChatModel cannot be null");
        this.aiService = AiServices.create(LangChain4jTagAiService.class, chatModel);
        this.serviceName = "LangChain4j-TagExtraction-" + chatModel.getClass().getSimpleName();
    }

    /**
     * Constructor with ChatModel and service.
     * 
     * @param chatModel The LangChain4j ChatModel to use for AI operations
     * @param service The Service instance to use for AI operations
     * @param serviceName The name of the service
     * @throws IllegalArgumentException if chatModel, service, or serviceName is null
     */
    public LangChain4jTagExtractionService(ChatModel chatModel, LangChain4jTagAiService service, String serviceName) {
        Objects.requireNonNull(chatModel, "ChatModel cannot be null");
        this.aiService = Objects.requireNonNull(service, "AI service cannot be null");
        this.serviceName = Objects.requireNonNull(serviceName, "Service name cannot be null");
    }
    
    @Override
    public TagResponse extractTags(String content) {
        return extractTags(content, "{}");
    }
    
    @Override
    public TagResponse extractTags(String content, String contextMetadata) {
        validateContent(content);
        
        try {
            // Ensure metadata is not null
            String metadata = contextMetadata != null ? contextMetadata : "{}";
            
            // Call the AI service for tag extraction
            TagResponse response = aiService.extractTags(content, metadata);
            
            // Enhance with pattern-based extraction
            enhanceWithPatternBasedTags(response, content);
            
            // Validate and return response
            return validateAndNormalizeResponse(response);
            
        } catch (Exception e) {
            // Fallback to pattern-based extraction only
            TagResponse fallbackResponse = createPatternBasedResponse(content);
            if (fallbackResponse.hasData()) {
                return fallbackResponse;
            }
            throw new TagExtractionException("Failed to extract tags", e);
        }
    }
    
    @Override
    public TagResponse extractTagsWithFocus(String content, String entityTypes) {
        validateContent(content);
        
        try {
            // Prepare focused extraction prompt
            String focusedEntityTypes = entityTypes != null ? entityTypes : String.join(",", SUPPORTED_ENTITY_TYPES);
            
            // Call the AI service with focused extraction
            TagResponse response = aiService.extractTagsWithFocus(content, focusedEntityTypes);
            
            // Enhance with pattern-based extraction for specific types
            enhanceWithPatternBasedTagsForTypes(response, content, focusedEntityTypes);
            
            // Validate and return response
            return validateAndNormalizeResponse(response);
            
        } catch (Exception e) {
            throw new TagExtractionException("Failed to extract focused tags", e);
        }
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Perform a simple health check by trying minimal tag extraction
            TagResponse response = extractTags("test content for health check");
            return response != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getServiceName() {
        return serviceName;
    }
    
    @Override
    public String[] getSupportedEntityTypes() {
        return SUPPORTED_ENTITY_TYPES.clone();
    }
    
    /**
     * Validates content input parameter.
     * 
     * @param content The content to validate
     * @throws IllegalArgumentException if content is invalid
     */
    private void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
    }
    
    /**
     * Enhances AI-extracted tags with pattern-based extraction.
     * 
     * @param response The response to enhance
     * @param content The original content to extract from
     */
    private void enhanceWithPatternBasedTags(TagResponse response, String content) {
        // Extract emails
        emailPattern.matcher(content).results()
            .forEach(match -> {
                response.addTag(match.group());
                response.addEntity("email", match.group());
            });
        
        // Extract phone numbers
        phonePattern.matcher(content).results()
            .forEach(match -> {
                response.addTag(match.group());
                response.addEntity("phone", match.group());
            });
        
        // Extract URLs
        urlPattern.matcher(content).results()
            .forEach(match -> {
                response.addTag(match.group());
                response.addEntity("url", match.group());
            });
        
        // Extract dates
        datePattern.matcher(content).results()
            .forEach(match -> {
                response.addTag(match.group());
                response.addEntity("date", match.group());
            });
    }
    
    /**
     * Enhances response with pattern-based extraction for specific entity types.
     * 
     * @param response The response to enhance
     * @param content The original content
     * @param focusedTypes Comma-separated list of focused entity types
     */
    private void enhanceWithPatternBasedTagsForTypes(TagResponse response, String content, String focusedTypes) {
        String[] types = focusedTypes.toLowerCase().split(",");
        
        for (String type : types) {
            type = type.trim();
            switch (type) {
                case "email":
                    emailPattern.matcher(content).results()
                        .forEach(match -> {
                            response.addTag(match.group());
                            response.addEntity("email", match.group());
                        });
                    break;
                case "phone":
                    phonePattern.matcher(content).results()
                        .forEach(match -> {
                            response.addTag(match.group());
                            response.addEntity("phone", match.group());
                        });
                    break;
                case "url":
                    urlPattern.matcher(content).results()
                        .forEach(match -> {
                            response.addTag(match.group());
                            response.addEntity("url", match.group());
                        });
                    break;
                case "date":
                    datePattern.matcher(content).results()
                        .forEach(match -> {
                            response.addTag(match.group());
                            response.addEntity("date", match.group());
                        });
                    break;
            }
        }
    }
    
    /**
     * Creates a fallback response using only pattern-based extraction.
     * 
     * @param content The content to extract from
     * @return TagResponse with pattern-based tags
     */
    private TagResponse createPatternBasedResponse(String content) {
        TagResponse response = new TagResponse();
        enhanceWithPatternBasedTags(response, content);
        return response;
    }
    
    /**
     * Validates and normalizes the AI service response.
     * 
     * @param response The response from the AI service
     * @return Validated and normalized TagResponse
     */
    private TagResponse validateAndNormalizeResponse(TagResponse response) {
        if (response == null) {
            return new TagResponse();
        }
        
        // Response is already validated through DTO methods
        return response;
    }
    

}