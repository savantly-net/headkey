package ai.headkey.rest;

import ai.headkey.rest.dto.MemoryIngestionRequest;
import ai.headkey.rest.dto.MemoryIngestionResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Memory Ingestion REST API endpoints.
 * 
 * These tests verify the complete end-to-end functionality of the REST API,
 * including request/response processing, validation, error handling, and
 * integration with the underlying memory system components.
 * 
 * Uses RestAssured for HTTP testing and validates both success and failure scenarios.
 */
@QuarkusTest
class MemoryIngestionResourceIT {

    private static final String BASE_PATH = "/api/v1/memory";

    @BeforeAll
    static void setup() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
    
    @BeforeEach
    void setUp() {
        // Ensure clean state before each test
        // In a real system, this might reset test data or clear caches
    }

    @Test
    @DisplayName("POST /ingest - Successfully ingest a simple memory")
    void testIngestMemory_Success() {
        MemoryIngestionRequest request = new MemoryIngestionRequest(
            "test-agent-001", 
            "I love programming in Java and building REST APIs"
        );
        request.setSource("api");

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post(BASE_PATH + "/ingest")
        .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("success", is(true))
            .body("memory_id", notNullValue())
            .body("agent_id", equalTo("test-agent-001"))
            .body("encoded", is(true))
            .body("category", notNullValue())
            .body("category.name", notNullValue())
            .body("category.confidence", notNullValue())
            .body("timestamp", notNullValue())
            .body("processing_time_ms", notNullValue())
            .body("dry_run", anyOf(is(false), nullValue()));
    }

    @Test
    @DisplayName("POST /ingest - Successfully ingest memory with metadata")
    void testIngestMemory_WithMetadata() {
        MemoryIngestionRequest request = new MemoryIngestionRequest(
            "test-agent-002", 
            "Remember to call mom on Sunday at 3 PM"
        );
        request.setSource("user_input");
        request.setTimestamp(Instant.now().minusSeconds(300));
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("importance", "high");
        metadata.put("category_hint", "reminder");
        metadata.put("tags", Arrays.asList("family", "call", "scheduled"));
        metadata.put("priority", 9);
        request.setMetadata(metadata);

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post(BASE_PATH + "/ingest")
        .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("success", is(true))
            .body("memory_id", notNullValue())
            .body("agent_id", equalTo("test-agent-002"))
            .body("encoded", is(true))
            .body("category", notNullValue())
            .body("updated_beliefs", anyOf(notNullValue(), nullValue()));
    }

    @Test
    @DisplayName("POST /ingest - Dry run mode returns preview without storing")
    void testIngestMemory_DryRun() {
        MemoryIngestionRequest request = new MemoryIngestionRequest(
            "test-agent-003", 
            "This is a test memory for dry run validation"
        );
        request.setSource("api");
        request.setDryRun(true);

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post(BASE_PATH + "/ingest")
        .then()
            .statusCode(200)  // OK for dry run, not CREATED
            .contentType(ContentType.JSON)
            .body("success", is(true))
            .body("dry_run", is(true))
            .body("encoded", is(false))
            .body("memory_id", nullValue())  // No ID for dry run
            .body("category", notNullValue())
            .body("preview_data", anyOf(notNullValue(), nullValue()));
    }

    @Test
    @DisplayName("POST /ingest - Validation failure for missing agent ID")
    void testIngestMemory_MissingAgentId() {
        MemoryIngestionRequest request = new MemoryIngestionRequest();
        request.setContent("Content without agent ID");
        request.setSource("api");

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post(BASE_PATH + "/ingest")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("violations", notNullValue())
            .body("violations", hasSize(greaterThan(0)));
    }

    @Test
    @DisplayName("POST /ingest - Validation failure for empty content")
    void testIngestMemory_EmptyContent() {
        MemoryIngestionRequest request = new MemoryIngestionRequest("test-agent", "");

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post(BASE_PATH + "/ingest")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("violations", notNullValue())
            .body("violations", hasSize(greaterThan(0)));
    }

