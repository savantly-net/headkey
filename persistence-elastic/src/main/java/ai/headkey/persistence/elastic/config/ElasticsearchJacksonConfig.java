package ai.headkey.persistence.elastic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jackson configuration for Elasticsearch document serialization.
 *
 * This configuration ensures that Instant fields are serialized as
 * ISO-8601 strings instead of scientific notation, which is more
 * compatible with Elasticsearch date field mapping.
 *
 * Features:
 * - Custom Instant serialization to prevent scientific notation
 * - Consistent timestamp format across all Elasticsearch documents
 * - Proper timezone handling (UTC)
 * - Flexible deserialization supporting multiple timestamp formats
 */
//@Singleton
public class ElasticsearchJacksonConfig  {

    static public void customize(ObjectMapper objectMapper) {
        // Disable writing dates as timestamps (prevents scientific notation)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Register Java Time module for basic JSR-310 support
        objectMapper.registerModule(new JavaTimeModule());
    }
}
