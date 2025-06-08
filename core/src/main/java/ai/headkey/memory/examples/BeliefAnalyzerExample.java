package ai.headkey.memory.examples;

import ai.headkey.memory.dto.*;
import ai.headkey.memory.factory.BeliefReinforcementConflictAnalyzerFactory;
import ai.headkey.memory.implementations.InMemoryBeliefReinforcementConflictAnalyzer;
import ai.headkey.memory.implementations.InMemoryBeliefStorageService;
import ai.headkey.memory.implementations.SimplePatternBeliefExtractionService;
import ai.headkey.memory.interfaces.BeliefReinforcementConflictAnalyzer;
import ai.headkey.memory.spi.BeliefExtractionService;
import ai.headkey.memory.spi.BeliefStorageService;

import java.time.Instant;
import java.util.*;

/**
 * Example demonstrating the new belief analyzer architecture.
 * 
 * This example shows how the refactored belief system separates concerns
 * between business logic, persistence, and content analysis. It demonstrates:
 * 
 * 1. Using the factory pattern to create different analyzer configurations
 * 2. Pluggable service providers for extraction and storage
 * 3. Custom implementations of service provider interfaces
 * 4. How the abstract base class provides consistent business logic
 * 5. Future extensibility for AI-powered implementations
 * 
 * Key benefits of the new architecture:
 * - Separation of concerns (SOLID principles)
 * - Testability through dependency injection
 * - Extensibility for different implementations
 * - Maintainability through clear abstractions
 * - Future-proofing for AI integration
 * 
 * @since 1.0
 */
public class BeliefAnalyzerExample {
    
    public static void main(String[] args) {
        System.out.println("=== Belief Analyzer Architecture Example ===\n");
        
        // Example 1: Simple factory usage
        demonstrateSimpleFactory();
        
        // Example 2: Custom service configuration
        demonstrateCustomServices();
        
        // Example 3: Different configurations
        demonstrateDifferentConfigurations();
        
        // Example 4: Service provider interfaces
        demonstrateServiceProviderPattern();
        
        // Example 5: Future AI integration (placeholder)
        demonstrateFutureAIIntegration();
        
        System.out.println("\n=== Example Complete ===");
    }
    
