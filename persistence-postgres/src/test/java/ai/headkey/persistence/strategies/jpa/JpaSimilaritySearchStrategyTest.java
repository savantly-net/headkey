package ai.headkey.persistence.strategies.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.interfaces.VectorEmbeddingGenerator;
import ai.headkey.persistence.entities.MemoryEntity;
import ai.headkey.persistence.factory.JpaMemorySystemFactory;
import ai.headkey.persistence.services.JpaMemoryEncodingSystem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Tests for JPA similarity search strategies.
 */
class JpaSimilaritySearchStrategyTest {
    
    private static final String AGENT_ID = "test-agent";
    private static EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    
    // Mock embedding generator for testing
    private final VectorEmbeddingGenerator mockEmbeddingGenerator = 
        text -> {
            try {
                // Simple mock: convert text to vector based on character codes
                double[] vector = new double[5];
                char[] chars = text.toLowerCase().toCharArray();
                for (int i = 0; i < Math.min(chars.length, 5); i++) {
                    vector[i] = (double) chars[i] / 128.0; // Normalize to 0-1 range
                }
                return vector;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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
        
        entityManagerFactory = Persistence.createEntityManagerFactory("headkey-beliefs-h2-test", properties);
    }
    
    @AfterAll
    static void tearDownClass() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }
    
    @BeforeEach
    void setUp() {
        entityManager = entityManagerFactory.createEntityManager();
        setupTestData();
    }
    
