package ai.headkey.memory.implementations;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefRelationship;
import ai.headkey.memory.dto.BeliefKnowledgeGraph;
import ai.headkey.memory.enums.RelationshipType;
import ai.headkey.memory.interfaces.BeliefRelationshipService;
import ai.headkey.memory.interfaces.BeliefGraphQueryService;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of BeliefRelationshipService for development and testing.
 * 
 * This implementation stores belief relationships in memory using concurrent data structures
 * for thread safety. It provides full functionality for managing belief relationships
 * including temporal deprecation, graph traversal, and analytics.
 * 
 * Note: This implementation is not persistent across application restarts and is intended
 * for development, testing, and prototyping purposes.
 * 
 * @since 1.0
 */
public class InMemoryBeliefRelationshipService implements BeliefRelationshipService {
    
    private final Map<String, BeliefRelationship> relationships = new ConcurrentHashMap<>();
    private final Map<String, List<String>> outgoingRelationships = new ConcurrentHashMap<>();
    private final Map<String, List<String>> incomingRelationships = new ConcurrentHashMap<>();
    private final Map<String, Map<String, List<String>>> agentRelationships = new ConcurrentHashMap<>();
    
    // For accessing beliefs - this would typically be injected
    private final Map<String, Belief> beliefs = new ConcurrentHashMap<>();
    
    // Efficient query service for heavy operations
    private final BeliefGraphQueryService queryService;
    
    /**
     * Constructor for dependency injection.
     */
    public InMemoryBeliefRelationshipService() {
        // Initialize with efficient query service
        this.queryService = new EfficientBeliefGraphQueryService();
        // Initialize data structures
    }
    
    /**
     * Constructor with query service injection.
     */
    public InMemoryBeliefRelationshipService(BeliefGraphQueryService queryService) {
        this.queryService = queryService != null ? queryService : new EfficientBeliefGraphQueryService();
        // Synchronize initial data if query service is EfficientBeliefGraphQueryService
        if (this.queryService instanceof EfficientBeliefGraphQueryService) {
            synchronizeWithQueryService();
        }
    }
    
    /**
     * Sets the beliefs map for testing purposes.
     * In a real implementation, this would be injected or accessed via a BeliefService.
     */
    public void setBeliefs(Map<String, Belief> beliefs) {
        this.beliefs.clear();
        this.beliefs.putAll(beliefs);
        synchronizeWithQueryService();
    }
    
    /**
     * Synchronizes data with the query service for efficient operations.
     */
    private void synchronizeWithQueryService() {
        if (queryService instanceof EfficientBeliefGraphQueryService) {
            EfficientBeliefGraphQueryService efficientService = (EfficientBeliefGraphQueryService) queryService;
            // Add all beliefs
            for (Belief belief : beliefs.values()) {
                efficientService.addBelief(belief);
            }
            // Add all relationships
            for (BeliefRelationship relationship : relationships.values()) {
                efficientService.addRelationship(relationship);
            }
        }
    }
    
    @Override
    public BeliefRelationship createRelationship(String sourceBeliefId, String targetBeliefId, 
                                               RelationshipType relationshipType, double strength, String agentId) {
        validateRelationshipParameters(sourceBeliefId, targetBeliefId, relationshipType, agentId);
        
        String relationshipId = generateRelationshipId();
        BeliefRelationship relationship = new BeliefRelationship(
            sourceBeliefId, targetBeliefId, relationshipType, strength, agentId
        );
        relationship.setId(relationshipId);
        
        storeRelationship(relationship);
        return relationship;
    }
    
    @Override
    public BeliefRelationship createRelationshipWithMetadata(String sourceBeliefId, String targetBeliefId, 
                                                           RelationshipType relationshipType, double strength, 
                                                           String agentId, Map<String, Object> metadata) {
        BeliefRelationship relationship = createRelationship(sourceBeliefId, targetBeliefId, relationshipType, strength, agentId);
        if (metadata != null) {
            relationship.setMetadata(metadata);
        }
        storeRelationship(relationship);
        return relationship;
    }
    
    @Override
    public BeliefRelationship createTemporalRelationship(String sourceBeliefId, String targetBeliefId, 
                                                       RelationshipType relationshipType, double strength, 
                                                       String agentId, Instant effectiveFrom, Instant effectiveUntil) {
        BeliefRelationship relationship = createRelationship(sourceBeliefId, targetBeliefId, relationshipType, strength, agentId);
        relationship.setEffectiveFrom(effectiveFrom);
        relationship.setEffectiveUntil(effectiveUntil);
        storeRelationship(relationship);
        return relationship;
    }
    
