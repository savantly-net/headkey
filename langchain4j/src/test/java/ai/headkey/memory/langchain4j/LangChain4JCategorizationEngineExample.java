package ai.headkey.memory.langchain4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.Metadata;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * Example demonstrating usage of LangChain4JContextualCategorizationEngine.
 * 
 * This example shows how to:
 * - Initialize the engine with different LangChain4j models
 * - Perform content categorization
 * - Extract semantic tags
 * - Use batch processing
 * - Configure custom categories
 * - Monitor performance statistics
 */
public class LangChain4JCategorizationEngineExample {
    
    public static void main(String[] args) {
        // Example with OpenAI (requires API key in environment)
        exampleWithOpenAI();
        
        // Example with custom configuration
        exampleWithCustomConfiguration();
        
        // Example batch processing
        exampleBatchProcessing();
        
        // Example statistics and monitoring
        exampleStatisticsAndMonitoring();
    }
    
    /**
     * Example using OpenAI ChatGPT model.
     * Requires OPENAI_API_KEY environment variable.
     */
    public static void exampleWithOpenAI() {
        System.out.println("=== OpenAI Example ===");
        
        try {
            // Create OpenAI chat model
            ChatModel chatModel = OpenAiChatModel.builder()
                    .apiKey(System.getenv("OPENAI_API_KEY"))
                    .modelName("gpt-3.5-turbo")
                    .temperature(0.3)
                    .build();
            
            // Initialize the categorization engine
            LangChain4JContextualCategorizationEngine engine = 
                new LangChain4JContextualCategorizationEngine(chatModel);
            
            // Example categorizations
            demonstrateBasicCategorization(engine);
            
        } catch (Exception e) {
            System.err.println("OpenAI example failed (likely missing API key): " + e.getMessage());
            System.out.println("Set OPENAI_API_KEY environment variable to run this example.");
        }
    }
    
    /**
     * Example with custom configuration and categories.
     */
    public static void exampleWithCustomConfiguration() {
        System.out.println("\n=== Custom Configuration Example ===");
        
        // Create a mock chat model for demonstration
        ChatModel mockModel = createMockChatModel();
        
        LangChain4JContextualCategorizationEngine engine = 
            new LangChain4JContextualCategorizationEngine(mockModel);
        
        // Add custom categories
        engine.addCustomCategory("ProjectManagement", 
            Set.of("Planning", "Execution", "Monitoring", "Documentation"));
        engine.addCustomCategory("CustomerService", 
            Set.of("Inquiry", "Complaint", "Feedback", "Support"));
        
        // Set custom confidence threshold
        engine.setConfidenceThreshold(0.8);
        
        System.out.println("Available categories: " + engine.getAvailableCategories());
        System.out.println("ProjectManagement subcategories: " + 
            engine.getSubcategories("ProjectManagement"));
        System.out.println("Confidence threshold: " + engine.getConfidenceThreshold());
        
        // Test categorization with custom categories
        String projectContent = "We need to update the project timeline and deliverables for Q2";
        CategoryLabel result = engine.categorize(projectContent, null);
        System.out.println("Project content categorized as: " + result.getPrimary() + 
            " (confidence: " + result.getConfidence() + ")");
    }
    
    /**
     * Example demonstrating batch processing capabilities.
     */
    public static void exampleBatchProcessing() {
        System.out.println("\n=== Batch Processing Example ===");
        
        ChatModel mockModel = createMockChatModel();
        LangChain4JContextualCategorizationEngine engine = 
            new LangChain4JContextualCategorizationEngine(mockModel);
        
        // Prepare batch content
        Map<String, String> batchContent = new HashMap<>();
        batchContent.put("user1", "I love hiking in the mountains and taking photos");
        batchContent.put("user2", "The capital of France is Paris, located on the Seine River");
        batchContent.put("user3", "My email is john.doe@company.com and phone is 555-1234");
        batchContent.put("user4", "I'm working on a Java Spring Boot project with microservices");
        batchContent.put("user5", "Feeling excited about the upcoming vacation to Tokyo");
        
        // Create common metadata
        Metadata commonMeta = new Metadata();
        commonMeta.setProperty("source", "user_survey");
        commonMeta.setProperty("timestamp", System.currentTimeMillis());
        
        // Process batch
        long startTime = System.currentTimeMillis();
        Map<String, CategoryLabel> results = engine.categorizeBatch(batchContent, commonMeta);
        long processingTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Processed " + results.size() + " items in " + processingTime + "ms");
        
        // Display results
        results.forEach((id, label) -> {
            System.out.printf("%-6s: %-15s %-12s (%.2f confidence)%n", 
                id, label.getPrimary(), 
                label.getSecondary() != null ? label.getSecondary() : "", 
                label.getConfidence());
        });
    }
    
