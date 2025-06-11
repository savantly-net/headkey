package ai.headkey.memory.examples;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefRelationship;
import ai.headkey.memory.dto.BeliefKnowledgeGraph;
import ai.headkey.memory.enums.RelationshipType;
import ai.headkey.memory.implementations.InMemoryBeliefRelationshipService;

import java.time.Instant;
import java.util.*;

/**
 * Demonstration class showing how to use the belief relationship system
 * to create and manage knowledge graphs with rich connections between beliefs.
 * 
 * This example demonstrates:
 * - Creating beliefs and relationships
 * - Temporal deprecation of beliefs
 * - Graph traversal and analysis
 * - Knowledge graph operations
 * 
 * @since 1.0
 */
public class BeliefRelationshipDemo {
    
    private final InMemoryBeliefRelationshipService relationshipService;
    private final String agentId = "demo-agent-001";
    
    public BeliefRelationshipDemo() {
        this.relationshipService = new InMemoryBeliefRelationshipService();
        setupBeliefs();
    }
    
    /**
     * Sets up a collection of beliefs for demonstration purposes.
     */
    private void setupBeliefs() {
        Map<String, Belief> beliefs = new HashMap<>();
        
        // User preferences
        beliefs.put("belief-001", new Belief("belief-001", agentId, "User likes coffee", 0.9));
        beliefs.put("belief-002", new Belief("belief-002", agentId, "User prefers morning beverages", 0.8));
        beliefs.put("belief-003", new Belief("belief-003", agentId, "User dislikes tea", 0.6));
        beliefs.put("belief-004", new Belief("belief-004", agentId, "User enjoys hot drinks", 0.85));
        
        // Knowledge facts
        beliefs.put("belief-005", new Belief("belief-005", agentId, "Coffee is a morning beverage", 0.95));
        beliefs.put("belief-006", new Belief("belief-006", agentId, "Coffee contains caffeine", 0.98));
        beliefs.put("belief-007", new Belief("belief-007", agentId, "Tea is a hot drink", 0.9));
        beliefs.put("belief-008", new Belief("belief-008", agentId, "Caffeine provides energy", 0.85));
        
        // Updated beliefs (for temporal demonstration)
        beliefs.put("belief-009", new Belief("belief-009", agentId, "User likes both coffee and tea", 0.8));
        beliefs.put("belief-010", new Belief("belief-010", agentId, "User prefers afternoon tea", 0.75));
        
        relationshipService.setBeliefs(beliefs);
    }
    
    /**
     * Demonstrates basic relationship creation between beliefs.
     */
    public void demonstrateBasicRelationships() {
        System.out.println("=== Basic Relationship Creation ===");
        
        // Create supporting relationships
        BeliefRelationship supports1 = relationshipService.createRelationship(
            "belief-001", "belief-002", RelationshipType.SUPPORTS, 0.8, agentId
        );
        System.out.println("Created relationship: " + supports1.getRelationshipType() + 
                          " between belief-001 and belief-002");
        
        // Create semantic relationships
        BeliefRelationship specializes = relationshipService.createRelationship(
            "belief-005", "belief-002", RelationshipType.SPECIALIZES, 0.9, agentId
        );
        System.out.println("Created relationship: " + specializes.getRelationshipType() + 
                          " between belief-005 and belief-002");
        
        // Create logical relationships
        BeliefRelationship implies = relationshipService.createRelationship(
            "belief-006", "belief-008", RelationshipType.IMPLIES, 0.85, agentId
        );
        System.out.println("Created relationship: " + implies.getRelationshipType() + 
                          " between belief-006 and belief-008");
        
        // Create contradictory relationship
        BeliefRelationship contradicts = relationshipService.createRelationship(
            "belief-001", "belief-003", RelationshipType.CONTRADICTS, 0.7, agentId
        );
        System.out.println("Created relationship: " + contradicts.getRelationshipType() + 
                          " between belief-001 and belief-003");
        
        System.out.println();
    }
    