    @Override
    public BeliefRelationship deprecateBeliefWith(String oldBeliefId, String newBeliefId, 
                                                String reason, String agentId) {
        validateRelationshipParameters(newBeliefId, oldBeliefId, RelationshipType.SUPERSEDES, agentId);
        
        String relationshipId = generateRelationshipId();
        BeliefRelationship relationship = new BeliefRelationship(
            newBeliefId, oldBeliefId, RelationshipType.SUPERSEDES, 1.0, agentId
        );
        relationship.setId(relationshipId);
        relationship.setDeprecationReason(reason);
        relationship.setEffectiveFrom(Instant.now());
        relationship.addMetadata("deprecation_reason", reason);
        relationship.addMetadata("deprecated_at", Instant.now().toString());
        
        storeRelationship(relationship);
        return relationship;
    }
    
    @Override
    public BeliefRelationship updateRelationship(String relationshipId, double strength, 
                                               Map<String, Object> metadata) {
        BeliefRelationship relationship = relationships.get(relationshipId);
        if (relationship == null) {
            throw new IllegalArgumentException("Relationship not found: " + relationshipId);
        }
        
        relationship.setStrength(strength);
        if (metadata != null) {
            relationship.setMetadata(metadata);
        }
        relationship.setLastUpdated(Instant.now());
        synchronizeRelationshipWithQueryService(relationship);
        
        return relationship;
    }
    
    @Override
    public boolean deactivateRelationship(String relationshipId, String agentId) {
        BeliefRelationship relationship = relationships.get(relationshipId);
        if (relationship == null || !relationship.getAgentId().equals(agentId)) {
            return false;
        }
        
        relationship.deactivate();
        synchronizeRelationshipWithQueryService(relationship);
        return true;
    }
    
    @Override
    public boolean reactivateRelationship(String relationshipId, String agentId) {
        BeliefRelationship relationship = relationships.get(relationshipId);
        if (relationship == null || !relationship.getAgentId().equals(agentId)) {
            return false;
        }
        
        relationship.reactivate();
        synchronizeRelationshipWithQueryService(relationship);
        return true;
    }
    
    @Override
    public boolean deleteRelationship(String relationshipId, String agentId) {
        BeliefRelationship relationship = relationships.get(relationshipId);
        if (relationship == null || !relationship.getAgentId().equals(agentId)) {
            return false;
        }
        
        removeRelationship(relationship);
        removeRelationshipFromQueryService(relationship);
        return true;
    }
    
    @Override
    public Optional<BeliefRelationship> findRelationshipById(String relationshipId, String agentId) {
        BeliefRelationship relationship = relationships.get(relationshipId);
        if (relationship != null && relationship.getAgentId().equals(agentId)) {
            return Optional.of(relationship);
        }
        return Optional.empty();
    }
    
    @Override
    public List<BeliefRelationship> findRelationshipsForBelief(String beliefId, String agentId) {
        List<BeliefRelationship> result = new ArrayList<>();
        result.addAll(findOutgoingRelationships(beliefId, agentId));
        result.addAll(findIncomingRelationships(beliefId, agentId));
        return result.stream().distinct().collect(Collectors.toList());
    }
    
