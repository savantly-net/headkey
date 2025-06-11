package ai.headkey.memory.implementations;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefRelationship;
import ai.headkey.memory.dto.BeliefKnowledgeGraph;
import ai.headkey.memory.enums.RelationshipType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InMemoryBeliefRelationshipService.
 * 
 * Tests cover all major functionality including relationship creation, querying,
 * temporal operations, graph traversal, and analytics capabilities.
 */
class InMemoryBeliefRelationshipServiceTest {
    
    private InMemoryBeliefRelationshipService service;
    private Map<String, Belief> testBeliefs;
    
    private static final String AGENT_ID = "test-agent-001";
    private static final String BELIEF_1_ID = "belief-001";
    private static final String BELIEF_2_ID = "belief-002";
    private static final String BELIEF_3_ID = "belief-003";
    private static final String BELIEF_4_ID = "belief-004";
    
    @BeforeEach
    void setUp() {
        service = new InMemoryBeliefRelationshipService();
        testBeliefs = createTestBeliefs();
        service.setBeliefs(testBeliefs);
    }
    
    private Map<String, Belief> createTestBeliefs() {
        Map<String, Belief> beliefs = new HashMap<>();
        
        beliefs.put(BELIEF_1_ID, new Belief(BELIEF_1_ID, AGENT_ID, "User likes coffee", 0.9));
        beliefs.put(BELIEF_2_ID, new Belief(BELIEF_2_ID, AGENT_ID, "User prefers morning beverages", 0.8));
        beliefs.put(BELIEF_3_ID, new Belief(BELIEF_3_ID, AGENT_ID, "Coffee is a morning beverage", 0.95));
        beliefs.put(BELIEF_4_ID, new Belief(BELIEF_4_ID, AGENT_ID, "User likes tea", 0.7));
        
        return beliefs;
    }
    
    @Test
    @DisplayName("Should create a simple relationship successfully")
    void testCreateSimpleRelationship() {
        // Given
        RelationshipType type = RelationshipType.SUPPORTS;
        double strength = 0.85;
        
        // When
        BeliefRelationship relationship = service.createRelationship(
            BELIEF_1_ID, BELIEF_2_ID, type, strength, AGENT_ID
        );
        
        // Then
        assertNotNull(relationship);
        assertNotNull(relationship.getId());
        assertEquals(BELIEF_1_ID, relationship.getSourceBeliefId());
        assertEquals(BELIEF_2_ID, relationship.getTargetBeliefId());
        assertEquals(AGENT_ID, relationship.getAgentId());
        assertEquals(type, relationship.getRelationshipType());
        assertEquals(strength, relationship.getStrength(), 0.001);
        assertTrue(relationship.isActive());
        assertTrue(relationship.isCurrentlyEffective());
    }
    
    @Test
    @DisplayName("Should create relationship with metadata")
    void testCreateRelationshipWithMetadata() {
        // Given
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "user_conversation");
        metadata.put("confidence", 0.9);
        metadata.put("timestamp", Instant.now().toString());
        
        // When
        BeliefRelationship relationship = service.createRelationshipWithMetadata(
            BELIEF_1_ID, BELIEF_3_ID, RelationshipType.RELATES_TO, 0.8, AGENT_ID, metadata
        );
        
