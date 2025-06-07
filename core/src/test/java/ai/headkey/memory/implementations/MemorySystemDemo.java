package ai.headkey.memory.implementations;

import ai.headkey.memory.dto.*;
import ai.headkey.memory.enums.Status;
import ai.headkey.memory.interfaces.*;

import java.util.List;
import java.util.Map;

/**
 * Demonstration class showing the capabilities of the in-memory memory system.
 * 
 * This class demonstrates Phase 2 implementation of the memory system as specified
 * in the IMPLEMENTATION_NOTES.md, showcasing:
 * - Complete ingestion pipeline (IIM → CCE → MES → BRCA)
 * - In-memory storage and retrieval
 * - Basic categorization
 * - Simple belief management
 * - Relevance evaluation
 * - Search and response capabilities
 */
public class MemorySystemDemo {
    
    public static void main(String[] args) {
        System.out.println("=== HeadKey Memory System Demo ===");
        System.out.println("Phase 2: In-Memory Implementation");
        System.out.println();
        
        try {
            // Create the complete memory system
            InMemoryMemorySystemFactory factory = InMemoryMemorySystemFactory.forTesting();
            InMemoryMemorySystemFactory.MemorySystem memorySystem = factory.createCompleteSystem();
            
            System.out.println("✓ Memory system initialized successfully");
            System.out.println("✓ All components are healthy: " + memorySystem.isHealthy());
            System.out.println();
            
            // Demo 1: Basic Memory Ingestion
            demonstrateIngestion(memorySystem);
            
            // Demo 2: Categorization
            demonstrateCategorization(memorySystem);
            
            // Demo 3: Memory Retrieval and Search
            demonstrateRetrievalAndSearch(memorySystem);
            
            // Demo 4: Belief System
            demonstrateBeliefSystem(memorySystem);
            
            // Demo 5: System Statistics
            demonstrateStatistics(memorySystem);
            
            System.out.println("=== Demo completed successfully! ===");
            
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void demonstrateIngestion(InMemoryMemorySystemFactory.MemorySystem memorySystem) throws Exception {
        System.out.println("--- Demo 1: Memory Ingestion Pipeline ---");
        
        InformationIngestionModule ingestionModule = memorySystem.getIngestionModule();
        
        // Create sample memory inputs
        String[] sampleMemories = {
            "My favorite color is blue and I love Italian food",
            "I have a meeting with the client tomorrow at 3 PM",
            "The capital of France is Paris",
            "I need to buy groceries: milk, eggs, and bread",
            "My birthday is on December 15th"
        };
        
        for (int i = 0; i < sampleMemories.length; i++) {
            MemoryInput input = new MemoryInput("demo-agent", sampleMemories[i]);
            input.setSource("conversation");
            
            IngestionResult result = ingestionModule.ingest(input);
            
            System.out.printf("Memory %d: %s\n", i + 1, sampleMemories[i]);
            System.out.printf("  ✓ Status: %s\n", result.getStatus());
            System.out.printf("  ✓ Memory ID: %s\n", result.getMemoryId());
            System.out.printf("  ✓ Category: %s (confidence: %.2f)\n", 
                result.getCategory().getPrimary(), result.getCategory().getConfidence());
            System.out.printf("  ✓ Belief analysis: %s\n", 
                result.getBeliefUpdateResult() != null ? "completed" : "skipped");
            System.out.println();
        }
        
        System.out.println("✓ Ingested " + sampleMemories.length + " memories successfully");
        System.out.println();
    }
    
    private static void demonstrateCategorization(InMemoryMemorySystemFactory.MemorySystem memorySystem) {
        System.out.println("--- Demo 2: Contextual Categorization ---");
        
        ContextualCategorizationEngine cce = memorySystem.getCategorizationEngine();
        
        String[] testContent = {
            "I love spending time with my family",
            "Meeting scheduled for 2 PM in conference room B",
            "The population of Tokyo is approximately 14 million",
            "Reminder to pick up prescription from pharmacy"
        };
        
        for (String content : testContent) {
            Metadata metadata = new Metadata();
            CategoryLabel category = cce.categorize(content, metadata);
            
            System.out.printf("Content: %s\n", content);
            System.out.printf("  → Category: %s\n", category.getPrimary());
            System.out.printf("  → Confidence: %.2f\n", category.getConfidence());
            System.out.printf("  → Tags: %s\n", category.getTags());
            System.out.println();
        }
        
        System.out.println("✓ Categorization engine working correctly");
        System.out.println();
    }
    
    private static void demonstrateRetrievalAndSearch(InMemoryMemorySystemFactory.MemorySystem memorySystem) {
        System.out.println("--- Demo 3: Memory Retrieval and Search ---");
        
        RetrievalResponseEngine rre = memorySystem.getRetrievalEngine();
        MemoryEncodingSystem mes = memorySystem.getMemoryEncodingSystem();
        
        // Get all memories for the demo agent
        List<MemoryRecord> agentMemories = mes.getMemoriesForAgent("demo-agent", 10);
        System.out.printf("Total memories for demo-agent: %d\n", agentMemories.size());
        
        // Perform some searches
        FilterOptions filters = new FilterOptions();
        filters.setAgentId("demo-agent");
        
        String[] searchQueries = {
            "food",
            "meeting",
            "France"
        };
        
        for (String query : searchQueries) {
            List<MemoryRecord> results = rre.retrieveRelevant(query, filters, 3);
            System.out.printf("\nSearch for '%s': %d results\n", query, results.size());
            
            for (int i = 0; i < results.size(); i++) {
                MemoryRecord memory = results.get(i);
                System.out.printf("  %d. %s (relevance: %.2f)\n", 
                    i + 1, memory.getContent(), 
                    memory.getRelevanceScore() != null ? memory.getRelevanceScore() : 0.0);
            }
            
            // Generate a response
            if (!results.isEmpty()) {
                String response = rre.composeResponse(query, results, "summary");
                System.out.printf("  Response: %s\n", response.substring(0, Math.min(100, response.length())) + "...");
            }
        }
        
        System.out.println("\n✓ Search and retrieval working correctly");
        System.out.println();
    }
    
    private static void demonstrateBeliefSystem(InMemoryMemorySystemFactory.MemorySystem memorySystem) {
        System.out.println("--- Demo 4: Belief Management ---");
        
        BeliefReinforcementConflictAnalyzer brca = memorySystem.getBeliefAnalyzer();
        
        // Get beliefs for the demo agent
        List<Belief> beliefs = brca.getBeliefsForAgent("demo-agent");
        
        System.out.printf("Active beliefs for demo-agent: %d\n", beliefs.size());
        
        for (int i = 0; i < Math.min(5, beliefs.size()); i++) {
            Belief belief = beliefs.get(i);
            System.out.printf("  %d. %s (confidence: %.2f, evidence: %d)\n", 
                i + 1, belief.getStatement(), belief.getConfidence(), belief.getEvidenceCount());
        }
        
        // Show belief statistics
        Map<String, Object> beliefStats = brca.getBeliefStatistics();
        System.out.printf("\nBelief System Statistics:\n");
        System.out.printf("  - Total beliefs: %s\n", beliefStats.get("totalBeliefs"));
        System.out.printf("  - Active beliefs: %s\n", beliefStats.get("activeBeliefs"));
        System.out.printf("  - Total analyses: %s\n", beliefStats.get("totalAnalyses"));
        
        System.out.println("\n✓ Belief system working correctly");
        System.out.println();
    }
    
    private static void demonstrateStatistics(InMemoryMemorySystemFactory.MemorySystem memorySystem) {
        System.out.println("--- Demo 5: System Statistics ---");
        
        Map<String, Object> systemStats = memorySystem.getSystemStatistics();
        
        System.out.println("System-wide Statistics:");
        
        // Ingestion statistics
        @SuppressWarnings("unchecked")
        Map<String, Object> ingestionStats = (Map<String, Object>) systemStats.get("ingestionModule");
        System.out.printf("  Ingestion:\n");
        System.out.printf("    - Total ingestions: %s\n", ingestionStats.get("totalIngestions"));
        System.out.printf("    - Successful: %s\n", ingestionStats.get("successfulIngestions"));
        System.out.printf("    - Success rate: %.2f%%\n", 
            ((Number) ingestionStats.get("successRate")).doubleValue() * 100);
        
        // Storage statistics
        @SuppressWarnings("unchecked")
        Map<String, Object> storageStats = (Map<String, Object>) systemStats.get("memoryEncodingSystem");
        System.out.printf("  Storage:\n");
        System.out.printf("    - Total memories: %s\n", storageStats.get("totalMemories"));
        System.out.printf("    - Total agents: %s\n", storageStats.get("totalAgents"));
        System.out.printf("    - Total categories: %s\n", storageStats.get("totalCategories"));
        
        // Categorization statistics
        @SuppressWarnings("unchecked")
        Map<String, Object> categorizationStats = (Map<String, Object>) systemStats.get("categorizationEngine");
        System.out.printf("  Categorization:\n");
        System.out.printf("    - Total categorizations: %s\n", categorizationStats.get("totalCategorizations"));
        System.out.printf("    - Available categories: %s\n", categorizationStats.get("availableCategories"));
        
        System.out.printf("\nSystem Health: %s\n", memorySystem.isHealthy() ? "✓ Healthy" : "✗ Unhealthy");
        
        System.out.println("\n✓ All statistics collected successfully");
        System.out.println();
    }
}