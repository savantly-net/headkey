package ai.headkey.memory.implementations;

import ai.headkey.memory.abstracts.AbstractBeliefReinforcementConflictAnalyzer;
import ai.headkey.memory.interfaces.BeliefExtractionService;
import ai.headkey.memory.interfaces.BeliefStorageService;

import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of the Belief Reinforcement & Conflict Analyzer (BRCA).
 * 
 * This implementation provides rule-based belief management and conflict detection
 * using simple pattern matching and keyword analysis. It maintains beliefs in memory
 * and provides basic conflict resolution strategies.
 * 
 * This refactored version extends the abstract base class and uses pluggable
 * service provider interfaces for content extraction and storage, enabling
 * different implementations to be swapped in without changing the core logic.
 * 
 * Key features:
 * - Extends AbstractBeliefReinforcementConflictAnalyzer for core business logic
 * - Uses SimplePatternBeliefExtractionService for content analysis
 * - Uses InMemoryBeliefStorageService for persistence
 * - Maintains thread-safe operations with atomic counters
 * - Provides configurable conflict resolution strategies
 * 
 * Note: This implementation uses simple heuristics and is designed for development,
 * testing, and demonstration purposes. A production system would likely use more
 * sophisticated NLP and reasoning capabilities via different service implementations.
 * 
 * @since 1.0
 */
public class InMemoryBeliefReinforcementConflictAnalyzer extends AbstractBeliefReinforcementConflictAnalyzer {
    
    private final AtomicLong beliefIdGenerator;
    private final AtomicLong conflictIdGenerator;
    
    /**
     * Creates a new in-memory belief analysis system with default services.
     */
    public InMemoryBeliefReinforcementConflictAnalyzer() {
        this(new SimplePatternBeliefExtractionService(), new InMemoryBeliefStorageService());
    }
    
    /**
     * Creates a new in-memory belief analysis system with custom services.
     * 
     * @param extractionService The service to use for belief extraction
     * @param storageService The service to use for belief storage
     */
    public InMemoryBeliefReinforcementConflictAnalyzer(BeliefExtractionService extractionService, 
                                                      BeliefStorageService storageService) {
        super(extractionService, storageService);
        this.beliefIdGenerator = new AtomicLong(1);
        this.conflictIdGenerator = new AtomicLong(1);
    }
    
    /**
     * Initializes default conflict resolution strategies.
     * 
     * These strategies define how different types of conflicts should be
     * automatically resolved:
     * - Preferences: newer information wins (people's tastes change)
     * - Facts: higher confidence wins (more reliable sources)
     * - Relationships: merge information when possible
     * - Location: newer information wins (people move)
     * - Default: flag for manual review when unsure
     */
    @Override
    protected void initializeDefaultResolutionStrategies() {
        resolutionStrategies.put("preference", "newer_wins");
        resolutionStrategies.put("fact", "higher_confidence");
        resolutionStrategies.put("relationship", "merge");
        resolutionStrategies.put("location", "newer_wins");
        resolutionStrategies.put("general", "flag_for_review");
        resolutionStrategies.put("default", "flag_for_review");
    }
    
    /**
     * Generates a unique belief ID using an atomic counter.
     * 
     * @return A unique belief identifier
     */
    @Override
    protected String generateBeliefId() {
        return "belief-" + beliefIdGenerator.getAndIncrement();
    }
    
    /**
     * Generates a unique conflict ID using an atomic counter.
     * 
     * @return A unique conflict identifier
     */
    @Override
    protected String generateConflictId() {
        return "conflict-" + conflictIdGenerator.getAndIncrement();
    }
    
    /**
     * Gets the current belief ID counter value.
     * Useful for testing and diagnostics.
     * 
     * @return Current belief ID counter value
     */
    public long getCurrentBeliefIdCounter() {
        return beliefIdGenerator.get();
    }
    
    /**
     * Gets the current conflict ID counter value.
     * Useful for testing and diagnostics.
     * 
     * @return Current conflict ID counter value
     */
    public long getCurrentConflictIdCounter() {
        return conflictIdGenerator.get();
    }
    
    /**
     * Resets the ID generators to their initial state.
     * Useful for testing scenarios where predictable IDs are needed.
     * 
     * WARNING: This should not be used in production as it may cause ID conflicts.
     */
    public void resetIdGenerators() {
        beliefIdGenerator.set(1);
        conflictIdGenerator.set(1);
    }
    
    /**
     * Gets information about this specific implementation.
     * 
     * @return Map containing implementation-specific information
     */
    public java.util.Map<String, Object> getImplementationInfo() {
        java.util.Map<String, Object> info = new java.util.HashMap<>();
        
        info.put("implementationType", "InMemoryBeliefReinforcementConflictAnalyzer");
        info.put("version", "2.0");
        info.put("description", "In-memory BRCA implementation with pluggable services");
        info.put("extractionService", extractionService.getServiceInfo());
        info.put("storageService", storageService.getServiceInfo());
        info.put("currentBeliefIdCounter", getCurrentBeliefIdCounter());
        info.put("currentConflictIdCounter", getCurrentConflictIdCounter());
        info.put("resolutionStrategies", new java.util.HashMap<>(resolutionStrategies));
        info.put("startTime", startTime);
        info.put("statistics", getBeliefStatistics());
        
        return info;
    }
    
    /**
     * Performs a comprehensive health check of the system.
     * 
     * This method checks not only the base health status but also
     * validates the internal state of this implementation.
     * 
     * @return Detailed health information
     */
    public java.util.Map<String, Object> getDetailedHealthInfo() {
        java.util.Map<String, Object> health = new java.util.HashMap<>();
        
        health.put("baseHealthy", super.isHealthy());
        health.put("extractionServiceHealthy", extractionService.isHealthy());
        health.put("storageServiceHealthy", storageService.isHealthy());
        health.put("idGeneratorsWorking", testIdGenerators());
        health.put("resolutionStrategiesConfigured", !resolutionStrategies.isEmpty());
        health.put("checkedAt", java.time.Instant.now());
        
        boolean overallHealthy = super.isHealthy() && 
                                testIdGenerators() && 
                                !resolutionStrategies.isEmpty();
        health.put("overallHealthy", overallHealthy);
        
        if (!overallHealthy) {
            java.util.List<String> issues = new java.util.ArrayList<>();
            if (!super.isHealthy()) issues.add("Base system unhealthy");
            if (!testIdGenerators()) issues.add("ID generators not working");
            if (resolutionStrategies.isEmpty()) issues.add("No resolution strategies configured");
            health.put("issues", issues);
        }
        
        return health;
    }
    
    /**
     * Tests whether the ID generators are working correctly.
     * 
     * @return true if ID generators are working, false otherwise
     */
    private boolean testIdGenerators() {
        try {
            // Test belief ID generation
            String beliefId1 = generateBeliefId();
            String beliefId2 = generateBeliefId();
            
            // Test conflict ID generation  
            String conflictId1 = generateConflictId();
            String conflictId2 = generateConflictId();
            
            // Verify IDs are non-null, non-empty, and unique
            return beliefId1 != null && !beliefId1.trim().isEmpty() &&
                   beliefId2 != null && !beliefId2.trim().isEmpty() &&
                   !beliefId1.equals(beliefId2) &&
                   conflictId1 != null && !conflictId1.trim().isEmpty() &&
                   conflictId2 != null && !conflictId2.trim().isEmpty() &&
                   !conflictId1.equals(conflictId2);
        } catch (Exception e) {
            return false;
        }
    }
}