    @Override
    public List<BeliefRelationship> findOutgoingRelationships(String beliefId, String agentId) {
        return outgoingRelationships.getOrDefault(beliefId, Collections.emptyList()).stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .filter(rel -> rel.getAgentId().equals(agentId))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<BeliefRelationship> findIncomingRelationships(String beliefId, String agentId) {
        return incomingRelationships.getOrDefault(beliefId, Collections.emptyList()).stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .filter(rel -> rel.getAgentId().equals(agentId))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<BeliefRelationship> findRelationshipsByType(RelationshipType relationshipType, String agentId) {
        return relationships.values().stream()
                .filter(rel -> rel.getRelationshipType() == relationshipType)
                .filter(rel -> rel.getAgentId().equals(agentId))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<BeliefRelationship> findRelationshipsBetween(String sourceBeliefId, String targetBeliefId, String agentId) {
        return relationships.values().stream()
                .filter(rel -> rel.getSourceBeliefId().equals(sourceBeliefId))
                .filter(rel -> rel.getTargetBeliefId().equals(targetBeliefId))
                .filter(rel -> rel.getAgentId().equals(agentId))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<String> findDeprecatedBeliefs(String agentId) {
        return relationships.values().stream()
                .filter(rel -> rel.getAgentId().equals(agentId))
                .filter(BeliefRelationship::isDeprecating)
                .filter(BeliefRelationship::isCurrentlyEffective)
                .map(BeliefRelationship::getTargetBeliefId)
                .distinct()
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Belief> findSupersedingBeliefs(String beliefId, String agentId) {
        return findIncomingRelationships(beliefId, agentId).stream()
                .filter(rel -> rel.getRelationshipType() == RelationshipType.SUPERSEDES)
                .filter(BeliefRelationship::isCurrentlyEffective)
                .map(rel -> beliefs.get(rel.getSourceBeliefId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Belief> findDeprecationChain(String beliefId, String agentId) {
        List<Belief> chain = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        findDeprecationChainRecursive(beliefId, chain, visited, agentId);
        return chain;
    }
    
    private void findDeprecationChainRecursive(String beliefId, List<Belief> chain, Set<String> visited, String agentId) {
        if (visited.contains(beliefId)) {
            return; // Avoid cycles
        }
        
        visited.add(beliefId);
        Belief belief = beliefs.get(beliefId);
        if (belief != null) {
            chain.add(belief);
        }
        
        // Find beliefs that supersede this one
        findIncomingRelationships(beliefId, agentId).stream()
                .filter(BeliefRelationship::isDeprecating)
                .filter(BeliefRelationship::isCurrentlyEffective)
                .forEach(rel -> findDeprecationChainRecursive(rel.getSourceBeliefId(), chain, visited, agentId));
    }
    
    @Override
    public Set<String> findRelatedBeliefs(String beliefId, String agentId, int maxDepth) {
        Set<String> related = new HashSet<>();
        Set<String> visited = new HashSet<>();
        findRelatedBeliefsRecursive(beliefId, related, visited, 0, maxDepth, agentId);
        related.remove(beliefId); // Remove the starting belief
        return related;
    }
    
    private void findRelatedBeliefsRecursive(String beliefId, Set<String> related, Set<String> visited, 
                                           int currentDepth, int maxDepth, String agentId) {
        if (currentDepth >= maxDepth || visited.contains(beliefId)) {
            return;
        }
        
        visited.add(beliefId);
        related.add(beliefId);
        
        // Traverse outgoing relationships
        findOutgoingRelationships(beliefId, agentId).stream()
                .filter(BeliefRelationship::isCurrentlyEffective)
                .forEach(rel -> findRelatedBeliefsRecursive(rel.getTargetBeliefId(), related, visited, currentDepth + 1, maxDepth, agentId));
        
        // Traverse incoming relationships
        findIncomingRelationships(beliefId, agentId).stream()
                .filter(BeliefRelationship::isCurrentlyEffective)
                .forEach(rel -> findRelatedBeliefsRecursive(rel.getSourceBeliefId(), related, visited, currentDepth + 1, maxDepth, agentId));
    }
    
    @Override
    public List<Map<String, Object>> findSimilarBeliefs(String beliefId, String agentId, double similarityThreshold) {
        // Simple implementation based on shared relationships
        List<Map<String, Object>> similarBeliefs = new ArrayList<>();
        
        List<BeliefRelationship> targetRelationships = findRelationshipsForBelief(beliefId, agentId);
        
        Set<String> allBeliefs = beliefs.keySet().stream()
                .filter(id -> beliefs.get(id).getAgentId().equals(agentId))
                .collect(Collectors.toSet());
        
        for (String otherId : allBeliefs) {
            if (!otherId.equals(beliefId)) {
                List<BeliefRelationship> otherRelationships = findRelationshipsForBelief(otherId, agentId);
                double similarity = calculateSimilarity(targetRelationships, otherRelationships);
                
                if (similarity >= similarityThreshold) {
                    Map<String, Object> similarBelief = new HashMap<>();
                    similarBelief.put("beliefId", otherId);
                    similarBelief.put("similarity", similarity);
                    similarBelief.put("belief", beliefs.get(otherId));
                    similarBeliefs.add(similarBelief);
                }
            }
        }
        
        return similarBeliefs.stream()
                .sorted((a, b) -> Double.compare((Double) b.get("similarity"), (Double) a.get("similarity")))
                .collect(Collectors.toList());
    }
    
    private double calculateSimilarity(List<BeliefRelationship> relationships1, List<BeliefRelationship> relationships2) {
        if (relationships1.isEmpty() && relationships2.isEmpty()) {
            return 0.0;
        }
        
        Set<String> types1 = relationships1.stream()
                .map(rel -> rel.getRelationshipType().getCode())
                .collect(Collectors.toSet());
        
        Set<String> types2 = relationships2.stream()
                .map(rel -> rel.getRelationshipType().getCode())
                .collect(Collectors.toSet());
        
        Set<String> intersection = new HashSet<>(types1);
        intersection.retainAll(types2);
        
        Set<String> union = new HashSet<>(types1);
        union.addAll(types2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    @Override
    public BeliefKnowledgeGraph getKnowledgeGraph(String agentId) {
        Map<String, Belief> agentBeliefs = beliefs.entrySet().stream()
                .filter(entry -> entry.getValue().getAgentId().equals(agentId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        List<BeliefRelationship> agentRelationships = relationships.values().stream()
                .filter(rel -> rel.getAgentId().equals(agentId))
                .collect(Collectors.toList());
        
        return new BeliefKnowledgeGraph(agentId, agentBeliefs.values(), agentRelationships);
    }
    
    @Override
    public BeliefKnowledgeGraph getActiveKnowledgeGraph(String agentId) {
        Map<String, Belief> agentBeliefs = beliefs.entrySet().stream()
                .filter(entry -> entry.getValue().getAgentId().equals(agentId))
                .filter(entry -> entry.getValue().isActive())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        List<BeliefRelationship> activeRelationships = relationships.values().stream()
                .filter(rel -> rel.getAgentId().equals(agentId))
                .filter(BeliefRelationship::isActive)
                .filter(BeliefRelationship::isCurrentlyEffective)
                .collect(Collectors.toList());
        
        return new BeliefKnowledgeGraph(agentId, agentBeliefs.values(), activeRelationships);
    }
    
    @Override
    public Map<String, Object> getKnowledgeGraphStatistics(String agentId) {
        // Delegate to efficient query service for better performance
        return getEfficientGraphStatistics(agentId);
    }
    
    @Override
    public List<String> validateKnowledgeGraph(String agentId) {
        // Delegate to efficient query service for better performance
        return performEfficientGraphValidation(agentId);
    }
    
    @Override
    public Map<String, Set<String>> findBeliefClusters(String agentId, double strengthThreshold) {
        // Delegate to efficient query service for better performance
        return queryService.findStronglyConnectedBeliefClusters(agentId, strengthThreshold);
    }
    
    // ========================================
    // EFFICIENT GRAPH OPERATIONS
    // ========================================
    
    @Override
    public Map<String, Object> getEfficientGraphStatistics(String agentId) {
        return queryService.getComprehensiveGraphStatistics(agentId);
    }
    
    @Override
    public List<String> performEfficientGraphValidation(String agentId) {
        return queryService.validateGraphStructure(agentId);
    }
    
    @Override
    public BeliefKnowledgeGraph createSnapshotGraph(String agentId, boolean includeInactive) {
        return queryService.createSnapshotGraph(agentId, includeInactive);
    }
    
    @Override
    public BeliefKnowledgeGraph createFilteredSnapshot(String agentId, 
                                                     Set<String> beliefIds,
                                                     Set<RelationshipType> relationshipTypes,
                                                     int maxBeliefs) {
        return queryService.createFilteredSnapshotGraph(agentId, beliefIds, relationshipTypes, maxBeliefs);
    }
    
    @Override
    public BeliefKnowledgeGraph createExportGraph(String agentId, String format) {
        return queryService.createExportGraph(agentId, format);
    }
    
    /**
     * Synchronizes a relationship with the query service.
     */
    private void synchronizeRelationshipWithQueryService(BeliefRelationship relationship) {
        if (queryService instanceof EfficientBeliefGraphQueryService) {
            EfficientBeliefGraphQueryService efficientService = (EfficientBeliefGraphQueryService) queryService;
            efficientService.addRelationship(relationship);
        }
    }
    
    /**
     * Removes a relationship from the query service.
     */
    private void removeRelationshipFromQueryService(BeliefRelationship relationship) {
        if (queryService instanceof EfficientBeliefGraphQueryService) {
            EfficientBeliefGraphQueryService efficientService = (EfficientBeliefGraphQueryService) queryService;
            efficientService.removeRelationship(relationship.getId());
        }
    }
    
    @Override
    public List<Map<String, Object>> findPotentialConflicts(String agentId) {
        List<Map<String, Object>> conflicts = new ArrayList<>();
        
        List<BeliefRelationship> contradictoryRels = findRelationshipsByType(RelationshipType.CONTRADICTS, agentId);
        
        for (BeliefRelationship rel : contradictoryRels) {
            if (rel.isCurrentlyEffective()) {
                Map<String, Object> conflict = new HashMap<>();
                conflict.put("sourceBeliefId", rel.getSourceBeliefId());
                conflict.put("targetBeliefId", rel.getTargetBeliefId());
                conflict.put("relationshipType", rel.getRelationshipType());
                conflict.put("strength", rel.getStrength());
                conflict.put("description", "Contradictory beliefs detected");
                conflicts.add(conflict);
            }
        }
        
        return conflicts;
    }
    
    @Override
    public List<BeliefRelationship> findShortestPath(String sourceBeliefId, String targetBeliefId, String agentId) {
        // Simple BFS implementation for shortest path
        Queue<String> queue = new LinkedList<>();
        Map<String, String> parent = new HashMap<>();
        Map<String, BeliefRelationship> pathRelationships = new HashMap<>();
        Set<String> visited = new HashSet<>();
        
        queue.offer(sourceBeliefId);
        visited.add(sourceBeliefId);
        
        while (!queue.isEmpty()) {
            String current = queue.poll();
            
            if (current.equals(targetBeliefId)) {
                // Reconstruct path
                List<BeliefRelationship> path = new ArrayList<>();
                String node = targetBeliefId;
                
                while (parent.containsKey(node)) {
                    path.add(0, pathRelationships.get(node));
                    node = parent.get(node);
                }
                
                return path;
            }
            
            for (BeliefRelationship rel : findOutgoingRelationships(current, agentId)) {
                if (rel.isCurrentlyEffective() && !visited.contains(rel.getTargetBeliefId())) {
                    visited.add(rel.getTargetBeliefId());
                    parent.put(rel.getTargetBeliefId(), current);
                    pathRelationships.put(rel.getTargetBeliefId(), rel);
                    queue.offer(rel.getTargetBeliefId());
                }
            }
        }
        
        return Collections.emptyList(); // No path found
    }
    
    @Override
    public List<Map<String, Object>> suggestRelationships(String agentId, int maxSuggestions) {
        // Simple suggestion algorithm based on content similarity and existing patterns
        List<Map<String, Object>> suggestions = new ArrayList<>();
        
        // This is a placeholder implementation - in a real system, this would use
        // more sophisticated algorithms, possibly involving ML models
        
        return suggestions;
    }
    
    @Override
    public List<BeliefRelationship> createRelationshipsBulk(List<BeliefRelationship> relationships, String agentId) {
        List<BeliefRelationship> created = new ArrayList<>();
        
        for (BeliefRelationship rel : relationships) {
            if (rel.getAgentId().equals(agentId)) {
                rel.setId(generateRelationshipId());
                storeRelationship(rel);
                created.add(rel);
            }
        }
        
        return created;
    }
    
    @Override
    public String exportKnowledgeGraph(String agentId, String format) {
        // Use efficient export graph creation for better performance
        BeliefKnowledgeGraph graph = createExportGraph(agentId, format);
        
        switch (format.toLowerCase()) {
            case "json":
                return exportAsJson(graph);
            case "dot":
                return exportAsDot(graph);
            default:
                throw new IllegalArgumentException("Unsupported export format: " + format);
        }
    }
    
    private String exportAsJson(BeliefKnowledgeGraph graph) {
        // Simple JSON export - in a real implementation, use a proper JSON library
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"agentId\":\"").append(graph.getAgentId()).append("\",");
        json.append("\"beliefs\":").append(graph.getBeliefs().size()).append(",");
        json.append("\"relationships\":").append(graph.getRelationships().size());
        json.append("}");
        return json.toString();
    }
    
    private String exportAsDot(BeliefKnowledgeGraph graph) {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph KnowledgeGraph {\n");
        
        // Add nodes
        for (Belief belief : graph.getBeliefs().values()) {
            dot.append("  \"").append(belief.getId()).append("\" [label=\"")
               .append(belief.getStatement().substring(0, Math.min(30, belief.getStatement().length())))
               .append("...\"];\n");
        }
        
        // Add edges
        for (BeliefRelationship rel : graph.getRelationships().values()) {
            if (rel.isActive()) {
                dot.append("  \"").append(rel.getSourceBeliefId()).append("\" -> \"")
                   .append(rel.getTargetBeliefId()).append("\" [label=\"")
                   .append(rel.getRelationshipType().getCode()).append("\"];\n");
            }
        }
        
        dot.append("}\n");
        return dot.toString();
    }
    
    @Override
    public int importRelationships(String data, String format, String agentId) {
        // Placeholder implementation
        return 0;
    }
    
    @Override
    public int cleanupKnowledgeGraph(String agentId, int olderThanDays) {
        Instant cutoff = Instant.now().minusSeconds(olderThanDays * 24 * 60 * 60L);
        
        List<String> toRemove = relationships.values().stream()
                .filter(rel -> rel.getAgentId().equals(agentId))
                .filter(rel -> !rel.isActive())
                .filter(rel -> rel.getLastUpdated().isBefore(cutoff))
                .map(BeliefRelationship::getId)
                .collect(Collectors.toList());
        
        toRemove.forEach(id -> {
            BeliefRelationship rel = relationships.get(id);
            if (rel != null) {
                removeRelationship(rel);
            }
        });
        
        return toRemove.size();
    }
    
    @Override
    public Map<String, Object> getHealthInfo() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("totalRelationships", relationships.size());
        health.put("implementation", "InMemoryBeliefRelationshipService");
        health.put("uptime", System.currentTimeMillis());
        return health;
    }
    
    // Helper methods
    
    private void validateRelationshipParameters(String sourceBeliefId, String targetBeliefId, 
                                              RelationshipType relationshipType, String agentId) {
        if (sourceBeliefId == null || sourceBeliefId.trim().isEmpty()) {
            throw new IllegalArgumentException("Source belief ID cannot be null or empty");
        }
        if (targetBeliefId == null || targetBeliefId.trim().isEmpty()) {
            throw new IllegalArgumentException("Target belief ID cannot be null or empty");
        }
        if (sourceBeliefId.equals(targetBeliefId)) {
            throw new IllegalArgumentException("Self-referential relationships are not allowed");
        }
        if (relationshipType == null) {
            throw new IllegalArgumentException("Relationship type cannot be null");
        }
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }
    }
    
    private String generateRelationshipId() {
        return "rel_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    private void storeRelationship(BeliefRelationship relationship) {
        relationships.put(relationship.getId(), relationship);
        
        // Update adjacency lists
        String sourceId = relationship.getSourceBeliefId();
        String targetId = relationship.getTargetBeliefId();
        
        outgoingRelationships.computeIfAbsent(sourceId, k -> new ArrayList<>()).add(relationship.getId());
        incomingRelationships.computeIfAbsent(targetId, k -> new ArrayList<>()).add(relationship.getId());
        
        // Update agent index
        agentRelationships.computeIfAbsent(relationship.getAgentId(), k -> new HashMap<>())
                .computeIfAbsent("relationships", k -> new ArrayList<>()).add(relationship.getId());
        
        // Sync with query service
        synchronizeRelationshipWithQueryService(relationship);
    }
    
    private void removeRelationship(BeliefRelationship relationship) {
        relationships.remove(relationship.getId());
        
        // Update adjacency lists
        String sourceId = relationship.getSourceBeliefId();
        String targetId = relationship.getTargetBeliefId();
        
        List<String> outgoing = outgoingRelationships.get(sourceId);
        if (outgoing != null) {
            outgoing.remove(relationship.getId());
            if (outgoing.isEmpty()) {
                outgoingRelationships.remove(sourceId);
            }
        }
        
        List<String> incoming = incomingRelationships.get(targetId);
        if (incoming != null) {
            incoming.remove(relationship.getId());
            if (incoming.isEmpty()) {
                incomingRelationships.remove(targetId);
            }
        }
        
        // Update agent index
        Map<String, List<String>> agentRels = agentRelationships.get(relationship.getAgentId());
        if (agentRels != null) {
            List<String> allRels = agentRels.get("all");
            if (allRels != null) {
                allRels.remove(relationship.getId());
            }
        }
    }
}