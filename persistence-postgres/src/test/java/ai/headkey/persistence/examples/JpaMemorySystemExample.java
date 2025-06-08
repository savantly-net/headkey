package ai.headkey.persistence.examples;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.memory.abstracts.AbstractMemoryEncodingSystem;
import ai.headkey.persistence.services.JpaMemoryEncodingSystem;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.time.Instant;
import java.util.*;

/**
 * Example demonstrating the usage of JpaMemoryEncodingSystem.
 * 
 * This example shows how to:
 * 1. Set up JPA configuration and EntityManagerFactory
 * 2. Create and configure the JPA memory system
 * 3. Store memories with vector embeddings
 * 4. Perform similarity searches
 * 5. Manage memory lifecycle (CRUD operations)
 * 6. Monitor system health and statistics
 * 7. Handle cleanup and shutdown
 * 
 * The example uses a mock embedding generator for demonstration purposes.
 * In production, you would integrate with a real embedding service like
 * OpenAI, Cohere, or a local embedding model.
 * 
 * @since 1.0
 */
public class JpaMemorySystemExample {
    
    private static final String AGENT_ID = "demo-agent-001";
    private static final String PERSISTENCE_UNIT = "headkey-memory-hsqldb-dev";
    
    public static void main(String[] args) {
        System.out.println("=== JPA Memory Encoding System Example ===\n");
        
        // Step 1: Initialize JPA and create memory system
        EntityManagerFactory entityManagerFactory = null;
        JpaMemoryEncodingSystem memorySystem = null;
        
        try {
            System.out.println("1. Initializing JPA EntityManagerFactory...");
            entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
            
            System.out.println("2. Creating JPA Memory Encoding System with embedding generator...");
            DemoEmbeddingGenerator embeddingGenerator = new DemoEmbeddingGenerator();
            memorySystem = new JpaMemoryEncodingSystem(
                entityManagerFactory, 
                embeddingGenerator,
                50,    // batch size
                true,  // enable second level cache
                100,   // max similarity search results
                0.1    // similarity threshold
            );
            
            // Step 2: Verify system health
            demonstrateHealthCheck(memorySystem);
            
            // Step 3: Store various types of memories
            demonstrateStoringMemories(memorySystem);
            
            // Step 4: Demonstrate retrieval operations
            demonstrateRetrievalOperations(memorySystem);
            
            // Step 5: Demonstrate similarity search
            demonstrateSimilaritySearch(memorySystem);
            
            // Step 6: Demonstrate memory management
            demonstrateMemoryManagement(memorySystem);
            
            // Step 7: Show statistics and monitoring
            demonstrateStatisticsAndMonitoring(memorySystem);
            
            // Step 8: Demonstrate optimization
            demonstrateOptimization(memorySystem);
            
            System.out.println("\n=== Example completed successfully! ===");
            
        } catch (Exception e) {
            System.err.println("Error during example execution: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Step 9: Cleanup
            if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
                System.out.println("\nCleaning up resources...");
                entityManagerFactory.close();
                System.out.println("EntityManagerFactory closed.");
            }
        }
    }
    
    private static void demonstrateHealthCheck(JpaMemoryEncodingSystem memorySystem) {
        System.out.println("\n=== Health Check ===");
        
        boolean isHealthy = memorySystem.isHealthy();
        System.out.println("System health status: " + (isHealthy ? "HEALTHY" : "UNHEALTHY"));
        
        Map<String, Object> capacityInfo = memorySystem.getCapacityInfo();
        System.out.println("Capacity info: " + capacityInfo);
        
        if (!isHealthy) {
            throw new RuntimeException("Memory system is not healthy!");
        }
    }
    
