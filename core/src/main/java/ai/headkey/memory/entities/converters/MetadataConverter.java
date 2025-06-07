package ai.headkey.memory.entities.converters;

import ai.headkey.memory.dto.Metadata;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;

/**
 * JPA AttributeConverter for Metadata to JSON conversion.
 * 
 * This converter handles the serialization and deserialization of Metadata
 * objects to/from JSON strings for database storage. It uses Jackson ObjectMapper
 * for reliable JSON processing and handles null values gracefully.
 * 
 * The converter is automatically applied to all Metadata fields in JPA entities
 * due to the @Converter(autoApply = true) annotation.
 * 
 * @since 1.0
 */
@Converter(autoApply = true)
public class MetadataConverter implements AttributeConverter<Metadata, String> {
    
    private static final ObjectMapper objectMapper;
    
    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // Configure ObjectMapper to handle various edge cases
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED, false);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
    }
    
    /**
     * Converts Metadata object to JSON string for database storage.
     * 
     * @param attribute The Metadata object to convert
     * @return JSON string representation, or null if attribute is null
     * @throws RuntimeException if JSON serialization fails
     */
    @Override
    public String convertToDatabaseColumn(Metadata attribute) {
        if (attribute == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert Metadata to JSON", e);
        }
    }
    
    /**
     * Converts JSON string from database to Metadata object.
     * 
     * @param dbData The JSON string from database
     * @return Metadata object, or null if dbData is null or empty
     * @throws RuntimeException if JSON deserialization fails
     */
    @Override
    public Metadata convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(dbData, Metadata.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert JSON to Metadata", e);
        }
    }
}