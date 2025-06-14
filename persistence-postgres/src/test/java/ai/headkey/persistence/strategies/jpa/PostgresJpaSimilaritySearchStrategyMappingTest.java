package ai.headkey.persistence.strategies.jpa;

import static org.junit.jupiter.api.Assertions.*;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.dto.Metadata;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PostgresJpaSimilaritySearchStrategy's result mapping functionality.
 *
 * This test class specifically tests the mapResultToMemoryRecord method to ensure
 * it properly handles different database return types, particularly the ClassCastException
 * issue with timestamp types.
 */
class PostgresJpaSimilaritySearchStrategyMappingTest {

    private PostgresJpaSimilaritySearchStrategy strategy;
    private Method mapResultToMemoryRecordMethod;

    @BeforeEach
    void setUp() throws Exception {
        strategy = new PostgresJpaSimilaritySearchStrategy();

        // Get the private method using reflection
        mapResultToMemoryRecordMethod =
            PostgresJpaSimilaritySearchStrategy.class.getDeclaredMethod(
                    "mapResultToMemoryRecord",
                    Object[].class
                );
        mapResultToMemoryRecordMethod.setAccessible(true);
    }

    @Test
    @DisplayName("Should handle SQL Timestamp objects correctly")
    void testMapResultWithSqlTimestamp() throws Exception {
        // Given
        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.from(now);

        Object[] row = {
            "memory-123", // id
            "agent-456", // agentId
            "Test memory content", // content
            null, // category (null)
            new BigDecimal("0.85"), // relevanceScore
            timestamp, // createdAt (SQL Timestamp)
            timestamp, // lastAccessed (SQL Timestamp)
            null, // metadata (null)
            null, // embedding (null)
        };

        // When
        MemoryRecord result =
            (MemoryRecord) mapResultToMemoryRecordMethod.invoke(
                strategy,
                (Object) row
            );

        // Then
        assertNotNull(result);
        assertEquals("memory-123", result.getId());
        assertEquals("agent-456", result.getAgentId());
        assertEquals("Test memory content", result.getContent());
        assertEquals(0.85, result.getRelevanceScore(), 0.001);
        assertEquals(
            now.getEpochSecond(),
            result.getCreatedAt().getEpochSecond()
        );
        assertEquals(
            now.getEpochSecond(),
            result.getLastAccessed().getEpochSecond()
        );
    }

    @Test
    @DisplayName("Should handle Java Instant objects correctly")
    void testMapResultWithInstant() throws Exception {
        // Given
        Instant now = Instant.now();

        Object[] row = {
            "memory-789", // id
            "agent-123", // agentId
            "Another test memory", // content
            null, // category (null)
            new BigDecimal("0.75"), // relevanceScore
            now, // createdAt (Instant)
            now, // lastAccessed (Instant)
            null, // metadata (null)
            null, // embedding (null)
        };

        // When
        MemoryRecord result =
            (MemoryRecord) mapResultToMemoryRecordMethod.invoke(
                strategy,
                (Object) row
            );

        // Then
        assertNotNull(result);
        assertEquals("memory-789", result.getId());
        assertEquals("agent-123", result.getAgentId());
        assertEquals("Another test memory", result.getContent());
        assertEquals(0.75, result.getRelevanceScore(), 0.001);
        assertEquals(now, result.getCreatedAt());
        assertEquals(now, result.getLastAccessed());
    }

    @Test
    @DisplayName("Should handle null timestamps gracefully")
    void testMapResultWithNullTimestamps() throws Exception {
        // Given
        Object[] row = {
            "memory-null", // id
            "agent-null", // agentId
            "Memory with null timestamps", // content
            null, // category (null)
            new BigDecimal("0.90"), // relevanceScore
            null, // createdAt (null)
            null, // lastAccessed (null)
            null, // metadata (null)
            null, // embedding (null)
        };

        // When
        MemoryRecord result =
            (MemoryRecord) mapResultToMemoryRecordMethod.invoke(
                strategy,
                (Object) row
            );

        // Then
        assertNotNull(result);
        assertEquals("memory-null", result.getId());
        assertEquals("agent-null", result.getAgentId());
        assertEquals("Memory with null timestamps", result.getContent());
        assertEquals(0.90, result.getRelevanceScore(), 0.001);
        assertNull(result.getCreatedAt());
        assertNull(result.getLastAccessed());
    }

