package ai.headkey.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefConflict;
import ai.headkey.memory.interfaces.BeliefStorageService;
import ai.headkey.persistence.repositories.impl.JpaBeliefConflictRepository;
import ai.headkey.persistence.repositories.impl.JpaBeliefRepository;
import ai.headkey.persistence.services.JpaBeliefStorageService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Comprehensive PostgreSQL integration test for belief storage functionality.
 * 
 * This test verifies that the complete belief storage system works correctly
 * with a real PostgreSQL database using TestContainers.
 * 
 * @since 1.0
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PostgreSQLBeliefStorageTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("belief_test_db")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true);

    private static EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private BeliefStorageService storageService;

    @BeforeAll
    static void setUpDatabase() {
        // Ensure container is ready
        assertTrue(postgres.isRunning(), "PostgreSQL container should be running");
        
        // Create EntityManagerFactory with PostgreSQL configuration
        Map<String, Object> properties = new HashMap<>();
        
        // Database connection settings
        properties.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        properties.put("jakarta.persistence.jdbc.url", postgres.getJdbcUrl());
        properties.put("jakarta.persistence.jdbc.user", postgres.getUsername());
        properties.put("jakarta.persistence.jdbc.password", postgres.getPassword());
        
        // Hibernate PostgreSQL settings
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.hbm2ddl.auto", "create-drop");
        properties.put("hibernate.show_sql", "false");
        properties.put("hibernate.format_sql", "false");
        
        // Performance settings
        properties.put("hibernate.jdbc.batch_size", "20");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        
        // Cache settings disabled for test isolation
        properties.put("hibernate.cache.use_second_level_cache", "false");
        properties.put("hibernate.cache.use_query_cache", "false");
        
        entityManagerFactory = Persistence.createEntityManagerFactory("headkey-beliefs-postgres-test", properties);
    }

    @AfterAll
    static void tearDownDatabase() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @BeforeEach
    void setUp() {
        entityManager = entityManagerFactory.createEntityManager();
        
        // Create repositories and service
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
    @DisplayName("Should be healthy with PostgreSQL connection")
    void testServiceHealth() {
        assertTrue(storageService.isHealthy(), "Service should be healthy with PostgreSQL");
        
        Map<String, Object> serviceInfo = storageService.getServiceInfo();
        assertNotNull(serviceInfo);
        assertTrue(serviceInfo.containsKey("serviceType"));
    }

    @Test
    @Order(2)
    @DisplayName("Should store and retrieve belief with PostgreSQL")
    void testStoreAndRetrieveBelief() {
        entityManager.getTransaction().begin();
        try {
            // Arrange
            Belief belief = createTestBelief("postgres-belief-1", "test-agent-1", "PostgreSQL test belief");

            // Act
            Belief stored = storageService.storeBelief(belief);
            entityManager.flush();
            
            Optional<Belief> retrieved = storageService.getBeliefById(stored.getId());

            // Assert
            assertNotNull(stored);
            assertNotNull(stored.getId());
            assertTrue(retrieved.isPresent());
            assertEquals(belief.getStatement(), retrieved.get().getStatement());
            assertEquals(belief.getAgentId(), retrieved.get().getAgentId());
            assertEquals(belief.getCategory(), retrieved.get().getCategory());
            assertEquals(belief.getConfidence(), retrieved.get().getConfidence());
            assertNotNull(retrieved.get().getCreatedAt());
            assertNotNull(retrieved.get().getLastUpdated());
            
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw e;
        }
    }

    @Test
    @Order(3)
    @DisplayName("Should handle batch operations with PostgreSQL")
    void testBatchOperations() {
        entityManager.getTransaction().begin();
        try {
            // Arrange
            String agentId = "batch-agent-postgres";
            List<Belief> beliefs = Arrays.asList(
                createTestBelief("batch-postgres-1", agentId, "First PostgreSQL batch belief"),
                createTestBelief("batch-postgres-2", agentId, "Second PostgreSQL batch belief"),
                createTestBelief("batch-postgres-3", agentId, "Third PostgreSQL batch belief")
            );

            // Act
            List<Belief> stored = storageService.storeBeliefs(beliefs);
            entityManager.flush();
            
            List<Belief> retrieved = storageService.getBeliefsForAgent(agentId, false);

            // Assert
            assertEquals(3, stored.size());
            assertEquals(3, retrieved.size());
            
            // Verify all beliefs have proper IDs and are persisted
            assertTrue(stored.stream().allMatch(b -> b.getId() != null));
            assertTrue(retrieved.stream().allMatch(b -> b.getAgentId().equals(agentId)));
            
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw e;
        }
    }

    @Test
    @Order(4)
    @DisplayName("Should query beliefs by agent with PostgreSQL")
    void testGetBeliefsByAgent() {
        entityManager.getTransaction().begin();
        try {
            // Arrange
            String targetAgentId = "target-agent-postgres";
            String otherAgentId = "other-agent-postgres";
            
            List<Belief> beliefs = Arrays.asList(
                createTestBelief("agent-postgres-1", targetAgentId, "First agent belief"),
                createTestBelief("agent-postgres-2", targetAgentId, "Second agent belief"),
                createTestBelief("agent-postgres-3", otherAgentId, "Other agent belief")
            );
            
            storageService.storeBeliefs(beliefs);
            entityManager.flush();

            // Act
            List<Belief> retrieved = storageService.getBeliefsForAgent(targetAgentId, false);

            // Assert
            assertEquals(2, retrieved.size());
            assertTrue(retrieved.stream().allMatch(b -> targetAgentId.equals(b.getAgentId())));
            assertFalse(retrieved.stream().anyMatch(b -> otherAgentId.equals(b.getAgentId())));
            
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw e;
        }
    }

    @Test
    @Order(5)
    @DisplayName("Should query beliefs by category with PostgreSQL")
    void testGetBeliefsByCategory() {
        entityManager.getTransaction().begin();
        try {
            // Arrange
            String agentId = "category-agent-postgres";
            String targetCategory = "PostgreSQLCategory";
            String otherCategory = "OtherCategory";
            
            List<Belief> beliefs = Arrays.asList(
                createTestBeliefWithCategory("cat-postgres-1", agentId, "Statement 1", targetCategory),
                createTestBeliefWithCategory("cat-postgres-2", agentId, "Statement 2", targetCategory),
                createTestBeliefWithCategory("cat-postgres-3", agentId, "Statement 3", otherCategory)
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
    @Order(6)
    @DisplayName("Should handle belief updates with PostgreSQL")
    void testUpdateBelief() {
        entityManager.getTransaction().begin();
        try {
            // Arrange
            Belief original = createTestBelief("update-postgres", "update-agent", "Original PostgreSQL statement");
            Belief stored = storageService.storeBelief(original);
            entityManager.flush();
            
            // Modify the belief
            stored.setStatement("Updated PostgreSQL statement");
            stored.setConfidence(0.95);
            stored.setCategory("UpdatedPostgreSQLCategory");

            // Act
            Belief updated = storageService.storeBelief(stored);
            entityManager.flush();
            
            Optional<Belief> retrieved = storageService.getBeliefById(stored.getId());

            // Assert
            assertNotNull(updated);
            assertTrue(retrieved.isPresent());
            assertEquals("Updated PostgreSQL statement", retrieved.get().getStatement());
            assertEquals(0.95, retrieved.get().getConfidence(), 0.001);
            assertEquals("UpdatedPostgreSQLCategory", retrieved.get().getCategory());
            
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw e;
        }
    }

    @Test
    @Order(7)
    @DisplayName("Should delete beliefs with PostgreSQL")
    void testDeleteBelief() {
        entityManager.getTransaction().begin();
        try {
            // Arrange
            Belief belief = createTestBelief("delete-postgres", "delete-agent", "To be deleted from PostgreSQL");
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
    @Order(8)
    @DisplayName("Should manage conflicts with PostgreSQL")
    void testConflictManagement() {
        entityManager.getTransaction().begin();
        try {
            // Arrange
            BeliefConflict conflict = createTestConflict("conflict-postgres", "conflict-agent-postgres");

            // Act
            BeliefConflict stored = storageService.storeConflict(conflict);
            entityManager.flush();
            
            List<BeliefConflict> conflicts = storageService.getUnresolvedConflicts("conflict-agent-postgres");

            // Assert
            assertNotNull(stored);
            assertNotNull(stored.getConflictId());
            assertEquals(1, conflicts.size());
            assertEquals(conflict.getDescription(), conflicts.get(0).getDescription());
            assertEquals(conflict.getAgentId(), conflicts.get(0).getAgentId());
            
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw e;
        }
    }

    @Test
    @Order(9)
    @DisplayName("Should provide storage statistics with PostgreSQL")
    void testStorageStatistics() {
        entityManager.getTransaction().begin();
        try {
            // Arrange - store some test data
            String agentId = "stats-agent-postgres";
            List<Belief> beliefs = Arrays.asList(
                createTestBelief("stats-postgres-1", agentId, "Stats belief 1"),
                createTestBelief("stats-postgres-2", agentId, "Stats belief 2")
            );
            storageService.storeBeliefs(beliefs);
            entityManager.flush();

            // Act
            Map<String, Object> stats = storageService.getStorageStatistics();

            // Assert
            assertNotNull(stats);
            assertTrue(stats.size() > 0);
            
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw e;
        }
    }

    @Test
    @Order(10)
    @DisplayName("Should handle PostgreSQL-specific data types and constraints")
    void testPostgreSQLSpecificFeatures() {
        entityManager.getTransaction().begin();
        try {
            // Arrange - create belief with PostgreSQL-specific features
            Belief belief = createTestBelief("postgres-features", "postgres-agent", "PostgreSQL specific test");
            belief.setTags(Set.of("postgresql", "test", "integration", "database"));
            belief.setEvidenceMemoryIds(Set.of("evidence-1", "evidence-2", "evidence-3"));

            // Act
            Belief stored = storageService.storeBelief(belief);
            entityManager.flush();
            
            Optional<Belief> retrieved = storageService.getBeliefById(stored.getId());

            // Assert
            assertTrue(retrieved.isPresent());
            assertEquals(4, retrieved.get().getTags().size());
            assertEquals(3, retrieved.get().getEvidenceMemoryIds().size());
            assertTrue(retrieved.get().getTags().contains("postgresql"));
            assertTrue(retrieved.get().getEvidenceMemoryIds().contains("evidence-1"));
            
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw e;
        }
    }

    // Helper methods

    private Belief createTestBelief(String id, String agentId, String statement) {
        return createTestBeliefWithCategory(id, agentId, statement, "PostgreSQLTestCategory");
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
        belief.setTags(Set.of("test", "postgresql"));
        belief.setEvidenceMemoryIds(Set.of("memory-postgres-1", "memory-postgres-2"));
        return belief;
    }

    private BeliefConflict createTestConflict(String id, String agentId) {
        BeliefConflict conflict = new BeliefConflict();
        conflict.setConflictId(id);
        conflict.setAgentId(agentId);
        conflict.setDescription("PostgreSQL test conflict description");
        conflict.setBeliefId("belief-postgres-" + id);
        conflict.setMemoryId("memory-postgres-" + id);
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