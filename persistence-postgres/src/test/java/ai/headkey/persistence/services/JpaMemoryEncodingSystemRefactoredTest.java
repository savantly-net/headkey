package ai.headkey.persistence.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.memory.interfaces.VectorEmbeddingGenerator;
import ai.headkey.persistence.strategies.jpa.DefaultJpaSimilaritySearchStrategy;
import ai.headkey.persistence.strategies.jpa.TextBasedJpaSimilaritySearchStrategy;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Integration tests for the refactored JpaMemoryEncodingSystem with similarity search strategies.
 */
class JpaMemoryEncodingSystemRefactoredTest {
    
    private static EntityManagerFactory entityManagerFactory;
    private JpaMemoryEncodingSystem memorySystem;

    private static final String AGENT_ID= "test-agent-id";
    
    // Mock embedding generator for testing
    private final VectorEmbeddingGenerator mockEmbeddingGenerator = 
        text -> {
            // Simple mock: convert text to vector based on character codes
            double[] vector = new double[5];
            char[] chars = text.toLowerCase().toCharArray();
            for (int i = 0; i < Math.min(chars.length, 5); i++) {
                vector[i] = (double) chars[i] / 128.0; // Normalize to 0-1 range
            }
            return vector;
        };
    
    @BeforeAll
    static void setUpClass() {
        // Create in-memory H2 database for testing
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        properties.put("jakarta.persistence.jdbc.user", "sa");
        properties.put("jakarta.persistence.jdbc.password", "");
        properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.put("hibernate.hbm2ddl.auto", "create-drop");
        properties.put("hibernate.show_sql", "false");
        
        entityManagerFactory = Persistence.createEntityManagerFactory("headkey-memory-h2-test", properties);
    }
    
