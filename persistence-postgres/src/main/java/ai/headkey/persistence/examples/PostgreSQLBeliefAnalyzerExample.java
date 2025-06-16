package ai.headkey.persistence.examples;

import ai.headkey.memory.dto.*;
import ai.headkey.memory.factory.BeliefReinforcementConflictAnalyzerFactory;
import ai.headkey.memory.implementations.SimplePatternBeliefExtractionService;
import ai.headkey.memory.interfaces.BeliefExtractionService;
import ai.headkey.memory.interfaces.BeliefReinforcementConflictAnalyzer;
import ai.headkey.memory.interfaces.BeliefStorageService;
import ai.headkey.persistence.factory.JpaBeliefStorageServiceFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.*;

/**
 * Example demonstrating integration of PostgreSQL persistence with the
 * existing belief analyzer architecture.
 * 
 * This example shows how to:
 * 1. Create a PostgreSQL-backed belief storage service
 * 2. Integrate it with the abstract belief analyzer
 * 3. Use the factory pattern for different configurations
 * 4. Demonstrate the complete workflow with persistent storage
 * 
 * Key benefits of this integration:
 * - Persistent storage of beliefs and conflicts
 * - Scalable PostgreSQL backend
 * - Full ACID transactions
 * - Advanced querying capabilities
 * - Production-ready performance
 * 
 * @since 1.0
 */
public class PostgreSQLBeliefAnalyzerExample {
    
