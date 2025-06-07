package ai.headkey.memory.implementations;

import ai.headkey.memory.interfaces.MemoryEncodingSystem;
import ai.headkey.memory.strategies.SimilaritySearchStrategy;
import ai.headkey.memory.strategies.HsqldbSimilaritySearchStrategy;
import ai.headkey.memory.strategies.PostgresSimilaritySearchStrategy;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Factory class for creating JDBC-based Memory Encoding Systems.
 * 
 * This factory provides convenient methods to create and configure
 * JDBC memory encoding systems for different database backends with
 * appropriate connection pooling and similarity search strategies.
 * 
 * Supports:
 * - PostgreSQL with pgvector for production use
 * - HSQLDB for testing and development
 * - Custom DataSource configurations
 * - Automatic strategy selection based on database type
 * 
 * @since 1.0
 */
public class JdbcMemorySystemFactory {
    
    /**
     * Creates a PostgreSQL-based memory encoding system for production use.
     * 
     * @param host Database host
     * @param port Database port
     * @param database Database name
     * @param username Database username
     * @param password Database password
     * @param embeddingGenerator Function to generate vector embeddings
     * @return Configured JdbcMemoryEncodingSystem
     */
    public static JdbcMemoryEncodingSystem createPostgreSQLSystem(
            String host, int port, String database, 
            String username, String password,
            JdbcMemoryEncodingSystem.VectorEmbeddingGenerator embeddingGenerator) {
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        
        // PostgreSQL-specific optimizations
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        
        // PostgreSQL connection properties
        Properties props = new Properties();
        props.setProperty("applicationName", "HeadKey-MemorySystem");
        props.setProperty("reWriteBatchedInserts", "true");
        props.setProperty("stringtype", "unspecified");
        config.setDataSourceProperties(props);
        
        HikariDataSource dataSource = new HikariDataSource(config);
        
        return new JdbcMemoryEncodingSystem(dataSource, embeddingGenerator, 
                                          new PostgresSimilaritySearchStrategy());
    }
    
    /**
     * Creates a PostgreSQL-based memory encoding system with custom connection pool settings.
     * 
     * @param jdbcUrl Full JDBC URL
     * @param username Database username
     * @param password Database password
     * @param maxPoolSize Maximum connection pool size
     * @param embeddingGenerator Function to generate vector embeddings
     * @return Configured JdbcMemoryEncodingSystem
     */
    public static JdbcMemoryEncodingSystem createPostgreSQLSystem(
            String jdbcUrl, String username, String password, int maxPoolSize,
            JdbcMemoryEncodingSystem.VectorEmbeddingGenerator embeddingGenerator) {
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(Math.max(1, maxPoolSize / 4));
        
        HikariDataSource dataSource = new HikariDataSource(config);
        
        return new JdbcMemoryEncodingSystem(dataSource, embeddingGenerator, 
                                          new PostgresSimilaritySearchStrategy());
    }
    
    /**
     * Creates an HSQLDB-based memory encoding system for testing and development.
     * 
     * @param databasePath Path for the HSQLDB files (use "mem:testdb" for in-memory)
     * @param embeddingGenerator Function to generate vector embeddings (can be null for testing)
     * @return Configured JdbcMemoryEncodingSystem
     */
    public static JdbcMemoryEncodingSystem createHSQLDBSystem(
            String databasePath,
            JdbcMemoryEncodingSystem.VectorEmbeddingGenerator embeddingGenerator) {
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:hsqldb:" + databasePath);
        config.setUsername("SA");
        config.setPassword("");
        config.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        
        // HSQLDB-specific settings
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(300000);
        
        // HSQLDB connection properties
        Properties props = new Properties();
        props.setProperty("shutdown", "true");
        config.setDataSourceProperties(props);
        
        HikariDataSource dataSource = new HikariDataSource(config);
        
        return new JdbcMemoryEncodingSystem(dataSource, embeddingGenerator, 
                                          new HsqldbSimilaritySearchStrategy());
    }
    
    /**
     * Creates an in-memory HSQLDB system for testing.
     * 
     * @param embeddingGenerator Function to generate vector embeddings (can be null for testing)
     * @return Configured JdbcMemoryEncodingSystem
     */
    public static JdbcMemoryEncodingSystem createInMemoryTestSystem(
            JdbcMemoryEncodingSystem.VectorEmbeddingGenerator embeddingGenerator) {
        return createHSQLDBSystem("mem:testdb", embeddingGenerator);
    }
    
    /**
     * Creates an in-memory HSQLDB system for testing without vector embeddings.
     * 
     * @return Configured JdbcMemoryEncodingSystem for testing
     */
    public static JdbcMemoryEncodingSystem createInMemoryTestSystem() {
        return createInMemoryTestSystem(null);
    }
    
    /**
     * Creates a memory encoding system from an existing DataSource.
     * Automatically detects the database type and selects appropriate strategy.
     * 
     * @param dataSource Existing DataSource
     * @param embeddingGenerator Function to generate vector embeddings
     * @return Configured JdbcMemoryEncodingSystem
     */
    public static JdbcMemoryEncodingSystem createFromDataSource(
            DataSource dataSource,
            JdbcMemoryEncodingSystem.VectorEmbeddingGenerator embeddingGenerator) {
        
        return new JdbcMemoryEncodingSystem(dataSource, embeddingGenerator);
    }
    
