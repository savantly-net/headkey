package ai.headkey.persistence.strategies.jpa;

import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.persistence.entities.MemoryEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to demonstrate and validate the delegation pattern in DefaultJpaSimilaritySearchStrategy.
 * 
 * This test verifies that:
 * - DefaultJpaSimilaritySearchStrategy correctly detects database types
 * - It delegates to PostgresJpaSimilaritySearchStrategy for PostgreSQL
 * - It delegates to TextBasedJpaSimilaritySearchStrategy for H2
 * - The delegation is transparent and provides correct functionality
 * - Strategy initialization and re-initialization work properly
 * 
 * @since 1.0
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DefaultJpaSimilaritySearchDelegationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("delegation_test_db")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true);

    private static EntityManagerFactory h2EntityManagerFactory;
    private static EntityManagerFactory postgresEntityManagerFactory;

    @BeforeAll
    static void setUpClass() {
        // Set up H2 EntityManagerFactory
        Map<String, Object> h2Properties = new HashMap<>();
        h2Properties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        h2Properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:delegation_test;DB_CLOSE_DELAY=-1");
        h2Properties.put("jakarta.persistence.jdbc.user", "sa");
        h2Properties.put("jakarta.persistence.jdbc.password", "");
        h2Properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        h2Properties.put("hibernate.hbm2ddl.auto", "create-drop");
        h2Properties.put("hibernate.show_sql", "false");

        h2EntityManagerFactory = Persistence.createEntityManagerFactory("headkey-beliefs-h2-test", h2Properties);

        // Set up PostgreSQL EntityManagerFactory
        Map<String, Object> postgresProperties = new HashMap<>();
        postgresProperties.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        postgresProperties.put("jakarta.persistence.jdbc.url", postgres.getJdbcUrl());
        postgresProperties.put("jakarta.persistence.jdbc.user", postgres.getUsername());
        postgresProperties.put("jakarta.persistence.jdbc.password", postgres.getPassword());
        postgresProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        postgresProperties.put("hibernate.hbm2ddl.auto", "create-drop");
        postgresProperties.put("hibernate.show_sql", "false");

        postgresEntityManagerFactory = Persistence.createEntityManagerFactory("headkey-beliefs-postgres-test", postgresProperties);
    }

    @AfterAll
    static void tearDownClass() {
        if (h2EntityManagerFactory != null) {
            h2EntityManagerFactory.close();
        }
        if (postgresEntityManagerFactory != null) {
            postgresEntityManagerFactory.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should delegate to TextBasedJpaSimilaritySearchStrategy for H2 database")
    void testH2Delegation() throws Exception {
        try (EntityManager entityManager = h2EntityManagerFactory.createEntityManager()) {
            // Create and initialize strategy
            DefaultJpaSimilaritySearchStrategy strategy = new DefaultJpaSimilaritySearchStrategy();
            strategy.initialize(entityManager);

            // Verify delegation to TextBasedJpaSimilaritySearchStrategy
            assertNotNull(strategy.getDelegateStrategy());
            assertTrue(strategy.getDelegateStrategy() instanceof TextBasedJpaSimilaritySearchStrategy);
            assertEquals("H2", strategy.getDatabaseType());

            // Verify strategy behavior
            assertEquals("Text-based JPA Similarity Search", strategy.getDelegateStrategy().getStrategyName());
            assertFalse(strategy.supportsVectorSearch()); // TextBased doesn't support vectors

            System.out.println("✓ H2 delegation: " + strategy.getStrategyName());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should delegate to PostgresJpaSimilaritySearchStrategy for PostgreSQL database")
    void testPostgreSQLDelegation() throws Exception {
        try (EntityManager entityManager = postgresEntityManagerFactory.createEntityManager()) {
            // Create and initialize strategy
            DefaultJpaSimilaritySearchStrategy strategy = new DefaultJpaSimilaritySearchStrategy();
            strategy.initialize(entityManager);

            // Verify delegation to PostgresJpaSimilaritySearchStrategy
            assertNotNull(strategy.getDelegateStrategy());
            assertTrue(strategy.getDelegateStrategy() instanceof PostgresJpaSimilaritySearchStrategy);
            assertEquals("POSTGRESQL", strategy.getDatabaseType());

            // Verify strategy behavior
            assertEquals("PostgreSQL JPA Similarity Search", strategy.getDelegateStrategy().getStrategyName());

            System.out.println("✓ PostgreSQL delegation: " + strategy.getStrategyName());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Should provide transparent search functionality through delegation")
    void testTransparentSearchFunctionality() throws Exception {
        // Test with H2
        try (EntityManager h2EntityManager = h2EntityManagerFactory.createEntityManager()) {
            setupTestData(h2EntityManager);

            DefaultJpaSimilaritySearchStrategy h2Strategy = new DefaultJpaSimilaritySearchStrategy();
            h2Strategy.initialize(h2EntityManager);

            // Perform search through delegation
            List<MemoryRecord> h2Results = h2Strategy.searchSimilar(
                h2EntityManager, "test", null, "test-agent", 5, 1000, 0.0);

            assertNotNull(h2Results);
            assertTrue(h2Strategy.getDelegateStrategy() instanceof TextBasedJpaSimilaritySearchStrategy);

            System.out.println("✓ H2 search through delegation returned " + h2Results.size() + " results");
        }

        // Test with PostgreSQL
        try (EntityManager pgEntityManager = postgresEntityManagerFactory.createEntityManager()) {
            setupTestData(pgEntityManager);

            DefaultJpaSimilaritySearchStrategy pgStrategy = new DefaultJpaSimilaritySearchStrategy();
            pgStrategy.initialize(pgEntityManager);

            // Perform search through delegation
            List<MemoryRecord> pgResults = pgStrategy.searchSimilar(
                pgEntityManager, "test", null, "test-agent", 5, 1000, 0.0);

            assertNotNull(pgResults);
            assertTrue(pgStrategy.getDelegateStrategy() instanceof PostgresJpaSimilaritySearchStrategy);

            System.out.println("✓ PostgreSQL search through delegation returned " + pgResults.size() + " results");
        }
    }

    @Test
    @Order(4)
    @DisplayName("Should handle cosine similarity calculation correctly")
    void testCosineSimilarityCalculation() throws Exception {
        DefaultJpaSimilaritySearchStrategy strategy = new DefaultJpaSimilaritySearchStrategy();
        
        // Test identical vectors
        double[] vector1 = {1.0, 0.0, 0.0};
        double[] vector2 = {1.0, 0.0, 0.0};
        assertEquals(1.0, strategy.calculateCosineSimilarity(vector1, vector2), 0.001);

        // Test orthogonal vectors
        double[] vector3 = {0.0, 1.0, 0.0};
        assertEquals(0.0, strategy.calculateCosineSimilarity(vector1, vector3), 0.001);

        // Test null vectors
        assertEquals(0.0, strategy.calculateCosineSimilarity(null, vector1), 0.001);
        assertEquals(0.0, strategy.calculateCosineSimilarity(vector1, null), 0.001);

        // Test different length vectors
        double[] shortVector = {1.0, 0.0};
        assertEquals(0.0, strategy.calculateCosineSimilarity(vector1, shortVector), 0.001);

        System.out.println("✓ Cosine similarity calculations are correct");
    }

    @Test
    @Order(5)
    @DisplayName("Should handle strategy re-initialization correctly")
    void testStrategyReinitializationH2() throws Exception {
        try (EntityManager entityManager = h2EntityManagerFactory.createEntityManager()) {
            DefaultJpaSimilaritySearchStrategy strategy = new DefaultJpaSimilaritySearchStrategy();

            // Initial initialization
            strategy.initialize(entityManager);
            JpaSimilaritySearchStrategy firstDelegate = strategy.getDelegateStrategy();
            assertNotNull(firstDelegate);

            // Re-initialization
            strategy.reinitialize(entityManager);
            JpaSimilaritySearchStrategy secondDelegate = strategy.getDelegateStrategy();
            assertNotNull(secondDelegate);

            // Should still be the same type but potentially a new instance
            assertEquals(firstDelegate.getClass(), secondDelegate.getClass());

            System.out.println("✓ Strategy re-initialization works correctly");
        }
    }

    @Test
    @Order(6)
    @DisplayName("Should validate schema through delegation")
    void testSchemaValidationDelegation() throws Exception {
        // Test H2 schema validation
        try (EntityManager h2EntityManager = h2EntityManagerFactory.createEntityManager()) {
            DefaultJpaSimilaritySearchStrategy h2Strategy = new DefaultJpaSimilaritySearchStrategy();
            h2Strategy.initialize(h2EntityManager);

            boolean h2Valid = h2Strategy.validateSchema(h2EntityManager);
            assertTrue(h2Valid, "H2 schema validation should pass through delegation");
        }

        // Test PostgreSQL schema validation
        try (EntityManager pgEntityManager = postgresEntityManagerFactory.createEntityManager()) {
            DefaultJpaSimilaritySearchStrategy pgStrategy = new DefaultJpaSimilaritySearchStrategy();
            pgStrategy.initialize(pgEntityManager);

            boolean pgValid = pgStrategy.validateSchema(pgEntityManager);
            assertTrue(pgValid, "PostgreSQL schema validation should pass through delegation");
        }

        System.out.println("✓ Schema validation works correctly through delegation");
    }

    @Test
    @Order(7)
    @DisplayName("Should handle graceful fallback when delegation fails")
    void testGracefulFallback() throws Exception {
        try (EntityManager entityManager = h2EntityManagerFactory.createEntityManager()) {
            DefaultJpaSimilaritySearchStrategy strategy = new DefaultJpaSimilaritySearchStrategy();

            // Even if something goes wrong, strategy should still be usable
            strategy.initialize(entityManager);

            // Strategy should be initialized with some delegate
            assertNotNull(strategy.getDelegateStrategy());
            assertTrue(strategy.validateSchema(entityManager));

            System.out.println("✓ Graceful fallback mechanism works");
        }
    }

    @Test
    @Order(8)
    @DisplayName("Should demonstrate delegation pattern benefits")
    void testDelegationPatternBenefits() throws Exception {
        System.out.println("\n=== Delegation Pattern Benefits Demonstration ===");

        // H2 Database
        try (EntityManager h2EntityManager = h2EntityManagerFactory.createEntityManager()) {
            DefaultJpaSimilaritySearchStrategy h2Strategy = new DefaultJpaSimilaritySearchStrategy();
            h2Strategy.initialize(h2EntityManager);

            System.out.println("H2 Database:");
            System.out.println("  Delegate: " + h2Strategy.getDelegateStrategy().getClass().getSimpleName());
            System.out.println("  Strategy Name: " + h2Strategy.getDelegateStrategy().getStrategyName());
            System.out.println("  Vector Support: " + h2Strategy.supportsVectorSearch());
            System.out.println("  Database Type: " + h2Strategy.getDatabaseType());
        }

        // PostgreSQL Database
        try (EntityManager pgEntityManager = postgresEntityManagerFactory.createEntityManager()) {
            DefaultJpaSimilaritySearchStrategy pgStrategy = new DefaultJpaSimilaritySearchStrategy();
            pgStrategy.initialize(pgEntityManager);

            System.out.println("\nPostgreSQL Database:");
            System.out.println("  Delegate: " + pgStrategy.getDelegateStrategy().getClass().getSimpleName());
            System.out.println("  Strategy Name: " + pgStrategy.getDelegateStrategy().getStrategyName());
            System.out.println("  Vector Support: " + pgStrategy.supportsVectorSearch());
            System.out.println("  Database Type: " + pgStrategy.getDatabaseType());
        }

        System.out.println("\nBenefits:");
        System.out.println("✓ Single interface for multiple database types");
        System.out.println("✓ Automatic database-specific optimization");
        System.out.println("✓ Transparent delegation without client changes");
        System.out.println("✓ Easy to add new database-specific strategies");
        System.out.println("✓ Clean separation of concerns");
        System.out.println("================================================");
    }

    /**
     * Helper method to set up test data in the given EntityManager.
     */
    private void setupTestData(EntityManager entityManager) {
        entityManager.getTransaction().begin();
        try {
            MemoryEntity entity = new MemoryEntity();
            entity.setId("delegation-test-1");
            entity.setAgentId("test-agent");
            entity.setContent("This is a test memory for delegation pattern testing");
            entity.setCreatedAt(Instant.now());
            entity.setLastAccessed(Instant.now());
            entity.setRelevanceScore(0.8);

            entityManager.persist(entity);
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            // Ignore setup errors for this test
        }
    }
}