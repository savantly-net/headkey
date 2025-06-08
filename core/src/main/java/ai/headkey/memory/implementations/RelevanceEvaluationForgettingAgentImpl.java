package ai.headkey.memory.implementations;

import ai.headkey.memory.dto.ForgettingReport;
import ai.headkey.memory.dto.ForgettingStrategy;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.enums.ForgettingStrategyType;
import ai.headkey.memory.interfaces.MemoryEncodingSystem;
import ai.headkey.memory.interfaces.RelevanceEvaluationForgettingAgent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * General implementation of the Relevance Evaluation & Forgetting Agent (REFA).
 * 
 * This implementation provides comprehensive memory lifecycle management with
 * multiple forgetting strategies and configurable relevance evaluation.
 * It includes protection mechanisms, archiving capabilities, and detailed
 * statistics tracking.
 * 
 * This is a general-purpose implementation that can work with any MemoryEncodingSystem
 * implementation injected through its constructor, making it suitable for both
 * in-memory and persistent storage scenarios.
 * 
 * @since 1.0
 */
public class RelevanceEvaluationForgettingAgentImpl implements RelevanceEvaluationForgettingAgent {
    
    private final MemoryEncodingSystem memorySystem;
    private final Map<String, String> protectionRules;
    private final Map<String, Object> relevanceParameters;
    private final Map<String, ArchiveEntry> archivedMemories;
    private final Object lock = new Object();
    
    // Default relevance parameters
    private double recencyWeight = 0.3;
    private double frequencyWeight = 0.3;
    private double importanceWeight = 0.2;
    private double beliefWeight = 0.2;
    private int maxAgeForHighRelevance = 7; // days
    private int minAccessesForHighRelevance = 3;
    
    // Statistics tracking
    private long totalEvaluations = 0;
    private long totalBatchEvaluations = 0;
    private long totalForgettingCycles = 0;
    private long totalMemoriesRemoved = 0;
    private long totalMemoriesArchived = 0;
    private long totalMemoriesRestored = 0;
    private final Map<String, Long> strategyUsage = new ConcurrentHashMap<>();
    private final Map<String, Long> protectionRuleHits = new ConcurrentHashMap<>();
    private final Instant startTime;
    
    /**
     * Creates a new relevance evaluation forgetting agent implementation.
     * 
     * @param memorySystem The memory encoding system to work with
     */
    public RelevanceEvaluationForgettingAgentImpl(MemoryEncodingSystem memorySystem) {
        this.memorySystem = memorySystem;
        this.protectionRules = new ConcurrentHashMap<>();
        this.relevanceParameters = new ConcurrentHashMap<>();
        this.archivedMemories = new ConcurrentHashMap<>();
        this.startTime = Instant.now();
        
        initializeDefaultProtectionRules();
        initializeDefaultRelevanceParameters();
    }
    
    /**
     * Initializes default protection rules.
     */
    private void initializeDefaultProtectionRules() {
        protectionRules.put("high_importance", "metadata.importance >= 8");
        protectionRules.put("recent_creation", "age_days <= 1");
        protectionRules.put("high_access_frequency", "access_count >= 10");
        protectionRules.put("belief_evidence", "is_belief_evidence = true");
        protectionRules.put("user_flagged", "metadata.protected = true");
    }
    
    /**
     * Initializes default relevance parameters.
     */
    private void initializeDefaultRelevanceParameters() {
        relevanceParameters.put("recencyWeight", recencyWeight);
        relevanceParameters.put("frequencyWeight", frequencyWeight);
        relevanceParameters.put("importanceWeight", importanceWeight);
        relevanceParameters.put("beliefWeight", beliefWeight);
        relevanceParameters.put("maxAgeForHighRelevance", maxAgeForHighRelevance);
        relevanceParameters.put("minAccessesForHighRelevance", minAccessesForHighRelevance);
    }
    
