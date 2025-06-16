package ai.headkey.rest;

import java.util.Map;

import ai.headkey.memory.implementations.InMemoryMemorySystemFactory;
import ai.headkey.memory.interfaces.InformationIngestionModule;
import ai.headkey.persistence.services.JpaMemoryEncodingSystem;
import ai.headkey.rest.service.MemoryDtoMapper;
import io.quarkus.test.junit.QuarkusTestProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManagerFactory;

/**
 * Test configuration for REST module tests.
 *
 * This configuration provides test-specific beans and settings for the REST API tests,
 * ensuring isolated and repeatable test execution with proper dependency injection.
 *
 * Follows TDD principles by providing clean test setup and teardown mechanisms.
 */
@ApplicationScoped
public class TestConfiguration implements QuarkusTestProfile {

    @Inject
    EntityManagerFactory emFactory;

    /**
     * Provides configuration properties for testing.
     *
     * @return Map of configuration properties
     */
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "quarkus.log.level",
            "INFO",
            "quarkus.log.category.\"ai.headkey\".level",
            "DEBUG",
            "quarkus.http.test-port",
            "0",
            "quarkus.http.test-ssl-port",
            "0"
        );
    }

    /**
     * Produces a test-specific InformationIngestionModule.
     *
     * This creates a fresh instance for each test run to ensure test isolation
     * and prevent state leakage between tests.
     *
     * @return A test instance of InformationIngestionModule
     */
    @Produces
    @Singleton
    public InformationIngestionModule testInformationIngestionModule() {
        // Create a fresh memory system for testing
        InMemoryMemorySystemFactory factory =
            InMemoryMemorySystemFactory.forTesting();
        InMemoryMemorySystemFactory.MemorySystem memorySystem =
            factory.createCompleteSystem();
        return memorySystem.getIngestionModule();
    }

    /**
     * Produces a test-specific MemoryDtoMapper.
     *
     * @return A test instance of MemoryDtoMapper
     */
    @Produces
    @Singleton
    public MemoryDtoMapper testMemoryDtoMapper() {
        return new MemoryDtoMapper();
    }

    /**
     * Produces a test-specific memory system factory.
     *
     * @return A test instance of InMemoryMemorySystemFactory
     */
    @Produces
    @Singleton
    public InMemoryMemorySystemFactory testMemorySystemFactory() {
        return InMemoryMemorySystemFactory.forTesting();
    }

    @Produces
    @Singleton
    public JpaMemoryEncodingSystem testJpaMemoryEncodingSystem() {
        // Return a no-op implementation for testing purposes
        return new JpaMemoryEncodingSystem(
            emFactory
        );
    }
}
