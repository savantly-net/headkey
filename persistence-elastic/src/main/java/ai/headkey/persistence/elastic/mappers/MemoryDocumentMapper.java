package ai.headkey.persistence.elastic.mappers;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.persistence.elastic.documents.MemoryDocument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper class for converting between MemoryRecord DTOs and MemoryDocument Elasticsearch documents.
 *
 * This mapper handles the transformation of domain objects to Elasticsearch-optimized documents
 * and vice versa, including proper handling of vector embeddings, metadata flattening,
 * and field mapping for efficient search and aggregation operations.
 *
 * Features:
 * - Bidirectional mapping between DTO and document
 * - Vector embedding conversion between double[] and List<Double>
 * - Metadata flattening for Elasticsearch storage
 * - Category label decomposition for searchable fields
 * - Null-safe conversions with appropriate defaults
 * - Optimized field mapping for search performance
 */
public class MemoryDocumentMapper {

    /**
     * Converts a MemoryRecord DTO to a MemoryDocument for Elasticsearch storage.
     *
     * @param memoryRecord The MemoryRecord DTO to convert
     * @return The corresponding MemoryDocument, or null if input is null
     */
    public static MemoryDocument toDocument(MemoryRecord memoryRecord) {
        if (memoryRecord == null) {
            return null;
        }

        MemoryDocument document = new MemoryDocument();

        // Basic fields
        document.setId(memoryRecord.getId());
        document.setAgentId(memoryRecord.getAgentId());
        document.setContent(memoryRecord.getContent());
        document.setRelevanceScore(memoryRecord.getRelevanceScore());
        document.setVersion(memoryRecord.getVersion());
        document.setActive(true); // MemoryRecord doesn't have active field, default to true

        // Timestamps
        document.setCreatedAt(memoryRecord.getCreatedAt());
        document.setLastAccessed(memoryRecord.getLastAccessed());
        document.setLastUpdated(memoryRecord.getLastAccessed()); // Use lastAccessed as lastUpdated

        // Category mapping
        if (memoryRecord.getCategory() != null) {
            CategoryLabel category = memoryRecord.getCategory();
            document.setPrimaryCategory(category.getPrimary());
            document.setSecondaryCategory(category.getSecondary());
            document.setCategoryConfidence(category.getConfidence());

            // Convert tags to list
            if (category.getTags() != null && !category.getTags().isEmpty()) {
                document.setTags(new ArrayList<>(category.getTags()));
            }
        }

        // Metadata mapping
        if (memoryRecord.getMetadata() != null) {
            Metadata metadata = memoryRecord.getMetadata();

            // Extract commonly used metadata fields for direct indexing
            document.setSource(metadata.getSource());
            document.setImportanceScore(metadata.getImportance());
            document.setAccessCount(metadata.getAccessCount());

            // Store all metadata properties in a nested object
            Map<String, Object> metadataMap = new HashMap<>();
            if (metadata.getProperties() != null) {
                metadataMap.putAll(metadata.getProperties());
            }

            // Add metadata fields to the map
            if (metadata.getImportance() != null) {
                metadataMap.put("importance", metadata.getImportance());
            }
            if (metadata.getSource() != null) {
                metadataMap.put("source", metadata.getSource());
            }
            if (metadata.getAccessCount() != null) {
                metadataMap.put("accessCount", metadata.getAccessCount());
            }
            if (metadata.getConfidence() != null) {
                metadataMap.put("confidence", metadata.getConfidence());
            }
            if (metadata.getTags() != null) {
                metadataMap.put("tags", metadata.getTags());
            }
            if (metadata.getLastAccessed() != null) {
                metadataMap.put("lastAccessed", metadata.getLastAccessed().toString());
            }

            document.setMetadata(metadataMap);
        }

        return document;
    }

    /**
     * Converts a MemoryDocument with vector embedding to a MemoryDocument.
     *
     * @param memoryRecord The MemoryRecord DTO
     * @param embedding The vector embedding as double array
     * @return The MemoryDocument with embedding
     */
    public static MemoryDocument toDocumentWithEmbedding(MemoryRecord memoryRecord, double[] embedding) {
        MemoryDocument document = toDocument(memoryRecord);
        if (document != null && embedding != null) {
            document.setContentEmbedding(convertEmbeddingToList(embedding));
        }
        return document;
    }

