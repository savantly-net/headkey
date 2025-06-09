package ai.headkey.rest.config;

import ai.headkey.memory.dto.IngestionResult;
import ai.headkey.memory.dto.MemoryInput;
import ai.headkey.memory.exceptions.InvalidInputException;
import ai.headkey.memory.implementations.InformationIngestionModuleImpl;
import ai.headkey.memory.interfaces.InformationIngestionModule;
import ai.headkey.memory.interfaces.ContextualCategorizationEngine;
import ai.headkey.memory.interfaces.MemoryEncodingSystem;
import ai.headkey.memory.interfaces.BeliefReinforcementConflictAnalyzer;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;

/**
 * Adapter that bridges the REST API with the InformationIngestionModuleImpl.
 * 
 * This adapter delegates all ingestion operations to the InformationIngestionModuleImpl,
 * providing a clean separation between the REST layer and the core business logic.
 * It maintains the same interface contract while leveraging the full ingestion pipeline
 * that includes categorization, belief analysis, and conflict resolution.
 * 
 * The adapter handles:
 * - Delegation to the core ingestion module
 * - REST-specific logging and error handling
 * - Statistics aggregation and monitoring
 * - Health check coordination
 * 
 * This follows the adapter pattern to integrate the core InformationIngestionModuleImpl
 * with the Quarkus REST framework while maintaining clean architecture boundaries.
 */
public class JpaInformationIngestionAdapter implements InformationIngestionModule {
    
    private static final Logger LOG = Logger.getLogger(JpaInformationIngestionAdapter.class);
    
    private final InformationIngestionModuleImpl ingestionModule;
    private final Instant startTime;
    
    /**
     * Creates a new adapter wrapping the provided core components.
     * 
     * @param categorizationEngine The categorization engine
     * @param memorySystem The memory encoding system
     * @param beliefAnalyzer The belief analysis system
     */
    public JpaInformationIngestionAdapter(
            ContextualCategorizationEngine categorizationEngine,
            MemoryEncodingSystem memorySystem,
            BeliefReinforcementConflictAnalyzer beliefAnalyzer) {
        
        this.ingestionModule = new InformationIngestionModuleImpl(
            categorizationEngine, memorySystem, beliefAnalyzer);
        this.startTime = Instant.now();
        LOG.info("JpaInformationIngestionAdapter initialized with InformationIngestionModuleImpl");
    }
    
    @Override
    public IngestionResult ingest(MemoryInput input) throws InvalidInputException {
        LOG.debugf("Delegating ingestion to core module for agent: %s", input.getAgentId());
        
        try {
            IngestionResult result = ingestionModule.ingest(input);
            LOG.infof("Successfully processed ingestion for agent: %s, status: %s", 
                     input.getAgentId(), result.getStatus());
            return result;
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to ingest memory for agent: %s", input.getAgentId());
            
            // Print detailed error information for debugging
            System.err.println("DETAILED ERROR in JpaInformationIngestionAdapter.ingest:");
            System.err.println("Agent ID: " + input.getAgentId());
            System.err.println("Content length: " + (input.getContent() != null ? input.getContent().length() : "null"));
            System.err.println("Exception type: " + e.getClass().getSimpleName());
            System.err.println("Exception message: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Caused by: " + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            }
            e.printStackTrace();
            
            // Let the core module handle error result creation
            throw e;
        }
    }
    
    @Override
    public IngestionResult dryRunIngest(MemoryInput input) throws InvalidInputException {
        LOG.debugf("Delegating dry run to core module for agent: %s", input.getAgentId());
        
        try {
            IngestionResult result = ingestionModule.dryRunIngest(input);
            LOG.infof("Successfully completed dry run for agent: %s", input.getAgentId());
            return result;
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed dry run for agent: %s", input.getAgentId());
            throw e;
        }
    }
    
    @Override
    public void validateInput(MemoryInput input) throws InvalidInputException {
        try {
            ingestionModule.validateInput(input);
            LOG.debugf("Input validation passed for agent: %s", input.getAgentId());
        } catch (Exception e) {
            LOG.errorf(e, "Input validation failed for agent: %s", input.getAgentId());
            throw e;
        }
    }
    
    @Override
    public Map<String, Object> getIngestionStatistics() {
        Map<String, Object> coreStats = ingestionModule.getIngestionStatistics();
        
        // Add adapter-specific statistics
        coreStats.put("adapterStartTime", startTime);
        coreStats.put("adapterUptime", System.currentTimeMillis() - startTime.toEpochMilli());
        
        return coreStats;
    }
    
    @Override
    public boolean isHealthy() {
        try {
            boolean healthy = ingestionModule.isHealthy();
            LOG.debugf("Health check result from core module: %s", healthy);
            return healthy;
            
        } catch (Exception e) {
            LOG.errorf(e, "Health check failed: %s", e.getMessage());
            return false;
        }
    }
}