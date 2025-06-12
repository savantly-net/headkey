package ai.headkey.persistence.services;

import ai.headkey.memory.dto.BeliefRelationship;
import ai.headkey.memory.dto.BeliefKnowledgeGraph;
import ai.headkey.memory.enums.RelationshipType;
import ai.headkey.persistence.entities.BeliefEntity;
import ai.headkey.persistence.entities.BeliefRelationshipEntity;
import ai.headkey.persistence.repositories.BeliefRelationshipRepository;
import ai.headkey.persistence.repositories.BeliefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JpaBeliefRelationshipService.
 * 
 * These tests verify the core functionality of the JPA-based belief relationship
 * service including CRUD operations, graph queries, temporal relationships,
 * and bulk operations using mocked repositories.
 */
@ExtendWith(MockitoExtension.class)
class JpaBeliefRelationshipServiceTest {

    @Mock
    private BeliefRelationshipRepository relationshipRepository;
    
    @Mock
    private BeliefRepository beliefRepository;
    
    private JpaBeliefRelationshipService service;
    
    private static final String AGENT_ID = "test-agent-001";
    private static final String BELIEF_1_ID = "belief-001";
    private static final String BELIEF_2_ID = "belief-002";
    private static final String BELIEF_3_ID = "belief-003";
    private static final String RELATIONSHIP_ID = "rel-001";

    private BeliefRelationshipEntity testRelationshipEntity;
    private BeliefEntity testBeliefEntity1;
    private BeliefEntity testBeliefEntity2;

    @BeforeEach
    void setUp() {
        service = new JpaBeliefRelationshipService(relationshipRepository, beliefRepository);
        
        // Setup test entities
        testRelationshipEntity = createTestRelationshipEntity();
        testBeliefEntity1 = createTestBeliefEntity(BELIEF_1_ID, "The sky is blue");
        testBeliefEntity2 = createTestBeliefEntity(BELIEF_2_ID, "The ocean is vast");
        

    }

    // ========== Relationship Creation Tests ==========

    @Test
    void testCreateRelationship_Success() {
        // Given
        when(beliefRepository.existsById(BELIEF_1_ID)).thenReturn(true);
        when(beliefRepository.existsById(BELIEF_2_ID)).thenReturn(true);
        when(relationshipRepository.save(any(BeliefRelationshipEntity.class)))
                .thenReturn(testRelationshipEntity);

        // When
        BeliefRelationship result = service.createRelationship(
                BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID);

        // Then
        assertNotNull(result);
        assertEquals(BELIEF_1_ID, result.getSourceBeliefId());
        assertEquals(BELIEF_2_ID, result.getTargetBeliefId());
        assertEquals(RelationshipType.SUPPORTS, result.getRelationshipType());
        assertEquals(0.8, result.getStrength());
        assertEquals(AGENT_ID, result.getAgentId());
        assertTrue(result.isActive());
        
        verify(relationshipRepository).save(any(BeliefRelationshipEntity.class));
        verify(beliefRepository, times(2)).existsById(anyString());
    }

    @Test
    void testCreateRelationship_InvalidSourceBelief() {
        // Given
        when(beliefRepository.existsById(BELIEF_1_ID)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID));
        
