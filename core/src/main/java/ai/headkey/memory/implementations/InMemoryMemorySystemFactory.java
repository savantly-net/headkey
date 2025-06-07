package ai.headkey.memory.implementations;

import ai.headkey.memory.interfaces.*;

/**
 * Factory class for creating and wiring up in-memory implementations
 * of all memory system components.
 * 
 * This factory provides a convenient way to create a complete, working
 * memory system using the in-memory implementations. It handles the
 * dependency injection and configuration of all components.
 * 
 * The factory follows the Builder pattern to allow for flexible
 * configuration of the memory system components.
 * 
 * @since 1.0
 */
public class InMemoryMemorySystemFactory {
    
    private ContextualCategorizationEngine categorizationEngine;
    private MemoryEncodingSystem memoryEncodingSystem;
    private BeliefReinforcementConflictAnalyzer beliefAnalyzer;
    private RelevanceEvaluationForgettingAgent forgettingAgent;
    private RetrievalResponseEngine retrievalEngine;
    private InformationIngestionModule ingestionModule;
    
    /**
     * Creates a new factory instance.
     */
    public InMemoryMemorySystemFactory() {
        // Constructor is intentionally empty - components are created lazily
    }
    
    /**
     * Creates a complete memory system with all components using default configurations.
     * 
     * @return A fully configured MemorySystem instance
     */
    public MemorySystem createCompleteSystem() {
        return new MemorySystemBuilder()
            .withCategorizationEngine(createCategorizationEngine())
            .withMemoryEncodingSystem(createMemoryEncodingSystem())
            .withBeliefAnalyzer(createBeliefAnalyzer())
            .withForgettingAgent(createForgettingAgent())
            .withRetrievalEngine(createRetrievalEngine())
            .withIngestionModule(createIngestionModule())
            .build();
    }
    
    /**
     * Creates and returns a contextualized categorization engine.
     * 
     * @return An instance of InMemoryContextualCategorizationEngine
     */
    public ContextualCategorizationEngine createCategorizationEngine() {
        if (categorizationEngine == null) {
            categorizationEngine = new InMemoryContextualCategorizationEngine();
        }
        return categorizationEngine;
    }
    
    /**
     * Creates and returns a memory encoding system.
     * 
     * @return An instance of InMemoryMemoryEncodingSystem
     */
    public MemoryEncodingSystem createMemoryEncodingSystem() {
        if (memoryEncodingSystem == null) {
            memoryEncodingSystem = new InMemoryMemoryEncodingSystem();
        }
        return memoryEncodingSystem;
    }
    
    /**
     * Creates and returns a belief reinforcement conflict analyzer.
     * 
     * @return An instance of InMemoryBeliefReinforcementConflictAnalyzer
     */
    public BeliefReinforcementConflictAnalyzer createBeliefAnalyzer() {
        if (beliefAnalyzer == null) {
            beliefAnalyzer = new InMemoryBeliefReinforcementConflictAnalyzer();
        }
        return beliefAnalyzer;
    }
    
    /**
     * Creates and returns a relevance evaluation forgetting agent.
     * 
     * @return An instance of InMemoryRelevanceEvaluationForgettingAgent
     */
    public RelevanceEvaluationForgettingAgent createForgettingAgent() {
        if (forgettingAgent == null) {
            // The forgetting agent needs the memory encoding system
            MemoryEncodingSystem mes = createMemoryEncodingSystem();
            forgettingAgent = new InMemoryRelevanceEvaluationForgettingAgent(mes);
        }
        return forgettingAgent;
    }
    
    /**
     * Creates and returns a retrieval response engine.
     * 
     * @return An instance of InMemoryRetrievalResponseEngine
     */
    public RetrievalResponseEngine createRetrievalEngine() {
        if (retrievalEngine == null) {
            // The retrieval engine needs the memory encoding system
            MemoryEncodingSystem mes = createMemoryEncodingSystem();
            retrievalEngine = new InMemoryRetrievalResponseEngine(mes);
        }
        return retrievalEngine;
    }
    
    /**
     * Creates and returns an information ingestion module.
     * 
     * @return An instance of InMemoryInformationIngestionModule
     */
    public InformationIngestionModule createIngestionModule() {
        if (ingestionModule == null) {
            // The ingestion module needs all three core components
            ContextualCategorizationEngine cce = createCategorizationEngine();
            MemoryEncodingSystem mes = createMemoryEncodingSystem();
            BeliefReinforcementConflictAnalyzer brca = createBeliefAnalyzer();
            
            ingestionModule = new InMemoryInformationIngestionModule(cce, mes, brca);
        }
        return ingestionModule;
    }
    
    /**
     * Builder class for creating customized memory systems.
     */
    public static class MemorySystemBuilder {
        private ContextualCategorizationEngine categorizationEngine;
        private MemoryEncodingSystem memoryEncodingSystem;
        private BeliefReinforcementConflictAnalyzer beliefAnalyzer;
        private RelevanceEvaluationForgettingAgent forgettingAgent;
        private RetrievalResponseEngine retrievalEngine;
        private InformationIngestionModule ingestionModule;
        
        public MemorySystemBuilder withCategorizationEngine(ContextualCategorizationEngine engine) {
            this.categorizationEngine = engine;
            return this;
        }
        
        public MemorySystemBuilder withMemoryEncodingSystem(MemoryEncodingSystem system) {
            this.memoryEncodingSystem = system;
            return this;
        }
        
        public MemorySystemBuilder withBeliefAnalyzer(BeliefReinforcementConflictAnalyzer analyzer) {
            this.beliefAnalyzer = analyzer;
            return this;
        }
        
