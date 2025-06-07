package ai.headkey.memory.implementations;

import ai.headkey.memory.dto.*;
import ai.headkey.memory.enums.ConflictResolution;
import ai.headkey.memory.interfaces.BeliefReinforcementConflictAnalyzer;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * In-memory implementation of the Belief Reinforcement & Conflict Analyzer (BRCA).
 * 
 * This implementation provides rule-based belief management and conflict detection
 * using simple pattern matching and keyword analysis. It maintains beliefs in memory
 * and provides basic conflict resolution strategies.
 * 
 * Note: This implementation uses simple heuristics and is designed for development,
 * testing, and demonstration purposes. A production system would likely use more
 * sophisticated NLP and reasoning capabilities.
 * 
 * @since 1.0
 */
public class InMemoryBeliefReinforcementConflictAnalyzer implements BeliefReinforcementConflictAnalyzer {
    
    private final Map<String, Belief> beliefs;
    private final Map<String, Set<String>> agentBeliefs;
    private final Map<String, Set<String>> categoryBeliefs;
    private final Map<String, BeliefConflict> unresolvedConflicts;
    private final Map<String, String> resolutionStrategies;
    private final AtomicLong beliefIdGenerator;
    private final AtomicLong conflictIdGenerator;
    private final Object lock = new Object();
    
    // Pattern matchers for belief extraction
    private final Pattern preferencePattern = Pattern.compile("(?i)(favorite|prefer|like|love|enjoy|hate|dislike)\\s+(.+)");
    private final Pattern factPattern = Pattern.compile("(?i)(.+)\\s+(is|are|was|were)\\s+(.+)");
    private final Pattern relationshipPattern = Pattern.compile("(?i)(.+)\\s+(knows|friend|married|related)\\s+(.+)");
    private final Pattern locationPattern = Pattern.compile("(?i)(.+)\\s+(lives?|located|from)\\s+(.+)");
    private final Pattern negationPattern = Pattern.compile("(?i)(not|never|no|don't|doesn't|isn't|aren't)");
    
    // Statistics tracking
    private long totalAnalyses = 0;
    private long totalBatchAnalyses = 0;
    private long totalConflictsDetected = 0;
    private long totalConflictsResolved = 0;
    private long totalBeliefsCreated = 0;
    private long totalBeliefsReinforced = 0;
    private final Instant startTime;
    
    /**
     * Creates a new in-memory belief analysis system.
     */
    public InMemoryBeliefReinforcementConflictAnalyzer() {
        this.beliefs = new ConcurrentHashMap<>();
        this.agentBeliefs = new ConcurrentHashMap<>();
        this.categoryBeliefs = new ConcurrentHashMap<>();
        this.unresolvedConflicts = new ConcurrentHashMap<>();
        this.resolutionStrategies = new ConcurrentHashMap<>();
        this.beliefIdGenerator = new AtomicLong(1);
        this.conflictIdGenerator = new AtomicLong(1);
        this.startTime = Instant.now();
        
        initializeDefaultResolutionStrategies();
    }
    
    /**
     * Initializes default conflict resolution strategies.
     */
    private void initializeDefaultResolutionStrategies() {
        resolutionStrategies.put("preference", "newer_wins");
        resolutionStrategies.put("fact", "higher_confidence");
        resolutionStrategies.put("relationship", "merge");
        resolutionStrategies.put("location", "newer_wins");
        resolutionStrategies.put("default", "flag_for_review");
    }
    
