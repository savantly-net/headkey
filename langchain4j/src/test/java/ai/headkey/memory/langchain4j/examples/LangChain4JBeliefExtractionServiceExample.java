package ai.headkey.memory.langchain4j.examples;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.memory.interfaces.BeliefExtractionService;
import ai.headkey.memory.interfaces.BeliefExtractionService.ExtractedBelief;
import ai.headkey.memory.interfaces.BeliefExtractionService.ExtractionContext;
import ai.headkey.memory.langchain4j.LangChain4JBeliefExtractionService;
import ai.headkey.memory.langchain4j.dto.*;
import ai.headkey.memory.langchain4j.services.ai.*;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Example demonstrating the usage of LangChain4JBeliefExtractionService.
 * 
 * This example shows how to:
 * 1. Create a LangChain4JBeliefExtractionService with a ChatModel
 * 2. Create a service with custom AI service implementations
 * 3. Extract beliefs from memory content
 * 4. Calculate similarity between belief statements
 * 5. Detect conflicts between beliefs
 * 6. Extract categories and calculate confidence scores
 * 
 * Note: This example uses mock implementations for demonstration.
 * In a real application, you would use proper AI models like OpenAI GPT.
 */
public class LangChain4JBeliefExtractionServiceExample {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("LangChain4J Belief Extraction Service Example");
        System.out.println("=".repeat(80));
        
        // Example 1: Basic service creation and usage
        demonstrateBasicUsage();
        
        // Example 2: Custom AI service creation
        demonstrateCustomAiServiceUsage();
        
        // Example 3: Advanced belief extraction scenarios
        demonstrateAdvancedExtraction();
        
        // Example 4: Similarity calculation and conflict detection
        demonstrateSimilarityAndConflicts();
        
        // Example 5: Service health and information
        demonstrateServiceInfo();
        
