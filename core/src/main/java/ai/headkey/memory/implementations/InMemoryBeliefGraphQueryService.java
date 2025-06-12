package ai.headkey.memory.implementations;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefRelationship;
import ai.headkey.memory.enums.RelationshipType;
import ai.headkey.memory.interfaces.BeliefGraphQueryService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.stream.Collectors;

/**
 * Efficient implementation of BeliefGraphQueryService that avoids loading complete graphs.
 * 
 * This implementation is designed for performance and memory efficiency:
 * - Uses lazy evaluation and streaming where possible
 * - Implements targeted queries that fetch only required data
 * - Maintains efficient indexes for common operations
 * - Avoids creating full BeliefKnowledgeGraph objects
 * - Provides batch operations for multiple queries
 * 
 * Key optimizations:
 * - Adjacency list caching for O(1) relationship lookups
 * - Streaming operations for large datasets
 * - Efficient graph traversal algorithms
 * - Minimal object creation during queries
 * 
 * @since 1.0
 */
public class InMemoryBeliefGraphQueryService implements BeliefGraphQueryService {
    
    // Core data storage
    private final Map<String, Belief> beliefs = new ConcurrentHashMap<>();
    private final Map<String, BeliefRelationship> relationships = new ConcurrentHashMap<>();
    
    // Efficient indexes for fast lookups
    private final Map<String, Set<String>> outgoingRelationshipIndex = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> incomingRelationshipIndex = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> agentBeliefIndex = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> agentRelationshipIndex = new ConcurrentHashMap<>();
    private final Map<RelationshipType, Set<String>> relationshipTypeIndex = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> deprecatedBeliefIndex = new ConcurrentHashMap<>();
    
    // Cache for frequently accessed statistics
    private final Map<String, Map<String, Long>> statisticsCache = new ConcurrentHashMap<>();
    private volatile long lastCacheUpdate = 0;
    private static final long CACHE_TTL_MS = 60000; // 1 minute cache TTL
    
    public InMemoryBeliefGraphQueryService() {
        // Initialize with empty state
    }
    
    /**
     * Adds a belief to the service and updates indexes.
     */
    public void addBelief(Belief belief) {
        if (belief == null || belief.getId() == null) return;
        
        beliefs.put(belief.getId(), belief);
        agentBeliefIndex.computeIfAbsent(belief.getAgentId(), k -> ConcurrentHashMap.newKeySet())
                       .add(belief.getId());
        invalidateStatisticsCache(belief.getAgentId());
    }
    
    /**
     * Adds a relationship to the service and updates indexes.
     */
    public void addRelationship(BeliefRelationship relationship) {
        if (relationship == null || relationship.getId() == null) return;
        
        relationships.put(relationship.getId(), relationship);
        
        // Update adjacency indexes
        outgoingRelationshipIndex.computeIfAbsent(relationship.getSourceBeliefId(), k -> ConcurrentHashMap.newKeySet())
                                 .add(relationship.getId());
        incomingRelationshipIndex.computeIfAbsent(relationship.getTargetBeliefId(), k -> ConcurrentHashMap.newKeySet())
                                 .add(relationship.getId());
        
        // Update agent index
        agentRelationshipIndex.computeIfAbsent(relationship.getAgentId(), k -> ConcurrentHashMap.newKeySet())
                              .add(relationship.getId());
        
        // Update type index
        relationshipTypeIndex.computeIfAbsent(relationship.getRelationshipType(), k -> ConcurrentHashMap.newKeySet())
                             .add(relationship.getId());
        
        // Update deprecation index if it's a deprecating relationship
        if (relationship.isDeprecating() && relationship.isCurrentlyEffective()) {
            deprecatedBeliefIndex.computeIfAbsent(relationship.getAgentId(), k -> ConcurrentHashMap.newKeySet())
                                 .add(relationship.getTargetBeliefId());
        }
        
        invalidateStatisticsCache(relationship.getAgentId());
    }
    
    /**
     * Removes a belief and updates indexes.
     */
    public void removeBelief(String beliefId) {
        Belief belief = beliefs.remove(beliefId);
        if (belief != null) {
            agentBeliefIndex.getOrDefault(belief.getAgentId(), Collections.emptySet()).remove(beliefId);
            invalidateStatisticsCache(belief.getAgentId());
        }
    }
    
    /**
     * Removes a relationship and updates indexes.
     */
    public void removeRelationship(String relationshipId) {
        BeliefRelationship relationship = relationships.remove(relationshipId);
        if (relationship != null) {
            outgoingRelationshipIndex.getOrDefault(relationship.getSourceBeliefId(), Collections.emptySet())
                                     .remove(relationshipId);
            incomingRelationshipIndex.getOrDefault(relationship.getTargetBeliefId(), Collections.emptySet())
                                     .remove(relationshipId);
            agentRelationshipIndex.getOrDefault(relationship.getAgentId(), Collections.emptySet())
                                  .remove(relationshipId);
            relationshipTypeIndex.getOrDefault(relationship.getRelationshipType(), Collections.emptySet())
                                 .remove(relationshipId);
            
            if (relationship.isDeprecating()) {
                deprecatedBeliefIndex.getOrDefault(relationship.getAgentId(), Collections.emptySet())
                                     .remove(relationship.getTargetBeliefId());
            }
            
            invalidateStatisticsCache(relationship.getAgentId());
        }
    }
    
