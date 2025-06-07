package ai.headkey.memory.examples;

import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.implementations.AbstractMemoryEncodingSystem;
import ai.headkey.memory.implementations.JpaMemoryEncodingSystem;
import ai.headkey.memory.implementations.JpaMemorySystemFactory;
import ai.headkey.memory.strategies.jpa.*;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example demonstrating the new JPA similarity search strategy system.
 * 
 * This example shows how to:
 * 1. Create JPA memory systems with different similarity search strategies
 * 2. Use the factory pattern for easy configuration
 * 3. Analyze database capabilities
 * 4. Compare different search strategies
 * 5. Use the builder pattern for complex configurations
 */
public class JpaSimilaritySearchStrategyExample {
    
    // Mock embedding generator for demonstration
    private static final AbstractMemoryEncodingSystem.VectorEmbeddingGenerator embeddingGenerator = 
        text -> {
            // Simple demonstration embedding: hash-based vector
            double[] vector = new double[10];
            int hash = text.toLowerCase().hashCode();
            for (int i = 0; i < vector.length; i++) {
                vector[i] = ((hash >> i) & 1) == 1 ? 1.0 : 0.0;
            }
            return vector;
        };
    
    public static void main(String[] args) {
        System.out.println("=== JPA Similarity Search Strategy Example ===\n");
        
        // Create in-memory H2 database for demonstration
        EntityManagerFactory emf = createTestEntityManagerFactory();
        
        try {
            demonstrateBasicUsage(emf);
            demonstrateStrategyComparison(emf);
            demonstrateDatabaseAnalysis(emf);
            demonstrateBuilderPattern(emf);
            demonstrateCustomStrategy(emf);
        } finally {
            emf.close();
        }
    }
    
    private static EntityManagerFactory createTestEntityManagerFactory() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:example;DB_CLOSE_DELAY=-1");
        properties.put("jakarta.persistence.jdbc.user", "sa");
        properties.put("jakarta.persistence.jdbc.password", "");
        properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.put("hibernate.hbm2ddl.auto", "create-drop");
        properties.put("hibernate.show_sql", "false");
        