    /**
     * Creates a memory encoding system with a custom similarity search strategy.
     * 
     * @param dataSource Database data source
     * @param embeddingGenerator Function to generate vector embeddings
     * @param similarityStrategy Custom similarity search strategy
     * @return Configured JdbcMemoryEncodingSystem
     */
    public static JdbcMemoryEncodingSystem createCustomSystem(
            DataSource dataSource,
            JdbcMemoryEncodingSystem.VectorEmbeddingGenerator embeddingGenerator,
            SimilaritySearchStrategy similarityStrategy) {
        
        return new JdbcMemoryEncodingSystem(dataSource, embeddingGenerator, similarityStrategy);
    }
    
    /**
     * Configuration builder for more complex setups.
     */
    public static class Builder {
        private String jdbcUrl;
        private String username;
        private String password;
        private String driverClassName;
        private int maxPoolSize = 10;
        private int minIdle = 2;
        private long connectionTimeout = 30000;
        private long idleTimeout = 600000;
        private long maxLifetime = 1800000;
        private Properties connectionProperties = new Properties();
        private JdbcMemoryEncodingSystem.VectorEmbeddingGenerator embeddingGenerator;
        private SimilaritySearchStrategy similarityStrategy;
        
        public Builder jdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
            return this;
        }
        
        public Builder credentials(String username, String password) {
            this.username = username;
            this.password = password;
            return this;
        }
        
        public Builder driverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
            return this;
        }
        
        public Builder poolSize(int maxPoolSize, int minIdle) {
            this.maxPoolSize = maxPoolSize;
            this.minIdle = minIdle;
            return this;
        }
        
        public Builder timeouts(long connectionTimeout, long idleTimeout, long maxLifetime) {
            this.connectionTimeout = connectionTimeout;
            this.idleTimeout = idleTimeout;
            this.maxLifetime = maxLifetime;
            return this;
        }
        
        public Builder connectionProperty(String key, String value) {
            this.connectionProperties.setProperty(key, value);
            return this;
        }
        
        public Builder embeddingGenerator(JdbcMemoryEncodingSystem.VectorEmbeddingGenerator embeddingGenerator) {
            this.embeddingGenerator = embeddingGenerator;
            return this;
        }
        
        public Builder similarityStrategy(SimilaritySearchStrategy similarityStrategy) {
            this.similarityStrategy = similarityStrategy;
            return this;
        }
        
        /**
         * Builds the JdbcMemoryEncodingSystem with the configured settings.
         * 
         * @return Configured JdbcMemoryEncodingSystem
         * @throws IllegalStateException if required configuration is missing
         */
        public JdbcMemoryEncodingSystem build() {
            if (jdbcUrl == null) {
                throw new IllegalStateException("JDBC URL is required");
            }
            
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            
            if (username != null) {
                config.setUsername(username);
            }
            if (password != null) {
                config.setPassword(password);
            }
            if (driverClassName != null) {
                config.setDriverClassName(driverClassName);
            }
            
            config.setMaximumPoolSize(maxPoolSize);
            config.setMinimumIdle(minIdle);
            config.setConnectionTimeout(connectionTimeout);
            config.setIdleTimeout(idleTimeout);
            config.setMaxLifetime(maxLifetime);
            
            if (!connectionProperties.isEmpty()) {
                config.setDataSourceProperties(connectionProperties);
            }
            
            HikariDataSource dataSource = new HikariDataSource(config);
            
            if (similarityStrategy != null) {
                return new JdbcMemoryEncodingSystem(dataSource, embeddingGenerator, similarityStrategy);
            } else {
                return new JdbcMemoryEncodingSystem(dataSource, embeddingGenerator);
            }
        }
    }
    
    /**
     * Creates a new configuration builder.
     * 
     * @return New Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Validates database connectivity and schema.
     * 
     * @param memorySystem The memory system to validate
     * @return true if the system is properly configured and accessible
     */
    public static boolean validateSystem(MemoryEncodingSystem memorySystem) {
        try {
            return memorySystem.isHealthy();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Utility method to detect database type from a DataSource.
     * 
     * @param dataSource DataSource to check
     * @return Database type string ("postgresql", "hsqldb", or "unknown")
     */
    public static String detectDatabaseType(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            String databaseProductName = conn.getMetaData().getDatabaseProductName().toLowerCase();
            
            if (databaseProductName.contains("postgresql")) {
                return "postgresql";
            } else if (databaseProductName.contains("hsql")) {
                return "hsqldb";
            } else {
                return "unknown";
            }
        } catch (SQLException e) {
            return "unknown";
        }
    }
    
    /**
     * Creates a simple mock embedding generator for testing purposes.
     * 
     * @param dimension The dimension of the mock vectors
     * @return Mock embedding generator that creates random vectors
     */
    public static JdbcMemoryEncodingSystem.VectorEmbeddingGenerator createMockEmbeddingGenerator(int dimension) {
        return content -> {
            double[] vector = new double[dimension];
            // Create a simple hash-based vector for consistent results
            int hash = content.hashCode();
            for (int i = 0; i < dimension; i++) {
                vector[i] = ((hash + i) % 1000) / 1000.0;
            }
            return vector;
        };
    }
    
    /**
     * Creates a simple mock embedding generator with default dimension (1536 for OpenAI).
     * 
     * @return Mock embedding generator
     */
    public static JdbcMemoryEncodingSystem.VectorEmbeddingGenerator createMockEmbeddingGenerator() {
        return createMockEmbeddingGenerator(1536);
    }
}