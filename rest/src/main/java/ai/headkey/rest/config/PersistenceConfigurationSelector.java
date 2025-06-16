package ai.headkey.rest.config;

import ai.headkey.memory.interfaces.BeliefExtractionService;
import ai.headkey.memory.interfaces.BeliefReinforcementConflictAnalyzer;
import ai.headkey.memory.interfaces.BeliefRelationshipService;
import ai.headkey.memory.interfaces.BeliefStorageService;
import ai.headkey.memory.interfaces.ContextualCategorizationEngine;
import ai.headkey.memory.interfaces.InformationIngestionModule;
import ai.headkey.memory.interfaces.MemoryEncodingSystem;
import ai.headkey.persistence.services.JpaMemoryEncodingSystem;
import ai.headkey.rest.service.MemoryDtoMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Central configuration selector for persistence implementations.
 *
 * This class acts as a dispatcher that selects the appropriate persistence
 * implementation based on the configured persistence type. It follows the
 * strategy pattern to enable runtime selection of persistence backends.
 *
 * The selector supports:
 * - PostgreSQL persistence (via JPA)
 * - Elasticsearch persistence
 * - Automatic fallback to PostgreSQL if configuration is invalid
 *
 * Configuration is driven by the `headkey.memory.persistence-type` property:
 * - "postgres": Uses PostgreSQL/JPA implementations
 * - "elasticsearch": Uses Elasticsearch implementations
 *
 * Features:
 * - Type-safe dependency injection using CDI qualifiers
 * - Fail-fast validation of persistence configuration
 * - Comprehensive logging for troubleshooting
 * - Graceful fallback to PostgreSQL for unknown configurations
 *
 * The configuration follows the 12-factor app principles by:
 * - Separating configuration from code
 * - Using environment-driven configuration
 * - Providing clear error messages for misconfiguration
 */
@ApplicationScoped
public class PersistenceConfigurationSelector {

    @Inject
    MemorySystemProperties properties;

    // PostgreSQL implementations
    @Inject
    @PostgresPersistence
    Instance<JpaMemoryEncodingSystem> postgresMemorySystem;

    @Inject
    @PostgresPersistence
    Instance<ContextualCategorizationEngine> postgresCategorizationEngine;

    @Inject
    @PostgresPersistence
    Instance<BeliefExtractionService> postgresBeliefExtractionService;

    @Inject
    @PostgresPersistence
    Instance<BeliefStorageService> postgresBeliefStorageService;

    @Inject
    @PostgresPersistence
    Instance<BeliefReinforcementConflictAnalyzer> postgresBeliefAnalyzer;

    @Inject
    @PostgresPersistence
    Instance<InformationIngestionModule> postgresIngestionModule;

    @Inject
    @PostgresPersistence
    Instance<BeliefRelationshipService> postgresRelationshipService;

    // Elasticsearch implementations
    @Inject
    @ElasticsearchPersistence
    Instance<MemoryEncodingSystem> elasticsearchMemorySystem;

    @Inject
    @ElasticsearchPersistence
    Instance<ContextualCategorizationEngine> elasticsearchCategorizationEngine;

    @Inject
    @ElasticsearchPersistence
    Instance<BeliefExtractionService> elasticsearchBeliefExtractionService;

    @Inject
    @ElasticsearchPersistence
    Instance<BeliefStorageService> elasticsearchBeliefStorageService;

    @Inject
    @ElasticsearchPersistence
    Instance<BeliefReinforcementConflictAnalyzer> elasticsearchBeliefAnalyzer;

    @Inject
    @ElasticsearchPersistence
    Instance<InformationIngestionModule> elasticsearchIngestionModule;

    @Inject
    @ElasticsearchPersistence
    Instance<BeliefRelationshipService> elasticsearchRelationshipService;