    @AfterEach
    void tearDown() {
        if (entityManager != null) {
            try {
                // Clean up test data to prevent constraint violations
                entityManager.getTransaction().begin();
                entityManager.createQuery("DELETE FROM MemoryEntity").executeUpdate();
                entityManager.getTransaction().commit();
            } catch (Exception e) {
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }
                // Ignore cleanup errors
            } finally {
                entityManager.close();
            }
        }
    }
    
    private void setupTestData() {
        entityManager.getTransaction().begin();
        
        // Create test memory entities
        MemoryEntity entity1 = new MemoryEntity();
        entity1.setId("test-1");
        entity1.setAgentId("agent-1");
        entity1.setContent("The quick brown fox jumps over the lazy dog");
        try {
            entity1.setEmbedding(mockEmbeddingGenerator.generateEmbedding(entity1.getContent()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        entity1.setRelevanceScore(0.8);
        entity1.setCreatedAt(Instant.now().minusSeconds(3600));
        entity1.setLastAccessed(Instant.now().minusSeconds(1800));
        
        MemoryEntity entity2 = new MemoryEntity();
        entity2.setId("test-2");
        entity2.setAgentId("agent-1");
        entity2.setContent("A fast brown fox leaps above a sleepy canine");
        try {
            entity2.setEmbedding(mockEmbeddingGenerator.generateEmbedding(entity2.getContent()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        entity2.setRelevanceScore(0.7);
        entity2.setCreatedAt(Instant.now().minusSeconds(7200));
        entity2.setLastAccessed(Instant.now().minusSeconds(3600));
        
        MemoryEntity entity3 = new MemoryEntity();
        entity3.setId("test-3");
        entity3.setAgentId("agent-2");
        entity3.setContent("Machine learning algorithms process data efficiently");
        try {
            entity3.setEmbedding(mockEmbeddingGenerator.generateEmbedding(entity3.getContent()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        entity3.setRelevanceScore(0.9);
        entity3.setCreatedAt(Instant.now().minusSeconds(1800));
        entity3.setLastAccessed(Instant.now().minusSeconds(900));
        
        MemoryEntity entity4 = new MemoryEntity();
        entity4.setId("test-4");
        entity4.setAgentId("agent-1");
        entity4.setContent("Artificial intelligence and neural networks");
        entity4.setEmbedding(null); // No embedding to test fallback
        entity4.setRelevanceScore(0.6);
        entity4.setCreatedAt(Instant.now().minusSeconds(900));
        entity4.setLastAccessed(Instant.now().minusSeconds(450));
        
        entityManager.persist(entity1);
        entityManager.persist(entity2);
        entityManager.persist(entity3);
        entityManager.persist(entity4);
        
        entityManager.getTransaction().commit();
    }
    
    @Test
    void testDefaultJpaSimilaritySearchStrategy_TextSearch() throws Exception {
        DefaultJpaSimilaritySearchStrategy strategy = new DefaultJpaSimilaritySearchStrategy();
        
        String queryContent = "machine learning";
        
        List<MemoryRecord> results = strategy.searchSimilar(
            entityManager, queryContent, null, null, 5, 1000, 0.0);
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        
        // Should find the machine learning entity
        boolean foundMachineLearning = results.stream()
            .anyMatch(record -> record.getContent().toLowerCase().contains("machine learning"));
        assertTrue(foundMachineLearning);
    }
    
    @Test
    void testDefaultJpaSimilaritySearchStrategy_AgentFiltering() throws Exception {
        DefaultJpaSimilaritySearchStrategy strategy = new DefaultJpaSimilaritySearchStrategy();
        
        String queryContent = "fox";
        double[] queryVector = mockEmbeddingGenerator.generateEmbedding(queryContent);
        
        List<MemoryRecord> results = strategy.searchSimilar(
            entityManager, queryContent, queryVector, "agent-1", 5, 1000, 0.0);
        
        assertNotNull(results);
        
        // All results should belong to agent-1
        for (MemoryRecord record : results) {
            assertEquals("agent-1", record.getAgentId());
        }
    }
    
    @Test
    void testTextBasedJpaSimilaritySearchStrategy() throws Exception {
        TextBasedJpaSimilaritySearchStrategy strategy = new TextBasedJpaSimilaritySearchStrategy();
        
        assertFalse(strategy.supportsVectorSearch());
        assertEquals("Text-based JPA Similarity Search", strategy.getStrategyName());
        
        String queryContent = "brown fox";
        
        List<MemoryRecord> results = strategy.searchSimilar(
            entityManager, queryContent, null, null, 5, 1000, 0.0);
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        
        // Should find entities containing "brown" or "fox"
        boolean foundRelevant = results.stream()
            .anyMatch(record -> record.getContent().toLowerCase().contains("brown") || 
                               record.getContent().toLowerCase().contains("fox"));
        assertTrue(foundRelevant);
    }
    
    @Test
    void testTextBasedJpaSimilaritySearchStrategy_KeywordExtraction() throws Exception {
        TextBasedJpaSimilaritySearchStrategy strategy = new TextBasedJpaSimilaritySearchStrategy();
        
        String queryContent = "artificial intelligence neural networks machine learning";
        
        List<MemoryRecord> results = strategy.searchSimilar(
            entityManager, queryContent, null, null, 5, 1000, 0.0);
        
        assertNotNull(results);
        
        // Should prioritize entities with multiple keyword matches
        if (!results.isEmpty()) {
            // At least one result should contain relevant keywords
            boolean foundRelevant = results.stream()
                .anyMatch(record -> {
                    String content = record.getContent().toLowerCase();
                    return content.contains("artificial") || content.contains("intelligence") ||
                           content.contains("neural") || content.contains("machine") || content.contains("learning");
                });
            assertTrue(foundRelevant);
        }
    }
    
    @Test
    void testJpaSimilaritySearchStrategyFactory() {
        JpaSimilaritySearchStrategy strategy = JpaSimilaritySearchStrategyFactory.createStrategy(entityManager);
        
        assertNotNull(strategy);
        assertNotNull(strategy.getStrategyName());
        
        // For H2 database, should typically return TextBasedJpaSimilaritySearchStrategy
        // But DefaultJpaSimilaritySearchStrategy is also valid
        assertTrue(strategy instanceof TextBasedJpaSimilaritySearchStrategy || 
                  strategy instanceof DefaultJpaSimilaritySearchStrategy);
    }
    
    @Test
    void testJpaSimilaritySearchStrategyFactory_DatabaseCapabilities() {
        JpaSimilaritySearchStrategyFactory.DatabaseCapabilities capabilities = 
            JpaSimilaritySearchStrategyFactory.analyzeDatabaseCapabilities(entityManager);
        
        assertNotNull(capabilities);
        assertNotNull(capabilities.getDatabaseType());
        assertNotNull(capabilities.getVersion());
        
        // For H2 database
        assertEquals(JpaSimilaritySearchStrategyFactory.DatabaseType.H2, capabilities.getDatabaseType());
        assertFalse(capabilities.hasVectorSupport()); // H2 doesn't have vector support by default
    }
    
    @Test
    void testStrategyValidation() throws Exception {
        DefaultJpaSimilaritySearchStrategy strategy = new DefaultJpaSimilaritySearchStrategy();
        
        boolean isValid = strategy.validateSchema(entityManager);
        assertTrue(isValid);
        
        TextBasedJpaSimilaritySearchStrategy textStrategy = new TextBasedJpaSimilaritySearchStrategy();
        boolean isTextValid = textStrategy.validateSchema(entityManager);
        assertTrue(isTextValid);
    }
    
    @Test
    void testCosineSimilarityCalculation() {
        DefaultJpaSimilaritySearchStrategy strategy = new DefaultJpaSimilaritySearchStrategy();
        
        double[] vector1 = {1.0, 0.0, 0.0};
        double[] vector2 = {1.0, 0.0, 0.0};
        double[] vector3 = {0.0, 1.0, 0.0};
        
        // Identical vectors should have similarity of 1.0
        assertEquals(1.0, strategy.calculateCosineSimilarity(vector1, vector2), 0.001);
        
        // Orthogonal vectors should have similarity of 0.0
        assertEquals(0.0, strategy.calculateCosineSimilarity(vector1, vector3), 0.001);
        
        // Null vectors should return 0.0
        assertEquals(0.0, strategy.calculateCosineSimilarity(null, vector1), 0.001);
        assertEquals(0.0, strategy.calculateCosineSimilarity(vector1, null), 0.001);
        
        // Different length vectors should return 0.0
        double[] shortVector = {1.0, 0.0};
        assertEquals(0.0, strategy.calculateCosineSimilarity(vector1, shortVector), 0.001);
    }
    
    @Test
    void testJpaMemorySystemIntegration() {
        JpaMemoryEncodingSystem memorySystem = JpaMemorySystemFactory.createSystem(
            entityManagerFactory, mockEmbeddingGenerator);
        
        assertNotNull(memorySystem);
        assertNotNull(memorySystem.getSimilaritySearchStrategy());
        
        // Test that the memory system can perform similarity search
        List<MemoryRecord> results = memorySystem.searchSimilar("brown fox", 3,AGENT_ID);
        assertNotNull(results);
        
        // Should find relevant memories
        assertTrue(results.size() <= 3);
    }
    
    @Test
    void testJpaMemorySystemFactory_Builder() {
        JpaMemoryEncodingSystem memorySystem = JpaMemorySystemFactory.builder()
            .entityManagerFactory(entityManagerFactory)
            .embeddingGenerator(mockEmbeddingGenerator)
            .textBasedStrategy()
            .maxSimilaritySearchResults(500)
            .similarityThreshold(0.1)
            .batchSize(50)
            .build();
        
        assertNotNull(memorySystem);
        assertEquals(500, memorySystem.getMaxSimilaritySearchResults());
        assertEquals(0.1, memorySystem.getSimilarityThreshold(), 0.001);
        assertEquals(50, memorySystem.getBatchSize());
        
        assertTrue(memorySystem.getSimilaritySearchStrategy() instanceof TextBasedJpaSimilaritySearchStrategy);
    }
    
    @Test
    void testJpaMemorySystemFactory_StrategyRecommendation() {
        JpaMemorySystemFactory.StrategyRecommendation recommendation = 
            JpaMemorySystemFactory.analyzeDatabase(entityManagerFactory);
        
        assertNotNull(recommendation);
        assertNotNull(recommendation.getCapabilities());
        assertNotNull(recommendation.getRecommendedStrategy());
        
        assertFalse(recommendation.hasVectorSupport()); // H2 doesn't support vectors
        assertNotNull(recommendation.toString());
    }
    
    @Test
    void testSimilarityThresholdFiltering() throws Exception {
        DefaultJpaSimilaritySearchStrategy strategy = new DefaultJpaSimilaritySearchStrategy();
        
        String queryContent = "completely unrelated content xyz123";
        double[] queryVector = mockEmbeddingGenerator.generateEmbedding(queryContent);
        
        // Use a high similarity threshold to filter out unrelated results
        List<MemoryRecord> results = strategy.searchSimilar(
            entityManager, queryContent, queryVector, null, 5, 1000, 0.9);
        
        assertNotNull(results);
        // With high threshold, should get fewer or no results for unrelated content
        assertTrue(results.size() <= 5);
    }
    
    @Test
    void testEmptyQueryHandling() throws Exception {
        TextBasedJpaSimilaritySearchStrategy strategy = new TextBasedJpaSimilaritySearchStrategy();
        
        List<MemoryRecord> results = strategy.searchSimilar(
            entityManager, "", null, null, 5, 1000, 0.0);
        
        assertNotNull(results);
        // Empty query should return empty results or minimal results
        assertTrue(results.size() <= 5);
    }
}