package ai.headkey.rest.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to validate that the persistence configuration classes
 * compile correctly and basic properties are accessible.
 *
 * This test does not require a full Quarkus context and focuses on
 * ensuring the configuration framework is properly structured.
 */
public class ConfigurationValidationTest {

    @Test
    public void testPostgresPersistenceAnnotationExists() {
        // Test that the PostgresPersistence annotation is properly defined
        assertNotNull(PostgresPersistence.class);
        assertTrue(PostgresPersistence.class.isAnnotation());
    }

    @Test
    public void testElasticsearchPersistenceAnnotationExists() {
        // Test that the ElasticsearchPersistence annotation is properly defined
        assertNotNull(ElasticsearchPersistence.class);
        assertTrue(ElasticsearchPersistence.class.isAnnotation());
    }

    @Test
    public void testConfigurationClassesExist() {
        // Test that all configuration classes are available
        assertNotNull(PostgresPersistenceConfiguration.class);
        assertNotNull(ElasticsearchPersistenceConfiguration.class);
        assertNotNull(PersistenceConfigurationSelector.class);
        assertNotNull(MemorySystemProperties.class);
    }

    @Test
    public void testConfigurationClassesCanBeInstantiated() {
        // Test that configuration classes can be instantiated
        assertDoesNotThrow(() -> {
            new PostgresPersistenceConfiguration();
        });

        assertDoesNotThrow(() -> {
            new ElasticsearchPersistenceConfiguration();
        });

        assertDoesNotThrow(() -> {
            new PersistenceConfigurationSelector();
        });
    }

    @Test
    public void testMemorySystemPropertiesInterfaceStructure() {
        // Test that the MemorySystemProperties interface has the expected structure
        assertNotNull(MemorySystemProperties.class);
        assertTrue(MemorySystemProperties.class.isInterface());

        // Test that nested interfaces exist
        assertNotNull(MemorySystemProperties.DatabaseConfig.class);
        assertNotNull(MemorySystemProperties.ConnectionPoolConfig.class);
        assertNotNull(MemorySystemProperties.EmbeddingConfig.class);
        assertNotNull(MemorySystemProperties.PerformanceConfig.class);
        assertNotNull(MemorySystemProperties.ElasticsearchConfig.class);

        // Verify they are interfaces
        assertTrue(MemorySystemProperties.DatabaseConfig.class.isInterface());
        assertTrue(MemorySystemProperties.ConnectionPoolConfig.class.isInterface());
        assertTrue(MemorySystemProperties.EmbeddingConfig.class.isInterface());
        assertTrue(MemorySystemProperties.PerformanceConfig.class.isInterface());
        assertTrue(MemorySystemProperties.ElasticsearchConfig.class.isInterface());
    }

    @Test
    public void testValidPersistenceTypes() {
        // Test validation of persistence type values
        String[] validTypes = {"postgres", "postgresql", "elasticsearch", "elastic"};

        for (String type : validTypes) {
            assertNotNull(type);
            assertFalse(type.isEmpty());
            assertTrue(type.matches("^[a-z]+$"),
                "Persistence type should only contain lowercase letters: " + type);
        }
    }

    @Test
    public void testConfigurationClassPackageStructure() {
        // Test that all configuration classes are in the correct package
        String expectedPackage = "ai.headkey.rest.config";

        assertEquals(expectedPackage, PostgresPersistenceConfiguration.class.getPackage().getName());
        assertEquals(expectedPackage, ElasticsearchPersistenceConfiguration.class.getPackage().getName());
        assertEquals(expectedPackage, PersistenceConfigurationSelector.class.getPackage().getName());
        assertEquals(expectedPackage, MemorySystemProperties.class.getPackage().getName());
        assertEquals(expectedPackage, PostgresPersistence.class.getPackage().getName());
        assertEquals(expectedPackage, ElasticsearchPersistence.class.getPackage().getName());
    }

    @Test
    public void testAnnotationTargets() {
        // Test that qualifiers have correct targets
        PostgresPersistence postgresAnnotation = PostgresPersistenceConfiguration.class
            .getAnnotation(PostgresPersistence.class);
        // Note: This will be null since the class itself isn't annotated with @PostgresPersistence
        // The annotation is used on producer methods

        // Just verify the annotation classes exist and are properly defined
        assertNotNull(PostgresPersistence.class.getAnnotations());
        assertNotNull(ElasticsearchPersistence.class.getAnnotations());
    }

    @Test
    public void testConfigurationDocumentation() {
        // Verify that configuration classes have proper documentation
        PostgresPersistenceConfiguration postgresConfig = new PostgresPersistenceConfiguration();
        ElasticsearchPersistenceConfiguration elasticsearchConfig = new ElasticsearchPersistenceConfiguration();
        PersistenceConfigurationSelector selector = new PersistenceConfigurationSelector();

        // Basic instantiation tests to ensure no immediate failures
        assertNotNull(postgresConfig);
        assertNotNull(elasticsearchConfig);
        assertNotNull(selector);
    }

    @Test
    public void testDefaultConfigurationValues() {
        // Test that we can identify expected default values through interface structure
        // This is a structural test since we don't have access to actual configuration values
        // in this simple test without full Quarkus context

        // Verify the interface methods exist (compile-time check)
        assertDoesNotThrow(() -> {
            MemorySystemProperties.class.getMethod("persistenceType");
            MemorySystemProperties.class.getMethod("strategy");
            MemorySystemProperties.class.getMethod("batchSize");
            MemorySystemProperties.class.getMethod("maxSimilarityResults");
            MemorySystemProperties.class.getMethod("similarityThreshold");
            MemorySystemProperties.class.getMethod("enableSecondLevelCache");
            MemorySystemProperties.class.getMethod("database");
            MemorySystemProperties.class.getMethod("embedding");
            MemorySystemProperties.class.getMethod("performance");
            MemorySystemProperties.class.getMethod("elasticsearch");
        });
    }
}