    // ========================================
    // EFFICIENT GRAPH STATISTICS
    // ========================================
    
    @Override
    public Map<String, Long> getGraphStatistics(String agentId) {
        Map<String, Long> cached = getCachedStatistics(agentId);
        if (cached != null) {
            return cached;
        }
        
        Map<String, Long> stats = new HashMap<>();
        
        // Count beliefs
        long totalBeliefs = agentBeliefIndex.getOrDefault(agentId, Collections.emptySet()).size();
        long activeBeliefs = streamBeliefs(agentId, false, 0).count();
        
        // Count relationships
        long totalRelationships = agentRelationshipIndex.getOrDefault(agentId, Collections.emptySet()).size();
        long activeRelationships = streamRelationships(agentId, false, 0).count();
        
        // Count deprecated beliefs
        long deprecatedBeliefs = getDeprecatedBeliefsCount(agentId);
        
        stats.put("totalBeliefs", totalBeliefs);
        stats.put("activeBeliefs", activeBeliefs);
        stats.put("inactiveBeliefs", totalBeliefs - activeBeliefs);
        stats.put("totalRelationships", totalRelationships);
        stats.put("activeRelationships", activeRelationships);
        stats.put("inactiveRelationships", totalRelationships - activeRelationships);
        stats.put("deprecatedBeliefs", deprecatedBeliefs);
        
        cacheStatistics(agentId, stats);
        return stats;
    }
    