    private static void demonstrateStoringMemories(JpaMemoryEncodingSystem memorySystem) {
        System.out.println("\n=== Storing Memories ===");
        
        // Store knowledge about AI
        MemoryRecord aiMemory = storeMemory(memorySystem,
            "Artificial Intelligence is a branch of computer science that aims to create " +
            "intelligent machines capable of performing tasks that typically require human intelligence.",
            "knowledge", "ai", 0.9, "AI textbook"
        );
        System.out.println("Stored AI memory: " + aiMemory.getId());
        
        // Store knowledge about machine learning
        MemoryRecord mlMemory = storeMemory(memorySystem,
            "Machine Learning is a subset of AI that enables computers to learn and improve " +
            "from experience without being explicitly programmed for every task.",
            "knowledge", "ml", 0.85, "ML research paper"
        );
        System.out.println("Stored ML memory: " + mlMemory.getId());
        
        // Store procedural knowledge
        MemoryRecord procedureMemory = storeMemory(memorySystem,
            "To train a neural network: 1) Prepare data, 2) Define architecture, " +
            "3) Initialize weights, 4) Forward pass, 5) Calculate loss, 6) Backpropagation, 7) Update weights",
            "procedure", "training", 0.8, "Training manual"
        );
        System.out.println("Stored procedure memory: " + procedureMemory.getId());
        
        // Store personal experience
        MemoryRecord experienceMemory = storeMemory(memorySystem,
            "Today I learned that vector embeddings can capture semantic meaning of text, " +
            "which is crucial for similarity search in memory systems.",
            "experience", "learning", 0.7, "Daily reflection"
        );
        System.out.println("Stored experience memory: " + experienceMemory.getId());
        
        // Store factual information
        MemoryRecord factMemory = storeMemory(memorySystem,
            "The Transformer architecture was introduced in the paper 'Attention Is All You Need' " +
            "by Vaswani et al. in 2017 and revolutionized natural language processing.",
            "fact", "nlp", 0.95, "Research database"
        );
        System.out.println("Stored fact memory: " + factMemory.getId());
        
        System.out.println("Successfully stored 5 different types of memories.");
    }
    
    private static void demonstrateRetrievalOperations(JpaMemoryEncodingSystem memorySystem) {
        System.out.println("\n=== Retrieval Operations ===");
        
        // Get all memories for the agent
        List<MemoryRecord> agentMemories = memorySystem.getMemoriesForAgent(AGENT_ID, 0);
        System.out.println("Total memories for agent: " + agentMemories.size());
        
        // Get limited number of memories
        List<MemoryRecord> recentMemories = memorySystem.getMemoriesForAgent(AGENT_ID, 3);
        System.out.println("Recent memories (limited to 3): " + recentMemories.size());
        
        // Get memories by IDs
        if (!agentMemories.isEmpty()) {
            Set<String> memoryIds = new HashSet<>();
            for (int i = 0; i < Math.min(2, agentMemories.size()); i++) {
                memoryIds.add(agentMemories.get(i).getId());
            }
            
            Map<String, MemoryRecord> retrievedMemories = memorySystem.getMemories(memoryIds);
            System.out.println("Retrieved memories by IDs: " + retrievedMemories.size());
            
            // Display retrieved memories
            retrievedMemories.values().forEach(memory -> {
                System.out.println("  - " + memory.getId() + ": " + 
                    memory.getContent().substring(0, Math.min(50, memory.getContent().length())) + "...");
            });
        }
        
        // Get memories in specific category
        List<MemoryRecord> knowledgeMemories = memorySystem.getMemoriesInCategory("knowledge", AGENT_ID, 5);
        System.out.println("Knowledge memories: " + knowledgeMemories.size());
        
        // Get old memories (older than 0 seconds - should include all)
        List<MemoryRecord> oldMemories = memorySystem.getOldMemories(0, AGENT_ID, 10);
        System.out.println("Old memories: " + oldMemories.size());
    }
    
    private static void demonstrateSimilaritySearch(JpaMemoryEncodingSystem memorySystem) {
        System.out.println("\n=== Similarity Search ===");
        
        // Search for AI-related content
        String aiQuery = "artificial intelligence and machine learning algorithms";
        List<MemoryRecord> aiResults = memorySystem.searchSimilar(aiQuery, 3);
        System.out.println("AI-related search results: " + aiResults.size());
        
        aiResults.forEach(memory -> {
            System.out.println("  - Score: N/A, Content: " + 
                memory.getContent().substring(0, Math.min(80, memory.getContent().length())) + "...");
        });
        
        // Search for learning-related content
        String learningQuery = "learning and training neural networks";
        List<MemoryRecord> learningResults = memorySystem.searchSimilar(learningQuery, 2);
        System.out.println("\nLearning-related search results: " + learningResults.size());
        
        learningResults.forEach(memory -> {
            System.out.println("  - Content: " + 
                memory.getContent().substring(0, Math.min(80, memory.getContent().length())) + "...");
        });
        
        // Search for specific technical terms
        String techQuery = "transformer architecture attention mechanism";
        List<MemoryRecord> techResults = memorySystem.searchSimilar(techQuery, 2);
        System.out.println("\nTechnical search results: " + techResults.size());
        
        techResults.forEach(memory -> {
            System.out.println("  - Content: " + 
                memory.getContent().substring(0, Math.min(80, memory.getContent().length())) + "...");
        });
    }
    
