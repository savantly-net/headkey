package ai.headkey.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefConflict;
import ai.headkey.memory.spi.BeliefStorageService;
import ai.headkey.persistence.repositories.impl.JpaBeliefConflictRepository;
import ai.headkey.persistence.repositories.impl.JpaBeliefRepository;
import ai.headkey.persistence.services.JpaBeliefStorageService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Basic test for JPA belief storage functionality using H2 in-memory database.
 * 
 * This test focuses on fundamental operations and proper JPA entity mapping
 * without complex transaction management or TestContainers setup.
 * 
 * @since 1.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JpaBeliefStorageBasicTest {

    private static EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private BeliefStorageService storageService;

    @BeforeAll
    static void setUpClass() {
        // Create H2 EntityManagerFactory for testing
        entityManagerFactory = Persistence.createEntityManagerFactory("headkey-beliefs-h2-test");
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
        
        // Create repositories and service directly
        JpaBeliefRepository beliefRepository = new JpaBeliefRepository(entityManagerFactory);
        JpaBeliefConflictRepository conflictRepository = new JpaBeliefConflictRepository(entityManagerFactory);
        storageService = new JpaBeliefStorageService(beliefRepository, conflictRepository);
        
        // Clean up any existing data
        cleanupDatabase();
    }

    @AfterEach
    void tearDown() {
        if (entityManager != null) {
            cleanupDatabase();
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
    @DisplayName("Should store and retrieve a belief with proper transaction")
    void testStoreAndRetrieveBelief() {
        entityManager.getTransaction().begin();
        try {
            // Arrange
            Belief belief = createTestBelief("test-belief-1", "test-agent-1", "Test belief statement");

            // Act
            Belief stored = storageService.storeBelief(belief);
            entityManager.flush(); // Ensure data is written
            
            Optional<Belief> retrieved = storageService.getBeliefById(stored.getId());

            // Assert
            assertNotNull(stored);
            assertNotNull(stored.getId());
            assertTrue(retrieved.isPresent());
            assertEquals(belief.getStatement(), retrieved.get().getStatement());
            assertEquals(belief.getAgentId(), retrieved.get().getAgentId());
            assertEquals(belief.getCategory(), retrieved.get().getCategory());
            
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw e;
        }
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
    @DisplayName("Should store multiple beliefs in batch")
    void testStoreBeliefsInBatch() {
        entityManager.getTransaction().begin();
        try {
            // Arrange
            String agentId = "batch-agent";
            List<Belief> beliefs = Arrays.asList(
                createTestBelief("batch-1", agentId, "First batch belief"),
                createTestBelief("batch-2", agentId, "Second batch belief"),
                createTestBelief("batch-3", agentId, "Third batch belief")
            );

            // Act
            List<Belief> stored = storageService.storeBeliefs(beliefs);
            entityManager.flush();
            
            List<Belief> retrieved = storageService.getBeliefsForAgent(agentId, false);

            // Assert
            assertEquals(3, stored.size());
            assertEquals(3, retrieved.size());
            
            // Verify all beliefs have IDs
            assertTrue(stored.stream().allMatch(b -> b.getId() != null));
            
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw e;
        }
    }

    @Test
    @Order(5)
    @DisplayName("Should update existing belief")
    void testUpdateBelief() {
        entityManager.getTransaction().begin();
        try {
            // Arrange
            Belief original = createTestBelief("belief-update", "agent-update", "Original statement");
            Belief stored = storageService.storeBelief(original);
            entityManager.flush();
            
            // Modify the belief
            stored.setStatement("Updated statement");
            stored.setConfidence(0.9);
            stored.setCategory("UpdatedCategory");

            // Act
            Belief updated = storageService.storeBelief(stored);
            entityManager.flush();
            
            Optional<Belief> retrieved = storageService.getBeliefById(stored.getId());

            // Assert
            assertNotNull(updated);
            assertTrue(retrieved.isPresent());
            assertEquals("Updated statement", retrieved.get().getStatement());
            assertEquals(0.9, retrieved.get().getConfidence(), 0.001);
            assertEquals("UpdatedCategory", retrieved.get().getCategory());
            
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw e;
        }
    }

    @Test
    @Order(6)
    @DisplayName("Should delete belief")
    void testDeleteBelief() {
        entityManager.getTransaction().begin();
        try {
            // Arrange
            Belief belief = createTestBelief("belief-delete", "agent-delete", "To be deleted");
            Belief stored = storageService.storeBelief(belief);
            entityManager.flush();

            // Act
            boolean deleted = storageService.deleteBelief(stored.getId());
            entityManager.flush();
            
            Optional<Belief> retrieved = storageService.getBeliefById(stored.getId());

            // Assert
            assertTrue(deleted);
            assertFalse(retrieved.isPresent());
            
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw e;
        }
    }

    @Test
    @Order(7)
    @DisplayName("Should handle belief conflicts")
    void testConflictManagement() {
        entityManager.getTransaction().begin();
        try {
            // Arrange
            BeliefConflict conflict = createTestConflict("conflict-1", "agent-conflict");

            // Act
            BeliefConflict stored = storageService.storeConflict(conflict);
            entityManager.flush();
            
            List<BeliefConflict> conflicts = storageService.getUnresolvedConflicts("agent-conflict");

            // Assert
            assertNotNull(stored);
            assertNotNull(stored.getConflictId());
            assertEquals(1, conflicts.size());
            assertEquals(conflict.getDescription(), conflicts.get(0).getDescription());
            
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw e;
        }
    }

    @Test
    @Order(8)
    @DisplayName("Should get beliefs by category")
    void testGetBeliefsByCategory() {
        entityManager.getTransaction().begin();
        try {
            // Arrange
            String agentId = "agent-category";
            String targetCategory = "TestCategory";
            String otherCategory = "OtherCategory";
            
            List<Belief> beliefs = Arrays.asList(
                createTestBeliefWithCategory("cat-1", agentId, "Statement 1", targetCategory),
                createTestBeliefWithCategory("cat-2", agentId, "Statement 2", targetCategory),
                createTestBeliefWithCategory("cat-3", agentId, "Statement 3", otherCategory)
            );
            
            storageService.storeBeliefs(beliefs);
            entityManager.flush();

            // Act
            List<Belief> categoryBeliefs = storageService.getBeliefsInCategory(targetCategory, agentId, false);

            // Assert
            assertEquals(2, categoryBeliefs.size());
            assertTrue(categoryBeliefs.stream()
                    .allMatch(b -> targetCategory.equals(b.getCategory())));
            
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw e;
        }
    }

    @Test
    @Order(9)
    @DisplayName("Should provide storage statistics")
    void testStorageStatistics() {
        entityManager.getTransaction().begin();
        try {
            // Arrange - store some test data
            String agentId = "stats-agent";
            List<Belief> beliefs = Arrays.asList(
                createTestBelief("stats-1", agentId, "Stats belief 1"),
                createTestBelief("stats-2", agentId, "Stats belief 2")
            );
            storageService.storeBeliefs(beliefs);
            entityManager.flush();

            // Act
            Map<String, Object> stats = storageService.getStorageStatistics();

            // Assert
            assertNotNull(stats);
            // Basic validation - just ensure we get some response
            assertTrue(stats.size() > 0);
            
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw e;
        }
    }

    @Test
    @Order(10)
    @DisplayName("Should handle validation errors gracefully")
    void testValidationErrors() {
        // Test null belief
        assertThrows(IllegalArgumentException.class, () -> 
            storageService.storeBelief(null));

        // Test belief with null required fields
        Belief invalidBelief = new Belief();
        entityManager.getTransaction().begin();
        try {
            assertThrows(Exception.class, () -> {
                storageService.storeBelief(invalidBelief);
                entityManager.flush();
            });
            entityManager.getTransaction().rollback();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            // Expected behavior
        }
    }

    // Helper methods

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