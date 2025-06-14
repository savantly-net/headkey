package ai.headkey.persistence.strategies.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import org.jboss.logging.Logger;

/**
 * Factory for creating appropriate JPA similarity search strategies based on database capabilities.
 * 
 * This factory automatically detects the underlying database type and available extensions
 * to select the most optimal similarity search strategy.
 * 
 * Supported strategies:
 * - PostgresJpaSimilaritySearchStrategy: For PostgreSQL with pgvector/pg_trgm extensions
 * - TextBasedJpaSimilaritySearchStrategy: For databases without vector support
 * - DefaultJpaSimilaritySearchStrategy: Fallback strategy with basic vector support
 * 
 * @since 1.0
 */
public class JpaSimilaritySearchStrategyFactory {

    private static final Logger log = Logger.getLogger(JpaSimilaritySearchStrategyFactory.class);
    
    /**
     * Creates the most appropriate similarity search strategy for the given EntityManager.
     * 
     * @param entityManager EntityManager to analyze for database capabilities
     * @return The most suitable JpaSimilaritySearchStrategy implementation
     */
    public static JpaSimilaritySearchStrategy createStrategy(EntityManager entityManager) {
        log.info("Creating JPA similarity search strategy based on database capabilities");
        try {
            DatabaseType dbType = detectDatabaseType(entityManager);
            
            switch (dbType) {
                case POSTGRESQL:
                    return createPostgreSQLStrategy(entityManager);
                default:
                    return new DefaultJpaSimilaritySearchStrategy();
            }
        } catch (Exception e) {
            // Fallback to default strategy if detection fails
            return new DefaultJpaSimilaritySearchStrategy();
        }
    }
    
    /**
     * Creates a strategy for the specified database type.
     * 
     * @param entityManager EntityManager instance
     * @param databaseType Specific database type to create strategy for
     * @return JpaSimilaritySearchStrategy for the specified database type
     */
    public static JpaSimilaritySearchStrategy createStrategy(EntityManager entityManager, DatabaseType databaseType) {
        log.infof("Creating JPA similarity search strategy for database type: {}", databaseType);
        switch (databaseType) {
            case POSTGRESQL:
                return createPostgreSQLStrategy(entityManager);
            case H2:
            case HSQLDB:
            case MYSQL:
            case MARIADB:
                return new TextBasedJpaSimilaritySearchStrategy();
            case UNKNOWN:
            default:
                return new DefaultJpaSimilaritySearchStrategy();
        }
    }
    
    /**
     * Creates a PostgreSQL-specific strategy with proper initialization.
     */
    private static JpaSimilaritySearchStrategy createPostgreSQLStrategy(EntityManager entityManager) {
        log.info("Creating PostgreSQL JPA similarity search strategy");
        PostgresJpaSimilaritySearchStrategy strategy = new PostgresJpaSimilaritySearchStrategy();
        try {
            strategy.initialize(entityManager);
        } catch (Exception e) {
            log.warn("Failed to initialize PostgreSQL JPA similarity search strategy, falling back to text-based strategy", e);
            // If PostgreSQL strategy fails to initialize, fall back to text-based strategy
            return new TextBasedJpaSimilaritySearchStrategy();
        }
        return strategy;
    }
    
    /**
     * Detects the database type from the EntityManager.
     */
    private static DatabaseType detectDatabaseType(EntityManager entityManager) {
        log.info("Detecting database type from EntityManager");
        try {
            // Get the underlying connection to check database type
            Connection connection = entityManager.unwrap(Connection.class);
            if (connection != null) {
                DatabaseMetaData metaData = connection.getMetaData();
                String productName = metaData.getDatabaseProductName().toLowerCase();
                
                if (productName.contains("postgresql")) {
                    return DatabaseType.POSTGRESQL;
                } else if (productName.contains("h2")) {
                    return DatabaseType.H2;
                } else if (productName.contains("hsql")) {
                    return DatabaseType.HSQLDB;
                } else if (productName.contains("mysql")) {
                    return DatabaseType.MYSQL;
                } else if (productName.contains("mariadb")) {
                    return DatabaseType.MARIADB;
                }
            }
        } catch (Exception e) {
            // Fallback detection using dialect-specific queries
            return detectDatabaseTypeByDialect(entityManager);
        }
        
        return DatabaseType.UNKNOWN;
    }
    