    @Override
    public BeliefUpdateResult analyzeNewMemory(MemoryRecord newMemory) {
        if (newMemory == null) {
            throw new IllegalArgumentException("Memory record cannot be null");
        }
        
        try {
            synchronized (lock) {
                BeliefUpdateResult result = new BeliefUpdateResult();
                result.setAgentId(newMemory.getAgentId());
                result.setAnalysisTimestamp(Instant.now());
                
                // Extract potential beliefs from memory content
                List<ExtractedBelief> extractedBeliefs = extractBeliefsFromContent(
                    newMemory.getContent(), newMemory.getAgentId(), newMemory.getCategory());
                
                for (ExtractedBelief extracted : extractedBeliefs) {
                    processExtractedBelief(extracted, newMemory, result);
                }
                
                // If no beliefs were extracted, create a general memory-based belief
                if (extractedBeliefs.isEmpty()) {
                    createGeneralBelief(newMemory, result);
                }
                
                totalAnalyses++;
                return result;
            }
        } catch (Exception e) {
            throw new BeliefAnalysisException("Failed to analyze memory: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<BeliefConflict> reviewAllBeliefs() {
        try {
            synchronized (lock) {
                List<BeliefConflict> conflicts = new ArrayList<>();
                
                // Group beliefs by agent for efficiency
                Map<String, List<Belief>> beliefsByAgent = beliefs.values().stream()
                    .filter(Belief::isActive)
                    .collect(Collectors.groupingBy(Belief::getAgentId));
                
                for (List<Belief> agentBeliefs : beliefsByAgent.values()) {
                    conflicts.addAll(findConflictsInBeliefs(agentBeliefs));
                }
                
                return conflicts;
            }
        } catch (Exception e) {
            throw new BeliefAnalysisException("Failed to review all beliefs: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<BeliefConflict> reviewBeliefsForAgent(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }
        
        try {
            Set<String> agentBeliefIds = agentBeliefs.get(agentId);
            if (agentBeliefIds == null || agentBeliefIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<Belief> agentBeliefList = agentBeliefIds.stream()
                .map(beliefs::get)
                .filter(Objects::nonNull)
                .filter(Belief::isActive)
                .collect(Collectors.toList());
            
            return findConflictsInBeliefs(agentBeliefList);
        } catch (Exception e) {
            throw new BeliefAnalysisException("Failed to review beliefs for agent: " + e.getMessage(), e);
        }
    }
    
    @Override
    public BeliefUpdateResult analyzeBatch(List<MemoryRecord> memories) {
        if (memories == null || memories.isEmpty()) {
            throw new IllegalArgumentException("Memories list cannot be null or empty");
        }
        
        try {
            BeliefUpdateResult combinedResult = new BeliefUpdateResult();
            combinedResult.setAnalysisTimestamp(Instant.now());
            
            for (MemoryRecord memory : memories) {
                BeliefUpdateResult memoryResult = analyzeNewMemory(memory);
                mergeBeliefUpdateResults(combinedResult, memoryResult);
            }
            
            totalBatchAnalyses++;
            return combinedResult;
        } catch (Exception e) {
            throw new BeliefAnalysisException("Failed to analyze batch: " + e.getMessage(), e);
        }
    }
    
    @Override
    public BeliefConflict resolveConflict(BeliefConflict conflict) {
        if (conflict == null) {
            throw new IllegalArgumentException("Conflict cannot be null");
        }
        
        try {
            synchronized (lock) {
                String strategy = getResolutionStrategy(conflict);
                BeliefConflict resolved = applyResolutionStrategy(conflict, strategy);
                
                if (resolved.getResolution() != ConflictResolution.REQUIRE_MANUAL_REVIEW) {
                    unresolvedConflicts.remove(conflict.getConflictId());
                    totalConflictsResolved++;
                }
                
                return resolved;
            }
        } catch (Exception e) {
            throw new BeliefAnalysisException("Failed to resolve conflict: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<Belief> getBeliefsForAgent(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }
        
        Set<String> agentBeliefIds = agentBeliefs.get(agentId);
        if (agentBeliefIds == null || agentBeliefIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        return agentBeliefIds.stream()
            .map(beliefs::get)
            .filter(Objects::nonNull)
            .filter(Belief::isActive)
            .sorted((a, b) -> b.getLastUpdated().compareTo(a.getLastUpdated()))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Belief> getBeliefsInCategory(String category, String agentId) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be null or empty");
        }
        
        Set<String> categoryBeliefIds = categoryBeliefs.get(category);
        if (categoryBeliefIds == null || categoryBeliefIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        return categoryBeliefIds.stream()
            .map(beliefs::get)
            .filter(Objects::nonNull)
            .filter(Belief::isActive)
            .filter(belief -> agentId == null || agentId.equals(belief.getAgentId()))
            .sorted((a, b) -> b.getLastUpdated().compareTo(a.getLastUpdated()))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Belief> findRelatedBeliefs(String queryContent, String agentId, int limit) {
        if (queryContent == null || queryContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Query content cannot be null or empty");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be at least 1");
        }
        
        String query = queryContent.toLowerCase().trim();
        List<BeliefScore> scoredBeliefs = new ArrayList<>();
        
        for (Belief belief : beliefs.values()) {
            if (!belief.isActive()) continue;
            if (agentId != null && !agentId.equals(belief.getAgentId())) continue;
            
            double score = calculateBeliefRelevance(belief, query);
            if (score > 0.1) {
                scoredBeliefs.add(new BeliefScore(belief, score));
            }
        }
        
        return scoredBeliefs.stream()
            .sorted((a, b) -> Double.compare(b.score, a.score))
            .limit(limit)
            .map(bs -> bs.belief)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<BeliefConflict> getUnresolvedConflicts(String agentId) {
        return unresolvedConflicts.values().stream()
            .filter(conflict -> agentId == null || agentId.equals(conflict.getAgentId()))
            .sorted((a, b) -> b.getDetectedAt().compareTo(a.getDetectedAt()))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Belief> getLowConfidenceBeliefs(double confidenceThreshold, String agentId) {
        if (confidenceThreshold < 0.0 || confidenceThreshold > 1.0) {
            throw new IllegalArgumentException("Confidence threshold must be between 0.0 and 1.0");
        }
        
        return beliefs.values().stream()
            .filter(Belief::isActive)
            .filter(belief -> belief.getConfidence() < confidenceThreshold)
            .filter(belief -> agentId == null || agentId.equals(belief.getAgentId()))
            .sorted(Comparator.comparingDouble(Belief::getConfidence))
            .collect(Collectors.toList());
    }
    
    @Override
    public Belief updateBeliefConfidence(String beliefId, double newConfidence, String reason) {
        if (beliefId == null || beliefId.trim().isEmpty()) {
            throw new IllegalArgumentException("Belief ID cannot be null or empty");
        }
        if (newConfidence < 0.0 || newConfidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
        
        synchronized (lock) {
            Belief belief = beliefs.get(beliefId);
            if (belief == null) {
                throw new IllegalArgumentException("Belief not found: " + beliefId);
            }
            
            belief.setConfidence(newConfidence);
            belief.setLastUpdated(Instant.now());
            
            if (reason != null && !reason.trim().isEmpty()) {
                belief.addTag("confidence_update:" + reason);
            }
            
            return belief;
        }
    }
    
    @Override
    public Belief deactivateBelief(String beliefId, String reason) {
        if (beliefId == null || beliefId.trim().isEmpty()) {
            throw new IllegalArgumentException("Belief ID cannot be null or empty");
        }
        
        synchronized (lock) {
            Belief belief = beliefs.get(beliefId);
            if (belief == null) {
                throw new IllegalArgumentException("Belief not found: " + beliefId);
            }
            
            belief.deactivate();
            
            if (reason != null && !reason.trim().isEmpty()) {
                belief.addTag("deactivated:" + reason);
            }
            
            return belief;
        }
    }
    
    @Override
    public Map<String, Object> getBeliefStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long activeBeliefs = beliefs.values().stream().mapToLong(b -> b.isActive() ? 1 : 0).sum();
        long inactiveBeliefs = beliefs.size() - activeBeliefs;
        
        stats.put("totalBeliefs", beliefs.size());
        stats.put("activeBeliefs", activeBeliefs);
        stats.put("inactiveBeliefs", inactiveBeliefs);
        stats.put("totalAgents", agentBeliefs.size());
        stats.put("totalCategories", categoryBeliefs.size());
        stats.put("totalAnalyses", totalAnalyses);
        stats.put("totalBatchAnalyses", totalBatchAnalyses);
        stats.put("totalConflictsDetected", totalConflictsDetected);
        stats.put("totalConflictsResolved", totalConflictsResolved);
        stats.put("totalBeliefsCreated", totalBeliefsCreated);
        stats.put("totalBeliefsReinforced", totalBeliefsReinforced);
        stats.put("unresolvedConflicts", unresolvedConflicts.size());
        stats.put("uptime", Instant.now().getEpochSecond() - startTime.getEpochSecond());
        
        // Confidence distribution
        Map<String, Long> confidenceDistribution = new HashMap<>();
        for (Belief belief : beliefs.values()) {
            if (belief.isActive()) {
                String bucket = getConfidenceBucket(belief.getConfidence());
                confidenceDistribution.merge(bucket, 1L, Long::sum);
            }
        }
        stats.put("confidenceDistribution", confidenceDistribution);
        
        // Category distribution
        Map<String, Integer> categoryDistribution = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : categoryBeliefs.entrySet()) {
            categoryDistribution.put(entry.getKey(), entry.getValue().size());
        }
        stats.put("categoryDistribution", categoryDistribution);
        
        return stats;
    }
    
    @Override
    public Map<String, Object> getAgentBeliefStatistics(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }
        
        Set<String> agentBeliefIds = agentBeliefs.get(agentId);
        Map<String, Object> stats = new HashMap<>();
        
        if (agentBeliefIds == null || agentBeliefIds.isEmpty()) {
            stats.put("totalBeliefs", 0);
            stats.put("activeBeliefs", 0);
            stats.put("averageConfidence", 0.0);
            stats.put("categories", new HashMap<String, Integer>());
            return stats;
        }
        
        List<Belief> agentBeliefList = agentBeliefIds.stream()
            .map(beliefs::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        long activeCount = agentBeliefList.stream().mapToLong(b -> b.isActive() ? 1 : 0).sum();
        double avgConfidence = agentBeliefList.stream()
            .filter(Belief::isActive)
            .mapToDouble(Belief::getConfidence)
            .average()
            .orElse(0.0);
        
        // Category breakdown
        Map<String, Integer> categories = new HashMap<>();
        for (Belief belief : agentBeliefList) {
            if (belief.isActive() && belief.getCategory() != null) {
                categories.merge(belief.getCategory(), 1, Integer::sum);
            }
        }
        
        stats.put("totalBeliefs", agentBeliefList.size());
        stats.put("activeBeliefs", activeCount);
        stats.put("inactiveBeliefs", agentBeliefList.size() - activeCount);
        stats.put("averageConfidence", avgConfidence);
        stats.put("categories", categories);
        
        return stats;
    }
    
    @Override
    public void configureResolutionStrategies(Map<String, String> strategies) {
        if (strategies == null) {
            throw new IllegalArgumentException("Strategies cannot be null");
        }
        
        synchronized (lock) {
            resolutionStrategies.putAll(strategies);
        }
    }
    
    @Override
    public boolean isHealthy() {
        try {
            return beliefs != null && agentBeliefs != null && categoryBeliefs != null &&
                   unresolvedConflicts != null && resolutionStrategies != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Extracts potential beliefs from memory content.
     */
    private List<ExtractedBelief> extractBeliefsFromContent(String content, String agentId, CategoryLabel category) {
        List<ExtractedBelief> beliefs = new ArrayList<>();
        String normalizedContent = content.toLowerCase().trim();
        
        // Extract preferences
        extractPreferences(normalizedContent, agentId, beliefs);
        
        // Extract facts
        extractFacts(normalizedContent, agentId, beliefs);
        
        // Extract relationships
        extractRelationships(normalizedContent, agentId, beliefs);
        
        // Extract locations
        extractLocations(normalizedContent, agentId, beliefs);
        
        // Set category for all extracted beliefs
        String beliefCategory = category != null ? category.getPrimary() : "general";
        for (ExtractedBelief belief : beliefs) {
            belief.category = beliefCategory;
        }
        
        return beliefs;
    }
    
    /**
     * Extracts preference-based beliefs.
     */
    private void extractPreferences(String content, String agentId, List<ExtractedBelief> beliefs) {
        if (preferencePattern.matcher(content).find()) {
            // Simple preference extraction
            if (content.contains("favorite")) {
                int favoriteIdx = content.indexOf("favorite");
                String remainder = content.substring(favoriteIdx);
                String statement = "User has preference: " + remainder;
                beliefs.add(new ExtractedBelief(statement, agentId, "preference", 0.7, !negationPattern.matcher(content).find()));
            }
        }
    }
    
    /**
     * Extracts fact-based beliefs.
     */
    private void extractFacts(String content, String agentId, List<ExtractedBelief> beliefs) {
        if (factPattern.matcher(content).find()) {
            // Extract statements that look like facts
            if (content.contains(" is ") || content.contains(" are ") || content.contains(" was ") || content.contains(" were ")) {
                String statement = "Fact: " + content;
                beliefs.add(new ExtractedBelief(statement, agentId, "fact", 0.6, !negationPattern.matcher(content).find()));
            }
        }
    }
    
    /**
     * Extracts relationship-based beliefs.
     */
    private void extractRelationships(String content, String agentId, List<ExtractedBelief> beliefs) {
        if (relationshipPattern.matcher(content).find()) {
            if (content.contains("friend") || content.contains("knows") || content.contains("married") || content.contains("related")) {
                String statement = "Relationship: " + content;
                beliefs.add(new ExtractedBelief(statement, agentId, "relationship", 0.7, !negationPattern.matcher(content).find()));
            }
        }
    }
    
    /**
     * Extracts location-based beliefs.
     */
    private void extractLocations(String content, String agentId, List<ExtractedBelief> beliefs) {
        if (locationPattern.matcher(content).find()) {
            if (content.contains("lives") || content.contains("located") || content.contains("from")) {
                String statement = "Location: " + content;
                beliefs.add(new ExtractedBelief(statement, agentId, "location", 0.6, !negationPattern.matcher(content).find()));
            }
        }
    }
    
    /**
     * Processes an extracted belief and updates the belief system.
     */
    private void processExtractedBelief(ExtractedBelief extracted, MemoryRecord memory, BeliefUpdateResult result) {
        // Look for existing similar beliefs
        List<Belief> similarBeliefs = findSimilarBeliefs(extracted, memory.getAgentId());
        
        if (!similarBeliefs.isEmpty()) {
            // Reinforce or conflict with existing beliefs
            for (Belief similar : similarBeliefs) {
                if (extracted.positive) {
                    reinforceBelief(similar, memory, result);
                } else {
                    detectConflict(similar, extracted, memory, result);
                }
            }
        } else {
            // Create new belief
            if (extracted.positive) {
                createNewBelief(extracted, memory, result);
            }
        }
    }
    
    /**
     * Creates a general belief for memories that don't extract specific beliefs.
     */
    private void createGeneralBelief(MemoryRecord memory, BeliefUpdateResult result) {
        String beliefId = "belief_" + beliefIdGenerator.getAndIncrement();
        String statement = "Memory content: " + memory.getContent().substring(0, Math.min(100, memory.getContent().length()));
        
        Belief belief = new Belief(beliefId, memory.getAgentId(), statement, 0.3);
        belief.setCategory("memory");
        belief.addEvidence(memory.getId());
        
        storeBelief(belief);
        result.getNewBeliefs().add(belief);
        totalBeliefsCreated++;
    }
    
    /**
     * Creates a new belief from extracted information.
     */
    private void createNewBelief(ExtractedBelief extracted, MemoryRecord memory, BeliefUpdateResult result) {
        String beliefId = "belief_" + beliefIdGenerator.getAndIncrement();
        
        Belief belief = new Belief(beliefId, extracted.agentId, extracted.statement, extracted.confidence);
        belief.setCategory(extracted.category);
        belief.addEvidence(memory.getId());
        
        storeBelief(belief);
        result.getNewBeliefs().add(belief);
        totalBeliefsCreated++;
    }
    
    /**
     * Reinforces an existing belief with new evidence.
     */
    private void reinforceBelief(Belief belief, MemoryRecord memory, BeliefUpdateResult result) {
        belief.addEvidence(memory.getId());
        belief.reinforce(0.1); // Small confidence boost
        result.addReinforcedBelief(belief);
        totalBeliefsReinforced++;
    }
    
    /**
     * Detects and handles conflicts between beliefs.
     */
    private void detectConflict(Belief existingBelief, ExtractedBelief newBelief, MemoryRecord memory, BeliefUpdateResult result) {
        String conflictId = "conflict_" + conflictIdGenerator.getAndIncrement();
        
        BeliefConflict conflict = new BeliefConflict();
        conflict.setConflictId(conflictId);
        conflict.setAgentId(existingBelief.getAgentId());
        conflict.setBeliefId(existingBelief.getId());
        conflict.setMemoryId(memory.getId());
        conflict.setDescription("Conflicting belief detected: " + newBelief.statement);
        conflict.setDetectedAt(Instant.now());
        conflict.setResolution(ConflictResolution.REQUIRE_MANUAL_REVIEW);
        
        unresolvedConflicts.put(conflictId, conflict);
        result.addConflict(conflict);
        totalConflictsDetected++;
    }
    
    /**
     * Stores a belief and updates indexes.
     */
    private void storeBelief(Belief belief) {
        beliefs.put(belief.getId(), belief);
        agentBeliefs.computeIfAbsent(belief.getAgentId(), k -> ConcurrentHashMap.newKeySet()).add(belief.getId());
        if (belief.getCategory() != null) {
            categoryBeliefs.computeIfAbsent(belief.getCategory(), k -> ConcurrentHashMap.newKeySet()).add(belief.getId());
        }
    }
    
    /**
     * Finds similar beliefs to a new extracted belief.
     */
    private List<Belief> findSimilarBeliefs(ExtractedBelief extracted, String agentId) {
        return beliefs.values().stream()
            .filter(Belief::isActive)
            .filter(belief -> agentId.equals(belief.getAgentId()))
            .filter(belief -> calculateSimilarity(belief.getStatement(), extracted.statement) > 0.7)
            .collect(Collectors.toList());
    }
    
    /**
     * Calculates similarity between two statements (simple approach).
     */
    private double calculateSimilarity(String statement1, String statement2) {
        String[] words1 = statement1.toLowerCase().split("\\s+");
        String[] words2 = statement2.toLowerCase().split("\\s+");
        
        Set<String> set1 = new HashSet<>(Arrays.asList(words1));
        Set<String> set2 = new HashSet<>(Arrays.asList(words2));
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * Finds conflicts within a list of beliefs.
     */
    private List<BeliefConflict> findConflictsInBeliefs(List<Belief> beliefList) {
        List<BeliefConflict> conflicts = new ArrayList<>();
        
        for (int i = 0; i < beliefList.size(); i++) {
            for (int j = i + 1; j < beliefList.size(); j++) {
                Belief belief1 = beliefList.get(i);
                Belief belief2 = beliefList.get(j);
                
                if (detectConflictBetweenBeliefs(belief1, belief2)) {
                    BeliefConflict conflict = createConflictBetweenBeliefs(belief1, belief2);
                    conflicts.add(conflict);
                }
            }
        }
        
        return conflicts;
    }
    
    /**
     * Detects if two beliefs conflict with each other.
     */
    private boolean detectConflictBetweenBeliefs(Belief belief1, Belief belief2) {
        // Simple conflict detection based on contradictory statements
        String statement1 = belief1.getStatement().toLowerCase();
        String statement2 = belief2.getStatement().toLowerCase();
        
        // Check for direct negations
        if ((statement1.contains("not") && !statement2.contains("not")) ||
            (!statement1.contains("not") && statement2.contains("not"))) {
            return calculateSimilarity(statement1.replace("not", ""), statement2.replace("not", "")) > 0.6;
        }
        
        return false;
    }
    
    /**
     * Creates a conflict record between two beliefs.
     */
    private BeliefConflict createConflictBetweenBeliefs(Belief belief1, Belief belief2) {
        String conflictId = "conflict_" + conflictIdGenerator.getAndIncrement();
        
        BeliefConflict conflict = new BeliefConflict();
        conflict.setConflictId(conflictId);
        conflict.setAgentId(belief1.getAgentId());
        conflict.setBeliefId(belief1.getId());
        conflict.setConflictingBeliefId(belief2.getId());
        conflict.setDescription("Conflicting beliefs detected between: " + belief1.getStatement() + " and " + belief2.getStatement());
        conflict.setDetectedAt(Instant.now());
        conflict.setResolution(ConflictResolution.REQUIRE_MANUAL_REVIEW);
        
        return conflict;
    }
    
    /**
     * Gets the resolution strategy for a conflict.
     */
    private String getResolutionStrategy(BeliefConflict conflict) {
        // Determine conflict type and get appropriate strategy
        String conflictType = determineConflictType(conflict);
        return resolutionStrategies.getOrDefault(conflictType, resolutionStrategies.get("default"));
    }
    
    /**
     * Determines the type of conflict.
     */
    private String determineConflictType(BeliefConflict conflict) {
        String description = conflict.getDescription().toLowerCase();
        
        if (description.contains("preference")) return "preference";
        if (description.contains("fact")) return "fact";
        if (description.contains("relationship")) return "relationship";
        if (description.contains("location")) return "location";
        
        return "default";
    }
    
    /**
     * Applies a resolution strategy to a conflict.
     */
    private BeliefConflict applyResolutionStrategy(BeliefConflict conflict, String strategy) {
        switch (strategy) {
            case "newer_wins":
                return resolveWithNewerWins(conflict);
            case "higher_confidence":
                return resolveWithHigherConfidence(conflict);
            case "merge":
                return resolveWithMerge(conflict);
            case "flag_for_review":
            default:
                return flagForReview(conflict);
        }
    }
    
    /**
     * Resolves conflict by choosing the newer belief.
     */
    private BeliefConflict resolveWithNewerWins(BeliefConflict conflict) {
        if (conflict.getConflictingBeliefId() != null) {
            Belief belief1 = beliefs.get(conflict.getBeliefId());
            Belief belief2 = beliefs.get(conflict.getConflictingBeliefId());
            
            if (belief1 != null && belief2 != null) {
                if (belief2.getLastUpdated().isAfter(belief1.getLastUpdated())) {
                    belief1.deactivate();
                    conflict.setResolution(ConflictResolution.TAKE_NEW);
                    conflict.setResolvedAt(Instant.now());
                } else {
                    belief2.deactivate();
                    conflict.setResolution(ConflictResolution.TAKE_NEW);
                    conflict.setResolvedAt(Instant.now());
                }
            }
        }
        return conflict;
    }
    
    /**
     * Resolves conflict by choosing the belief with higher confidence.
     */
    private BeliefConflict resolveWithHigherConfidence(BeliefConflict conflict) {
        if (conflict.getConflictingBeliefId() != null) {
            Belief belief1 = beliefs.get(conflict.getBeliefId());
            Belief belief2 = beliefs.get(conflict.getConflictingBeliefId());
            
            if (belief1 != null && belief2 != null) {
                if (belief2.getConfidence() > belief1.getConfidence()) {
                    belief1.deactivate();
                    conflict.setResolution(ConflictResolution.KEEP_OLD);
                    conflict.setResolvedAt(Instant.now());
                } else {
                    belief2.deactivate();
                    conflict.setResolution(ConflictResolution.KEEP_OLD);
                    conflict.setResolvedAt(Instant.now());
                }
            }
        }
        return conflict;
    }
    
    /**
     * Resolves conflict by merging beliefs.
     */
    private BeliefConflict resolveWithMerge(BeliefConflict conflict) {
        if (conflict.getConflictingBeliefId() != null) {
            Belief belief1 = beliefs.get(conflict.getBeliefId());
            Belief belief2 = beliefs.get(conflict.getConflictingBeliefId());
            
            if (belief1 != null && belief2 != null) {
                // Create merged belief
                String mergedStatement = "Merged: " + belief1.getStatement() + " | " + belief2.getStatement();
                double mergedConfidence = (belief1.getConfidence() + belief2.getConfidence()) / 2;
                
                belief1.setStatement(mergedStatement);
                belief1.setConfidence(mergedConfidence);
                belief1.getEvidenceMemoryIds().addAll(belief2.getEvidenceMemoryIds());
                belief1.setLastUpdated(Instant.now());
                
                belief2.deactivate();
                conflict.setResolution(ConflictResolution.MERGE);
                conflict.setResolvedAt(Instant.now());
            }
        }
        return conflict;
    }
    
    /**
     * Flags conflict for manual review.
     */
    private BeliefConflict flagForReview(BeliefConflict conflict) {
        conflict.setResolution(ConflictResolution.REQUIRE_MANUAL_REVIEW);
        conflict.setResolvedAt(Instant.now());
        return conflict;
    }
    
    /**
     * Merges two belief update results.
     */
    private void mergeBeliefUpdateResults(BeliefUpdateResult target, BeliefUpdateResult source) {
        target.getNewBeliefs().addAll(source.getNewBeliefs());
        target.getReinforcedBeliefs().addAll(source.getReinforcedBeliefs());
        target.getConflicts().addAll(source.getConflicts());
    }
    
    /**
     * Calculates belief relevance to a query.
     */
    private double calculateBeliefRelevance(Belief belief, String query) {
        String statement = belief.getStatement().toLowerCase();
        String[] queryWords = query.split("\\s+");
        int matches = 0;
        
        for (String word : queryWords) {
            if (statement.contains(word.toLowerCase())) {
                matches++;
            }
        }
        
        double baseScore = (double) matches / queryWords.length;
        
        // Boost based on confidence and evidence count
        double confidenceBoost = belief.getConfidence() * 0.2;
        double evidenceBoost = Math.min(0.2, belief.getEvidenceCount() * 0.05);
        
        return Math.min(1.0, baseScore + confidenceBoost + evidenceBoost);
    }
    
    /**
     * Gets confidence bucket for statistics.
     */
    private String getConfidenceBucket(double confidence) {
        if (confidence < 0.2) return "very_low";
        if (confidence < 0.4) return "low";
        if (confidence < 0.6) return "medium";
        if (confidence < 0.8) return "high";
        return "very_high";
    }
    
    /**
     * Helper class for extracted beliefs.
     */
    private static class ExtractedBelief {
        final String statement;
        final String agentId;
        String category;
        final double confidence;
        final boolean positive;
        
        ExtractedBelief(String statement, String agentId, String category, double confidence, boolean positive) {
            this.statement = statement;
            this.agentId = agentId;
            this.category = category;
            this.confidence = confidence;
            this.positive = positive;
        }
    }
    
    /**
     * Helper class for belief scoring.
     */
    private static class BeliefScore {
        final Belief belief;
        final double score;
        
        BeliefScore(Belief belief, double score) {
            this.belief = belief;
            this.score = score;
        }
    }
}