package ai.headkey.rest.config;

import ai.headkey.memory.implementations.InMemoryContextualCategorizationEngine;
import ai.headkey.memory.implementations.InformationIngestionModuleImpl;
import ai.headkey.memory.implementations.SimplePatternBeliefExtractionService;
import ai.headkey.memory.implementations.StandardBeliefReinforcementConflictAnalyzer;
import ai.headkey.memory.interfaces.BeliefExtractionService;
import ai.headkey.memory.interfaces.BeliefReinforcementConflictAnalyzer;
import ai.headkey.memory.interfaces.BeliefRelationshipService;
import ai.headkey.memory.interfaces.BeliefStorageService;
import ai.headkey.memory.interfaces.ContextualCategorizationEngine;
import ai.headkey.memory.interfaces.InformationIngestionModule;
import ai.headkey.memory.interfaces.VectorEmbeddingGenerator;
import ai.headkey.memory.langchain4j.LangChain4JBeliefExtractionService;
import ai.headkey.memory.langchain4j.LangChain4JContextualCategorizationEngine;
import ai.headkey.persistence.factory.JpaMemorySystemFactory;
import ai.headkey.persistence.repositories.BeliefConflictRepository;
import ai.headkey.persistence.repositories.BeliefRelationshipRepository;
import ai.headkey.persistence.repositories.BeliefRepository;
import ai.headkey.persistence.repositories.impl.BeliefRelationshipRepositoryImpl;
import ai.headkey.persistence.repositories.impl.JpaBeliefConflictRepository;
import ai.headkey.persistence.repositories.impl.JpaBeliefRepository;
import ai.headkey.persistence.services.JpaBeliefRelationshipService;
import ai.headkey.persistence.services.JpaBeliefStorageService;
import ai.headkey.persistence.services.JpaMemoryEncodingSystem;
import ai.headkey.persistence.strategies.jpa.DefaultJpaSimilaritySearchStrategy;
import ai.headkey.persistence.strategies.jpa.JpaSimilaritySearchStrategy;
import ai.headkey.persistence.strategies.jpa.JpaSimilaritySearchStrategyFactory;
import ai.headkey.persistence.strategies.jpa.PostgresJpaSimilaritySearchStrategy;
import ai.headkey.persistence.strategies.jpa.TextBasedJpaSimilaritySearchStrategy;
import ai.headkey.rest.service.LangChain4JVectorEmbeddingGenerator;
import ai.headkey.rest.service.MemoryDtoMapper;
import ai.headkey.rest.service.QuarkusCategoryExtractionService;
import ai.headkey.rest.service.QuarkusTagExtractionService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

/**
 * Legacy CDI Configuration for Memory System Components.
 *
 * This configuration class is deprecated in favor of the new persistence-specific
 * configurations (PostgresPersistenceConfiguration, ElasticsearchPersistenceConfiguration)
 * and the PersistenceConfigurationSelector.
 *
 * This class is kept for backward compatibility but will be removed in a future version.
 * New implementations should use the persistence-specific configurations.
 *
 * @deprecated Use {@link PersistenceConfigurationSelector} with persistence-specific configurations
 */
@Deprecated
@ApplicationScoped
public class MemorySystemConfig {

    @Inject
    EntityManager entityManager;

    @Inject
    MemorySystemProperties properties;

    @Inject
    @io.quarkus.arc.Unremovable
    jakarta.enterprise.inject.Instance<EmbeddingModel> embeddingModel;

    @Inject
    @io.quarkus.arc.Unremovable
    jakarta.enterprise.inject.Instance<ChatModel> chatModel;

    /**
     * Legacy producer for JPA-based MemoryEncodingSystem.
     *
     * @deprecated This producer is disabled to avoid conflicts with the new persistence selector
     * @return A singleton instance of JpaMemoryEncodingSystem
     */
    @Deprecated
    // @Produces - Commented out to avoid conflicts with PersistenceConfigurationSelector
    // @Singleton
    public JpaMemoryEncodingSystem jpaMemoryEncodingSystem() {
        Log.info("Initializing JpaMemoryEncodingSystem for CDI");

        try {
            // Create LangChain4J embedding generator
            VectorEmbeddingGenerator embeddingGenerator =
                createLangChain4JEmbeddingGenerator();

            // Create similarity search strategy based on configuration
            JpaSimilaritySearchStrategy strategy =
                createSimilaritySearchStrategy();
            strategy.initialize(entityManager);

            // Build the memory system with configuration
            JpaMemoryEncodingSystem memorySystem =
                JpaMemorySystemFactory.builder()
                    .entityManagerFactory(
                        entityManager.getEntityManagerFactory()
                    )
                    .embeddingGenerator(embeddingGenerator)
                    .similarityStrategy(strategy)
                    .batchSize(properties.batchSize())
                    .enableSecondLevelCache(properties.enableSecondLevelCache())
                    .maxSimilaritySearchResults(
                        properties.maxSimilarityResults()
                    )
                    .similarityThreshold(properties.similarityThreshold())
                    .build();

            Log.infof(
                "JpaMemoryEncodingSystem successfully initialized with strategy: %s",
                strategy.getStrategyName()
            );
            return memorySystem;
        } catch (Exception e) {
            Log.errorf(
                e,
                "Failed to initialize JpaMemoryEncodingSystem: %s",
                e.getMessage()
            );
            throw new RuntimeException(
                "Unable to initialize JPA memory system",
                e
            );
        }
    }

