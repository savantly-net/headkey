package ai.headkey.memory.implementations;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefRelationship;
import ai.headkey.memory.enums.RelationshipType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryBeliefGraphQueryServiceTest {

    private InMemoryBeliefGraphQueryService service;
    private Belief testBelief1;
    private Belief testBelief2;
    private BeliefRelationship testRelationship;
    private static final String AGENT_ID = "test-agent-123";

    @BeforeEach
    void setUp() {
        service = new InMemoryBeliefGraphQueryService();
        
        // Create test beliefs
        testBelief1 = new Belief();
        testBelief1.setId("belief-1");
        testBelief1.setAgentId(AGENT_ID);
        testBelief1.setStatement("The sky is blue");
        testBelief1.setCategory("observation");
        testBelief1.setConfidence(0.9);
        testBelief1.setActive(true);
        testBelief1.setCreatedAt(Instant.now());
        
        testBelief2 = new Belief();
        testBelief2.setId("belief-2");
        testBelief2.setAgentId(AGENT_ID);
        testBelief2.setStatement("The weather is clear");
        testBelief2.setCategory("observation");
        testBelief2.setConfidence(0.8);
        testBelief2.setActive(true);
        testBelief2.setCreatedAt(Instant.now());
        
        // Create test relationship
        testRelationship = new BeliefRelationship();
        testRelationship.setId("rel-1");
        testRelationship.setAgentId(AGENT_ID);
        testRelationship.setSourceBeliefId("belief-1");
        testRelationship.setTargetBeliefId("belief-2");
        testRelationship.setRelationshipType(RelationshipType.SUPPORTS);
        testRelationship.setStrength(0.7);
        testRelationship.setActive(true);
        testRelationship.setEffectiveFrom(Instant.now());
        testRelationship.setCreatedAt(Instant.now());
        
        // Add to service
        service.addBelief(testBelief1);
        service.addBelief(testBelief2);
        service.addRelationship(testRelationship);
    }

    @Test
    void testAddBelief() {
        assertTrue(service.beliefExists("belief-1", AGENT_ID));
        assertTrue(service.beliefExists("belief-2", AGENT_ID));
        assertFalse(service.beliefExists("non-existent", AGENT_ID));
    }

    @Test
    void testGetGraphStatistics() {
        Map<String, Long> stats = service.getGraphStatistics(AGENT_ID);
        
        assertNotNull(stats);
        assertEquals(2L, stats.get("totalBeliefs"));
        assertEquals(1L, stats.get("totalRelationships"));
        assertTrue(stats.containsKey("activeBeliefs"));
        assertTrue(stats.containsKey("activeRelationships"));
    }

    @Test
    void testGetBeliefsCount() {
        assertEquals(2L, service.getBeliefsCount(AGENT_ID, true));
        assertEquals(2L, service.getBeliefsCount(AGENT_ID, false));
    }

    @Test
    void testGetRelationshipsCount() {
        assertEquals(1L, service.getRelationshipsCount(AGENT_ID, true));
        assertEquals(1L, service.getRelationshipsCount(AGENT_ID, false));
    }

    @Test
    void testStreamBeliefs() {
        List<Belief> beliefs = service.streamBeliefs(AGENT_ID, false, 0).toList();
        assertEquals(2, beliefs.size());
        
        List<Belief> limitedBeliefs = service.streamBeliefs(AGENT_ID, false, 1).toList();
        assertEquals(1, limitedBeliefs.size());
    }

    @Test
    void testGetBeliefsByCategory() {
        List<Belief> observationBeliefs = service.getBeliefsInCategory(AGENT_ID, "observation", 0);
        assertEquals(2, observationBeliefs.size());
        
        List<Belief> unknownBeliefs = service.getBeliefsInCategory(AGENT_ID, "unknown", 0);
        assertEquals(0, unknownBeliefs.size());
    }

    @Test
    void testGetHighConfidenceBeliefs() {
        List<Belief> highConfidenceBeliefs = service.getHighConfidenceBeliefs(AGENT_ID, 0.85, 0);
        assertEquals(1, highConfidenceBeliefs.size());
        assertEquals("belief-1", highConfidenceBeliefs.get(0).getId());
        
        List<Belief> allBeliefs = service.getHighConfidenceBeliefs(AGENT_ID, 0.7, 0);
        assertEquals(2, allBeliefs.size());
    }

    @Test
    void testGetRecentBeliefs() {
        List<Belief> recentBeliefs = service.getRecentBeliefs(AGENT_ID, 1);
        assertEquals(1, recentBeliefs.size());
        
        List<Belief> allRecent = service.getRecentBeliefs(AGENT_ID, 10);
        assertEquals(2, allRecent.size());
    }

    @Test
    void testGetRelationshipIds() {
        List<String> outgoingRels = service.getRelationshipIds("belief-1", AGENT_ID, "outgoing", 0);
        assertEquals(1, outgoingRels.size());
        assertEquals("rel-1", outgoingRels.get(0));
        
        List<String> incomingRels = service.getRelationshipIds("belief-2", AGENT_ID, "incoming", 0);
        assertEquals(1, incomingRels.size());
        assertEquals("rel-1", incomingRels.get(0));
        
        List<String> bothRels = service.getRelationshipIds("belief-1", AGENT_ID, "both", 0);
        assertEquals(1, bothRels.size());
    }

    @Test
    void testGetConnectedBeliefIds() {
        List<String> connected = service.getConnectedBeliefIds("belief-1", AGENT_ID, "outgoing", null, 0);
        assertEquals(1, connected.size());
        assertEquals("belief-2", connected.get(0));
        
        List<String> connectedFiltered = service.getConnectedBeliefIds("belief-1", AGENT_ID, "outgoing", 
                Set.of(RelationshipType.SUPPORTS), 0);
        assertEquals(1, connectedFiltered.size());
        
        List<String> connectedWrongType = service.getConnectedBeliefIds("belief-1", AGENT_ID, "outgoing", 
                Set.of(RelationshipType.CONTRADICTS), 0);
        assertEquals(0, connectedWrongType.size());
    }

    @Test
    void testAreBeliefsDirectlyConnected() {
        assertTrue(service.areBeliefsDirectlyConnected("belief-1", "belief-2", AGENT_ID, null));
        assertTrue(service.areBeliefsDirectlyConnected("belief-1", "belief-2", AGENT_ID, 
                Set.of(RelationshipType.SUPPORTS)));
        assertFalse(service.areBeliefsDirectlyConnected("belief-1", "belief-2", AGENT_ID, 
                Set.of(RelationshipType.CONTRADICTS)));
        assertFalse(service.areBeliefsDirectlyConnected("belief-2", "belief-1", AGENT_ID, null));
    }

    @Test
    void testGetBeliefsById() {
        Map<String, Belief> beliefsMap = service.getBeliefsById(Set.of("belief-1", "belief-2"), AGENT_ID);
        assertEquals(2, beliefsMap.size());
        assertTrue(beliefsMap.containsKey("belief-1"));
        assertTrue(beliefsMap.containsKey("belief-2"));
        
        Map<String, Belief> partialMap = service.getBeliefsById(Set.of("belief-1", "non-existent"), AGENT_ID);
        assertEquals(1, partialMap.size());
        assertTrue(partialMap.containsKey("belief-1"));
    }

    @Test
    void testGetRelationshipsById() {
        Map<String, BeliefRelationship> relsMap = service.getRelationshipsById(Set.of("rel-1"), AGENT_ID);
        assertEquals(1, relsMap.size());
        assertTrue(relsMap.containsKey("rel-1"));
    }

    @Test
    void testCheckBeliefsExist() {
        Map<String, Boolean> existsMap = service.checkBeliefsExist(
                Set.of("belief-1", "belief-2", "non-existent"), AGENT_ID);
        assertEquals(3, existsMap.size());
        assertTrue(existsMap.get("belief-1"));
        assertTrue(existsMap.get("belief-2"));
        assertFalse(existsMap.get("non-existent"));
    }

    @Test
    void testGetBeliefDegrees() {
        Map<String, Long> degrees = service.getBeliefDegrees(Set.of("belief-1", "belief-2"), AGENT_ID, "both");
        assertEquals(2, degrees.size());
        assertEquals(1L, degrees.get("belief-1"));
        assertEquals(1L, degrees.get("belief-2"));
    }

    @Test
    void testSearchBeliefsByContent() {
        List<Belief> results = service.searchBeliefsByContent(AGENT_ID, "sky", 0);
        assertEquals(1, results.size());
        assertEquals("belief-1", results.get(0).getId());
        
        List<Belief> noResults = service.searchBeliefsByContent(AGENT_ID, "nonexistent", 0);
        assertEquals(0, noResults.size());
    }

    @Test
    void testGetServiceHealth() {
        Map<String, Object> health = service.getServiceHealth();
        
        assertNotNull(health);
        assertEquals("healthy", health.get("status"));
        assertEquals("InMemoryBeliefGraphQueryService", health.get("implementation"));
        assertEquals(2, health.get("totalBeliefs"));
        assertEquals(1, health.get("totalRelationships"));
        assertTrue(health.containsKey("indexesCount"));
        assertTrue(health.containsKey("agentCount"));
    }

    @Test
    void testGetPerformanceMetrics() {
        Map<String, Object> metrics = service.getPerformanceMetrics();
        
        assertNotNull(metrics);
        assertTrue(metrics.containsKey("totalIndexEntries"));
        assertTrue(metrics.containsKey("averageBeliefDegree"));
        assertTrue(metrics.containsKey("indexFragmentation"));
        assertTrue(metrics.containsKey("cacheHitRatio"));
        assertTrue(metrics.containsKey("memoryEstimate"));
        assertTrue(metrics.containsKey("graphDensity"));
        assertTrue(metrics.containsKey("averageClusterSize"));
    }

    @Test
    void testEstimateGraphMemoryUsage() {
        long memoryUsage = service.estimateGraphMemoryUsage(AGENT_ID);
        assertTrue(memoryUsage > 0);
        
        // Should be roughly: 2 beliefs * 512 bytes + 1 relationship * 256 bytes + index overhead
        assertTrue(memoryUsage > 1000); // At least 1KB for our test data
    }

    @Test
    void testGetRelationshipTypeDistribution() {
        Map<RelationshipType, Long> distribution = service.getRelationshipTypeDistribution(AGENT_ID);
        assertEquals(1, distribution.size());
        assertEquals(1L, distribution.get(RelationshipType.SUPPORTS));
    }

    @Test
    void testValidateGraphStructure() {
        List<String> issues = service.validateGraphStructure(AGENT_ID);
        assertEquals(0, issues.size()); // Should be no issues with our valid test data
    }

    @Test
    void testRemoveBelief() {
        assertTrue(service.beliefExists("belief-1", AGENT_ID));
        service.removeBelief("belief-1");
        assertFalse(service.beliefExists("belief-1", AGENT_ID));
        assertEquals(1L, service.getBeliefsCount(AGENT_ID, true));
    }

    @Test
    void testRemoveRelationship() {
        assertEquals(1L, service.getRelationshipsCount(AGENT_ID, true));
        service.removeRelationship("rel-1");
        assertEquals(0L, service.getRelationshipsCount(AGENT_ID, true));
    }
}