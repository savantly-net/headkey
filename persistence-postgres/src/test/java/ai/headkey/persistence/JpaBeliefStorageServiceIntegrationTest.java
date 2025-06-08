package ai.headkey.persistence;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefConflict;
import ai.headkey.memory.spi.BeliefStorageService;
import ai.headkey.persistence.factory.JpaBeliefStorageServiceFactory;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for JpaBeliefStorageService using TestContainers with PostgreSQL.
 * 
 * This test class verifies the complete functionality of the JPA belief storage
 * implementation against a real PostgreSQL database running in a Docker container.
 * It tests all CRUD operations, queries, conflict management, and statistics.
 * 
 * @since 1.0
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JpaBeliefStorageServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("headkey_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withInitScript("test-schema.sql");

    private static BeliefStorageService storageService;
    private static DataSource dataSource;

    @BeforeAll
    static void setUp() {
        // Create HikariCP DataSource
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        dataSource = new HikariDataSource(config);
        
        // Create JPA storage service
        storageService = JpaBeliefStorageServiceFactory.createForTesting();
        
        // Verify service is healthy
        assertTrue(storageService.isHealthy(), "Storage service should be healthy");
    }

    @AfterAll
    static void tearDown() {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should store and retrieve a belief")
    void testStoreAndRetrieveBelief() {
        // Given
        Belief belief = createTestBelief("belief-1", "user-123", "I love pizza", 0.8, "preference");
        
        // When
        Belief storedBelief = storageService.storeBelief(belief);
        Optional<Belief> retrievedBelief = storageService.getBeliefById("belief-1");
        
        // Then
        assertNotNull(storedBelief);
        assertEquals("belief-1", storedBelief.getId());
        assertTrue(retrievedBelief.isPresent());
        assertEquals("I love pizza", retrievedBelief.get().getStatement());
        assertEquals(0.8, retrievedBelief.get().getConfidence(), 0.001);
        assertEquals("preference", retrievedBelief.get().getCategory());
        assertTrue(retrievedBelief.get().isActive());
    }

    @Test
    @Order(2)
    @DisplayName("Should store multiple beliefs in batch")
    void testBatchStoreBelief() {
        // Given
        List<Belief> beliefs = Arrays.asList(
            createTestBelief("belief-2", "user-123", "I enjoy reading", 0.7, "preference"),
            createTestBelief("belief-3", "user-123", "Paris is in France", 0.9, "fact"),
            createTestBelief("belief-4", "user-456", "I dislike vegetables", 0.6, "preference")
        );
        
        // When
        List<Belief> storedBeliefs = storageService.storeBeliefs(beliefs);
        
        // Then
        assertEquals(3, storedBeliefs.size());
        
        // Verify individual beliefs
        Optional<Belief> belief2 = storageService.getBeliefById("belief-2");
        Optional<Belief> belief3 = storageService.getBeliefById("belief-3");
        Optional<Belief> belief4 = storageService.getBeliefById("belief-4");
        
        assertTrue(belief2.isPresent());
        assertTrue(belief3.isPresent());
        assertTrue(belief4.isPresent());
        
        assertEquals("I enjoy reading", belief2.get().getStatement());
        assertEquals("Paris is in France", belief3.get().getStatement());
        assertEquals("I dislike vegetables", belief4.get().getStatement());
    }

    @Test
    @Order(3)
    @DisplayName("Should retrieve beliefs by agent")
    void testGetBeliefsByAgent() {
        // When
        List<Belief> user123Beliefs = storageService.getBeliefsForAgent("user-123", false);
        List<Belief> user456Beliefs = storageService.getBeliefsForAgent("user-456", false);
        
        // Then
        assertEquals(3, user123Beliefs.size()); // belief-1, belief-2, belief-3
        assertEquals(1, user456Beliefs.size()); // belief-4
        
        // Verify agent-specific beliefs
        assertTrue(user123Beliefs.stream().allMatch(b -> "user-123".equals(b.getAgentId())));
        assertTrue(user456Beliefs.stream().allMatch(b -> "user-456".equals(b.getAgentId())));
    }

    @Test
    @Order(4)
    @DisplayName("Should retrieve beliefs by category")
    void testGetBeliefsByCategory() {
        // When
        List<Belief> preferenceBeliefs = storageService.getBeliefsInCategory("preference", null, false);
        List<Belief> factBeliefs = storageService.getBeliefsInCategory("fact", null, false);
        List<Belief> user123Preferences = storageService.getBeliefsInCategory("preference", "user-123", false);
        
        // Then
        assertEquals(3, preferenceBeliefs.size()); // belief-1, belief-2, belief-4
        assertEquals(1, factBeliefs.size()); // belief-3
        assertEquals(2, user123Preferences.size()); // belief-1, belief-2
        
        // Verify categories
        assertTrue(preferenceBeliefs.stream().allMatch(b -> "preference".equals(b.getCategory())));
        assertTrue(factBeliefs.stream().allMatch(b -> "fact".equals(b.getCategory())));
        assertTrue(user123Preferences.stream().allMatch(b -> "user-123".equals(b.getAgentId()) && "preference".equals(b.getCategory())));
    }

    @Test
    @Order(5)
    @DisplayName("Should search beliefs by text")
    void testSearchBeliefs() {
        // When
        List<Belief> pizzaResults = storageService.searchBeliefs("pizza", null, 10);
        List<Belief> loveResults = storageService.searchBeliefs("love", null, 10);
        List<Belief> parisResults = storageService.searchBeliefs("Paris", "user-123", 10);
        
        // Then
        assertEquals(1, pizzaResults.size());
        assertEquals("I love pizza", pizzaResults.get(0).getStatement());
        
        assertEquals(1, loveResults.size());
        assertEquals("I love pizza", loveResults.get(0).getStatement());
        
        assertEquals(1, parisResults.size());
        assertEquals("Paris is in France", parisResults.get(0).getStatement());
    }

    @Test
    @Order(6)
    @DisplayName("Should find similar beliefs")
    void testFindSimilarBeliefs() {
        // When
        List<BeliefStorageService.SimilarBelief> similarToLove = storageService.findSimilarBeliefs("I really love pizza", "user-123", 0.3, 10);
        List<BeliefStorageService.SimilarBelief> similarToFrance = storageService.findSimilarBeliefs("France is a country", null, 0.1, 10);
        
        // Then
        assertFalse(similarToLove.isEmpty());
        assertTrue(similarToLove.stream().anyMatch(sb -> sb.getBelief().getStatement().contains("pizza")));
        
        assertFalse(similarToFrance.isEmpty());
        assertTrue(similarToFrance.stream().anyMatch(sb -> sb.getBelief().getStatement().contains("France")));
    }

    @Test
    @Order(7)
    @DisplayName("Should get low confidence beliefs")
    void testGetLowConfidenceBeliefs() {
        // When
        List<Belief> lowConfidenceAll = storageService.getLowConfidenceBeliefs(0.75, null);
        List<Belief> lowConfidenceUser456 = storageService.getLowConfidenceBeliefs(0.75, "user-456");
        
        // Then
        assertEquals(2, lowConfidenceAll.size()); // belief-2 (0.7), belief-4 (0.6)
        assertEquals(1, lowConfidenceUser456.size()); // belief-4 (0.6)
        
        // Verify confidence levels
        assertTrue(lowConfidenceAll.stream().allMatch(b -> b.getConfidence() < 0.75));
        assertTrue(lowConfidenceUser456.stream().allMatch(b -> b.getConfidence() < 0.75 && "user-456".equals(b.getAgentId())));
    }

    @Test
    @Order(8)
    @DisplayName("Should store and retrieve conflicts")
    void testConflictManagement() {
        // Given
        BeliefConflict conflict = createTestConflict("conflict-1", "user-123", Arrays.asList("belief-1", "belief-2"), "Preference conflict");
        
        // When
        BeliefConflict storedConflict = storageService.storeConflict(conflict);
        Optional<BeliefConflict> retrievedConflict = storageService.getConflictById("conflict-1");
        List<BeliefConflict> unresolvedConflicts = storageService.getUnresolvedConflicts("user-123");
        
        // Then
        assertNotNull(storedConflict);
        assertEquals("conflict-1", storedConflict.getId());
        assertTrue(retrievedConflict.isPresent());
        assertEquals("Preference conflict", retrievedConflict.get().getDescription());
        assertFalse(retrievedConflict.get().isResolved());
        
        assertEquals(1, unresolvedConflicts.size());
        assertEquals("conflict-1", unresolvedConflicts.get(0).getId());
    }

    @Test
    @Order(9)
    @DisplayName("Should update belief confidence")
    void testUpdateBeliefConfidence() {
        // Given
        String beliefId = "belief-1";
        double newConfidence = 0.95;
        
        // When
        Optional<Belief> beforeUpdate = storageService.getBeliefById(beliefId);
        Belief updatedBelief = storageService.storeBelief(createUpdatedBelief(beforeUpdate.get(), newConfidence));
        Optional<Belief> afterUpdate = storageService.getBeliefById(beliefId);
        
        // Then
        assertTrue(beforeUpdate.isPresent());
        assertEquals(0.8, beforeUpdate.get().getConfidence(), 0.001);
        
        assertNotNull(updatedBelief);
        assertEquals(newConfidence, updatedBelief.getConfidence(), 0.001);
        
        assertTrue(afterUpdate.isPresent());
        assertEquals(newConfidence, afterUpdate.get().getConfidence(), 0.001);
    }

    @Test
    @Order(10)
    @DisplayName("Should get storage statistics")
    void testGetStatistics() {
        // When
        Map<String, Object> stats = storageService.getStorageStatistics();
        Map<String, Long> categoryDistribution = storageService.getBeliefDistributionByCategory(null);
        Map<String, Long> confidenceDistribution = storageService.getBeliefDistributionByConfidence(null);
        long user123Count = storageService.countBeliefsForAgent("user-123", false);
        
        // Then
        assertNotNull(stats);
        assertTrue((Long) stats.get("totalBeliefs") >= 4);
        assertTrue((Long) stats.get("activeBeliefs") >= 4);
        assertTrue((Long) stats.get("totalConflicts") >= 1);
        
        assertNotNull(categoryDistribution);
        assertTrue(categoryDistribution.containsKey("preference"));
        assertTrue(categoryDistribution.containsKey("fact"));
        assertTrue(categoryDistribution.get("preference") >= 3);
        assertTrue(categoryDistribution.get("fact") >= 1);
        
        assertNotNull(confidenceDistribution);
        assertTrue(confidenceDistribution.values().stream().mapToLong(Long::longValue).sum() >= 4);
        
        assertEquals(3, user123Count);
    }

    @Test
    @Order(11)
    @DisplayName("Should delete beliefs")
    void testDeleteBelief() {
        // Given
        String beliefToDelete = "belief-4";
        
        // When
        boolean deleted = storageService.deleteBelief(beliefToDelete);
        Optional<Belief> afterDeletion = storageService.getBeliefById(beliefToDelete);
        
        // Then
        assertTrue(deleted);
        assertFalse(afterDeletion.isPresent());
        
        // Verify count decreased
        long user456Count = storageService.countBeliefsForAgent("user-456", false);
        assertEquals(0, user456Count);
    }

    @Test
    @Order(12)
    @DisplayName("Should remove conflicts")
    void testRemoveConflict() {
        // Given
        String conflictToRemove = "conflict-1";
        
        // When
        boolean removed = storageService.removeConflict(conflictToRemove);
        Optional<BeliefConflict> afterRemoval = storageService.getConflictById(conflictToRemove);
        List<BeliefConflict> unresolvedAfterRemoval = storageService.getUnresolvedConflicts("user-123");
        
        // Then
        assertTrue(removed);
        assertFalse(afterRemoval.isPresent());
        assertEquals(0, unresolvedAfterRemoval.size());
    }

    @Test
    @Order(13)
    @DisplayName("Should validate storage integrity")
    void testValidateIntegrity() {
        // When
        Map<String, Object> validationResults = storageService.validateIntegrity();
        
        // Then
        assertNotNull(validationResults);
        assertEquals("validate", validationResults.get("operation"));
        assertTrue(validationResults.containsKey("healthy"));
        assertTrue(validationResults.containsKey("issuesFound"));
        assertTrue(validationResults.containsKey("issues"));
    }

    @Test
    @Order(14)
    @DisplayName("Should optimize storage")
    void testOptimizeStorage() {
        // When
        Map<String, Object> optimizationResults = storageService.optimizeStorage();
        
        // Then
        assertNotNull(optimizationResults);
        assertEquals("optimize", optimizationResults.get("operation"));
        assertTrue((Boolean) optimizationResults.get("success"));
        assertTrue(optimizationResults.containsKey("duration"));
        assertTrue(optimizationResults.containsKey("optimizedAt"));
    }

    @Test
    @Order(15)
    @DisplayName("Should check service health")
    void testServiceHealth() {
        // When
        boolean isHealthy = storageService.isHealthy();
        Map<String, Object> healthInfo = storageService.getHealthInfo();
        Map<String, Object> serviceInfo = storageService.getServiceInfo();
        
        // Then
        assertTrue(isHealthy);
        
        assertNotNull(healthInfo);
        assertEquals("healthy", healthInfo.get("status"));
        assertTrue(healthInfo.containsKey("checkedAt"));
        assertEquals("postgresql_jpa", healthInfo.get("storageType"));
        
        assertNotNull(serviceInfo);
        assertEquals("JpaBeliefStorageService", serviceInfo.get("serviceType"));
        assertEquals("1.0", serviceInfo.get("version"));
        assertEquals("postgresql", serviceInfo.get("persistence"));
    }

    // ========== Helper Methods ==========

    private Belief createTestBelief(String id, String agentId, String statement, double confidence, String category) {
        Belief belief = new Belief(id, agentId, statement, confidence);
        belief.setCategory(category);
        belief.setCreatedAt(Instant.now());
        belief.setLastUpdated(Instant.now());
        belief.addEvidence("memory-" + id);
        return belief;
    }

    private Belief createUpdatedBelief(Belief original, double newConfidence) {
        Belief updated = new Belief(original.getId(), original.getAgentId(), original.getStatement(), newConfidence);
        updated.setCategory(original.getCategory());
        updated.setCreatedAt(original.getCreatedAt());
        updated.setLastUpdated(Instant.now());
        updated.setEvidenceMemoryIds(original.getEvidenceMemoryIds());
        updated.setTags(original.getTags());
        updated.setReinforcementCount(original.getReinforcementCount());
        updated.setActive(original.isActive());
        return updated;
    }

    private BeliefConflict createTestConflict(String id, String agentId, List<String> beliefIds, String description) {
        BeliefConflict conflict = new BeliefConflict();
        conflict.setId(id);
        conflict.setAgentId(agentId);
        conflict.setConflictingBeliefIds(beliefIds);
        conflict.setDescription(description);
        conflict.setDetectedAt(Instant.now());
        conflict.setResolved(false);
        conflict.setConflictType("preference");
        conflict.setSeverity("MEDIUM");
        conflict.setAutoResolvable(true);
        return conflict;
    }
}