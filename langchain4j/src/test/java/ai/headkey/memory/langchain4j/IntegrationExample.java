package ai.headkey.memory.langchain4j;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.memory.interfaces.ContextualCategorizationEngine;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.*;

/**
 * Complete integration example showing LangChain4j ContextualCategorizationEngine usage.
 * 
 * This example demonstrates a realistic scenario where the categorization engine
 * is used to process various types of user content in a knowledge management system.
 * 
 * Features demonstrated:
 * - Content categorization with metadata
 * - Semantic tag extraction
 * - Batch processing for efficiency
 * - Custom categories for domain-specific needs
 * - Performance monitoring and health checks
 * - Error handling and fallback strategies
 */
public class IntegrationExample {
    
    private final ContextualCategorizationEngine categorizationEngine;
    
    public IntegrationExample(ChatLanguageModel chatModel) {
        // Create engine using factory with custom configuration
        this.categorizationEngine = LangChain4JComponentFactory.builder()
                .withChatModel(chatModel)
                .withConfidenceThreshold(0.75)
                .withCustomCategories(Set.of(
                    "ProductFeedback", "CustomerSupport", "TechnicalDocumentation", 
                    "MarketingContent", "CompliancePolicy"
                ))
                .build();
        
        // Configure custom subcategories
        LangChain4JContextualCategorizationEngine engine = 
            (LangChain4JContextualCategorizationEngine) categorizationEngine;
        engine.addCustomCategory("ProductFeedback", 
            Set.of("Feature Request", "Bug Report", "User Experience", "Performance"));
        engine.addCustomCategory("CustomerSupport", 
            Set.of("Technical Issue", "Billing Question", "Account Management", "General Inquiry"));
    }
    
    /**
     * Demonstrates processing various types of content that might come into
     * a knowledge management or customer service system.
     */
    public void demonstrateContentProcessing() {
        System.out.println("=== LangChain4j ContextualCategorizationEngine Integration Example ===\n");
        
        // Check engine health before processing
        if (!categorizationEngine.isHealthy()) {
            System.err.println("WARNING: Categorization engine is not healthy!");
            return;
        }
        
        System.out.println("Engine is healthy. Processing content...\n");
        
        // Process individual content items
        processIndividualContent();
        
        // Demonstrate batch processing
        processBatchContent();
        
        // Show performance statistics
        showPerformanceStatistics();
        
        // Demonstrate error handling
        demonstrateErrorHandling();
    }
    
    /**
     * Process individual content items with detailed analysis.
     */
    private void processIndividualContent() {
        System.out.println("--- Individual Content Processing ---\n");
        
        ContentItem[] items = {
            new ContentItem(
                "User feedback about slow loading times",
                "The app takes forever to load my dashboard. It's been like this for weeks.",
                createMetadata("user123", "mobile_app", "feedback")
            ),
            new ContentItem(
                "Employee technical question",
                "How do I configure SSL certificates for our microservices deployment?",
                createMetadata("employee456", "internal_docs", "question")
            ),
            new ContentItem(
                "Customer billing inquiry",
                "I was charged twice for my subscription this month. Can you help me understand why?",
                createMetadata("customer789", "billing_system", "inquiry")
            ),
            new ContentItem(
                "Product feature request",
                "It would be great if we could export reports to Excel format directly from the dashboard.",
                createMetadata("user321", "web_app", "feature_request")
            ),
            new ContentItem(
                "Personal information update",
                "My contact information has changed. New email is john.doe@newcompany.com and phone is 555-987-6543.",
                createMetadata("user654", "profile_system", "update")
            )
        };
        
        for (ContentItem item : items) {
            processContentItem(item);
        }
    }
    