    /**
     * Demonstrates temporal relationships and belief deprecation.
     */
    public void demonstrateTemporalRelationships() {
        System.out.println("=== Temporal Relationships and Deprecation ===");
        
        // Deprecate old belief with new one
        BeliefRelationship deprecation = relationshipService.deprecateBeliefWith(
            "belief-003", "belief-009", 
            "User changed preference - now likes both coffee and tea", 
            agentId
        );
        System.out.println("Deprecated belief-003 with belief-009: " + deprecation.getDeprecationReason());
        
        // Create temporal relationship with specific effective period
        Instant effectiveFrom = Instant.now();
        Instant effectiveUntil = Instant.now().plusSeconds(86400); // 24 hours
        
        BeliefRelationship temporal = relationshipService.createTemporalRelationship(
            "belief-010", "belief-002", RelationshipType.UPDATES, 0.9, agentId,
            effectiveFrom, effectiveUntil
        );
        System.out.println("Created temporal relationship effective for 24 hours");
        
        // Find deprecated beliefs
        List<String> deprecated = relationshipService.findDeprecatedBeliefs(agentId);
        System.out.println("Deprecated beliefs: " + deprecated);
        
        // Find superseding beliefs
        List<Belief> superseding = relationshipService.findSupersedingBeliefs("belief-003", agentId);
        System.out.println("Beliefs superseding belief-003: " + 
                          superseding.stream().map(Belief::getStatement).toList());
        
        System.out.println();
    }
    
    /**
     * Demonstrates graph traversal and analysis operations.
     */
    public void demonstrateGraphTraversal() {
        System.out.println("=== Graph Traversal and Analysis ===");
        
        // Find all relationships for a belief
        List<BeliefRelationship> allRels = relationshipService.findRelationshipsForBelief("belief-001", agentId);
        System.out.println("Total relationships for belief-001: " + allRels.size());
        
        // Find outgoing relationships
        List<BeliefRelationship> outgoing = relationshipService.findOutgoingRelationships("belief-001", agentId);
        System.out.println("Outgoing relationships from belief-001: " + outgoing.size());
        
        // Find incoming relationships
        List<BeliefRelationship> incoming = relationshipService.findIncomingRelationships("belief-002", agentId);
        System.out.println("Incoming relationships to belief-002: " + incoming.size());
        
        // Find related beliefs within depth
        Set<String> related = relationshipService.findRelatedBeliefs("belief-001", agentId, 2);
        System.out.println("Beliefs related to belief-001 within depth 2: " + related);
        
        // Find shortest path between beliefs
        List<BeliefRelationship> path = relationshipService.findShortestPath("belief-001", "belief-008", agentId);
        System.out.println("Shortest path from belief-001 to belief-008: " + path.size() + " hops");
        
        System.out.println();
    }
    
