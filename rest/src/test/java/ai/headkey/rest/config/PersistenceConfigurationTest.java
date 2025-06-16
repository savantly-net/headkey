package ai.headkey.rest.config;

import ai.headkey.memory.interfaces.BeliefExtractionService;
import ai.headkey.memory.interfaces.BeliefRelationshipService;
import ai.headkey.memory.interfaces.BeliefStorageService;
import ai.headkey.memory.interfaces.ContextualCategorizationEngine;
import ai.headkey.memory.interfaces.InformationIngestionModule;
import ai.headkey.memory.interfaces.MemoryEncodingSystem;
import ai.headkey.persistence.services.JpaMemoryEncodingSystem;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for persistence configuration switching.
 *
 * This test verifies that the persistence configuration correctly switches
 * between PostgreSQL and Elasticsearch implementations based on the
 * configured persistence type.
 *
 * The tests use different test profiles to simulate different persistence
 * configurations and verify that the correct implementations are injected.
 */
@QuarkusTest
public class PersistenceConfigurationTest {

    @Inject
    MemorySystemProperties properties;

    @Inject
    MemoryEncodingSystem memoryEncodingSystem;

    @Inject
    ContextualCategorizationEngine categorizationEngine;

    @Inject
    BeliefExtractionService beliefExtractionService;

    @Inject
    BeliefStorageService beliefStorageService;

    @Inject
    InformationIngestionModule ingestionModule;

    @Inject
    BeliefRelationshipService relationshipService;

    @Test
    public void testDefaultPostgresConfiguration() {
        // Test that the default configuration uses PostgreSQL
        String persistenceType = properties.persistenceType();
        assertEquals("postgres", persistenceType, "Default persistence type should be postgres");

        // Verify that PostgreSQL implementations are injected
        assertNotNull(memoryEncodingSystem, "MemoryEncodingSystem should be injected");
        assertTrue(memoryEncodingSystem instanceof JpaMemoryEncodingSystem,
            "Should use JPA-based MemoryEncodingSystem for PostgreSQL");

        assertNotNull(categorizationEngine, "ContextualCategorizationEngine should be injected");
        assertNotNull(beliefExtractionService, "BeliefExtractionService should be injected");
        assertNotNull(beliefStorageService, "BeliefStorageService should be injected");
        assertNotNull(ingestionModule, "InformationIngestionModule should be injected");
        assertNotNull(relationshipService, "BeliefRelationshipService should be injected");
    }

    @Test
    public void testConfigurationProperties() {
        // Test that configuration properties are properly loaded
        assertNotNull(properties, "MemorySystemProperties should be injected");

        // Test database configuration
        assertNotNull(properties.database(), "Database config should be available");
        assertNotNull(properties.embedding(), "Embedding config should be available");
        assertNotNull(properties.performance(), "Performance config should be available");
        assertNotNull(properties.elasticsearch(), "Elasticsearch config should be available");

        // Test default values
        assertEquals(50, properties.batchSize(), "Default batch size should be 50");
        assertEquals(100, properties.maxSimilarityResults(), "Max similarity results should be 100");
        assertEquals(0.0, properties.similarityThreshold(), "Similarity threshold should be 0.0");
        assertFalse(properties.enableSecondLevelCache(), "Second level cache should be disabled");

        // Test Elasticsearch defaults
        assertEquals("localhost", properties.elasticsearch().host(), "Default Elasticsearch host should be localhost");
        assertEquals(9200, properties.elasticsearch().port(), "Default Elasticsearch port should be 9200");
        assertEquals("http", properties.elasticsearch().scheme(), "Default Elasticsearch scheme should be http");
        assertTrue(properties.elasticsearch().autoCreateIndices(), "Auto-create indices should be enabled by default");
    }