    /**
     * Process a single content item and display detailed results.
     */
    private void processContentItem(ContentItem item) {
        System.out.println("Processing: " + item.title);
        System.out.println("Content: " + item.content);
        
        try {
            // Categorize the content
            CategoryLabel category = categorizationEngine.categorize(item.content, item.metadata);
            
            // Extract semantic tags
            Set<String> tags = categorizationEngine.extractTags(item.content);
            
            // Get alternative categorization suggestions
            List<CategoryLabel> alternatives = categorizationEngine.suggestAlternativeCategories(
                item.content, item.metadata, 3);
            
            // Display results
            System.out.printf("✓ Category: %s/%s (%.1f%% confidence)%n", 
                category.getPrimary(), 
                category.getSecondary() != null ? category.getSecondary() : "none",
                category.getConfidence() * 100);
            
            if (!tags.isEmpty()) {
                System.out.println("✓ Tags: " + String.join(", ", tags));
            }
            
            if (alternatives.size() > 1) {
                System.out.print("✓ Alternatives: ");
                alternatives.stream()
                    .skip(1)
                    .limit(2)
                    .forEach(alt -> System.out.printf("%s(%.1f%%) ", 
                        alt.getPrimary(), alt.getConfidence() * 100));
                System.out.println();
            }
            
            // Suggest action based on category and confidence
            suggestAction(category, item.metadata);
            
        } catch (Exception e) {
            System.err.println("✗ Error processing content: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrate batch processing for efficiency.
     */
    private void processBatchContent() {
        System.out.println("--- Batch Content Processing ---\n");
        
        Map<String, String> batchContent = new HashMap<>();
        batchContent.put("ticket001", "The software crashed when I tried to export the monthly report");
        batchContent.put("ticket002", "Can you update our company's billing address in the system?");
        batchContent.put("ticket003", "I love the new dark mode feature! Makes it easier to work at night");
        batchContent.put("ticket004", "What are the data retention policies for customer information?");
        batchContent.put("ticket005", "The mobile app keeps logging me out every few minutes");
        
        Metadata commonMetadata = createMetadata("batch_processor", "support_system", "batch");
        
        long startTime = System.currentTimeMillis();
        Map<String, CategoryLabel> results = categorizationEngine.categorizeBatch(batchContent, commonMetadata);
        long processingTime = System.currentTimeMillis() - startTime;
        
        System.out.printf("Processed %d items in %dms (%.1fms per item)%n%n", 
            results.size(), processingTime, (double) processingTime / results.size());
        
        // Display batch results in a table format
        System.out.println("Ticket ID | Category          | Subcategory      | Confidence");
        System.out.println("----------|-------------------|------------------|------------");
        
        results.forEach((id, label) -> {
            System.out.printf("%-9s | %-17s | %-16s | %.1f%%%n",
                id,
                truncate(label.getPrimary(), 17),
                truncate(label.getSecondary() != null ? label.getSecondary() : "none", 16),
                label.getConfidence() * 100);
        });
        
        System.out.println();
    }
    
    /**
     * Display comprehensive performance statistics.
     */
    private void showPerformanceStatistics() {
        System.out.println("--- Performance Statistics ---\n");
        
        Map<String, Object> stats = categorizationEngine.getCategorizationStatistics();
        
        System.out.println("Overall Statistics:");
        System.out.println("• Total categorizations: " + stats.get("totalCategorizations"));
        System.out.println("• Total tag extractions: " + stats.get("totalTagExtractions"));
        System.out.println("• Total batch operations: " + stats.get("totalBatchCategorizations"));
        System.out.println("• Engine uptime: " + stats.get("uptimeSeconds") + " seconds");
        System.out.println("• Confidence threshold: " + stats.get("confidenceThreshold"));
        
        // Category distribution
        @SuppressWarnings("unchecked")
        Map<String, Object> categoryDistribution = 
            (Map<String, Object>) stats.get("categoryDistribution");
        
        if (categoryDistribution != null && !categoryDistribution.isEmpty()) {
            System.out.println("\nCategory Distribution:");
            categoryDistribution.entrySet().stream()
                .sorted((e1, e2) -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> stats1 = (Map<String, Object>) e1.getValue();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> stats2 = (Map<String, Object>) e2.getValue();
                    return Long.compare((Long) stats2.get("count"), (Long) stats1.get("count"));
                })
                .forEach(entry -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> catStats = (Map<String, Object>) entry.getValue();
                    System.out.printf("• %-20s: %2d uses (%.1f%%, avg confidence: %.1f%%)%n",
                        entry.getKey(),
                        catStats.get("count"),
                        catStats.get("percentage"),
                        (Double) catStats.get("averageConfidence") * 100);
                });
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrate error handling and fallback behavior.
     */
    private void demonstrateErrorHandling() {
        System.out.println("--- Error Handling Examples ---\n");
        
        // Test with invalid content
        try {
            categorizationEngine.categorize("", null);
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Correctly handled empty content: " + e.getMessage());
        }
        
        try {
            categorizationEngine.extractTags(null);
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Correctly handled null content: " + e.getMessage());
        }
        
        // Test with problematic content that might cause AI service issues
        String problematicContent = "This content has weird characters: 🤖🚀💡 and symbols: @#$%^&*()";
        try {
            CategoryLabel result = categorizationEngine.categorize(problematicContent, null);
            System.out.printf("✓ Handled problematic content: %s (%.1f%% confidence)%n",
                result.getPrimary(), result.getConfidence() * 100);
        } catch (Exception e) {
            System.out.println("✗ Failed to handle problematic content: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Suggest appropriate action based on categorization results.
     */
    private void suggestAction(CategoryLabel category, Metadata metadata) {
        String action = switch (category.getPrimary()) {
            case "ProductFeedback" -> "→ Forward to product team for review";
            case "CustomerSupport" -> "→ Route to appropriate support specialist";
            case "TechnicalDocumentation" -> "→ Add to knowledge base for future reference";
            case "PersonalData" -> "→ Process with enhanced privacy protections";
            case "BusinessRule" -> "→ Review against current policies";
            default -> category.getConfidence() > 0.8 ? 
                "→ Process with standard workflow" : 
                "→ Flag for manual review (low confidence)";
        };
        
        System.out.println("✓ Action: " + action);
    }
    
    /**
     * Create metadata object with common fields.
     */
    private Metadata createMetadata(String userId, String source, String type) {
        Metadata metadata = new Metadata();
        metadata.put("userId", userId);
        metadata.put("source", source);
        metadata.put("type", type);
        metadata.put("timestamp", System.currentTimeMillis());
        metadata.put("priority", "normal");
        return metadata;
    }
    
    /**
     * Utility method to truncate strings for table display.
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return "null";
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }
    
    /**
     * Simple data class for content items.
     */
    private static class ContentItem {
        final String title;
        final String content;
        final Metadata metadata;
        
        ContentItem(String title, String content, Metadata metadata) {
            this.title = title;
            this.content = content;
            this.metadata = metadata;
        }
    }
    
    /**
     * Main method for running the integration example.
     * Uses a mock chat model for demonstration purposes.
     */
    public static void main(String[] args) {
        // Create a mock chat model for demonstration
        ChatLanguageModel mockModel = createDemoChatModel();
        
        // Run the integration example
        IntegrationExample example = new IntegrationExample(mockModel);
        example.demonstrateContentProcessing();
        
        System.out.println("=== Integration Example Complete ===");
        System.out.println("\nTo use with a real LLM provider:");
        System.out.println("1. Add your API key to environment variables");
        System.out.println("2. Replace mockModel with actual provider:");
        System.out.println("   ChatLanguageModel model = OpenAiChatModel.builder()");
        System.out.println("       .apiKey(System.getenv(\"OPENAI_API_KEY\"))");
        System.out.println("       .modelName(\"gpt-3.5-turbo\")");
        System.out.println("       .build();");
    }
    
    /**
     * Creates a demo chat model that provides realistic responses
     * for demonstration purposes without requiring an actual LLM API.
     */
    private static ChatLanguageModel createDemoChatModel() {
        return new ChatLanguageModel() {
            private final Random random = new Random(42); // Fixed seed for consistent results
            
            @Override
            public String generate(String userMessage) {
                // Analyze the message content to provide realistic categorization responses
                String content = userMessage.toLowerCase();
                
                if (content.contains("slow") || content.contains("crash") || content.contains("bug")) {
                    return """
                        {
                          "primary": "ProductFeedback",
                          "secondary": "Bug Report",
                          "confidence": 0.87,
                          "reasoning": "Content describes technical issues and problems"
                        }
                        """;
                } else if (content.contains("billing") || content.contains("charged") || content.contains("subscription")) {
                    return """
                        {
                          "primary": "CustomerSupport",
                          "secondary": "Billing Question",
                          "confidence": 0.92,
                          "reasoning": "Content relates to billing and payment issues"
                        }
                        """;
                } else if (content.contains("feature") || content.contains("request") || content.contains("would be great")) {
                    return """
                        {
                          "primary": "ProductFeedback",
                          "secondary": "Feature Request",
                          "confidence": 0.85,
                          "reasoning": "Content suggests new features or improvements"
                        }
                        """;
                } else if (content.contains("email") || content.contains("phone") || content.contains("contact")) {
                    return """
                        {
                          "primary": "PersonalData",
                          "secondary": "Identity",
                          "confidence": 0.89,
                          "reasoning": "Content contains personal contact information"
                        }
                        """;
                } else if (content.contains("configure") || content.contains("ssl") || content.contains("deployment")) {
                    return """
                        {
                          "primary": "TechnicalDocumentation",
                          "secondary": "Configuration",
                          "confidence": 0.83,
                          "reasoning": "Content relates to technical configuration and setup"
                        }
                        """;
                } else if (content.contains("love") || content.contains("great") || content.contains("easier")) {
                    return """
                        {
                          "primary": "ProductFeedback",
                          "secondary": "User Experience",
                          "confidence": 0.79,
                          "reasoning": "Content expresses positive sentiment about features"
                        }
                        """;
                } else if (content.contains("policy") || content.contains("retention") || content.contains("compliance")) {
                    return """
                        {
                          "primary": "CompliancePolicy",
                          "secondary": "Data Governance",
                          "confidence": 0.91,
                          "reasoning": "Content relates to policies and compliance requirements"
                        }
                        """;
                } else {
                    // Default response for unrecognized content
                    double confidence = 0.6 + random.nextDouble() * 0.2; // Random confidence between 0.6-0.8
                    return String.format("""
                        {
                          "primary": "Unknown",
                          "secondary": null,
                          "confidence": %.2f,
                          "reasoning": "Content does not clearly match any specific category"
                        }
                        """, confidence);
                }
            }
        };
    }
}