    /**
     * Produces the active MemoryEncodingSystem based on persistence configuration.
     *
     * @return The configured MemoryEncodingSystem implementation
     */
    @Produces
    @Singleton
    public MemoryEncodingSystem memoryEncodingSystem() {
        String persistenceType = properties.persistenceType();
        Log.infof("Selecting MemoryEncodingSystem for persistence type: %s", persistenceType);

        switch (persistenceType.toLowerCase()) {
            case "postgres":
            case "postgresql":
                if (postgresMemorySystem.isUnsatisfied()) {
                    throw new RuntimeException("PostgreSQL MemoryEncodingSystem not available. Check PostgreSQL configuration.");
                }
                Log.info("Using PostgreSQL MemoryEncodingSystem");
                return postgresMemorySystem.get();

            case "elasticsearch":
            case "elastic":
                if (elasticsearchMemorySystem.isUnsatisfied()) {
                    throw new RuntimeException("Elasticsearch MemoryEncodingSystem not available. Check Elasticsearch configuration.");
                }
                Log.info("Using Elasticsearch MemoryEncodingSystem");
                return elasticsearchMemorySystem.get();

            default:
                Log.warnf("Unknown persistence type '%s', falling back to PostgreSQL", persistenceType);
                if (postgresMemorySystem.isUnsatisfied()) {
                    throw new RuntimeException("PostgreSQL MemoryEncodingSystem not available for fallback. Check configuration.");
                }
                return postgresMemorySystem.get();
        }
    }

    /**
     * Produces the active ContextualCategorizationEngine based on persistence configuration.
     *
     * @return The configured ContextualCategorizationEngine implementation
     */
    @Produces
    @Singleton
    public ContextualCategorizationEngine contextualCategorizationEngine() {
        String persistenceType = properties.persistenceType();
        Log.infof("Selecting ContextualCategorizationEngine for persistence type: %s", persistenceType);

        switch (persistenceType.toLowerCase()) {
            case "postgres":
            case "postgresql":
                if (postgresCategorizationEngine.isUnsatisfied()) {
                    throw new RuntimeException("PostgreSQL ContextualCategorizationEngine not available. Check PostgreSQL configuration.");
                }
                return postgresCategorizationEngine.get();

            case "elasticsearch":
            case "elastic":
                if (elasticsearchCategorizationEngine.isUnsatisfied()) {
                    throw new RuntimeException("Elasticsearch ContextualCategorizationEngine not available. Check Elasticsearch configuration.");
                }
                return elasticsearchCategorizationEngine.get();

            default:
                Log.warnf("Unknown persistence type '%s', falling back to PostgreSQL", persistenceType);
                if (postgresCategorizationEngine.isUnsatisfied()) {
                    throw new RuntimeException("PostgreSQL ContextualCategorizationEngine not available for fallback. Check configuration.");
                }
                return postgresCategorizationEngine.get();
        }
    }

    /**
     * Produces the active BeliefExtractionService based on persistence configuration.
     *
     * @return The configured BeliefExtractionService implementation
     */
    @Produces
    @Singleton
    public BeliefExtractionService beliefExtractionService() {
        String persistenceType = properties.persistenceType();
        Log.infof("Selecting BeliefExtractionService for persistence type: %s", persistenceType);

        switch (persistenceType.toLowerCase()) {
            case "postgres":
            case "postgresql":
                if (postgresBeliefExtractionService.isUnsatisfied()) {
                    throw new RuntimeException("PostgreSQL BeliefExtractionService not available. Check PostgreSQL configuration.");
                }
                return postgresBeliefExtractionService.get();

            case "elasticsearch":
            case "elastic":
                if (elasticsearchBeliefExtractionService.isUnsatisfied()) {
                    throw new RuntimeException("Elasticsearch BeliefExtractionService not available. Check Elasticsearch configuration.");
                }
                return elasticsearchBeliefExtractionService.get();

            default:
                Log.warnf("Unknown persistence type '%s', falling back to PostgreSQL", persistenceType);
                if (postgresBeliefExtractionService.isUnsatisfied()) {
                    throw new RuntimeException("PostgreSQL BeliefExtractionService not available for fallback. Check configuration.");
                }
                return postgresBeliefExtractionService.get();
        }
    }

