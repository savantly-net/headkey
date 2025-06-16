package ai.headkey.persistence;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefConflict;
import ai.headkey.memory.dto.BeliefUpdateResult;
import ai.headkey.memory.enums.ConflictResolution;
import ai.headkey.memory.interfaces.BeliefStorageService;
import ai.headkey.persistence.factory.JpaBeliefStorageServiceFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JpaBeliefStorageService using H2 in-memory database.
 * 
 * These tests use H2 for fast execution and focus on testing the service logic
 * without external dependencies. For full PostgreSQL integration tests,
 * see JpaBeliefStorageServiceIntegrationTest.
 * 
 * Features tested:
 * - Basic CRUD operations for beliefs
 * - Conflict detection and management
 * - Service health checks
 * - Error handling and validation
 * - Transaction management
 * 
 * @since 1.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JpaBeliefStorageServiceUnitTest {

    private static EntityManagerFactory entityManagerFactory;
    private static BeliefStorageService storageService;
    private EntityManager entityManager;

    @BeforeAll
    static void setUpClass() {
        // Create H2 EntityManagerFactory for testing
        entityManagerFactory = Persistence.createEntityManagerFactory("headkey-beliefs-h2-test");
        
        // Create storage service with H2 database
        storageService = JpaBeliefStorageServiceFactory.builder()
                .withEntityManagerFactory(entityManagerFactory)
                .withPersistenceUnitName("headkey-beliefs-h2-test")
                .withAutoCreateSchema(true)
                .withStatistics(false)
                .build();
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
        // Clean up any existing data
        cleanupDatabase();
    }

    @AfterEach
    void tearDown() {
        if (entityManager != null) {
            entityManager.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should be healthy after initialization")
    void testServiceHealth() {
        assertTrue(storageService.isHealthy(), "Service should be healthy");
    }

    @Test
    @Order(2)
    @DisplayName("Should store and retrieve a belief")
    void testStoreAndRetrieveBelief() {
        executeInTransaction(() -> {
            // Arrange
            Belief belief = createTestBelief("test-belief-1", "test-agent-1", "Test belief statement");

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
        });
    }

    @Test
    @Order(3)
    @DisplayName("Should handle non-existent belief lookup")
    void testGetNonExistentBelief() {
        // Act
        Optional<Belief> result = storageService.getBeliefById("non-existent-id");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @Order(4)
    @DisplayName("Should store multiple beliefs")
    void testStoreBeliefsForAgent() {
        executeInTransaction(() -> {
            // Arrange
            String agentId = "test-agent-2";
            List<Belief> beliefs = Arrays.asList(
                createTestBelief("belief-1", agentId, "First belief"),
                createTestBelief("belief-2", agentId, "Second belief"),
                createTestBelief("belief-3", agentId, "Third belief")
            );

            // Act
            List<Belief> stored = storageService.storeBeliefs(beliefs);
            List<Belief> retrieved = storageService.getBeliefsForAgent(agentId, false);

            // Assert
            assertEquals(3, stored.size());
            assertEquals(3, retrieved.size());
            
            // Verify all beliefs are present
            Set<String> storedStatements = stored.stream()
                    .map(Belief::getStatement)
                    .collect(java.util.stream.Collectors.toSet());
            Set<String> retrievedStatements = retrieved.stream()
                    .map(Belief::getStatement)
                    .collect(java.util.stream.Collectors.toSet());
            
            assertEquals(storedStatements, retrievedStatements);
        });
    }

    @Test
    @Order(5)
    @DisplayName("Should update existing belief")
    void testUpdateBelief() {
        executeInTransaction(() -> {
            // Arrange
            Belief original = createTestBelief("belief-update", "agent-update", "Original statement");
            Belief stored = storageService.storeBelief(original);
            
            // Modify the belief
            stored.setStatement("Updated statement");
            stored.setConfidence(0.9);
            stored.setCategory("UpdatedCategory");

            // Act
            Belief updated = storageService.storeBelief(stored);
            Optional<Belief> retrieved = storageService.getBeliefById(stored.getId());

            // Assert
            assertNotNull(updated);
            assertTrue(retrieved.isPresent());
            assertEquals("Updated statement", retrieved.get().getStatement());
            assertEquals(0.9, retrieved.get().getConfidence(), 0.001);
            assertEquals("UpdatedCategory", retrieved.get().getCategory());
        });
    }

    @Test
    @Order(6)
    @DisplayName("Should delete belief")
    void testDeleteBelief() {
        executeInTransaction(() -> {
            // Arrange
            Belief belief = createTestBelief("belief-delete", "agent-delete", "To be deleted");
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
    @Order(7)
    @DisplayName("Should handle belief conflicts")
    void testConflictManagement() {
        executeInTransaction(() -> {
            // Arrange
            BeliefConflict conflict = createTestConflict("conflict-1", "agent-conflict");

            // Act
            BeliefConflict stored = storageService.storeConflict(conflict);
            List<BeliefConflict> conflicts = storageService.getUnresolvedConflicts("agent-conflict");

            // Assert
            assertNotNull(stored);
            assertNotNull(stored.getConflictId());
            assertEquals(1, conflicts.size());
            assertEquals(conflict.getDescription(), conflicts.get(0).getDescription());
        });
    }

    @Test
    @Order(8)
    @DisplayName("Should get beliefs by category")
    void testGetBeliefsByCategory() {
        executeInTransaction(() -> {
            // Arrange
            String agentId = "agent-category";
            String category = "TestCategory";
            
            List<Belief> beliefs = Arrays.asList(
                createTestBeliefWithCategory("belief-cat-1", agentId, "Statement 1", category),
                createTestBeliefWithCategory("belief-cat-2", agentId, "Statement 2", category),
                createTestBeliefWithCategory("belief-cat-3", agentId, "Statement 3", "OtherCategory")
            );
            
            storageService.storeBeliefs(beliefs);

            // Act
            List<Belief> categoryBeliefs = storageService.getBeliefsInCategory(category, agentId, false);

            // Assert
            assertEquals(2, categoryBeliefs.size());
            assertTrue(categoryBeliefs.stream()
                    .allMatch(b -> category.equals(b.getCategory())));
        });
    }

    @Test
    @Order(9)
    @DisplayName("Should provide storage statistics")
    void testStorageStatistics() {
        executeInTransaction(() -> {
            // Arrange - store some test data
            String agentId = "stats-agent";
            List<Belief> beliefs = Arrays.asList(
                createTestBelief("stats-1", agentId, "Stats belief 1"),
                createTestBelief("stats-2", agentId, "Stats belief 2")
            );
            storageService.storeBeliefs(beliefs);

            // Act
            Map<String, Object> stats = storageService.getStorageStatistics();

            // Assert
            assertNotNull(stats);
            assertTrue(stats.containsKey("totalBeliefs"));
            assertTrue(stats.containsKey("agentCount"));
            
            // Verify the counts are reasonable
            Object totalBeliefs = stats.get("totalBeliefs");
            if (totalBeliefs instanceof Number) {
                assertTrue(((Number) totalBeliefs).longValue() >= 2);
            }
        });
    }

    @Test
    @Order(10)
    @DisplayName("Should handle validation errors")
    void testValidationErrors() {
        // Test null belief
        assertThrows(IllegalArgumentException.class, () -> 
            storageService.storeBelief(null));

        // Test belief with null required fields
        Belief invalidBelief = new Belief();
        assertThrows(Exception.class, () -> 
            storageService.storeBelief(invalidBelief));
    }

    @Test
    @Order(11)
    @DisplayName("Should handle large batch operations")
    void testBatchOperations() {
        executeInTransaction(() -> {
            // Arrange
            String agentId = "batch-agent";
            List<Belief> largeBatch = new ArrayList<>();
            
            for (int i = 0; i < 100; i++) {
                largeBatch.add(createTestBelief("batch-" + i, agentId, "Batch statement " + i));
            }

            // Act
            List<Belief> stored = storageService.storeBeliefs(largeBatch);
            List<Belief> retrieved = storageService.getBeliefsForAgent(agentId, false);

            // Assert
            assertEquals(100, stored.size());
            assertEquals(100, retrieved.size());
            
            // Verify all beliefs have IDs
            assertTrue(stored.stream().allMatch(b -> b.getId() != null));
        });
    }

    // Helper methods

    private void executeInTransaction(Runnable operation) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            operation.run();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    private Belief createTestBelief(String id, String agentId, String statement) {
        return createTestBeliefWithCategory(id, agentId, statement, "TestCategory");
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
        belief.setTags(Set.of("test", "unit"));
        belief.setEvidenceMemoryIds(Set.of("memory-1", "memory-2"));
        return belief;
    }

    private BeliefConflict createTestConflict(String id, String agentId) {
        BeliefConflict conflict = new BeliefConflict();
        conflict.setConflictId(id);
        conflict.setAgentId(agentId);
        conflict.setDescription("Test conflict description");
        conflict.setBeliefId("belief-" + id);
        conflict.setMemoryId("memory-" + id);
        conflict.setDetectedAt(Instant.now());
        conflict.setResolved(false);
        conflict.setSeverity("MEDIUM");
        return conflict;
    }

    private void cleanupDatabase() {
        if (!entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().begin();
        }
        try {
            // Clean up in proper order due to foreign key constraints
            entityManager.createNativeQuery("DELETE FROM belief_tags").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM belief_evidence_memories").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM conflict_belief_ids").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM belief_conflicts").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM beliefs").executeUpdate();
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            // Ignore cleanup errors in tests
        }
    }
}