    /**
     * Converts a MemoryDocument back to a MemoryRecord DTO.
     *
     * @param document The MemoryDocument to convert
     * @return The corresponding MemoryRecord, or null if input is null
     */
    public static MemoryRecord fromDocument(MemoryDocument document) {
        if (document == null) {
            return null;
        }

        MemoryRecord memoryRecord = new MemoryRecord();

        // Basic fields
        memoryRecord.setId(document.getId());
        memoryRecord.setAgentId(document.getAgentId());
        memoryRecord.setContent(document.getContent());
        memoryRecord.setRelevanceScore(document.getRelevanceScore());
        memoryRecord.setVersion(document.getVersion());

        // Timestamps
        memoryRecord.setCreatedAt(document.getCreatedAt());
        memoryRecord.setLastAccessed(document.getLastAccessed());

        // Category reconstruction
        if (document.getPrimaryCategory() != null) {
            CategoryLabel category = new CategoryLabel();
            category.setPrimary(document.getPrimaryCategory());
            category.setSecondary(document.getSecondaryCategory());
            category.setConfidence(document.getCategoryConfidence() != null ?
                                 document.getCategoryConfidence() : 0.0);

            // Convert tags list back to set
            if (document.getTags() != null && !document.getTags().isEmpty()) {
                category.setTags(document.getTags().stream().collect(Collectors.toSet()));
            }

            memoryRecord.setCategory(category);
        }

        // Metadata reconstruction
        Metadata metadata = new Metadata();

        // Set direct fields
        metadata.setSource(document.getSource());
        metadata.setImportance(document.getImportanceScore());
        metadata.setAccessCount(document.getAccessCount());
        metadata.setLastAccessed(document.getLastAccessed());

        // Add metadata from nested object
        if (document.getMetadata() != null) {
            Map<String, Object> properties = new HashMap<>(document.getMetadata());

            // Remove direct fields from properties to avoid duplication
            properties.remove("importance");
            properties.remove("source");
            properties.remove("accessCount");
            properties.remove("lastAccessed");
            properties.remove("confidence");
            properties.remove("tags");

            metadata.setProperties(properties);

            // Extract other metadata fields
            Object confidence = document.getMetadata().get("confidence");
            if (confidence instanceof Number) {
                metadata.setConfidence(((Number) confidence).doubleValue());
            }

            Object tags = document.getMetadata().get("tags");
            if (tags instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> tagList = (List<String>) tags;
                metadata.setTags(tagList.stream().collect(Collectors.toSet()));
            }
        }

        memoryRecord.setMetadata(metadata);

        return memoryRecord;
    }

    /**
     * Extracts the vector embedding from a MemoryDocument as a double array.
     *
     * @param document The MemoryDocument containing the embedding
     * @return The embedding as double array, or null if not present
     */
    public static double[] extractEmbedding(MemoryDocument document) {
        if (document == null || document.getContentEmbedding() == null) {
            return null;
        }

        return convertEmbeddingToArray(document.getContentEmbedding());
    }

    /**
     * Updates a MemoryDocument with a new vector embedding.
     *
     * @param document The document to update
     * @param embedding The new embedding
     */
    public static void setEmbedding(MemoryDocument document, double[] embedding) {
        if (document != null) {
            document.setContentEmbedding(convertEmbeddingToList(embedding));
        }
    }

    /**
     * Converts a double array embedding to a List<Double> for Elasticsearch storage.
     *
     * @param embedding The embedding as double array
     * @return The embedding as List<Double>, or null if input is null
     */
    private static List<Double> convertEmbeddingToList(double[] embedding) {
        if (embedding == null) {
            return null;
        }

        List<Double> embeddingList = new ArrayList<>(embedding.length);
        for (double value : embedding) {
            embeddingList.add(value);
        }
        return embeddingList;
    }

    /**
     * Converts a List<Double> embedding to a double array.
     *
     * @param embedding The embedding as List<Double>
     * @return The embedding as double array, or null if input is null
     */
    private static double[] convertEmbeddingToArray(List<Double> embedding) {
        if (embedding == null) {
            return null;
        }

        double[] embeddingArray = new double[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            embeddingArray[i] = embedding.get(i);
        }
        return embeddingArray;
    }

    /**
     * Creates a minimal MemoryDocument with only required fields for indexing.
     * Useful for bulk operations where full document conversion is not needed.
     *
     * @param id The memory ID
     * @param agentId The agent ID
     * @param content The memory content
     * @return A minimal MemoryDocument
     */
    public static MemoryDocument createMinimalDocument(String id, String agentId, String content) {
        MemoryDocument document = new MemoryDocument();
        document.setId(id);
        document.setAgentId(agentId);
        document.setContent(content);
        return document;
    }

    /**
     * Updates only the mutable fields of a MemoryDocument from a MemoryRecord.
     * This is useful for update operations where we want to preserve certain
     * Elasticsearch-specific fields while updating the business data.
     *
     * @param document The document to update
     * @param memoryRecord The source DTO with updated data
     */
    public static void updateMutableFields(MemoryDocument document, MemoryRecord memoryRecord) {
        if (document == null || memoryRecord == null) {
            return;
        }

        // Update mutable fields
        document.setContent(memoryRecord.getContent());
        document.setRelevanceScore(memoryRecord.getRelevanceScore());
        document.setLastAccessed(memoryRecord.getLastAccessed());
        document.setVersion(memoryRecord.getVersion());
        document.updateLastUpdated();

        // Update category if present
        if (memoryRecord.getCategory() != null) {
            CategoryLabel category = memoryRecord.getCategory();
            document.setPrimaryCategory(category.getPrimary());
            document.setSecondaryCategory(category.getSecondary());
            document.setCategoryConfidence(category.getConfidence());

            if (category.getTags() != null) {
                document.setTags(new ArrayList<>(category.getTags()));
            }
        }

        // Update metadata if present
        if (memoryRecord.getMetadata() != null) {
            Metadata metadata = memoryRecord.getMetadata();
            document.setSource(metadata.getSource());
            document.setImportanceScore(metadata.getImportance());
            document.setAccessCount(metadata.getAccessCount());

            // Update metadata map
            Map<String, Object> metadataMap = new HashMap<>();
            if (metadata.getProperties() != null) {
                metadataMap.putAll(metadata.getProperties());
            }

            // Add metadata fields
            if (metadata.getImportance() != null) {
                metadataMap.put("importance", metadata.getImportance());
            }
            if (metadata.getSource() != null) {
                metadataMap.put("source", metadata.getSource());
            }
            if (metadata.getAccessCount() != null) {
                metadataMap.put("accessCount", metadata.getAccessCount());
            }
            if (metadata.getConfidence() != null) {
                metadataMap.put("confidence", metadata.getConfidence());
            }

            document.setMetadata(metadataMap);
        }
    }
}
