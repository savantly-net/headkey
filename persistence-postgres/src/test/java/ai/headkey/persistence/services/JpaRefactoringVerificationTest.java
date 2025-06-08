package ai.headkey.persistence.services;

import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.persistence.strategies.jpa.DefaultJpaSimilaritySearchStrategy;
import ai.headkey.persistence.strategies.jpa.JpaSimilaritySearchStrategy;
import ai.headkey.persistence.strategies.jpa.JpaSimilaritySearchStrategyFactory;
import ai.headkey.persistence.strategies.jpa.TextBasedJpaSimilaritySearchStrategy;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple verification test to ensure the refactored JPA similarity search strategy system works correctly.
 */
class JpaRefactoringVerificationTest {
    
    private static EntityManagerFactory entityManagerFactory;
    
    @BeforeAll
    static void setUpClass() {
        try {
            System.out.println("Creating EntityManagerFactory for verification tests...");
            entityManagerFactory = Persistence.createEntityManagerFactory("headkey-memory-h2-test");
            System.out.println("EntityManagerFactory created successfully");
        } catch (Exception e) {
            System.err.println("Failed to create EntityManagerFactory: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    @AfterAll
    static void tearDownClass() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
    }
    
    @Test
    void testJpaMemorySystemWithDefaultStrategy() {
        try {
            System.out.println("Testing JPA memory system with default strategy...");
            
            // Create memory system with automatic strategy detection
            JpaMemoryEncodingSystem memorySystem = new JpaMemoryEncodingSystem(entityManagerFactory);
            
            // Verify strategy was injected
            assertNotNull(memorySystem.getSimilaritySearchStrategy());
            assertNotNull(memorySystem.getSimilaritySearchStrategy().getStrategyName());
            System.out.println("Strategy: " + memorySystem.getSimilaritySearchStrategy().getStrategyName());
            
            // Store some test memories
            MemoryRecord memory1 = memorySystem.encodeAndStore("The quick brown fox jumps over the lazy dog", null, null);
            assertNotNull(memory1);
            assertNotNull(memory1.getId());
            assertEquals("The quick brown fox jumps over the lazy dog", memory1.getContent());
            
            MemoryRecord memory2 = memorySystem.encodeAndStore("Machine learning algorithms process data efficiently", null, null);
            assertNotNull(memory2);
            
            // Test similarity search
            List<MemoryRecord> results = memorySystem.searchSimilar("artificial intelligence", 5);
            assertNotNull(results);
            
            System.out.println("✓ Basic JPA memory system with strategy works");
            System.out.println("  Strategy: " + memorySystem.getSimilaritySearchStrategy().getStrategyName());
            System.out.println("  Search results: " + results.size());
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            fail("JPA memory system test should not throw exception: " + e.getMessage());
        }
    }
    
    @Test
    void testJpaMemorySystemWithCustomStrategy() {
        try {
            System.out.println("Testing JPA memory system with custom strategy...");
            
            // Create memory system with specific strategy
            TextBasedJpaSimilaritySearchStrategy customStrategy = new TextBasedJpaSimilaritySearchStrategy();
            JpaMemoryEncodingSystem memorySystem = new JpaMemoryEncodingSystem(
                entityManagerFactory, null, 100, true, 1000, 0.0, customStrategy);
            
            // Verify custom strategy was set
            assertTrue(memorySystem.getSimilaritySearchStrategy() instanceof TextBasedJpaSimilaritySearchStrategy);
            assertFalse(memorySystem.getSimilaritySearchStrategy().supportsVectorSearch());
            
            // Store test data
            memorySystem.encodeAndStore("Natural language processing techniques", null, null);
            memorySystem.encodeAndStore("Computer vision and image recognition", null, null);
            
            // Test search
            List<MemoryRecord> results = memorySystem.searchSimilar("language", 3);
            assertNotNull(results);
            
            System.out.println("✓ Custom strategy injection works");
            System.out.println("  Strategy: " + memorySystem.getSimilaritySearchStrategy().getStrategyName());
            System.out.println("  Supports vector search: " + memorySystem.getSimilaritySearchStrategy().supportsVectorSearch());
        } catch (Exception e) {
            System.err.println("Custom strategy test failed: " + e.getMessage());
            e.printStackTrace();
            fail("Custom strategy test should not throw exception: " + e.getMessage());
        }
    }
    
    @Test
    void testStrategyFactory() {
        try {
            System.out.println("Testing strategy factory...");
            
            // Test factory pattern
            var em = entityManagerFactory.createEntityManager();
            try {
                JpaSimilaritySearchStrategy strategy = JpaSimilaritySearchStrategyFactory.createStrategy(em);
                
                assertNotNull(strategy);
                assertNotNull(strategy.getStrategyName());
                
                // Should be either text-based or default strategy for H2
                assertTrue(strategy instanceof TextBasedJpaSimilaritySearchStrategy || 
                          strategy instanceof DefaultJpaSimilaritySearchStrategy);
                
                System.out.println("✓ Strategy factory works");
                System.out.println("  Created strategy: " + strategy.getStrategyName());
            } finally {
                em.close();
            }
        } catch (Exception e) {
            System.err.println("Strategy factory test failed: " + e.getMessage());
            e.printStackTrace();
            fail("Strategy factory test should not throw exception: " + e.getMessage());
        }
    }
    
    @Test
    void testMemorySystemFactory() {
        // Test factory methods
        JpaMemoryEncodingSystem system1 = JpaMemorySystemFactory.createSystem(entityManagerFactory);
        assertNotNull(system1);
        assertNotNull(system1.getSimilaritySearchStrategy());
        
        JpaMemoryEncodingSystem system2 = JpaMemorySystemFactory.builder()
            .entityManagerFactory(entityManagerFactory)
            .textBasedStrategy()
            .maxSimilaritySearchResults(500)
            .build();
        
        assertNotNull(system2);
        assertTrue(system2.getSimilaritySearchStrategy() instanceof TextBasedJpaSimilaritySearchStrategy);
        assertEquals(500, system2.getMaxSimilaritySearchResults());
        
        System.out.println("✓ Memory system factory works");
        System.out.println("  Builder pattern functional");
    }
    
    @Test
    void testBackwardsCompatibility() {
        // Ensure all existing functionality still works
        JpaMemoryEncodingSystem memorySystem = new JpaMemoryEncodingSystem(entityManagerFactory);
        
        // Store a memory
        MemoryRecord stored = memorySystem.encodeAndStore("Testing backwards compatibility", null, null);
        assertNotNull(stored);
        assertNotNull(stored.getId());
        
        // Retrieve the memory
        MemoryRecord retrieved = memorySystem.getMemory(stored.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals("Testing backwards compatibility", retrieved.getContent());
        
        // Update the memory
        MemoryRecord updated = new MemoryRecord(retrieved.getId(), retrieved.getAgentId(), "Updated content");
        updated.setCategory(retrieved.getCategory());
        updated.setMetadata(retrieved.getMetadata());
        updated.setCreatedAt(retrieved.getCreatedAt());
        updated.setRelevanceScore(retrieved.getRelevanceScore());
        updated.setVersion(retrieved.getVersion());
        
        memorySystem.updateMemory(updated);
        
        MemoryRecord afterUpdate = memorySystem.getMemory(stored.getId()).orElse(null);
        assertNotNull(afterUpdate);
        assertEquals("Updated content", afterUpdate.getContent());
        
        // Remove the memory
        memorySystem.removeMemory(stored.getId());
        assertTrue(memorySystem.getMemory(stored.getId()).isEmpty());
        
        System.out.println("✓ Backwards compatibility maintained");
        System.out.println("  CRUD operations work correctly");
    }
    
    @Test
    void testHealthAndStatistics() {
        JpaMemoryEncodingSystem memorySystem = new JpaMemoryEncodingSystem(entityManagerFactory);
        
        // Health check should work
        assertTrue(memorySystem.isHealthy());
        
        // Statistics should work
        Map<String, Object> stats = memorySystem.getStorageStatistics();
        assertNotNull(stats);
        assertTrue(stats.containsKey("implementationType"));
        
        System.out.println("✓ Health and statistics work");
        System.out.println("  System is healthy: " + memorySystem.isHealthy());
        System.out.println("  Statistics available: " + stats.keySet().size() + " metrics");
    }
}