package ai.headkey.persistence.factory;

import ai.headkey.memory.spi.BeliefExtractionService;
import ai.headkey.memory.spi.BeliefStorageService;
import ai.headkey.persistence.repositories.BeliefRepository;
import ai.headkey.persistence.repositories.BeliefConflictRepository;
import ai.headkey.persistence.repositories.impl.JpaBeliefRepository;
import ai.headkey.persistence.repositories.impl.JpaBeliefConflictRepository;
import ai.headkey.persistence.services.JpaBeliefStorageService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Factory for creating JPA-based BeliefStorageService instances.
 * 
 * This factory provides various creation methods for JPA belief storage services
 * with different configuration options:
 * - Default PostgreSQL configuration
 * - Custom EntityManagerFactory
 * - Custom DataSource configuration
 * - Testing configuration with H2
 * - Production configuration with connection pooling
 * 
 * The factory handles the setup of:
 * - EntityManagerFactory configuration
 * - Repository implementations
 * - Transaction management
 * - Connection pooling
 * - Database schema management
 * 
 * @since 1.0
 */
public class JpaBeliefStorageServiceFactory {

    /**
     * Configuration builder for JPA belief storage service.
     */
    public static class Builder {
        private EntityManagerFactory entityManagerFactory;
        private DataSource dataSource;
        private Map<String, Object> jpaProperties = new HashMap<>();
        private String persistenceUnitName = "headkey-beliefs-postgresql";
        private boolean autoCreateSchema = true;
        private boolean enableStatistics = true;

        public Builder withEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
            this.entityManagerFactory = Objects.requireNonNull(entityManagerFactory, "EntityManagerFactory cannot be null");
            return this;
        }

        public Builder withDataSource(DataSource dataSource) {
            this.dataSource = Objects.requireNonNull(dataSource, "DataSource cannot be null");
            return this;
        }

        public Builder withPersistenceUnitName(String persistenceUnitName) {
            this.persistenceUnitName = Objects.requireNonNull(persistenceUnitName, "Persistence unit name cannot be null");
            return this;
        }

        public Builder withJpaProperty(String key, Object value) {
            if (key != null && !key.trim().isEmpty()) {
                this.jpaProperties.put(key, value);
            }
            return this;
        }

        public Builder withJpaProperties(Map<String, Object> properties) {
            if (properties != null) {
                this.jpaProperties.putAll(properties);
            }
            return this;
        }

        public Builder withAutoCreateSchema(boolean autoCreateSchema) {
            this.autoCreateSchema = autoCreateSchema;
            return this;
        }

        public Builder withStatistics(boolean enableStatistics) {
            this.enableStatistics = enableStatistics;
            return this;
        }

        public BeliefStorageService build() {
            EntityManagerFactory emf = entityManagerFactory;
            
            if (emf == null) {
                emf = createEntityManagerFactory();
            }

            // Create repositories
            BeliefRepository beliefRepository = new JpaBeliefRepository();
            BeliefConflictRepository conflictRepository = new JpaBeliefConflictRepository();

            // Create and return service
            return new JpaBeliefStorageService(beliefRepository, conflictRepository);
        }

        private EntityManagerFactory createEntityManagerFactory() {
            Map<String, Object> properties = new HashMap<>(jpaProperties);
            
            // Set default properties if not provided
            if (dataSource != null) {
                properties.put("jakarta.persistence.nonJtaDataSource", dataSource);
            }
            
            // Schema management
            if (autoCreateSchema) {
                properties.putIfAbsent("jakarta.persistence.schema-generation.database.action", "create");
                properties.putIfAbsent("hibernate.hbm2ddl.auto", "update");
            }
            
            // Performance and statistics
            if (enableStatistics) {
                properties.putIfAbsent("hibernate.generate_statistics", "true");
                properties.putIfAbsent("hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS", "100");
            }
            
            // Default Hibernate properties
            properties.putIfAbsent("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            properties.putIfAbsent("hibernate.show_sql", "false");
            properties.putIfAbsent("hibernate.format_sql", "false");
            properties.putIfAbsent("hibernate.use_sql_comments", "true");
            properties.putIfAbsent("hibernate.jdbc.batch_size", "50");
            properties.putIfAbsent("hibernate.order_inserts", "true");
            properties.putIfAbsent("hibernate.order_updates", "true");
            properties.putIfAbsent("hibernate.jdbc.batch_versioned_data", "true");
            
            return Persistence.createEntityManagerFactory(persistenceUnitName, properties);
        }
    }