    /**
     * Demonstrates the simplest way to create and use a belief analyzer.
     */
    private static void demonstrateSimpleFactory() {
        System.out.println("1. Simple Factory Usage");
        System.out.println("-----------------------");
        
        // Create analyzer with default configuration
        BeliefReinforcementConflictAnalyzer analyzer = 
            BeliefReinforcementConflictAnalyzerFactory.createInMemory();
        
        // Create a sample memory record
        MemoryRecord memory = createSampleMemory("user-123", "I love pizza and hate spinach");
        
        // Analyze the memory
        BeliefUpdateResult result = analyzer.analyzeNewMemory(memory);
        
        System.out.println("Memory analyzed: " + memory.getContent());
        System.out.println("Beliefs created: " + result.getCreatedBeliefs().size());
        System.out.println("Beliefs reinforced: " + result.getReinforcedBeliefs().size());
        System.out.println("Conflicts detected: " + result.getDetectedConflicts().size());
        
        // Show created beliefs
        for (Belief belief : result.getCreatedBeliefs()) {
            System.out.println("  - " + belief.getStatement() + " (confidence: " + 
                             String.format("%.2f", belief.getConfidence()) + ")");
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates creating custom service implementations.
     */
    private static void demonstrateCustomServices() {
        System.out.println("2. Custom Service Configuration");
        System.out.println("-------------------------------");
        
        // Create custom services
        BeliefExtractionService customExtractor = new SimplePatternBeliefExtractionService();
        BeliefStorageService customStorage = new InMemoryBeliefStorageService();
        
        // Create analyzer with custom services
        BeliefReinforcementConflictAnalyzer analyzer = 
            BeliefReinforcementConflictAnalyzerFactory.create(customExtractor, customStorage);
        
        // Test with multiple memories
        String[] memories = {
            "My favorite color is blue",
            "I really enjoy reading books",
            "Pizza is my favorite food",
            "I don't like vegetables"
        };
        
        for (String content : memories) {
            MemoryRecord memory = createSampleMemory("user-456", content);
            BeliefUpdateResult result = analyzer.analyzeNewMemory(memory);
            
            System.out.println("Processed: " + content);
            for (Belief belief : result.getCreatedBeliefs()) {
                System.out.println("  → " + belief.getStatement());
            }
        }
        
        // Show agent statistics
        Map<String, Object> stats = analyzer.getAgentBeliefStatistics("user-456");
        System.out.println("\nAgent Statistics:");
        System.out.println("  Total beliefs: " + stats.get("totalBeliefs"));
        System.out.println("  Active beliefs: " + stats.get("activeBeliefs"));
        System.out.println("  Average confidence: " + 
                         String.format("%.2f", (Double) stats.get("averageConfidence")));
        
        System.out.println();
    }
    
    /**
     * Demonstrates different pre-configured setups.
     */
    private static void demonstrateDifferentConfigurations() {
        System.out.println("3. Different Configurations");
        System.out.println("---------------------------");
        
        // Development configuration
        BeliefReinforcementConflictAnalyzer devAnalyzer = 
            BeliefReinforcementConflictAnalyzerFactory.createForDevelopment();
        
        // Testing configuration
        BeliefReinforcementConflictAnalyzer testAnalyzer = 
            BeliefReinforcementConflictAnalyzerFactory.createForTesting();
        
        // Production configuration (same implementation for now, different config)
        BeliefReinforcementConflictAnalyzer prodAnalyzer = 
            BeliefReinforcementConflictAnalyzerFactory.createForProduction();
        
        System.out.println("Development analyzer healthy: " + devAnalyzer.isHealthy());
        System.out.println("Testing analyzer healthy: " + testAnalyzer.isHealthy());
        System.out.println("Production analyzer healthy: " + prodAnalyzer.isHealthy());
        
        // Show available configurations
        Map<String, Object> availableConfigs = 
            BeliefReinforcementConflictAnalyzerFactory.getAvailableConfigurations();
        
        System.out.println("\nAvailable configurations:");
        for (String configName : availableConfigs.keySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) availableConfigs.get(configName);
            System.out.println("  " + configName + ": " + config.get("description"));
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates the service provider pattern benefits.
     */
    private static void demonstrateServiceProviderPattern() {
        System.out.println("4. Service Provider Pattern");
        System.out.println("---------------------------");
        
        // Create analyzer using builder pattern
        BeliefReinforcementConflictAnalyzer analyzer = 
            BeliefReinforcementConflictAnalyzerFactory.builder()
                .withExtractionService(new SimplePatternBeliefExtractionService())
                .withStorageService(new InMemoryBeliefStorageService())
                .withResolutionStrategy("preference", "newer_wins")
                .withResolutionStrategy("fact", "higher_confidence")
                .withConfig("verboseLogging", true)
                .build();
        
        // Test conflict detection and resolution
        MemoryRecord memory1 = createSampleMemory("user-789", "I love chocolate ice cream");
        MemoryRecord memory2 = createSampleMemory("user-789", "I don't like chocolate ice cream");
        
        analyzer.analyzeNewMemory(memory1);
        BeliefUpdateResult result2 = analyzer.analyzeNewMemory(memory2);
        
        System.out.println("Analyzed conflicting preferences:");
        System.out.println("  Memory 1: " + memory1.getContent());
        System.out.println("  Memory 2: " + memory2.getContent());
        System.out.println("  Conflicts detected: " + result2.getDetectedConflicts().size());
        
        // Show unresolved conflicts
        List<BeliefConflict> conflicts = analyzer.getUnresolvedConflicts("user-789");
        for (BeliefConflict conflict : conflicts) {
            System.out.println("  Conflict: " + conflict.getId() + " - " + conflict.getResolutionStrategy());
        }
        
        // Get service information
        if (analyzer instanceof InMemoryBeliefReinforcementConflictAnalyzer) {
            InMemoryBeliefReinforcementConflictAnalyzer impl = 
                (InMemoryBeliefReinforcementConflictAnalyzer) analyzer;
            Map<String, Object> implInfo = impl.getImplementationInfo();
            
            System.out.println("\nImplementation info:");
            System.out.println("  Type: " + implInfo.get("implementationType"));
            System.out.println("  Version: " + implInfo.get("version"));
            System.out.println("  Current belief ID counter: " + implInfo.get("currentBeliefIdCounter"));
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates how future AI implementations would integrate.
     */
    private static void demonstrateFutureAIIntegration() {
        System.out.println("5. Future AI Integration");
        System.out.println("------------------------");
        
        try {
            // This will throw an exception since AI implementation isn't ready
            @SuppressWarnings("unused")
            BeliefReinforcementConflictAnalyzer aiAnalyzer = 
                BeliefReinforcementConflictAnalyzerFactory.createAIPowered();
        } catch (UnsupportedOperationException e) {
            System.out.println("AI-powered implementation not yet available:");
            System.out.println("  " + e.getMessage());
        }
        
        System.out.println("\nWhen AI implementation is ready, it would work like this:");
        System.out.println("```java");
        System.out.println("// Future AI-powered implementation");
        System.out.println("BeliefReinforcementConflictAnalyzer aiAnalyzer = ");
        System.out.println("    BeliefReinforcementConflictAnalyzerFactory.builder()");
        System.out.println("        .withExtractionService(new LangChain4JBeliefExtractionService())");
        System.out.println("        .withStorageService(new VectorBeliefStorageService())");
        System.out.println("        .withConfig(\"aiModel\", \"gpt-4\")");
        System.out.println("        .withConfig(\"semanticSimilarity\", true)");
        System.out.println("        .build();");
        System.out.println("```");
        
        System.out.println("\nBenefits of AI implementation:");
        System.out.println("  - Semantic understanding of beliefs");
        System.out.println("  - Context-aware extraction");
        System.out.println("  - Multi-language support");
        System.out.println("  - Advanced conflict detection");
        System.out.println("  - Improved confidence scoring");
        
        System.out.println();
    }
    
    /**
     * Helper method to create sample memory records.
     */
    private static MemoryRecord createSampleMemory(String agentId, String content) {
        MemoryRecord memory = new MemoryRecord();
        memory.setId("memory-" + System.nanoTime());
        memory.setAgentId(agentId);
        memory.setContent(content);
        memory.setCreatedAt(Instant.now());
        
        // Set category
        CategoryLabel category = new CategoryLabel();
        category.setPrimary("conversation");
        category.setConfidence(0.8);
        memory.setCategory(category);
        
        // Set metadata
        Metadata metadata = new Metadata();
        metadata.setSource("example");
        metadata.setImportance(0.7);
        memory.setMetadata(metadata);
        
        return memory;
    }
    
    /**
     * Demonstrates the architecture benefits.
     */
    @SuppressWarnings("unused")
    private static void demonstrateArchitectureBenefits() {
        System.out.println("Architecture Benefits");
        System.out.println("--------------------");
        
        System.out.println("1. Separation of Concerns:");
        System.out.println("   - Business logic in AbstractBeliefReinforcementConflictAnalyzer");
        System.out.println("   - Content analysis in BeliefExtractionService implementations");
        System.out.println("   - Persistence in BeliefStorageService implementations");
        
        System.out.println("\n2. Extensibility:");
        System.out.println("   - Easy to add new extraction algorithms (AI, NLP, rules)");
        System.out.println("   - Support for different storage backends (memory, DB, cloud)");
        System.out.println("   - Pluggable conflict resolution strategies");
        
        System.out.println("\n3. Testability:");
        System.out.println("   - Mock services for unit testing");
        System.out.println("   - Dependency injection support");
        System.out.println("   - Isolated testing of components");
        
        System.out.println("\n4. Maintainability:");
        System.out.println("   - Clear interfaces and contracts");
        System.out.println("   - Single responsibility principle");
        System.out.println("   - Easy to modify individual components");
        
        System.out.println("\n5. Performance:");
        System.out.println("   - Optimized storage implementations");
        System.out.println("   - Caching strategies in services");
        System.out.println("   - Efficient batch processing");
    }
}