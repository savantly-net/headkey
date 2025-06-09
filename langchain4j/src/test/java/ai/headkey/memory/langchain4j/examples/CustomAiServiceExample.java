package ai.headkey.memory.langchain4j.examples;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.headkey.memory.langchain4j.dto.CategoryResponse;
import ai.headkey.memory.langchain4j.dto.TagResponse;
import ai.headkey.memory.langchain4j.services.CategoryExtractionService;
import ai.headkey.memory.langchain4j.services.LangChain4jCategoryExtractionService;
import ai.headkey.memory.langchain4j.services.LangChain4jTagExtractionService;
import ai.headkey.memory.langchain4j.services.TagExtractionService;
import ai.headkey.memory.langchain4j.services.ai.LangChain4jCategoryAiService;
import ai.headkey.memory.langchain4j.services.ai.LangChain4jTagAiService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * Example demonstrating how to create custom AI service implementations
 * for specialized use cases while maintaining compatibility with the
 * LangChain4j service architecture.
 * 
 * This example shows:
 * 1. Custom category AI service with domain-specific logic
 * 2. Custom tag extraction AI service with specialized patterns
 * 3. Integration with the existing service architecture
 * 4. Fallback and validation patterns
 */
public class CustomAiServiceExample {
    
    public static void main(String[] args) {
        demonstrateCustomCategoryService();
        demonstrateCustomTagService();
        demonstrateMixedServices();
    }
    
    /**
     * Demonstrates a custom category AI service implementation.
     */
    private static void demonstrateCustomCategoryService() {
        System.out.println("=== Custom Category Service Example ===");
        
        // Create a mock ChatModel (in real usage, you'd use a real one)
        ChatModel chatModel = createMockChatModel();
        
        // Create custom AI service
        LangChain4jCategoryAiService customCategoryAi = new DomainSpecificCategoryAiService();
        
        // Create service with custom AI implementation
        CategoryExtractionService categoryService = new LangChain4jCategoryExtractionService(
            chatModel, customCategoryAi, "DomainSpecificCategoryService"
        );
        
        // Test the custom service
        CategoryResponse response = categoryService.categorizeContent(
            "Patient John Doe has high blood pressure and diabetes",
            "Medical,Personal,Business,Technical",
            "{\"domain\":\"healthcare\"}"
        );
        
        System.out.println("Service: " + categoryService.getServiceName());
        System.out.println("Primary: " + response.getPrimary());
        System.out.println("Secondary: " + response.getSecondary());
        System.out.println("Confidence: " + response.getConfidence());
        System.out.println("Reasoning: " + response.getReasoning());
        System.out.println();
    }
    
    /**
     * Demonstrates a custom tag extraction AI service implementation.
     */
    private static void demonstrateCustomTagService() {
        System.out.println("=== Custom Tag Service Example ===");
        
        // Create a mock ChatModel
        ChatModel chatModel = createMockChatModel();
        
        // Create custom AI service
        LangChain4jTagAiService customTagAi = new MedicalTermsTagAiService();
        
        // Create service with custom AI implementation
        TagExtractionService tagService = new LangChain4jTagExtractionService(
            chatModel, customTagAi, "MedicalTermsTagService"
        );
        
        // Test the custom service
        TagResponse response = tagService.extractTags(
            "Patient presents with acute myocardial infarction and requires immediate intervention",
            "{\"specialty\":\"cardiology\"}"
        );
        
        System.out.println("Service: " + tagService.getServiceName());
        System.out.println("Tags: " + response.getTags());
        System.out.println("Entities: " + response.getEntities());
        System.out.println();
    }
    
    /**
     * Demonstrates mixing custom and default services.
     */
    private static void demonstrateMixedServices() {
        System.out.println("=== Mixed Services Example ===");
        
        ChatModel chatModel = createMockChatModel();
        
        // Use custom category service but default tag service
        LangChain4jCategoryAiService customCategoryAi = new DomainSpecificCategoryAiService();
        CategoryExtractionService categoryService = new LangChain4jCategoryExtractionService(
            chatModel, customCategoryAi, "CustomCategoryService"
        );
        
        // Use default tag service
        TagExtractionService tagService = new LangChain4jTagExtractionService(chatModel);
        
        System.out.println("Category Service: " + categoryService.getServiceName());
        System.out.println("Tag Service: " + tagService.getServiceName());
        System.out.println("Mixed approach allows specialized behavior where needed!");
        System.out.println();
    }
    
    /**
     * Custom category AI service for domain-specific categorization.
     * This example focuses on healthcare domain classification.
     */
    private static class DomainSpecificCategoryAiService implements LangChain4jCategoryAiService {
        
        @Override
        public CategoryResponse categorizeContent(String content, String availableCategories, String contextMetadata) {
            // Domain-specific logic for healthcare content
            String lowerContent = content.toLowerCase();
            
            if (containsMedicalTerms(lowerContent)) {
                return new CategoryResponse(
                    "Medical",
                    detectMedicalSubcategory(lowerContent),
                    0.95,
                    "Detected medical terminology and healthcare context"
                );
            }
            
            if (containsPatientInfo(lowerContent)) {
                return new CategoryResponse(
                    "Personal",
                    "PatientData",
                    0.90,
                    "Contains patient information and personal health data"
                );
            }
            
            // Fallback to general categorization
            return new CategoryResponse(
                "Unknown",
                null,
                0.3,
                "Domain-specific patterns not detected"
            );
        }
        