    // ========== Factory Methods ==========

    /**
     * Creates a JPA belief storage service with default PostgreSQL configuration.
     * 
     * @return A configured JPA belief storage service
     */
    public static BeliefStorageService createDefault() {
        return builder().build();
    }

    /**
     * Creates a JPA belief storage service with custom EntityManagerFactory.
     * 
     * @param entityManagerFactory The EntityManagerFactory to use
     * @return A configured JPA belief storage service
     */
    public static BeliefStorageService create(EntityManagerFactory entityManagerFactory) {
        return builder()
            .withEntityManagerFactory(entityManagerFactory)
            .build();
    }

    /**
     * Creates a JPA belief storage service with custom DataSource.
     * 
     * @param dataSource The DataSource to use
     * @return A configured JPA belief storage service
     */
    public static BeliefStorageService create(DataSource dataSource) {
        return builder()
            .withDataSource(dataSource)
            .build();
    }

    /**
     * Creates a JPA belief storage service for production use.
     * 
     * This configuration includes:
     * - Connection pooling optimization
     * - Performance monitoring
     * - Production-grade settings
     * - Schema validation
     * 
     * @param dataSource The production DataSource
     * @return A production-configured JPA belief storage service
     */
    public static BeliefStorageService createForProduction(DataSource dataSource) {
        Map<String, Object> productionProperties = new HashMap<>();
        
        // Production Hibernate settings
        productionProperties.put("hibernate.show_sql", "false");
        productionProperties.put("hibernate.format_sql", "false");
        productionProperties.put("hibernate.generate_statistics", "true");
        productionProperties.put("hibernate.hbm2ddl.auto", "validate");
        productionProperties.put("hibernate.jdbc.batch_size", "100");
        productionProperties.put("hibernate.order_inserts", "true");
        productionProperties.put("hibernate.order_updates", "true");
        productionProperties.put("hibernate.jdbc.batch_versioned_data", "true");
        productionProperties.put("hibernate.query.plan_cache_max_size", "2048");
        productionProperties.put("hibernate.query.plan_parameter_metadata_max_size", "128");
        
        // Connection pool settings (if using Hibernate's built-in pool)
        productionProperties.put("hibernate.hikari.minimumIdle", "5");
        productionProperties.put("hibernate.hikari.maximumPoolSize", "20");
        productionProperties.put("hibernate.hikari.idleTimeout", "300000");
        productionProperties.put("hibernate.hikari.connectionTimeout", "30000");
        productionProperties.put("hibernate.hikari.leakDetectionThreshold", "60000");
        
        return builder()
            .withDataSource(dataSource)
            .withJpaProperties(productionProperties)
            .withAutoCreateSchema(false)
            .withStatistics(true)
            .build();
    }

    /**
     * Creates a JPA belief storage service for development use.
     * 
     * This configuration includes:
     * - SQL logging enabled
     * - Auto schema creation
     * - Development-friendly settings
     * 
     * @param dataSource The development DataSource
     * @return A development-configured JPA belief storage service
     */
    public static BeliefStorageService createForDevelopment(DataSource dataSource) {
        Map<String, Object> devProperties = new HashMap<>();
        
        // Development Hibernate settings
        devProperties.put("hibernate.show_sql", "true");
        devProperties.put("hibernate.format_sql", "true");
        devProperties.put("hibernate.use_sql_comments", "true");
        devProperties.put("hibernate.generate_statistics", "true");
        devProperties.put("hibernate.hbm2ddl.auto", "update");
        devProperties.put("hibernate.jdbc.batch_size", "20");
        
        return builder()
            .withDataSource(dataSource)
            .withJpaProperties(devProperties)
            .withAutoCreateSchema(true)
            .withStatistics(true)
            .build();
    }