    @Override
    public Map<RelationshipType, Long> getRelationshipTypeDistribution(String agentId) {
        return agentRelationshipIndex.getOrDefault(agentId, Collections.emptySet()).stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(BeliefRelationship::getRelationshipType, Collectors.counting()));
    }
    
    @Override
    public long getBeliefsCount(String agentId, boolean includeInactive) {
        if (includeInactive) {
            return agentBeliefIndex.getOrDefault(agentId, Collections.emptySet()).size();
        } else {
            return streamBeliefs(agentId, false, 0).count();
        }
    }
    
    @Override
    public long getRelationshipsCount(String agentId, boolean includeInactive) {
        if (includeInactive) {
            return agentRelationshipIndex.getOrDefault(agentId, Collections.emptySet()).size();
        } else {
            return streamRelationships(agentId, false, 0).count();
        }
    }
    
    @Override
    public long getDeprecatedBeliefsCount(String agentId) {
        return deprecatedBeliefIndex.getOrDefault(agentId, Collections.emptySet()).size();
    }
    
    // ========================================
    // TARGETED BELIEF QUERIES
    // ========================================
    
    @Override
    public Stream<Belief> streamBeliefs(String agentId, boolean includeInactive, int pageSize) {
        Stream<Belief> stream = agentBeliefIndex.getOrDefault(agentId, Collections.emptySet()).stream()
                .map(beliefs::get)
                .filter(Objects::nonNull)
                .filter(belief -> includeInactive || belief.isActive());
        
        return pageSize > 0 ? stream.limit(pageSize) : stream;
    }
    
    @Override
    public List<Belief> getBeliefsByCategory(String agentId, String category, int limit) {
        Stream<Belief> stream = streamBeliefs(agentId, false, 0)
                .filter(belief -> Objects.equals(belief.getCategory(), category));
        
        return (limit > 0 ? stream.limit(limit) : stream).collect(Collectors.toList());
    }
    
    @Override
    public List<Belief> getHighConfidenceBeliefs(String agentId, double confidenceThreshold, int limit) {
        Stream<Belief> stream = streamBeliefs(agentId, false, 0)
                .filter(belief -> belief.getConfidence() >= confidenceThreshold)
                .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));
        
        return (limit > 0 ? stream.limit(limit) : stream).collect(Collectors.toList());
    }
    
    @Override
    public List<Belief> getRecentBeliefs(String agentId, int limit) {
        return streamBeliefs(agentId, false, 0)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean beliefExists(String beliefId, String agentId) {
        Belief belief = beliefs.get(beliefId);
        return belief != null && belief.getAgentId().equals(agentId);
    }
    
    // ========================================
    // EFFICIENT RELATIONSHIP QUERIES
    // ========================================
    
    @Override
    public Stream<BeliefRelationship> streamRelationships(String agentId, boolean includeInactive, int pageSize) {
        Stream<BeliefRelationship> stream = agentRelationshipIndex.getOrDefault(agentId, Collections.emptySet()).stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .filter(rel -> includeInactive || (rel.isActive() && rel.isCurrentlyEffective()));
        
        return pageSize > 0 ? stream.limit(pageSize) : stream;
    }
    
    @Override
    public long getRelationshipCount(String beliefId, String agentId, String direction) {
        switch (direction.toLowerCase()) {
            case "outgoing":
                return getRelationshipIds(beliefId, agentId, "outgoing", 0).size();
            case "incoming":
                return getRelationshipIds(beliefId, agentId, "incoming", 0).size();
            case "both":
                return getRelationshipIds(beliefId, agentId, "outgoing", 0).size() + 
                       getRelationshipIds(beliefId, agentId, "incoming", 0).size();
            default:
                throw new IllegalArgumentException("Invalid direction: " + direction);
        }
    }
    
    @Override
    public List<String> getRelationshipIds(String beliefId, String agentId, String direction, int limit) {
        Set<String> relationshipIds = new HashSet<>();
        
        if ("outgoing".equals(direction) || "both".equals(direction)) {
            relationshipIds.addAll(outgoingRelationshipIndex.getOrDefault(beliefId, Collections.emptySet()));
        }
        
        if ("incoming".equals(direction) || "both".equals(direction)) {
            relationshipIds.addAll(incomingRelationshipIndex.getOrDefault(beliefId, Collections.emptySet()));
        }
        
        Stream<String> stream = relationshipIds.stream()
                .filter(id -> {
                    BeliefRelationship rel = relationships.get(id);
                    return rel != null && rel.getAgentId().equals(agentId) && 
                           rel.isActive() && rel.isCurrentlyEffective();
                });
        
        return (limit > 0 ? stream.limit(limit) : stream).collect(Collectors.toList());
    }
    
    @Override
    public List<String> getConnectedBeliefIds(String beliefId, String agentId, String direction, 
                                            Set<RelationshipType> relationshipTypes, int limit) {
        List<String> relationshipIds = getRelationshipIds(beliefId, agentId, direction, 0);
        
        Stream<String> stream = relationshipIds.stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .filter(rel -> relationshipTypes == null || relationshipTypes.contains(rel.getRelationshipType()))
                .map(rel -> {
                    if ("outgoing".equals(direction)) {
                        return rel.getTargetBeliefId();
                    } else if ("incoming".equals(direction)) {
                        return rel.getSourceBeliefId();
                    } else {
                        return rel.getSourceBeliefId().equals(beliefId) ? 
                               rel.getTargetBeliefId() : rel.getSourceBeliefId();
                    }
                })
                .distinct();
        
        return (limit > 0 ? stream.limit(limit) : stream).collect(Collectors.toList());
    }
    
    @Override
    public boolean areBeliefsDirectlyConnected(String sourceBeliefId, String targetBeliefId, 
                                             String agentId, Set<RelationshipType> relationshipTypes) {
        return outgoingRelationshipIndex.getOrDefault(sourceBeliefId, Collections.emptySet()).stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .anyMatch(rel -> rel.getTargetBeliefId().equals(targetBeliefId) &&
                                rel.getAgentId().equals(agentId) &&
                                rel.isActive() && rel.isCurrentlyEffective() &&
                                (relationshipTypes == null || relationshipTypes.contains(rel.getRelationshipType())));
    }
    
    // ========================================
    // EFFICIENT DEPRECATION QUERIES
    // ========================================
    
    @Override
    public List<String> getDeprecatedBeliefIds(String agentId, int limit) {
        Stream<String> stream = deprecatedBeliefIndex.getOrDefault(agentId, Collections.emptySet()).stream();
        return (limit > 0 ? stream.limit(limit) : stream).collect(Collectors.toList());
    }
    
    @Override
    public List<String> getSupersedingBeliefIds(String beliefId, String agentId) {
        return getConnectedBeliefIds(beliefId, agentId, "incoming", 
                Set.of(RelationshipType.SUPERSEDES, RelationshipType.UPDATES, 
                       RelationshipType.DEPRECATES, RelationshipType.REPLACES), 0);
    }
    
    @Override
    public boolean isBeliefDeprecated(String beliefId, String agentId) {
        return deprecatedBeliefIndex.getOrDefault(agentId, Collections.emptySet()).contains(beliefId);
    }
    
    @Override
    public List<String> getDeprecationChainIds(String beliefId, String agentId, int maxDepth) {
        List<String> chain = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        findDeprecationChainRecursive(beliefId, agentId, chain, visited, 0, maxDepth);
        return chain;
    }
    
    private void findDeprecationChainRecursive(String beliefId, String agentId, List<String> chain, 
                                             Set<String> visited, int currentDepth, int maxDepth) {
        if (currentDepth >= maxDepth || visited.contains(beliefId)) {
            return;
        }
        
        visited.add(beliefId);
        chain.add(beliefId);
        
        List<String> superseding = getSupersedingBeliefIds(beliefId, agentId);
        for (String supersedingId : superseding) {
            findDeprecationChainRecursive(supersedingId, agentId, chain, visited, currentDepth + 1, maxDepth);
        }
    }
    
    // ========================================
    // EFFICIENT GRAPH TRAVERSAL
    // ========================================
    
    @Override
    public Set<String> getReachableBeliefIds(String startBeliefId, String agentId, int maxDepth, 
                                           Set<RelationshipType> relationshipTypes) {
        Set<String> reachable = new HashSet<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        Map<String, Integer> depths = new HashMap<>();
        
        queue.offer(startBeliefId);
        depths.put(startBeliefId, 0);
        
        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            int currentDepth = depths.get(currentId);
            
            if (currentDepth >= maxDepth || visited.contains(currentId)) {
                continue;
            }
            
            visited.add(currentId);
            reachable.add(currentId);
            
            // Get connected beliefs
            List<String> connected = getConnectedBeliefIds(currentId, agentId, "both", relationshipTypes, 0);
            for (String connectedId : connected) {
                if (!visited.contains(connectedId) && currentDepth + 1 <= maxDepth) {
                    queue.offer(connectedId);
                    depths.put(connectedId, currentDepth + 1);
                }
            }
        }
        
        reachable.remove(startBeliefId); // Remove starting belief
        return reachable;
    }
    
    @Override
    public List<String> getShortestPathIds(String sourceBeliefId, String targetBeliefId, 
                                         String agentId, int maxDepth) {
        Queue<String> queue = new LinkedList<>();
        Map<String, String> parent = new HashMap<>();
        Map<String, String> parentRelationship = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Map<String, Integer> depths = new HashMap<>();
        
        queue.offer(sourceBeliefId);
        visited.add(sourceBeliefId);
        depths.put(sourceBeliefId, 0);
        
        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentDepth = depths.get(current);
            
            if (current.equals(targetBeliefId)) {
                // Reconstruct path
                List<String> path = new ArrayList<>();
                String node = targetBeliefId;
                
                while (parentRelationship.containsKey(node)) {
                    path.add(0, parentRelationship.get(node));
                    node = parent.get(node);
                }
                
                return path;
            }
            
            if (currentDepth >= maxDepth) {
                continue;
            }
            
            List<String> outgoingRels = getRelationshipIds(current, agentId, "outgoing", 0);
            for (String relId : outgoingRels) {
                BeliefRelationship rel = relationships.get(relId);
                if (rel != null && !visited.contains(rel.getTargetBeliefId())) {
                    visited.add(rel.getTargetBeliefId());
                    parent.put(rel.getTargetBeliefId(), current);
                    parentRelationship.put(rel.getTargetBeliefId(), relId);
                    depths.put(rel.getTargetBeliefId(), currentDepth + 1);
                    queue.offer(rel.getTargetBeliefId());
                }
            }
        }
        
        return Collections.emptyList(); // No path found
    }
    
    @Override
    public Map<String, Set<String>> getBeliefClusterIds(String agentId, double strengthThreshold, int minClusterSize) {
        Map<String, Set<String>> clusters = new HashMap<>();
        Set<String> processed = new HashSet<>();
        int clusterCount = 0;
        
        for (String beliefId : agentBeliefIndex.getOrDefault(agentId, Collections.emptySet())) {
            if (!processed.contains(beliefId)) {
                Set<String> cluster = new HashSet<>();
                findStronglyConnectedRecursive(beliefId, agentId, cluster, processed, strengthThreshold);
                
                if (cluster.size() >= minClusterSize) {
                    clusters.put("cluster_" + clusterCount++, cluster);
                }
            }
        }
        
        return clusters;
    }
    

    
    // ========================================
    // CONFLICT AND VALIDATION QUERIES
    // ========================================
    
    @Override
    public List<String> getConflictingRelationshipIds(String agentId, int limit) {
        Stream<String> stream = agentRelationshipIndex.getOrDefault(agentId, Collections.emptySet()).stream()
                .filter(id -> {
                    BeliefRelationship rel = relationships.get(id);
                    return rel != null && 
                           (rel.getRelationshipType() == RelationshipType.CONTRADICTS ||
                            rel.getRelationshipType() == RelationshipType.CONFLICTS_WITH) &&
                           rel.isActive() && rel.isCurrentlyEffective();
                });
        
        return (limit > 0 ? stream.limit(limit) : stream).collect(Collectors.toList());
    }
    
    @Override
    public List<Map<String, String>> getContradictoryBeliefPairs(String agentId, int limit) {
        List<String> conflictingRels = getConflictingRelationshipIds(agentId, limit);
        
        return conflictingRels.stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .map(rel -> {
                    Map<String, String> pair = new HashMap<>();
                    pair.put("sourceBeliefId", rel.getSourceBeliefId());
                    pair.put("targetBeliefId", rel.getTargetBeliefId());
                    return pair;
                })
                .collect(Collectors.toList());
    }
    

    
    // ========================================
    // BATCH AND UTILITY OPERATIONS
    // ========================================
    
    @Override
    public Map<String, Belief> getBeliefsById(Set<String> beliefIds, String agentId) {
        return beliefIds.stream()
                .map(beliefs::get)
                .filter(Objects::nonNull)
                .filter(belief -> belief.getAgentId().equals(agentId))
                .collect(Collectors.toMap(Belief::getId, belief -> belief));
    }
    
    @Override
    public Map<String, BeliefRelationship> getRelationshipsById(Set<String> relationshipIds, String agentId) {
        return relationshipIds.stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .filter(rel -> rel.getAgentId().equals(agentId))
                .collect(Collectors.toMap(BeliefRelationship::getId, rel -> rel));
    }
    
    @Override
    public Map<String, Boolean> checkBeliefsExist(Set<String> beliefIds, String agentId) {
        return beliefIds.stream()
                .collect(Collectors.toMap(
                    id -> id,
                    id -> beliefExists(id, agentId)
                ));
    }
    
    @Override
    public Map<String, Long> getBeliefDegrees(Set<String> beliefIds, String agentId, String direction) {
        return beliefIds.stream()
                .collect(Collectors.toMap(
                    id -> id,
                    id -> getRelationshipCount(id, agentId, direction)
                ));
    }
    
    // ========================================
    // EFFICIENT SEARCH AND FILTERING
    // ========================================
    
    @Override
    public List<Belief> searchBeliefsByContent(String agentId, String searchText, int limit) {
        String lowerSearchText = searchText.toLowerCase();
        
        Stream<Belief> stream = streamBeliefs(agentId, false, 0)
                .filter(belief -> belief.getStatement().toLowerCase().contains(lowerSearchText));
        
        return (limit > 0 ? stream.limit(limit) : stream).collect(Collectors.toList());
    }
    
    @Override
    public List<Map<String, Object>> getSimilarBeliefIds(String beliefId, String agentId, 
                                                        double similarityThreshold, int limit) {
        List<String> targetRelationshipIds = getRelationshipIds(beliefId, agentId, "both", 0);
        Set<RelationshipType> targetTypes = targetRelationshipIds.stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .map(BeliefRelationship::getRelationshipType)
                .collect(Collectors.toSet());
        
        List<Map<String, Object>> similarBeliefs = new ArrayList<>();
        
        for (String otherId : agentBeliefIndex.getOrDefault(agentId, Collections.emptySet())) {
            if (!otherId.equals(beliefId)) {
                List<String> otherRelationshipIds = getRelationshipIds(otherId, agentId, "both", 0);
                Set<RelationshipType> otherTypes = otherRelationshipIds.stream()
                        .map(relationships::get)
                        .filter(Objects::nonNull)
                        .map(BeliefRelationship::getRelationshipType)
                        .collect(Collectors.toSet());
                
                double similarity = calculateJaccardSimilarity(targetTypes, otherTypes);
                
                if (similarity >= similarityThreshold) {
                    Map<String, Object> similar = new HashMap<>();
                    similar.put("beliefId", otherId);
                    similar.put("similarity", similarity);
                    similarBeliefs.add(similar);
                }
            }
        }
        
        return similarBeliefs.stream()
                .sorted((a, b) -> Double.compare((Double) b.get("similarity"), (Double) a.get("similarity")))
                .limit(limit > 0 ? limit : similarBeliefs.size())
                .collect(Collectors.toList());
    }
    
    private double calculateJaccardSimilarity(Set<RelationshipType> set1, Set<RelationshipType> set2) {
        if (set1.isEmpty() && set2.isEmpty()) {
            return 0.0;
        }
        
        Set<RelationshipType> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<RelationshipType> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    // ========================================
    // HEALTH AND MAINTENANCE
    // ========================================
    
    @Override
    public Map<String, Object> getServiceHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("implementation", "InMemoryBeliefGraphQueryService");
        health.put("totalBeliefs", beliefs.size());
        health.put("totalRelationships", relationships.size());
        health.put("indexesCount", outgoingRelationshipIndex.size() + incomingRelationshipIndex.size());
        health.put("agentCount", agentBeliefIndex.size());
        health.put("cacheSize", statisticsCache.size());
        health.put("lastCacheUpdate", lastCacheUpdate);
        
        return health;
    }
    
    @Override
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Index efficiency metrics
        long totalIndexEntries = outgoingRelationshipIndex.values().stream()
                .mapToLong(Set::size).sum() +
                incomingRelationshipIndex.values().stream()
                .mapToLong(Set::size).sum();
        
        metrics.put("totalIndexEntries", totalIndexEntries);
        metrics.put("averageBeliefDegree", beliefs.isEmpty() ? 0.0 : 
                (double) totalIndexEntries / beliefs.size());
        metrics.put("indexFragmentation", calculateIndexFragmentation());
        metrics.put("cacheHitRatio", calculateCacheHitRatio());
        metrics.put("memoryEstimate", estimateCurrentMemoryUsage());
        
        // Graph connectivity metrics
        metrics.put("graphDensity", calculateGraphDensity());
        metrics.put("averageClusterSize", calculateAverageClusterSize());
        
        return metrics;
    }
    
    @Override
    public long estimateGraphMemoryUsage(String agentId) {
        long beliefCount = agentBeliefIndex.getOrDefault(agentId, Collections.emptySet()).size();
        long relationshipCount = agentRelationshipIndex.getOrDefault(agentId, Collections.emptySet()).size();
        
        // Rough estimates based on typical object sizes
        long beliefMemory = beliefCount * 512; // ~512 bytes per belief
        long relationshipMemory = relationshipCount * 256; // ~256 bytes per relationship
        long indexMemory = (beliefCount + relationshipCount) * 64; // ~64 bytes per index entry
        
        return beliefMemory + relationshipMemory + indexMemory;
    }
    
    // ========================================
    // PRIVATE HELPER METHODS
    // ========================================
    
    private Map<String, Long> getCachedStatistics(String agentId) {
        if (System.currentTimeMillis() - lastCacheUpdate > CACHE_TTL_MS) {
            return null;
        }
        return statisticsCache.get(agentId);
    }
    
    private void cacheStatistics(String agentId, Map<String, Long> stats) {
        statisticsCache.put(agentId, stats);
        lastCacheUpdate = System.currentTimeMillis();
    }
    
    private void invalidateStatisticsCache(String agentId) {
        statisticsCache.remove(agentId);
    }
    
    private double calculateIndexFragmentation() {
        if (beliefs.isEmpty()) return 0.0;
        
        long totalSlots = outgoingRelationshipIndex.size() + incomingRelationshipIndex.size();
        long usedSlots = beliefs.size() * 2; // Each belief could have outgoing and incoming
        
        return totalSlots == 0 ? 0.0 : 1.0 - ((double) usedSlots / totalSlots);
    }
    
    private double calculateCacheHitRatio() {
        // Simple implementation - in production this would track actual hits/misses
        return statisticsCache.isEmpty() ? 0.0 : 0.75; // Placeholder
    }
    
    private long estimateCurrentMemoryUsage() {
        return beliefs.size() * 512L + relationships.size() * 256L;
    }
    
    private double calculateGraphDensity() {
        long beliefCount = beliefs.size();
        if (beliefCount < 2) return 0.0;
        
        long maxPossibleEdges = beliefCount * (beliefCount - 1);
        long actualEdges = relationships.size();
        
        return maxPossibleEdges == 0 ? 0.0 : (double) actualEdges / maxPossibleEdges;
    }
    
    private double calculateAverageClusterSize() {
        if (agentBeliefIndex.isEmpty()) return 0.0;
        
        return agentBeliefIndex.values().stream()
                .mapToInt(Set::size)
                .average()
                .orElse(0.0);
    }

    // ========================================
    // COMPREHENSIVE GRAPH STATISTICS
    // ========================================

    @Override
    public Map<String, Object> getComprehensiveGraphStatistics(String agentId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Basic counts
        stats.put("totalBeliefs", getBeliefsCount(agentId, true));
        stats.put("activeBeliefs", getBeliefsCount(agentId, false));
        stats.put("totalRelationships", getRelationshipsCount(agentId, true));
        stats.put("activeRelationships", getRelationshipsCount(agentId, false));
        stats.put("deprecatedBeliefs", getDeprecatedBeliefsCount(agentId));
        
        // Relationship type distribution
        stats.put("relationshipTypeDistribution", getRelationshipTypeDistribution(agentId));
        
        // Average relationship strength
        stats.put("averageRelationshipStrength", getAverageRelationshipStrength(agentId, false));
        
        // Graph density and connectivity
        long beliefCount = getBeliefsCount(agentId, false);
        long relationshipCount = getRelationshipsCount(agentId, false);
        double density = beliefCount > 1 ? (double) relationshipCount / (beliefCount * (beliefCount - 1)) : 0.0;
        stats.put("graphDensity", density);
        
        return stats;
    }

    @Override
    public double getAverageRelationshipStrength(String agentId, boolean includeInactive) {
        Set<String> agentRelationshipIds = agentRelationshipIndex.getOrDefault(agentId, Collections.emptySet());
        
        return agentRelationshipIds.stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .filter(rel -> includeInactive || rel.isActive())
                .mapToDouble(BeliefRelationship::getStrength)
                .average()
                .orElse(0.0);
    }

    // ========================================
    // GRAPH VALIDATION
    // ========================================

    @Override
    public List<String> validateGraphStructure(String agentId) {
        List<String> issues = new ArrayList<>();
        
        // Find orphaned relationships
        issues.addAll(findOrphanedRelationships(agentId).stream()
                .map(id -> "Orphaned relationship: " + id)
                .collect(Collectors.toList()));
        
        // Find self-referential relationships
        issues.addAll(findSelfReferencingRelationships(agentId).stream()
                .map(id -> "Self-referential relationship: " + id)
                .collect(Collectors.toList()));
        
        // Find temporally invalid relationships
        issues.addAll(findTemporallyInvalidRelationships(agentId).stream()
                .map(id -> "Temporally invalid relationship: " + id)
                .collect(Collectors.toList()));
        
        return issues;
    }

    @Override
    public List<String> findOrphanedRelationships(String agentId) {
        Set<String> agentRelationshipIds = agentRelationshipIndex.getOrDefault(agentId, Collections.emptySet());
        Set<String> agentBeliefIds = agentBeliefIndex.getOrDefault(agentId, Collections.emptySet());
        
        return agentRelationshipIds.stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .filter(rel -> !agentBeliefIds.contains(rel.getSourceBeliefId()) || 
                              !agentBeliefIds.contains(rel.getTargetBeliefId()))
                .map(BeliefRelationship::getId)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findSelfReferencingRelationships(String agentId) {
        Set<String> agentRelationshipIds = agentRelationshipIndex.getOrDefault(agentId, Collections.emptySet());
        
        return agentRelationshipIds.stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .filter(rel -> Objects.equals(rel.getSourceBeliefId(), rel.getTargetBeliefId()))
                .map(BeliefRelationship::getId)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findTemporallyInvalidRelationships(String agentId) {
        Set<String> agentRelationshipIds = agentRelationshipIndex.getOrDefault(agentId, Collections.emptySet());
        
        return agentRelationshipIds.stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .filter(rel -> rel.getEffectiveFrom() != null && rel.getEffectiveUntil() != null &&
                              rel.getEffectiveFrom().isAfter(rel.getEffectiveUntil()))
                .map(BeliefRelationship::getId)
                .collect(Collectors.toList());
    }

    // ========================================
    // ADVANCED GRAPH ANALYSIS
    // ========================================

    @Override
    public List<String> findDeprecatedBeliefIds(String agentId) {
        Set<String> agentRelationshipIds = agentRelationshipIndex.getOrDefault(agentId, Collections.emptySet());
        Set<String> deprecated = new HashSet<>();
        
        agentRelationshipIds.stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .filter(rel -> rel.isDeprecating() && rel.isCurrentlyEffective())
                .forEach(rel -> deprecated.add(rel.getTargetBeliefId()));
        
        return new ArrayList<>(deprecated);
    }

    @Override
    public List<String> findSupersedingBeliefIds(String beliefId, String agentId) {
        Set<String> incomingRelIds = incomingRelationshipIndex.getOrDefault(beliefId, Collections.emptySet());
        
        return incomingRelIds.stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .filter(rel -> rel.getAgentId().equals(agentId))
                .filter(rel -> rel.getRelationshipType() == RelationshipType.SUPERSEDES)
                .filter(BeliefRelationship::isCurrentlyEffective)
                .map(BeliefRelationship::getSourceBeliefId)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findDeprecationChain(String beliefId, String agentId) {
        List<String> chain = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        findDeprecationChainRecursive(beliefId, agentId, chain, visited);
        return chain;
    }

    private void findDeprecationChainRecursive(String beliefId, String agentId, List<String> chain, Set<String> visited) {
        if (visited.contains(beliefId)) {
            return; // Avoid cycles
        }
        
        visited.add(beliefId);
        if (beliefs.containsKey(beliefId)) {
            chain.add(beliefId);
        }
        
        // Find beliefs that supersede this one
        Set<String> incomingRelIds = incomingRelationshipIndex.getOrDefault(beliefId, Collections.emptySet());
        incomingRelIds.stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .filter(rel -> rel.getAgentId().equals(agentId))
                .filter(rel -> rel.isDeprecating() && rel.isCurrentlyEffective())
                .forEach(rel -> findDeprecationChainRecursive(rel.getSourceBeliefId(), agentId, chain, visited));
    }

    @Override
    public Set<String> findRelatedBeliefIds(String beliefId, String agentId, int maxDepth) {
        Set<String> related = new HashSet<>();
        Set<String> visited = new HashSet<>();
        findRelatedBeliefIdsRecursive(beliefId, agentId, related, visited, 0, maxDepth);
        related.remove(beliefId); // Remove the starting belief
        return related;
    }

    private void findRelatedBeliefIdsRecursive(String beliefId, String agentId, Set<String> related, 
                                              Set<String> visited, int currentDepth, int maxDepth) {
        if (currentDepth >= maxDepth || visited.contains(beliefId)) {
            return;
        }
        
        visited.add(beliefId);
        related.add(beliefId);
        
        // Follow outgoing relationships
        Set<String> outgoingRelIds = outgoingRelationshipIndex.getOrDefault(beliefId, Collections.emptySet());
        outgoingRelIds.stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .filter(rel -> rel.getAgentId().equals(agentId))
                .filter(BeliefRelationship::isActive)
                .forEach(rel -> findRelatedBeliefIdsRecursive(rel.getTargetBeliefId(), agentId, related, visited, currentDepth + 1, maxDepth));
        
        // Follow incoming relationships
        Set<String> incomingRelIds = incomingRelationshipIndex.getOrDefault(beliefId, Collections.emptySet());
        incomingRelIds.stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .filter(rel -> rel.getAgentId().equals(agentId))
                .filter(BeliefRelationship::isActive)
                .forEach(rel -> findRelatedBeliefIdsRecursive(rel.getSourceBeliefId(), agentId, related, visited, currentDepth + 1, maxDepth));
    }

    @Override
    public Map<String, Set<String>> findStronglyConnectedBeliefClusters(String agentId, double strengthThreshold) {
        Map<String, Set<String>> clusters = new HashMap<>();
        Set<String> agentBeliefIds = agentBeliefIndex.getOrDefault(agentId, Collections.emptySet());
        Set<String> visited = new HashSet<>();
        int clusterId = 0;
        
        for (String beliefId : agentBeliefIds) {
            if (!visited.contains(beliefId)) {
                Set<String> cluster = new HashSet<>();
                findStronglyConnectedRecursive(beliefId, agentId, cluster, visited, strengthThreshold);
                
                if (cluster.size() > 1) { // Only include clusters with multiple beliefs
                    clusters.put("cluster-" + clusterId++, cluster);
                }
            }
        }
        
        return clusters;
    }

    private void findStronglyConnectedRecursive(String beliefId, String agentId, Set<String> cluster, 
                                               Set<String> visited, double strengthThreshold) {
        if (visited.contains(beliefId)) {
            return;
        }
        
        visited.add(beliefId);
        cluster.add(beliefId);
        
        // Follow strong outgoing relationships
        Set<String> outgoingRelIds = outgoingRelationshipIndex.getOrDefault(beliefId, Collections.emptySet());
        outgoingRelIds.stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .filter(rel -> rel.getAgentId().equals(agentId))
                .filter(BeliefRelationship::isActive)
                .filter(rel -> rel.getStrength() >= strengthThreshold)
                .forEach(rel -> findStronglyConnectedRecursive(rel.getTargetBeliefId(), agentId, cluster, visited, strengthThreshold));
        
        // Follow strong incoming relationships
        Set<String> incomingRelIds = incomingRelationshipIndex.getOrDefault(beliefId, Collections.emptySet());
        incomingRelIds.stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .filter(rel -> rel.getAgentId().equals(agentId))
                .filter(BeliefRelationship::isActive)
                .filter(rel -> rel.getStrength() >= strengthThreshold)
                .forEach(rel -> findStronglyConnectedRecursive(rel.getSourceBeliefId(), agentId, cluster, visited, strengthThreshold));
    }

    // ========================================
    // GRAPH SNAPSHOT CREATION
    // ========================================

    @Override
    public ai.headkey.memory.dto.BeliefKnowledgeGraph createSnapshotGraph(String agentId, boolean includeInactive) {
        Set<String> agentBeliefIds = agentBeliefIndex.getOrDefault(agentId, Collections.emptySet());
        Set<String> agentRelationshipIds = agentRelationshipIndex.getOrDefault(agentId, Collections.emptySet());
        
        // Filter beliefs
        List<Belief> beliefList = agentBeliefIds.stream()
                .map(beliefs::get)
                .filter(Objects::nonNull)
                .filter(belief -> includeInactive || belief.isActive())
                .collect(Collectors.toList());
        
        // Filter relationships
        List<BeliefRelationship> relationshipList = agentRelationshipIds.stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .filter(rel -> includeInactive || rel.isActive())
                .collect(Collectors.toList());
        
        return new ai.headkey.memory.dto.BeliefKnowledgeGraph(agentId, beliefList, relationshipList);
    }

    @Override
    public ai.headkey.memory.dto.BeliefKnowledgeGraph createFilteredSnapshotGraph(String agentId, 
                                                                                 Set<String> beliefIds,
                                                                                 Set<ai.headkey.memory.enums.RelationshipType> relationshipTypes,
                                                                                 int maxBeliefs) {
        Set<String> agentBeliefIds = agentBeliefIndex.getOrDefault(agentId, Collections.emptySet());
        Set<String> agentRelationshipIds = agentRelationshipIndex.getOrDefault(agentId, Collections.emptySet());
        
        // Filter beliefs
        Stream<Belief> beliefStream = agentBeliefIds.stream()
                .map(beliefs::get)
                .filter(Objects::nonNull)
                .filter(belief -> beliefIds == null || beliefIds.contains(belief.getId()));
        
        if (maxBeliefs > 0) {
            beliefStream = beliefStream.limit(maxBeliefs);
        }
        
        List<Belief> beliefList = beliefStream.collect(Collectors.toList());
        Set<String> includedBeliefIds = beliefList.stream()
                .map(Belief::getId)
                .collect(Collectors.toSet());
        
        // Filter relationships
        List<BeliefRelationship> relationshipList = agentRelationshipIds.stream()
                .map(relationships::get)
                .filter(Objects::nonNull)
                .filter(rel -> relationshipTypes == null || relationshipTypes.contains(rel.getRelationshipType()))
                .filter(rel -> includedBeliefIds.contains(rel.getSourceBeliefId()) && 
                              includedBeliefIds.contains(rel.getTargetBeliefId()))
                .collect(Collectors.toList());
        
        return new ai.headkey.memory.dto.BeliefKnowledgeGraph(agentId, beliefList, relationshipList);
    }

    @Override
    public ai.headkey.memory.dto.BeliefKnowledgeGraph createExportGraph(String agentId, String format) {
        // For export, include all data but optimize based on format
        boolean includeInactive = "json".equalsIgnoreCase(format); // JSON exports might want all data
        return createSnapshotGraph(agentId, includeInactive);
    }
}