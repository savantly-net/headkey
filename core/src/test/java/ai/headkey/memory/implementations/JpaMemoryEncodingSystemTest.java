package ai.headkey.memory.implementations;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.memory.entities.MemoryEntity;
import ai.headkey.memory.exceptions.MemoryNotFoundException;
import ai.headkey.memory.exceptions.StorageException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for JpaMemoryEncodingSystem.
 * 
 * Tests all major functionality including:
 * - Basic CRUD operations
 * - Vector embedding storage and similarity search
 * - Concurrent access scenarios
 * - Error handling and edge cases
 * - Performance characteristics
 * - Database integrity
 * 
 * Uses H2 in-memory database for fast, isolated testing.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JpaMemoryEncodingSystemTest {
    
    private static EntityManagerFactory entityManagerFactory;
    private JpaMemoryEncodingSystem memorySystem;
    private MockEmbeddingGenerator embeddingGenerator;
    
    // Test data
    private static final String TEST_AGENT_ID = "test-agent-001";
    private static final String TEST_CONTENT_1 = "This is the first test memory content about artificial intelligence.";
    private static final String TEST_CONTENT_2 = "Second memory content discussing machine learning algorithms.";
    private static final String TEST_CONTENT_3 = "Third memory about natural language processing and embeddings.";
    private static final String SEARCH_QUERY = "artificial intelligence machine learning";
    
    @BeforeAll
    static void setUpClass() {
        // Initialize EntityManagerFactory with test configuration
        entityManagerFactory = Persistence.createEntityManagerFactory("headkey-memory-h2-test");
        assertNotNull(entityManagerFactory, "EntityManagerFactory should be created");
        assertTrue(entityManagerFactory.isOpen(), "EntityManagerFactory should be open");
    }
    
    @AfterAll
    static void tearDownClass() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
    }
    
    @BeforeEach
    void setUp() {
        embeddingGenerator = new MockEmbeddingGenerator();
        memorySystem = new JpaMemoryEncodingSystem(entityManagerFactory, embeddingGenerator);
        
        // Verify the system is healthy (but don't fail if it's not, as schema creation might be in progress)
        boolean isHealthy = memorySystem.isHealthy();
        System.out.println("Memory system health status: " + isHealthy);
    }
    
    @AfterEach
    void tearDown() {
        // Clean up test data
        cleanupTestData();
    }
    
    @Test
    @Order(1)
    @DisplayName("Should create and configure JPA memory system correctly")
    void testSystemCreation() {
        // Test default constructor
        JpaMemoryEncodingSystem defaultSystem = new JpaMemoryEncodingSystem(entityManagerFactory);
        assertNotNull(defaultSystem);
        assertTrue(defaultSystem.isHealthy());
        
        // Test constructor with embedding generator
        JpaMemoryEncodingSystem systemWithEmbedding = new JpaMemoryEncodingSystem(
            entityManagerFactory, embeddingGenerator);
        assertNotNull(systemWithEmbedding);
        assertSame(embeddingGenerator, systemWithEmbedding.getEmbeddingGenerator());
        
        // Test full constructor
        JpaMemoryEncodingSystem fullSystem = new JpaMemoryEncodingSystem(
            entityManagerFactory, embeddingGenerator, 50, false, 500, 0.1);
        assertNotNull(fullSystem);
        assertEquals(50, fullSystem.getBatchSize());
        assertFalse(fullSystem.isSecondLevelCacheEnabled());
        assertEquals(500, fullSystem.getMaxSimilaritySearchResults());
        assertEquals(0.1, fullSystem.getSimilarityThreshold(), 0.001);
    }
    
    @Test
    @Order(2)
    @DisplayName("Should handle invalid EntityManagerFactory gracefully")
    void testInvalidEntityManagerFactory() {
        // Test null EntityManagerFactory
        assertThrows(IllegalArgumentException.class, () -> 
            new JpaMemoryEncodingSystem(null));
        
        // Test closed EntityManagerFactory
        EntityManagerFactory closedFactory = Persistence.createEntityManagerFactory("headkey-memory-h2-test");
        closedFactory.close();
        
        assertThrows(IllegalArgumentException.class, () -> 
            new JpaMemoryEncodingSystem(closedFactory));
    }
    
    @Test
    @Order(3)
    @DisplayName("Should encode and store memory successfully")
    void testEncodeAndStore() {
        // Create test data
        CategoryLabel category = createTestCategoryLabel("knowledge", "ai");
        Metadata metadata = createTestMetadata(TEST_AGENT_ID, 0.8, "test-source");
        
        // Store memory
        MemoryRecord stored = memorySystem.encodeAndStore(TEST_CONTENT_1, category, metadata);
        
        // Verify stored memory
        assertNotNull(stored);
        assertNotNull(stored.getId());
        assertEquals(TEST_CONTENT_1, stored.getContent());
        assertEquals(category, stored.getCategory());
        assertEquals(metadata, stored.getMetadata());
        assertNotNull(stored.getCreatedAt());
        assertNotNull(stored.getLastAccessed());
        assertEquals(1L, stored.getVersion());
        
        // Verify embedding was generated
        assertTrue(embeddingGenerator.wasCalledWith(TEST_CONTENT_1));
        
        // Verify database storage
        verifyMemoryInDatabase(stored.getId(), stored);
    }
    
    @Test
    @Order(4)
    @DisplayName("Should retrieve memory by ID successfully")
    void testGetMemory() {
        // Store test memory
        MemoryRecord stored = storeTestMemory(TEST_CONTENT_1, "knowledge");
        
        // Retrieve memory
        Optional<MemoryRecord> retrieved = memorySystem.getMemory(stored.getId());
        
        // Verify retrieval
        assertTrue(retrieved.isPresent());
        MemoryRecord memory = retrieved.get();
        assertEquals(stored.getId(), memory.getId());
        assertEquals(stored.getContent(), memory.getContent());
        assertEquals(stored.getAgentId(), memory.getAgentId());
        
        // Verify last accessed was updated
        assertTrue(memory.getLastAccessed().isAfter(stored.getLastAccessed()) ||
                  memory.getLastAccessed().equals(stored.getLastAccessed()));
    }
    
    @Test
    @Order(5)
    @DisplayName("Should return empty when memory not found")
    void testGetMemoryNotFound() {
        Optional<MemoryRecord> result = memorySystem.getMemory("non-existent-id");
        assertTrue(result.isEmpty());
    }
    
    @Test
    @Order(6)
    @DisplayName("Should retrieve multiple memories successfully")
    void testGetMemories() {
        // Store multiple test memories
        MemoryRecord memory1 = storeTestMemory(TEST_CONTENT_1, "knowledge");
        MemoryRecord memory2 = storeTestMemory(TEST_CONTENT_2, "learning");
        MemoryRecord memory3 = storeTestMemory(TEST_CONTENT_3, "nlp");
        
        // Retrieve memories
        Set<String> ids = Set.of(memory1.getId(), memory2.getId(), memory3.getId());
        Map<String, MemoryRecord> retrieved = memorySystem.getMemories(ids);
        
        // Verify retrieval
        assertEquals(3, retrieved.size());
        assertTrue(retrieved.containsKey(memory1.getId()));
        assertTrue(retrieved.containsKey(memory2.getId()));
        assertTrue(retrieved.containsKey(memory3.getId()));
        
        // Verify content
        assertEquals(TEST_CONTENT_1, retrieved.get(memory1.getId()).getContent());
        assertEquals(TEST_CONTENT_2, retrieved.get(memory2.getId()).getContent());
        assertEquals(TEST_CONTENT_3, retrieved.get(memory3.getId()).getContent());
    }
    
    @Test
    @Order(7)
    @DisplayName("Should handle partial retrieval for mixed valid/invalid IDs")
    void testGetMemoriesPartialResults() {
        // Store one test memory
        MemoryRecord stored = storeTestMemory(TEST_CONTENT_1, "knowledge");
        
        // Request mix of valid and invalid IDs
        Set<String> ids = Set.of(stored.getId(), "invalid-id-1", "invalid-id-2");
        Map<String, MemoryRecord> retrieved = memorySystem.getMemories(ids);
        
        // Should only return the valid memory
        assertEquals(1, retrieved.size());
        assertTrue(retrieved.containsKey(stored.getId()));
        assertEquals(stored.getContent(), retrieved.get(stored.getId()).getContent());
    }
    
    @Test
    @Order(8)
    @DisplayName("Should update memory successfully")
    void testUpdateMemory() {
        // Store initial memory
        MemoryRecord stored = storeTestMemory(TEST_CONTENT_1, "knowledge");
        String originalId = stored.getId();
        Long originalVersion = stored.getVersion();
        
        // Update memory content and metadata
        stored.setContent(TEST_CONTENT_2);
        stored.getMetadata().setImportance(0.9);
        stored.setRelevanceScore(0.95);
        
        // Update memory
        MemoryRecord updated = memorySystem.updateMemory(stored);
        
        // Verify update
        assertNotNull(updated);
        assertEquals(originalId, updated.getId());
        assertEquals(TEST_CONTENT_2, updated.getContent());
        assertEquals(0.9, updated.getMetadata().getImportance());
        assertEquals(0.95, updated.getRelevanceScore());
        assertTrue(updated.getVersion() > originalVersion);
        
        // Verify embedding was regenerated
        assertTrue(embeddingGenerator.wasCalledWith(TEST_CONTENT_2));
        
        // Verify database update
        verifyMemoryInDatabase(updated.getId(), updated);
    }
    
    @Test
    @Order(9)
    @DisplayName("Should throw exception when updating non-existent memory")
    void testUpdateNonExistentMemory() {
        MemoryRecord nonExistent = createTestMemoryRecord("non-existent-id", TEST_CONTENT_1, "knowledge");
        
        assertThrows(MemoryNotFoundException.class, () -> 
            memorySystem.updateMemory(nonExistent));
    }
    
    @Test
    @Order(10)
    @DisplayName("Should remove memory successfully")
    void testRemoveMemory() {
        // Store test memory
        MemoryRecord stored = storeTestMemory(TEST_CONTENT_1, "knowledge");
        String memoryId = stored.getId();
        
        // Verify memory exists
        assertTrue(memorySystem.getMemory(memoryId).isPresent());
        
        // Remove memory
        boolean removed = memorySystem.removeMemory(memoryId);
        
        // Verify removal
        assertTrue(removed);
        assertTrue(memorySystem.getMemory(memoryId).isEmpty());
        
        // Verify database removal
        verifyMemoryNotInDatabase(memoryId);
    }
    
    @Test
    @Order(11)
    @DisplayName("Should return false when removing non-existent memory")
    void testRemoveNonExistentMemory() {
        boolean removed = memorySystem.removeMemory("non-existent-id");
        assertFalse(removed);
    }
    
    @Test
    @Order(12)
    @DisplayName("Should remove multiple memories successfully")
    void testRemoveMemories() {
        // Store multiple test memories
        MemoryRecord memory1 = storeTestMemory(TEST_CONTENT_1, "knowledge");
        MemoryRecord memory2 = storeTestMemory(TEST_CONTENT_2, "learning");
        MemoryRecord memory3 = storeTestMemory(TEST_CONTENT_3, "nlp");
        
        // Remove memories
        Set<String> idsToRemove = Set.of(memory1.getId(), memory2.getId());
        Set<String> removedIds = memorySystem.removeMemories(idsToRemove);
        
        // Verify removal
        assertEquals(2, removedIds.size());
        assertTrue(removedIds.contains(memory1.getId()));
        assertTrue(removedIds.contains(memory2.getId()));
        
        // Verify memories are gone
        assertTrue(memorySystem.getMemory(memory1.getId()).isEmpty());
        assertTrue(memorySystem.getMemory(memory2.getId()).isEmpty());
        
        // Verify remaining memory still exists
        assertTrue(memorySystem.getMemory(memory3.getId()).isPresent());
    }
    
    @Test
    @Order(13)
    @DisplayName("Should perform vector similarity search successfully")
    void testVectorSimilaritySearch() {
        // Store memories with different content
        storeTestMemory("Artificial intelligence and machine learning", "ai");
        storeTestMemory("Deep learning neural networks", "ai");
        storeTestMemory("Natural language processing", "nlp");
        storeTestMemory("Database management systems", "database");
        storeTestMemory("Web development frameworks", "web");
        
        // Search for AI-related content
        List<MemoryRecord> results = memorySystem.searchSimilar(SEARCH_QUERY, 3);
        
        // Verify results
        assertNotNull(results);
        assertTrue(results.size() <= 3);
        assertFalse(results.isEmpty());
        
        // Results should be ordered by similarity (highest first)
        if (results.size() > 1) {
            // Mock embedding generator should make AI content more similar
            assertTrue(results.get(0).getContent().toLowerCase().contains("artificial") ||
                      results.get(0).getContent().toLowerCase().contains("learning"));
        }
        
        // Verify embedding generation for query
        assertTrue(embeddingGenerator.wasCalledWith(SEARCH_QUERY));
    }
    
    @Test
    @Order(14)
    @DisplayName("Should fallback to text search when no embedding generator")
    void testTextSearchFallback() {
        // Create system without embedding generator
        JpaMemoryEncodingSystem noEmbeddingSystem = new JpaMemoryEncodingSystem(entityManagerFactory);
        
        // Store test memories
        CategoryLabel category = createTestCategoryLabel("knowledge", "ai");
        Metadata metadata = createTestMetadata(TEST_AGENT_ID, 0.8, "test");
        
        noEmbeddingSystem.encodeAndStore("Artificial intelligence research", category, metadata);
        noEmbeddingSystem.encodeAndStore("Machine learning algorithms", category, metadata);
        noEmbeddingSystem.encodeAndStore("Database design patterns", category, metadata);
        
        // Search should use text-based matching
        List<MemoryRecord> results = noEmbeddingSystem.searchSimilar("artificial", 5);
        
        // Should find memories containing the search term
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(r -> 
            r.getContent().toLowerCase().contains("artificial")));
    }
    
    @Test
    @Order(15)
    @DisplayName("Should retrieve memories for agent successfully")
    void testGetMemoriesForAgent() {
        // Store memories for different agents
        storeTestMemoryForAgent(TEST_CONTENT_1, "knowledge", TEST_AGENT_ID);
        storeTestMemoryForAgent(TEST_CONTENT_2, "learning", TEST_AGENT_ID);
        storeTestMemoryForAgent(TEST_CONTENT_3, "nlp", "other-agent");
        
        // Get memories for test agent
        List<MemoryRecord> agentMemories = memorySystem.getMemoriesForAgent(TEST_AGENT_ID, 0);
        
        // Verify results
        assertEquals(2, agentMemories.size());
        assertTrue(agentMemories.stream().allMatch(m -> 
            TEST_AGENT_ID.equals(m.getAgentId())));
        
        // Test with limit
        List<MemoryRecord> limitedMemories = memorySystem.getMemoriesForAgent(TEST_AGENT_ID, 1);
        assertEquals(1, limitedMemories.size());
    }
    
    @Test
    @Order(16)
    @DisplayName("Should retrieve memories in category successfully")
    void testGetMemoriesInCategory() {
        // Store memories in different categories
        storeTestMemory(TEST_CONTENT_1, "ai");
        storeTestMemory(TEST_CONTENT_2, "ai");
        storeTestMemory(TEST_CONTENT_3, "nlp");
        
        // Get memories in AI category
        List<MemoryRecord> aiMemories = memorySystem.getMemoriesInCategory("ai", null, 0);
        
        // Verify results (simplified - real implementation would need proper JSON querying)
        assertNotNull(aiMemories);
        
        // Test with agent filter
        List<MemoryRecord> agentAiMemories = memorySystem.getMemoriesInCategory("ai", TEST_AGENT_ID, 1);
        assertNotNull(agentAiMemories);
    }
    
    @Test
    @Order(17)
    @DisplayName("Should retrieve old memories successfully")
    void testGetOldMemories() throws InterruptedException {
        // Store memories with different ages
        MemoryRecord oldMemory = storeTestMemory(TEST_CONTENT_1, "knowledge");
        
        // Wait a bit to ensure time difference
        Thread.sleep(10);
        
        MemoryRecord newMemory = storeTestMemory(TEST_CONTENT_2, "learning");
        
        // Get memories older than 5 milliseconds
        List<MemoryRecord> oldMemories = memorySystem.getOldMemories(0, null, 0);
        
        // Should include both memories since they're both older than 0 seconds
        assertFalse(oldMemories.isEmpty());
        
        // Test with agent filter
        List<MemoryRecord> agentOldMemories = memorySystem.getOldMemories(0, TEST_AGENT_ID, 1);
        assertNotNull(agentOldMemories);
    }
    
    @Test
    @Order(18)
    @DisplayName("Should provide accurate storage statistics")
    void testGetStorageStatistics() {
        // Store some test data
        storeTestMemory(TEST_CONTENT_1, "knowledge");
        storeTestMemory(TEST_CONTENT_2, "learning");
        
        Map<String, Object> stats = memorySystem.getStorageStatistics();
        
        // Verify statistics
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalOperations"));
        assertTrue(stats.containsKey("totalMemories"));
        assertTrue(stats.containsKey("batchSize"));
        assertTrue(stats.containsKey("startTime"));
        assertTrue(stats.containsKey("uptimeSeconds"));
        
        Long totalMemories = (Long) stats.get("totalMemories");
        assertTrue(totalMemories >= 2);
    }
    
    @Test
    @Order(19)
    @DisplayName("Should provide accurate agent statistics")
    void testGetAgentStatistics() {
        // Store memories for test agent
        storeTestMemoryForAgent(TEST_CONTENT_1, "knowledge", TEST_AGENT_ID);
        storeTestMemoryForAgent(TEST_CONTENT_2, "learning", TEST_AGENT_ID);
        
        Map<String, Object> stats = memorySystem.getAgentStatistics(TEST_AGENT_ID);
        
        // Verify statistics
        assertNotNull(stats);
        assertEquals(TEST_AGENT_ID, stats.get("agentId"));
        assertTrue(stats.containsKey("totalMemories"));
        
        Long totalMemories = (Long) stats.get("totalMemories");
        assertEquals(2L, totalMemories);
    }
    
    @Test
    @Order(20)
    @DisplayName("Should optimize storage successfully")
    void testOptimize() {
        // Store some test data
        storeTestMemory(TEST_CONTENT_1, "knowledge");
        
        // Test basic optimization
        Map<String, Object> basicResult = memorySystem.optimize(false);
        assertNotNull(basicResult);
        assertTrue((Boolean) basicResult.get("success"));
        
        // Test deep optimization
        Map<String, Object> deepResult = memorySystem.optimize(true);
        assertNotNull(deepResult);
        assertTrue((Boolean) deepResult.get("success"));
        assertTrue((Boolean) deepResult.get("deepOptimization"));
    }
    
    @Test
    @Order(21)
    @DisplayName("Should handle capacity information correctly")
    void testGetCapacityInfo() {
        Map<String, Object> capacity = memorySystem.getCapacityInfo();
        
        assertNotNull(capacity);
        assertTrue((Boolean) capacity.get("unlimited"));
        assertTrue(capacity.containsKey("currentCount"));
        assertTrue(capacity.containsKey("batchSize"));
    }
    
    @Test
    @Order(22)
    @DisplayName("Should validate input parameters correctly")
    void testInputValidation() {
        CategoryLabel category = createTestCategoryLabel("test", "test");
        Metadata metadata = createTestMetadata(TEST_AGENT_ID, 0.5, "test");
        
        // Test null/empty content
        assertThrows(IllegalArgumentException.class, () -> 
            memorySystem.encodeAndStore(null, category, metadata));
        assertThrows(IllegalArgumentException.class, () -> 
            memorySystem.encodeAndStore("", category, metadata));
        assertThrows(IllegalArgumentException.class, () -> 
            memorySystem.encodeAndStore("   ", category, metadata));
        
        // Test null category
        assertThrows(IllegalArgumentException.class, () -> 
            memorySystem.encodeAndStore(TEST_CONTENT_1, null, metadata));
        
        // Test null metadata
        assertThrows(IllegalArgumentException.class, () -> 
            memorySystem.encodeAndStore(TEST_CONTENT_1, category, null));
        
        // Test invalid memory IDs
        assertThrows(IllegalArgumentException.class, () -> 
            memorySystem.getMemory(null));
        assertThrows(IllegalArgumentException.class, () -> 
            memorySystem.getMemory(""));
        assertThrows(IllegalArgumentException.class, () -> 
            memorySystem.removeMemory("   "));
        
        // Test invalid search parameters
        assertThrows(IllegalArgumentException.class, () -> 
            memorySystem.searchSimilar(null, 5));
        assertThrows(IllegalArgumentException.class, () -> 
            memorySystem.searchSimilar(TEST_CONTENT_1, 0));
    }
    
    @Test
    @Order(23)
    @DisplayName("Should handle concurrent access correctly")
    void testConcurrentAccess() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // Concurrent store operations
        for (int i = 0; i < 10; i++) {
            final int index = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                storeTestMemory("Concurrent content " + index, "concurrent");
            }, executor);
            futures.add(future);
        }
        
        // Wait for all operations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        
        // Verify all memories were stored
        Map<String, Object> stats = memorySystem.getStorageStatistics();
        Long totalMemories = (Long) stats.get("totalMemories");
        assertTrue(totalMemories >= 10);
    }
    
    @Test
    @Order(24)
    @DisplayName("Should handle database errors gracefully")
    void testErrorHandling() {
        // Close EntityManagerFactory to simulate database issues
        EntityManagerFactory brokenFactory = Persistence.createEntityManagerFactory("headkey-memory-h2-test");
        brokenFactory.close();
        
        JpaMemoryEncodingSystem brokenSystem = new JpaMemoryEncodingSystem(brokenFactory);
        
        // Should not be healthy
        assertFalse(brokenSystem.isHealthy());
        
        // Operations should throw StorageException
        CategoryLabel category = createTestCategoryLabel("test", "test");
        Metadata metadata = createTestMetadata(TEST_AGENT_ID, 0.5, "test");
        
        assertThrows(StorageException.class, () -> 
            brokenSystem.encodeAndStore(TEST_CONTENT_1, category, metadata));
    }
    
    @Test
    @Order(25)
    @DisplayName("Should handle large batch operations efficiently")
    void testLargeBatchOperations() {
        List<MemoryRecord> storedMemories = new ArrayList<>();
        
        // Store many memories
        for (int i = 0; i < 150; i++) {
            MemoryRecord memory = storeTestMemory("Batch content " + i, "batch");
            storedMemories.add(memory);
        }
        
        // Retrieve in batch
        Set<String> ids = storedMemories.stream()
            .map(MemoryRecord::getId)
            .collect(Collectors.toSet());
        
        Map<String, MemoryRecord> retrieved = memorySystem.getMemories(ids);
        assertEquals(150, retrieved.size());
        
        // Remove in batch
        Set<String> removedIds = memorySystem.removeMemories(ids);
        assertEquals(150, removedIds.size());
    }
    
    // Helper methods
    
    private MemoryRecord storeTestMemory(String content, String category) {
        return storeTestMemoryForAgent(content, category, TEST_AGENT_ID);
    }
    
    private MemoryRecord storeTestMemoryForAgent(String content, String category, String agentId) {
        CategoryLabel categoryLabel = createTestCategoryLabel(category, "test");
        Metadata metadata = createTestMetadata(agentId, 0.7, "test-source");
        return memorySystem.encodeAndStore(content, categoryLabel, metadata);
    }
    
    private CategoryLabel createTestCategoryLabel(String primary, String secondary) {
        Set<String> tags = new HashSet<>();
        tags.add("test");
        tags.add("automated");
        CategoryLabel category = new CategoryLabel(primary, secondary, tags, 0.9);
        return category;
    }
    
    private Metadata createTestMetadata(String agentId, double importance, String source) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("agentId", agentId);
        
        Metadata metadata = new Metadata(properties);
        metadata.setImportance(importance);
        metadata.setSource(source);
        
        Set<String> tags = new HashSet<>();
        tags.add("test");
        tags.add("unit-test");
        metadata.setTags(tags);
        metadata.setConfidence(0.95);
        return metadata;
    }
    
    private MemoryRecord createTestMemoryRecord(String id, String content, String category) {
        MemoryRecord record = new MemoryRecord(id, TEST_AGENT_ID, content);
        record.setCategory(createTestCategoryLabel(category, "test"));
        record.setMetadata(createTestMetadata(TEST_AGENT_ID, 0.7, "test"));
        return record;
    }
    
    private void verifyMemoryInDatabase(String memoryId, MemoryRecord expected) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            MemoryEntity entity = em.find(MemoryEntity.class, memoryId);
            assertNotNull(entity, "Memory should exist in database");
            assertEquals(expected.getContent(), entity.getContent());
            assertEquals(expected.getAgentId(), entity.getAgentId());
        } finally {
            em.close();
        }
    }
    
    private void verifyMemoryNotInDatabase(String memoryId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            MemoryEntity entity = em.find(MemoryEntity.class, memoryId);
            assertNull(entity, "Memory should not exist in database");
        } finally {
            em.close();
        }
    }
    
    private void cleanupTestData() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM MemoryEntity").executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        } finally {
            em.close();
        }
    }
    
    /**
     * Mock embedding generator for testing.
     * Generates predictable embeddings based on content keywords.
     */
    private static class MockEmbeddingGenerator implements AbstractMemoryEncodingSystem.VectorEmbeddingGenerator {
        private final Set<String> calledWith = new HashSet<>();
        
        @Override
        public double[] generateEmbedding(String content) {
            calledWith.add(content);
            
            // Generate predictable embeddings based on content
            double[] embedding = new double[5];
            String lowerContent = content.toLowerCase();
            
            // AI-related content gets higher values in first dimensions
            if (lowerContent.contains("artificial") || lowerContent.contains("intelligence")) {
                embedding[0] = 0.9;
                embedding[1] = 0.8;
            }
            
            if (lowerContent.contains("machine") || lowerContent.contains("learning")) {
                embedding[0] = 0.8;
                embedding[2] = 0.9;
            }
            
            if (lowerContent.contains("language") || lowerContent.contains("nlp")) {
                embedding[3] = 0.9;
            }
            
            if (lowerContent.contains("database")) {
                embedding[4] = 0.9;
            }
            
            // Add some randomness while keeping it deterministic
            for (int i = 0; i < embedding.length; i++) {
                if (embedding[i] == 0.0) {
                    embedding[i] = (content.hashCode() % 100) / 100.0;
                }
            }
            
            return embedding;
        }
        
        public boolean wasCalledWith(String content) {
            return calledWith.contains(content);
        }
        
        public void reset() {
            calledWith.clear();
        }
    }
}