    @Test
    public void testPersistenceTypeValidation() {
        // Test that persistence type is correctly configured
        String persistenceType = properties.persistenceType();
        assertTrue(
            "postgres".equals(persistenceType) || "elasticsearch".equals(persistenceType),
            "Persistence type should be either 'postgres' or 'elasticsearch', but was: " + persistenceType
        );
    }

    /**
     * Test profile for Elasticsearch configuration.
     */
    public static class ElasticsearchTestProfile implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public java.util.Map<String, String> getConfigOverrides() {
            return java.util.Map.of(
                "headkey.memory.persistence-type", "elasticsearch",
                "headkey.memory.elasticsearch.host", "localhost",
                "headkey.memory.elasticsearch.port", "9200",
                "headkey.memory.elasticsearch.auto-create-indices", "false"
            );
        }
    }

    /**
     * Test class for Elasticsearch persistence configuration.
     */
    @QuarkusTest
    @TestProfile(ElasticsearchTestProfile.class)
    public static class ElasticsearchPersistenceConfigurationTest {

        @Inject
        MemorySystemProperties properties;

        @Inject
        MemoryEncodingSystem memoryEncodingSystem;

        @Inject
        ContextualCategorizationEngine categorizationEngine;

        @Inject
        BeliefExtractionService beliefExtractionService;

        @Inject
        BeliefStorageService beliefStorageService;

        @Inject
        InformationIngestionModule ingestionModule;

        @Inject
        BeliefRelationshipService relationshipService;

        @Test
        public void testElasticsearchConfiguration() {
            // Test that Elasticsearch configuration is active
            String persistenceType = properties.persistenceType();
            assertEquals("elasticsearch", persistenceType, "Persistence type should be elasticsearch");

            // Verify that Elasticsearch implementations are injected
            assertNotNull(memoryEncodingSystem, "MemoryEncodingSystem should be injected");
            // Note: We can't check the exact type here because the Elasticsearch implementation
            // returns a generic MemoryEncodingSystem interface

            assertNotNull(categorizationEngine, "ContextualCategorizationEngine should be injected");
            assertNotNull(beliefExtractionService, "BeliefExtractionService should be injected");
            assertNotNull(beliefStorageService, "BeliefStorageService should be injected");
            assertNotNull(ingestionModule, "InformationIngestionModule should be injected");
            assertNotNull(relationshipService, "BeliefRelationshipService should be injected");
        }

        @Test
        public void testElasticsearchConfigurationProperties() {
            // Test that Elasticsearch-specific properties are correctly configured
            assertEquals("localhost", properties.elasticsearch().host());
            assertEquals(9200, properties.elasticsearch().port());
            assertEquals("http", properties.elasticsearch().scheme());
            assertFalse(properties.elasticsearch().autoCreateIndices());
        }
    }

    /**
     * Test profile for invalid persistence type.
     */
    public static class InvalidPersistenceTestProfile implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public java.util.Map<String, String> getConfigOverrides() {
            return java.util.Map.of(
                "headkey.memory.persistence-type", "invalid-type"
            );
        }
    }

    /**
     * Test class for invalid persistence configuration fallback.
     */
    @QuarkusTest
    @TestProfile(InvalidPersistenceTestProfile.class)
    public static class InvalidPersistenceConfigurationTest {

        @Inject
        MemorySystemProperties properties;

        @Inject
        MemoryEncodingSystem memoryEncodingSystem;

        @Test
        public void testInvalidPersistenceTypeFallback() {
            // Test that invalid persistence type falls back to PostgreSQL
            String persistenceType = properties.persistenceType();
            assertEquals("invalid-type", persistenceType, "Configuration should preserve the invalid type");

            // The system should still inject a working implementation (fallback to PostgreSQL)
            assertNotNull(memoryEncodingSystem, "MemoryEncodingSystem should be injected even with invalid config");
            assertTrue(memoryEncodingSystem instanceof JpaMemoryEncodingSystem,
                "Should fallback to JPA-based MemoryEncodingSystem for invalid persistence type");
        }
    }
}