    /**
     * Fallback database detection using dialect-specific queries.
     */
    private static DatabaseType detectDatabaseTypeByDialect(EntityManager entityManager) {
        try {
            // Try PostgreSQL-specific query
            Query pgQuery = entityManager.createNativeQuery("SELECT version()");
            Object result = pgQuery.getSingleResult();
            if (result != null && result.toString().toLowerCase().contains("postgresql")) {
                return DatabaseType.POSTGRESQL;
            }
        } catch (Exception e) {
            // Not PostgreSQL, continue testing
        }
        
        try {
            // Try H2-specific query
            Query h2Query = entityManager.createNativeQuery("SELECT H2VERSION()");
            h2Query.getSingleResult();
            return DatabaseType.H2;
        } catch (Exception e) {
            // Not H2, continue testing
        }
        
        try {
            // Try HSQLDB-specific query
            Query hsqlQuery = entityManager.createNativeQuery("SELECT * FROM INFORMATION_SCHEMA.SYSTEM_PROPERTIES WHERE PROPERTY_NAME = 'hsqldb.version'");
            hsqlQuery.getResultList();
            return DatabaseType.HSQLDB;
        } catch (Exception e) {
            // Not HSQLDB, continue testing
        }
        
        try {
            // Try MySQL-specific query
            Query mysqlQuery = entityManager.createNativeQuery("SELECT @@version");
            Object result = mysqlQuery.getSingleResult();
            if (result != null && result.toString().toLowerCase().contains("mysql")) {
                return DatabaseType.MYSQL;
            }
        } catch (Exception e) {
            // Not MySQL
        }
        
        return DatabaseType.UNKNOWN;
    }
    
    /**
     * Validates that a strategy is compatible with the current database.
     * 
     * @param strategy Strategy to validate
     * @param entityManager EntityManager to validate against
     * @return true if the strategy is compatible
     */
    public static boolean validateStrategy(JpaSimilaritySearchStrategy strategy, EntityManager entityManager) {
        try {
            return strategy.validateSchema(entityManager);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets information about the current database capabilities.
     * 
     * @param entityManager EntityManager to analyze
     * @return DatabaseCapabilities object with feature information
     */
    public static DatabaseCapabilities analyzeDatabaseCapabilities(EntityManager entityManager) {
        log.info("Analyzing database capabilities for similarity search");
        DatabaseType dbType = detectDatabaseType(entityManager);
        boolean vectorSupport = false;
        boolean fullTextSupport = false;
        String version = "Unknown";
        
        try {
            switch (dbType) {
                case POSTGRESQL:
                    vectorSupport = checkPostgreSQLVectorSupport(entityManager);
                    fullTextSupport = checkPostgreSQLFullTextSupport(entityManager);
                    version = getPostgreSQLVersion(entityManager);
                    break;
                case H2:
                case HSQLDB:
                case MYSQL:
                case MARIADB:
                    fullTextSupport = true; // Basic text search is always available
                    break;
            }
        } catch (Exception e) {
            // Ignore errors during capability detection
        }
        
        return new DatabaseCapabilities(dbType, vectorSupport, fullTextSupport, version);
    }
    
    private static boolean checkPostgreSQLVectorSupport(EntityManager entityManager) {
        log.debug("Checking PostgreSQL vector support");
        try {
            Query query = entityManager.createNativeQuery("SELECT 1 FROM pg_extension WHERE extname = 'vector'");
            query.getSingleResult();
            log.info("PostgreSQL vector support is enabled");
            return true;
        } catch (Exception e) {
            log.warn("PostgreSQL vector support is not enabled", e);
            return false;
        }
    }
    
    private static boolean checkPostgreSQLFullTextSupport(EntityManager entityManager) {
        log.debug("Checking PostgreSQL full-text search support");
        try {
            Query query = entityManager.createNativeQuery("SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm'");
            query.getSingleResult();
            log.info("PostgreSQL full-text search support is enabled");
            return true;
        } catch (Exception e) {
            log.warn("PostgreSQL full-text search support is not enabled", e);
            return false;
        }
    }
    
    private static String getPostgreSQLVersion(EntityManager entityManager) {
        try {
            Query query = entityManager.createNativeQuery("SELECT version()");
            return query.getSingleResult().toString();
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * Enumeration of supported database types.
     */
    public enum DatabaseType {
        POSTGRESQL,
        H2,
        HSQLDB,
        MYSQL,
        MARIADB,
        UNKNOWN
    }
    
    /**
     * Information about database capabilities for similarity search.
     */
    public static class DatabaseCapabilities {
        private final DatabaseType databaseType;
        private final boolean vectorSupport;
        private final boolean fullTextSupport;
        private final String version;
        
        public DatabaseCapabilities(DatabaseType databaseType, boolean vectorSupport, 
                                  boolean fullTextSupport, String version) {
            this.databaseType = databaseType;
            this.vectorSupport = vectorSupport;
            this.fullTextSupport = fullTextSupport;
            this.version = version;
        }
        
        public DatabaseType getDatabaseType() {
            return databaseType;
        }
        
        public boolean hasVectorSupport() {
            return vectorSupport;
        }
        
        public boolean hasFullTextSupport() {
            return fullTextSupport;
        }
        
        public String getVersion() {
            return version;
        }
        
        @Override
        public String toString() {
            return String.format("DatabaseCapabilities{type=%s, vector=%s, fullText=%s, version='%s'}", 
                               databaseType, vectorSupport, fullTextSupport, version);
        }
    }
}