        // Example 6: Integration patterns
        demonstrateIntegrationPatterns();
    }

    /**
     * Demonstrates basic service creation and belief extraction.
     */
    private static void demonstrateBasicUsage() {
        System.out.println("\n1. Basic Usage Example");
        System.out.println("-".repeat(40));
        
        try {
            // Create a mock ChatModel for demonstration
            ChatModel mockChatModel = createMockChatModel();
            
            // Create the belief extraction service
            LangChain4JBeliefExtractionService service = 
                new LangChain4JBeliefExtractionService(mockChatModel);
            
            System.out.println("✓ Service created successfully");
            
            // Example memory content
            String content = "I love programming in Java and I really enjoy working with AI systems. " +
                           "My favorite programming language is definitely Java, and I dislike debugging complex code.";
            String agentId = "user123";
            CategoryLabel category = new CategoryLabel("UserProfile", "Preferences", Set.of("programming"), 0.9);
            
            // Extract beliefs from the content
            System.out.println("\nExtracting beliefs from content:");
            System.out.println("Content: " + content);
            
            try {
                List<ExtractedBelief> beliefs = service.extractBeliefs(content, agentId, category);
                System.out.println("✓ Belief extraction completed (may use fallback due to mock model)");
                System.out.println("Number of beliefs extracted: " + beliefs.size());
            } catch (Exception e) {
                System.out.println("⚠ Belief extraction failed (expected with mock model): " + e.getClass().getSimpleName());
                System.out.println("  This is normal - real AI models would provide actual extraction");
            }
            
        } catch (Exception e) {
            System.out.println("✗ Error in basic usage: " + e.getMessage());
        }
    }

    /**
     * Demonstrates custom AI service creation and usage.
     */
    private static void demonstrateCustomAiServiceUsage() {
        System.out.println("\n2. Custom AI Service Usage Example");
        System.out.println("-".repeat(40));
        
        try {
            // Create mock AI services
            LangChain4jBeliefExtractionAiService beliefService = createMockBeliefExtractionService();
            LangChain4jSimilarityAiService similarityService = createMockSimilarityService();
            LangChain4jConflictDetectionAiService conflictService = createMockConflictService();
            
            // Create service with custom AI services
            LangChain4JBeliefExtractionService service = new LangChain4JBeliefExtractionService(
                beliefService, similarityService, conflictService, "CustomAI-BeliefExtractor");
            
            System.out.println("✓ Service created with custom AI services");
            System.out.println("Service name: " + service.getServiceName());
            
            // Test basic functionality
            String content = "I absolutely love machine learning and AI development!";
            try {
                List<ExtractedBelief> beliefs = service.extractBeliefs(content, "dev-user", null);
                System.out.println("✓ Custom service extraction completed");
            } catch (Exception e) {
                System.out.println("⚠ Expected exception with mock services: " + e.getClass().getSimpleName());
            }
            
        } catch (Exception e) {
            System.out.println("✗ Error in custom service usage: " + e.getMessage());
        }
    }

    /**
     * Demonstrates advanced belief extraction scenarios.
     */
    private static void demonstrateAdvancedExtraction() {
        System.out.println("\n3. Advanced Extraction Scenarios");
        System.out.println("-".repeat(40));
        
        ChatModel mockChatModel = createMockChatModel();
        LangChain4JBeliefExtractionService service = 
            new LangChain4JBeliefExtractionService(mockChatModel);
        
        // Scenario 1: Complex preferences
        System.out.println("\nScenario 1: Complex preferences");
        String complexContent = "I absolutely love Italian cuisine, especially pasta dishes. " +
                              "However, I'm not a big fan of spicy food. I prefer mild flavors " +
                              "and I definitely enjoy cooking at home on weekends.";
        
        testExtractionScenario(service, complexContent, "user456", "preferences");
        
        // Scenario 2: Factual information
        System.out.println("\nScenario 2: Factual statements");
        String factualContent = "John was born in 1985 in New York. He graduated from MIT in 2007 " +
                              "and currently works as a software engineer at Google.";
        
        testExtractionScenario(service, factualContent, "profile789", "facts");
        
        // Scenario 3: Relationships
        System.out.println("\nScenario 3: Relationship information");
        String relationshipContent = "Sarah is married to Michael. They have two children together. " +
                                   "Sarah's best friend is Emma, and they've known each other since college.";
        
        testExtractionScenario(service, relationshipContent, "social123", "relationships");
    }

    /**
     * Demonstrates similarity calculation and conflict detection.
     */
    private static void demonstrateSimilarityAndConflicts() {
        System.out.println("\n4. Similarity and Conflict Detection");
        System.out.println("-".repeat(40));
        
        ChatModel mockChatModel = createMockChatModel();
        LangChain4JBeliefExtractionService service = 
            new LangChain4JBeliefExtractionService(mockChatModel);
        
        // Test similarity calculations
        System.out.println("\nSimilarity calculations:");
        
        String[][] similarityPairs = {
            {"I love coffee", "I enjoy coffee"},
            {"John lives in Paris", "John resides in Paris"},
            {"She is a doctor", "She works as a physician"},
            {"I hate vegetables", "I love vegetables"},
            {"The weather is nice", "Programming is fun"}
        };
        
        for (String[] pair : similarityPairs) {
            double similarity = service.calculateSimilarity(pair[0], pair[1]);
            System.out.printf("  '%s' vs '%s' → %.2f%n", pair[0], pair[1], similarity);
        }
        
        // Test conflict detection
        System.out.println("\nConflict detection:");
        
        String[][] conflictPairs = {
            {"I like pizza", "I don't like pizza"},
            {"He is 25 years old", "He is 30 years old"},
            {"She lives in London", "She lives in Paris"},
            {"I prefer tea", "I prefer coffee"},
            {"The sky is blue", "Today is sunny"}
        };
        
        for (String[] pair : conflictPairs) {
            boolean conflict = service.areConflicting(pair[0], pair[1], "general", "general");
            System.out.printf("  '%s' vs '%s' → %s%n", 
                pair[0], pair[1], conflict ? "CONFLICT" : "NO CONFLICT");
        }
    }

    /**
     * Demonstrates service health and information retrieval.
     */
    private static void demonstrateServiceInfo() {
        System.out.println("\n5. Service Health and Information");
        System.out.println("-".repeat(40));
        
        ChatModel mockChatModel = createMockChatModel();
        LangChain4JBeliefExtractionService service = 
            new LangChain4JBeliefExtractionService(mockChatModel);
        
        // Check service health
        boolean healthy = service.isHealthy();
        System.out.println("Service health status: " + (healthy ? "HEALTHY" : "UNHEALTHY"));
        System.out.println("(Mock service expected to be unhealthy)");
        
        // Get service information
        System.out.println("\nService information:");
        Map<String, Object> info = service.getServiceInfo();
        info.forEach((key, value) -> {
            System.out.printf("  %s: %s%n", key, value);
        });
    }

    /**
     * Demonstrates integration patterns and best practices.
     */
    private static void demonstrateIntegrationPatterns() {
        System.out.println("\n6. Integration Patterns");
        System.out.println("-".repeat(40));
        
        System.out.println("\nRecommended integration patterns:");
        
        System.out.println("\n// Pattern 1: Service creation with ChatModel dependency injection");
        System.out.println("@Inject");
        System.out.println("private ChatModel chatModel;");
        System.out.println("");
        System.out.println("@Produces");
        System.out.println("public BeliefExtractionService createBeliefExtractor() {");
        System.out.println("    return new LangChain4JBeliefExtractionService(chatModel);");
        System.out.println("}");
        
        System.out.println("\n// Pattern 2: Service creation with custom AI services");
        System.out.println("@Inject");
        System.out.println("private LangChain4jBeliefExtractionAiService beliefService;");
        System.out.println("@Inject");
        System.out.println("private LangChain4jSimilarityAiService similarityService;");
        System.out.println("@Inject");
        System.out.println("private LangChain4jConflictDetectionAiService conflictService;");
        System.out.println("");
        System.out.println("@Produces");
        System.out.println("public BeliefExtractionService createCustomBeliefExtractor() {");
        System.out.println("    return new LangChain4JBeliefExtractionService(");
        System.out.println("        beliefService, similarityService, conflictService);");
        System.out.println("}");
        
        System.out.println("\n// Pattern 3: Error handling and fallback");
        System.out.println("try {");
        System.out.println("    List<ExtractedBelief> beliefs = service.extractBeliefs(content, agentId, category);");
        System.out.println("    // Process beliefs...");
        System.out.println("} catch (BeliefExtractionException e) {");
        System.out.println("    // Log error and use fallback service");
        System.out.println("    logger.warn(\"AI extraction failed, using simple extractor\", e);");
        System.out.println("    beliefs = fallbackService.extractBeliefs(content, agentId, category);");
        System.out.println("}");
        
        System.out.println("\n// Pattern 4: Configuration for different environments");
        System.out.println("// Development: Use mock or cheaper models");
        System.out.println("// Production: Use production-grade models with proper error handling");
        System.out.println("// Testing: Use deterministic mock responses");
        
        System.out.println("\n// Pattern 5: Monitoring and metrics");
        System.out.println("// - Track extraction success rates");
        System.out.println("// - Monitor API usage and costs");
        System.out.println("// - Alert on service health issues");
        System.out.println("// - Measure extraction quality and confidence");
    }

    /**
     * Helper method to test belief extraction scenarios.
     */
    private static void testExtractionScenario(BeliefExtractionService service, 
                                             String content, String agentId, String scenario) {
        System.out.printf("Testing %s extraction:%n", scenario);
        System.out.printf("Content: %s%n", content);
        
        try {
            List<ExtractedBelief> beliefs = service.extractBeliefs(content, agentId, null);
            System.out.printf("✓ Extracted %d beliefs%n", beliefs.size());
            
            for (ExtractedBelief belief : beliefs) {
                System.out.printf("  - %s (confidence: %.2f, category: %s)%n", 
                    belief.getStatement(), belief.getConfidence(), belief.getCategory());
            }
        } catch (Exception e) {
            System.out.printf("⚠ Extraction failed: %s%n", e.getClass().getSimpleName());
        }
    }

    /**
     * Creates a mock ChatModel for demonstration purposes.
     * In a real application, this would be replaced with an actual AI model.
     */
    private static ChatModel createMockChatModel() {
        return new ChatModel() {
            @Override
            public ChatResponse chat(List<ChatMessage> messages) {
                // Mock implementation - always throws exception
                throw new RuntimeException("Mock ChatModel - not implemented");
            }
        };
    }

    /**
     * Creates a mock belief extraction AI service for demonstration.
     */
    private static LangChain4jBeliefExtractionAiService createMockBeliefExtractionService() {
        return new LangChain4jBeliefExtractionAiService() {
            @Override
            public BeliefExtractionResponse extractBeliefs(String content, String agentId, String categoryContext) {
                throw new RuntimeException("Mock BeliefExtractionAiService - not implemented");
            }

            @Override
            public CategoryExtractionResponse extractCategory(String statement) {
                throw new RuntimeException("Mock CategoryExtraction - not implemented");
            }

            @Override
            public ConfidenceResponse calculateConfidence(String content, String statement, String context) {
                throw new RuntimeException("Mock ConfidenceCalculation - not implemented");
            }
        };
    }

    /**
     * Creates a mock similarity AI service for demonstration.
     */
    private static LangChain4jSimilarityAiService createMockSimilarityService() {
        return new LangChain4jSimilarityAiService() {
            @Override
            public SimilarityResponse calculateSimilarity(String statement1, String statement2) {
                throw new RuntimeException("Mock SimilarityAiService - not implemented");
            }
        };
    }

    /**
     * Creates a mock conflict detection AI service for demonstration.
     */
    private static LangChain4jConflictDetectionAiService createMockConflictService() {
        return new LangChain4jConflictDetectionAiService() {
            @Override
            public ConflictDetectionResponse detectConflict(String statement1, String statement2, String categories) {
                throw new RuntimeException("Mock ConflictDetectionAiService - not implemented");
            }
        };
    }

    /**
     * Example of creating an ExtractionContext for advanced scenarios.
     */
    private static ExtractionContext createExampleContext() {
        // Create a sample memory record
        Metadata metadata = new Metadata();
        metadata.setSource("user_input");
        metadata.setImportance(0.8);
        
        MemoryRecord memoryRecord = new MemoryRecord(
            "memory-123",
            "user-456",
            "Sample memory content",
            new CategoryLabel("UserProfile", "Personal", Set.of("preferences"), 0.9),
            metadata,
            Instant.now()
        );
        
        // Create extraction context
        ExtractionContext context = new ExtractionContext(memoryRecord);
        context.addExistingBelief("User likes programming");
        context.addExistingBelief("User prefers Java over Python");
        context.addContext("session_id", "session-789");
        context.addContext("extraction_mode", "detailed");
        
        return context;
    }

    /**
     * Demonstrates confidence calculation with context.
     */
    private static void demonstrateConfidenceCalculation() {
        System.out.println("\n7. Confidence Calculation with Context");
        System.out.println("-".repeat(40));
        
        ChatModel mockChatModel = createMockChatModel();
        LangChain4JBeliefExtractionService service = 
            new LangChain4JBeliefExtractionService(mockChatModel);
        
        ExtractionContext context = createExampleContext();
        
        String content = "I absolutely love working with Java frameworks, especially Spring Boot!";
        String statement = "User loves Java frameworks";
        
        double confidence = service.calculateConfidence(content, statement, context);
        System.out.printf("Content: %s%n", content);
        System.out.printf("Statement: %s%n", statement);
        System.out.printf("Confidence: %.2f%n", confidence);
        System.out.printf("(With mock model, returns default confidence of 0.5)%n");
    }
}