    private static void demonstrateMemoryManagement(JpaMemoryEncodingSystem memorySystem) {
        System.out.println("\n=== Memory Management ===");
        
        // Create a memory to update and delete
        MemoryRecord tempMemory = storeMemory(memorySystem,
            "This is a temporary memory that will be updated and then deleted.",
            "temporary", "test", 0.5, "Test system"
        );
        System.out.println("Created temporary memory: " + tempMemory.getId());
        
        // Update the memory
        tempMemory.setContent("This memory has been updated with new content about memory management.");
        tempMemory.getMetadata().setImportance(0.8);
        tempMemory.setRelevanceScore(0.9);
        
        MemoryRecord updatedMemory = memorySystem.updateMemory(tempMemory);
        System.out.println("Updated memory - new version: " + updatedMemory.getVersion());
        System.out.println("Updated content preview: " + 
            updatedMemory.getContent().substring(0, Math.min(60, updatedMemory.getContent().length())) + "...");
        
        // Retrieve the updated memory to verify changes
        Optional<MemoryRecord> retrieved = memorySystem.getMemory(updatedMemory.getId());
        if (retrieved.isPresent()) {
            System.out.println("Verified update - relevance score: " + retrieved.get().getRelevanceScore());
        }
        
        // Delete the memory
        boolean deleted = memorySystem.removeMemory(updatedMemory.getId());
        System.out.println("Memory deleted: " + deleted);
        
        // Verify deletion
        Optional<MemoryRecord> afterDeletion = memorySystem.getMemory(updatedMemory.getId());
        System.out.println("Memory exists after deletion: " + afterDeletion.isPresent());
        
        // Demonstrate batch deletion
        List<MemoryRecord> batchMemories = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            MemoryRecord batchMemory = storeMemory(memorySystem,
                "Batch memory #" + i + " for testing bulk operations.",
                "batch", "test", 0.3, "Batch system"
            );
            batchMemories.add(batchMemory);
        }
        
        Set<String> batchIds = new HashSet<>();
        batchMemories.forEach(memory -> batchIds.add(memory.getId()));
        
        System.out.println("Created " + batchMemories.size() + " batch memories");
        
