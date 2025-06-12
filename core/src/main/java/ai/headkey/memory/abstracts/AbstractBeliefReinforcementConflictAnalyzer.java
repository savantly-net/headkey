package ai.headkey.memory.abstracts;

import ai.headkey.memory.dto.*;
import ai.headkey.memory.enums.ConflictResolution;
import ai.headkey.memory.exceptions.StorageException;
import ai.headkey.memory.interfaces.BeliefReinforcementConflictAnalyzer;
import ai.headkey.memory.spi.BeliefExtractionService;
import ai.headkey.memory.spi.BeliefStorageService;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Abstract base class for Belief Reinforcement & Conflict Analyzer implementations.
 * 
 * This class separates the core business logic of belief management from persistence
 * and content analysis concerns. It provides a template method pattern for the
 * belief analysis workflow while allowing different implementations for:
 * - Belief storage and persistence
 * - Content analysis and belief extraction
 * - Conflict detection and resolution strategies
 * 
 * Subclasses must implement the abstract methods for persistence operations
 * and content analysis, enabling different approaches from simple in-memory
 * storage with pattern matching to sophisticated AI-powered analysis with
 * persistent storage backends.
 * 
 * @since 1.0
 */
public abstract class AbstractBeliefReinforcementConflictAnalyzer implements BeliefReinforcementConflictAnalyzer {

    // Service dependencies
    protected final BeliefExtractionService extractionService;
    protected final BeliefStorageService storageService;
    
    // Conflict resolution strategies
    protected final Map<String, String> resolutionStrategies;

    // Statistics tracking
    protected long totalAnalyses = 0;
    protected long totalBatchAnalyses = 0;
    protected long totalConflictsDetected = 0;
    protected long totalConflictsResolved = 0;
    protected long totalBeliefsCreated = 0;
    protected long totalBeliefsReinforced = 0;
    protected final Instant startTime;

    public AbstractBeliefReinforcementConflictAnalyzer(BeliefExtractionService extractionService, 
                                                      BeliefStorageService storageService) {
        this.extractionService = Objects.requireNonNull(extractionService, "Extraction service cannot be null");
        this.storageService = Objects.requireNonNull(storageService, "Storage service cannot be null");
        this.resolutionStrategies = new ConcurrentHashMap<>();
        this.startTime = Instant.now();
        
        initializeDefaultResolutionStrategies();
    }

    // ========== Template Methods (Business Logic) ==========

    @Override
    public final BeliefUpdateResult analyzeNewMemory(MemoryRecord newMemory) {
        if (newMemory == null) {
            throw new IllegalArgumentException("Memory record cannot be null");
        }

        try {
            System.out.println("AbstractBeliefReinforcementConflictAnalyzer: Starting analysis for memory: " + newMemory.getId());
            System.out.println("AbstractBeliefReinforcementConflictAnalyzer: Memory content: '" + newMemory.getContent() + "'");
            System.out.println("AbstractBeliefReinforcementConflictAnalyzer: Extraction service: " + extractionService.getClass().getSimpleName());
            
            BeliefUpdateResult result = new BeliefUpdateResult();
            result.setAgentId(newMemory.getAgentId());
            result.setAnalysisTimestamp(Instant.now());

            // Extract potential beliefs from memory content using pluggable strategy
            List<BeliefExtractionService.ExtractedBelief> extractedBeliefs = extractionService.extractBeliefs(
                newMemory.getContent(), newMemory.getAgentId(), newMemory.getCategory());

            System.out.println("AbstractBeliefReinforcementConflictAnalyzer: Received " + extractedBeliefs.size() + " extracted beliefs from extraction service");

            for (BeliefExtractionService.ExtractedBelief extracted : extractedBeliefs) {
                System.out.println("AbstractBeliefReinforcementConflictAnalyzer: Processing extracted belief: " + extracted.getStatement());
                processExtractedBelief(extracted, newMemory, result);
            }

            // If no beliefs were extracted, create a general memory-based belief
            if (extractedBeliefs.isEmpty()) {
                System.out.println("AbstractBeliefReinforcementConflictAnalyzer: No beliefs extracted, creating general belief");
                createGeneralBelief(newMemory, result);
            }

            System.out.println("AbstractBeliefReinforcementConflictAnalyzer: Final result - " + 
                result.getNewBeliefs().size() + " new beliefs, " + 
                result.getReinforcedBeliefs().size() + " reinforced beliefs");

            totalAnalyses++;
            return result;
        } catch (Exception e) {
            System.out.println("AbstractBeliefReinforcementConflictAnalyzer: Analysis failed with exception: " + e.getMessage());
            e.printStackTrace();
            throw new BeliefAnalysisException("Failed to analyze memory: " + e.getMessage(), e);
        }
    }