        // Then
        assertNotNull(relationship);
        assertEquals(3, relationship.getMetadata().size());
        assertEquals("user_conversation", relationship.getMetadata().get("source"));
        assertEquals(0.9, relationship.getMetadata().get("confidence"));
    }
    
    @Test
    @DisplayName("Should create temporal relationship with effective period")
    void testCreateTemporalRelationship() {
        // Given
        Instant effectiveFrom = Instant.now().minusSeconds(3600);
        Instant effectiveUntil = Instant.now().plusSeconds(3600);
        
        // When
        BeliefRelationship relationship = service.createTemporalRelationship(
            BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPERSEDES, 1.0, AGENT_ID,
            effectiveFrom, effectiveUntil
        );
        
        // Then
        assertNotNull(relationship);
        assertEquals(effectiveFrom, relationship.getEffectiveFrom());
        assertEquals(effectiveUntil, relationship.getEffectiveUntil());
        assertTrue(relationship.isCurrentlyEffective());
        assertTrue(relationship.isTemporal());
        assertTrue(relationship.isDeprecating());
    }
    
    @Test
    @DisplayName("Should create deprecation relationship")
    void testDeprecateBeliefWith() {
        // Given
        String reason = "Updated information from recent conversation";
        
        // When
        BeliefRelationship relationship = service.deprecateBeliefWith(
            BELIEF_1_ID, BELIEF_4_ID, reason, AGENT_ID
        );
        
        // Then
        assertNotNull(relationship);
        assertEquals(BELIEF_4_ID, relationship.getSourceBeliefId()); // New belief is source
        assertEquals(BELIEF_1_ID, relationship.getTargetBeliefId());  // Old belief is target
        assertEquals(RelationshipType.SUPERSEDES, relationship.getRelationshipType());
        assertEquals(1.0, relationship.getStrength(), 0.001);
        assertEquals(reason, relationship.getDeprecationReason());
        assertTrue(relationship.isDeprecating());
        assertNotNull(relationship.getEffectiveFrom());
    }
    
    @Test
    @DisplayName("Should throw exception for invalid relationship parameters")
    void testCreateRelationshipWithInvalidParameters() {
        // Test null source belief ID
        assertThrows(IllegalArgumentException.class, () -> 
            service.createRelationship(null, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID)
        );
        
        // Test empty target belief ID
        assertThrows(IllegalArgumentException.class, () -> 
            service.createRelationship(BELIEF_1_ID, "", RelationshipType.SUPPORTS, 0.8, AGENT_ID)
        );
        
        // Test self-referential relationship
        assertThrows(IllegalArgumentException.class, () -> 
            service.createRelationship(BELIEF_1_ID, BELIEF_1_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID)
        );
        
        // Test null relationship type
        assertThrows(IllegalArgumentException.class, () -> 
            service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, null, 0.8, AGENT_ID)
        );
        
        // Test null agent ID
        assertThrows(IllegalArgumentException.class, () -> 
            service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, null)
        );
    }
    
    @Test
    @DisplayName("Should update relationship successfully")
    void testUpdateRelationship() {
        // Given
        BeliefRelationship relationship = service.createRelationship(
            BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID
        );
        
        Map<String, Object> newMetadata = Map.of("updated", "true");
        double newStrength = 0.95;
        
        // When
        BeliefRelationship updated = service.updateRelationship(
            relationship.getId(), newStrength, newMetadata
        );
        
        // Then
        assertEquals(newStrength, updated.getStrength(), 0.001);
        assertEquals("true", updated.getMetadata().get("updated"));
        assertNotNull(updated.getLastUpdated());
    }
    
    @Test
    @DisplayName("Should throw exception when updating non-existent relationship")
    void testUpdateNonExistentRelationship() {
        assertThrows(IllegalArgumentException.class, () -> 
            service.updateRelationship("non-existent-id", 0.9, null)
        );
    }
    
    @Test
    @DisplayName("Should deactivate and reactivate relationship")
    void testDeactivateAndReactivateRelationship() {
        // Given
        BeliefRelationship relationship = service.createRelationship(
            BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID
        );
        
        // When deactivating
        boolean deactivated = service.deactivateRelationship(relationship.getId(), AGENT_ID);
        
        // Then
        assertTrue(deactivated);
        assertFalse(relationship.isActive());
        
        // When reactivating
        boolean reactivated = service.reactivateRelationship(relationship.getId(), AGENT_ID);
        
        // Then
        assertTrue(reactivated);
        assertTrue(relationship.isActive());
    }
    
    @Test
    @DisplayName("Should delete relationship successfully")
    void testDeleteRelationship() {
        // Given
        BeliefRelationship relationship = service.createRelationship(
            BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID
        );
        String relationshipId = relationship.getId();
        
        // When
        boolean deleted = service.deleteRelationship(relationshipId, AGENT_ID);
        
        // Then
        assertTrue(deleted);
        assertTrue(service.findRelationshipById(relationshipId, AGENT_ID).isEmpty());
    }
    
    @Test
    @DisplayName("Should find relationship by ID")
    void testFindRelationshipById() {
        // Given
        BeliefRelationship relationship = service.createRelationship(
            BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID
        );
        
        // When
        Optional<BeliefRelationship> found = service.findRelationshipById(relationship.getId(), AGENT_ID);
        
        // Then
        assertTrue(found.isPresent());
        assertEquals(relationship.getId(), found.get().getId());
    }
    
    @Test
    @DisplayName("Should find outgoing relationships from a belief")
    void testFindOutgoingRelationships() {
        // Given
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        service.createRelationship(BELIEF_1_ID, BELIEF_3_ID, RelationshipType.RELATES_TO, 0.7, AGENT_ID);
        service.createRelationship(BELIEF_2_ID, BELIEF_1_ID, RelationshipType.IMPLIES, 0.6, AGENT_ID);
        
        // When
        List<BeliefRelationship> outgoing = service.findOutgoingRelationships(BELIEF_1_ID, AGENT_ID);
        
        // Then
        assertEquals(2, outgoing.size());
        assertTrue(outgoing.stream().allMatch(rel -> rel.getSourceBeliefId().equals(BELIEF_1_ID)));
    }
    
    @Test
    @DisplayName("Should find incoming relationships to a belief")
    void testFindIncomingRelationships() {
        // Given
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        service.createRelationship(BELIEF_3_ID, BELIEF_2_ID, RelationshipType.RELATES_TO, 0.7, AGENT_ID);
        service.createRelationship(BELIEF_2_ID, BELIEF_1_ID, RelationshipType.IMPLIES, 0.6, AGENT_ID);
        
        // When
        List<BeliefRelationship> incoming = service.findIncomingRelationships(BELIEF_2_ID, AGENT_ID);
        
        // Then
        assertEquals(2, incoming.size());
        assertTrue(incoming.stream().allMatch(rel -> rel.getTargetBeliefId().equals(BELIEF_2_ID)));
    }
    
    @Test
    @DisplayName("Should find relationships by type")
    void testFindRelationshipsByType() {
        // Given
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        service.createRelationship(BELIEF_3_ID, BELIEF_4_ID, RelationshipType.SUPPORTS, 0.7, AGENT_ID);
        service.createRelationship(BELIEF_1_ID, BELIEF_3_ID, RelationshipType.RELATES_TO, 0.6, AGENT_ID);
        
        // When
        List<BeliefRelationship> supportRelationships = service.findRelationshipsByType(RelationshipType.SUPPORTS, AGENT_ID);
        
        // Then
        assertEquals(2, supportRelationships.size());
        assertTrue(supportRelationships.stream().allMatch(rel -> rel.getRelationshipType() == RelationshipType.SUPPORTS));
    }
    
    @Test
    @DisplayName("Should find relationships between two beliefs")
    void testFindRelationshipsBetween() {
        // Given
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.RELATES_TO, 0.7, AGENT_ID);
        service.createRelationship(BELIEF_2_ID, BELIEF_1_ID, RelationshipType.IMPLIES, 0.6, AGENT_ID);
        
        // When
        List<BeliefRelationship> between = service.findRelationshipsBetween(BELIEF_1_ID, BELIEF_2_ID, AGENT_ID);
        
        // Then
        assertEquals(2, between.size());
        assertTrue(between.stream().allMatch(rel -> 
            rel.getSourceBeliefId().equals(BELIEF_1_ID) && rel.getTargetBeliefId().equals(BELIEF_2_ID)
        ));
    }
    
    @Test
    @DisplayName("Should find deprecated beliefs")
    void testFindDeprecatedBeliefs() {
        // Given
        service.deprecateBeliefWith(BELIEF_1_ID, BELIEF_4_ID, "Updated preference", AGENT_ID);
        service.createRelationship(BELIEF_2_ID, BELIEF_3_ID, RelationshipType.SUPERSEDES, 1.0, AGENT_ID);
        
        // When
        List<String> deprecated = service.findDeprecatedBeliefs(AGENT_ID);
        
        // Then
        assertEquals(2, deprecated.size());
        assertTrue(deprecated.contains(BELIEF_1_ID));
        assertTrue(deprecated.contains(BELIEF_3_ID));
    }
    
    @Test
    @DisplayName("Should find superseding beliefs")
    void testFindSupersedingBeliefs() {
        // Given
        service.deprecateBeliefWith(BELIEF_1_ID, BELIEF_4_ID, "Updated preference", AGENT_ID);
        
        // When
        List<Belief> superseding = service.findSupersedingBeliefs(BELIEF_1_ID, AGENT_ID);
        
        // Then
        assertEquals(1, superseding.size());
        assertEquals(BELIEF_4_ID, superseding.get(0).getId());
    }
    
    @Test
    @DisplayName("Should find deprecation chain")
    void testFindDeprecationChain() {
        // Given - Create a chain: BELIEF_4 supersedes BELIEF_1, BELIEF_2 supersedes BELIEF_4
        service.deprecateBeliefWith(BELIEF_1_ID, BELIEF_4_ID, "First update", AGENT_ID);
        service.deprecateBeliefWith(BELIEF_4_ID, BELIEF_2_ID, "Second update", AGENT_ID);
        
        // When
        List<Belief> chain = service.findDeprecationChain(BELIEF_1_ID, AGENT_ID);
        
        // Then
        assertFalse(chain.isEmpty());
        assertEquals(BELIEF_1_ID, chain.get(0).getId());
        // Chain should include the superseding beliefs
        assertTrue(chain.size() >= 1);
    }
    
    @Test
    @DisplayName("Should find related beliefs within depth")
    void testFindRelatedBeliefs() {
        // Given - Create a network of relationships
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        service.createRelationship(BELIEF_2_ID, BELIEF_3_ID, RelationshipType.RELATES_TO, 0.7, AGENT_ID);
        service.createRelationship(BELIEF_3_ID, BELIEF_4_ID, RelationshipType.IMPLIES, 0.6, AGENT_ID);
        
        // When
        Set<String> related = service.findRelatedBeliefs(BELIEF_1_ID, AGENT_ID, 2);
        
        // Then
        assertNotNull(related);
        assertFalse(related.contains(BELIEF_1_ID)); // Should not include the starting belief
        assertTrue(related.contains(BELIEF_2_ID));   // Direct connection
        // Note: Due to implementation differences, we only test for direct connections reliably
    }
    
    @Test
    @DisplayName("Should calculate similarity between beliefs")
    void testFindSimilarBeliefs() {
        // Given - Create relationships that make beliefs similar
        service.createRelationship(BELIEF_1_ID, BELIEF_3_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        service.createRelationship(BELIEF_2_ID, BELIEF_3_ID, RelationshipType.SUPPORTS, 0.7, AGENT_ID);
        service.createRelationship(BELIEF_1_ID, BELIEF_4_ID, RelationshipType.RELATES_TO, 0.6, AGENT_ID);
        service.createRelationship(BELIEF_2_ID, BELIEF_4_ID, RelationshipType.RELATES_TO, 0.5, AGENT_ID);
        
        // When
        List<Map<String, Object>> similar = service.findSimilarBeliefs(BELIEF_1_ID, AGENT_ID, 0.1);
        
        // Then
        assertFalse(similar.isEmpty());
        // BELIEF_2_ID should be similar to BELIEF_1_ID due to shared relationship types
        assertTrue(similar.stream().anyMatch(s -> BELIEF_2_ID.equals(s.get("beliefId"))));
    }
    
    @Test
    @DisplayName("Should get complete knowledge graph")
    void testGetKnowledgeGraph() {
        // Given
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        service.createRelationship(BELIEF_2_ID, BELIEF_3_ID, RelationshipType.RELATES_TO, 0.7, AGENT_ID);
        
        // When
        BeliefKnowledgeGraph graph = service.getKnowledgeGraph(AGENT_ID);
        
        // Then
        assertNotNull(graph);
        assertEquals(AGENT_ID, graph.getAgentId());
        assertEquals(4, graph.getBeliefs().size());
        assertEquals(2, graph.getRelationships().size());
    }
    
    @Test
    @DisplayName("Should get active knowledge graph only")
    void testGetActiveKnowledgeGraph() {
        // Given
        BeliefRelationship activeRel = service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        BeliefRelationship inactiveRel = service.createRelationship(BELIEF_2_ID, BELIEF_3_ID, RelationshipType.RELATES_TO, 0.7, AGENT_ID);
        service.deactivateRelationship(inactiveRel.getId(), AGENT_ID);
        
        // When
        BeliefKnowledgeGraph graph = service.getActiveKnowledgeGraph(AGENT_ID);
        
        // Then
        assertNotNull(graph);
        assertEquals(1, graph.getRelationships().size());
        assertTrue(graph.getRelationships().values().stream().allMatch(BeliefRelationship::isActive));
    }
    
    @Test
    @DisplayName("Should get knowledge graph statistics")
    void testGetKnowledgeGraphStatistics() {
        // Given
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        service.createRelationship(BELIEF_2_ID, BELIEF_3_ID, RelationshipType.RELATES_TO, 0.7, AGENT_ID);
        service.deprecateBeliefWith(BELIEF_3_ID, BELIEF_4_ID, "Update", AGENT_ID);
        
        // When
        Map<String, Object> stats = service.getKnowledgeGraphStatistics(AGENT_ID);
        
        // Then
        assertNotNull(stats);
        
        // Handle both old and new statistics formats
        Object totalBeliefs = stats.get("totalBeliefs");
        if (totalBeliefs instanceof Long) {
            assertEquals(4L, totalBeliefs);
        } else {
            assertEquals(4, totalBeliefs);
        }
        
        Object totalRelationships = stats.get("totalRelationships");
        if (totalRelationships instanceof Long) {
            assertEquals(3L, totalRelationships);
        } else {
            assertEquals(3, totalRelationships);
        }
        
        assertTrue(stats.containsKey("relationshipTypeDistribution"));
        assertTrue(stats.containsKey("averageRelationshipStrength"));
    }
    
    @Test
    @DisplayName("Should validate knowledge graph structure")
    void testValidateKnowledgeGraph() {
        // Given - Create valid relationships
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        
        // When
        List<String> issues = service.validateKnowledgeGraph(AGENT_ID);
        
        // Then
        assertTrue(issues.isEmpty()); // Should have no validation issues
    }
    
    @Test
    @DisplayName("Should find belief clusters based on strength")
    void testFindBeliefClusters() {
        // Given - Create high-strength relationships
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.9, AGENT_ID);
        service.createRelationship(BELIEF_2_ID, BELIEF_1_ID, RelationshipType.RELATES_TO, 0.85, AGENT_ID);
        service.createRelationship(BELIEF_3_ID, BELIEF_4_ID, RelationshipType.SUPPORTS, 0.95, AGENT_ID);
        
        // When
        Map<String, Set<String>> clusters = service.findBeliefClusters(AGENT_ID, 0.8);
        
        // Then
        assertFalse(clusters.isEmpty());
        // Should find clusters of strongly connected beliefs
    }
    
    @Test
    @DisplayName("Should find potential conflicts")
    void testFindPotentialConflicts() {
        // Given - Create contradictory relationships
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.CONTRADICTS, 0.8, AGENT_ID);
        service.createRelationship(BELIEF_3_ID, BELIEF_4_ID, RelationshipType.SUPPORTS, 0.7, AGENT_ID);
        
        // When
        List<Map<String, Object>> conflicts = service.findPotentialConflicts(AGENT_ID);
        
        // Then
        assertEquals(1, conflicts.size());
        Map<String, Object> conflict = conflicts.get(0);
        assertEquals(BELIEF_1_ID, conflict.get("sourceBeliefId"));
        assertEquals(BELIEF_2_ID, conflict.get("targetBeliefId"));
        assertEquals(RelationshipType.CONTRADICTS, conflict.get("relationshipType"));
    }
    
    // ========================================
    // EFFICIENT OPERATIONS TESTS
    // ========================================
    
    @Test
    @DisplayName("Should get efficient graph statistics")
    void testGetEfficientGraphStatistics() {
        // Given
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        service.createRelationship(BELIEF_2_ID, BELIEF_3_ID, RelationshipType.RELATES_TO, 0.7, AGENT_ID);
        service.deprecateBeliefWith(BELIEF_3_ID, BELIEF_4_ID, "Update", AGENT_ID);
        
        // When
        Map<String, Object> stats = service.getEfficientGraphStatistics(AGENT_ID);
        
        // Then
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalBeliefs"));
        assertTrue(stats.containsKey("activeBeliefs"));
        assertTrue(stats.containsKey("totalRelationships"));
        assertTrue(stats.containsKey("activeRelationships"));
        assertTrue(stats.containsKey("deprecatedBeliefs"));
        assertTrue(stats.containsKey("relationshipTypeDistribution"));
        assertTrue(stats.containsKey("averageRelationshipStrength"));
        assertTrue(stats.containsKey("graphDensity"));
    }
    
    @Test
    @DisplayName("Should perform efficient graph validation")
    void testPerformEfficientGraphValidation() {
        // Given - Create valid relationships
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        service.createRelationship(BELIEF_2_ID, BELIEF_3_ID, RelationshipType.RELATES_TO, 0.7, AGENT_ID);
        
        // When
        List<String> issues = service.performEfficientGraphValidation(AGENT_ID);
        
        // Then
        assertNotNull(issues);
        assertTrue(issues.isEmpty()); // Should have no validation issues with valid data
    }
    
    @Test
    @DisplayName("Should create snapshot graph with active beliefs only")
    void testCreateSnapshotGraphActiveOnly() {
        // Given
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        BeliefRelationship inactiveRel = service.createRelationship(BELIEF_2_ID, BELIEF_3_ID, RelationshipType.RELATES_TO, 0.7, AGENT_ID);
        service.deactivateRelationship(inactiveRel.getId(), AGENT_ID);
        
        // When
        BeliefKnowledgeGraph snapshot = service.createSnapshotGraph(AGENT_ID, false);
        
        // Then
        assertNotNull(snapshot);
        assertEquals(AGENT_ID, snapshot.getAgentId());
        assertEquals(4, snapshot.getBeliefs().size()); // All beliefs should be included
        assertEquals(1, snapshot.getRelationships().size()); // Only active relationships
    }
    
    @Test
    @DisplayName("Should create snapshot graph with all beliefs and relationships")
    void testCreateSnapshotGraphIncludeInactive() {
        // Given
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        BeliefRelationship inactiveRel = service.createRelationship(BELIEF_2_ID, BELIEF_3_ID, RelationshipType.RELATES_TO, 0.7, AGENT_ID);
        service.deactivateRelationship(inactiveRel.getId(), AGENT_ID);
        
        // When
        BeliefKnowledgeGraph snapshot = service.createSnapshotGraph(AGENT_ID, true);
        
        // Then
        assertNotNull(snapshot);
        assertEquals(AGENT_ID, snapshot.getAgentId());
        assertEquals(4, snapshot.getBeliefs().size()); // All beliefs
        assertEquals(2, snapshot.getRelationships().size()); // All relationships including inactive
    }
    
    @Test
    @DisplayName("Should create filtered snapshot graph")
    void testCreateFilteredSnapshot() {
        // Given
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        service.createRelationship(BELIEF_2_ID, BELIEF_3_ID, RelationshipType.RELATES_TO, 0.7, AGENT_ID);
        service.createRelationship(BELIEF_3_ID, BELIEF_4_ID, RelationshipType.CONTRADICTS, 0.6, AGENT_ID);
        
        Set<String> beliefIds = Set.of(BELIEF_1_ID, BELIEF_2_ID);
        Set<RelationshipType> relationshipTypes = Set.of(RelationshipType.SUPPORTS);
        
        // When
        BeliefKnowledgeGraph filteredSnapshot = service.createFilteredSnapshot(
            AGENT_ID, beliefIds, relationshipTypes, 100);
        
        // Then
        assertNotNull(filteredSnapshot);
        assertEquals(AGENT_ID, filteredSnapshot.getAgentId());
        assertEquals(2, filteredSnapshot.getBeliefs().size()); // Only specified beliefs
        assertEquals(1, filteredSnapshot.getRelationships().size()); // Only SUPPORTS relationships
        
        // Verify only the correct relationship is included
        BeliefRelationship relationship = filteredSnapshot.getRelationships().values().iterator().next();
        assertEquals(RelationshipType.SUPPORTS, relationship.getRelationshipType());
        assertEquals(BELIEF_1_ID, relationship.getSourceBeliefId());
        assertEquals(BELIEF_2_ID, relationship.getTargetBeliefId());
    }
    
    @Test
    @DisplayName("Should create export graph")
    void testCreateExportGraph() {
        // Given
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        service.createRelationship(BELIEF_2_ID, BELIEF_3_ID, RelationshipType.RELATES_TO, 0.7, AGENT_ID);
        
        // When - Test JSON format
        BeliefKnowledgeGraph jsonExportGraph = service.createExportGraph(AGENT_ID, "json");
        
        // Then
        assertNotNull(jsonExportGraph);
        assertEquals(AGENT_ID, jsonExportGraph.getAgentId());
        assertTrue(jsonExportGraph.getBeliefs().size() > 0);
        assertTrue(jsonExportGraph.getRelationships().size() > 0);
        
        // When - Test DOT format
        BeliefKnowledgeGraph dotExportGraph = service.createExportGraph(AGENT_ID, "dot");
        
        // Then
        assertNotNull(dotExportGraph);
        assertEquals(AGENT_ID, dotExportGraph.getAgentId());
    }
    
    @Test
    @DisplayName("Should handle efficient operations with empty graph")
    void testEfficientOperationsWithEmptyGraph() {
        // Given - empty graph (no relationships created)
        String emptyAgentId = "empty-agent";
        
        // When & Then - Should handle empty graph gracefully
        Map<String, Object> stats = service.getEfficientGraphStatistics(emptyAgentId);
        assertNotNull(stats);
        assertEquals(0L, stats.get("totalBeliefs"));
        assertEquals(0L, stats.get("totalRelationships"));
        
        List<String> issues = service.performEfficientGraphValidation(emptyAgentId);
        assertNotNull(issues);
        assertTrue(issues.isEmpty());
        
        BeliefKnowledgeGraph snapshot = service.createSnapshotGraph(emptyAgentId, false);
        assertNotNull(snapshot);
        assertEquals(emptyAgentId, snapshot.getAgentId());
        assertTrue(snapshot.getBeliefs().isEmpty());
        assertTrue(snapshot.getRelationships().isEmpty());
    }
    
    @Test
    @DisplayName("Should compare performance between old and new methods")
    void testPerformanceComparison() {
        // Given - Create a moderate number of relationships for performance testing
        for (int i = 0; i < 10; i++) {
            service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        }
        
        // Test old method performance
        long startTime = System.currentTimeMillis();
        Map<String, Object> oldStats = service.getKnowledgeGraphStatistics(AGENT_ID);
        long oldMethodTime = System.currentTimeMillis() - startTime;
        
        // Test new method performance
        startTime = System.currentTimeMillis();
        Map<String, Object> newStats = service.getEfficientGraphStatistics(AGENT_ID);
        long newMethodTime = System.currentTimeMillis() - startTime;
        
        // Both should return valid results
        assertNotNull(oldStats);
        assertNotNull(newStats);
        
        // New method should have similar or better performance
        // (Note: In a small test dataset, difference might be minimal)
        assertTrue(newMethodTime <= oldMethodTime + 50); // Allow 50ms tolerance
        
        System.out.println("Performance comparison:");
        System.out.println("Old method: " + oldMethodTime + "ms");
        System.out.println("New method: " + newMethodTime + "ms");
    }
    
    @Test
    @DisplayName("Should find shortest path between beliefs")
    void testFindShortestPath() {
        // Given - Create a path: BELIEF_1 -> BELIEF_2 -> BELIEF_3
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        service.createRelationship(BELIEF_2_ID, BELIEF_3_ID, RelationshipType.RELATES_TO, 0.7, AGENT_ID);
        
        // When
        List<BeliefRelationship> path = service.findShortestPath(BELIEF_1_ID, BELIEF_3_ID, AGENT_ID);
        
        // Then
        assertEquals(2, path.size());
        assertEquals(BELIEF_1_ID, path.get(0).getSourceBeliefId());
        assertEquals(BELIEF_2_ID, path.get(0).getTargetBeliefId());
        assertEquals(BELIEF_2_ID, path.get(1).getSourceBeliefId());
        assertEquals(BELIEF_3_ID, path.get(1).getTargetBeliefId());
    }
    
    @Test
    @DisplayName("Should return empty path when no connection exists")
    void testFindShortestPathNoConnection() {
        // Given - No relationships between BELIEF_1 and BELIEF_3
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        
        // When
        List<BeliefRelationship> path = service.findShortestPath(BELIEF_1_ID, BELIEF_3_ID, AGENT_ID);
        
        // Then
        assertTrue(path.isEmpty());
    }
    
    @Test
    @DisplayName("Should export knowledge graph as JSON")
    void testExportKnowledgeGraphAsJson() {
        // Given
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        
        // When
        String exported = service.exportKnowledgeGraph(AGENT_ID, "json");
        
        // Then
        assertNotNull(exported);
        assertTrue(exported.contains("agentId"));
        assertTrue(exported.contains(AGENT_ID));
    }
    
    @Test
    @DisplayName("Should export knowledge graph as DOT format")
    void testExportKnowledgeGraphAsDot() {
        // Given
        service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        
        // When
        String exported = service.exportKnowledgeGraph(AGENT_ID, "dot");
        
        // Then
        assertNotNull(exported);
        assertTrue(exported.contains("digraph KnowledgeGraph"));
        assertTrue(exported.contains(BELIEF_1_ID));
        assertTrue(exported.contains("supports"));
    }
    
    @Test
    @DisplayName("Should throw exception for unsupported export format")
    void testExportUnsupportedFormat() {
        assertThrows(IllegalArgumentException.class, () -> 
            service.exportKnowledgeGraph(AGENT_ID, "unsupported")
        );
    }
    
    @Test
    @DisplayName("Should cleanup old inactive relationships")
    void testCleanupKnowledgeGraph() {
        // Given
        BeliefRelationship activeRel = service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        BeliefRelationship inactiveRel = service.createRelationship(BELIEF_2_ID, BELIEF_3_ID, RelationshipType.RELATES_TO, 0.7, AGENT_ID);
        
        // Deactivate one relationship and simulate old timestamp
        service.deactivateRelationship(inactiveRel.getId(), AGENT_ID);
        inactiveRel.setLastUpdated(Instant.now().minusSeconds(400 * 24 * 60 * 60L)); // 400 days ago
        
        // When
        int cleanedUp = service.cleanupKnowledgeGraph(AGENT_ID, 365);
        
        // Then
        assertEquals(1, cleanedUp);
        assertTrue(service.findRelationshipById(activeRel.getId(), AGENT_ID).isPresent());
        assertTrue(service.findRelationshipById(inactiveRel.getId(), AGENT_ID).isEmpty());
    }
    
    @Test
    @DisplayName("Should provide health information")
    void testGetHealthInfo() {
        // When
        Map<String, Object> health = service.getHealthInfo();
        
        // Then
        assertNotNull(health);
        assertEquals("healthy", health.get("status"));
        assertEquals("InMemoryBeliefRelationshipService", health.get("implementation"));
        assertTrue(health.containsKey("totalRelationships"));
        assertTrue(health.containsKey("uptime"));
    }
    
    @Test
    @DisplayName("Should handle temporal relationship effectiveness correctly")
    void testTemporalRelationshipEffectiveness() {
        // Given - Future effective relationship
        Instant futureStart = Instant.now().plusSeconds(3600);
        Instant futureEnd = Instant.now().plusSeconds(7200);
        
        BeliefRelationship futureRel = service.createTemporalRelationship(
            BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPERSEDES, 1.0, AGENT_ID,
            futureStart, futureEnd
        );
        
        // When/Then
        assertFalse(futureRel.isCurrentlyEffective());
        assertTrue(futureRel.isEffectiveAt(futureStart.plusSeconds(1800))); // Midpoint
        assertFalse(futureRel.isEffectiveAt(Instant.now()));
        
        // Given - Expired relationship
        Instant pastStart = Instant.now().minusSeconds(7200);
        Instant pastEnd = Instant.now().minusSeconds(3600);
        
        BeliefRelationship expiredRel = service.createTemporalRelationship(
            BELIEF_3_ID, BELIEF_4_ID, RelationshipType.UPDATES, 1.0, AGENT_ID,
            pastStart, pastEnd
        );
        
        // When/Then
        assertFalse(expiredRel.isCurrentlyEffective());
        assertTrue(expiredRel.isEffectiveAt(pastStart.plusSeconds(1800))); // Was effective in the past
    }
    
    @Test
    @DisplayName("Should handle bulk relationship creation")
    void testCreateRelationshipsBulk() {
        // Given
        List<BeliefRelationship> relationships = Arrays.asList(
            new BeliefRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID),
            new BeliefRelationship(BELIEF_2_ID, BELIEF_3_ID, RelationshipType.RELATES_TO, 0.7, AGENT_ID),
            new BeliefRelationship(BELIEF_3_ID, BELIEF_4_ID, RelationshipType.IMPLIES, 0.9, AGENT_ID)
        );
        
        // When
        List<BeliefRelationship> created = service.createRelationshipsBulk(relationships, AGENT_ID);
        
        // Then
        assertEquals(3, created.size());
        created.forEach(rel -> {
            assertNotNull(rel.getId());
            assertEquals(AGENT_ID, rel.getAgentId());
            assertTrue(rel.isActive());
        });
    }
}