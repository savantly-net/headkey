package ai.headkey.persistence.strategies.jpa;

import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.persistence.entities.MemoryEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demonstration of the simplified delegation pattern for JPA similarity search strategies.
 * 
 * This demo shows how the DefaultJpaSimilaritySearchStrategy automatically:
 * - Detects the database type (PostgreSQL vs H2/Other)
 * - Delegates to the appropriate specialized strategy
 * - Provides a unified interface for all database types
 * - Handles initialization and configuration transparently
 * 
 * Benefits of this approach:
 * 1. Single entry point - clients only need to use DefaultJpaSimilaritySearchStrategy
 * 2. Automatic optimization - best strategy selected for each database
 * 3. Easy maintenance - add new strategies without changing client code
 * 4. Clean separation - each strategy handles its specific database optimally
 * 
 * @since 1.0
 */
public class DelegationPatternDemo {
    
    public static void main(String[] args) {
        System.out.println("=== JPA Similarity Search Strategy Delegation Pattern Demo ===\n");
        
        demonstrateH2Delegation();
        demonstrateUnifiedInterface();
        demonstrateStrategyBenefits();
        
        System.out.println("=== Demo Complete ===");
    }
    
    /**
     * Demonstrates delegation to TextBasedJpaSimilaritySearchStrategy for H2 database.
     */
    private static void demonstrateH2Delegation() {
        System.out.println("1. H2 Database Delegation:");
        System.out.println("   Creating H2 in-memory database...");
        
        Map<String, Object> h2Properties = new HashMap<>();
        h2Properties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        h2Properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1");
        h2Properties.put("jakarta.persistence.jdbc.user", "sa");
        h2Properties.put("jakarta.persistence.jdbc.password", "");
        h2Properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        h2Properties.put("hibernate.hbm2ddl.auto", "create-drop");
        h2Properties.put("hibernate.show_sql", "false");
        
        try (EntityManagerFactory emf = Persistence.createEntityManagerFactory("headkey-beliefs-h2-test", h2Properties);
             EntityManager em = emf.createEntityManager()) {
            
            // Create and initialize the default strategy
            DefaultJpaSimilaritySearchStrategy strategy = new DefaultJpaSimilaritySearchStrategy();
            strategy.initialize(em);
            
            // Show delegation results
            System.out.println("   ✓ Database Type Detected: " + strategy.getDatabaseType());
            System.out.println("   ✓ Delegate Strategy: " + strategy.getDelegateStrategy().getClass().getSimpleName());
            System.out.println("   ✓ Strategy Name: " + strategy.getDelegateStrategy().getStrategyName());
            System.out.println("   ✓ Vector Support: " + strategy.supportsVectorSearch());
            System.out.println("   ✓ Schema Valid: " + strategy.validateSchema(em));
            
        } catch (Exception e) {
            System.err.println("   ✗ H2 delegation failed: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates the unified interface that works regardless of the underlying database.
     */
    private static void demonstrateUnifiedInterface() {
        System.out.println("2. Unified Interface Demo:");
        System.out.println("   Client code remains the same regardless of database type...");
        
        try {
            // This is the ONLY code a client needs to write:
            System.out.println("   ```java");
            System.out.println("   // Universal client code - works with ANY database:");
            System.out.println("   DefaultJpaSimilaritySearchStrategy strategy = new DefaultJpaSimilaritySearchStrategy();");
            System.out.println("   strategy.initialize(entityManager);");
            System.out.println("   ");
            System.out.println("   List<MemoryRecord> results = strategy.searchSimilar(");
            System.out.println("       entityManager, \"search query\", null, \"agent-id\", 10, 1000, 0.0);");
            System.out.println("   ```");
            
            System.out.println("   ✓ Same interface for PostgreSQL, H2, MySQL, Oracle, etc.");
            System.out.println("   ✓ Automatic database-specific optimization");
            System.out.println("   ✓ No client code changes needed when switching databases");
            
        } catch (Exception e) {
            System.err.println("   ✗ Interface demo failed: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrates the benefits of the delegation pattern.
     */
    private static void demonstrateStrategyBenefits() {
        System.out.println("3. Strategy Pattern Benefits:");
        
        System.out.println("   🎯 BEFORE (without delegation):");
        System.out.println("      - Client must choose strategy: PostgresJpaSimilaritySearchStrategy vs TextBasedJpaSimilaritySearchStrategy");
        System.out.println("      - Client must detect database type manually");
        System.out.println("      - Client must handle different initialization requirements");
        System.out.println("      - Code duplication across strategies");
        System.out.println("      - Hard to maintain and extend");
        
        System.out.println();
        System.out.println("   ✨ AFTER (with delegation):");
        System.out.println("      ✓ Single DefaultJpaSimilaritySearchStrategy for all databases");
        System.out.println("      ✓ Automatic database detection and optimization");
        System.out.println("      ✓ Transparent delegation to specialized strategies");
        System.out.println("      ✓ Unified initialization and configuration");
        System.out.println("      ✓ Easy to add new database-specific strategies");
        System.out.println("      ✓ Clean separation of concerns");
        System.out.println("      ✓ Better testability and maintainability");
        
        System.out.println();
        System.out.println("   🔧 Architecture Improvement:");
        System.out.println("      DefaultJpaSimilaritySearchStrategy (Coordinator)");
        System.out.println("      ├── PostgresJpaSimilaritySearchStrategy (PostgreSQL optimizations)");
        System.out.println("      │   ├── Native pgvector support");
        System.out.println("      │   ├── ILIKE for case-insensitive search");
        System.out.println("      │   └── PostgreSQL-specific functions");
        System.out.println("      └── TextBasedJpaSimilaritySearchStrategy (Universal fallback)");
        System.out.println("          ├── JPQL-based search");
        System.out.println("          ├── Compatible with H2, MySQL, Oracle");
        System.out.println("          └── Safe function parameter handling");
        
        System.out.println();
    }
    
    /**
     * Creates a simple test memory entity for demonstration purposes.
     */
    private static MemoryEntity createTestMemory(String id, String agentId, String content) {
        MemoryEntity entity = new MemoryEntity();
        entity.setId(id);
        entity.setAgentId(agentId);
        entity.setContent(content);
        entity.setCreatedAt(Instant.now());
        entity.setLastAccessed(Instant.now());
        entity.setRelevanceScore(0.8);
        return entity;
    }
}