    @Override
    public final List<BeliefConflict> reviewAllBeliefs() {
        try {
            List<BeliefConflict> conflicts = new ArrayList<>();

            // Group beliefs by agent for efficiency
            Map<String, List<Belief>> beliefsByAgent = storageService.getAllActiveBeliefs().stream()
                .collect(Collectors.groupingBy(Belief::getAgentId));

            for (List<Belief> agentBeliefs : beliefsByAgent.values()) {
                conflicts.addAll(findConflictsInBeliefs(agentBeliefs));
            }

            return conflicts;
        } catch (Exception e) {
            throw new BeliefAnalysisException("Failed to review all beliefs: " + e.getMessage(), e);
        }
    }

    @Override
    public final List<BeliefConflict> reviewBeliefsForAgent(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }

        try {
            List<Belief> agentBeliefs = getBeliefsForAgent(agentId);
            return findConflictsInBeliefs(agentBeliefs);
        } catch (Exception e) {
            throw new BeliefAnalysisException("Failed to review beliefs for agent: " + e.getMessage(), e);
        }
    }

    @Override
    public final BeliefUpdateResult analyzeBatch(List<MemoryRecord> memories) {
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
    public final BeliefConflict resolveConflict(BeliefConflict conflict) {
        if (conflict == null) {
            throw new IllegalArgumentException("Conflict cannot be null");
        }

        try {
            String conflictType = determineConflictType(conflict);
            String strategy = getResolutionStrategy(conflictType);
            
            BeliefConflict resolvedConflict = applyResolutionStrategy(conflict, strategy);
            
            if (resolvedConflict.isResolved()) {
                storageService.removeConflict(conflict.getConflictId());
                totalConflictsResolved++;
            } else {
                storageService.storeConflict(resolvedConflict);
            }
            
            return resolvedConflict;
        } catch (Exception e) {
            throw new BeliefAnalysisException("Failed to resolve conflict: " + e.getMessage(), e);
        }
    }

    @Override
    public final Belief updateBeliefConfidence(String beliefId, double newConfidence, String reason) {
        if (beliefId == null || beliefId.trim().isEmpty()) {
            throw new IllegalArgumentException("Belief ID cannot be null or empty");
        }
        if (newConfidence < 0.0 || newConfidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }

        try {
            Optional<Belief> beliefOpt = storageService.getBeliefById(beliefId);
            if (!beliefOpt.isPresent()) {
                throw new BeliefNotFoundException("Belief not found with ID: " + beliefId, beliefId);
            }

            Belief belief = beliefOpt.get();
            belief.setConfidence(newConfidence);
            belief.setLastUpdated(Instant.now());
            
            return storageService.storeBelief(belief);
        } catch (Exception e) {
            throw new StorageException("Failed to update belief confidence: " + e.getMessage(), e);
        }
    }

    @Override
    public final Belief deactivateBelief(String beliefId, String reason) {
        if (beliefId == null || beliefId.trim().isEmpty()) {
            throw new IllegalArgumentException("Belief ID cannot be null or empty");
        }

        try {
            Optional<Belief> beliefOpt = storageService.getBeliefById(beliefId);
            if (!beliefOpt.isPresent()) {
                throw new BeliefNotFoundException("Belief not found with ID: " + beliefId, beliefId);
            }

            Belief belief = beliefOpt.get();
            belief.deactivate();
            
            return storageService.storeBelief(belief);
        } catch (Exception e) {
            throw new StorageException("Failed to deactivate belief: " + e.getMessage(), e);
        }
    }

    @Override
    public final Map<String, Object> getBeliefStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<Belief> allBeliefs = storageService.getAllBeliefs();
        List<Belief> activeBeliefs = storageService.getAllActiveBeliefs();
        
        stats.put("totalBeliefs", allBeliefs.size());
        stats.put("activeBeliefs", activeBeliefs.size());
        stats.put("inactiveBeliefs", allBeliefs.size() - activeBeliefs.size());
        stats.put("totalAnalyses", totalAnalyses);
        stats.put("totalBatchAnalyses", totalBatchAnalyses);
        stats.put("totalConflictsDetected", totalConflictsDetected);
        stats.put("totalConflictsResolved", totalConflictsResolved);
        stats.put("totalBeliefsCreated", totalBeliefsCreated);
        stats.put("totalBeliefsReinforced", totalBeliefsReinforced);
        stats.put("systemUptime", Instant.now().toEpochMilli() - startTime.toEpochMilli());
        
        // Get distribution data from storage service
        stats.put("confidenceDistribution", storageService.getBeliefDistributionByConfidence(null));
        stats.put("categoryDistribution", storageService.getBeliefDistributionByCategory(null));
        
        // Add storage statistics
        stats.putAll(storageService.getStorageStatistics());
        
        return stats;
    }

    @Override
    public final Map<String, Object> getAgentBeliefStatistics(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }

        Map<String, Object> stats = new HashMap<>();
        List<Belief> activeAgentBeliefs = storageService.getBeliefsForAgent(agentId, false);
        long totalAgentBeliefs = storageService.countBeliefsForAgent(agentId, true);

        stats.put("totalBeliefs", totalAgentBeliefs);
        stats.put("activeBeliefs", activeAgentBeliefs.size());
        stats.put("inactiveBeliefs", totalAgentBeliefs - activeAgentBeliefs.size());

        if (!activeAgentBeliefs.isEmpty()) {
            double avgConfidence = activeAgentBeliefs.stream()
                .mapToDouble(Belief::getConfidence)
                .average()
                .orElse(0.0);
            stats.put("averageConfidence", avgConfidence);

            // High confidence beliefs
            long highConfidenceCount = activeAgentBeliefs.stream()
                .filter(belief -> belief.isHighConfidence(0.8))
                .count();
            stats.put("highConfidenceBeliefs", highConfidenceCount);

            // Category distribution for this agent
            stats.put("categoryDistribution", storageService.getBeliefDistributionByCategory(agentId));
        }

        return stats;
    }

    @Override
    public final List<Belief> findRelatedBeliefs(String queryContent, String agentId, int limit) {
        if (queryContent == null || queryContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Query content cannot be null or empty");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be at least 1");
        }

        List<Belief> candidates = agentId != null ? 
            storageService.getBeliefsForAgent(agentId, false) : storageService.getAllActiveBeliefs();

        return candidates.stream()
            .map(belief -> new BeliefScore(belief, calculateBeliefRelevance(belief, queryContent)))
            .sorted((a, b) -> Double.compare(b.score, a.score))
            .limit(limit)
            .map(scored -> scored.belief)
            .collect(Collectors.toList());
    }

    @Override
    public final List<Belief> getLowConfidenceBeliefs(double confidenceThreshold, String agentId) {
        if (confidenceThreshold < 0.0 || confidenceThreshold > 1.0) {
            throw new IllegalArgumentException("Confidence threshold must be between 0.0 and 1.0");
        }

        List<Belief> candidates = agentId != null ? 
            storageService.getBeliefsForAgent(agentId, false) : storageService.getAllActiveBeliefs();

        return candidates.stream()
            .filter(belief -> belief.getConfidence() < confidenceThreshold)
            .sorted((a, b) -> Double.compare(a.getConfidence(), b.getConfidence()))
            .collect(Collectors.toList());
    }

    // ========== Protected Helper Methods (Business Logic) ==========

    protected final void processExtractedBelief(BeliefExtractionService.ExtractedBelief extracted, MemoryRecord memory, BeliefUpdateResult result) {
        // Look for existing similar beliefs
        List<BeliefStorageService.SimilarBelief> similarBeliefs = storageService.findSimilarBeliefs(
            extracted.getStatement(), memory.getAgentId(), 0.7, 10);

        if (!similarBeliefs.isEmpty()) {
            // Reinforce or conflict with existing beliefs
            for (BeliefStorageService.SimilarBelief similar : similarBeliefs) {
                if (extracted.isPositive()) {
                    reinforceBelief(similar.getBelief(), memory, result);
                } else {
                    detectConflict(similar.getBelief(), extracted, memory, result);
                }
            }
        } else {
            // Create new belief
            if (extracted.isPositive()) {
                createNewBelief(extracted, memory, result);
            }
        }
    }

    protected final void createGeneralBelief(MemoryRecord memory, BeliefUpdateResult result) {
        BeliefExtractionService.ExtractedBelief general = new BeliefExtractionService.ExtractedBelief(
            "General memory: " + memory.getContent(),
            memory.getAgentId(),
            memory.getCategory() != null ? memory.getCategory().getPrimary() : "general",
            0.5,
            true
        );
        createNewBelief(general, memory, result);
    }

    protected final void createNewBelief(BeliefExtractionService.ExtractedBelief extracted, MemoryRecord memory, BeliefUpdateResult result) {
        String beliefId = generateBeliefId();
        Belief newBelief = new Belief(beliefId, extracted.getAgentId(), extracted.getStatement(), extracted.getConfidence());
        newBelief.setCategory(extracted.getCategory());
        newBelief.addEvidence(memory.getId());

        Belief storedBelief = storageService.storeBelief(newBelief);
        result.addNewBelief(storedBelief);
        totalBeliefsCreated++;
    }

    protected final void reinforceBelief(Belief belief, MemoryRecord memory, BeliefUpdateResult result) {
        belief.reinforce(0.1); // Standard reinforcement boost
        belief.addEvidence(memory.getId());
        Belief storedBelief = storageService.storeBelief(belief);
        result.addReinforcedBelief(storedBelief);
        totalBeliefsReinforced++;
    }

    protected final void detectConflict(Belief existingBelief, BeliefExtractionService.ExtractedBelief extracted, MemoryRecord memory, BeliefUpdateResult result) {
        String conflictId = generateConflictId();
        BeliefConflict conflict = new BeliefConflict();
        conflict.setConflictId(conflictId);
        conflict.setAgentId(extracted.getAgentId());
        conflict.setBeliefId(existingBelief.getId());
        conflict.setMemoryId(memory.getId());
        conflict.setDetectedAt(Instant.now());
        conflict.setResolved(false);

        storageService.storeConflict(conflict);
        result.addConflict(conflict);
        totalConflictsDetected++;
    }

    protected final List<BeliefConflict> findConflictsInBeliefs(List<Belief> beliefs) {
        List<BeliefConflict> conflicts = new ArrayList<>();
        
        for (int i = 0; i < beliefs.size(); i++) {
            for (int j = i + 1; j < beliefs.size(); j++) {
                Belief belief1 = beliefs.get(i);
                Belief belief2 = beliefs.get(j);
                
                if (detectConflictBetweenBeliefs(belief1, belief2)) {
                    BeliefConflict conflict = createConflictBetweenBeliefs(belief1, belief2);
                    conflicts.add(conflict);
                }
            }
        }
        
        return conflicts;
    }

    protected final boolean detectConflictBetweenBeliefs(Belief belief1, Belief belief2) {
        return extractionService.areConflicting(
            belief1.getStatement(), 
            belief2.getStatement(), 
            belief1.getCategory(), 
            belief2.getCategory()
        );
    }

    protected final BeliefConflict createConflictBetweenBeliefs(Belief belief1, Belief belief2) {
        String conflictId = generateConflictId();
        BeliefConflict conflict = new BeliefConflict();
        conflict.setConflictId(conflictId);
        conflict.setAgentId(belief1.getAgentId());
        conflict.setBeliefId(belief1.getId());
        conflict.setConflictingBeliefId(belief2.getId());
        conflict.setDetectedAt(Instant.now());
        conflict.setResolved(false);
        return conflict;
    }

    protected final String determineConflictType(BeliefConflict conflict) {
        // Simple heuristic - in a more sophisticated implementation, this could use AI
        String beliefId = conflict.getBeliefId();
        String conflictingBeliefId = conflict.getConflictingBeliefId();
        String memoryId = conflict.getMemoryId();
        
        if (beliefId != null && conflictingBeliefId != null) {
            return "belief_belief";
        } else if (beliefId != null && memoryId != null) {
            return "belief_memory";
        } else {
            return "unknown";
        }
    }

    protected final BeliefConflict applyResolutionStrategy(BeliefConflict conflict, String strategy) {
        switch (strategy.toLowerCase()) {
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

    protected final BeliefConflict resolveWithNewerWins(BeliefConflict conflict) {
        String beliefId1 = conflict.getBeliefId();
        String beliefId2 = conflict.getConflictingBeliefId();
        if (beliefId1 != null && beliefId2 != null) {
            Optional<Belief> belief1Opt = storageService.getBeliefById(beliefId1);
            Optional<Belief> belief2Opt = storageService.getBeliefById(beliefId2);
            
            if (!belief1Opt.isPresent() || !belief2Opt.isPresent()) {
                return conflict;
            }
            
            Belief belief1 = belief1Opt.get();
            Belief belief2 = belief2Opt.get();
            
            if (belief1 != null && belief2 != null) {
                Belief newer = belief1.getCreatedAt().isAfter(belief2.getCreatedAt()) ? belief1 : belief2;
                Belief older = newer == belief1 ? belief2 : belief1;
                
                older.deactivate();
                storageService.storeBelief(older);
                
                conflict.setResolved(true);
                conflict.setResolvedAt(Instant.now());
                conflict.setResolution(ConflictResolution.ARCHIVE_OLD);
                conflict.setResolutionDetails("Kept newer belief: " + newer.getId());
            }
        }
        return conflict;
    }

    protected final BeliefConflict resolveWithHigherConfidence(BeliefConflict conflict) {
        String beliefId1 = conflict.getBeliefId();
        String beliefId2 = conflict.getConflictingBeliefId();
        if (beliefId1 != null && beliefId2 != null) {
            Optional<Belief> belief1Opt = storageService.getBeliefById(beliefId1);
            Optional<Belief> belief2Opt = storageService.getBeliefById(beliefId2);
            
            if (!belief1Opt.isPresent() || !belief2Opt.isPresent()) {
                return conflict;
            }
            
            Belief belief1 = belief1Opt.get();
            Belief belief2 = belief2Opt.get();
            
            if (belief1 != null && belief2 != null) {
                Belief higher = belief1.getConfidence() > belief2.getConfidence() ? belief1 : belief2;
                Belief lower = higher == belief1 ? belief2 : belief1;
                
                lower.deactivate();
                storageService.storeBelief(lower);
                
                conflict.setResolved(true);
                conflict.setResolvedAt(Instant.now());
                conflict.setResolution(ConflictResolution.KEEP_OLD);
                conflict.setResolutionDetails("Kept higher confidence belief: " + higher.getId());
            }
        }
        return conflict;
    }

    protected final BeliefConflict resolveWithMerge(BeliefConflict conflict) {
        // For now, just flag for review. A more sophisticated implementation
        // would actually merge the beliefs
        return flagForReview(conflict);
    }

    protected final BeliefConflict flagForReview(BeliefConflict conflict) {
        conflict.setResolved(false);
        conflict.setResolution(ConflictResolution.REQUIRE_MANUAL_REVIEW);
        conflict.setResolutionDetails("Conflict requires manual review");
        return conflict;
    }

    protected final void mergeBeliefUpdateResults(BeliefUpdateResult target, BeliefUpdateResult source) {
        source.getNewBeliefs().forEach(target::addNewBelief);
        source.getReinforcedBeliefs().forEach(target::addReinforcedBelief);
        source.getConflicts().forEach(target::addConflict);
    }

    protected final double calculateBeliefRelevance(Belief belief, String queryContent) {
        String statement = belief.getStatement().toLowerCase();
        String query = queryContent.toLowerCase();
        
        // Simple relevance calculation - count common words
        String[] statementWords = statement.split("\\s+");
        String[] queryWords = query.split("\\s+");
        
        int commonWords = 0;
        for (String statementWord : statementWords) {
            for (String queryWord : queryWords) {
                if (statementWord.equals(queryWord) && statementWord.length() > 2) {
                    commonWords++;
                }
            }
        }
        
        double baseRelevance = (double) commonWords / Math.max(statementWords.length, queryWords.length);
        
        // Boost by confidence
        return baseRelevance * belief.getConfidence();
    }

    protected final double calculateStatementSimilarity(String statement1, String statement2) {
        return extractionService.calculateSimilarity(statement1, statement2);
    }

    protected final String getConfidenceBucket(double confidence) {
        if (confidence >= 0.8) return "high";
        if (confidence >= 0.5) return "medium";
        return "low";
    }

    // ========== Abstract Methods (Must be implemented by subclasses) ==========

    /**
     * Initializes default conflict resolution strategies.
     */
    protected abstract void initializeDefaultResolutionStrategies();

    /**
     * Gets the resolution strategy for a given conflict type.
     */
    protected final String getResolutionStrategy(String conflictType) {
        return resolutionStrategies.getOrDefault(conflictType, resolutionStrategies.get("default"));
    }

    /**
     * Generates a unique belief ID.
     */
    protected abstract String generateBeliefId();

    /**
     * Generates a unique conflict ID.
     */
    protected abstract String generateConflictId();

    // ========== Implementation Methods ==========

    @Override
    public final List<Belief> getBeliefsForAgent(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }
        return storageService.getBeliefsForAgent(agentId, false);
    }

    @Override
    public final List<Belief> getBeliefsInCategory(String category, String agentId) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be null or empty");
        }
        return storageService.getBeliefsInCategory(category, agentId, false);
    }

    @Override
    public final List<BeliefConflict> getUnresolvedConflicts(String agentId) {
        return storageService.getUnresolvedConflicts(agentId);
    }

    @Override
    public final void configureResolutionStrategies(Map<String, String> strategies) {
        if (strategies == null) {
            throw new IllegalArgumentException("Strategies cannot be null");
        }
        this.resolutionStrategies.clear();
        this.resolutionStrategies.putAll(strategies);
        
        // Ensure default strategy exists
        if (!this.resolutionStrategies.containsKey("default")) {
            this.resolutionStrategies.put("default", "flag_for_review");
        }
    }

    @Override
    public final boolean isHealthy() {
        try {
            return extractionService.isHealthy() && storageService.isHealthy();
        } catch (Exception e) {
            return false;
        }
    }

    // ========== Static Helper Classes ==========

    /**
     * Pairs a belief with a relevance score for ranking.
     */
    protected static class BeliefScore {
        public final Belief belief;
        public final double score;

        public BeliefScore(Belief belief, double score) {
            this.belief = belief;
            this.score = score;
        }
    }
}