package ai.headkey.memory.implementations;

import java.util.UUID;

import ai.headkey.memory.abstracts.AbstractBeliefReinforcementConflictAnalyzer;
import ai.headkey.memory.interfaces.BeliefExtractionService;
import ai.headkey.memory.interfaces.BeliefStorageService;

public class StandardBeliefReinforcementConflictAnalyzer
    extends AbstractBeliefReinforcementConflictAnalyzer {

            /**
     * Creates a new belief analysis system with custom services.
     * 
     * @param extractionService The service to use for belief extraction
     * @param storageService The service to use for belief storage
     */
    public StandardBeliefReinforcementConflictAnalyzer(BeliefExtractionService extractionService, 
                                                      BeliefStorageService storageService) {
        super(extractionService, storageService);
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

    @Override
    protected String generateBeliefId() {
        return "belief-" + UUID.randomUUID().toString();
    }

    @Override
    protected String generateConflictId() {
        return "conflict-" + UUID.randomUUID().toString();
    }}