        return Persistence.createEntityManagerFactory("default", properties);
    }
    
    private static void demonstrateBasicUsage(EntityManagerFactory emf) {
        System.out.println("1. Basic Usage with Auto-Detection");
        System.out.println("==================================");
        
        // Create memory system with automatic strategy detection
        JpaMemoryEncodingSystem memorySystem = JpaMemorySystemFactory.createSystem(emf, embeddingGenerator);
        
        System.out.println("Strategy: " + memorySystem.getSimilaritySearchStrategy().getStrategyName());
        System.out.println("Supports Vector Search: " + memorySystem.getSimilaritySearchStrategy().supportsVectorSearch());
        
        // Store some sample memories
        storeExampleMemories(memorySystem);
        
        // Perform similarity search
        List<MemoryRecord> results = memorySystem.searchSimilar("machine learning AI", 3);
        System.out.println("\nSearch Results for 'machine learning AI':");
        printSearchResults(results);
        
        System.out.println();
    }
    
    private static void demonstrateStrategyComparison(EntityManagerFactory emf) {
        System.out.println("2. Strategy Comparison");
        System.out.println("=====================");
        
        // Create memory systems with different strategies
        JpaMemoryEncodingSystem vectorSystem = JpaMemorySystemFactory.createSystemWithStrategy(
            emf, embeddingGenerator, new DefaultJpaSimilaritySearchStrategy());
        
        JpaMemoryEncodingSystem textSystem = JpaMemorySystemFactory.createSystemWithStrategy(
            emf, embeddingGenerator, new TextBasedJpaSimilaritySearchStrategy());
        
        // Store sample data in both systems
        storeExampleMemories(vectorSystem);
        storeExampleMemories(textSystem);
        
        String query = "neural network deep learning";
        
        System.out.println("Vector-based Strategy Results:");
        List<MemoryRecord> vectorResults = vectorSystem.searchSimilar(query, 3);
        printSearchResults(vectorResults);
        
        System.out.println("Text-based Strategy Results:");
        List<MemoryRecord> textResults = textSystem.searchSimilar(query, 3);
        printSearchResults(textResults);
        
        System.out.println();
    }
    
    private static void demonstrateDatabaseAnalysis(EntityManagerFactory emf) {
        System.out.println("3. Database Capability Analysis");
        System.out.println("===============================");
        
        // Analyze database capabilities
        JpaMemorySystemFactory.StrategyRecommendation recommendation = 
            JpaMemorySystemFactory.analyzeDatabase(emf);
        
        System.out.println("Database Analysis:");
        System.out.println("- Type: " + recommendation.getCapabilities().getDatabaseType());
        System.out.println("- Version: " + recommendation.getCapabilities().getVersion());
        System.out.println("- Vector Support: " + recommendation.hasVectorSupport());
        System.out.println("- Full Text Support: " + recommendation.hasFullTextSupport());
        System.out.println("- Recommended Strategy: " + recommendation.getRecommendedStrategy().getStrategyName());
        
        System.out.println();
    }
    
    private static void demonstrateBuilderPattern(EntityManagerFactory emf) {
        System.out.println("4. Builder Pattern Configuration");
        System.out.println("================================");
        
        // Use builder pattern for complex configuration
        JpaMemoryEncodingSystem memorySystem = JpaMemorySystemFactory.builder()
            .entityManagerFactory(emf)
            .embeddingGenerator(embeddingGenerator)
            .textBasedStrategy()
            .maxSimilaritySearchResults(100)
            .similarityThreshold(0.2)
            .batchSize(25)
            .enableSecondLevelCache(false)
            .build();
        
        System.out.println("Configured Memory System:");
        System.out.println("- Strategy: " + memorySystem.getSimilaritySearchStrategy().getStrategyName());
        System.out.println("- Max Results: " + memorySystem.getMaxSimilaritySearchResults());
        System.out.println("- Similarity Threshold: " + memorySystem.getSimilarityThreshold());
        System.out.println("- Batch Size: " + memorySystem.getBatchSize());
        System.out.println("- Second Level Cache: " + memorySystem.isSecondLevelCacheEnabled());
        
        storeExampleMemories(memorySystem);
        
        // Test with configured threshold
        List<MemoryRecord> results = memorySystem.searchSimilar("artificial intelligence", 5);
        System.out.println("\nFiltered Results (threshold: 0.2):");
        printSearchResults(results);
        
        System.out.println();
    }
    
    private static void demonstrateCustomStrategy(EntityManagerFactory emf) {
        System.out.println("5. Custom Strategy Implementation");
        System.out.println("=================================");
        
        // Create a custom strategy that combines multiple approaches
        JpaSimilaritySearchStrategy customStrategy = new JpaSimilaritySearchStrategy() {
            private final DefaultJpaSimilaritySearchStrategy defaultStrategy = new DefaultJpaSimilaritySearchStrategy();
            private final TextBasedJpaSimilaritySearchStrategy textStrategy = new TextBasedJpaSimilaritySearchStrategy();
            
            @Override
            public List<MemoryRecord> searchSimilar(
                    jakarta.persistence.EntityManager entityManager, 
                    String queryContent, 
                    double[] queryVector, 
                    String agentId, 
                    int limit,
                    int maxSimilaritySearchResults, 
                    double similarityThreshold) throws Exception {
                
                System.out.println("  Using custom hybrid strategy...");
                
                // Use vector search if available, otherwise fall back to text search
                if (queryVector != null) {
                    return defaultStrategy.searchSimilar(entityManager, queryContent, queryVector, 
                                                       agentId, limit, maxSimilaritySearchResults, similarityThreshold);
                } else {
                    return textStrategy.searchSimilar(entityManager, queryContent, queryVector, 
                                                    agentId, limit, maxSimilaritySearchResults, similarityThreshold);
                }
            }
            
            @Override
            public boolean supportsVectorSearch() {
                return true;
            }
            
            @Override
            public String getStrategyName() {
                return "Custom Hybrid Strategy";
            }
        };
        
        JpaMemoryEncodingSystem memorySystem = JpaMemorySystemFactory.createSystemWithStrategy(
            emf, embeddingGenerator, customStrategy);
        
        System.out.println("Custom Strategy: " + memorySystem.getSimilaritySearchStrategy().getStrategyName());
        
        storeExampleMemories(memorySystem);
        
        List<MemoryRecord> results = memorySystem.searchSimilar("computer science", 3);
        System.out.println("\nCustom Strategy Results:");
        printSearchResults(results);
        
        System.out.println();
    }
    
    private static void storeExampleMemories(JpaMemoryEncodingSystem memorySystem) {
        MemoryRecord memory1 = new MemoryRecord("ml-" + System.nanoTime(), "demo-agent", 
            "Machine learning algorithms can automatically improve through experience");
        memory1.setRelevanceScore(0.9);
        memory1.setCreatedAt(Instant.now().minusSeconds(3600));
            
        MemoryRecord memory2 = new MemoryRecord("ai-" + System.nanoTime(), "demo-agent", 
            "Artificial intelligence enables computers to simulate human intelligence");
        memory2.setRelevanceScore(0.8);
        memory2.setCreatedAt(Instant.now().minusSeconds(7200));
            
        MemoryRecord memory3 = new MemoryRecord("nn-" + System.nanoTime(), "demo-agent", 
            "Neural networks are computing systems inspired by biological neural networks");
        memory3.setRelevanceScore(0.85);
        memory3.setCreatedAt(Instant.now().minusSeconds(1800));
            
        MemoryRecord memory4 = new MemoryRecord("dl-" + System.nanoTime(), "demo-agent", 
            "Deep learning uses multiple layers to progressively extract features from data");
        memory4.setRelevanceScore(0.87);
        memory4.setCreatedAt(Instant.now().minusSeconds(900));
            
        MemoryRecord memory5 = new MemoryRecord("cs-" + System.nanoTime(), "demo-agent", 
            "Computer science encompasses algorithms, data structures, and computational thinking");
        memory5.setRelevanceScore(0.7);
        memory5.setCreatedAt(Instant.now().minusSeconds(5400));
            
        MemoryRecord[] memories = {memory1, memory2, memory3, memory4, memory5};
        
        for (MemoryRecord memory : memories) {
            try {
                memorySystem.encodeAndStore(memory.getContent(), memory.getCategory(), memory.getMetadata());
            } catch (Exception e) {
                // Skip if already exists (for demonstration purposes)
            }
        }
    }
    
    private static void printSearchResults(List<MemoryRecord> results) {
        if (results.isEmpty()) {
            System.out.println("  No results found");
            return;
        }
        
        for (int i = 0; i < results.size(); i++) {
            MemoryRecord record = results.get(i);
            System.out.printf("  %d. %s (Score: %.2f)%n", 
                i + 1, 
                record.getContent(), 
                record.getRelevanceScore() != null ? record.getRelevanceScore() : 0.0);
        }
    }
}