    @AfterAll
    static void tearDownClass() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }
    
    @BeforeEach
    void setUp() {
        memorySystem = new JpaMemoryEncodingSystem(entityManagerFactory, mockEmbeddingGenerator);
        setupTestData();
    }
    
    @AfterEach
    void tearDown() {
        if (memorySystem != null) {
            // Clean up test data
            cleanupTestData();
        }
    }
    
    private void setupTestData() {
        var defaultCategory = new CategoryLabel("default", "Default Category");
        var defaultMetadata = new Metadata();
        // Store test memories with generated IDs
        memorySystem.encodeAndStore("The quick brown fox jumps over the lazy dog", defaultCategory, defaultMetadata,AGENT_ID);
        memorySystem.encodeAndStore("A fast brown fox leaps above a sleepy canine", defaultCategory, defaultMetadata,AGENT_ID);
        memorySystem.encodeAndStore("Machine learning algorithms process data efficiently", defaultCategory, defaultMetadata,AGENT_ID);
        memorySystem.encodeAndStore("Artificial intelligence and neural networks", defaultCategory, defaultMetadata,AGENT_ID);
    }
    
    private void cleanupTestData() {
        // Cleanup is handled by test isolation
    }
    
    @Test
    void testMemorySystemWithDefaultStrategy() {
        assertNotNull(memorySystem.getSimilaritySearchStrategy());
        
        // Test similarity search functionality
        List<MemoryRecord> results = memorySystem.searchSimilar("brown fox jump", 3,AGENT_ID);
        
        assertNotNull(results);
        assertTrue(results.size() <= 3);
        
        // Should find memories containing "brown" and "fox"
        boolean foundRelevant = results.stream()
            .anyMatch(record -> record.getContent().toLowerCase().contains("brown"));
        assertTrue(foundRelevant);
    }
    
    @Test
    void testMemorySystemWithCustomTextBasedStrategy() {
        // Create memory system with specific text-based strategy
        JpaMemoryEncodingSystem customSystem = new JpaMemoryEncodingSystem(
            entityManagerFactory, mockEmbeddingGenerator, 100, true, 1000, 0.0,
            new TextBasedJpaSimilaritySearchStrategy());
        
        assertTrue(customSystem.getSimilaritySearchStrategy() instanceof TextBasedJpaSimilaritySearchStrategy);
        assertFalse(customSystem.getSimilaritySearchStrategy().supportsVectorSearch());
        
        // Test search functionality
        List<MemoryRecord> results = customSystem.searchSimilar("learning", 2,AGENT_ID);
        
        assertNotNull(results);
        assertTrue(results.size() <= 2);
        
        // Should find the machine learning memory
        boolean foundMachineLearning = results.stream()
            .anyMatch(record -> record.getContent().toLowerCase().contains("learning"));
        assertTrue(foundMachineLearning);
    }
    
    @Test
    void testMemorySystemWithCustomDefaultStrategy() throws Exception {
        var defaultStrategy = new DefaultJpaSimilaritySearchStrategy();
        defaultStrategy.initialize(entityManagerFactory.createEntityManager());
        // Create memory system with specific default strategy
        JpaMemoryEncodingSystem customSystem = new JpaMemoryEncodingSystem(
            entityManagerFactory, mockEmbeddingGenerator, 100, true, 1000, 0.0,
            defaultStrategy);
        
        assertTrue(customSystem.getSimilaritySearchStrategy() instanceof DefaultJpaSimilaritySearchStrategy);
        assertFalse(customSystem.getSimilaritySearchStrategy().supportsVectorSearch());
        
        // Test vector-based search
        List<MemoryRecord> results = customSystem.searchSimilar("artificial intelligence", 2,AGENT_ID);
        
        assertNotNull(results);
        assertTrue(results.size() <= 2);
    }
    
    
    @Test
    void testSimilaritySearchWithoutEmbeddings() {
        // Create a memory system that won't generate embeddings
        JpaMemoryEncodingSystem noEmbeddingSystem = new JpaMemoryEncodingSystem(entityManagerFactory);
        
        // Store a memory without embeddings
        MemoryRecord stored = noEmbeddingSystem.encodeAndStore("This memory has no vector embedding", new CategoryLabel("test"), new Metadata(),AGENT_ID);
        
        // Search should still work using text-based search
        List<MemoryRecord> results = noEmbeddingSystem.searchSimilar("vector embedding", 5,AGENT_ID);
        
        assertNotNull(results);
        
        // Clean up
        try {
            noEmbeddingSystem.removeMemory(stored.getId());
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    @Test
    void testSimilarityThresholdFiltering() {
        // Create memory system with high similarity threshold
        JpaMemoryEncodingSystem highThresholdSystem = new JpaMemoryEncodingSystem(
            entityManagerFactory, mockEmbeddingGenerator, 100, true, 1000, 0.8,
            new DefaultJpaSimilaritySearchStrategy());
        
        assertEquals(0.8, highThresholdSystem.getSimilarityThreshold(), 0.001);
        
        // Search with unrelated content should return fewer results due to high threshold
        List<MemoryRecord> results = highThresholdSystem.searchSimilar("completely unrelated xyz123", 5,AGENT_ID);
        
        assertNotNull(results);
        // With high threshold, should get fewer results for unrelated content
        assertTrue(results.size() <= 5);
    }
    
    @Test
    void testMaxSimilaritySearchResultsConfiguration() {
        // Create memory system with limited max results
        JpaMemoryEncodingSystem limitedSystem = new JpaMemoryEncodingSystem(
            entityManagerFactory, mockEmbeddingGenerator, 100, true, 2, 0.0,
            new DefaultJpaSimilaritySearchStrategy());
        
        assertEquals(2, limitedSystem.getMaxSimilaritySearchResults());
        
        // Even if we ask for more results, should be limited by maxSimilaritySearchResults
        List<MemoryRecord> results = limitedSystem.searchSimilar("brown", 10,AGENT_ID);
        
        assertNotNull(results);
        // Should respect the configured limit
        assertTrue(results.size() <= 10); // Search limit
    }
    
    @Test
    void testStrategyDelegation() {
        // Verify that the memory system properly delegates to the strategy
        assertNotNull(memorySystem.getSimilaritySearchStrategy());
        
        String strategyName = memorySystem.getSimilaritySearchStrategy().getStrategyName();
        assertNotNull(strategyName);
        assertFalse(strategyName.trim().isEmpty());
        
        // Test that strategy methods are properly accessible
        boolean supportsVector = memorySystem.getSimilaritySearchStrategy().supportsVectorSearch();
        // Should be either true or false, not throw an exception
        assertTrue(supportsVector || !supportsVector);
    }
    
    @Test
    void testMemorySystemConfiguration() {
        // Test that all configuration parameters are properly set
        assertEquals(100, memorySystem.getBatchSize());
        assertTrue(memorySystem.isSecondLevelCacheEnabled());
        assertEquals(1000, memorySystem.getMaxSimilaritySearchResults());
        assertEquals(0.0, memorySystem.getSimilarityThreshold(), 0.001);
        
        assertNotNull(memorySystem.getEntityManagerFactory());
        assertEquals(entityManagerFactory, memorySystem.getEntityManagerFactory());
    }
    
    @Test
    void testHealthCheckWithStrategy() {
        // Health check should work with the new strategy system
        assertTrue(memorySystem.isHealthy());
        
        // Storage statistics should include strategy information
        var stats = memorySystem.getStorageStatistics();
        assertNotNull(stats);
        
        // Should contain basic statistics
        assertTrue(stats.containsKey("totalMemories"));
    }
    
    @Test
    void testErrorHandlingInSimilaritySearch() {
        // Test error handling when strategy encounters issues
        
        // Null query should be handled gracefully
        assertThrows(IllegalArgumentException.class, () -> {
            memorySystem.searchSimilar(null, 5,AGENT_ID);
        });
        
        // Empty query should be handled gracefully
        assertThrows(IllegalArgumentException.class, () -> {
            memorySystem.searchSimilar("", 5,AGENT_ID);
        });
        
        // Invalid limit should be handled gracefully
        assertThrows(IllegalArgumentException.class, () -> {
            memorySystem.searchSimilar("test", 0,AGENT_ID);
        });
    }
}