    /**
     * Demonstrates knowledge graph operations and analytics.
     */
    public void demonstrateKnowledgeGraphAnalytics() {
        System.out.println("=== Knowledge Graph Analytics (Performance Comparison) ===");
        
        // Performance comparison: Old vs New methods
        System.out.println("\n--- OLD METHOD (Deprecated - loads full graph) ---");
        long startTime = System.currentTimeMillis();
        
        BeliefKnowledgeGraph graph = relationshipService.getKnowledgeGraph(agentId);
        System.out.println("⚠️  DEPRECATED: getKnowledgeGraph() - loads full graph into memory");
        System.out.println("Knowledge graph contains:");
        System.out.println("  - Beliefs: " + graph.getBeliefs().size());
        System.out.println("  - Relationships: " + graph.getRelationships().size());
        
        Map<String, Object> oldStats = relationshipService.getKnowledgeGraphStatistics(agentId);
        System.out.println("⚠️  DEPRECATED: Graph statistics (loads full graph):");
        oldStats.forEach((key, value) -> System.out.println("  - " + key + ": " + value));
        
        List<String> oldValidation = relationshipService.validateKnowledgeGraph(agentId);
        System.out.println("⚠️  DEPRECATED: Validation issues (loads full graph): " + oldValidation.size());
        
        long oldMethodTime = System.currentTimeMillis() - startTime;
        System.out.println("Old method execution time: " + oldMethodTime + "ms");
        
        System.out.println("\n--- NEW EFFICIENT METHODS (Recommended) ---");
        startTime = System.currentTimeMillis();
        
        // Use new efficient methods
        Map<String, Object> efficientStats = relationshipService.getEfficientGraphStatistics(agentId);
        System.out.println("✅ EFFICIENT: Graph statistics (database-level):");
        efficientStats.forEach((key, value) -> System.out.println("  - " + key + ": " + value));
        
        List<String> efficientValidation = relationshipService.performEfficientGraphValidation(agentId);
        System.out.println("✅ EFFICIENT: Validation issues (database queries): " + efficientValidation.size());
        if (!efficientValidation.isEmpty()) {
            efficientValidation.forEach(issue -> System.out.println("  - " + issue));
        }
        
        // Demonstrate lightweight snapshot for small operations
        BeliefKnowledgeGraph snapshot = relationshipService.createSnapshotGraph(agentId, false);
        System.out.println("✅ EFFICIENT: Lightweight snapshot (active only):");
        System.out.println("  - Beliefs: " + snapshot.getBeliefs().size());
        System.out.println("  - Relationships: " + snapshot.getRelationships().size());
        
        long newMethodTime = System.currentTimeMillis() - startTime;
        System.out.println("New efficient methods execution time: " + newMethodTime + "ms");
        
        // Performance summary
        System.out.println("\n--- PERFORMANCE SUMMARY ---");
        System.out.println("Performance improvement: " + 
            (oldMethodTime > newMethodTime ? 
                ((oldMethodTime - newMethodTime) * 100 / oldMethodTime) + "% faster" :
                "Similar performance (small dataset)"));
        
        // Find belief clusters (still uses efficient method)
        Map<String, Set<String>> clusters = relationshipService.findBeliefClusters(agentId, 0.7);
        System.out.println("Belief clusters (strength ≥ 0.7): " + clusters.size());
        clusters.forEach((clusterName, beliefs) -> 
            System.out.println("  - " + clusterName + ": " + beliefs.size() + " beliefs"));
        
        // Find potential conflicts
        List<Map<String, Object>> conflicts = relationshipService.findPotentialConflicts(agentId);
        System.out.println("Potential conflicts detected: " + conflicts.size());
        conflicts.forEach(conflict -> 
            System.out.println("  - " + conflict.get("relationshipType") + " between " + 
                             conflict.get("sourceBeliefId") + " and " + conflict.get("targetBeliefId")));
        
        System.out.println();
    }
    
    /**
     * Demonstrates the new efficient graph snapshot creation methods.
     */
    public void demonstrateEfficientGraphOperations() {
        System.out.println("=== Efficient Graph Operations Demo ===");
        
        // Create different types of snapshots
        System.out.println("\n--- Snapshot Graph Creation ---");
        
        // Active-only snapshot (recommended for most operations)
        BeliefKnowledgeGraph activeSnapshot = relationshipService.createSnapshotGraph(agentId, false);
        System.out.println("Active snapshot: " + activeSnapshot.getBeliefs().size() + 
                          " beliefs, " + activeSnapshot.getRelationships().size() + " relationships");
        
        // Full snapshot (includes inactive)
        BeliefKnowledgeGraph fullSnapshot = relationshipService.createSnapshotGraph(agentId, true);
        System.out.println("Full snapshot: " + fullSnapshot.getBeliefs().size() + 
                          " beliefs, " + fullSnapshot.getRelationships().size() + " relationships");
        
        // Filtered snapshot example
        Set<RelationshipType> supportTypes = Set.of(RelationshipType.SUPPORTS, RelationshipType.REINFORCES);
        BeliefKnowledgeGraph filteredSnapshot = relationshipService.createFilteredSnapshot(
            agentId, null, supportTypes, 500);
        System.out.println("Filtered snapshot (support relationships only): " + 
                          filteredSnapshot.getBeliefs().size() + " beliefs, " + 
                          filteredSnapshot.getRelationships().size() + " relationships");
        
        // Export-optimized snapshot
        BeliefKnowledgeGraph exportSnapshot = relationshipService.createExportGraph(agentId, "json");
        System.out.println("Export snapshot: " + exportSnapshot.getBeliefs().size() + 
                          " beliefs, " + exportSnapshot.getRelationships().size() + " relationships");
        
        System.out.println("\n--- Best Practices ---");
        System.out.println("✅ Use createSnapshotGraph() for small graphs (<1000 beliefs)");
        System.out.println("✅ Use createFilteredSnapshot() for specific data subsets");
        System.out.println("✅ Use createExportGraph() for export operations");
        System.out.println("✅ Use getEfficientGraphStatistics() for metrics");
        System.out.println("✅ Use performEfficientGraphValidation() for validation");
        System.out.println("⚠️  Avoid getKnowledgeGraph() for large datasets");
        
        System.out.println();
    }
    
