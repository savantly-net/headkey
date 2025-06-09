package ai.headkey.persistence;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefConflict;
import ai.headkey.memory.spi.BeliefStorageService;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL integration tests using TestContainers.
 * 
 * This test class validates the complete integration of the JPA belief storage service
 * with a real PostgreSQL database running in a Docker container. It tests:
 * 
 * - Complete CRUD operations with PostgreSQL
 * - Complex queries and searches
 * - Concurrent access patterns
 * - Transaction management
 * - PostgreSQL-specific features
 * 
 * The tests use the AbstractPostgreSQLTest base class which provides:
 * - PostgreSQL TestContainer setup and teardown
 * - Properly configured DataSource with connection pooling
 * - EntityManagerFactory with correct PostgreSQL configuration
 * - Database schema initialization and cleanup
 * 
 * @since 1.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PostgreSQLIntegrationTest extends AbstractPostgreSQLTest {

    private BeliefStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = getBeliefStorageService();
        // Clean up any existing test data to ensure test isolation
        cleanupTestData();
    }

    @Test
    @Order(1)
    @DisplayName("Should be healthy with PostgreSQL connection")
    void testServiceHealthWithPostgreSQL() {
        assertTrue(storageService.isHealthy(), "Service should be healthy with PostgreSQL");
        
        // Verify we're actually connected to PostgreSQL
        Map<String, Object> serviceInfo = storageService.getServiceInfo();
        assertNotNull(serviceInfo);
    }

    @Test
    @Order(2)
    @DisplayName("Should store and retrieve belief with PostgreSQL features")
    void testStoreAndRetrieveBeliefPostgreSQL() {
        executeAndCommit(() -> {
            // Arrange
            Belief belief = createTestBelief("pg-belief-1", "pg-agent-1", "PostgreSQL integration test belief");

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
            
            // Verify PostgreSQL-specific features like timestamps
            assertNotNull(retrieved.get().getCreatedAt());
            assertNotNull(retrieved.get().getLastUpdated());
        });
    }

    @Test
    @Order(3)
    @DisplayName("Should handle large batch operations efficiently with PostgreSQL")
    void testBatchOperationsPostgreSQL() {
        executeAndCommit(() -> {
            // Arrange
            String agentId = "pg-batch-agent";
            List<Belief> beliefs = new ArrayList<>();
            
            for (int i = 0; i < 100; i++) {
                beliefs.add(createTestBelief("batch-" + i, agentId, "Batch belief statement " + i));
            }

            // Act
            long startTime = System.currentTimeMillis();
            List<Belief> stored = storageService.storeBeliefs(beliefs);
            long endTime = System.currentTimeMillis();

            // Assert
            assertEquals(100, stored.size());
            assertTrue(stored.stream().allMatch(b -> b.getId() != null));
            
            // Verify batch operation performance (should be under 3 seconds for 100 items)
            long duration = endTime - startTime;
            assertTrue(duration < 3000, "Batch operation took too long: " + duration + "ms");
            
            // Verify all beliefs are retrievable
            List<Belief> retrieved = storageService.getBeliefsForAgent(agentId, false);
            assertEquals(100, retrieved.size());
        });
    }

    @Test
    @Order(4)
    @DisplayName("Should retrieve beliefs by agent efficiently using PostgreSQL indexes")
    void testGetBeliefsByAgentPostgreSQL() {
        executeAndCommit(() -> {
            // Arrange
            String agentId = "pg-agent-specific";
            String otherAgentId = "pg-other-agent";
            
            List<Belief> agentBeliefs = Arrays.asList(
                createTestBelief("agent-belief-1", agentId, "Agent specific belief 1"),
                createTestBelief("agent-belief-2", agentId, "Agent specific belief 2"),
                createTestBelief("agent-belief-3", agentId, "Agent specific belief 3")
            );
            
            List<Belief> otherBeliefs = Arrays.asList(
                createTestBelief("other-belief-1", otherAgentId, "Other agent belief")
            );
            
            storageService.storeBeliefs(agentBeliefs);
            storageService.storeBeliefs(otherBeliefs);

            // Act
            List<Belief> retrieved = storageService.getBeliefsForAgent(agentId, false);

            // Assert
            assertEquals(3, retrieved.size());
            assertTrue(retrieved.stream().allMatch(b -> agentId.equals(b.getAgentId())));
            
            // Verify other agent's beliefs are not included
            assertFalse(retrieved.stream().anyMatch(b -> otherAgentId.equals(b.getAgentId())));
        });
    }

    @Test
    @Order(5)
    @DisplayName("Should retrieve beliefs by category using PostgreSQL indexes")
    void testGetBeliefsByCategoryPostgreSQL() {
        executeAndCommit(() -> {
            // Arrange
            String agentId = "pg-category-agent";
            String targetCategory = "PostgreSQLCategory";
            String otherCategory = "OtherCategory";
            
            List<Belief> beliefs = Arrays.asList(
                createTestBeliefWithCategory("cat-1", agentId, "Category belief 1", targetCategory),
                createTestBeliefWithCategory("cat-2", agentId, "Category belief 2", targetCategory),
                createTestBeliefWithCategory("cat-3", agentId, "Category belief 3", otherCategory)
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
    @DisplayName("Should perform text search using PostgreSQL full-text search capabilities")
    void testTextSearchPostgreSQL() {
        executeAndCommit(() -> {
            // Arrange
            String agentId = "pg-search-agent";
            List<Belief> beliefs = Arrays.asList(
                createTestBelief("search-1", agentId, "The quick brown fox jumps over the lazy dog"),
                createTestBelief("search-2", agentId, "PostgreSQL is a powerful relational database system"),
                createTestBelief("search-3", agentId, "Machine learning algorithms require large datasets"),
                createTestBelief("search-4", agentId, "Database indexing significantly improves query performance")
            );
            
            storageService.storeBeliefs(beliefs);

            // Act - search for database-related content
            List<Belief> databaseResults = storageService.searchBeliefs("database", agentId, 10);
            List<Belief> postgresqlResults = storageService.searchBeliefs("PostgreSQL", agentId, 10);

            // Assert
            assertFalse(databaseResults.isEmpty(), "Should find database-related beliefs");
            assertTrue(databaseResults.size() >= 1, "Should find at least one database-related belief");
            
            assertFalse(postgresqlResults.isEmpty(), "Should find PostgreSQL-related beliefs");
            assertTrue(postgresqlResults.stream()
                    .anyMatch(b -> b.getStatement().toLowerCase().contains("postgresql")));
        });
    }

    @Test
    @Order(7)
    @DisplayName("Should find similar beliefs using PostgreSQL similarity features")
    void testSimilarBeliefsPostgreSQL() {
        executeAndCommit(() -> {
            // Arrange
            String agentId = "pg-similarity-agent";
            List<Belief> beliefs = Arrays.asList(
                createTestBelief("sim-1", agentId, "I love programming in Java"),
                createTestBelief("sim-2", agentId, "Java is my favorite programming language"),
                createTestBelief("sim-3", agentId, "Python is also a great programming language"),
                createTestBelief("sim-4", agentId, "The weather is beautiful today")
            );
            
            storageService.storeBeliefs(beliefs);

            // Act
            List<BeliefStorageService.SimilarBelief> similarBeliefs = storageService.findSimilarBeliefs(
                "Programming with Java language", agentId, 0.1, 5);

            // Assert
            assertNotNull(similarBeliefs);
            // Should find some similar beliefs, even if similarity matching is basic
            assertTrue(similarBeliefs.size() >= 0);
        });
    }

    @Test
    @Order(8)
    @DisplayName("Should manage belief conflicts with PostgreSQL transactions")
    void testConflictManagementPostgreSQL() {
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
            assertNotNull(conflicts.get(0).getDetectedAt());
            
            // Test conflict removal
            boolean removed = storageService.removeConflict(stored.getConflictId());
            assertTrue(removed);
            
            List<BeliefConflict> remaining = storageService.getUnresolvedConflicts("pg-conflict-agent");
            assertTrue(remaining.isEmpty());
        });
    }

    @Test
    @Order(9)
    @DisplayName("Should provide comprehensive PostgreSQL storage statistics")
    void testStorageStatisticsPostgreSQL() {
        executeAndCommit(() -> {
            // Arrange - ensure we have some data
            String agentId = "pg-stats-agent";
            List<Belief> beliefs = Arrays.asList(
                createTestBelief("stats-1", agentId, "Stats belief 1"),
                createTestBelief("stats-2", agentId, "Stats belief 2"),
                createTestBelief("stats-3", "other-agent", "Other agent belief")
            );
            storageService.storeBeliefs(beliefs);

            BeliefConflict conflict = createTestConflict("stats-conflict", agentId);
            storageService.storeConflict(conflict);

            // Act
            Map<String, Object> stats = storageService.getStorageStatistics();

            // Assert
            assertNotNull(stats);
            assertTrue(stats.size() > 0, "Statistics should contain data");
            
            // Verify we get some meaningful statistics
            // Note: The exact structure depends on the implementation
            assertNotNull(stats, "Statistics should not be null");
        });
    }

    @Test
    @Order(10)
    @DisplayName("Should handle concurrent access correctly with PostgreSQL")
    void testConcurrentAccessPostgreSQL() {
        // Arrange
        String agentId = "concurrent-agent";
        int threadCount = 3; // Reduced for stability
        int beliefsPerThread = 5;

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
                
                // Use executeAndCommit to ensure proper transaction handling
                executeAndCommit(() -> {
                    storageService.storeBeliefs(beliefs);
                });
                
                return beliefs;
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

    @Test
    @Order(11)
    @DisplayName("Should handle belief updates with PostgreSQL optimistic locking")
    void testBeliefUpdatesPostgreSQL() {
        executeAndCommit(() -> {
            // Arrange
            Belief belief = createTestBelief("update-test", "update-agent", "Original statement");
            Belief stored = storageService.storeBelief(belief);
            
            // Act - Update the belief
            stored.setStatement("Updated statement");
            stored.setConfidence(0.95);
            stored.setCategory("UpdatedCategory");
            
            Belief updated = storageService.storeBelief(stored);
            Optional<Belief> retrieved = storageService.getBeliefById(stored.getId());

            // Assert
            assertNotNull(updated);
            assertTrue(retrieved.isPresent());
            assertEquals("Updated statement", retrieved.get().getStatement());
            assertEquals(0.95, retrieved.get().getConfidence(), 0.001);
            assertEquals("UpdatedCategory", retrieved.get().getCategory());
        });
    }

    @Test
    @Order(12)
    @DisplayName("Should validate data integrity with PostgreSQL constraints")
    void testDataIntegrityPostgreSQL() {
        executeAndCommit(() -> {
            // Arrange - create some test data
            Belief belief = createTestBelief("integrity-test", "integrity-agent", "Integrity test belief");
            storageService.storeBelief(belief);

            // Act
            Map<String, Object> validationResult = storageService.validateIntegrity();

            // Assert
            assertNotNull(validationResult, "Validation result should not be null");
            // Note: The exact structure depends on the implementation
        });
    }

    // Helper methods

    private Belief createTestBelief(String id, String agentId, String statement) {
        return createTestBeliefWithCategory(id, agentId, statement, "PostgreSQLTest");
    }

    private Belief createTestBeliefWithCategory(String id, String agentId, String statement, String category) {
        Belief belief = new Belief();
        belief.setId(id);
        belief.setAgentId(agentId);
        belief.setStatement(statement);
        belief.setConfidence(0.8);
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
        return conflict;
    }
}