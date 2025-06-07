package ai.headkey.memory.implementations;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.memory.exceptions.MemoryNotFoundException;
import ai.headkey.memory.exceptions.StorageException;
import ai.headkey.memory.interfaces.MemoryEncodingSystem;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for JdbcMemoryEncodingSystem.
 * 
 * Tests both HSQLDB (in-memory) and basic functionality that would work
 * across different database backends. Uses mock embedding generator for testing.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcMemoryEncodingSystemTest {
    
    private MemoryEncodingSystem memorySystem;
    private JdbcMemoryEncodingSystem.VectorEmbeddingGenerator mockEmbeddingGenerator;
    
    @BeforeEach
    void setUp() {
        // Create mock embedding generator that creates consistent test vectors
        mockEmbeddingGenerator = JdbcMemorySystemFactory.createMockEmbeddingGenerator(1536);
        
        // Create in-memory HSQLDB system for testing
        memorySystem = JdbcMemorySystemFactory.createInMemoryTestSystem(mockEmbeddingGenerator);
    }
    
    @AfterEach
    void tearDown() {
        // Cleanup is automatic for in-memory database
        memorySystem = null;
    }
    
    @Test
    @Order(1)
    void testSystemInitialization() {
        assertNotNull(memorySystem);
        assertTrue(memorySystem.isHealthy());
        
        Map<String, Object> stats = memorySystem.getStorageStatistics();
        assertNotNull(stats);
        assertEquals(0L, stats.get("totalMemories"));
    }
    
    @Test
    @Order(2)
    void testEncodeAndStore_ValidInput() {
        // Arrange
        String content = "This is a test memory about artificial intelligence and machine learning.";
        CategoryLabel category = createTestCategory("technology", "ai", Arrays.asList("machine-learning", "test"));
        Metadata metadata = createTestMetadata("test-agent-1", "unit-test");
        
        // Act
        MemoryRecord result = memorySystem.encodeAndStore(content, category, metadata);
        
        // Assert
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(content, result.getContent());
        assertEquals("test-agent-1", result.getAgentId());
        assertEquals(category.getPrimary(), result.getCategory().getPrimary());
        assertEquals(category.getSecondary(), result.getCategory().getSecondary());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getLastAccessed());
        assertEquals(1.0, result.getRelevanceScore());
        assertEquals(1L, result.getVersion());
    }
    
    @Test
    @Order(3)
    void testEncodeAndStore_InvalidInput() {
        CategoryLabel category = createTestCategory("test", "test", Arrays.asList("test"));
        Metadata metadata = createTestMetadata("test-agent", "test");
        
        // Test null content
        assertThrows(IllegalArgumentException.class, () -> 
            memorySystem.encodeAndStore(null, category, metadata));
        
        // Test empty content
        assertThrows(IllegalArgumentException.class, () -> 
            memorySystem.encodeAndStore("", category, metadata));
        
        // Test null metadata
        assertThrows(IllegalArgumentException.class, () -> 
            memorySystem.encodeAndStore("content", category, null));
        
        // Test metadata with null agent ID
        Metadata invalidMetadata = new Metadata();
        assertThrows(IllegalArgumentException.class, () -> 
            memorySystem.encodeAndStore("content", category, invalidMetadata));
    }
    
    @Test
    @Order(4)
    void testGetMemory_ExistingMemory() {
        // Arrange - store a memory first
        String content = "Test memory for retrieval";
        CategoryLabel category = createTestCategory("test", "retrieval", Arrays.asList("get"));
        Metadata metadata = createTestMetadata("test-agent-2", "retrieval-test");
        
        MemoryRecord stored = memorySystem.encodeAndStore(content, category, metadata);
        
        // Act
        Optional<MemoryRecord> retrieved = memorySystem.getMemory(stored.getId());
        
        // Assert
        assertTrue(retrieved.isPresent());
        MemoryRecord memory = retrieved.get();
        assertEquals(stored.getId(), memory.getId());
        assertEquals(stored.getContent(), memory.getContent());
        assertEquals(stored.getAgentId(), memory.getAgentId());
        // Last accessed should be updated
        assertTrue(memory.getLastAccessed().isAfter(stored.getLastAccessed()) || 
                  memory.getLastAccessed().equals(stored.getLastAccessed()));
    }
    
    @Test
    @Order(5)
    void testGetMemory_NonExistentMemory() {
        // Act
        Optional<MemoryRecord> result = memorySystem.getMemory("non-existent-id");
        
        // Assert
        assertFalse(result.isPresent());
    }
    
    @Test
    @Order(6)
    void testGetMemory_InvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> 
            memorySystem.getMemory(null));
        
        assertThrows(IllegalArgumentException.class, () -> 
            memorySystem.getMemory(""));
    }
    
    @Test
    @Order(7)
    void testGetMemories_MultipleIds() {
        // Arrange - store multiple memories
        List<MemoryRecord> storedMemories = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String content = "Test memory " + i;
            CategoryLabel category = createTestCategory("test", "batch", Arrays.asList("multi"));
            Metadata metadata = createTestMetadata("test-agent-3", "batch-test");
            
            storedMemories.add(memorySystem.encodeAndStore(content, category, metadata));
        }
        
        Set<String> requestedIds = storedMemories.stream()
            .map(MemoryRecord::getId)
            .collect(Collectors.toSet());
        
        // Act
        Map<String, MemoryRecord> retrieved = memorySystem.getMemories(requestedIds);
        
        // Assert
        assertEquals(3, retrieved.size());
        for (MemoryRecord stored : storedMemories) {
            assertTrue(retrieved.containsKey(stored.getId()));
            assertEquals(stored.getContent(), retrieved.get(stored.getId()).getContent());
        }
    }
    
    @Test
    @Order(8)
    void testUpdateMemory_ExistingMemory() {
        // Arrange - store a memory first
        String originalContent = "Original content";
        CategoryLabel originalCategory = createTestCategory("original", "test", Arrays.asList("update"));
        Metadata originalMetadata = createTestMetadata("test-agent-4", "update-test");
        
        MemoryRecord stored = memorySystem.encodeAndStore(originalContent, originalCategory, originalMetadata);
        
        // Modify the memory
        stored.setContent("Updated content");
        stored.setRelevanceScore(0.8);
        CategoryLabel updatedCategory = createTestCategory("updated", "test", Arrays.asList("modified"));
        stored.setCategory(updatedCategory);
        
        // Act
        MemoryRecord updated = memorySystem.updateMemory(stored);
        
        // Assert
        assertNotNull(updated);
        assertEquals("Updated content", updated.getContent());
        assertEquals(0.8, updated.getRelevanceScore());
        assertEquals("updated", updated.getCategory().getPrimary());
        assertEquals(2L, updated.getVersion()); // Version should be incremented
    }
    
    @Test
    @Order(9)
    void testUpdateMemory_NonExistentMemory() {
        // Arrange
        MemoryRecord nonExistent = new MemoryRecord();
        nonExistent.setId("non-existent-id");
        nonExistent.setContent("Some content");
        
        // Act & Assert
        assertThrows(StorageException.class, () -> 
            memorySystem.updateMemory(nonExistent));
    }
    
    @Test
    @Order(10)
    void testRemoveMemory_ExistingMemory() {
        // Arrange - store a memory first
        String content = "Memory to be removed";
        CategoryLabel category = createTestCategory("test", "removal", Arrays.asList("delete"));
        Metadata metadata = createTestMetadata("test-agent-5", "removal-test");
        
        MemoryRecord stored = memorySystem.encodeAndStore(content, category, metadata);
        
        // Act
        boolean removed = memorySystem.removeMemory(stored.getId());
        
        // Assert
        assertTrue(removed);
        
        // Verify memory is actually removed
        Optional<MemoryRecord> retrieved = memorySystem.getMemory(stored.getId());
        assertFalse(retrieved.isPresent());
    }
    
    @Test
    @Order(11)
    void testRemoveMemory_NonExistentMemory() {
        // Act
        boolean removed = memorySystem.removeMemory("non-existent-id");
        
        // Assert
        assertFalse(removed);
    }
    
    @Test
    @Order(12)
    void testRemoveMemories_MultipleBatch() {
        // Arrange - store multiple memories
        Set<String> storedIds = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            String content = "Memory to remove " + i;
            CategoryLabel category = createTestCategory("test", "batch-removal", Arrays.asList("batch"));
            Metadata metadata = createTestMetadata("test-agent-6", "batch-removal-test");
            
            MemoryRecord stored = memorySystem.encodeAndStore(content, category, metadata);
            storedIds.add(stored.getId());
        }
        
        // Act
        Set<String> removedIds = memorySystem.removeMemories(storedIds);
        
        // Assert
        assertEquals(storedIds.size(), removedIds.size());
        assertTrue(removedIds.containsAll(storedIds));
        
        // Verify memories are actually removed
        Map<String, MemoryRecord> retrieved = memorySystem.getMemories(storedIds);
        assertTrue(retrieved.isEmpty());
    }
    
    @Test
    @Order(13)
    void testSearchSimilar_WithResults() {
        // Arrange - store memories with similar content
        String[] contents = {
            "Machine learning is a subset of artificial intelligence",
            "Deep learning uses neural networks with multiple layers",
            "Natural language processing helps computers understand text",
            "Computer vision enables machines to interpret visual information"
        };
        
        for (String content : contents) {
            CategoryLabel category = createTestCategory("technology", "ai", Arrays.asList("ml", "ai"));
            Metadata metadata = createTestMetadata("test-agent-7", "similarity-test");
            memorySystem.encodeAndStore(content, category, metadata);
        }
        
        // Act
        List<MemoryRecord> results = memorySystem.searchSimilar("machine learning artificial intelligence", 2);
        
        // Assert
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.size() <= 2); // Respects limit
        
        // Results should be ordered by relevance (assuming text-based matching for HSQLDB)
        for (MemoryRecord result : results) {
            assertNotNull(result.getContent());
            assertNotNull(result.getId());
        }
    }
    
    @Test
    @Order(14)
    void testSearchSimilar_NoResults() {
        // Act - search for content that doesn't exist
        List<MemoryRecord> results = memorySystem.searchSimilar("completely unrelated content xyz", 10);
        
        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
    
    @Test
    @Order(15)
    void testGetMemoriesForAgent() {
        // Arrange - store memories for different agents
        String agentId = "test-agent-8";
        
        for (int i = 0; i < 3; i++) {
            String content = "Memory for agent 8, entry " + i;
            CategoryLabel category = createTestCategory("test", "agent", Arrays.asList("specific"));
            Metadata metadata = createTestMetadata(agentId, "agent-test");
            memorySystem.encodeAndStore(content, category, metadata);
        }
        
        // Store memory for different agent
        CategoryLabel category = createTestCategory("test", "other", Arrays.asList("different"));
        Metadata otherMetadata = createTestMetadata("other-agent", "agent-test");
        memorySystem.encodeAndStore("Memory for other agent", category, otherMetadata);
        
        // Act
        List<MemoryRecord> results = memorySystem.getMemoriesForAgent(agentId, 10);
        
        // Assert
        assertNotNull(results);
        assertEquals(3, results.size());
        for (MemoryRecord result : results) {
            assertEquals(agentId, result.getAgentId());
        }
    }
    
    @Test
    @Order(16)
    void testGetMemoriesInCategory() {
        // Arrange - store memories in different categories
        String category = "test-category";
        
        for (int i = 0; i < 2; i++) {
            String content = "Memory in test category " + i;
            CategoryLabel categoryLabel = createTestCategory(category, "sub", Arrays.asList("cat"));
            Metadata metadata = createTestMetadata("test-agent-9", "category-test");
            memorySystem.encodeAndStore(content, categoryLabel, metadata);
        }
        
        // Store memory in different category
        CategoryLabel differentCategory = createTestCategory("different-category", "sub", Arrays.asList("other"));
        Metadata metadata = createTestMetadata("test-agent-9", "category-test");
        memorySystem.encodeAndStore("Memory in different category", differentCategory, metadata);
        
        // Act
        List<MemoryRecord> results = memorySystem.getMemoriesInCategory(category, null, 10);
        
        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        for (MemoryRecord result : results) {
            assertTrue(category.equals(result.getCategory().getPrimary()) || 
                      category.equals(result.getCategory().getSecondary()));
        }
    }
    
    @Test
    @Order(17)
    void testGetOldMemories() throws InterruptedException {
        // Arrange - store a memory and wait
        String content = "Old memory";
        CategoryLabel category = createTestCategory("test", "old", Arrays.asList("age"));
        Metadata metadata = createTestMetadata("test-agent-10", "age-test");
        
        memorySystem.encodeAndStore(content, category, metadata);
        
        // Wait a bit to ensure time difference
        Thread.sleep(1000);
        
        // Act - look for memories older than 1 second
        List<MemoryRecord> results = memorySystem.getOldMemories(0, null, 10);
        
        // Assert
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }
    
    @Test
    @Order(18)
    void testGetStorageStatistics() {
        // Act
        Map<String, Object> stats = memorySystem.getStorageStatistics();
        
        // Assert
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalMemories"));
        assertTrue(stats.containsKey("totalOperations"));
        assertTrue(stats.containsKey("totalSearches"));
        assertTrue(stats.containsKey("uptime"));
        assertTrue(stats.containsKey("strategyName"));
        assertTrue(stats.containsKey("supportsVectorSearch"));
        
        // Should have some memories from previous tests
        Long totalMemories = (Long) stats.get("totalMemories");
        assertTrue(totalMemories >= 0);
    }
    
    @Test
    @Order(19)
    void testGetAgentStatistics() {
        // Arrange - ensure we have some memories for a specific agent
        String agentId = "stats-test-agent";
        
        for (int i = 0; i < 2; i++) {
            String content = "Memory for statistics test " + i;
            CategoryLabel category = createTestCategory("stats", "test", Arrays.asList("metrics"));
            Metadata metadata = createTestMetadata(agentId, "stats-test");
            memorySystem.encodeAndStore(content, category, metadata);
        }
        
        // Act
        Map<String, Object> stats = memorySystem.getAgentStatistics(agentId);
        
        // Assert
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalMemories"));
        assertTrue(stats.containsKey("categoryBreakdown"));
        
        Long totalMemories = (Long) stats.get("totalMemories");
        assertTrue(totalMemories >= 2);
    }
    
    @Test
    @Order(20)
    void testOptimize() {
        // Act
        Map<String, Object> results = memorySystem.optimize(false);
        
        // Assert
        assertNotNull(results);
        assertTrue(results.containsKey("optimizationDurationMs"));
        assertTrue(results.containsKey("timestamp"));
        
        // Test with vacuum
        Map<String, Object> vacuumResults = memorySystem.optimize(true);
        assertNotNull(vacuumResults);
    }
    
    @Test
    @Order(21)
    void testGetCapacityInfo() {
        // Act
        Map<String, Object> info = memorySystem.getCapacityInfo();
        
        // Assert
        assertNotNull(info);
        assertTrue(info.containsKey("currentMemoryCount"));
        assertTrue(info.containsKey("operationsPerSecond"));
        assertTrue(info.containsKey("estimatedDailyGrowth"));
        
        Long currentCount = (Long) info.get("currentMemoryCount");
        assertTrue(currentCount >= 0);
    }
    
    @Test
    @Order(22)
    void testConcurrentOperations() throws InterruptedException {
        // Test basic thread safety with concurrent operations
        final String agentId = "concurrent-test-agent";
        final int threadCount = 5;
        final int operationsPerThread = 10;
        
        Thread[] threads = new Thread[threadCount];
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    String content = String.format("Concurrent memory thread-%d operation-%d", threadId, i);
                    CategoryLabel category = createTestCategory("concurrent", "test", Arrays.asList("thread"));
                    Metadata metadata = createTestMetadata(agentId, "concurrent-test");
                    
                    MemoryRecord stored = memorySystem.encodeAndStore(content, category, metadata);
                    assertNotNull(stored);
                    
                    // Try to retrieve it immediately
                    Optional<MemoryRecord> retrieved = memorySystem.getMemory(stored.getId());
                    assertTrue(retrieved.isPresent());
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(10000); // 10 second timeout
        }
        
        // Verify all memories were stored
        List<MemoryRecord> agentMemories = memorySystem.getMemoriesForAgent(agentId, 0);
        assertEquals(threadCount * operationsPerThread, agentMemories.size());
    }
    
    // Helper methods
    
    private CategoryLabel createTestCategory(String primary, String secondary, List<String> tags) {
        CategoryLabel category = new CategoryLabel();
        category.setPrimary(primary);
        category.setSecondary(secondary);
        category.setTags(new HashSet<>(tags));
        category.setConfidence(0.9);
        return category;
    }
    
    private Metadata createTestMetadata(String agentId, String source) {
        Metadata metadata = new Metadata();
        metadata.setProperty("agentId", agentId);
        metadata.setSource(source);
        metadata.setImportance(0.8);
        metadata.setProperty("timestamp", Instant.now());
        return metadata;
    }
}