    /**
     * Demonstrates advanced relationship operations with metadata.
     */
    public void demonstrateAdvancedOperations() {
        System.out.println("=== Advanced Operations ===");
        
        // Create relationship with rich metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "user_conversation");
        metadata.put("confidence", 0.92);
        metadata.put("context", "morning_routine_discussion");
        metadata.put("timestamp", Instant.now().toString());
        metadata.put("evidence_count", 3);
        
        BeliefRelationship richRel = relationshipService.createRelationshipWithMetadata(
            "belief-004", "belief-005", RelationshipType.RELATES_TO, 0.85, agentId, metadata
        );
        System.out.println("Created relationship with metadata: " + richRel.getMetadata().size() + " properties");
        
        // Find relationships by type
        List<BeliefRelationship> supportRels = relationshipService.findRelationshipsByType(
            RelationshipType.SUPPORTS, agentId
        );
        System.out.println("SUPPORTS relationships: " + supportRels.size());
        
        List<BeliefRelationship> contradictRels = relationshipService.findRelationshipsByType(
            RelationshipType.CONTRADICTS, agentId
        );
        System.out.println("CONTRADICTS relationships: " + contradictRels.size());
        
        // Find similar beliefs based on relationship patterns
        List<Map<String, Object>> similar = relationshipService.findSimilarBeliefs("belief-001", agentId, 0.1);
        System.out.println("Beliefs similar to belief-001: " + similar.size());
        similar.forEach(sim -> 
            System.out.println("  - " + sim.get("beliefId") + " (similarity: " + 
                             String.format("%.3f", (Double) sim.get("similarity")) + ")"));
        
        System.out.println();
    }
    
    /**
     * Demonstrates export and import capabilities.
     */
    public void demonstrateExportImport() {
        System.out.println("=== Export and Import ===");
        
        // Export as JSON
        String jsonExport = relationshipService.exportKnowledgeGraph(agentId, "json");
        System.out.println("JSON export preview: " + jsonExport.substring(0, Math.min(100, jsonExport.length())) + "...");
        
        // Export as DOT format for visualization
        String dotExport = relationshipService.exportKnowledgeGraph(agentId, "dot");
        System.out.println("DOT export preview: " + dotExport.substring(0, Math.min(100, dotExport.length())) + "...");
        
        // Cleanup old relationships
        int cleanedUp = relationshipService.cleanupKnowledgeGraph(agentId, 30);
        System.out.println("Cleaned up " + cleanedUp + " old inactive relationships");
        
        System.out.println();
    }
    
