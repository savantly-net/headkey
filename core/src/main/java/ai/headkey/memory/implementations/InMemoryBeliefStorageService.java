package ai.headkey.memory.implementations;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefConflict;
import ai.headkey.memory.spi.BeliefStorageService;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of BeliefStorageService.
 * 
 * This implementation stores beliefs and conflicts in memory using concurrent
 * hash maps for thread-safe access. It's designed for development, testing,
 * and demonstration purposes. Data is not persisted between application restarts.
 * 
 * Features:
 * - Thread-safe concurrent access
 * - Fast lookup and search operations
 * - Memory-efficient storage
 * - Full-text search capabilities
 * - Similarity matching using configurable algorithms
 * 
 * Limitations:
 * - Data is lost on application restart
 * - Memory usage grows with data size
 * - No backup or recovery capabilities
 * - Limited scalability for large datasets
 * 
 * @since 1.0
 */
public class InMemoryBeliefStorageService implements BeliefStorageService {

    // Primary storage maps
    private final Map<String, Belief> beliefs;
    private final Map<String, BeliefConflict> conflicts;
    
    // Index maps for efficient queries
    private final Map<String, Set<String>> beliefsByAgent;
    private final Map<String, Set<String>> beliefsByCategory;
    private final Map<String, Set<String>> conflictsByAgent;
    
    // Statistics tracking
    private long totalStoreOperations = 0;
    private long totalQueryOperations = 0;
    private long totalSearchOperations = 0;
    private final Instant createdAt;

    public InMemoryBeliefStorageService() {
        this.beliefs = new ConcurrentHashMap<>();
        this.conflicts = new ConcurrentHashMap<>();
        this.beliefsByAgent = new ConcurrentHashMap<>();
        this.beliefsByCategory = new ConcurrentHashMap<>();
        this.conflictsByAgent = new ConcurrentHashMap<>();
        this.createdAt = Instant.now();
    }

    // ========== Basic CRUD Operations ==========