    /**
     * Produces the active BeliefStorageService based on persistence configuration.
     *
     * @return The configured BeliefStorageService implementation
     */
    @Produces
    @Singleton
    public BeliefStorageService beliefStorageService() {
        String persistenceType = properties.persistenceType();
        Log.infof("Selecting BeliefStorageService for persistence type: %s", persistenceType);

        switch (persistenceType.toLowerCase()) {
            case "postgres":
            case "postgresql":
                if (postgresBeliefStorageService.isUnsatisfied()) {
                    throw new RuntimeException("PostgreSQL BeliefStorageService not available. Check PostgreSQL configuration.");
                }
                return postgresBeliefStorageService.get();

            case "elasticsearch":
            case "elastic":
                if (elasticsearchBeliefStorageService.isUnsatisfied()) {
                    throw new RuntimeException("Elasticsearch BeliefStorageService not available. Check Elasticsearch configuration.");
                }
                return elasticsearchBeliefStorageService.get();

            default:
                Log.warnf("Unknown persistence type '%s', falling back to PostgreSQL", persistenceType);
                if (postgresBeliefStorageService.isUnsatisfied()) {
                    throw new RuntimeException("PostgreSQL BeliefStorageService not available for fallback. Check configuration.");
                }
                return postgresBeliefStorageService.get();
        }
    }

    /**
     * Produces the active BeliefReinforcementConflictAnalyzer based on persistence configuration.
     *
     * @return The configured BeliefReinforcementConflictAnalyzer implementation
     */
    @Produces
    @Singleton
    public BeliefReinforcementConflictAnalyzer beliefReinforcementConflictAnalyzer() {
        String persistenceType = properties.persistenceType();
        Log.infof("Selecting BeliefReinforcementConflictAnalyzer for persistence type: %s", persistenceType);

        switch (persistenceType.toLowerCase()) {
            case "postgres":
            case "postgresql":
                if (postgresBeliefAnalyzer.isUnsatisfied()) {
                    throw new RuntimeException("PostgreSQL BeliefReinforcementConflictAnalyzer not available. Check PostgreSQL configuration.");
                }
                return postgresBeliefAnalyzer.get();

            case "elasticsearch":
            case "elastic":
                if (elasticsearchBeliefAnalyzer.isUnsatisfied()) {
                    throw new RuntimeException("Elasticsearch BeliefReinforcementConflictAnalyzer not available. Check Elasticsearch configuration.");
                }
                return elasticsearchBeliefAnalyzer.get();

            default:
                Log.warnf("Unknown persistence type '%s', falling back to PostgreSQL", persistenceType);
                if (postgresBeliefAnalyzer.isUnsatisfied()) {
                    throw new RuntimeException("PostgreSQL BeliefReinforcementConflictAnalyzer not available for fallback. Check configuration.");
                }
                return postgresBeliefAnalyzer.get();
        }
    }

    /**
     * Produces the active InformationIngestionModule based on persistence configuration.
     *
     * @return The configured InformationIngestionModule implementation
     */
    @Produces
    @Singleton
    public InformationIngestionModule informationIngestionModule() {
        String persistenceType = properties.persistenceType();
        Log.infof("Selecting InformationIngestionModule for persistence type: %s", persistenceType);

        switch (persistenceType.toLowerCase()) {
            case "postgres":
            case "postgresql":
                if (postgresIngestionModule.isUnsatisfied()) {
                    throw new RuntimeException("PostgreSQL InformationIngestionModule not available. Check PostgreSQL configuration.");
                }
                return postgresIngestionModule.get();

            case "elasticsearch":
            case "elastic":
                if (elasticsearchIngestionModule.isUnsatisfied()) {
                    throw new RuntimeException("Elasticsearch InformationIngestionModule not available. Check Elasticsearch configuration.");
                }
                return elasticsearchIngestionModule.get();

            default:
                Log.warnf("Unknown persistence type '%s', falling back to PostgreSQL", persistenceType);
                if (postgresIngestionModule.isUnsatisfied()) {
                    throw new RuntimeException("PostgreSQL InformationIngestionModule not available for fallback. Check configuration.");
                }
                return postgresIngestionModule.get();
        }
    }

