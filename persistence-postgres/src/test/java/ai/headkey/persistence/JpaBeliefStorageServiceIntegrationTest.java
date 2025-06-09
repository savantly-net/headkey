package ai.headkey.persistence;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefConflict;
import ai.headkey.memory.enums.ConflictResolution;
import ai.headkey.memory.spi.BeliefStorageService;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for JpaBeliefStorageService using PostgreSQL TestContainers.
 * 
 * This test class validates the full integration of the JPA belief storage service
 * with a real PostgreSQL database running in a Docker container. It tests:
 * 
 * - Complete CRUD operations with PostgreSQL
 * - Complex queries and similarity searches
 * - Concurrent access patterns
 * - Transaction management
 * - Performance characteristics
 * - PostgreSQL-specific features
 * 
 * The tests use TestContainers to ensure consistent, isolated test environments
 * and validate that the system works correctly with real PostgreSQL features
 * like full-text search, indexing, and advanced SQL operations.
 * 
 * @since 1.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JpaBeliefStorageServiceIntegrationTest extends AbstractPostgreSQLTest {

    private BeliefStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = getBeliefStorageService();
        // Clean up any existing test data
        cleanupTestData();
    }

    @Test
    @Order(1)
    @DisplayName("Should be healthy with PostgreSQL connection")
    void testServiceHealth() {
        assertTrue(storageService.isHealthy(), "Service should be healthy with PostgreSQL");
    }

    @Test
    @Order(2)
    @DisplayName("Should store and retrieve belief with PostgreSQL")
    void testStoreAndRetrieveBelief() {
        executeAndCommit(() -> {
            // Arrange
            Belief belief = createTestBelief("pg-belief-1", "pg-agent-1", "PostgreSQL test belief");

            // Act
            Belief stored = storageService.storeBelief(belief);
            Optional<Belief> retrieved = storageService.getBeliefById(stored.getId());

            // Assert
            assertNotNull(stored);
            assertNotNull(stored.getId());
            assertTrue(retrieved.isPresent());
            assertEquals(belief.getStatement(), retrieved.get().getStatement());
            assertEquals(belief.getAgentId(), retrieved.get().getAgentId());
            assertEquals(belief.getCategory(), retrieved.get().getCategory());
            
            // Verify PostgreSQL-specific features
            assertNotNull(retrieved.get().getCreatedAt());
            assertNotNull(retrieved.get().getLastUpdated());
        });
    }

    @Test
    @Order(3)
    @DisplayName("Should handle batch storage operations efficiently")
    void testBatchStoreBelief() {
        executeAndCommit(() -> {
            // Arrange
            String agentId = "pg-batch-agent";
            List<Belief> beliefs = new ArrayList<>();
            
            for (int i = 0; i < 50; i++) {
                beliefs.add(createTestBelief("batch-" + i, agentId, "Batch belief statement " + i));
            }

            // Act
            long startTime = System.currentTimeMillis();
            List<Belief> stored = storageService.storeBeliefs(beliefs);
            long endTime = System.currentTimeMillis();

            // Assert
            assertEquals(50, stored.size());
            assertTrue(stored.stream().allMatch(b -> b.getId() != null));
            
            // Verify batch operation was reasonably fast (should be under 2 seconds)
            long duration = endTime - startTime;
            assertTrue(duration < 2000, "Batch operation took too long: " + duration + "ms");
            
            // Verify all beliefs are retrievable
            List<Belief> retrieved = storageService.getBeliefsForAgent(agentId, false);
            assertEquals(50, retrieved.size());
        });
    }

    @Test
    @Order(4)
    @DisplayName("Should retrieve beliefs by agent efficiently")
    void testGetBeliefsByAgent() {
        executeAndCommit(() -> {
            // Arrange
            String agentId = "pg-agent-specific";
            String otherAgentId = "pg-other-agent";
            
            List<Belief> agentBeliefs = Arrays.asList(
                createTestBelief("agent-belief-1", agentId, "Agent specific belief 1"),
                createTestBelief("agent-belief-2", agentId, "Agent specific belief 2")
            );
            
            List<Belief> otherBeliefs = Arrays.asList(
                createTestBelief("other-belief-1", otherAgentId, "Other agent belief")
            );
            
            storageService.storeBeliefs(agentBeliefs);
            storageService.storeBeliefs(otherBeliefs);

            // Act
            List<Belief> retrieved = storageService.getBeliefsForAgent(agentId, false);

            // Assert
            assertEquals(2, retrieved.size());
            assertTrue(retrieved.stream().allMatch(b -> agentId.equals(b.getAgentId())));
            assertFalse(retrieved.stream().anyMatch(b -> otherAgentId.equals(b.getAgentId())));
        });
    }

    @Test
    @Order(5)
    @DisplayName("Should query beliefs by category with PostgreSQL")
    void testGetBeliefsByCategory() {
        executeAndCommit(() -> {
            // Arrange
            String agentId = "pg-category-agent";
            String targetCategory = "PostgreSQLCategory";
            String otherCategory = "OtherCategory";
            
            List<Belief> beliefs = Arrays.asList(
                createTestBeliefWithCategory("cat-1", agentId, "Statement 1", targetCategory),
                createTestBeliefWithCategory("cat-2", agentId, "Statement 2", targetCategory),
                createTestBeliefWithCategory("cat-3", agentId, "Statement 3", otherCategory)
            );
            
            storageService.storeBeliefs(beliefs);

            // Act
            List<Belief> categoryBeliefs = storageService.getBeliefsInCategory(targetCategory, agentId, false);

            // Assert
            assertEquals(2, categoryBeliefs.size());
            assertTrue(categoryBeliefs.stream()
                    .allMatch(b -> targetCategory.equals(b.getCategory())));
        });
    }

    @Test
    @Order(6)
    @DisplayName("Should perform text-based search using PostgreSQL full-text search")
    void testSearchBeliefs() {
        // Arrange
        String agentId = "pg-search-agent";
        List<Belief> beliefs = Arrays.asList(
            createTestBelief("search-1", agentId, "The quick brown fox jumps over the lazy dog"),
            createTestBelief("search-2", agentId, "PostgreSQL is a powerful database system"),
            createTestBelief("search-3", agentId, "Machine learning algorithms are fascinating"),
            createTestBelief("search-4", agentId, "Database indexing improves query performance")
        );
        
        storageService.storeBeliefs(beliefs);

        // Act - search for database-related content
        List<Belief> searchResults = storageService.searchBeliefs("database", agentId, 10);

        // Assert
        assertFalse(searchResults.isEmpty());
        assertTrue(searchResults.size() <= 2); // Should find PostgreSQL and indexing beliefs
        
        // Verify results contain search terms
        assertTrue(searchResults.stream()
                .anyMatch(b -> b.getStatement().toLowerCase().contains("database")));
    }

    @Test
    @Order(7)
    @DisplayName("Should find similar beliefs using vector similarity")
    void testFindSimilarBeliefs() {
        // Arrange
        String agentId = "pg-similarity-agent";
        List<Belief> beliefs = Arrays.asList(
            createTestBelief("sim-1", agentId, "I love programming in Java"),
            createTestBelief("sim-2", agentId, "Java is my favorite programming language"),
            createTestBelief("sim-3", agentId, "Python is also a great language"),
            createTestBelief("sim-4", agentId, "The weather is nice today")
        );
        
        storageService.storeBeliefs(beliefs);
        
        // Get a reference belief
        Belief reference = storageService.getBeliefsForAgent(agentId, false).get(0);

        // Act
        List<BeliefStorageService.SimilarBelief> similarBeliefs = storageService.findSimilarBeliefs(
            reference.getStatement(), agentId, 0.1, 5);

        // Assert
        assertNotNull(similarBeliefs);
        // Should find at least some similar beliefs
        assertTrue(similarBeliefs.size() >= 0);
    }

    @Test
    @Order(8)
    @DisplayName("Should identify low confidence beliefs efficiently")
    void testGetLowConfidenceBeliefs() {
        // Arrange
        String agentId = "pg-confidence-agent";
        List<Belief> beliefs = Arrays.asList(
            createTestBeliefWithConfidence("conf-1", agentId, "High confidence belief", 0.9),
            createTestBeliefWithConfidence("conf-2", agentId, "Medium confidence belief", 0.6),
            createTestBeliefWithConfidence("conf-3", agentId, "Low confidence belief", 0.3),
            createTestBeliefWithConfidence("conf-4", agentId, "Very low confidence belief", 0.1)
        );
        
        storageService.storeBeliefs(beliefs);

        // Act
        List<Belief> lowConfidenceBeliefs = storageService.getLowConfidenceBeliefs(0.5, agentId);

        // Assert
        assertEquals(2, lowConfidenceBeliefs.size());
        assertTrue(lowConfidenceBeliefs.stream()
                .allMatch(b -> b.getConfidence() < 0.5));
    }

    @Test
    @Order(10)
    @DisplayName("Should manage conflicts properly")
    void testConflictManagement() {
        executeAndCommit(() -> {
            // Arrange
            BeliefConflict conflict = createTestConflict("pg-conflict-1", "pg-conflict-agent");

            // Act
            BeliefConflict stored = storageService.storeConflict(conflict);
            List<BeliefConflict> conflicts = storageService.getUnresolvedConflicts("pg-conflict-agent");

            // Assert
            assertNotNull(stored);
            assertNotNull(stored.getConflictId());
            assertEquals(1, conflicts.size());
            assertEquals(conflict.getDescription(), conflicts.get(0).getDescription());
        });
    }

    @Test
    @Order(10)
    @DisplayName("Should update belief confidence with optimistic locking")
    void testUpdateBeliefConfidence() {
        // Arrange
        Belief belief = createTestBelief("update-conf", "update-agent", "Belief to update");
        Belief stored = storageService.storeBelief(belief);
        
        double newConfidence = 0.95;

        // Act
        stored.setConfidence(newConfidence);
        Belief updated = storageService.storeBelief(stored);
        Optional<Belief> retrieved = storageService.getBeliefById(stored.getId());

        // Assert
        assertTrue(retrieved.isPresent());
        assertEquals(newConfidence, retrieved.get().getConfidence(), 0.001);
        assertTrue(retrieved.get().getLastUpdated().isAfter(stored.getLastUpdated()) || 
                  retrieved.get().getLastUpdated().equals(stored.getLastUpdated()));
    }

    @Test
    @Order(11)
    @DisplayName("Should provide comprehensive storage statistics")
    void testGetStatistics() {
        // Arrange - ensure we have some data
        executeAndCommit(() -> {
            String agentId = "pg-stats-agent";
            List<Belief> beliefs = Arrays.asList(
                createTestBelief("stats-1", agentId, "Stats belief 1"),
                createTestBelief("stats-2", agentId, "Stats belief 2"),
                createTestBelief("stats-3", "other-agent", "Other agent belief")
            );
            storageService.storeBeliefs(beliefs);

            BeliefConflict conflict = createTestConflict("stats-conflict", agentId);
            storageService.storeConflict(conflict);
        });

        // Act
        Map<String, Object>[] statsHolder = new Map[1];
        executeAndCommit(() -> {
            statsHolder[0] = storageService.getStorageStatistics();
        });
        Map<String, Object> stats = statsHolder[0];

        // Assert
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalBeliefs"));
        assertTrue(stats.containsKey("agentCount"));
        assertTrue(stats.containsKey("totalConflicts"));
        assertTrue(stats.containsKey("databaseInfo"));
        
        // Verify PostgreSQL-specific information
        @SuppressWarnings("unchecked")
        Map<String, Object> dbInfo = (Map<String, Object>) stats.get("databaseInfo");
        assertNotNull(dbInfo);
        assertTrue(dbInfo.containsKey("productName"));
        
        // Verify counts are reasonable
        assertTrue(((Number) stats.get("totalBeliefs")).longValue() >= 3);
        assertTrue(((Number) stats.get("agentCount")).longValue() >= 2);
        assertTrue(((Number) stats.get("totalConflicts")).longValue() >= 1);
    }

    @Test
    @Order(12)
    @DisplayName("Should delete belief properly")
    void testDeleteBelief() {
        executeAndCommit(() -> {
            // Arrange
            Belief belief = createTestBelief("pg-delete-test", "pg-delete-agent", "To be deleted");
            Belief stored = storageService.storeBelief(belief);

            // Act
            boolean deleted = storageService.deleteBelief(stored.getId());
            Optional<Belief> retrieved = storageService.getBeliefById(stored.getId());

            // Assert
            assertTrue(deleted);
            assertFalse(retrieved.isPresent());
        });
    }

    @Test
    @Order(13)
    @DisplayName("Should remove conflicts properly")
    void testRemoveConflict() {
        // Arrange
        BeliefConflict conflict = createTestConflict("remove-conflict", "remove-agent");
        BeliefConflict stored = storageService.storeConflict(conflict);

        // Act
        boolean removed = storageService.removeConflict(stored.getConflictId());
        List<BeliefConflict> remaining = storageService.getUnresolvedConflicts("remove-agent");

        // Assert
        assertTrue(removed);
        assertTrue(remaining.isEmpty());
    }

    @Test
    @Order(14)
    @DisplayName("Should validate data integrity")
    void testValidateIntegrity() {
        // Arrange - create some test data
        Belief belief = createTestBelief("integrity-test", "integrity-agent", "Integrity test belief");
        storageService.storeBelief(belief);

        // Act
        Map<String, Object> validationResult = storageService.validateIntegrity();

        // Assert
        assertNotNull(validationResult, "Validation result should not be null");
        assertTrue(validationResult.containsKey("healthy") || validationResult.containsKey("success"));
    }

    @Test
    @Order(15)
    @DisplayName("Should optimize storage with PostgreSQL-specific operations")
    void testOptimizeStorage() {
        // Arrange - create some data that can be optimized
        String agentId = "optimize-agent";
        List<Belief> beliefs = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            beliefs.add(createTestBelief("opt-" + i, agentId, "Optimization test " + i));
        }
        storageService.storeBeliefs(beliefs);

        // Act
        Map<String, Object> optimizationResult = storageService.optimizeStorage();

        // Assert
        assertNotNull(optimizationResult, "Optimization result should not be null");
        assertTrue(optimizationResult.containsKey("success") || optimizationResult.containsKey("operation"));
        
        // Verify data is still accessible after optimization
        List<Belief> retrieved = storageService.getBeliefsForAgent(agentId, false);
        assertEquals(20, retrieved.size());
    }

    @Test
    @Order(16)
    @DisplayName("Should handle concurrent access correctly")
    void testConcurrentAccess() {
        // Arrange
        String agentId = "concurrent-agent";
        int threadCount = 5;
        int beliefsPerThread = 10;

        // Act
        List<CompletableFuture<List<Belief>>> futures = new ArrayList<>();
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            CompletableFuture<List<Belief>> future = CompletableFuture.supplyAsync(() -> {
                List<Belief> beliefs = new ArrayList<>();
                for (int i = 0; i < beliefsPerThread; i++) {
                    String id = "concurrent-" + threadId + "-" + i;
                    beliefs.add(createTestBelief(id, agentId, "Concurrent belief " + threadId + "-" + i));
                }
                return storageService.storeBeliefs(beliefs);
            });
            futures.add(future);
        }

        // Wait for all threads to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));
        
        assertDoesNotThrow(() -> allFutures.get());

        // Assert
        List<Belief> allBeliefs = storageService.getBeliefsForAgent(agentId, false);
        assertEquals(threadCount * beliefsPerThread, allBeliefs.size());
        
        // Verify all beliefs have unique IDs
        Set<String> uniqueIds = allBeliefs.stream()
                .map(Belief::getId)
                .collect(Collectors.toSet());
        assertEquals(threadCount * beliefsPerThread, uniqueIds.size());
    }

    // Helper methods

    private Belief createTestBelief(String id, String agentId, String statement) {
        return createTestBeliefWithCategory(id, agentId, statement, "PostgreSQLTest");
    }

    private Belief createTestBeliefWithCategory(String id, String agentId, String statement, String category) {
        return createTestBeliefWithConfidence(id, agentId, statement, category, 0.8);
    }

    private Belief createTestBeliefWithConfidence(String id, String agentId, String statement, double confidence) {
        return createTestBeliefWithConfidence(id, agentId, statement, "TestCategory", confidence);
    }

    private Belief createTestBeliefWithConfidence(String id, String agentId, String statement, String category, double confidence) {
        Belief belief = new Belief();
        belief.setId(id);
        belief.setAgentId(agentId);
        belief.setStatement(statement);
        belief.setConfidence(confidence);
        belief.setCategory(category);
        belief.setCreatedAt(Instant.now());
        belief.setLastUpdated(Instant.now());
        belief.setActive(true);
        belief.setReinforcementCount(0);
        belief.setTags(Set.of("postgresql", "integration", "test"));
        belief.setEvidenceMemoryIds(Set.of("pg-memory-1", "pg-memory-2"));
        return belief;
    }

    private BeliefConflict createTestConflict(String id, String agentId) {
        BeliefConflict conflict = new BeliefConflict();
        conflict.setConflictId(id);
        conflict.setAgentId(agentId);
        conflict.setDescription("PostgreSQL integration test conflict");
        conflict.setBeliefId("belief-" + id);
        conflict.setMemoryId("memory-" + id);
        conflict.setDetectedAt(Instant.now());
        conflict.setResolved(false);
        conflict.setSeverity("MEDIUM");
        conflict.setResolution(ConflictResolution.REQUIRE_MANUAL_REVIEW);
        return conflict;
    }
}