    /**
     * Creates a JPA belief storage service for testing with H2 database.
     * 
     * This configuration includes:
     * - In-memory H2 database
     * - Auto schema creation
     * - Fast test execution settings
     * 
     * @return A test-configured JPA belief storage service
     */
    public static BeliefStorageService createForTesting() {
        Map<String, Object> testProperties = new HashMap<>();
        
        // H2 test database settings
        testProperties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        testProperties.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        testProperties.put("jakarta.persistence.jdbc.user", "sa");
        testProperties.put("jakarta.persistence.jdbc.password", "");
        
        // Test Hibernate settings
        testProperties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        testProperties.put("hibernate.hbm2ddl.auto", "create-drop");
        testProperties.put("hibernate.show_sql", "false");
        testProperties.put("hibernate.format_sql", "false");
        testProperties.put("hibernate.generate_statistics", "false");
        testProperties.put("hibernate.jdbc.batch_size", "10");
        
        return builder()
            .withPersistenceUnitName("headkey-beliefs-test")
            .withJpaProperties(testProperties)
            .withAutoCreateSchema(true)
            .withStatistics(false)
            .build();
    }

    /**
     * Creates a JPA belief storage service with PostgreSQL connection URL.
     * 
     * @param jdbcUrl The PostgreSQL JDBC URL
     * @param username The database username
     * @param password The database password
     * @return A configured JPA belief storage service
     */
    public static BeliefStorageService createWithUrl(String jdbcUrl, String username, String password) {
        Objects.requireNonNull(jdbcUrl, "JDBC URL cannot be null");
        Objects.requireNonNull(username, "Username cannot be null");
        Objects.requireNonNull(password, "Password cannot be null");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        properties.put("jakarta.persistence.jdbc.url", jdbcUrl);
        properties.put("jakarta.persistence.jdbc.user", username);
        properties.put("jakarta.persistence.jdbc.password", password);
        
        return builder()
            .withJpaProperties(properties)
            .build();
    }

    /**
     * Creates a builder for custom configuration.
     * 
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    // ========== Utility Methods ==========

    /**
     * Validates that a JPA belief storage service is properly configured.
     * 
     * @param service The service to validate
     * @throws IllegalStateException if the service is not properly configured
     */
    public static void validateService(BeliefStorageService service) {
        Objects.requireNonNull(service, "Service cannot be null");
        
        if (!service.isHealthy()) {
            throw new IllegalStateException("JPA belief storage service failed health check");
        }
        
        Map<String, Object> serviceInfo = service.getServiceInfo();
        if (!"JpaBeliefStorageService".equals(serviceInfo.get("serviceType"))) {
            throw new IllegalStateException("Service is not a JpaBeliefStorageService");
        }
    }

    /**
     * Gets default JPA properties for PostgreSQL.
     * 
     * @return Map of default JPA properties
     */
    public static Map<String, Object> getDefaultPostgreSQLProperties() {
        Map<String, Object> properties = new HashMap<>();
        
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.show_sql", "false");
        properties.put("hibernate.format_sql", "false");
        properties.put("hibernate.use_sql_comments", "true");
        properties.put("hibernate.jdbc.batch_size", "50");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.batch_versioned_data", "true");
        properties.put("hibernate.generate_statistics", "true");
        
        return properties;
    }

    /**
     * Gets optimized JPA properties for high-performance scenarios.
     * 
     * @return Map of performance-optimized JPA properties
     */
    public static Map<String, Object> getHighPerformanceProperties() {
        Map<String, Object> properties = getDefaultPostgreSQLProperties();
        
        // Enhanced performance settings
        properties.put("hibernate.jdbc.batch_size", "100");
        properties.put("hibernate.jdbc.fetch_size", "50");
        properties.put("hibernate.query.plan_cache_max_size", "4096");
        properties.put("hibernate.query.plan_parameter_metadata_max_size", "256");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.batch_versioned_data", "true");
        properties.put("hibernate.connection.provider_disables_autocommit", "true");
        
        return properties;
    }

    /**
     * Creates a DataSource configuration for PostgreSQL.
     * 
     * @param host The database host
     * @param port The database port
     * @param database The database name
     * @param username The username
     * @param password The password
     * @return JDBC URL for PostgreSQL
     */
    public static String createPostgreSQLUrl(String host, int port, String database, String username, String password) {
        Objects.requireNonNull(host, "Host cannot be null");
        Objects.requireNonNull(database, "Database cannot be null");
        
        return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
    }

    // Private constructor to prevent instantiation
    private JpaBeliefStorageServiceFactory() {
        throw new UnsupportedOperationException("Factory class cannot be instantiated");
    }
}