    /**
     * Produces the active BeliefRelationshipService based on persistence configuration.
     *
     * @return The configured BeliefRelationshipService implementation
     */
    @Produces
    @Singleton
    public BeliefRelationshipService beliefRelationshipService() {
        String persistenceType = properties.persistenceType();
        Log.infof("Selecting BeliefRelationshipService for persistence type: %s", persistenceType);

        switch (persistenceType.toLowerCase()) {
            case "postgres":
            case "postgresql":
                if (postgresRelationshipService.isUnsatisfied()) {
                    throw new RuntimeException("PostgreSQL BeliefRelationshipService not available. Check PostgreSQL configuration.");
                }
                return postgresRelationshipService.get();

            case "elasticsearch":
            case "elastic":
                if (elasticsearchRelationshipService.isUnsatisfied()) {
                    throw new RuntimeException("Elasticsearch BeliefRelationshipService not available. Check Elasticsearch configuration.");
                }
                return elasticsearchRelationshipService.get();

            default:
                Log.warnf("Unknown persistence type '%s', falling back to PostgreSQL", persistenceType);
                if (postgresRelationshipService.isUnsatisfied()) {
                    throw new RuntimeException("PostgreSQL BeliefRelationshipService not available for fallback. Check configuration.");
                }
                return postgresRelationshipService.get();
        }
    }

    /**
     * Produces the MemoryDtoMapper as a CDI bean.
     *
     * The mapper is stateless and can be safely shared across the application
     * as a singleton, regardless of persistence type.
     *
     * @return A singleton instance of MemoryDtoMapper
     */
    @Produces
    @Singleton
    public MemoryDtoMapper memoryDtoMapper() {
        Log.debug("Creating MemoryDtoMapper bean");
        return new MemoryDtoMapper();
    }

    /**
     * Validates the persistence configuration at startup.
     *
     * This method performs early validation to ensure that the configured
     * persistence type is valid and that the required implementations are available.
     *
     * @throws RuntimeException if the configuration is invalid
     */
    public void validateConfiguration() {
        String persistenceType = properties.persistenceType();
        Log.infof("Validating persistence configuration: %s", persistenceType);

        switch (persistenceType.toLowerCase()) {
            case "postgres":
            case "postgresql":
                if (postgresMemorySystem.isUnsatisfied()) {
                    throw new RuntimeException(
                        "PostgreSQL persistence is configured but PostgreSQL implementations are not available. " +
                        "Check that the persistence-postgres module is on the classpath and PostgreSQL is properly configured."
                    );
                }
                Log.info("PostgreSQL persistence configuration validated successfully");
                break;

            case "elasticsearch":
            case "elastic":
                if (elasticsearchMemorySystem.isUnsatisfied()) {
                    throw new RuntimeException(
                        "Elasticsearch persistence is configured but Elasticsearch implementations are not available. " +
                        "Check that the persistence-elastic module is on the classpath and Elasticsearch is properly configured."
                    );
                }
                Log.info("Elasticsearch persistence configuration validated successfully");
                break;

            default:
                Log.warnf(
                    "Unknown persistence type '%s'. Valid values are: 'postgres', 'postgresql', 'elasticsearch', 'elastic'. " +
                    "Falling back to PostgreSQL.", persistenceType
                );
                if (postgresMemorySystem.isUnsatisfied()) {
                    throw new RuntimeException(
                        "Unknown persistence type configured and PostgreSQL fallback is not available. " +
                        "Check configuration and ensure PostgreSQL persistence is properly set up."
                    );
                }
                break;
        }
    }
}
