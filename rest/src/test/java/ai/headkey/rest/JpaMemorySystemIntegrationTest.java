package ai.headkey.rest;

import ai.headkey.persistence.services.JpaMemoryEncodingSystem;
import ai.headkey.memory.interfaces.InformationIngestionModule;
import ai.headkey.rest.config.MemorySystemProperties;
import ai.headkey.rest.dto.MemoryIngestionRequest;
import ai.headkey.rest.dto.MemoryIngestionResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the refactored REST application using JPA memory system.
 * 
 * This test verifies that:
 * 1. The JPA memory system is properly configured and injected
 * 2. Database connectivity works correctly
 * 3. Similarity search strategies are functioning
 * 4. REST endpoints work with the new JPA-based backend
 * 5. Configuration properties are properly applied
 * 6. Health checks reflect the new system status
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JpaMemorySystemIntegrationTest {
    
    @Inject
    JpaMemoryEncodingSystem jpaMemorySystem;
    
    @Inject
    InformationIngestionModule ingestionModule;
    
    @Inject
    MemorySystemProperties properties;
    
    @BeforeAll
    static void setup() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
    
    @Test
    @Order(1)
    @DisplayName("JPA Memory System should be properly injected and configured")
    void testJpaMemorySystemInjection() {
        // Verify JPA memory system is injected
        assertNotNull(jpaMemorySystem, "JPA memory system should be injected");
        
        // Verify similarity search strategy is configured
        assertNotNull(jpaMemorySystem.getSimilaritySearchStrategy(), "Similarity search strategy should be configured");
        
        // Verify ingestion module is injected
        assertNotNull(ingestionModule, "Information ingestion module should be injected");
        
        // Verify properties are injected
        assertNotNull(properties, "Memory system properties should be injected");
        
        System.out.println("✅ JPA Memory System injected successfully");
        System.out.println("   Strategy: " + jpaMemorySystem.getSimilaritySearchStrategy().getStrategyName());
        System.out.println("   Vector Support: " + jpaMemorySystem.getSimilaritySearchStrategy().supportsVectorSearch());
    }
    
    @Test
    @Order(2)
    @DisplayName("Configuration properties should be properly applied")
    void testConfigurationProperties() {
        // Test that configuration is applied to the memory system
        assertEquals(properties.batchSize(), jpaMemorySystem.getBatchSize(), 
                    "Batch size should match configuration");
        assertEquals(properties.maxSimilarityResults(), jpaMemorySystem.getMaxSimilaritySearchResults(),
                    "Max similarity results should match configuration");
        assertEquals(properties.similarityThreshold(), jpaMemorySystem.getSimilarityThreshold(), 0.001,
                    "Similarity threshold should match configuration");
        assertEquals(properties.enableSecondLevelCache(), jpaMemorySystem.isSecondLevelCacheEnabled(),
                    "Second level cache setting should match configuration");
        
        System.out.println("✅ Configuration properly applied:");
        System.out.println("   Strategy: " + properties.strategy());
        System.out.println("   Batch Size: " + properties.batchSize());
        System.out.println("   Max Results: " + properties.maxSimilarityResults());
        System.out.println("   Threshold: " + properties.similarityThreshold());
        System.out.println("   Cache Enabled: " + properties.enableSecondLevelCache());
    }
    
    @Test
    @Order(3)
    @DisplayName("System health check should indicate healthy JPA system")
    void testSystemHealthCheck() {
        given()
            .when()
                .get("/api/v1/system/health")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("healthy", is(true))
                .body("status", is("UP"))
                .body("service", is("jpa-memory-system"))
                .body("memorySystem.healthy", is(true))
                .body("database.healthy", is(true))
                .body("similarityStrategy.healthy", is(true));
                
        System.out.println("✅ System health check passed - JPA system is healthy");
    }
    
    @Test
    @Order(4)
    @DisplayName("Memory ingestion health check should work with JPA backend")
    void testMemoryIngestionHealth() {
        given()
            .when()
                .get("/api/v1/memory/health")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("healthy", is(true))
                .body("status", is("UP"))
                .body("service", is("memory-ingestion"));
                
        System.out.println("✅ Memory ingestion health check passed");
    }
    
    @Test
    @Order(5)
    @DisplayName("Configuration endpoint should return JPA system configuration")
    void testConfigurationEndpoint() {
        given()
            .when()
                .get("/api/v1/system/config")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("memory.strategy", notNullValue())
                .body("memory.batchSize", greaterThan(0))
                .body("database.kind", notNullValue())
                .body("embedding.enabled", notNullValue())
                .body("runtime.actualStrategy", notNullValue())
                .body("runtime.supportsVectorSearch", notNullValue());
                
        System.out.println("✅ Configuration endpoint working correctly");
    }
    
    @Test
    @Order(6)
    @DisplayName("Database capabilities endpoint should analyze H2 database")
    void testDatabaseCapabilitiesEndpoint() {
        given()
            .when()
                .get("/api/v1/system/database/capabilities")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("databaseType", is("H2"))
                .body("hasVectorSupport", is(false)) // H2 doesn't support vectors
                .body("hasFullTextSupport", notNullValue())
                .body("recommendedStrategy", notNullValue())
                .body("currentStrategy", notNullValue());
                
        System.out.println("✅ Database capabilities correctly analyzed H2 database");
    }
    
    @Test
    @Order(7)
    @DisplayName("Statistics endpoint should return JPA memory system statistics")
    void testStatisticsEndpoint() {
        given()
            .when()
                .get("/api/v1/system/statistics")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("memorySystem", notNullValue())
                .body("strategy.name", notNullValue())
                .body("strategy.supportsVectorSearch", notNullValue())
                .body("database.entityManagerFactoryOpen", is(true))
                .body("timestamp", notNullValue());
                
        System.out.println("✅ Statistics endpoint returning JPA system information");
    }
    
    @Test
    @Order(8)
    @DisplayName("Memory ingestion should work with JPA backend")
    void testMemoryIngestionWithJpaBackend() {
        // Create test ingestion request
        MemoryIngestionRequest request = new MemoryIngestionRequest();
        request.setAgentId("test-agent-jpa");
        request.setContent("Testing JPA memory system integration with similarity search strategies");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("test", "jpa-integration");
        metadata.put("backend", "h2-database");
        request.setMetadata(metadata);
        
        // Test actual ingestion
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
                .post("/api/v1/memory/ingest")
            .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("success", is(true))
                .body("memory_id", notNullValue())
                .body("agent_id", is("test-agent-jpa"))
                .body("category", notNullValue())
                .body("processing_time_ms", notNullValue());
                
        System.out.println("✅ Memory ingestion working with JPA backend");
    }
    
    @Test
    @Order(9)
    @DisplayName("Dry run ingestion should work with JPA backend")
    void testDryRunIngestionWithJpaBackend() {
        // Create test dry run request
        MemoryIngestionRequest request = new MemoryIngestionRequest();
        request.setAgentId("test-agent-dry-run");
        request.setContent("Testing dry run functionality with JPA similarity search strategies");
        request.setDryRun(true);
        
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
                .post("/api/v1/memory/dry-run")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("success", is(true))
                .body("memory_id", startsWith("dry-run-"))
                .body("agent_id", is("test-agent-dry-run"))
                .body("category", notNullValue())
                .body("dry_run", is(true))
                .body("processing_time_ms", notNullValue());
                
        System.out.println("✅ Dry run ingestion working with JPA backend");
    }
    
    @Test
    @Order(10)
    @DisplayName("Input validation should work with JPA backend")
    void testInputValidationWithJpaBackend() {
        // Create test validation request
        MemoryIngestionRequest request = new MemoryIngestionRequest();
        request.setAgentId("test-agent-validation");
        request.setContent("Testing input validation with the new JPA-based memory system");
        
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
                .post("/api/v1/memory/validate")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("valid", is(true))
                .body("message", is("Input is valid"));
                
        System.out.println("✅ Input validation working with JPA backend");
    }
    
    @Test
    @Order(11)
    @DisplayName("Memory ingestion statistics should include JPA system information")
    void testMemoryIngestionStatistics() {
        given()
            .when()
                .get("/api/v1/memory/statistics")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("totalIngestions", greaterThan(0)) // Should have at least one from previous test
                .body("memorySystem", notNullValue())
                .body("similarityStrategy", notNullValue())
                .body("supportsVectorSearch", notNullValue())
                .body("batchSize", greaterThan(0))
                .body("maxSimilarityResults", greaterThan(0));
                
        System.out.println("✅ Memory ingestion statistics include JPA system information");
    }
    
    @Test
    @Order(12)
    @DisplayName("Error handling should work correctly with JPA backend")
    void testErrorHandlingWithJpaBackend() {
        // Test with invalid input (empty content)
        MemoryIngestionRequest invalidRequest = new MemoryIngestionRequest();
        invalidRequest.setAgentId("test-agent");
        invalidRequest.setContent(""); // Empty content should be invalid
        
        given()
            .contentType(ContentType.JSON)
            .body(invalidRequest)
            .when()
                .post("/api/v1/memory/ingest")
            .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("success", is(false))
                .body("errors", notNullValue());
                
        System.out.println("✅ Error handling working correctly with JPA backend");
    }
    
    @Test
    @Order(13)
    @DisplayName("System should maintain performance with JPA backend")
    void testPerformanceWithJpaBackend() {
        long startTime = System.currentTimeMillis();
        
        // Perform multiple operations to test performance
        for (int i = 0; i < 5; i++) {
            MemoryIngestionRequest request = new MemoryIngestionRequest();
            request.setAgentId("performance-test-agent");
            request.setContent("Performance test memory " + i + " with JPA backend and similarity search");
            
            given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                    .post("/api/v1/memory/ingest")
                .then()
                    .statusCode(201)
                    .body("success", is(true));
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        long avgTime = totalTime / 5;
        
        // Should complete 5 ingestions in reasonable time (< 500ms average)
        assertTrue(avgTime < 500, 
                  String.format("Average ingestion time (%d ms) should be < 500ms", avgTime));
        
        System.out.printf("✅ Performance test passed - Average ingestion time: %d ms%n", avgTime);
    }
    
    @Test
    @Order(14)
    @DisplayName("JPA Memory System refactoring should be complete and functional")
    void testRefactoringSuccess() {
        // Verify that we're using JPA system, not in-memory
        assertTrue(jpaMemorySystem.getClass().getSimpleName().contains("Jpa"), 
                  "Should be using JPA memory system");
        
        // Verify similarity search strategy is working
        assertNotNull(jpaMemorySystem.getSimilaritySearchStrategy().getStrategyName(),
                     "Similarity search strategy should be functional");
        
        // Verify system is healthy
        assertTrue(jpaMemorySystem.isHealthy(), "JPA memory system should be healthy");
        
        // Verify ingestion module is working with JPA backend
        assertTrue(ingestionModule.isHealthy(), "Ingestion module should be healthy with JPA backend");
        
        System.out.println("\n🎉 JPA MEMORY SYSTEM REFACTORING SUCCESS!");
        System.out.println("=====================================");
        System.out.println("✅ JPA Memory Encoding System: Active");
        System.out.println("✅ Similarity Search Strategy: " + jpaMemorySystem.getSimilaritySearchStrategy().getStrategyName());
        System.out.println("✅ Database Backend: H2 (configurable)");
        System.out.println("✅ Configuration Properties: Applied");
        System.out.println("✅ REST Endpoints: Functional");
        System.out.println("✅ Health Monitoring: Working");
        System.out.println("✅ Error Handling: Proper");
        System.out.println("✅ Performance: Acceptable");
        System.out.println("\n🚀 REST application successfully refactored to use JpaMemorySystemFactory!");
    }
}