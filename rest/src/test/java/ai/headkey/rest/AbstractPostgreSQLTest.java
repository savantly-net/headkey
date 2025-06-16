package ai.headkey.rest;

import ai.headkey.memory.interfaces.BeliefStorageService;
import ai.headkey.persistence.factory.JpaBeliefStorageServiceFactory;
import io.quarkus.test.junit.QuarkusTestProfile;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for PostgreSQL integration tests using TestContainers.
 * 
 * This class provides:
 * - PostgreSQL TestContainer setup and teardown
 * - Properly configured DataSource with connection pooling
 * - EntityManagerFactory with correct PostgreSQL configuration
 * - BeliefStorageService factory methods for testing
 * - Database schema initialization
 * 
 * Test classes should extend this class to get access to a fully configured
 * PostgreSQL test environment.
 * 
 * Features:
 * - Automatic schema creation and cleanup
 * - Connection pooling with HikariCP
 * - Proper transaction management for tests
 * - Database isolation between test methods
 * - Performance optimized for test execution
 * 
 * @since 1.0
 */
@Testcontainers
public abstract class AbstractPostgreSQLTest implements QuarkusTestProfile {

    public Map<String, String> getConfigOverrides() {
         return Map.of(
            "headkey.memory.persistence-type", "postgres"
        );
    }

    /**
     * PostgreSQL TestContainer with test database configuration.
     * Uses PGVector extension for vector support.
     */
    @Container
    protected static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg17")
            .withDatabaseName("headkey_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true) // Reuse container across test classes for performance
            .withCommand("postgres", "-c", "fsync=off") // Faster for testing
            .withTmpFs(Map.of("/var/lib/postgresql/data", "rw")); // Use tmpfs for performance

    protected static DataSource dataSource;
    protected static EntityManagerFactory entityManagerFactory;
    protected static BeliefStorageService beliefStorageService;

    @BeforeAll
    static void setUpDatabase() {
        // Wait for container to be ready
        postgres.start();

        // Create HikariCP DataSource with optimized test settings
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setDriverClassName("org.postgresql.Driver");
        
        // Optimized for testing
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(900000);
        config.setLeakDetectionThreshold(30000);
        
        // Test-specific settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "100");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "1024");
        
        dataSource = new HikariDataSource(config);

        // Create EntityManagerFactory with PostgreSQL-specific configuration
        entityManagerFactory = createTestEntityManagerFactory();

        // Create BeliefStorageService for testing
        beliefStorageService = createTestBeliefStorageService();
    }

    @AfterAll
    static void tearDownDatabase() {
        if (beliefStorageService != null) {
            // Cleanup service resources
        }
        
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
        
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }
        
        // Container will be stopped automatically by TestContainers
    }

    /**
     * Creates an EntityManagerFactory configured for PostgreSQL testing.
     * 
     * @return Configured EntityManagerFactory
     */
    private static EntityManagerFactory createTestEntityManagerFactory() {
        Map<String, Object> properties = new HashMap<>();
        
        // Database connection settings
        properties.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        properties.put("jakarta.persistence.jdbc.url", postgres.getJdbcUrl());
        properties.put("jakarta.persistence.jdbc.user", postgres.getUsername());
        properties.put("jakarta.persistence.jdbc.password", postgres.getPassword());
        
        // Hibernate PostgreSQL settings
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.hbm2ddl.auto", "create-drop"); // Auto-create schema for tests
        properties.put("hibernate.show_sql", "false"); // Set to true for debugging
        properties.put("hibernate.format_sql", "false");
        properties.put("hibernate.generate_statistics", "false");
        
        // Performance settings for testing
        properties.put("hibernate.jdbc.batch_size", "20");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.batch_versioned_data", "true");
        
        // Connection pool settings (using external DataSource)
        properties.put("hibernate.connection.provider_disables_autocommit", "true");
        
        // Cache settings (disabled for testing isolation)
        properties.put("hibernate.cache.use_second_level_cache", "false");
        properties.put("hibernate.cache.use_query_cache", "false");
        
        // Validation settings
        properties.put("hibernate.check_nullability", "true");
        
        return Persistence.createEntityManagerFactory("headkey-beliefs-postgres-test", properties);
    }

    /**
     * Creates a BeliefStorageService configured for PostgreSQL testing.
     * 
     * @return Configured BeliefStorageService
     */
    private static BeliefStorageService createTestBeliefStorageService() {
        return JpaBeliefStorageServiceFactory.builder()
                .withEntityManagerFactory(entityManagerFactory)
                .build();
    }

    /**
     * Creates a new EntityManager for test methods that need transaction control.
     * 
     * @return New EntityManager instance
     */
    protected EntityManager createEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    /**
     * Executes a test operation within a transaction and rolls it back.
     * Useful for tests that need to modify data but maintain isolation.
     * 
     * @param operation The operation to execute
     */
    protected void executeInTransaction(Runnable operation) {
        EntityManager em = createEntityManager();
        try {
            em.getTransaction().begin();
            operation.run();
            em.getTransaction().rollback(); // Always rollback for test isolation
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Executes a test operation within a transaction and commits it.
     * Use sparingly and ensure proper cleanup in test teardown.
     * 
     * @param operation The operation to execute
     */
    protected void executeAndCommit(Runnable operation) {
        EntityManager em = createEntityManager();
        try {
            em.getTransaction().begin();
            operation.run();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Cleans up test data from all tables.
     * Should be called between tests if needed for isolation.
     */
    protected void cleanupTestData() {
        EntityManager em = createEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery("DELETE FROM belief_tags").executeUpdate();
            em.createNativeQuery("DELETE FROM belief_evidence_memories").executeUpdate();
            em.createNativeQuery("DELETE FROM conflict_belief_ids").executeUpdate();
            em.createNativeQuery("DELETE FROM belief_conflicts").executeUpdate();
            em.createNativeQuery("DELETE FROM beliefs").executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            // Ignore cleanup errors in tests
        } finally {
            em.close();
        }
    }

    /**
     * Gets the JDBC URL for the test PostgreSQL container.
     * 
     * @return JDBC URL
     */
    protected static String getJdbcUrl() {
        return postgres.getJdbcUrl();
    }

    /**
     * Gets the test database username.
     * 
     * @return Database username
     */
    protected static String getUsername() {
        return postgres.getUsername();
    }

    /**
     * Gets the test database password.
     * 
     * @return Database password
     */
    protected static String getPassword() {
        return postgres.getPassword();
    }

    /**
     * Gets the configured DataSource for advanced test scenarios.
     * 
     * @return Test DataSource
     */
    protected static DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Gets the EntityManagerFactory for advanced test scenarios.
     * 
     * @return Test EntityManagerFactory
     */
    protected static EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    /**
     * Gets the configured BeliefStorageService for testing.
     * 
     * @return Test BeliefStorageService
     */
    protected static BeliefStorageService getBeliefStorageService() {
        return beliefStorageService;
    }
}