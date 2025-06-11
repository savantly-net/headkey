package ai.headkey.memory.dto;

import ai.headkey.memory.enums.RelationshipType;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Transfer Object representing a complete belief knowledge graph for an agent.
 * 
 * This class encapsulates a collection of beliefs and their relationships, providing
 * graph traversal operations, analysis capabilities, and visualization support.
 * The knowledge graph enables complex queries like finding belief chains,
 * deprecated beliefs, and semantic clusters.
 * 
 * @since 1.0
 */
public class BeliefKnowledgeGraph {
    
    /**
     * The agent this knowledge graph belongs to.
     */
    private String agentId;
    
    /**
     * All beliefs in this knowledge graph, indexed by belief ID.
     */
    private Map<String, Belief> beliefs;
    
    /**
     * All relationships in this knowledge graph, indexed by relationship ID.
     */
    private Map<String, BeliefRelationship> relationships;
    
    /**
     * Adjacency list for outgoing relationships (source belief -> list of relationships).
     */
    private Map<String, List<BeliefRelationship>> outgoingRelationships;
    
    /**
     * Adjacency list for incoming relationships (target belief -> list of relationships).
     */
    private Map<String, List<BeliefRelationship>> incomingRelationships;
    
    /**
     * Timestamp when this graph was created/last updated.
     */
    private Instant lastUpdated;
    
    /**
     * Metadata about the graph structure and statistics.
     */
    private Map<String, Object> metadata;
    
    /**
     * Default constructor.
     */
    public BeliefKnowledgeGraph() {
        this.beliefs = new HashMap<>();
        this.relationships = new HashMap<>();
        this.outgoingRelationships = new HashMap<>();
        this.incomingRelationships = new HashMap<>();
        this.metadata = new HashMap<>();
        this.lastUpdated = Instant.now();
    }
    
    /**
     * Constructor with agent ID.
     * 
     * @param agentId The agent this graph belongs to
     */
    public BeliefKnowledgeGraph(String agentId) {
        this();
        this.agentId = agentId;
    }
    
    /**
     * Constructor with beliefs and relationships.
     * 
     * @param agentId The agent ID
     * @param beliefs Collection of beliefs
     * @param relationships Collection of relationships
     */
    public BeliefKnowledgeGraph(String agentId, Collection<Belief> beliefs, 
                               Collection<BeliefRelationship> relationships) {
        this(agentId);
        addBeliefs(beliefs);
        addRelationships(relationships);
    }
    
    /**
     * Adds a belief to the knowledge graph.
     * 
     * @param belief The belief to add
     */
    public void addBelief(Belief belief) {
        if (belief != null && belief.getId() != null) {
            beliefs.put(belief.getId(), belief);
            updateLastModified();
        }
    }
    
    /**
     * Adds multiple beliefs to the knowledge graph.
     * 
     * @param beliefs Collection of beliefs to add
     */
    public void addBeliefs(Collection<Belief> beliefs) {
        if (beliefs != null) {
            beliefs.forEach(this::addBelief);
        }
    }
    
    /**
     * Adds a relationship to the knowledge graph.
     * 
     * @param relationship The relationship to add
     */
    public void addRelationship(BeliefRelationship relationship) {
        if (relationship != null && relationship.getId() != null) {
            relationships.put(relationship.getId(), relationship);
            
            // Update adjacency lists
            String sourceId = relationship.getSourceBeliefId();
            String targetId = relationship.getTargetBeliefId();
            
            outgoingRelationships.computeIfAbsent(sourceId, k -> new ArrayList<>()).add(relationship);
            incomingRelationships.computeIfAbsent(targetId, k -> new ArrayList<>()).add(relationship);
            
            updateLastModified();
        }
    }
    
    /**
     * Adds multiple relationships to the knowledge graph.
     * 
     * @param relationships Collection of relationships to add
     */
    public void addRelationships(Collection<BeliefRelationship> relationships) {
        if (relationships != null) {
            relationships.forEach(this::addRelationship);
        }
    }
    
    /**
     * Gets a belief by ID.
     * 
     * @param beliefId The belief ID
     * @return The belief, or null if not found
     */
    public Belief getBelief(String beliefId) {
        return beliefs.get(beliefId);
    }
    
    /**
     * Gets a relationship by ID.
     * 
     * @param relationshipId The relationship ID
     * @return The relationship, or null if not found
     */
    public BeliefRelationship getRelationship(String relationshipId) {
        return relationships.get(relationshipId);
    }
    