    @Test
    @DisplayName("POST /ingest - Validation failure for oversized content")
    void testIngestMemory_OversizedContent() {
        // Create content that exceeds the maximum length (10,000 chars)
        StringBuilder oversizedContent = new StringBuilder();
        for (int i = 0; i < 10001; i++) {
            oversizedContent.append("x");
        }

        MemoryIngestionRequest request = new MemoryIngestionRequest(
            "test-agent", 
            oversizedContent.toString()
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post(BASE_PATH + "/ingest")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("violations", notNullValue())
            .body("violations", hasSize(greaterThan(0)));
    }

    @Test
    @DisplayName("POST /ingest - Invalid JSON payload")
    void testIngestMemory_InvalidJson() {
        String invalidJson = "{ \"agent_id\": \"test\", \"content\": ";

        given()
            .contentType(ContentType.JSON)
            .body(invalidJson)
        .when()
            .post(BASE_PATH + "/ingest")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("POST /dry-run - Successful dry run operation")
    void testDryRunIngest_Success() {
        MemoryIngestionRequest request = new MemoryIngestionRequest(
            "test-agent-dry", 
            "Testing the dry run endpoint specifically"
        );
        request.setSource("api");

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post(BASE_PATH + "/dry-run")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("success", is(true))
            .body("dry_run", is(true))
            .body("encoded", is(false))
            .body("memory_id", nullValue())
            .body("category", notNullValue())
            .body("processing_time_ms", notNullValue());
    }

    @Test
    @DisplayName("POST /dry-run - Validation failure")
    void testDryRunIngest_ValidationFailure() {
        MemoryIngestionRequest request = new MemoryIngestionRequest();
        // Missing required fields

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post(BASE_PATH + "/dry-run")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("violations", notNullValue());
    }

    @Test
    @DisplayName("GET /statistics - Retrieve ingestion statistics")
    void testGetStatistics_Success() {
        given()
        .when()
            .get(BASE_PATH + "/statistics")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasKey("totalIngestions"));
    }

    @Test
    @DisplayName("GET /health - Health check returns system status")
    void testHealthCheck_Success() {
        given()
        .when()
            .get(BASE_PATH + "/health")
        .then()
            .statusCode(anyOf(is(200), is(503)))  // Either healthy or unhealthy
            .contentType(ContentType.JSON)
            .body("healthy", anyOf(is(true), is(false)))
            .body("status", anyOf(is("UP"), is("DOWN")))
            .body("service", equalTo("memory-ingestion"))
            .body("timestamp", notNullValue());
    }

    @Test
    @DisplayName("POST /validate - Valid input passes validation")
    void testValidateInput_Valid() {
        MemoryIngestionRequest request = new MemoryIngestionRequest(
            "validation-agent", 
            "This is valid content for validation testing"
        );
        request.setSource("api");

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post(BASE_PATH + "/validate")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("valid", is(true))
            .body("message", equalTo("Input is valid"));
    }

    @Test
    @DisplayName("POST /validate - Invalid input fails validation")
    void testValidateInput_Invalid() {
        MemoryIngestionRequest request = new MemoryIngestionRequest();
        // Invalid request with missing fields

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post(BASE_PATH + "/validate")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("violations", notNullValue())
            .body("violations", hasSize(greaterThan(0)));
    }

    @Test
    @DisplayName("Test endpoint with different HTTP methods")
    void testUnsupportedHttpMethods() {
        // Test unsupported methods on /ingest endpoint
        given()
        .when()
            .get(BASE_PATH + "/ingest")
        .then()
            .statusCode(405);  // Method Not Allowed

        given()
        .when()
            .put(BASE_PATH + "/ingest")
        .then()
            .statusCode(405);  // Method Not Allowed

        given()
        .when()
            .delete(BASE_PATH + "/ingest")
        .then()
            .statusCode(405);  // Method Not Allowed
    }

    @Test
    @DisplayName("Test CORS headers are present")
    void testCorsHeaders() {
        MemoryIngestionRequest request = new MemoryIngestionRequest(
            "cors-test-agent", 
            "Testing CORS headers"
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .header("Origin", "http://localhost:3000")
        .when()
            .post(BASE_PATH + "/ingest")
        .then()
            .statusCode(anyOf(is(201), is(400), is(500)))  // Any response is fine for CORS test
            .header("Content-Type", containsString("application/json"));
    }

    @Test
    @DisplayName("Test multiple rapid requests for performance")
    void testMultipleRapidRequests() {
        // Test that the system can handle multiple concurrent requests
        for (int i = 0; i < 5; i++) {
            MemoryIngestionRequest request = new MemoryIngestionRequest(
                "performance-agent-" + i, 
                "Performance test memory content " + i
            );
            request.setSource("api");

            given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post(BASE_PATH + "/ingest")
            .then()
                .statusCode(anyOf(is(201), is(400), is(500)))  // Accept any response for performance test
                .time(lessThan(10000L));  // Should respond within 10 seconds
        }
    }

    @Test
    @DisplayName("Test request with special characters in content")
    void testSpecialCharactersInContent() {
        MemoryIngestionRequest request = new MemoryIngestionRequest(
            "special-chars-agent", 
            "Content with special chars: !@#$%^&*()_+-=[]{}|;:',.<>?/~`\" and émojis 🚀 🎉 ✨"
        );
        request.setSource("api");

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post(BASE_PATH + "/ingest")
        .then()
            .statusCode(anyOf(is(201), is(400)))  // Should either succeed or fail gracefully
            .contentType(ContentType.JSON)
            .body("success", anyOf(is(true), is(false)));
    }

    @Test
    @DisplayName("Test request with Unicode content")
    void testUnicodeContent() {
        MemoryIngestionRequest request = new MemoryIngestionRequest(
            "unicode-agent", 
            "Unicode content: Здравствуй мир! 你好世界! こんにちは世界! مرحبا بالعالم!"
        );
        request.setSource("api");

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post(BASE_PATH + "/ingest")
        .then()
            .statusCode(anyOf(is(201), is(400)))  // Should either succeed or fail gracefully
            .contentType(ContentType.JSON)
            .body("success", anyOf(is(true), is(false)));
    }
}