package ai.headkey.rest.config;

import ai.headkey.memory.implementations.InMemoryMemorySystemFactory;
import ai.headkey.memory.interfaces.InformationIngestionModule;
import ai.headkey.rest.service.MemoryDtoMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

/**
 * CDI Configuration for Memory System Components.
 * 
 * This configuration class provides CDI beans for the memory system components,
 * ensuring proper dependency injection and lifecycle management throughout the
 * REST application.
 * 
 * The configuration follows the 12-factor app principles by:
 * - Separating configuration from code
 * - Using dependency injection for testability
 * - Providing singleton instances for stateless services
 */
@ApplicationScoped
public class MemorySystemConfig {
    
    private static final Logger LOG = Logger.getLogger(MemorySystemConfig.class);
    
    /**
     * Produces the InformationIngestionModule as a CDI bean.
     * 
     * This method creates and configures the complete memory system using the
     * InMemoryMemorySystemFactory and extracts the ingestion module for use
     * throughout the application.
     * 
     * @return A singleton instance of InformationIngestionModule
     */
    @Produces
    @Singleton
    public InformationIngestionModule informationIngestionModule() {
        LOG.info("Initializing InformationIngestionModule for CDI");
        
        try {
            // Create the complete memory system using the factory
            InMemoryMemorySystemFactory factory = InMemoryMemorySystemFactory.forTesting();
            InMemoryMemorySystemFactory.MemorySystem memorySystem = factory.createCompleteSystem();
            
            // Extract the ingestion module
            InformationIngestionModule ingestionModule = memorySystem.getIngestionModule();
            
            LOG.info("InformationIngestionModule successfully initialized");
            return ingestionModule;
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to initialize InformationIngestionModule: %s", e.getMessage());
            throw new RuntimeException("Unable to initialize memory system", e);
        }
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
     * Produces the complete memory system factory for advanced use cases.
     * 
     * This bean provides access to the full memory system factory for
     * components that need access to multiple memory system components
     * or need to create custom configurations.
     * 
     * @return A singleton instance of InMemoryMemorySystemFactory
     */
    @Produces
    @Singleton
    public InMemoryMemorySystemFactory memorySystemFactory() {
        LOG.debug("Creating InMemoryMemorySystemFactory bean");
        return InMemoryMemorySystemFactory.forTesting();
    }
    
    /**
     * Produces the complete memory system for components that need access
     * to multiple memory subsystems.
     * 
     * @param factory The memory system factory
     * @return A singleton instance of the complete memory system
     */
    @Produces
    @Singleton
    public InMemoryMemorySystemFactory.MemorySystem memorySystem(InMemoryMemorySystemFactory factory) {
        LOG.debug("Creating complete MemorySystem bean");
        return factory.createCompleteSystem();
    }
}