        private boolean containsMedicalTerms(String content) {
            String[] medicalTerms = {
                "patient", "diagnosis", "treatment", "medication", "symptoms",
                "blood pressure", "diabetes", "hypertension", "myocardial",
                "intervention", "acute", "chronic"
            };
            
            for (String term : medicalTerms) {
                if (content.contains(term)) {
                    return true;
                }
            }
            return false;
        }
        
        private boolean containsPatientInfo(String content) {
            return content.contains("patient") && 
                   (content.contains("john") || content.contains("jane") || 
                    content.contains("mr.") || content.contains("ms."));
        }
        
        private String detectMedicalSubcategory(String content) {
            if (content.contains("cardio") || content.contains("heart") || content.contains("myocardial")) {
                return "Cardiology";
            }
            if (content.contains("diabetes") || content.contains("blood sugar")) {
                return "Endocrinology";
            }
            if (content.contains("pressure") || content.contains("hypertension")) {
                return "Hypertension";
            }
            return "General";
        }
    }
    
    /**
     * Custom tag extraction AI service specialized for medical terminology.
     */
    private static class MedicalTermsTagAiService implements LangChain4jTagAiService {
        
        @Override
        public TagResponse extractTags(String content, String contextMetadata) {
            TagResponse response = new TagResponse();
            String lowerContent = content.toLowerCase();
            
            // Extract medical conditions
            extractMedicalConditions(lowerContent, response);
            
            // Extract medical procedures
            extractMedicalProcedures(lowerContent, response);
            
            // Extract anatomical terms
            extractAnatomicalTerms(lowerContent, response);
            
            // Extract urgency indicators
            extractUrgencyIndicators(lowerContent, response);
            
            return response;
        }
        
        @Override
        public TagResponse extractTagsWithFocus(String content, String entityTypes) {
            TagResponse response = extractTags(content, "{}");
            
            // Filter results based on focused entity types
            if (entityTypes.contains("condition")) {
                // Keep only medical condition entities
                Map<String, List<String>> filteredEntities = new HashMap<>();
                if (response.getEntities().containsKey("condition")) {
                    filteredEntities.put("condition", response.getEntities().get("condition"));
                }
                response.setEntities(filteredEntities);
            }
            
            return response;
        }
        
        private void extractMedicalConditions(String content, TagResponse response) {
            String[] conditions = {
                "myocardial infarction", "heart attack", "diabetes", "hypertension",
                "blood pressure", "acute", "chronic"
            };
            
            for (String condition : conditions) {
                if (content.contains(condition)) {
                    response.addTag(condition);
                    response.addEntity("condition", condition);
                }
            }
        }
        
        private void extractMedicalProcedures(String content, TagResponse response) {
            String[] procedures = {
                "intervention", "surgery", "treatment", "therapy", "medication"
            };
            
            for (String procedure : procedures) {
                if (content.contains(procedure)) {
                    response.addTag(procedure);
                    response.addEntity("procedure", procedure);
                }
            }
        }
        
        private void extractAnatomicalTerms(String content, TagResponse response) {
            String[] anatomicalTerms = {
                "heart", "cardiac", "myocardial", "vascular", "arterial"
            };
            
            for (String term : anatomicalTerms) {
                if (content.contains(term)) {
                    response.addTag(term);
                    response.addEntity("anatomy", term);
                }
            }
        }
        
        private void extractUrgencyIndicators(String content, TagResponse response) {
            String[] urgencyTerms = {
                "immediate", "urgent", "acute", "emergency", "critical"
            };
            
            for (String term : urgencyTerms) {
                if (content.contains(term)) {
                    response.addTag(term);
                    response.addEntity("urgency", term);
                }
            }
        }
    }
    
    /**
     * Creates a mock ChatModel for demonstration purposes.
     * In real usage, you would create a proper ChatModel with actual AI backend.
     */
    private static ChatModel createMockChatModel() {
        try {
            // Try to create a real OpenAI model if API key is available
            String apiKey = System.getenv("OPENAI_API_KEY");
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                return OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName("gpt-3.5-turbo")
                    .temperature(0.3)
                    .build();
            }
        } catch (Exception e) {
            System.out.println("Note: Using mock ChatModel since no OpenAI API key available");
        }
        
        // Return a mock implementation for demonstration
        return new MockChatModel();
    }
    
    /**
     * Mock ChatModel implementation for testing without actual AI backend.
     */
    private static class MockChatModel implements ChatModel {
        @Override
        public ChatResponse chat(
                List<dev.langchain4j.data.message.ChatMessage> messages) {
            // Return a mock response
            dev.langchain4j.data.message.AiMessage mockMessage = 
                dev.langchain4j.data.message.AiMessage.from("Mock AI response");
            return ChatResponse.builder().aiMessage(mockMessage).build();
        }
        
        @Override
        public ChatResponse chat(
                dev.langchain4j.data.message.ChatMessage... messages) {
            return chat(Arrays.asList(messages));
        }
    }
}