        assertTrue(exception.getMessage().contains("Source belief not found"));
        verify(relationshipRepository, never()).save(any());
    }

    @Test
    void testCreateRelationship_InvalidStrength() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.createRelationship(BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 1.5, AGENT_ID));
        
        assertTrue(exception.getMessage().contains("Strength must be between 0.0 and 1.0"));
        verify(relationshipRepository, never()).save(any());
    }

    @Test
    void testCreateRelationshipWithMetadata_Success() {
        // Given
        when(beliefRepository.existsById(BELIEF_1_ID)).thenReturn(true);
        when(beliefRepository.existsById(BELIEF_2_ID)).thenReturn(true);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "scientific study");
        metadata.put("confidence", 0.95);
        
        when(relationshipRepository.save(any(BeliefRelationshipEntity.class)))
                .thenReturn(testRelationshipEntity);

        // When
        BeliefRelationship result = service.createRelationshipWithMetadata(
                BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID, metadata);

        // Then
        assertNotNull(result);
        assertEquals(BELIEF_1_ID, result.getSourceBeliefId());
        assertEquals(BELIEF_2_ID, result.getTargetBeliefId());
        
        verify(relationshipRepository).save(any(BeliefRelationshipEntity.class));
    }

    @Test
    void testCreateTemporalRelationship_Success() {
        // Given
        when(beliefRepository.existsById(BELIEF_1_ID)).thenReturn(true);
        when(beliefRepository.existsById(BELIEF_2_ID)).thenReturn(true);
        Instant effectiveFrom = Instant.now();
        Instant effectiveUntil = effectiveFrom.plusSeconds(3600);
        
        when(relationshipRepository.save(any(BeliefRelationshipEntity.class)))
                .thenReturn(testRelationshipEntity);

        // When
        BeliefRelationship result = service.createTemporalRelationship(
                BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID,
                effectiveFrom, effectiveUntil);

        // Then
        assertNotNull(result);
        assertEquals(BELIEF_1_ID, result.getSourceBeliefId());
        assertEquals(BELIEF_2_ID, result.getTargetBeliefId());
        
        verify(relationshipRepository).save(any(BeliefRelationshipEntity.class));
    }

    @Test
    void testCreateTemporalRelationship_InvalidTimeRange() {
        // Given
        when(beliefRepository.existsById(BELIEF_1_ID)).thenReturn(true);
        when(beliefRepository.existsById(BELIEF_2_ID)).thenReturn(true);
        Instant effectiveFrom = Instant.now();
        Instant effectiveUntil = effectiveFrom.minusSeconds(3600); // Before effectiveFrom

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.createTemporalRelationship(
                        BELIEF_1_ID, BELIEF_2_ID, RelationshipType.SUPPORTS, 0.8, AGENT_ID,
                        effectiveFrom, effectiveUntil));
        
        assertTrue(exception.getMessage().contains("Effective from must be before effective until"));
        verify(relationshipRepository, never()).save(any());
    }

    @Test
    void testDeprecateBeliefWith_Success() {
        // Given
        when(beliefRepository.existsById(BELIEF_1_ID)).thenReturn(true);
        when(beliefRepository.existsById(BELIEF_2_ID)).thenReturn(true);
        
        // Create a superseding relationship entity for the mock
        BeliefRelationshipEntity supersedingEntity = new BeliefRelationshipEntity();
        supersedingEntity.setId("supersede-rel-001");
        supersedingEntity.setSourceBeliefId(BELIEF_2_ID); // New belief supersedes old
        supersedingEntity.setTargetBeliefId(BELIEF_1_ID); // Old belief is superseded
        supersedingEntity.setAgentId(AGENT_ID);
        supersedingEntity.setRelationshipType(RelationshipType.SUPERSEDES);
        supersedingEntity.setStrength(1.0);
        supersedingEntity.setActive(true);
        supersedingEntity.setCreatedAt(Instant.now());
        supersedingEntity.setLastUpdated(Instant.now());
        
        when(relationshipRepository.save(any(BeliefRelationshipEntity.class)))
                .thenReturn(supersedingEntity);

        // When
        BeliefRelationship result = service.deprecateBeliefWith(
                BELIEF_1_ID, BELIEF_2_ID, "Updated with new information", AGENT_ID);

        // Then
        assertNotNull(result);
        assertEquals(BELIEF_2_ID, result.getSourceBeliefId()); // New belief supersedes old
        assertEquals(BELIEF_1_ID, result.getTargetBeliefId()); // Old belief is superseded
        assertEquals(RelationshipType.SUPERSEDES, result.getRelationshipType());
        assertEquals(1.0, result.getStrength());
        
        verify(relationshipRepository).save(any(BeliefRelationshipEntity.class));
    }

    // ========== Relationship Management Tests ==========

    @Test
    void testUpdateRelationship_Success() {
        // Given
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("updated", true);
        
        when(relationshipRepository.findById(RELATIONSHIP_ID))
                .thenReturn(Optional.of(testRelationshipEntity));
        when(relationshipRepository.save(any(BeliefRelationshipEntity.class)))
                .thenReturn(testRelationshipEntity);

        // When
        BeliefRelationship result = service.updateRelationship(RELATIONSHIP_ID, 0.9, metadata);

        // Then
        assertNotNull(result);
        verify(relationshipRepository).findById(RELATIONSHIP_ID);
        verify(relationshipRepository).save(any(BeliefRelationshipEntity.class));
    }

    @Test
    void testUpdateRelationship_NotFound() {
        // Given
        when(relationshipRepository.findById(RELATIONSHIP_ID))
                .thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.updateRelationship(RELATIONSHIP_ID, 0.9, null));
        
        assertTrue(exception.getMessage().contains("Relationship not found"));
        verify(relationshipRepository, never()).save(any());
    }

    @Test
    void testDeactivateRelationship_Success() {
        // Given
        when(relationshipRepository.findById(RELATIONSHIP_ID))
                .thenReturn(Optional.of(testRelationshipEntity));
        when(relationshipRepository.save(any(BeliefRelationshipEntity.class)))
                .thenReturn(testRelationshipEntity);

        // When
        boolean result = service.deactivateRelationship(RELATIONSHIP_ID, AGENT_ID);

        // Then
        assertTrue(result);
        verify(relationshipRepository).findById(RELATIONSHIP_ID);
        verify(relationshipRepository).save(any(BeliefRelationshipEntity.class));
    }

    @Test
    void testDeactivateRelationship_WrongAgent() {
        // Given
        when(relationshipRepository.findById(RELATIONSHIP_ID))
                .thenReturn(Optional.of(testRelationshipEntity));

        // When
        boolean result = service.deactivateRelationship(RELATIONSHIP_ID, "wrong-agent");

        // Then
        assertFalse(result);
        verify(relationshipRepository).findById(RELATIONSHIP_ID);
        verify(relationshipRepository, never()).save(any());
    }

    @Test
    void testDeleteRelationship_Success() {
        // Given
        when(relationshipRepository.findById(RELATIONSHIP_ID))
                .thenReturn(Optional.of(testRelationshipEntity));

        // When
        boolean result = service.deleteRelationship(RELATIONSHIP_ID, AGENT_ID);

        // Then
        assertTrue(result);
        verify(relationshipRepository).findById(RELATIONSHIP_ID);
        verify(relationshipRepository).deleteById(RELATIONSHIP_ID);
    }

    // ========== Query Tests ==========

    @Test
    void testFindRelationshipById_Success() {
        // Given
        when(relationshipRepository.findById(RELATIONSHIP_ID))
                .thenReturn(Optional.of(testRelationshipEntity));

        // When
        Optional<BeliefRelationship> result = service.findRelationshipById(RELATIONSHIP_ID, AGENT_ID);

        // Then
        assertTrue(result.isPresent());
        assertEquals(RELATIONSHIP_ID, result.get().getId());
        verify(relationshipRepository).findById(RELATIONSHIP_ID);
    }

    @Test
    void testFindRelationshipById_NotFound() {
        // Given
        when(relationshipRepository.findById(RELATIONSHIP_ID))
                .thenReturn(Optional.empty());

        // When
        Optional<BeliefRelationship> result = service.findRelationshipById(RELATIONSHIP_ID, AGENT_ID);

        // Then
        assertFalse(result.isPresent());
        verify(relationshipRepository).findById(RELATIONSHIP_ID);
    }

    @Test
    void testFindRelationshipsForBelief_Success() {
        // Given
        List<BeliefRelationshipEntity> entities = Arrays.asList(testRelationshipEntity);
        when(relationshipRepository.findByBelief(BELIEF_1_ID, AGENT_ID, false))
                .thenReturn(entities);

        // When
        List<BeliefRelationship> result = service.findRelationshipsForBelief(BELIEF_1_ID, AGENT_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(relationshipRepository).findByBelief(BELIEF_1_ID, AGENT_ID, false);
    }

    @Test
    void testFindOutgoingRelationships_Success() {
        // Given
        List<BeliefRelationshipEntity> entities = Arrays.asList(testRelationshipEntity);
        when(relationshipRepository.findBySourceBelief(BELIEF_1_ID, AGENT_ID, false))
                .thenReturn(entities);

        // When
        List<BeliefRelationship> result = service.findOutgoingRelationships(BELIEF_1_ID, AGENT_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(relationshipRepository).findBySourceBelief(BELIEF_1_ID, AGENT_ID, false);
    }

    @Test
    void testFindIncomingRelationships_Success() {
        // Given
        List<BeliefRelationshipEntity> entities = Arrays.asList(testRelationshipEntity);
        when(relationshipRepository.findByTargetBelief(BELIEF_1_ID, AGENT_ID, false))
                .thenReturn(entities);

        // When
        List<BeliefRelationship> result = service.findIncomingRelationships(BELIEF_1_ID, AGENT_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(relationshipRepository).findByTargetBelief(BELIEF_1_ID, AGENT_ID, false);
    }

    @Test
    void testFindRelationshipsByType_Success() {
        // Given
        List<BeliefRelationshipEntity> entities = Arrays.asList(testRelationshipEntity);
        when(relationshipRepository.findByType(RelationshipType.SUPPORTS, AGENT_ID, false))
                .thenReturn(entities);

        // When
        List<BeliefRelationship> result = service.findRelationshipsByType(RelationshipType.SUPPORTS, AGENT_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(relationshipRepository).findByType(RelationshipType.SUPPORTS, AGENT_ID, false);
    }

    // ========== Graph Operations Tests ==========

    @Test
    void testCreateSnapshotGraph_Success() {
        // Given
        List<BeliefRelationshipEntity> relationshipEntities = Arrays.asList(testRelationshipEntity);
        List<BeliefEntity> beliefEntities = Arrays.asList(testBeliefEntity1, testBeliefEntity2);
        
        when(relationshipRepository.findByAgent(AGENT_ID, false))
                .thenReturn(relationshipEntities);
        when(beliefRepository.findByIds(any(Set.class)))
                .thenReturn(beliefEntities);

        // When
        BeliefKnowledgeGraph result = service.createSnapshotGraph(AGENT_ID, false);

        // Then
        assertNotNull(result);
        assertNotNull(result.getAgentId());
        assertEquals(AGENT_ID, result.getAgentId());
        assertNotNull(result.getBeliefs());
        assertEquals(2, result.getBeliefs().size());
        assertNotNull(result.getRelationships());
        assertEquals(1, result.getRelationships().size());
        
        verify(relationshipRepository).findByAgent(AGENT_ID, false);
        verify(beliefRepository).findByIds(any(Set.class));
    }

    @Test
    void testGetEfficientGraphStatistics_Success() {
        // Given
        Object[] stats = {5L, 4L, 0.75}; // total, active, avg strength
        
        List<Object[]> typeDistribution = new ArrayList<>();
        typeDistribution.add(new Object[]{RelationshipType.SUPPORTS, 3L});
        typeDistribution.add(new Object[]{RelationshipType.CONTRADICTS, 1L});
        
        List<Object[]> avgStrengthByType = new ArrayList<>();
        avgStrengthByType.add(new Object[]{RelationshipType.SUPPORTS, 0.8});
        
        when(relationshipRepository.getRelationshipStatistics(AGENT_ID))
                .thenReturn(stats);
        when(relationshipRepository.getRelationshipTypeDistribution(AGENT_ID, false))
                .thenReturn(typeDistribution);
        when(relationshipRepository.getAverageStrengthByType(AGENT_ID))
                .thenReturn(avgStrengthByType);

        // When
        Map<String, Object> result = service.getEfficientGraphStatistics(AGENT_ID);

        // Then
        assertNotNull(result);
        assertEquals(5L, result.get("totalRelationships"));
        assertEquals(4L, result.get("activeRelationships"));
        assertEquals(0.75, result.get("averageStrength"));
        assertNotNull(result.get("typeDistribution"));
        assertNotNull(result.get("averageStrengthByType"));
        assertNotNull(result.get("generatedAt"));
        
        verify(relationshipRepository).getRelationshipStatistics(AGENT_ID);
        verify(relationshipRepository).getRelationshipTypeDistribution(AGENT_ID, false);
        verify(relationshipRepository).getAverageStrengthByType(AGENT_ID);
    }

    @Test
    void testPerformEfficientGraphValidation_Success() {
        // Given
        List<BeliefRelationshipEntity> orphaned = Arrays.asList(testRelationshipEntity);
        List<BeliefRelationshipEntity> selfReferencing = new ArrayList<>();
        List<BeliefRelationshipEntity> temporallyInvalid = new ArrayList<>();
        
        when(relationshipRepository.findOrphanedRelationships(AGENT_ID))
                .thenReturn(orphaned);
        when(relationshipRepository.findSelfReferencingRelationships(AGENT_ID))
                .thenReturn(selfReferencing);
        when(relationshipRepository.findTemporallyInvalidRelationships(AGENT_ID))
                .thenReturn(temporallyInvalid);

        // When
        List<String> result = service.performEfficientGraphValidation(AGENT_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("orphaned"));
        
        verify(relationshipRepository).findOrphanedRelationships(AGENT_ID);
        verify(relationshipRepository).findSelfReferencingRelationships(AGENT_ID);
        verify(relationshipRepository).findTemporallyInvalidRelationships(AGENT_ID);
    }

    @Test
    void testGetHealthInfo_Success() {
        // When
        Map<String, Object> result = service.getHealthInfo();

        // Then
        assertNotNull(result);
        assertEquals("healthy", result.get("status"));
        assertEquals("JpaBeliefRelationshipService", result.get("implementation"));
        assertTrue(result.containsKey("totalCreateOperations"));
        assertTrue(result.containsKey("totalUpdateOperations"));
        assertTrue(result.containsKey("totalQueryOperations"));
        assertTrue(result.containsKey("uptime"));
        assertTrue(result.containsKey("createdAt"));
    }

    // ========== Bulk Operations Tests ==========

    @Test
    void testCreateRelationshipsBulk_Success() {
        // Given
        BeliefRelationship rel1 = createTestBeliefRelationship(null, BELIEF_1_ID, BELIEF_2_ID);
        BeliefRelationship rel2 = createTestBeliefRelationship(null, BELIEF_2_ID, BELIEF_3_ID);
        List<BeliefRelationship> relationships = Arrays.asList(rel1, rel2);
        
        List<BeliefRelationshipEntity> savedEntities = Arrays.asList(testRelationshipEntity, testRelationshipEntity);
        when(relationshipRepository.saveAll(any()))
                .thenReturn(savedEntities);

        // When
        List<BeliefRelationship> result = service.createRelationshipsBulk(relationships, AGENT_ID);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(relationshipRepository).saveAll(any());
    }

    @Test
    void testCleanupKnowledgeGraph_Success() {
        // Given
        int deletedCount = 5;
        when(relationshipRepository.deleteOldInactiveRelationships(AGENT_ID, 30))
                .thenReturn(deletedCount);

        // When
        int result = service.cleanupKnowledgeGraph(AGENT_ID, 30);

        // Then
        assertEquals(deletedCount, result);
        verify(relationshipRepository).deleteOldInactiveRelationships(AGENT_ID, 30);
    }

    // ========== Helper Methods ==========

    private BeliefRelationshipEntity createTestRelationshipEntity() {
        BeliefRelationshipEntity entity = new BeliefRelationshipEntity();
        entity.setId(RELATIONSHIP_ID);
        entity.setSourceBeliefId(BELIEF_1_ID);
        entity.setTargetBeliefId(BELIEF_2_ID);
        entity.setAgentId(AGENT_ID);
        entity.setRelationshipType(RelationshipType.SUPPORTS);
        entity.setStrength(0.8);
        entity.setActive(true);
        entity.setCreatedAt(Instant.now());
        entity.setLastUpdated(Instant.now());
        return entity;
    }

    private BeliefEntity createTestBeliefEntity(String id, String statement) {
        BeliefEntity entity = new BeliefEntity();
        entity.setId(id);
        entity.setAgentId(AGENT_ID);
        entity.setStatement(statement);
        entity.setActive(true);
        entity.setCreatedAt(Instant.now());
        entity.setLastUpdated(Instant.now());
        entity.setConfidence(0.8);
        entity.setReinforcementCount(0);
        return entity;
    }

    private BeliefRelationship createTestBeliefRelationship(String id, String sourceId, String targetId) {
        BeliefRelationship relationship = new BeliefRelationship(sourceId, targetId, RelationshipType.SUPPORTS, 0.8, AGENT_ID);
        if (id != null) {
            relationship.setId(id);
        }
        return relationship;
    }
}