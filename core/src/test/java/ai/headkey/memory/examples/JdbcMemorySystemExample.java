package ai.headkey.memory.examples;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.memory.implementations.JdbcMemoryEncodingSystem;
import ai.headkey.memory.implementations.JdbcMemorySystemFactory;
import ai.headkey.memory.interfaces.MemoryEncodingSystem;

import java.time.Instant;
import java.util.*;

/**
 * Comprehensive example demonstrating how to use the JDBC Memory Encoding System.
 * 
 * This example shows:
 * - Setting up different database backends (HSQLDB for testing, PostgreSQL for production)
 * - Creating and configuring vector embedding generators
 * - Storing, retrieving, and searching memories
 * - Using different similarity search strategies
 * - Performance monitoring and optimization
 * 
 * Run this example to see the JDBC memory system in action.
 */
public class JdbcMemorySystemExample {
    
    public static void main(String[] args) {
        System.out.println("=== JDBC Memory Encoding System Example ===\n");
        
        try {
            // Example 1: Basic HSQLDB setup for testing
            runBasicExample();
            
            // Example 2: Advanced configuration with custom embedding generator
            runAdvancedExample();
            
            // Example 3: Production PostgreSQL setup (commented out - requires PostgreSQL)
            // runProductionExample();
            
            // Example 4: Performance and monitoring
            runPerformanceExample();
            
        } catch (Exception e) {
            System.err.println("Example failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Basic example using HSQLDB in-memory database.
     */
    private static void runBasicExample() {
        System.out.println("--- Basic HSQLDB Example ---");
        
        // Create an in-memory HSQLDB system for testing
        MemoryEncodingSystem memorySystem = JdbcMemorySystemFactory.createInMemoryTestSystem(
            JdbcMemorySystemFactory.createMockEmbeddingGenerator()
        );
        
        // Store some memories
        System.out.println("Storing memories...");
        
        List<String> contents = Arrays.asList(
            "Machine learning is a powerful subset of artificial intelligence that enables computers to learn without explicit programming.",
            "Deep learning neural networks have revolutionized computer vision and natural language processing.",
            "Data science combines statistics, programming, and domain expertise to extract insights from data.",
            "Cloud computing provides scalable and flexible infrastructure for modern applications.",
            "Blockchain technology offers decentralized and secure transaction recording."
        );
        
        List<MemoryRecord> storedMemories = new ArrayList<>();
        
        for (int i = 0; i < contents.size(); i++) {
            CategoryLabel category = createCategory("technology", "ai", Arrays.asList("tech", "innovation"));
            Metadata metadata = createMetadata("example-agent-" + (i % 2 + 1), "example-source");
            
            MemoryRecord memory = memorySystem.encodeAndStore(contents.get(i), category, metadata);
            storedMemories.add(memory);
            System.out.println("  Stored: " + memory.getId() + " - " + memory.getContent().substring(0, 50) + "...");
        }
        
        // Retrieve memories
        System.out.println("\nRetrieving memories...");
        for (MemoryRecord memory : storedMemories) {
            Optional<MemoryRecord> retrieved = memorySystem.getMemory(memory.getId());
            if (retrieved.isPresent()) {
                System.out.println("  Retrieved: " + retrieved.get().getId());
            }
        }
        
        // Search for similar memories
        System.out.println("\nSearching for similar memories...");
        List<MemoryRecord> searchResults = memorySystem.searchSimilar("artificial intelligence machine learning", 3);
        for (MemoryRecord result : searchResults) {
            System.out.println("  Found: " + result.getContent().substring(0, 60) + "...");
        }
        
        // Get statistics
        System.out.println("\nMemory system statistics:");
        Map<String, Object> stats = memorySystem.getStorageStatistics();
        System.out.println("  Total memories: " + stats.get("totalMemories"));
        System.out.println("  Total operations: " + stats.get("totalOperations"));
        System.out.println("  Strategy: " + stats.get("strategyName"));
        System.out.println("  Supports vector search: " + stats.get("supportsVectorSearch"));
        
        System.out.println("Basic example completed!\n");
    }
    
    /**
     * Advanced example with custom configuration.
     */
    private static void runAdvancedExample() {
        System.out.println("--- Advanced Configuration Example ---");
        
        // Create a custom embedding generator that simulates OpenAI embeddings
        JdbcMemoryEncodingSystem.VectorEmbeddingGenerator customEmbeddingGenerator = content -> {
            // In a real implementation, this would call OpenAI API or use a local model
            System.out.println("  Generating embedding for: " + content.substring(0, Math.min(30, content.length())) + "...");
            
            // Create a more realistic mock embedding based on content
            double[] embedding = new double[1536]; // OpenAI embedding dimension
            int hash = content.hashCode();
            
            for (int i = 0; i < embedding.length; i++) {
                // Create pseudo-semantic embeddings based on content
                double value = Math.sin(hash + i) * 0.5 + 0.5; // Normalize to [0, 1]
                embedding[i] = value;
            }
            
            return embedding;
        };
        
        // Create system with custom configuration using builder pattern
        MemoryEncodingSystem memorySystem = JdbcMemorySystemFactory.builder()
            .jdbcUrl("jdbc:hsqldb:mem:advanced_testdb")
            .credentials("SA", "")
            .driverClassName("org.hsqldb.jdbc.JDBCDriver")
            .poolSize(10, 2)
            .embeddingGenerator(customEmbeddingGenerator)
            .build();
        
        // Store memories with different categories and agents
        System.out.println("Storing categorized memories...");
        
        Map<String, List<String>> categorizedContent = Map.of(
            "science", Arrays.asList(
                "Quantum computing leverages quantum mechanical phenomena to process information.",
                "CRISPR gene editing technology allows precise modification of DNA sequences.",
                "Renewable energy sources like solar and wind are becoming increasingly efficient."
            ),
            "business", Arrays.asList(
                "Digital transformation is reshaping how companies operate and deliver value.",
                "Remote work has become a permanent fixture in many organizations.",
                "Sustainable business practices are driving competitive advantage."
            ),
            "education", Arrays.asList(
                "Online learning platforms have democratized access to quality education.",
                "Personalized learning adapts to individual student needs and pace.",
                "Virtual reality is creating immersive educational experiences."
            )
        );
        
        for (Map.Entry<String, List<String>> entry : categorizedContent.entrySet()) {
            String category = entry.getKey();
            
            for (int i = 0; i < entry.getValue().size(); i++) {
                String content = entry.getValue().get(i);
                
                CategoryLabel categoryLabel = createCategory(category, "general", Arrays.asList(category, "example"));
                Metadata metadata = createMetadata("agent-" + category, "advanced-example");
                metadata.setImportance(0.7 + (i * 0.1)); // Varying importance
                
                MemoryRecord memory = memorySystem.encodeAndStore(content, categoryLabel, metadata);
                System.out.println("  Stored [" + category + "]: " + memory.getId());
            }
        }
        
        // Test category-based retrieval
        System.out.println("\nRetrieving memories by category...");
        for (String category : categorizedContent.keySet()) {
            List<MemoryRecord> categoryMemories = memorySystem.getMemoriesInCategory(category, null, 10);
            System.out.println("  " + category + " memories: " + categoryMemories.size());
        }
        
        // Test agent-based retrieval
        System.out.println("\nRetrieving memories by agent...");
        List<MemoryRecord> scienceAgentMemories = memorySystem.getMemoriesForAgent("agent-science", 10);
        System.out.println("  Science agent memories: " + scienceAgentMemories.size());
        
        // Test semantic search across categories
        System.out.println("\nSemantic search across categories...");
        List<MemoryRecord> techResults = memorySystem.searchSimilar("technology innovation digital", 5);
        System.out.println("  Found " + techResults.size() + " technology-related memories");
        
        for (MemoryRecord result : techResults) {
            System.out.println("    - " + result.getCategory().getPrimary() + ": " + 
                             result.getContent().substring(0, Math.min(60, result.getContent().length())) + "...");
        }
        
        System.out.println("Advanced example completed!\n");
    }
    
    /**
     * Example showing how to set up PostgreSQL for production use.
     * Note: This requires a running PostgreSQL instance with pgvector extension.
     */
    @SuppressWarnings("unused")
    private static void runProductionExample() {
        System.out.println("--- Production PostgreSQL Example ---");
        
        // Production embedding generator (would integrate with actual embedding service)
        JdbcMemoryEncodingSystem.VectorEmbeddingGenerator productionEmbeddingGenerator = content -> {
            // In production, this would call an actual embedding service
            // Example: OpenAI Embeddings API, HuggingFace Transformers, etc.
            System.out.println("  Calling production embedding service for: " + 
                             content.substring(0, Math.min(40, content.length())) + "...");
            
            // Mock production-quality embeddings
            double[] embedding = new double[1536];
            // ... actual embedding generation logic would go here
            return embedding;
        };
        
        // Create PostgreSQL system for production
        MemoryEncodingSystem memorySystem = JdbcMemorySystemFactory.createPostgreSQLSystem(
            "localhost", 5432, "headkey_memory", "postgres", "password",
            productionEmbeddingGenerator
        );
        
        // Production usage patterns...
        System.out.println("Production setup would go here (requires PostgreSQL with pgvector)");
        System.out.println("Production example completed!\n");
    }
    
    /**
     * Example demonstrating performance monitoring and optimization.
     */
    private static void runPerformanceExample() {
        System.out.println("--- Performance and Monitoring Example ---");
        
        MemoryEncodingSystem memorySystem = JdbcMemorySystemFactory.createInMemoryTestSystem(
            JdbcMemorySystemFactory.createMockEmbeddingGenerator()
        );
        
        // Bulk operations for performance testing
        System.out.println("Performing bulk operations...");
        long startTime = System.currentTimeMillis();
        
        List<MemoryRecord> bulkMemories = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String content = "Performance test memory " + i + " with various content about " +
                           (i % 2 == 0 ? "artificial intelligence" : "machine learning") +
                           " and data processing techniques.";
            
            CategoryLabel category = createCategory("performance", "test", Arrays.asList("bulk", "test"));
            Metadata metadata = createMetadata("perf-agent", "performance-test");
            
            MemoryRecord memory = memorySystem.encodeAndStore(content, category, metadata);
            bulkMemories.add(memory);
            
            if ((i + 1) % 20 == 0) {
                System.out.println("  Stored " + (i + 1) + " memories...");
            }
        }
        
        long bulkTime = System.currentTimeMillis() - startTime;
        System.out.println("Bulk storage completed in " + bulkTime + "ms");
        
        // Performance search testing
        System.out.println("\nPerformance search testing...");
        startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10; i++) {
            List<MemoryRecord> results = memorySystem.searchSimilar("artificial intelligence", 10);
            if (i == 0) {
                System.out.println("  First search found " + results.size() + " results");
            }
        }
        
        long searchTime = System.currentTimeMillis() - startTime;
        System.out.println("10 searches completed in " + searchTime + "ms (avg: " + (searchTime / 10.0) + "ms)");
        
        // System optimization
        System.out.println("\nOptimizing system...");
        Map<String, Object> optimizationResults = memorySystem.optimize(true);
        System.out.println("  Optimization completed in " + optimizationResults.get("optimizationDurationMs") + "ms");
        
        // Detailed statistics
        System.out.println("\nDetailed system statistics:");
        Map<String, Object> stats = memorySystem.getStorageStatistics();
        
        for (Map.Entry<String, Object> stat : stats.entrySet()) {
            System.out.println("  " + stat.getKey() + ": " + stat.getValue());
        }
        
        // Agent-specific statistics
        System.out.println("\nAgent-specific statistics:");
        Map<String, Object> agentStats = memorySystem.getAgentStatistics("perf-agent");
        
        for (Map.Entry<String, Object> stat : agentStats.entrySet()) {
            System.out.println("  " + stat.getKey() + ": " + stat.getValue());
        }
        
        // Capacity information
        System.out.println("\nCapacity information:");
        Map<String, Object> capacity = memorySystem.getCapacityInfo();
        
        for (Map.Entry<String, Object> info : capacity.entrySet()) {
            System.out.println("  " + info.getKey() + ": " + info.getValue());
        }
        
        // Health check
        System.out.println("\nSystem health check: " + (memorySystem.isHealthy() ? "HEALTHY" : "UNHEALTHY"));
        
        System.out.println("Performance example completed!\n");
    }
    
    // Helper methods
    
    private static CategoryLabel createCategory(String primary, String secondary, List<String> tags) {
        CategoryLabel category = new CategoryLabel();
        category.setPrimary(primary);
        category.setSecondary(secondary);
        category.setTags(new HashSet<>(tags));
        category.setConfidence(0.85 + (Math.random() * 0.15)); // Random confidence between 0.85-1.0
        return category;
    }
    
    private static Metadata createMetadata(String agentId, String source) {
        Metadata metadata = new Metadata();
        metadata.setProperty("agentId", agentId);
        metadata.setSource(source);
        metadata.setImportance(0.6 + (Math.random() * 0.4)); // Random importance between 0.6-1.0
        metadata.setProperty("timestamp", Instant.now());
        
        // Add some example tags
        List<String> tags = Arrays.asList("example", "test", "demo");
        metadata.setTags(new HashSet<>(tags));
        
        return metadata;
    }
}