    @Test
    @DisplayName("Should handle different numeric types for relevance score")
    void testMapResultWithDifferentNumericTypes() throws Exception {
        // Given - Test with Double instead of BigDecimal
        Object[] row = {
            "memory-double", // id
            "agent-double", // agentId
            "Memory with double score", // content
            null, // category (null)
            0.95, // relevanceScore (Double)
            Instant.now(), // createdAt
            Instant.now(), // lastAccessed
            null, // metadata (null)
            null, // embedding (null)
        };

        // When
        MemoryRecord result =
            (MemoryRecord) mapResultToMemoryRecordMethod.invoke(
                strategy,
                (Object) row
            );

        // Then
        assertEquals(0.95, result.getRelevanceScore(), 0.001);
    }

    @Test
    @DisplayName("Should handle valid category JSON")
    void testMapResultWithValidCategoryJson() throws Exception {
        // Given
        String categoryJson =
            "{\"primary\":\"knowledge\",\"secondary\":\"technology\",\"tags\":[\"ai\",\"ml\"],\"confidence\":0.85}";

        Object[] row = {
            "memory-category", // id
            "agent-category", // agentId
            "Memory with category", // content
            categoryJson, // category (JSON)
            new BigDecimal("0.80"), // relevanceScore
            Instant.now(), // createdAt
            Instant.now(), // lastAccessed
            null, // metadata (null)
            null, // embedding (null)
        };

        // When
        MemoryRecord result =
            (MemoryRecord) mapResultToMemoryRecordMethod.invoke(
                strategy,
                (Object) row
            );

        // Then
        assertNotNull(result.getCategory());
        assertEquals("knowledge", result.getCategory().getPrimary());
        assertEquals("technology", result.getCategory().getSecondary());
        assertEquals(0.85, result.getCategory().getConfidence(), 0.001);
        assertTrue(result.getCategory().getTags().contains("ai"));
        assertTrue(result.getCategory().getTags().contains("ml"));
    }

    @Test
    @DisplayName("Should handle valid metadata JSON")
    void testMapResultWithValidMetadataJson() throws Exception {
        // Given
        String metadataJson =
            "{\"source\":\"user_input\",\"importance\":0.8,\"accessCount\":5}";

        Object[] row = {
            "memory-metadata", // id
            "agent-metadata", // agentId
            "Memory with metadata", // content
            null, // category (null)
            new BigDecimal("0.70"), // relevanceScore
            Instant.now(), // createdAt
            Instant.now(), // lastAccessed
            metadataJson, // metadata (JSON)
            null, // embedding (null)
        };

        // When
        MemoryRecord result =
            (MemoryRecord) mapResultToMemoryRecordMethod.invoke(
                strategy,
                (Object) row
            );

        // Then
        assertNotNull(result.getMetadata());
        assertEquals("user_input", result.getMetadata().getSource());
        assertEquals(0.8, result.getMetadata().getImportance(), 0.001);
        assertEquals(Integer.valueOf(5), result.getMetadata().getAccessCount());
    }

    @Test
    @DisplayName("Should handle malformed category JSON gracefully")
    void testMapResultWithMalformedCategoryJson() throws Exception {
        // Given
        String malformedJson = "{\"primary\":\"knowledge\",\"invalid\":}"; // Malformed JSON

        Object[] row = {
            "memory-malformed", // id
            "agent-malformed", // agentId
            "Memory with malformed category", // content
            malformedJson, // category (malformed JSON)
            new BigDecimal("0.60"), // relevanceScore
            Instant.now(), // createdAt
            Instant.now(), // lastAccessed
            null, // metadata (null)
            null, // embedding (null)
        };

        // When
        MemoryRecord result =
            (MemoryRecord) mapResultToMemoryRecordMethod.invoke(
                strategy,
                (Object) row
            );

        // Then - Should not throw exception, but category should be null
        assertNotNull(result);
        assertNull(result.getCategory()); // Should be null due to parsing error
        assertEquals(0.60, result.getRelevanceScore(), 0.001);
    }

