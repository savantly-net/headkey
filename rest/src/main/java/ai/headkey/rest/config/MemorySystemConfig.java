package ai.headkey.rest.config;

import org.jboss.logging.Logger;

import ai.headkey.memory.abstracts.AbstractMemoryEncodingSystem;
import ai.headkey.memory.implementations.InMemoryBeliefReinforcementConflictAnalyzer;
import ai.headkey.memory.implementations.InMemoryContextualCategorizationEngine;
import ai.headkey.memory.interfaces.BeliefReinforcementConflictAnalyzer;
import ai.headkey.memory.interfaces.ContextualCategorizationEngine;
import ai.headkey.memory.interfaces.InformationIngestionModule;
import ai.headkey.memory.langchain4j.LangChain4JContextualCategorizationEngine;
import ai.headkey.persistence.factory.JpaMemorySystemFactory;
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
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

/**
 * CDI Configuration for Memory System Components.
 *
 * This configuration class provides CDI beans for the JPA-based memory system components,
 * ensuring proper dependency injection and lifecycle management throughout the
 * REST application.
 *
 * The configuration follows the 12-factor app principles by:
 * - Separating configuration from code
 * - Using dependency injection for testability
 * - Providing singleton instances for stateless services
 * - Supporting configurable database backends
 *
 * Database Configuration:
 * - Development: H2 in-memory database
 * - Test: H2 in-memory database
 * - Production: PostgreSQL (configurable via environment variables)
 *
 * Memory System Features:
 * - Automatic similarity search strategy detection
 * - Configurable performance parameters
 * - Database-specific optimizations
 */
@ApplicationScoped
public class MemorySystemConfig {

    private static final Logger LOG = Logger.getLogger(
        MemorySystemConfig.class
    );

    @Inject
    EntityManager entityManager;

    @Inject
    MemorySystemProperties properties;

    @Inject
    @io.quarkus.arc.Unremovable
    jakarta.enterprise.inject.Instance<EmbeddingModel> embeddingModel;

    /**
     * Produces the JPA-based MemoryEncodingSystem as a CDI bean.
     *
     * This method creates and configures the JPA memory system using the
     * JpaMemorySystemFactory with the appropriate similarity search strategy
     * based on configuration and database capabilities.
     *
     * @return A singleton instance of JpaMemoryEncodingSystem
     */
    @Produces
    @Singleton
    public JpaMemoryEncodingSystem jpaMemoryEncodingSystem() {
        LOG.info("Initializing JpaMemoryEncodingSystem for CDI");

        try {
            // Create LangChain4J embedding generator
            AbstractMemoryEncodingSystem.VectorEmbeddingGenerator embeddingGenerator =
                createLangChain4JEmbeddingGenerator();

            // Create similarity search strategy based on configuration
            JpaSimilaritySearchStrategy strategy =
                createSimilaritySearchStrategy();

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

            LOG.infof(
                "JpaMemoryEncodingSystem successfully initialized with strategy: %s",
                strategy.getStrategyName()
            );
            return memorySystem;
        } catch (Exception e) {
            LOG.errorf(
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
     * Produces the ContextualCategorizationEngine as a CDI bean.
     *
     * @return A singleton instance of ContextualCategorizationEngine
     */
    @Produces
    @Singleton
    public ContextualCategorizationEngine contextualCategorizationEngine(QuarkusCategoryExtractionService categoryService, QuarkusTagExtractionService tagService) {
        LOG.info("Creating InMemoryContextualCategorizationEngine");
        if (System.getenv("OPENAI_API_KEY") == null) {
            LOG.warn("OPENAI_API_KEY environment variable not set");
            return new InMemoryContextualCategorizationEngine();
        }

        // Initialize the categorization engine
        return new LangChain4JContextualCategorizationEngine(categoryService, tagService);
    }

    /**
     * Produces the BeliefReinforcementConflictAnalyzer as a CDI bean.
     *
     * @param memorySystem The memory encoding system for belief storage
     * @return A singleton instance of BeliefReinforcementConflictAnalyzer
     */
    @Produces
    @Singleton
    public BeliefReinforcementConflictAnalyzer beliefReinforcementConflictAnalyzer(
        JpaMemoryEncodingSystem memorySystem
    ) {
        LOG.info("Creating InMemoryBeliefReinforcementConflictAnalyzer");
        // For now using in-memory implementation, could be enhanced with JPA-based storage
        return new InMemoryBeliefReinforcementConflictAnalyzer();
    }

    /**
     * Produces the InformationIngestionModule as a CDI bean.
     *
     * This creates the refactored adapter that uses InformationIngestionModuleImpl
     * with proper separation of concerns through dependency injection.
     *
     * @param categorizationEngine The categorization engine
     * @param memorySystem The JPA memory encoding system
     * @param beliefAnalyzer The belief analysis system
     * @return A singleton instance of InformationIngestionModule
     */
    @Produces
    @Singleton
    public InformationIngestionModule informationIngestionModule(
        ContextualCategorizationEngine categorizationEngine,
        JpaMemoryEncodingSystem memorySystem,
        BeliefReinforcementConflictAnalyzer beliefAnalyzer
    ) {
        LOG.info(
            "Creating InformationIngestionModule using InformationIngestionModuleImpl"
        );

        return new JpaInformationIngestionAdapter(
            categorizationEngine,
            memorySystem,
            beliefAnalyzer
        );
    }

    /**
     * Produces the MemoryDtoMapper as a CDI bean.
     *
     * The mapper is stateless and can be safely shared across the application
     * as a singleton.
     *
     * @return A singleton instance of MemoryDtoMapper
     */
    @Produces
    @Singleton
    public MemoryDtoMapper memoryDtoMapper() {
        LOG.debug("Creating MemoryDtoMapper bean");
        return new MemoryDtoMapper();
    }

    /**
     * Creates a similarity search strategy based on configuration.
     *
     * @return The appropriate similarity search strategy
     */
    private JpaSimilaritySearchStrategy createSimilaritySearchStrategy() {
        String strategyName = properties.strategy();
        LOG.infof("Creating similarity search strategy: %s", strategyName);

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
                LOG.warnf(
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
    private AbstractMemoryEncodingSystem.VectorEmbeddingGenerator createLangChain4JEmbeddingGenerator() {
        if (!properties.embedding().enabled()) {
            LOG.info("Vector embedding generation disabled by configuration");
            return createFallbackEmbeddingGenerator();
        }

        if (embeddingModel.isUnsatisfied()) {
            LOG.warn("No EmbeddingModel bean available, falling back to mock generator");
            return createFallbackEmbeddingGenerator();
        }

        EmbeddingModel model = embeddingModel.get();
        LOG.infof(
            "Creating LangChain4J embedding generator with model: %s",
            model.getClass().getSimpleName()
        );

        try {
            return new LangChain4JVectorEmbeddingGenerator(model);
        } catch (Exception e) {
            LOG.errorf(
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
    private AbstractMemoryEncodingSystem.VectorEmbeddingGenerator createFallbackEmbeddingGenerator() {
        LOG.warn(
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