    public static void main(String[] args) {
        System.out.println("=== PostgreSQL Belief Analyzer Integration Example ===\n");
        
        try {
            // Example 1: Basic PostgreSQL integration
            demonstrateBasicIntegration();
            
            // Example 2: Production configuration
            demonstrateProductionSetup();
            
            // Example 3: Complete workflow with persistence
            demonstrateCompleteWorkflow();
            
            // Example 4: Performance optimization
            demonstratePerformanceOptimization();
            
            // Example 5: Monitoring and health checks
            demonstrateMonitoring();
            
        } catch (Exception e) {
            System.err.println("Example failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== Example Complete ===");
    }
    
    /**
     * Demonstrates basic integration of PostgreSQL storage with belief analyzer.
     */
    private static void demonstrateBasicIntegration() {
        System.out.println("1. Basic PostgreSQL Integration");
        System.out.println("-------------------------------");
        
        try {
            // Create PostgreSQL-backed storage service
            BeliefStorageService postgresStorage = createPostgreSQLStorage();
            
            // Create belief extraction service
            BeliefExtractionService extractor = new SimplePatternBeliefExtractionService();
            
            // Create belief analyzer with PostgreSQL persistence
            BeliefReinforcementConflictAnalyzer analyzer = 
                BeliefReinforcementConflictAnalyzerFactory.create(extractor, postgresStorage);
            
            // Test basic operations
            MemoryRecord memory = createSampleMemory("user-123", "I love PostgreSQL databases");
            BeliefUpdateResult result = analyzer.analyzeNewMemory(memory);
            
            System.out.println("Analyzed memory: " + memory.getContent());
            System.out.println("Beliefs created: " + result.getNewBeliefs().size());
            System.out.println("Storage service healthy: " + postgresStorage.isHealthy());
            
            // Verify persistence
            List<Belief> storedBeliefs = analyzer.getBeliefsForAgent("user-123");
            System.out.println("Persisted beliefs: " + storedBeliefs.size());
            
        } catch (Exception e) {
            System.out.println("Note: This example requires a running PostgreSQL instance");
            System.out.println("Error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates production-ready configuration with optimizations.
     */
    private static void demonstrateProductionSetup() {
        System.out.println("2. Production Configuration");
        System.out.println("---------------------------");
        
        try {
            // Create production DataSource with connection pooling
            DataSource productionDataSource = createProductionDataSource();
            
            // Create production-optimized storage service
            BeliefStorageService prodStorage = JpaBeliefStorageServiceFactory
                .createForProduction(productionDataSource);
            
            // Create analyzer with production configuration
            BeliefReinforcementConflictAnalyzer prodAnalyzer = 
                BeliefReinforcementConflictAnalyzerFactory.builder()
                    .withExtractionService(new SimplePatternBeliefExtractionService())
                    .withStorageService(prodStorage)
                    .withResolutionStrategy("preference", "newer_wins")
                    .withResolutionStrategy("fact", "higher_confidence")
                    .withConfig("environment", "production")
                    .withConfig("performanceMonitoring", true)
                    .build();
            
            System.out.println("Production analyzer created successfully");
            System.out.println("Storage type: " + prodStorage.getServiceInfo().get("serviceType"));
            System.out.println("Thread safety: " + prodStorage.getServiceInfo().get("threadSafety"));
            System.out.println("Transaction support: " + prodStorage.getServiceInfo().get("transactionSupport"));
            
        } catch (Exception e) {
            System.out.println("Production setup example (requires PostgreSQL)");
            System.out.println("Configuration would include:");
            System.out.println("  - HikariCP connection pooling");
            System.out.println("  - Performance optimization");
            System.out.println("  - Monitoring integration");
            System.out.println("  - Schema validation");
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates complete workflow with persistent storage.
     */
    private static void demonstrateCompleteWorkflow() {
        System.out.println("3. Complete Workflow with Persistence");
        System.out.println("------------------------------------");
        
        try {
            // Use test storage for demonstration
            BeliefStorageService storage = JpaBeliefStorageServiceFactory.createForTesting();
            BeliefExtractionService extractor = new SimplePatternBeliefExtractionService();
            
            BeliefReinforcementConflictAnalyzer analyzer = 
                BeliefReinforcementConflictAnalyzerFactory.create(extractor, storage);
            
            // Step 1: Analyze multiple memories
            String[] memories = {
                "I love Italian food",
                "Pizza is my favorite meal",
                "I enjoy pasta dishes",
                "I don't like spicy food",
                "Italian cuisine is the best"
            };
            
            System.out.println("Analyzing memories and storing in database...");
            for (String content : memories) {
                MemoryRecord memory = createSampleMemory("user-456", content);
                BeliefUpdateResult result = analyzer.analyzeNewMemory(memory);
                
                System.out.println("  Memory: " + content);
                System.out.println("    Created beliefs: " + result.getNewBeliefs().size());
                System.out.println("    Reinforced beliefs: " + result.getReinforcedBeliefs().size());
                System.out.println("    Conflicts detected: " + result.getConflicts().size());
            }
            
            // Step 2: Query persisted beliefs
            List<Belief> allBeliefs = analyzer.getBeliefsForAgent("user-456");
            List<Belief> preferences = analyzer.getBeliefsInCategory("preference", "user-456");
            List<Belief> lowConfidence = analyzer.getLowConfidenceBeliefs(0.7, "user-456");
            
            System.out.println("\nQuerying persisted data:");
            System.out.println("  Total beliefs: " + allBeliefs.size());
            System.out.println("  Preference beliefs: " + preferences.size());
            System.out.println("  Low confidence beliefs: " + lowConfidence.size());
            
            // Step 3: Search and similarity
            List<Belief> searchResults = storage.searchBeliefs("Italian", "user-456", 10);
            List<BeliefStorageService.SimilarBelief> similar = storage.findSimilarBeliefs(
                "I really enjoy Italian cooking", "user-456", 0.3, 5);
            
            System.out.println("  Search results for 'Italian': " + searchResults.size());
            System.out.println("  Similar beliefs found: " + similar.size());
            
            // Step 4: Analytics and statistics
            Map<String, Object> stats = analyzer.getAgentBeliefStatistics("user-456");
            Map<String, Long> categoryDist = storage.getBeliefDistributionByCategory("user-456");
            
            System.out.println("\nAnalytics:");
            System.out.println("  Average confidence: " + 
                             String.format("%.2f", (Double) stats.get("averageConfidence")));
            System.out.println("  Category distribution: " + categoryDist);
            
        } catch (Exception e) {
            System.out.println("Workflow demonstration (using test storage)");
            System.out.println("Error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates performance optimization techniques.
     */
    private static void demonstratePerformanceOptimization() {
        System.out.println("4. Performance Optimization");
        System.out.println("---------------------------");
        
        try {
            // Create optimized storage service
            Map<String, Object> optimizedProps = JpaBeliefStorageServiceFactory.getHighPerformanceProperties();
            
            BeliefStorageService storage = JpaBeliefStorageServiceFactory.builder()
                .withJpaProperties(optimizedProps)
                .withAutoCreateSchema(true)
                .withStatistics(true)
                .build();
            
            System.out.println("Performance optimizations applied:");
            System.out.println("  Batch size: " + optimizedProps.get("hibernate.jdbc.batch_size"));
            System.out.println("  Fetch size: " + optimizedProps.get("hibernate.jdbc.fetch_size"));
            System.out.println("  Query cache size: " + optimizedProps.get("hibernate.query.plan_cache_max_size"));
            System.out.println("  Connection optimization: " + optimizedProps.get("hibernate.connection.provider_disables_autocommit"));
            
            // Demonstrate batch operations
            List<Belief> batchBeliefs = createSampleBeliefs(100);
            long startTime = System.currentTimeMillis();
            
            List<Belief> storedBeliefs = storage.storeBeliefs(batchBeliefs);
            
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("  Batch stored " + storedBeliefs.size() + " beliefs in " + duration + "ms");
            
            // Demonstrate bulk retrieval
            Set<String> beliefIds = storedBeliefs.stream()
                .map(Belief::getId)
                .limit(50)
                .collect(java.util.stream.Collectors.toSet());
            
            startTime = System.currentTimeMillis();
            List<Belief> retrieved = storage.getBeliefsById(beliefIds);
            duration = System.currentTimeMillis() - startTime;
            
            System.out.println("  Bulk retrieved " + retrieved.size() + " beliefs in " + duration + "ms");
            
        } catch (Exception e) {
            System.out.println("Performance optimization examples:");
            System.out.println("  - Batch operations for bulk inserts/updates");
            System.out.println("  - Connection pooling with HikariCP");
            System.out.println("  - Query plan caching");
            System.out.println("  - Strategic indexing");
            System.out.println("  - Optimized JPA settings");
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates monitoring and health check capabilities.
     */
    private static void demonstrateMonitoring() {
        System.out.println("5. Monitoring and Health Checks");
        System.out.println("-------------------------------");
        
        try {
            BeliefStorageService storage = JpaBeliefStorageServiceFactory.createForTesting();
            
            // Health checks
            boolean healthy = storage.isHealthy();
            Map<String, Object> healthInfo = storage.getHealthInfo();
            Map<String, Object> serviceInfo = storage.getServiceInfo();
            
            System.out.println("Health Status:");
            System.out.println("  Service healthy: " + healthy);
            System.out.println("  Storage type: " + healthInfo.get("storageType"));
            System.out.println("  Status: " + healthInfo.get("status"));
            System.out.println("  Service version: " + serviceInfo.get("version"));
            
            // Storage statistics
            Map<String, Object> stats = storage.getStorageStatistics();
            System.out.println("\nStorage Statistics:");
            System.out.println("  Total beliefs: " + stats.get("totalBeliefs"));
            System.out.println("  Total conflicts: " + stats.get("totalConflicts"));
            System.out.println("  Storage operations: " + stats.get("totalStoreOperations"));
            System.out.println("  Query operations: " + stats.get("totalQueryOperations"));
            System.out.println("  Search operations: " + stats.get("totalSearchOperations"));
            
            // Maintenance operations
            Map<String, Object> optimizationResult = storage.optimizeStorage();
            Map<String, Object> validationResult = storage.validateIntegrity();
            
            System.out.println("\nMaintenance:");
            System.out.println("  Optimization successful: " + optimizationResult.get("success"));
            System.out.println("  Integrity healthy: " + validationResult.get("healthy"));
            System.out.println("  Issues found: " + validationResult.get("issuesFound"));
            
        } catch (Exception e) {
            System.out.println("Monitoring capabilities include:");
            System.out.println("  - Health checks for database connectivity");
            System.out.println("  - Performance statistics tracking");
            System.out.println("  - Storage optimization operations");
            System.out.println("  - Data integrity validation");
            System.out.println("  - Service metadata and versioning");
        }
        
        System.out.println();
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Creates a PostgreSQL DataSource for testing (requires running PostgreSQL).
     */
    private static BeliefStorageService createPostgreSQLStorage() {
        // This would work with a real PostgreSQL instance
        String jdbcUrl = "jdbc:postgresql://localhost:5432/headkey";
        String username = "headkey_user";
        String password = "headkey_password";
        
        return JpaBeliefStorageServiceFactory.createWithUrl(jdbcUrl, username, password);
    }
    
    /**
     * Creates a production-grade DataSource with connection pooling.
     */
    private static DataSource createProductionDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://prod-db-host:5432/headkey_prod");
        config.setUsername("headkey_prod_user");
        config.setPassword("secure_password");
        config.setDriverClassName("org.postgresql.Driver");
        
        // Production connection pool settings
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        
        // Performance optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        return new HikariDataSource(config);
    }
    
    /**
     * Creates a sample memory record for testing.
     */
    private static MemoryRecord createSampleMemory(String agentId, String content) {
        MemoryRecord memory = new MemoryRecord();
        memory.setId("memory-" + System.nanoTime());
        memory.setAgentId(agentId);
        memory.setContent(content);
        memory.setCreatedAt(Instant.now());
        
        CategoryLabel category = new CategoryLabel();
        category.setPrimary("conversation");
        category.setConfidence(0.8);
        memory.setCategory(category);
        
        Metadata metadata = new Metadata();
        metadata.setSource("example");
        metadata.setImportance(0.7);
        memory.setMetadata(metadata);
        
        return memory;
    }
    
    /**
     * Creates sample beliefs for batch testing.
     */
    private static List<Belief> createSampleBeliefs(int count) {
        List<Belief> beliefs = new ArrayList<>();
        String[] categories = {"preference", "fact", "relationship", "location", "opinion"};
        Random random = new Random();
        
        for (int i = 0; i < count; i++) {
            String id = "batch-belief-" + i;
            String agentId = "batch-user-" + (i % 10); // 10 different users
            String statement = "Sample belief statement " + i;
            double confidence = 0.3 + (random.nextDouble() * 0.7); // 0.3 to 1.0
            String category = categories[i % categories.length];
            
            Belief belief = new Belief(id, agentId, statement, confidence);
            belief.setCategory(category);
            belief.setCreatedAt(Instant.now());
            belief.addEvidence("evidence-" + i);
            belief.addTag("batch-test");
            
            beliefs.add(belief);
        }
        
        return beliefs;
    }
    
    /**
     * Demonstrates the benefits of this integration approach.
     */
    @SuppressWarnings("unused")
    private static void showArchitecturalBenefits() {
        System.out.println("Architectural Benefits of PostgreSQL Integration:");
        System.out.println("-----------------------------------------------");
        
        System.out.println("1. Separation of Concerns:");
        System.out.println("   - Business logic in AbstractBeliefReinforcementConflictAnalyzer");
        System.out.println("   - Persistence logic in JpaBeliefStorageService");
        System.out.println("   - Content analysis in BeliefExtractionService");
        
        System.out.println("\n2. Scalability:");
        System.out.println("   - PostgreSQL handles large datasets efficiently");
        System.out.println("   - Connection pooling for concurrent access");
        System.out.println("   - Optimized indexing for fast queries");
        
        System.out.println("\n3. Reliability:");
        System.out.println("   - ACID transactions ensure data consistency");
        System.out.println("   - Persistent storage survives application restarts");
        System.out.println("   - Backup and recovery capabilities");
        
        System.out.println("\n4. Performance:");
        System.out.println("   - Batch operations for bulk processing");
        System.out.println("   - Advanced query optimization");
        System.out.println("   - Efficient similarity search with indexes");
        
        System.out.println("\n5. Maintainability:");
        System.out.println("   - Clear interface boundaries");
        System.out.println("   - Easy to test with different storage backends");
        System.out.println("   - Configuration-driven setup");
        
        System.out.println("\n6. Future Extensibility:");
        System.out.println("   - Easy integration with AI services (LangChain4J)");
        System.out.println("   - Support for vector similarity search");
        System.out.println("   - Pluggable conflict resolution strategies");
    }
}