    @Test
    @DisplayName("Should handle empty and whitespace-only JSON strings")
    void testMapResultWithEmptyJson() throws Exception {
        // Given
        Object[] row = {
            "memory-empty", // id
            "agent-empty", // agentId
            "Memory with empty JSON", // content
            "   ", // category (whitespace only)
            new BigDecimal("0.50"), // relevanceScore
            Instant.now(), // createdAt
            Instant.now(), // lastAccessed
            "", // metadata (empty string)
            null, // embedding (null)
        };

        // When
        MemoryRecord result =
            (MemoryRecord) mapResultToMemoryRecordMethod.invoke(
                strategy,
                (Object) row
            );

        // Then
        assertNotNull(result);
        assertNull(result.getCategory()); // Should be null for whitespace-only
        assertNull(result.getMetadata()); // Should be null for empty string
        assertEquals(0.50, result.getRelevanceScore(), 0.001);
    }

    @Test
    @DisplayName("Should handle null field values gracefully")
    void testMapResultWithNullFields() throws Exception {
        // Given
        Object[] row = {
            null, // id (null)
            null, // agentId (null)
            null, // content (null)
            null, // category (null)
            null, // relevanceScore (null)
            null, // createdAt (null)
            null, // lastAccessed (null)
            null, // metadata (null)
            null, // embedding (null)
        };

        // When
        MemoryRecord result =
            (MemoryRecord) mapResultToMemoryRecordMethod.invoke(
                strategy,
                (Object) row
            );

        // Then
        assertNotNull(result);
        assertNull(result.getId());
        assertNull(result.getAgentId());
        assertNull(result.getContent());
        assertNull(result.getCategory());
        assertNull(result.getRelevanceScore());
        assertNull(result.getCreatedAt());
        assertNull(result.getLastAccessed());
        assertNull(result.getMetadata());
    }

    @Test
    @DisplayName("Should handle complete valid row data")
    void testMapResultWithCompleteValidData() throws Exception {
        // Given
        Instant now = Instant.now();
        String categoryJson =
            "{\"primary\":\"personal\",\"secondary\":\"experience\",\"tags\":[\"learning\",\"memory\"],\"confidence\":0.92}";
        String metadataJson =
            "{\"source\":\"conversation\",\"importance\":0.6,\"accessCount\":3,\"tags\":[\"test\",\"demo\"]}";

        Object[] row = {
            "memory-complete", // id
            "agent-complete", // agentId
            "Complete memory record", // content
            categoryJson, // category (valid JSON)
            new BigDecimal("0.88"), // relevanceScore
            Timestamp.from(now), // createdAt (SQL Timestamp)
            now, // lastAccessed (Instant)
            metadataJson, // metadata (valid JSON)
            null, // embedding (null for this test)
        };

        // When
        MemoryRecord result =
            (MemoryRecord) mapResultToMemoryRecordMethod.invoke(
                strategy,
                (Object) row
            );

        // Then
        assertNotNull(result);
        assertEquals("memory-complete", result.getId());
        assertEquals("agent-complete", result.getAgentId());
        assertEquals("Complete memory record", result.getContent());
        assertEquals(0.88, result.getRelevanceScore(), 0.001);
        assertEquals(
            now.getEpochSecond(),
            result.getCreatedAt().getEpochSecond()
        );
        assertEquals(now, result.getLastAccessed());

        // Verify category
        assertNotNull(result.getCategory());
        assertEquals("personal", result.getCategory().getPrimary());
        assertEquals("experience", result.getCategory().getSecondary());
        assertEquals(0.92, result.getCategory().getConfidence(), 0.001);
        assertTrue(result.getCategory().getTags().contains("learning"));
        assertTrue(result.getCategory().getTags().contains("memory"));

        // Verify metadata
        assertNotNull(result.getMetadata());
        assertEquals("conversation", result.getMetadata().getSource());
        assertEquals(0.6, result.getMetadata().getImportance(), 0.001);
        assertEquals(Integer.valueOf(3), result.getMetadata().getAccessCount());
    }
}