        public MemorySystemBuilder withForgettingAgent(RelevanceEvaluationForgettingAgent agent) {
            this.forgettingAgent = agent;
            return this;
        }
        
        public MemorySystemBuilder withRetrievalEngine(RetrievalResponseEngine engine) {
            this.retrievalEngine = engine;
            return this;
        }
        
        public MemorySystemBuilder withIngestionModule(InformationIngestionModule module) {
            this.ingestionModule = module;
            return this;
        }
        
        public MemorySystem build() {
            return new MemorySystem(
                categorizationEngine,
                memoryEncodingSystem,
                beliefAnalyzer,
                forgettingAgent,
                retrievalEngine,
                ingestionModule
            );
        }
    }
    
    /**
     * Wrapper class that provides access to all memory system components.
     */
    public static class MemorySystem {
        private final ContextualCategorizationEngine categorizationEngine;
        private final MemoryEncodingSystem memoryEncodingSystem;
        private final BeliefReinforcementConflictAnalyzer beliefAnalyzer;
        private final RelevanceEvaluationForgettingAgent forgettingAgent;
        private final RetrievalResponseEngine retrievalEngine;
        private final InformationIngestionModule ingestionModule;
        
        public MemorySystem(
                ContextualCategorizationEngine categorizationEngine,
                MemoryEncodingSystem memoryEncodingSystem,
                BeliefReinforcementConflictAnalyzer beliefAnalyzer,
                RelevanceEvaluationForgettingAgent forgettingAgent,
                RetrievalResponseEngine retrievalEngine,
                InformationIngestionModule ingestionModule) {
            
            this.categorizationEngine = categorizationEngine;
            this.memoryEncodingSystem = memoryEncodingSystem;
            this.beliefAnalyzer = beliefAnalyzer;
            this.forgettingAgent = forgettingAgent;
            this.retrievalEngine = retrievalEngine;
            this.ingestionModule = ingestionModule;
        }
        
        public ContextualCategorizationEngine getCategorizationEngine() {
            return categorizationEngine;
        }
        
        public MemoryEncodingSystem getMemoryEncodingSystem() {
            return memoryEncodingSystem;
        }
        
        public BeliefReinforcementConflictAnalyzer getBeliefAnalyzer() {
            return beliefAnalyzer;
        }
        
        public RelevanceEvaluationForgettingAgent getForgettingAgent() {
            return forgettingAgent;
        }
        
        public RetrievalResponseEngine getRetrievalEngine() {
            return retrievalEngine;
        }
        
        public InformationIngestionModule getIngestionModule() {
            return ingestionModule;
        }
        
        /**
         * Performs a comprehensive health check of all system components.
         * 
         * @return true if all components are healthy
         */
        public boolean isHealthy() {
            return categorizationEngine.isHealthy() &&
                   memoryEncodingSystem.isHealthy() &&
                   beliefAnalyzer.isHealthy() &&
                   forgettingAgent.isHealthy() &&
                   retrievalEngine.isHealthy() &&
                   ingestionModule.isHealthy();
        }
        
        /**
         * Gets comprehensive statistics from all system components.
         * 
         * @return Map containing statistics from all components
         */
        public java.util.Map<String, Object> getSystemStatistics() {
            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            
            stats.put("categorizationEngine", categorizationEngine.getCategorizationStatistics());
            stats.put("memoryEncodingSystem", memoryEncodingSystem.getStorageStatistics());
            stats.put("beliefAnalyzer", beliefAnalyzer.getBeliefStatistics());
            stats.put("forgettingAgent", forgettingAgent.getForgettingStatistics());
            stats.put("retrievalEngine", retrievalEngine.getSearchStatistics());
            stats.put("ingestionModule", ingestionModule.getIngestionStatistics());
            stats.put("systemHealthy", isHealthy());
            stats.put("timestamp", java.time.Instant.now().toString());
            
            return stats;
        }
    }
    
    /**
     * Creates a factory configured for testing purposes.
     * 
     * @return A factory with test-friendly configurations
     */
    public static InMemoryMemorySystemFactory forTesting() {
        InMemoryMemorySystemFactory factory = new InMemoryMemorySystemFactory();
        
        // Create components with test-friendly configurations
        InMemoryContextualCategorizationEngine cce = new InMemoryContextualCategorizationEngine();
        cce.setConfidenceThreshold(0.5); // Lower threshold for testing
        
        factory.categorizationEngine = cce;
        
        return factory;
    }
    
    /**
     * Creates a factory configured for development purposes.
     * 
     * @return A factory with development-friendly configurations
     */
    public static InMemoryMemorySystemFactory forDevelopment() {
        InMemoryMemorySystemFactory factory = new InMemoryMemorySystemFactory();
        
        // Create components with development-friendly configurations
        InMemoryContextualCategorizationEngine cce = new InMemoryContextualCategorizationEngine();
        cce.setConfidenceThreshold(0.6);
        
        factory.categorizationEngine = cce;
        
        return factory;
    }
    
    /**
     * Creates a factory configured for production-like environments.
     * 
     * @return A factory with production-ready configurations
     */
    public static InMemoryMemorySystemFactory forProduction() {
        InMemoryMemorySystemFactory factory = new InMemoryMemorySystemFactory();
        
        // Create components with production-like configurations
        InMemoryContextualCategorizationEngine cce = new InMemoryContextualCategorizationEngine();
        cce.setConfidenceThreshold(0.7); // Higher threshold for production
        
        factory.categorizationEngine = cce;
        
        return factory;
    }
}