        Set<String> deletedIds = memorySystem.removeMemories(batchIds);
        System.out.println("Batch deleted " + deletedIds.size() + " memories");
    }
    
    private static void demonstrateStatisticsAndMonitoring(JpaMemoryEncodingSystem memorySystem) {
        System.out.println("\n=== Statistics and Monitoring ===");
        
        // Get overall storage statistics
        Map<String, Object> storageStats = memorySystem.getStorageStatistics();
        System.out.println("Storage Statistics:");
        storageStats.forEach((key, value) -> {
            System.out.println("  " + key + ": " + value);
        });
        
        // Get agent-specific statistics
        Map<String, Object> agentStats = memorySystem.getAgentStatistics(AGENT_ID);
        System.out.println("\nAgent Statistics:");
        agentStats.forEach((key, value) -> {
            System.out.println("  " + key + ": " + value);
        });
        
        // Check system configuration
        System.out.println("\nSystem Configuration:");
        System.out.println("  Batch Size: " + memorySystem.getBatchSize());
        System.out.println("  Second Level Cache: " + memorySystem.isSecondLevelCacheEnabled());
        System.out.println("  Max Similarity Results: " + memorySystem.getMaxSimilaritySearchResults());
        System.out.println("  Similarity Threshold: " + memorySystem.getSimilarityThreshold());
        
        // Verify health status
        boolean currentHealth = memorySystem.isHealthy();
        System.out.println("  Current Health: " + (currentHealth ? "HEALTHY" : "UNHEALTHY"));
    }
    
    private static void demonstrateOptimization(JpaMemoryEncodingSystem memorySystem) {
        System.out.println("\n=== System Optimization ===");
        
        // Perform basic optimization
        System.out.println("Running basic optimization...");
        Map<String, Object> basicResults = memorySystem.optimize(false);
        System.out.println("Basic optimization results:");
        basicResults.forEach((key, value) -> {
            System.out.println("  " + key + ": " + value);
        });
        
        // Perform deep optimization with vacuum
        System.out.println("\nRunning deep optimization with vacuum...");
        Map<String, Object> deepResults = memorySystem.optimize(true);
        System.out.println("Deep optimization results:");
        deepResults.forEach((key, value) -> {
            System.out.println("  " + key + ": " + value);
        });
    }
    
    private static MemoryRecord storeMemory(JpaMemoryEncodingSystem memorySystem,
                                          String content, 
                                          String primaryCategory, 
                                          String secondaryCategory,
                                          double importance, 
                                          String source) {
        // Create category label
        CategoryLabel category = new CategoryLabel();
        category.setPrimary(primaryCategory);
        category.setSecondary(secondaryCategory);
        category.setConfidence(0.9);
        Set<String> categoryTags = new HashSet<>();
        categoryTags.add("example");
        categoryTags.add("demo");
        categoryTags.add(primaryCategory);
        category.setTags(categoryTags);
        
        // Create metadata
        Metadata metadata = new Metadata();
        metadata.setProperty("agentId", AGENT_ID);
        metadata.setImportance(importance);
        metadata.setSource(source);
        Set<String> metadataTags = new HashSet<>();
        metadataTags.add("demo");
        metadataTags.add("example");
        metadataTags.add(primaryCategory);
        metadata.setTags(metadataTags);
        metadata.setConfidence(0.95);
        
        // Store the memory
        return memorySystem.encodeAndStore(content, category, metadata);
    }
    
    /**
     * Demo embedding generator that creates simple vector embeddings
     * based on keyword analysis of the content.
     * 
     * In production, you would replace this with a real embedding service
     * that uses trained language models.
     */
    private static class DemoEmbeddingGenerator implements AbstractMemoryEncodingSystem.VectorEmbeddingGenerator {
        
        private static final Map<String, double[]> KEYWORD_VECTORS = Map.of(
            "artificial", new double[]{0.9, 0.1, 0.2, 0.3, 0.1},
            "intelligence", new double[]{0.8, 0.2, 0.1, 0.4, 0.2},
            "machine", new double[]{0.7, 0.9, 0.3, 0.2, 0.1},
            "learning", new double[]{0.6, 0.8, 0.9, 0.1, 0.2},
            "neural", new double[]{0.5, 0.7, 0.8, 0.9, 0.3},
            "network", new double[]{0.4, 0.6, 0.7, 0.8, 0.9},
            "transformer", new double[]{0.3, 0.5, 0.6, 0.7, 0.8},
            "attention", new double[]{0.2, 0.4, 0.5, 0.6, 0.7},
            "embedding", new double[]{0.1, 0.3, 0.4, 0.5, 0.6},
            "vector", new double[]{0.9, 0.8, 0.7, 0.6, 0.5}
        );
        
        @Override
        public double[] generateEmbedding(String content) throws Exception {
            System.out.println("    Generating embedding for: " + 
                content.substring(0, Math.min(50, content.length())) + "...");
            
            double[] embedding = new double[5];
            String[] words = content.toLowerCase().split("\\s+");
            
            // Simple keyword-based embedding generation
            for (String word : words) {
                double[] keywordVector = KEYWORD_VECTORS.get(word.replaceAll("[^a-zA-Z]", ""));
                if (keywordVector != null) {
                    for (int i = 0; i < embedding.length; i++) {
                        embedding[i] += keywordVector[i];
                    }
                }
            }
            
            // Normalize the embedding
            double magnitude = 0.0;
            for (double value : embedding) {
                magnitude += value * value;
            }
            
            if (magnitude > 0.0) {
                magnitude = Math.sqrt(magnitude);
                for (int i = 0; i < embedding.length; i++) {
                    embedding[i] /= magnitude;
                }
            } else {
                // Fallback: generate pseudo-random embedding based on content hash
                Random random = new Random(content.hashCode());
                for (int i = 0; i < embedding.length; i++) {
                    embedding[i] = random.nextGaussian() * 0.1;
                }
            }
            
            return embedding;
        }
    }
}