    /**
     * Legacy producer for ContextualCategorizationEngine.
     *
     * @deprecated This producer is disabled to avoid conflicts with the new persistence selector
     * @return A singleton instance of ContextualCategorizationEngine
     */
    @Deprecated
    // @Produces - Commented out to avoid conflicts with PersistenceConfigurationSelector
    // @Singleton
    public ContextualCategorizationEngine contextualCategorizationEngine(
        QuarkusCategoryExtractionService categoryService,
        QuarkusTagExtractionService tagService
    ) {
        Log.info("Creating ContextualCategorizationEngine");
        if (chatModel.isUnsatisfied()) {
            Log.info(
                "ChatModel not available, using InMemoryContextualCategorizationEngine"
            );
            return new InMemoryContextualCategorizationEngine();
        }

        // Initialize the categorization engine
        Log.info("Creating LangChain4JContextualCategorizationEngine");
        return new LangChain4JContextualCategorizationEngine(
            categoryService,
            tagService
        );
    }

    /**
     * Legacy producer for BeliefExtractionService.
     *
     * @deprecated This producer is disabled to avoid conflicts with the new persistence selector
     * @return A singleton instance of BeliefExtractionService
     */
    @Deprecated
    // @Produces - Commented out to avoid conflicts with PersistenceConfigurationSelector
    // @Singleton
    public BeliefExtractionService beliefExtractionService() {
        if (chatModel.isUnsatisfied()) {
            Log.info(
                "ChatModel not available, using SimplePatternBeliefExtractionService"
            );
            return new SimplePatternBeliefExtractionService();
        }

        try {
            ChatModel model = chatModel.get();
            Log.infof(
                "Creating LangChain4JBeliefExtractionService with model: %s",
                model.getClass().getSimpleName()
            );
            return new LangChain4JBeliefExtractionService(model);
        } catch (Exception e) {
            Log.errorf(
                e,
                "Failed to create LangChain4JBeliefExtractionService, falling back to simple implementation"
            );
            return new SimplePatternBeliefExtractionService();
        }
    }

    /**
     * Legacy producer for BeliefStorageService.
     *
     * @deprecated This producer is disabled to avoid conflicts with the new persistence selector
     * @return A singleton instance of BeliefStorageService
     */
    @Deprecated
    // @Produces - Commented out to avoid conflicts with PersistenceConfigurationSelector
    // @Singleton
    public BeliefStorageService beliefStorageService() {
        Log.info("Creating JpaBeliefStorageService");

        // Create LangChain4J embedding generator
        VectorEmbeddingGenerator embeddingGenerator =
            createLangChain4JEmbeddingGenerator();
        // Create repository dependencies
        BeliefRepository beliefRepository = new JpaBeliefRepository(
            entityManager.getEntityManagerFactory(),
            embeddingGenerator
        );
        BeliefConflictRepository conflictRepository =
            new JpaBeliefConflictRepository(
                entityManager.getEntityManagerFactory()
            );
        return new JpaBeliefStorageService(
            beliefRepository,
            conflictRepository
        );
    }

    /**
     * Legacy producer for BeliefReinforcementConflictAnalyzer.
     *
     * @deprecated This producer is disabled to avoid conflicts with the new persistence selector
     * @param beliefExtractionService The belief extraction service
     * @param beliefStorageService The belief storage service
     * @return A singleton instance of BeliefReinforcementConflictAnalyzer
     */
    @Deprecated
    // @Produces - Commented out to avoid conflicts with PersistenceConfigurationSelector
    // @Singleton
    public BeliefReinforcementConflictAnalyzer beliefReinforcementConflictAnalyzer(
        BeliefExtractionService beliefExtractionService,
        BeliefStorageService beliefStorageService
    ) {
        Log.infof(
            "Creating InMemoryBeliefReinforcementConflictAnalyzer with extraction service: %s",
            beliefExtractionService.getClass().getSimpleName()
        );
        return new StandardBeliefReinforcementConflictAnalyzer(
            beliefExtractionService,
            beliefStorageService
        );
    }

    /**
     * Legacy producer for InformationIngestionModule.
     *
     * @deprecated This producer is disabled to avoid conflicts with the new persistence selector
     * @param categorizationEngine The categorization engine
     * @param memorySystem The JPA memory encoding system
     * @param beliefAnalyzer The belief analysis system
     * @return A singleton instance of InformationIngestionModule
     */
    @Deprecated
    // @Produces - Commented out to avoid conflicts with PersistenceConfigurationSelector
    // @Singleton
    public InformationIngestionModule informationIngestionModule(
        ContextualCategorizationEngine categorizationEngine,
        JpaMemoryEncodingSystem memorySystem,
        BeliefReinforcementConflictAnalyzer beliefAnalyzer
    ) {
        Log.info(
            "Creating InformationIngestionModule using InformationIngestionModuleImpl"
        );

        return new InformationIngestionModuleImpl(
            categorizationEngine,
            memorySystem,
            beliefAnalyzer
        );
    }

