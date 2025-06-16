package ai.headkey.persistence.elastic.documents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ai.headkey.persistence.elastic.serializers.ElasticsearchInstantSerializer;
import ai.headkey.persistence.elastic.serializers.ElasticsearchInstantDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for MemoryDocument serialization and deserialization,
 * specifically focusing on Instant field handling to prevent
 * scientific notation issues in Elasticsearch.
 */
class MemoryDocumentSerializationTest {

    private ObjectMapper objectMapper;
    private MemoryDocument testDocument;
    private Instant testInstant;

    @BeforeEach
    void setUp() {
        // Configure ObjectMapper with custom serializers
        objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.registerModule(new JavaTimeModule());

        // Register custom serializers
        SimpleModule elasticsearchModule = new SimpleModule("ElasticsearchModule");
        elasticsearchModule.addSerializer(Instant.class, new ElasticsearchInstantSerializer());
        elasticsearchModule.addDeserializer(Instant.class, new ElasticsearchInstantDeserializer());
        objectMapper.registerModule(elasticsearchModule);

        // Create test instant (2024-01-15T10:30:45.123Z)
        testInstant = LocalDateTime.of(2024, 1, 15, 10, 30, 45, 123_000_000)
            .toInstant(ZoneOffset.UTC);

        // Create test document with all fields populated
        testDocument = new MemoryDocument("test-id", "agent-123", "Test memory content");
        testDocument.setContentEmbedding(Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5));
        testDocument.setPrimaryCategory("personal");
        testDocument.setSecondaryCategory("work");
        testDocument.setTags(Arrays.asList("important", "project", "meeting"));
        testDocument.setCategoryConfidence(0.95);
        testDocument.setRelevanceScore(0.87);
        testDocument.setImportanceScore(0.92);
        testDocument.setSource("chat");
        testDocument.setAccessCount(5);
        testDocument.setCreatedAt(testInstant);
        testDocument.setLastAccessed(testInstant.plusSeconds(3600));
        testDocument.setLastUpdated(testInstant.plusSeconds(1800));
        testDocument.setVersion(2L);
        testDocument.setActive(true);

        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("user_id", "user-456");
        metadata.put("session_id", "session-789");
        metadata.put("priority", "high");
        testDocument.setMetadata(metadata);
    }

    @Test
    @DisplayName("Should serialize Instant fields as ISO-8601 strings without scientific notation")
    void testInstantSerialization() throws Exception {
        String json = objectMapper.writeValueAsString(testDocument);

        // Verify no scientific notation in timestamps
        assertFalse(json.contains("E"), "JSON should not contain scientific notation (E)");
        assertFalse(json.contains("e"), "JSON should not contain scientific notation (e)");

        // Verify ISO-8601 format is present
        assertTrue(json.contains("2024-01-15T10:30:45.123Z"),
            "JSON should contain properly formatted ISO-8601 timestamp");
        assertTrue(json.contains("2024-01-15T11:30:45.123Z"),
            "JSON should contain properly formatted last_accessed timestamp");
        assertTrue(json.contains("2024-01-15T11:00:45.123Z"),
            "JSON should contain properly formatted last_updated timestamp");

        // Verify field names are correctly mapped
        assertTrue(json.contains("\"created_at\""), "JSON should contain created_at field");
        assertTrue(json.contains("\"last_accessed\""), "JSON should contain last_accessed field");
        assertTrue(json.contains("\"last_updated\""), "JSON should contain last_updated field");
    }

    @Test
    @DisplayName("Should deserialize ISO-8601 strings back to Instant objects")
    void testInstantDeserialization() throws Exception {
        String json = objectMapper.writeValueAsString(testDocument);
        MemoryDocument deserialized = objectMapper.readValue(json, MemoryDocument.class);

        assertEquals(testDocument.getCreatedAt(), deserialized.getCreatedAt(),
            "Created timestamp should be preserved during serialization/deserialization");
        assertEquals(testDocument.getLastAccessed(), deserialized.getLastAccessed(),
            "Last accessed timestamp should be preserved during serialization/deserialization");
        assertEquals(testDocument.getLastUpdated(), deserialized.getLastUpdated(),
            "Last updated timestamp should be preserved during serialization/deserialization");
    }

    @Test
    @DisplayName("Should handle null Instant values gracefully")
    void testNullInstantHandling() throws Exception {
        MemoryDocument documentWithNullTimestamps = new MemoryDocument();
        documentWithNullTimestamps.setId("test-null");
        documentWithNullTimestamps.setAgentId("agent-null");
        documentWithNullTimestamps.setContent("Test content");
        documentWithNullTimestamps.setCreatedAt(null);
        documentWithNullTimestamps.setLastAccessed(null);
        documentWithNullTimestamps.setLastUpdated(null);

        String json = objectMapper.writeValueAsString(documentWithNullTimestamps);
        MemoryDocument deserialized = objectMapper.readValue(json, MemoryDocument.class);

        assertNull(deserialized.getCreatedAt(), "Null created_at should remain null");
        assertNull(deserialized.getLastAccessed(), "Null last_accessed should remain null");
        assertNull(deserialized.getLastUpdated(), "Null last_updated should remain null");
    }

    @Test
    @DisplayName("Should deserialize epoch milliseconds correctly")
    void testEpochMillisecondsDeserialization() throws Exception {
        long epochMillis = testInstant.toEpochMilli();
        String jsonWithEpochMillis = String.format(
            "{\"id\":\"test\",\"agent_id\":\"agent\",\"content\":\"test\",\"created_at\":\"%d\"}",
            epochMillis
        );

        MemoryDocument deserialized = objectMapper.readValue(jsonWithEpochMillis, MemoryDocument.class);
        assertEquals(testInstant, deserialized.getCreatedAt(),
            "Epoch milliseconds should be correctly deserialized to Instant");
    }

    @Test
    @DisplayName("Should deserialize epoch seconds correctly")
    void testEpochSecondsDeserialization() throws Exception {
        long epochSeconds = testInstant.getEpochSecond();
        String jsonWithEpochSeconds = String.format(
            "{\"id\":\"test\",\"agent_id\":\"agent\",\"content\":\"test\",\"created_at\":\"%d\"}",
            epochSeconds
        );

        MemoryDocument deserialized = objectMapper.readValue(jsonWithEpochSeconds, MemoryDocument.class);
        assertEquals(Instant.ofEpochSecond(epochSeconds), deserialized.getCreatedAt(),
            "Epoch seconds should be correctly deserialized to Instant");
    }

    @Test
    @DisplayName("Should maintain all field values during round-trip serialization")
    void testCompleteRoundTripSerialization() throws Exception {
        String json = objectMapper.writeValueAsString(testDocument);
        MemoryDocument deserialized = objectMapper.readValue(json, MemoryDocument.class);

        assertEquals(testDocument.getId(), deserialized.getId());
        assertEquals(testDocument.getAgentId(), deserialized.getAgentId());
        assertEquals(testDocument.getContent(), deserialized.getContent());
        assertEquals(testDocument.getContentEmbedding(), deserialized.getContentEmbedding());
        assertEquals(testDocument.getPrimaryCategory(), deserialized.getPrimaryCategory());
        assertEquals(testDocument.getSecondaryCategory(), deserialized.getSecondaryCategory());
        assertEquals(testDocument.getTags(), deserialized.getTags());
        assertEquals(testDocument.getCategoryConfidence(), deserialized.getCategoryConfidence());
        assertEquals(testDocument.getRelevanceScore(), deserialized.getRelevanceScore());
        assertEquals(testDocument.getImportanceScore(), deserialized.getImportanceScore());
        assertEquals(testDocument.getSource(), deserialized.getSource());
        assertEquals(testDocument.getAccessCount(), deserialized.getAccessCount());
        assertEquals(testDocument.getVersion(), deserialized.getVersion());
        assertEquals(testDocument.getActive(), deserialized.getActive());
        assertEquals(testDocument.getMetadata(), deserialized.getMetadata());
    }

    @Test
    @DisplayName("Should handle utility methods correctly")
    void testUtilityMethods() {
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        Instant oneDayAgo = Instant.now().minusSeconds(86400);

        testDocument.setCreatedAt(oneDayAgo);
        testDocument.setLastAccessed(oneHourAgo);

        assertTrue(testDocument.isRecentlyCreated(Instant.now().minusSeconds(90000)),
            "Should identify recently created memory");
        assertFalse(testDocument.isRecentlyCreated(Instant.now().minusSeconds(3600)),
            "Should not identify old memory as recently created");

        assertTrue(testDocument.isRecentlyAccessed(Instant.now().minusSeconds(7200)),
            "Should identify recently accessed memory");
        assertFalse(testDocument.isRecentlyAccessed(Instant.now().minusSeconds(1800)),
            "Should not identify old access as recent");

        assertTrue(testDocument.getAgeInSeconds() > 0,
            "Age should be positive for past timestamps");
    }

    @Test
    @DisplayName("Should update access tracking correctly")
    void testAccessTracking() {
        testDocument.setAccessCount(10);
        Instant beforeUpdate = Instant.now().minusSeconds(1);

        testDocument.updateLastAccessed();

        assertEquals(11, testDocument.getAccessCount(),
            "Access count should be incremented");
        assertTrue(testDocument.getLastAccessed().isAfter(beforeUpdate),
            "Last accessed timestamp should be updated");
    }

    @Test
    @DisplayName("Should update last updated timestamp correctly")
    void testLastUpdatedTracking() {
        Instant beforeUpdate = Instant.now().minusSeconds(1);

        testDocument.updateLastUpdated();

        assertTrue(testDocument.getLastUpdated().isAfter(beforeUpdate),
            "Last updated timestamp should be updated");
    }
}