    /**
     * Gets all outgoing relationships from a belief.
     * 
     * @param beliefId The source belief ID
     * @return List of outgoing relationships
     */
    public List<BeliefRelationship> getOutgoingRelationships(String beliefId) {
        return outgoingRelationships.getOrDefault(beliefId, new ArrayList<>());
    }
    
    /**
     * Gets all incoming relationships to a belief.
     * 
     * @param beliefId The target belief ID
     * @return List of incoming relationships
     */
    public List<BeliefRelationship> getIncomingRelationships(String beliefId) {
        return incomingRelationships.getOrDefault(beliefId, new ArrayList<>());
    }
    
    /**
     * Gets all relationships connected to a belief (both incoming and outgoing).
     * 
     * @param beliefId The belief ID
     * @return List of all connected relationships
     */
    public List<BeliefRelationship> getAllRelationships(String beliefId) {
        List<BeliefRelationship> allRels = new ArrayList<>();
        allRels.addAll(getOutgoingRelationships(beliefId));
        allRels.addAll(getIncomingRelationships(beliefId));
        return allRels;
    }
    
    /**
     * Gets relationships of a specific type.
     * 
     * @param relationshipType The relationship type to filter by
     * @return List of relationships of the specified type
     */
    public List<BeliefRelationship> getRelationshipsByType(RelationshipType relationshipType) {
        return relationships.values().stream()
                .filter(rel -> rel.getRelationshipType() == relationshipType)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets relationships of a specific type from a belief.
     * 
     * @param beliefId The source belief ID
     * @param relationshipType The relationship type
     * @return List of outgoing relationships of the specified type
     */
    public List<BeliefRelationship> getOutgoingRelationshipsByType(String beliefId, RelationshipType relationshipType) {
        return getOutgoingRelationships(beliefId).stream()
                .filter(rel -> rel.getRelationshipType() == relationshipType)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets relationships of a specific type to a belief.
     * 
     * @param beliefId The target belief ID
     * @param relationshipType The relationship type
     * @return List of incoming relationships of the specified type
     */
    public List<BeliefRelationship> getIncomingRelationshipsByType(String beliefId, RelationshipType relationshipType) {
        return getIncomingRelationships(beliefId).stream()
                .filter(rel -> rel.getRelationshipType() == relationshipType)
                .collect(Collectors.toList());
    }
    
    /**
     * Finds beliefs that have been deprecated by other beliefs in the loaded graph data.
     * 
     * @deprecated Use BeliefGraphQueryService.findDeprecatedBeliefIds() for efficient database-level queries.
     * This method only searches within the currently loaded graph data and may miss external relationships.
     * 
     * @return Set of deprecated belief IDs from loaded data
     */
    @Deprecated
    public Set<String> findDeprecatedBeliefs() {
        Set<String> deprecated = new HashSet<>();
        
        for (BeliefRelationship rel : relationships.values()) {
            if (rel.isDeprecating() && rel.isCurrentlyEffective()) {
                deprecated.add(rel.getTargetBeliefId());
            }
        }
        
        return deprecated;
    }
    
    /**
     * Finds beliefs that supersede a given belief in the loaded graph data.
     * 
     * @deprecated Use BeliefGraphQueryService.findSupersedingBeliefIds() for efficient database-level queries.
     * This method only searches within the currently loaded graph data and may miss external relationships.
     * 
     * @param beliefId The deprecated belief ID
     * @return List of beliefs that supersede the given belief from loaded data
     */
    @Deprecated
    public List<Belief> findSupersedingBeliefs(String beliefId) {
        return getIncomingRelationshipsByType(beliefId, RelationshipType.SUPERSEDES).stream()
                .filter(BeliefRelationship::isCurrentlyEffective)
                .map(rel -> getBelief(rel.getSourceBeliefId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * Finds the complete deprecation chain for a belief in the loaded graph data.
     * 
     * @deprecated Use BeliefGraphQueryService.findDeprecationChain() for efficient database-level traversal.
     * This method only searches within the currently loaded graph data and may miss external relationships.
     * 
     * @param beliefId The belief ID
     * @return List of beliefs in the deprecation chain from loaded data
     */
    @Deprecated
    public List<Belief> findDeprecationChain(String beliefId) {
        List<Belief> chain = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        
        findDeprecationChainRecursive(beliefId, chain, visited);
        return chain;
    }
    
    private void findDeprecationChainRecursive(String beliefId, List<Belief> chain, Set<String> visited) {
        if (visited.contains(beliefId)) {
            return; // Avoid cycles
        }
        
        visited.add(beliefId);
        Belief belief = getBelief(beliefId);
        if (belief != null) {
            chain.add(belief);
        }
        
        // Find beliefs that supersede this one
        for (BeliefRelationship rel : getIncomingRelationships(beliefId)) {
            if (rel.isDeprecating() && rel.isCurrentlyEffective()) {
                findDeprecationChainRecursive(rel.getSourceBeliefId(), chain, visited);
            }
        }
    }
    
    /**
     * Finds beliefs related to a given belief in the loaded graph data.
     * 
     * @deprecated Use BeliefGraphQueryService.findRelatedBeliefIds() for efficient database-level traversal.
     * This method only searches within the currently loaded graph data and may miss external relationships.
     * 
     * @param beliefId The belief ID
     * @param maxDepth Maximum traversal depth
     * @return Set of related belief IDs from loaded data
     */
    @Deprecated
    public Set<String> findRelatedBeliefs(String beliefId, int maxDepth) {
        Set<String> related = new HashSet<>();
        Set<String> visited = new HashSet<>();
        
        findRelatedBeliefsRecursive(beliefId, related, visited, 0, maxDepth);
        related.remove(beliefId); // Remove the starting belief
        
        return related;
    }
    
    private void findRelatedBeliefsRecursive(String beliefId, Set<String> related, 
                                           Set<String> visited, int currentDepth, int maxDepth) {
        if (currentDepth >= maxDepth || visited.contains(beliefId)) {
            return;
        }
        
        visited.add(beliefId);
        related.add(beliefId);
        
        // Traverse outgoing relationships
        for (BeliefRelationship rel : getOutgoingRelationships(beliefId)) {
            if (rel.isCurrentlyEffective()) {
                findRelatedBeliefsRecursive(rel.getTargetBeliefId(), related, visited, currentDepth + 1, maxDepth);
            }
        }
        
        // Traverse incoming relationships
        for (BeliefRelationship rel : getIncomingRelationships(beliefId)) {
            if (rel.isCurrentlyEffective()) {
                findRelatedBeliefsRecursive(rel.getSourceBeliefId(), related, visited, currentDepth + 1, maxDepth);
            }
        }
    }
    
    /**
     * Finds strongly connected beliefs in the loaded graph data based on relationship strength.
     * 
     * @deprecated Use BeliefGraphQueryService.findStronglyConnectedBeliefClusters() for efficient database-level clustering.
     * This method only analyzes the currently loaded graph data and may miss external connections.
     * 
     * @param strengthThreshold Minimum relationship strength threshold
     * @return Map of cluster ID to belief IDs in loaded data
     */
    @Deprecated
    public Map<String, Set<String>> findStronglyConnectedBeliefs(double strengthThreshold) {
        Map<String, Set<String>> clusters = new HashMap<>();
        Set<String> visited = new HashSet<>();
        int clusterId = 0;
        
        for (String beliefId : beliefs.keySet()) {
            if (!visited.contains(beliefId)) {
                Set<String> cluster = new HashSet<>();
                findStronglyConnectedRecursive(beliefId, cluster, visited, strengthThreshold);
                
                if (cluster.size() > 1) { // Only include clusters with multiple beliefs
                    clusters.put("cluster-" + clusterId++, cluster);
                }
            }
        }
        
        return clusters;
    }
    
    private void findStronglyConnectedRecursive(String beliefId, Set<String> cluster, 
                                              Set<String> processed, double strengthThreshold) {
        if (processed.contains(beliefId)) {
            return;
        }
        
        processed.add(beliefId);
        cluster.add(beliefId);
        
        // Find strongly connected neighbors
        for (BeliefRelationship rel : getOutgoingRelationships(beliefId)) {
            if (rel.isCurrentlyEffective() && rel.getStrength() >= strengthThreshold) {
                findStronglyConnectedRecursive(rel.getTargetBeliefId(), cluster, processed, strengthThreshold);
            }
        }
        
        for (BeliefRelationship rel : getIncomingRelationships(beliefId)) {
            if (rel.isCurrentlyEffective() && rel.getStrength() >= strengthThreshold) {
                findStronglyConnectedRecursive(rel.getSourceBeliefId(), cluster, processed, strengthThreshold);
            }
        }
    }
    
    /**
     * Gets basic statistics about the knowledge graph.
     * 
     * @deprecated Use BeliefGraphQueryService.getComprehensiveGraphStatistics() for efficient statistics computation.
     * This method only provides basic counts from the loaded graph data.
     * 
     * @return Map containing basic graph metrics
     */
    @Deprecated
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalBeliefs", beliefs.size());
        stats.put("totalRelationships", relationships.size());
        stats.put("activeBeliefs", beliefs.values().stream().mapToInt(b -> b.isActive() ? 1 : 0).sum());
        stats.put("activeRelationships", relationships.values().stream().mapToInt(r -> r.isActive() ? 1 : 0).sum());
        
        // Basic relationship type distribution
        Map<RelationshipType, Long> typeDistribution = relationships.values().stream()
                .collect(Collectors.groupingBy(BeliefRelationship::getRelationshipType, Collectors.counting()));
        stats.put("relationshipTypeDistribution", typeDistribution);
        
        // Average relationship strength
        double avgStrength = relationships.values().stream()
                .mapToDouble(BeliefRelationship::getStrength)
                .average().orElse(0.0);
        stats.put("averageRelationshipStrength", avgStrength);
        
        return stats;
    }
    
    /**
     * Validates the loaded graph structure for basic consistency.
     * 
     * @deprecated Use BeliefGraphQueryService.validateGraphStructure() for comprehensive database-level validation.
     * This method only validates the currently loaded graph data.
     * 
     * @return List of validation issues found in loaded data
     */
    @Deprecated
    public List<String> validate() {
        List<String> issues = new ArrayList<>();
        
        // Check for orphaned relationships in loaded data
        for (BeliefRelationship rel : relationships.values()) {
            if (!beliefs.containsKey(rel.getSourceBeliefId())) {
                issues.add("Relationship " + rel.getId() + " references non-existent source belief: " + rel.getSourceBeliefId());
            }
            if (!beliefs.containsKey(rel.getTargetBeliefId())) {
                issues.add("Relationship " + rel.getId() + " references non-existent target belief: " + rel.getTargetBeliefId());
            }
        }
        
        // Check for self-referential relationships in loaded data
        for (BeliefRelationship rel : relationships.values()) {
            if (Objects.equals(rel.getSourceBeliefId(), rel.getTargetBeliefId())) {
                issues.add("Self-referential relationship detected: " + rel.getId());
            }
        }
        
        // Check for temporal consistency in loaded data
        for (BeliefRelationship rel : relationships.values()) {
            if (rel.getEffectiveFrom() != null && rel.getEffectiveUntil() != null) {
                if (rel.getEffectiveFrom().isAfter(rel.getEffectiveUntil())) {
                    issues.add("Invalid temporal period in relationship " + rel.getId() + ": effectiveFrom is after effectiveUntil");
                }
            }
        }
        
        return issues;
    }
    
    /**
     * Updates the last modified timestamp.
     */
    private void updateLastModified() {
        this.lastUpdated = Instant.now();
    }
    
    // Getters and Setters
    
    public String getAgentId() {
        return agentId;
    }
    
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }
    
    public Map<String, Belief> getBeliefs() {
        return new HashMap<>(beliefs);
    }
    
    public void setBeliefs(Map<String, Belief> beliefs) {
        this.beliefs = new HashMap<>(beliefs != null ? beliefs : new HashMap<>());
        // Rebuild adjacency lists when beliefs are set
        rebuildAdjacencyLists();
    }
    
    public Map<String, BeliefRelationship> getRelationships() {
        return new HashMap<>(relationships);
    }
    
    public void setRelationships(Map<String, BeliefRelationship> relationships) {
        this.relationships = new HashMap<>(relationships != null ? relationships : new HashMap<>());
        rebuildAdjacencyLists();
    }
    
    public Instant getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = new HashMap<>(metadata != null ? metadata : new HashMap<>());
    }
    
    /**
     * Rebuilds the adjacency lists from the current relationships.
     */
    private void rebuildAdjacencyLists() {
        outgoingRelationships.clear();
        incomingRelationships.clear();
        
        for (BeliefRelationship rel : relationships.values()) {
            String sourceId = rel.getSourceBeliefId();
            String targetId = rel.getTargetBeliefId();
            
            outgoingRelationships.computeIfAbsent(sourceId, k -> new ArrayList<>()).add(rel);
            incomingRelationships.computeIfAbsent(targetId, k -> new ArrayList<>()).add(rel);
        }
    }
    
    @Override
    public String toString() {
        return "BeliefKnowledgeGraph{" +
                "agentId='" + agentId + '\'' +
                ", beliefs=" + beliefs.size() +
                ", relationships=" + relationships.size() +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}