    @Override
    public double evaluateRelevance(MemoryRecord memory) {
        if (memory == null) {
            throw new IllegalArgumentException("Memory record cannot be null");
        }
        
        try {
            totalEvaluations++;
            
            double recencyScore = calculateRecencyScore(memory);
            double frequencyScore = calculateFrequencyScore(memory);
            double importanceScore = calculateImportanceScore(memory);
            double beliefScore = calculateBeliefScore(memory);
            
            double relevanceScore = (recencyScore * recencyWeight) +
                                  (frequencyScore * frequencyWeight) +
                                  (importanceScore * importanceWeight) +
                                  (beliefScore * beliefWeight);
            
            // Ensure score is within bounds
            relevanceScore = Math.max(0.0, Math.min(1.0, relevanceScore));
            
            // Update the memory's relevance score
            memory.setRelevanceScore(relevanceScore);
            
            return relevanceScore;
        } catch (Exception e) {
            throw new RelevanceEvaluationException("Failed to evaluate relevance: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, Double> evaluateRelevanceBatch(List<MemoryRecord> memories) {
        if (memories == null || memories.isEmpty()) {
            throw new IllegalArgumentException("Memories list cannot be null or empty");
        }
        
        try {
            Map<String, Double> results = new HashMap<>();
            
            for (MemoryRecord memory : memories) {
                double score = evaluateRelevance(memory);
                results.put(memory.getId(), score);
            }
            
            totalBatchEvaluations++;
            return results;
        } catch (Exception e) {
            throw new RelevanceEvaluationException("Failed to evaluate batch relevance: " + e.getMessage(), e);
        }
    }
    
    @Override
    public ForgettingReport performForgetting(ForgettingStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Forgetting strategy cannot be null");
        }
        
        try {
            synchronized (lock) {
                totalForgettingCycles++;
                strategyUsage.merge(strategy.getType().toString(), 1L, Long::sum);
                
                ForgettingReport report = new ForgettingReport();
                
                // Identify candidates
                List<MemoryRecord> candidates = identifyForgettingCandidates(strategy);
                
                // Filter out protected memories
                List<MemoryRecord> eligibleForRemoval = candidates.stream()
                    .filter(memory -> !isProtected(memory))
                    .collect(Collectors.toList());
                
                // Apply removal limit if specified
                int maxToRemove = (Integer) strategy.getCustomParameter("maxToRemove", Integer.class);
                if (maxToRemove > 0 && eligibleForRemoval.size() > maxToRemove) {
                    eligibleForRemoval = eligibleForRemoval.stream()
                        .limit(maxToRemove)
                        .collect(Collectors.toList());
                }
                
                Set<String> removedIds = new HashSet<>();
                Set<String> archivedIds = new HashSet<>();
                
                if (!strategy.isDryRun()) {
                    for (MemoryRecord memory : eligibleForRemoval) {
                        Boolean archiveFirst = (Boolean) strategy.getCustomParameter("archiveBeforeRemoval", Boolean.class);
                        if (archiveFirst != null && archiveFirst) {
                            // Archive before removal
                            String reason = "Forgotten by strategy: " + strategy.getType();
                            Set<String> archived = archiveMemories(Set.of(memory.getId()), reason);
                            archivedIds.addAll(archived);
                        }
                        
                        // Remove from active storage
                        if (memorySystem.removeMemory(memory.getId())) {
                            removedIds.add(memory.getId());
                            totalMemoriesRemoved++;
                        }
                    }
                }
                
                return report;
            }
        } catch (Exception e) {
            throw new ForgettingException("Failed to perform forgetting: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<MemoryRecord> identifyForgettingCandidates(ForgettingStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Forgetting strategy cannot be null");
        }
        
        try {
            List<MemoryRecord> allMemories;
            
            // Get memories for specific agent or all memories
            if (strategy.getAgentId() != null && !strategy.getAgentId().trim().isEmpty()) {
                allMemories = memorySystem.getMemoriesForAgent(strategy.getAgentId(), 0);
            } else {
                // Get all memories by querying storage statistics and then fetching
                Map<String, Object> stats = memorySystem.getStorageStatistics();
                @SuppressWarnings("unchecked")
                Map<String, Integer> agentDist = (Map<String, Integer>) stats.get("agentDistribution");
                
                allMemories = new ArrayList<>();
                for (String agentId : agentDist.keySet()) {
                    allMemories.addAll(memorySystem.getMemoriesForAgent(agentId, 0));
                }
            }
            
            // Filter by category if specified
            if (strategy.getCategory() != null && !strategy.getCategory().trim().isEmpty()) {
                allMemories = allMemories.stream()
                    .filter(memory -> strategy.getCategory().equals(
                        memory.getCategory() != null ? memory.getCategory().getPrimary() : null))
                    .collect(Collectors.toList());
            }
            
            // Apply strategy-specific filtering
            return applyStrategyFiltering(allMemories, strategy);
            
        } catch (Exception e) {
            throw new RelevanceEvaluationException("Failed to identify candidates: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void setProtectionRules(Map<String, String> protectionRules) {
        if (protectionRules == null) {
            throw new IllegalArgumentException("Protection rules cannot be null");
        }
        
        synchronized (lock) {
            this.protectionRules.clear();
            this.protectionRules.putAll(protectionRules);
        }
    }
    
    @Override
    public Map<String, String> getProtectionRules() {
        return new HashMap<>(protectionRules);
    }
    
    @Override
    public boolean isProtected(MemoryRecord memory) {
        if (memory == null) {
            throw new IllegalArgumentException("Memory record cannot be null");
        }
        
        for (Map.Entry<String, String> rule : protectionRules.entrySet()) {
            if (evaluateProtectionRule(memory, rule.getValue())) {
                protectionRuleHits.merge(rule.getKey(), 1L, Long::sum);
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public int updateRelevanceScores(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }
        
        try {
            List<MemoryRecord> memories = memorySystem.getMemoriesForAgent(agentId, 0);
            int updatedCount = 0;
            
            for (MemoryRecord memory : memories) {
                double oldScore = memory.getRelevanceScore() != null ? memory.getRelevanceScore() : 0.0;
                double newScore = evaluateRelevance(memory);
                
                if (Math.abs(newScore - oldScore) > 0.01) { // Only update if significantly different
                    memorySystem.updateMemory(memory);
                    updatedCount++;
                }
            }
            
            return updatedCount;
        } catch (Exception e) {
            throw new RelevanceEvaluationException("Failed to update relevance scores: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<MemoryRecord> getLowRelevanceMemories(double scoreThreshold, String agentId, int limit) {
        if (scoreThreshold < 0.0 || scoreThreshold > 1.0) {
            throw new IllegalArgumentException("Score threshold must be between 0.0 and 1.0");
        }
        
        List<MemoryRecord> memories;
        
        if (agentId != null && !agentId.trim().isEmpty()) {
            memories = memorySystem.getMemoriesForAgent(agentId, 0);
        } else {
            // Get all memories
            Map<String, Object> stats = memorySystem.getStorageStatistics();
            @SuppressWarnings("unchecked")
            Map<String, Integer> agentDist = (Map<String, Integer>) stats.get("agentDistribution");
            
            memories = new ArrayList<>();
            for (String agent : agentDist.keySet()) {
                memories.addAll(memorySystem.getMemoriesForAgent(agent, 0));
            }
        }
        
        List<MemoryRecord> lowRelevanceMemories = memories.stream()
            .filter(memory -> {
                double score = memory.getRelevanceScore() != null ? memory.getRelevanceScore() : 0.0;
                return score < scoreThreshold;
            })
            .sorted(Comparator.comparingDouble(memory -> 
                memory.getRelevanceScore() != null ? memory.getRelevanceScore() : 0.0))
            .collect(Collectors.toList());
        
        if (limit > 0) {
            return lowRelevanceMemories.stream().limit(limit).collect(Collectors.toList());
        }
        
        return lowRelevanceMemories;
    }
    
    @Override
    public List<MemoryRecord> getUnaccessedMemories(int daysUnaccessed, String agentId, int limit) {
        if (daysUnaccessed < 0) {
            throw new IllegalArgumentException("Days unaccessed cannot be negative");
        }
        
        Instant threshold = Instant.now().minus(daysUnaccessed, ChronoUnit.DAYS);
        
        List<MemoryRecord> memories;
        
        if (agentId != null && !agentId.trim().isEmpty()) {
            memories = memorySystem.getMemoriesForAgent(agentId, 0);
        } else {
            // Get all memories
            Map<String, Object> stats = memorySystem.getStorageStatistics();
            @SuppressWarnings("unchecked")
            Map<String, Integer> agentDist = (Map<String, Integer>) stats.get("agentDistribution");
            
            memories = new ArrayList<>();
            for (String agent : agentDist.keySet()) {
                memories.addAll(memorySystem.getMemoriesForAgent(agent, 0));
            }
        }
        
        List<MemoryRecord> unaccessedMemories = memories.stream()
            .filter(memory -> memory.getLastAccessed() == null || memory.getLastAccessed().isBefore(threshold))
            .sorted(Comparator.comparing(memory -> 
                memory.getLastAccessed() != null ? memory.getLastAccessed() : Instant.MIN))
            .collect(Collectors.toList());
        
        if (limit > 0) {
            return unaccessedMemories.stream().limit(limit).collect(Collectors.toList());
        }
        
        return unaccessedMemories;
    }
    
    @Override
    public Set<String> archiveMemories(Set<String> memoryIds, String archiveReason) {
        if (memoryIds == null || memoryIds.isEmpty()) {
            throw new IllegalArgumentException("Memory IDs cannot be null or empty");
        }
        
        try {
            synchronized (lock) {
                Set<String> archivedIds = new HashSet<>();
                
                for (String memoryId : memoryIds) {
                    Optional<MemoryRecord> memoryOpt = memorySystem.getMemory(memoryId);
                    if (memoryOpt.isPresent()) {
                        MemoryRecord memory = memoryOpt.get();
                        
                        // Create archive entry
                        ArchiveEntry entry = new ArchiveEntry(
                            memoryId,
                            memory.getAgentId(),
                            Instant.now().toString(),
                            archiveReason != null ? archiveReason : "Manual archive",
                            createMemorySummary(memory)
                        );
                        
                        archivedMemories.put(memoryId, entry);
                        archivedIds.add(memoryId);
                        totalMemoriesArchived++;
                    }
                }
                
                return archivedIds;
            }
        } catch (Exception e) {
            throw new ForgettingException("Failed to archive memories: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Set<String> restoreArchivedMemories(Set<String> memoryIds) {
        if (memoryIds == null || memoryIds.isEmpty()) {
            throw new IllegalArgumentException("Memory IDs cannot be null or empty");
        }
        
        try {
            synchronized (lock) {
                Set<String> restoredIds = new HashSet<>();
                
                for (String memoryId : memoryIds) {
                    ArchiveEntry entry = archivedMemories.remove(memoryId);
                    if (entry != null) {
                        // In a real implementation, we would restore the full memory record
                        // For this in-memory version, we just track the restoration
                        restoredIds.add(memoryId);
                        totalMemoriesRestored++;
                    }
                }
                
                return restoredIds;
            }
        } catch (Exception e) {
            throw new ForgettingException("Failed to restore archived memories: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<ArchiveEntry> getArchivedMemories(String agentId, int limit) {
        List<ArchiveEntry> entries = archivedMemories.values().stream()
            .filter(entry -> agentId == null || agentId.equals(entry.getAgentId()))
            .sorted((a, b) -> b.getArchivedAt().compareTo(a.getArchivedAt()))
            .collect(Collectors.toList());
        
        if (limit > 0) {
            return entries.stream().limit(limit).collect(Collectors.toList());
        }
        
        return entries;
    }
    
    @Override
    public Map<String, Object> getForgettingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalForgettingCycles", totalForgettingCycles);
        stats.put("totalMemoriesRemoved", totalMemoriesRemoved);
        stats.put("totalMemoriesArchived", totalMemoriesArchived);
        stats.put("totalMemoriesRestored", totalMemoriesRestored);
        stats.put("archivedMemoriesCount", archivedMemories.size());
        stats.put("uptime", Instant.now().getEpochSecond() - startTime.getEpochSecond());
        
        // Strategy usage distribution
        stats.put("strategyUsage", new HashMap<>(strategyUsage));
        
        // Protection rule hit counts
        stats.put("protectionRuleHits", new HashMap<>(protectionRuleHits));
        
        // Archive statistics
        Map<String, Integer> archiveByAgent = new HashMap<>();
        for (ArchiveEntry entry : archivedMemories.values()) {
            archiveByAgent.merge(entry.getAgentId(), 1, Integer::sum);
        }
        stats.put("archiveDistribution", archiveByAgent);
        
        return stats;
    }
    
    @Override
    public Map<String, Object> getRelevanceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalEvaluations", totalEvaluations);
        stats.put("totalBatchEvaluations", totalBatchEvaluations);
        stats.put("relevanceParameters", new HashMap<>(relevanceParameters));
        
        // Get current relevance score distribution
        Map<String, Integer> scoreDistribution = new HashMap<>();
        Map<String, Object> storageStats = memorySystem.getStorageStatistics();
        @SuppressWarnings("unchecked")
        Map<String, Integer> agentDist = (Map<String, Integer>) storageStats.get("agentDistribution");
        
        for (String agentId : agentDist.keySet()) {
            List<MemoryRecord> memories = memorySystem.getMemoriesForAgent(agentId, 0);
            for (MemoryRecord memory : memories) {
                double score = memory.getRelevanceScore() != null ? memory.getRelevanceScore() : 0.0;
                String bucket = getRelevanceBucket(score);
                scoreDistribution.merge(bucket, 1, Integer::sum);
            }
        }
        
        stats.put("scoreDistribution", scoreDistribution);
        
        return stats;
    }
    
    @Override
    public void configureRelevanceParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        
        synchronized (lock) {
            relevanceParameters.putAll(parameters);
            
            // Update internal parameters
            if (parameters.containsKey("recencyWeight")) {
                recencyWeight = ((Number) parameters.get("recencyWeight")).doubleValue();
            }
            if (parameters.containsKey("frequencyWeight")) {
                frequencyWeight = ((Number) parameters.get("frequencyWeight")).doubleValue();
            }
            if (parameters.containsKey("importanceWeight")) {
                importanceWeight = ((Number) parameters.get("importanceWeight")).doubleValue();
            }
            if (parameters.containsKey("beliefWeight")) {
                beliefWeight = ((Number) parameters.get("beliefWeight")).doubleValue();
            }
            if (parameters.containsKey("maxAgeForHighRelevance")) {
                maxAgeForHighRelevance = ((Number) parameters.get("maxAgeForHighRelevance")).intValue();
            }
            if (parameters.containsKey("minAccessesForHighRelevance")) {
                minAccessesForHighRelevance = ((Number) parameters.get("minAccessesForHighRelevance")).intValue();
            }
        }
    }
    
    @Override
    public boolean isHealthy() {
        try {
            return memorySystem != null && memorySystem.isHealthy() &&
                   protectionRules != null && relevanceParameters != null &&
                   archivedMemories != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Calculates recency score based on memory age.
     */
    private double calculateRecencyScore(MemoryRecord memory) {
        long ageInDays = memory.getAgeInSeconds() / (24 * 60 * 60);
        
        if (ageInDays <= maxAgeForHighRelevance) {
            return 1.0;
        }
        
        // Exponential decay
        return Math.exp(-0.1 * ageInDays);
    }
    
    /**
     * Calculates frequency score based on access count.
     */
    private double calculateFrequencyScore(MemoryRecord memory) {
        int accessCount = memory.getAccessCount();
        
        if (accessCount >= minAccessesForHighRelevance) {
            return 1.0;
        }
        
        return Math.min(1.0, (double) accessCount / minAccessesForHighRelevance);
    }
    
    /**
     * Calculates importance score from metadata.
     */
    private double calculateImportanceScore(MemoryRecord memory) {
        if (memory.getMetadata() != null && memory.getMetadata().getImportance() != null) {
            return memory.getMetadata().getImportance() / 10.0; // Assuming importance is 0-10
        }
        
        // Check for importance indicators in tags
        if (memory.getMetadata() != null && memory.getMetadata().getTags() != null) {
            Set<String> tags = memory.getMetadata().getTags();
            if (tags.contains("important") || tags.contains("critical")) {
                return 0.9;
            }
            if (tags.contains("high_priority")) {
                return 0.8;
            }
        }
        
        return 0.5; // Default moderate importance
    }
    
    /**
     * Calculates belief score based on whether memory supports beliefs.
     */
    private double calculateBeliefScore(MemoryRecord memory) {
        // Check if this memory is evidence for any beliefs
        if (memory.getMetadata() != null && memory.getMetadata().getTags() != null) {
            Set<String> tags = memory.getMetadata().getTags();
            if (tags.stream().anyMatch(tag -> tag.startsWith("belief_evidence:"))) {
                return 1.0;
            }
        }
        
        // Check category relevance to beliefs
        if (memory.getCategory() != null) {
            String category = memory.getCategory().getPrimary();
            if ("knowledge".equals(category) || "fact".equals(category)) {
                return 0.7;
            }
        }
        
        return 0.3; // Default low belief relevance
    }
    
    /**
     * Applies strategy-specific filtering to identify candidates.
     */
    private List<MemoryRecord> applyStrategyFiltering(List<MemoryRecord> memories, ForgettingStrategy strategy) {
        switch (strategy.getType()) {
            case AGE:
                return filterByAge(memories, strategy);
            case LOW_SCORE:
                return filterByRelevance(memories, strategy);
            case LEAST_USED:
                return filterByAccessFrequency(memories, strategy);
            case CUSTOM:
                return filterByHybrid(memories, strategy);
            default:
                return new ArrayList<>();
        }
    }
    
    /**
     * Filters memories by age.
     */
    private List<MemoryRecord> filterByAge(List<MemoryRecord> memories, ForgettingStrategy strategy) {
        long ageThresholdSeconds = strategy.getMaxAge() != null ? 
            strategy.getMaxAge().getSeconds() : 30 * 24 * 60 * 60; // default 30 days
        
        return memories.stream()
            .filter(memory -> memory.getAgeInSeconds() > ageThresholdSeconds)
            .sorted(Comparator.comparingLong(MemoryRecord::getAgeInSeconds).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Filters memories by relevance score.
     */
    private List<MemoryRecord> filterByRelevance(List<MemoryRecord> memories, ForgettingStrategy strategy) {
        double threshold = strategy.getScoreThreshold() != null ? strategy.getScoreThreshold() : 0.5;
        return memories.stream()
            .filter(memory -> {
                double score = memory.getRelevanceScore() != null ? memory.getRelevanceScore() : 0.0;
                return score < threshold;
            })
            .sorted(Comparator.comparingDouble(memory -> 
                memory.getRelevanceScore() != null ? memory.getRelevanceScore() : 0.0))
            .collect(Collectors.toList());
    }
    
    /**
     * Filters memories by access frequency.
     */
    private List<MemoryRecord> filterByAccessFrequency(List<MemoryRecord> memories, ForgettingStrategy strategy) {
        int retainCount = strategy.getRetainCount() != null ? strategy.getRetainCount() : 1000;
        return memories.stream()
            .sorted(Comparator.comparingInt(MemoryRecord::getAccessCount))
            .skip(retainCount) // Keep the most accessed, remove the rest
            .collect(Collectors.toList());
    }
    
    /**
     * Filters memories using hybrid criteria.
     */
    private List<MemoryRecord> filterByHybrid(List<MemoryRecord> memories, ForgettingStrategy strategy) {
        long ageThreshold = strategy.getMaxAge() != null ? strategy.getMaxAge().getSeconds() : 30 * 24 * 60 * 60;
        double relevanceThreshold = strategy.getScoreThreshold() != null ? strategy.getScoreThreshold() : 0.5;
        int accessThreshold = 3; // default minimum access count
        
        return memories.stream()
            .filter(memory -> {
                // Apply multiple criteria
                boolean oldEnough = memory.getAgeInSeconds() > ageThreshold;
                boolean lowRelevance = (memory.getRelevanceScore() != null ? memory.getRelevanceScore() : 0.0) < relevanceThreshold;
                boolean lowAccess = memory.getAccessCount() < accessThreshold;
                
                // Memory must meet at least 2 of 3 criteria
                int criteriaCount = (oldEnough ? 1 : 0) + (lowRelevance ? 1 : 0) + (lowAccess ? 1 : 0);
                return criteriaCount >= 2;
            })
            .sorted((a, b) -> {
                // Sort by combined score
                double scoreA = calculateCombinedScore(a, ageThreshold, relevanceThreshold, accessThreshold);
                double scoreB = calculateCombinedScore(b, ageThreshold, relevanceThreshold, accessThreshold);
                return Double.compare(scoreA, scoreB);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Calculates combined score for hybrid filtering.
     */
    private double calculateCombinedScore(MemoryRecord memory, long ageThreshold, double relevanceThreshold, int accessThreshold) {
        double ageScore = Math.min(1.0, (double) memory.getAgeInSeconds() / ageThreshold);
        double relevanceScore = 1.0 - (memory.getRelevanceScore() != null ? memory.getRelevanceScore() : 0.0);
        double accessScore = Math.min(1.0, 1.0 - ((double) memory.getAccessCount() / accessThreshold));
        
        return (ageScore + relevanceScore + accessScore) / 3.0;
    }
    
    /**
     * Evaluates a protection rule against a memory.
     */
    private boolean evaluateProtectionRule(MemoryRecord memory, String rule) {
        // Simple rule evaluation (in production, this would be more sophisticated)
        if (rule.contains("metadata.importance >= 8")) {
            return memory.getMetadata() != null && 
                   memory.getMetadata().getImportance() != null && 
                   memory.getMetadata().getImportance() >= 8;
        }
        
        if (rule.contains("age_days <= 1")) {
            return memory.getAgeInSeconds() <= (24 * 60 * 60);
        }
        
        if (rule.contains("access_count >= 10")) {
            return memory.getAccessCount() >= 10;
        }
        
        if (rule.contains("is_belief_evidence = true")) {
            return memory.getMetadata() != null && 
                   memory.getMetadata().getTags() != null &&
                   memory.getMetadata().getTags().stream().anyMatch(tag -> tag.startsWith("belief_evidence:"));
        }
        
        if (rule.contains("metadata.protected = true")) {
            return memory.getMetadata() != null && 
                   memory.getMetadata().getProperties() != null &&
                   Boolean.TRUE.equals(memory.getMetadata().getProperties().get("protected"));
        }
        
        return false;
    }
    
    /**
     * Calculates remaining memory statistics.
     */
    private Map<String, Object> calculateRemainingStats(String agentId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<MemoryRecord> memories;
        if (agentId != null && !agentId.trim().isEmpty()) {
            memories = memorySystem.getMemoriesForAgent(agentId, 0);
        } else {
            Map<String, Object> storageStats = memorySystem.getStorageStatistics();
            @SuppressWarnings("unchecked")
            Map<String, Integer> agentDist = (Map<String, Integer>) storageStats.get("agentDistribution");
            
            memories = new ArrayList<>();
            for (String agent : agentDist.keySet()) {
                memories.addAll(memorySystem.getMemoriesForAgent(agent, 0));
            }
        }
        
        stats.put("totalMemories", memories.size());
        
        double avgRelevance = memories.stream()
            .mapToDouble(memory -> memory.getRelevanceScore() != null ? memory.getRelevanceScore() : 0.0)
            .average()
            .orElse(0.0);
        
        stats.put("averageRelevance", avgRelevance);
        
        return stats;
    }
    
    /**
     * Creates a summary of a memory for archiving.
     */
    private String createMemorySummary(MemoryRecord memory) {
        String content = memory.getContent();
        if (content.length() <= 100) {
            return content;
        }
        return content.substring(0, 97) + "...";
    }
    
    /**
     * Gets relevance bucket for statistics.
     */
    private String getRelevanceBucket(double score) {
        if (score < 0.2) return "very_low";
        if (score < 0.4) return "low";
        if (score < 0.6) return "medium";
        if (score < 0.8) return "high";
        return "very_high";
    }
}