    /**
     * Example showing statistics and health monitoring.
     */
    public static void exampleStatisticsAndMonitoring() {
        System.out.println("\n=== Statistics and Monitoring Example ===");
        
        ChatModel mockModel = createMockChatModel();
        LangChain4JContextualCategorizationEngine engine = 
            new LangChain4JContextualCategorizationEngine(mockModel);
        
        // Perform various operations to generate statistics
        engine.categorize("Personal information about my background", null);
        engine.categorize("Scientific fact about quantum physics", null);
        engine.categorize("Business process for handling customer complaints", null);
        engine.extractTags("Contact John at john@example.com or call 555-123-4567");
        
        // Check health
        boolean healthy = engine.isHealthy();
        System.out.println("Engine health status: " + (healthy ? "HEALTHY" : "UNHEALTHY"));
        
        // Get comprehensive statistics
        Map<String, Object> stats = engine.getCategorizationStatistics();
        
        System.out.println("\nPerformance Statistics:");
        System.out.println("Total categorizations: " + stats.get("totalCategorizations"));
        System.out.println("Total tag extractions: " + stats.get("totalTagExtractions"));
        System.out.println("Uptime seconds: " + stats.get("uptimeSeconds"));
        System.out.println("Confidence threshold: " + stats.get("confidenceThreshold"));
        
        // Display category distribution
        @SuppressWarnings("unchecked")
        Map<String, Object> categoryDistribution = 
            (Map<String, Object>) stats.get("categoryDistribution");
        
        if (categoryDistribution != null && !categoryDistribution.isEmpty()) {
            System.out.println("\nCategory Distribution:");
            categoryDistribution.forEach((category, catStats) -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> categoryStats = (Map<String, Object>) catStats;
                System.out.printf("  %-15s: %d uses, %.1f%% of total, avg confidence: %.2f%n",
                    category,
                    categoryStats.get("count"),
                    categoryStats.get("percentage"),
                    categoryStats.get("averageConfidence"));
            });
        }
        
        // Model information
        @SuppressWarnings("unchecked")
        Map<String, Object> modelInfo = (Map<String, Object>) stats.get("model");
        System.out.println("\nModel Information:");
        System.out.println("Model class: " + modelInfo.get("modelClass"));
        System.out.println("Model healthy: " + modelInfo.get("healthy"));
    }
    
    /**
     * Demonstrates basic categorization features.
     */
    private static void demonstrateBasicCategorization(
            LangChain4JContextualCategorizationEngine engine) {
        
        System.out.println("Testing various content types...\n");
        
        // Test cases with expected categories
        String[] testContents = {
            "My name is Sarah and I work as a software engineer in San Francisco",
            "The Great Wall of China is over 13,000 miles long",
            "I prefer Italian food over Chinese food, especially pasta",
            "Our company policy requires approval for expenses over $500",
            "I'm feeling anxious about the upcoming presentation",
            "My goal is to learn Spanish by the end of this year",
            "Contact me at sarah.jones@email.com or call 415-555-0123"
        };
        
        for (String content : testContents) {
            // Basic categorization
            CategoryLabel label = engine.categorize(content, null);
            
            // Extract tags
            Set<String> tags = engine.extractTags(content);
            
            // Get alternative suggestions
            List<CategoryLabel> alternatives = 
                engine.suggestAlternativeCategories(content, null, 3);
            
            System.out.println("Content: " + content);
            System.out.printf("Category: %s/%s (%.2f confidence)%n", 
                label.getPrimary(), 
                label.getSecondary() != null ? label.getSecondary() : "none",
                label.getConfidence());
            
            if (!tags.isEmpty()) {
                System.out.println("Tags: " + tags);
            }
            
            if (alternatives.size() > 1) {
                System.out.print("Alternatives: ");
                alternatives.stream().skip(1).limit(2)
                    .forEach(alt -> System.out.printf("%s(%.2f) ", 
                        alt.getPrimary(), alt.getConfidence()));
                System.out.println();
            }
            
            System.out.println();
        }
    }
    
    /**
     * Creates a mock chat model for demonstration purposes.
     * In real usage, you would use actual LangChain4j models.
     */
    private static ChatModel createMockChatModel() {
        return new ChatModel() {
            @Override
            public String chat(String userMessage) {
                // Simple mock responses based on content keywords
                if (userMessage.toLowerCase().contains("project") || 
                    userMessage.toLowerCase().contains("timeline")) {
                    return """
                        {
                          "primary": "ProjectManagement",
                          "secondary": "Planning",
                          "confidence": 0.85,
                          "reasoning": "Content relates to project planning"
                        }
                        """;
                } else if (userMessage.toLowerCase().contains("customer") || 
                          userMessage.toLowerCase().contains("support")) {
                    return """
                        {
                          "primary": "CustomerService",
                          "secondary": "Support",
                          "confidence": 0.80,
                          "reasoning": "Content relates to customer support"
                        }
                        """;
                } else {
                    return """
                        {
                          "primary": "Unknown",
                          "secondary": null,
                          "confidence": 0.60,
                          "reasoning": "Unable to categorize clearly"
                        }
                        """;
                }
            }

            @Override
            public ChatResponse chat(List<ChatMessage> messages) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'generate'");
            }
        };
    }
    
    /**
     * Example of integrating with different LangChain4j providers.
     */
    public static void exampleWithDifferentProviders() {
        System.out.println("\n=== Different Provider Examples ===");
        
        // Example configurations for different providers
        // (Actual API keys would be required to run these)
        
        System.out.println("Examples of different LangChain4j providers:");
        System.out.println("1. OpenAI GPT models");
        System.out.println("2. Azure OpenAI Service");
        System.out.println("3. Anthropic Claude");
        System.out.println("4. Google Vertex AI");
        System.out.println("5. Local models via Ollama");
        
        // OpenAI example
        System.out.println("\n// OpenAI Configuration:");
        System.out.println("""
            ChatModel openAiModel = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4")
                .temperature(0.2)
                .maxTokens(500)
                .build();
            """);
        
        // Azure OpenAI example
        System.out.println("// Azure OpenAI Configuration:");
        System.out.println("""
            ChatModel azureModel = AzureOpenAiChatModel.builder()
                .apiKey(System.getenv("AZURE_OPENAI_KEY"))
                .endpoint(System.getenv("AZURE_OPENAI_ENDPOINT"))
                .deploymentName("gpt-35-turbo")
                .build();
            """);
        
        System.out.println("\nAll providers can be used with the same categorization engine:");
        System.out.println("""
            LangChain4JContextualCategorizationEngine engine = 
                new LangChain4JContextualCategorizationEngine(chatModel);
            """);
    }
}