    /**
     * Legacy producer for BeliefRelationshipService.
     *
     * @deprecated This producer is disabled to avoid conflicts with the new persistence selector
     * @return A singleton instance of BeliefRelationshipService
     */
    @Deprecated
    // @Produces - Commented out to avoid conflicts with PersistenceConfigurationSelector
    // @Singleton
    public BeliefRelationshipService beliefRelationshipService() {
        Log.info("Creating JpaBeliefRelationshipService");

        // Create repository dependencies
        BeliefRepository beliefRepository = new JpaBeliefRepository(
            entityManager.getEntityManagerFactory()
        );
        BeliefRelationshipRepository relationshipRepository =
            new BeliefRelationshipRepositoryImpl(entityManager);

        return new JpaBeliefRelationshipService(
            relationshipRepository,
            beliefRepository
        );
    }

    /**
     * Legacy producer for MemoryDtoMapper.
     *
     * @deprecated This producer is disabled to avoid conflicts with the new persistence selector
     * @return A singleton instance of MemoryDtoMapper
     */
    @Deprecated
    // @Produces - Commented out to avoid conflicts with PersistenceConfigurationSelector
    // @Singleton
    public MemoryDtoMapper memoryDtoMapper() {
        Log.debug("Creating MemoryDtoMapper bean (legacy)");
        return new MemoryDtoMapper();
    }

    /**
     * Creates a similarity search strategy based on configuration.
     *
     * @return The appropriate similarity search strategy
     */
    private JpaSimilaritySearchStrategy createSimilaritySearchStrategy() {
        String strategyName = properties.strategy();
        Log.infof("Creating similarity search strategy: %s", strategyName);

        switch (strategyName.toLowerCase()) {
            case "auto":
                // Let the factory auto-detect the best strategy
                return JpaSimilaritySearchStrategyFactory.createStrategy(
                    entityManager
                );
            case "text":
                return new TextBasedJpaSimilaritySearchStrategy();
            case "vector":
            case "default":
                return new DefaultJpaSimilaritySearchStrategy();
            case "postgres":
                return new PostgresJpaSimilaritySearchStrategy();
            default:
                Log.warnf(
                    "Unknown strategy '%s', falling back to auto-detection",
                    strategyName
                );
                return JpaSimilaritySearchStrategyFactory.createStrategy(
                    entityManager
                );
        }
    }

    /**
     * Creates a LangChain4J-based vector embedding generator.
     *
     * This method creates a production-ready embedding generator that uses
     * LangChain4J's EmbeddingModel to generate high-quality embeddings.
     *
     * @return A LangChain4J-based vector embedding generator
     */
    private VectorEmbeddingGenerator createLangChain4JEmbeddingGenerator() {
        if (!properties.embedding().enabled()) {
            Log.info("Vector embedding generation disabled by configuration");
            return createFallbackEmbeddingGenerator();
        }

        if (embeddingModel.isUnsatisfied()) {
            Log.warn(
                "No EmbeddingModel bean available, falling back to mock generator"
            );
            return createFallbackEmbeddingGenerator();
        }

        EmbeddingModel model = embeddingModel.get();
        Log.infof(
            "Creating LangChain4J embedding generator with model: %s",
            model.getClass().getSimpleName()
        );

        try {
            return new LangChain4JVectorEmbeddingGenerator(model);
        } catch (Exception e) {
            Log.errorf(
                e,
                "Failed to create LangChain4J embedding generator, falling back to mock generator"
            );
            return createFallbackEmbeddingGenerator();
        }
    }

    /**
     * Creates a fallback embedding generator for cases where LangChain4J is not available.
     *
     * This is useful for development, testing, or when API keys are not configured.
     *
     * @return A simple mock embedding generator
     */
    private VectorEmbeddingGenerator createFallbackEmbeddingGenerator() {
        Log.warn(
            "Using fallback mock embedding generator - not suitable for production"
        );

        return text -> {
            int dimension = properties.embedding().dimension();
            double[] vector = new double[dimension];

            // Simple hash-based embedding for demonstration
            int hash = text.hashCode();
            for (int i = 0; i < dimension; i++) {
                vector[i] = ((hash >> (i % 32)) & 1) == 1 ? 1.0 : 0.0;
            }

            // Normalize the vector
            double norm = 0.0;
            for (double v : vector) {
                norm += v * v;
            }
            norm = Math.sqrt(norm);

            if (norm > 0) {
                for (int i = 0; i < vector.length; i++) {
                    vector[i] /= norm;
                }
            }

            return vector;
        };
    }
}