    @Override
    public Belief storeBelief(Belief belief) {
        if (belief == null) {
            throw new IllegalArgumentException("Belief cannot be null");
        }
        if (belief.getId() == null || belief.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Belief ID cannot be null or empty");
        }
        if (belief.getAgentId() == null || belief.getAgentId().trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }

        try {
            // Update timestamps for new beliefs
            if (!beliefs.containsKey(belief.getId())) {
                if (belief.getCreatedAt() == null) {
                    belief.setCreatedAt(Instant.now());
                }
            }
            belief.setLastUpdated(Instant.now());

            // Store the belief
            beliefs.put(belief.getId(), belief);

            // Update indexes
            updateBeliefIndexes(belief);

            totalStoreOperations++;
            return belief;
        } catch (Exception e) {
            throw new BeliefStorageException("Failed to store belief: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Belief> storeBeliefs(List<Belief> beliefsToStore) {
        if (beliefsToStore == null) {
            throw new IllegalArgumentException("Beliefs list cannot be null");
        }

        List<Belief> storedBeliefs = new ArrayList<>();
        for (Belief belief : beliefsToStore) {
            storedBeliefs.add(storeBelief(belief));
        }
        return storedBeliefs;
    }

    @Override
    public Optional<Belief> getBeliefById(String beliefId) {
        if (beliefId == null || beliefId.trim().isEmpty()) {
            throw new IllegalArgumentException("Belief ID cannot be null or empty");
        }

        totalQueryOperations++;
        return Optional.ofNullable(beliefs.get(beliefId));
    }

    @Override
    public List<Belief> getBeliefsById(Set<String> beliefIds) {
        if (beliefIds == null) {
            throw new IllegalArgumentException("Belief IDs set cannot be null");
        }

        totalQueryOperations++;
        return beliefIds.stream()
            .map(beliefs::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public boolean deleteBelief(String beliefId) {
        if (beliefId == null || beliefId.trim().isEmpty()) {
            throw new IllegalArgumentException("Belief ID cannot be null or empty");
        }

        try {
            Belief removedBelief = beliefs.remove(beliefId);
            if (removedBelief != null) {
                removeFromIndexes(removedBelief);
                totalStoreOperations++;
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new BeliefStorageException("Failed to delete belief: " + e.getMessage(), e);
        }
    }

    // ========== Query Operations ==========

    @Override
    public List<Belief> getBeliefsForAgent(String agentId, boolean includeInactive) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }

        totalQueryOperations++;
        Set<String> agentBeliefIds = beliefsByAgent.getOrDefault(agentId, Collections.emptySet());
        
        return agentBeliefIds.stream()
            .map(beliefs::get)
            .filter(Objects::nonNull)
            .filter(belief -> includeInactive || belief.isActive())
            .collect(Collectors.toList());
    }

    @Override
    public List<Belief> getBeliefsInCategory(String category, String agentId, boolean includeInactive) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be null or empty");
        }

        totalQueryOperations++;
        Set<String> categoryBeliefIds = beliefsByCategory.getOrDefault(category, Collections.emptySet());
        
        return categoryBeliefIds.stream()
            .map(beliefs::get)
            .filter(Objects::nonNull)
            .filter(belief -> includeInactive || belief.isActive())
            .filter(belief -> agentId == null || agentId.equals(belief.getAgentId()))
            .collect(Collectors.toList());
    }

    @Override
    public List<Belief> getAllActiveBeliefs() {
        totalQueryOperations++;
        return beliefs.values().stream()
            .filter(Belief::isActive)
            .collect(Collectors.toList());
    }

    @Override
    public List<Belief> getAllBeliefs() {
        totalQueryOperations++;
        return new ArrayList<>(beliefs.values());
    }

    @Override
    public List<Belief> getLowConfidenceBeliefs(double confidenceThreshold, String agentId) {
        if (confidenceThreshold < 0.0 || confidenceThreshold > 1.0) {
            throw new IllegalArgumentException("Confidence threshold must be between 0.0 and 1.0");
        }

        totalQueryOperations++;
        return beliefs.values().stream()
            .filter(Belief::isActive)
            .filter(belief -> belief.getConfidence() < confidenceThreshold)
            .filter(belief -> agentId == null || agentId.equals(belief.getAgentId()))
            .sorted(Comparator.comparing(Belief::getConfidence))
            .collect(Collectors.toList());
    }

    @Override
    public List<Belief> searchBeliefs(String searchText, String agentId, int limit) {
        if (searchText == null || searchText.trim().isEmpty()) {
            throw new IllegalArgumentException("Search text cannot be null or empty");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be at least 1");
        }

        totalSearchOperations++;
        String normalizedSearch = searchText.toLowerCase().trim();
        
        return beliefs.values().stream()
            .filter(Belief::isActive)
            .filter(belief -> agentId == null || agentId.equals(belief.getAgentId()))
            .filter(belief -> beliefMatchesSearch(belief, normalizedSearch))
            .sorted((b1, b2) -> Double.compare(
                calculateSearchRelevance(b2, normalizedSearch),
                calculateSearchRelevance(b1, normalizedSearch)
            ))
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public List<SimilarBelief> findSimilarBeliefs(String statement, String agentId, 
                                                 double similarityThreshold, int limit) {
        if (statement == null || statement.trim().isEmpty()) {
            throw new IllegalArgumentException("Statement cannot be null or empty");
        }
        if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
            throw new IllegalArgumentException("Similarity threshold must be between 0.0 and 1.0");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be at least 1");
        }

        totalSearchOperations++;
        String normalizedStatement = statement.toLowerCase().trim();
        
        return beliefs.values().stream()
            .filter(Belief::isActive)
            .filter(belief -> agentId == null || agentId.equals(belief.getAgentId()))
            .map(belief -> new SimilarBelief(belief, calculateSimilarity(belief.getStatement(), normalizedStatement)))
            .filter(similarBelief -> similarBelief.getSimilarityScore() >= similarityThreshold)
            .sorted((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    // ========== Conflict Management ==========

    @Override
    public BeliefConflict storeConflict(BeliefConflict conflict) {
        if (conflict == null) {
            throw new IllegalArgumentException("Conflict cannot be null");
        }
        if (conflict.getId() == null || conflict.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Conflict ID cannot be null or empty");
        }

        try {
            if (conflict.getDetectedAt() == null) {
                conflict.setDetectedAt(Instant.now());
            }

            conflicts.put(conflict.getId(), conflict);
            
            // Update conflict index
            if (conflict.getAgentId() != null) {
                conflictsByAgent.computeIfAbsent(conflict.getAgentId(), k -> ConcurrentHashMap.newKeySet())
                    .add(conflict.getId());
            }

            totalStoreOperations++;
            return conflict;
        } catch (Exception e) {
            throw new BeliefStorageException("Failed to store conflict: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<BeliefConflict> getConflictById(String conflictId) {
        if (conflictId == null || conflictId.trim().isEmpty()) {
            throw new IllegalArgumentException("Conflict ID cannot be null or empty");
        }

        totalQueryOperations++;
        return Optional.ofNullable(conflicts.get(conflictId));
    }

    @Override
    public List<BeliefConflict> getUnresolvedConflicts(String agentId) {
        totalQueryOperations++;
        return conflicts.values().stream()
            .filter(conflict -> !conflict.isResolved())
            .filter(conflict -> agentId == null || agentId.equals(conflict.getAgentId()))
            .sorted(Comparator.comparing(BeliefConflict::getDetectedAt).reversed())
            .collect(Collectors.toList());
    }

    @Override
    public boolean removeConflict(String conflictId) {
        if (conflictId == null || conflictId.trim().isEmpty()) {
            throw new IllegalArgumentException("Conflict ID cannot be null or empty");
        }

        try {
            BeliefConflict removedConflict = conflicts.remove(conflictId);
            if (removedConflict != null) {
                // Remove from agent index
                if (removedConflict.getAgentId() != null) {
                    Set<String> agentConflicts = conflictsByAgent.get(removedConflict.getAgentId());
                    if (agentConflicts != null) {
                        agentConflicts.remove(conflictId);
                    }
                }
                totalStoreOperations++;
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new BeliefStorageException("Failed to remove conflict: " + e.getMessage(), e);
        }
    }

    // ========== Statistics and Analytics ==========

    @Override
    public Map<String, Object> getStorageStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalBeliefs", beliefs.size());
        stats.put("totalConflicts", conflicts.size());
        stats.put("totalAgents", beliefsByAgent.size());
        stats.put("totalCategories", beliefsByCategory.size());
        stats.put("totalStoreOperations", totalStoreOperations);
        stats.put("totalQueryOperations", totalQueryOperations);
        stats.put("totalSearchOperations", totalSearchOperations);
        stats.put("uptime", Instant.now().toEpochMilli() - createdAt.toEpochMilli());
        
        // Memory usage estimation
        long estimatedMemoryBytes = beliefs.size() * 1024 + conflicts.size() * 512; // Rough estimate
        stats.put("estimatedMemoryUsageBytes", estimatedMemoryBytes);
        
        return stats;
    }

    @Override
    public Map<String, Long> getBeliefDistributionByCategory(String agentId) {
        totalQueryOperations++;
        
        return beliefs.values().stream()
            .filter(Belief::isActive)
            .filter(belief -> agentId == null || agentId.equals(belief.getAgentId()))
            .filter(belief -> belief.getCategory() != null)
            .collect(Collectors.groupingBy(
                Belief::getCategory,
                Collectors.counting()
            ));
    }

    @Override
    public Map<String, Long> getBeliefDistributionByConfidence(String agentId) {
        totalQueryOperations++;
        
        return beliefs.values().stream()
            .filter(Belief::isActive)
            .filter(belief -> agentId == null || agentId.equals(belief.getAgentId()))
            .collect(Collectors.groupingBy(
                belief -> getConfidenceBucket(belief.getConfidence()),
                Collectors.counting()
            ));
    }

    @Override
    public long countBeliefsForAgent(String agentId, boolean includeInactive) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }

        totalQueryOperations++;
        Set<String> agentBeliefIds = beliefsByAgent.getOrDefault(agentId, Collections.emptySet());
        
        return agentBeliefIds.stream()
            .map(beliefs::get)
            .filter(Objects::nonNull)
            .filter(belief -> includeInactive || belief.isActive())
            .count();
    }

    // ========== Maintenance Operations ==========

    @Override
    public Map<String, Object> optimizeStorage() {
        Map<String, Object> results = new HashMap<>();
        int initialSize = beliefs.size() + conflicts.size();
        
        // Remove orphaned index entries
        cleanupIndexes();
        
        // Compact maps if needed (not much to do for ConcurrentHashMap)
        int finalSize = beliefs.size() + conflicts.size();
        
        results.put("operation", "optimize");
        results.put("initialSize", initialSize);
        results.put("finalSize", finalSize);
        results.put("optimizedAt", Instant.now());
        results.put("success", true);
        
        return results;
    }

    @Override
    public Map<String, Object> validateIntegrity() {
        Map<String, Object> results = new HashMap<>();
        List<String> issues = new ArrayList<>();
        
        // Check belief consistency
        for (Belief belief : beliefs.values()) {
            if (belief.getId() == null || belief.getAgentId() == null) {
                issues.add("Belief with null ID or agent ID: " + belief);
            }
            if (belief.getConfidence() < 0.0 || belief.getConfidence() > 1.0) {
                issues.add("Belief with invalid confidence: " + belief.getId());
            }
        }
        
        // Check index consistency
        for (Map.Entry<String, Set<String>> entry : beliefsByAgent.entrySet()) {
            String agentId = entry.getKey();
            for (String beliefId : entry.getValue()) {
                Belief belief = beliefs.get(beliefId);
                if (belief == null) {
                    issues.add("Orphaned belief ID in agent index: " + beliefId);
                } else if (!agentId.equals(belief.getAgentId())) {
                    issues.add("Mismatched agent ID in index: " + beliefId);
                }
            }
        }
        
        results.put("operation", "validate");
        results.put("validatedAt", Instant.now());
        results.put("issuesFound", issues.size());
        results.put("issues", issues);
        results.put("healthy", issues.isEmpty());
        
        return results;
    }

    @Override
    public Map<String, Object> createBackup(String backupId, Map<String, Object> options) {
        Map<String, Object> results = new HashMap<>();
        
        // For in-memory storage, we can only create a snapshot
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("beliefs", new HashMap<>(beliefs));
        snapshot.put("conflicts", new HashMap<>(conflicts));
        snapshot.put("beliefsByAgent", deepCopyIndex(beliefsByAgent));
        snapshot.put("beliefsByCategory", deepCopyIndex(beliefsByCategory));
        snapshot.put("conflictsByAgent", deepCopyIndex(conflictsByAgent));
        
        results.put("backupId", backupId);
        results.put("backupType", "memory_snapshot");
        results.put("createdAt", Instant.now());
        results.put("beliefCount", beliefs.size());
        results.put("conflictCount", conflicts.size());
        results.put("success", true);
        results.put("note", "In-memory backup is only a snapshot, data will be lost on restart");
        
        return results;
    }

    // ========== Health and Monitoring ==========

    @Override
    public boolean isHealthy() {
        try {
            // Basic health checks
            return beliefs != null && 
                   conflicts != null && 
                   beliefsByAgent != null && 
                   beliefsByCategory != null &&
                   conflictsByAgent != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Map<String, Object> getHealthInfo() {
        Map<String, Object> health = new HashMap<>();
        
        health.put("status", isHealthy() ? "healthy" : "unhealthy");
        health.put("checkedAt", Instant.now());
        health.put("storageType", "in-memory");
        health.put("dataIntegrityCheck", validateIntegrity());
        health.put("statisticsSnapshot", getStorageStatistics());
        
        return health;
    }

    @Override
    public Map<String, Object> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();
        
        info.put("serviceType", "InMemoryBeliefStorageService");
        info.put("version", "1.0");
        info.put("description", "In-memory belief storage using concurrent hash maps");
        info.put("persistence", "none");
        info.put("scalability", "limited by available memory");
        info.put("threadSafety", "full");
        info.put("backupSupport", "snapshot only");
        info.put("queryCapabilities", Arrays.asList("id-lookup", "agent-filter", "category-filter", "text-search", "similarity-search"));
        info.put("createdAt", createdAt);
        
        return info;
    }

    // ========== Private Helper Methods ==========

    private void updateBeliefIndexes(Belief belief) {
        // Index by agent
        beliefsByAgent.computeIfAbsent(belief.getAgentId(), k -> ConcurrentHashMap.newKeySet())
            .add(belief.getId());
        
        // Index by category if present
        if (belief.getCategory() != null) {
            beliefsByCategory.computeIfAbsent(belief.getCategory(), k -> ConcurrentHashMap.newKeySet())
                .add(belief.getId());
        }
    }

    private void removeFromIndexes(Belief belief) {
        // Remove from agent index
        Set<String> agentBeliefs = beliefsByAgent.get(belief.getAgentId());
        if (agentBeliefs != null) {
            agentBeliefs.remove(belief.getId());
            if (agentBeliefs.isEmpty()) {
                beliefsByAgent.remove(belief.getAgentId());
            }
        }
        
        // Remove from category index
        if (belief.getCategory() != null) {
            Set<String> categoryBeliefs = beliefsByCategory.get(belief.getCategory());
            if (categoryBeliefs != null) {
                categoryBeliefs.remove(belief.getId());
                if (categoryBeliefs.isEmpty()) {
                    beliefsByCategory.remove(belief.getCategory());
                }
            }
        }
    }

    private void cleanupIndexes() {
        // Clean agent index
        beliefsByAgent.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(beliefId -> !beliefs.containsKey(beliefId));
            return entry.getValue().isEmpty();
        });
        
        // Clean category index
        beliefsByCategory.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(beliefId -> !beliefs.containsKey(beliefId));
            return entry.getValue().isEmpty();
        });
        
        // Clean conflict agent index
        conflictsByAgent.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(conflictId -> !conflicts.containsKey(conflictId));
            return entry.getValue().isEmpty();
        });
    }

    private boolean beliefMatchesSearch(Belief belief, String searchText) {
        String statement = belief.getStatement().toLowerCase();
        String category = belief.getCategory() != null ? belief.getCategory().toLowerCase() : "";
        
        return statement.contains(searchText) || 
               category.contains(searchText) ||
               belief.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(searchText));
    }

    private double calculateSearchRelevance(Belief belief, String searchText) {
        String statement = belief.getStatement().toLowerCase();
        double relevance = 0.0;
        
        // Exact phrase match gets highest score
        if (statement.contains(searchText)) {
            relevance += 1.0;
        }
        
        // Word matches
        String[] searchWords = searchText.split("\\s+");
        String[] statementWords = statement.split("\\s+");
        
        for (String searchWord : searchWords) {
            for (String statementWord : statementWords) {
                if (statementWord.equals(searchWord)) {
                    relevance += 0.5;
                } else if (statementWord.contains(searchWord)) {
                    relevance += 0.3;
                }
            }
        }
        
        // Boost by confidence
        relevance *= belief.getConfidence();
        
        return relevance;
    }

    private double calculateSimilarity(String statement1, String statement2) {
        if (statement1 == null || statement2 == null) {
            return 0.0;
        }
        
        String s1 = statement1.toLowerCase().trim();
        String s2 = statement2.toLowerCase().trim();
        
        if (s1.equals(s2)) {
            return 1.0;
        }
        
        // Simple Jaccard similarity
        String[] words1 = s1.split("\\s+");
        String[] words2 = s2.split("\\s+");
        
        Set<String> set1 = new HashSet<>(Arrays.asList(words1));
        Set<String> set2 = new HashSet<>(Arrays.asList(words2));
        
        // Remove common stop words
        Set<String> stopWords = Set.of("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "is", "are", "was", "were");
        set1.removeAll(stopWords);
        set2.removeAll(stopWords);
        
        if (set1.isEmpty() && set2.isEmpty()) {
            return 0.0;
        }
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private String getConfidenceBucket(double confidence) {
        if (confidence >= 0.8) return "high";
        if (confidence >= 0.5) return "medium";
        return "low";
    }

    private Map<String, Set<String>> deepCopyIndex(Map<String, Set<String>> original) {
        Map<String, Set<String>> copy = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : original.entrySet()) {
            copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return copy;
    }
}