    /**
     * Demonstrates the deprecation chain functionality.
     */
    public void demonstrateDeprecationChain() {
        System.out.println("=== Deprecation Chain Example ===");
        
        // Create a chain of belief evolution
        // Original belief -> Updated belief -> Latest belief
        
        // Create some evolution beliefs
        relationshipService.setBeliefs(Map.of(
            "belief-v1", new Belief("belief-v1", agentId, "User drinks coffee occasionally", 0.6),
            "belief-v2", new Belief("belief-v2", agentId, "User drinks coffee daily", 0.8),
            "belief-v3", new Belief("belief-v3", agentId, "User drinks coffee multiple times daily", 0.9)
        ));
        
        // Create evolution chain
        relationshipService.deprecateBeliefWith("belief-v1", "belief-v2", "Observed daily coffee consumption", agentId);
        relationshipService.deprecateBeliefWith("belief-v2", "belief-v3", "Noted multiple cups per day", agentId);
        
        // Trace the evolution
        List<Belief> chain = relationshipService.findDeprecationChain("belief-v1", agentId);
        System.out.println("Belief evolution chain starting from belief-v1:");
        for (int i = 0; i < chain.size(); i++) {
            Belief belief = chain.get(i);
            System.out.println("  " + (i + 1) + ". " + belief.getId() + ": " + belief.getStatement() + 
                             " (confidence: " + belief.getConfidence() + ")");
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates real-world scenario: User preference evolution.
     */
    public void demonstrateRealWorldScenario() {
        System.out.println("=== Real-World Scenario: User Preference Evolution ===");
        
        // Initial beliefs about user's beverage preferences
        Map<String, Belief> scenarioBeliefs = new HashMap<>();
        scenarioBeliefs.put("pref-001", new Belief("pref-001", agentId, "User prefers black coffee", 0.7));
        scenarioBeliefs.put("pref-002", new Belief("pref-002", agentId, "User dislikes sweet drinks", 0.8));
        scenarioBeliefs.put("pref-003", new Belief("pref-003", agentId, "User drinks coffee for energy", 0.9));
        scenarioBeliefs.put("pref-004", new Belief("pref-004", agentId, "User avoids afternoon caffeine", 0.6));
        
        // New observations
        scenarioBeliefs.put("pref-005", new Belief("pref-005", agentId, "User started adding milk to coffee", 0.8));
        scenarioBeliefs.put("pref-006", new Belief("pref-006", agentId, "User enjoys flavored lattes", 0.75));
        scenarioBeliefs.put("pref-007", new Belief("pref-007", agentId, "User drinks tea in afternoons", 0.85));
        
        relationshipService.setBeliefs(scenarioBeliefs);
        
        // Create initial relationship network
        relationshipService.createRelationship("pref-001", "pref-003", RelationshipType.SUPPORTS, 0.8, agentId);
        relationshipService.createRelationship("pref-002", "pref-001", RelationshipType.SUPPORTS, 0.7, agentId);
        relationshipService.createRelationship("pref-003", "pref-004", RelationshipType.IMPLIES, 0.6, agentId);
        
        // New evidence contradicts old beliefs
        relationshipService.createRelationship("pref-005", "pref-001", RelationshipType.CONTRADICTS, 0.8, agentId);
        relationshipService.createRelationship("pref-006", "pref-002", RelationshipType.CONTRADICTS, 0.9, agentId);
        relationshipService.createRelationship("pref-007", "pref-004", RelationshipType.CONTRADICTS, 0.7, agentId);
        
        // Update beliefs based on new evidence
        relationshipService.deprecateBeliefWith("pref-001", "pref-005", "User behavior changed - now adds milk", agentId);
        relationshipService.deprecateBeliefWith("pref-002", "pref-006", "User now enjoys sweet flavored drinks", agentId);
        
        // Create new supporting relationships
        relationshipService.createRelationship("pref-005", "pref-006", RelationshipType.SUPPORTS, 0.8, agentId);
        relationshipService.createRelationship("pref-007", "pref-004", RelationshipType.UPDATES, 0.9, agentId);
        
        System.out.println("Scenario Analysis:");
        System.out.println("- Deprecated beliefs: " + relationshipService.findDeprecatedBeliefs(agentId));
        System.out.println("- Conflicting relationships: " + relationshipService.findPotentialConflicts(agentId).size());
        System.out.println("- Related beliefs to pref-001: " + relationshipService.findRelatedBeliefs("pref-001", agentId, 2));
        
        BeliefKnowledgeGraph finalGraph = relationshipService.getActiveKnowledgeGraph(agentId);
        System.out.println("- Final active graph: " + finalGraph.getBeliefs().size() + " beliefs, " + 
                          finalGraph.getRelationships().size() + " relationships");
        
        System.out.println();
    }
    
    /**
     * Main method to run all demonstrations.
     */
    public static void main(String[] args) {
        System.out.println("HeadKey Belief Relationship System Demo");
        System.out.println("======================================");
        System.out.println();
        
        BeliefRelationshipDemo demo = new BeliefRelationshipDemo();
        
        try {
            demo.demonstrateBasicRelationships();
            demo.demonstrateTemporalRelationships();
            demo.demonstrateGraphTraversal();
            demo.demonstrateKnowledgeGraphAnalytics();
            demo.demonstrateEfficientGraphOperations();
            demo.demonstrateAdvancedOperations();
            demo.demonstrateDeprecationChain();
            demo.demonstrateExportImport();
            demo.demonstrateRealWorldScenario();
            
            System.out.println("Demo completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Demo failed with error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}