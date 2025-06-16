package ai.headkey.persistence.elastic.mappers;

import ai.headkey.memory.dto.Belief;
import ai.headkey.persistence.elastic.documents.BeliefDocument;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper class for converting between Belief DTOs and BeliefDocument Elasticsearch documents.
 *
 * This mapper handles the transformation of belief domain objects to Elasticsearch-optimized documents
 * and vice versa, including proper handling of vector embeddings, evidence memory IDs,
 * and field mapping for efficient search and aggregation operations.
 *
 * Features:
 * - Bidirectional mapping between DTO and document
 * - Vector embedding conversion between double[] and List<Double>
 * - Evidence memory ID set handling
 * - Tag collection conversion
 * - Null-safe conversions with appropriate defaults
 * - Optimized field mapping for search performance
 */
public class BeliefDocumentMapper {

    /**
     * Converts a Belief DTO to a BeliefDocument for Elasticsearch storage.
     *
     * @param belief The Belief DTO to convert
     * @return The corresponding BeliefDocument, or null if input is null
     */
    public static BeliefDocument toDocument(Belief belief) {
        if (belief == null) {
            return null;
        }

        BeliefDocument document = new BeliefDocument();

        // Basic fields
        document.setId(belief.getId());
        document.setAgentId(belief.getAgentId());
        document.setStatement(belief.getStatement());
        document.setConfidence(belief.getConfidence());
        document.setCategory(belief.getCategory());
        document.setReinforcementCount(belief.getReinforcementCount());
        document.setActive(belief.isActive());
        document.setVersion(1L); // Default version for new documents

        // Timestamps
        document.setCreatedAt(belief.getCreatedAt());
        document.setLastUpdated(belief.getLastUpdated());

        // Evidence memory IDs
        if (belief.getEvidenceMemoryIds() != null) {
            document.setEvidenceMemoryIds(new HashSet<>(belief.getEvidenceMemoryIds()));
        }

        // Tags
        if (belief.getTags() != null) {
            document.setTags(new HashSet<>(belief.getTags()));
        }

        return document;
    }

    /**
     * Converts a BeliefDocument with vector embedding to a BeliefDocument.
     *
     * @param belief The Belief DTO
     * @param embedding The vector embedding as double array
     * @return The BeliefDocument with embedding
     */
    public static BeliefDocument toDocumentWithEmbedding(Belief belief, double[] embedding) {
        BeliefDocument document = toDocument(belief);
        if (document != null && embedding != null) {
            document.setStatementEmbedding(convertEmbeddingToList(embedding));
        }
        return document;
    }

    /**
     * Converts a BeliefDocument back to a Belief DTO.
     *
     * @param document The BeliefDocument to convert
     * @return The corresponding Belief, or null if input is null
     */
    public static Belief fromDocument(BeliefDocument document) {
        if (document == null) {
            return null;
        }

        Belief belief = new Belief();

        // Basic fields
        belief.setId(document.getId());
        belief.setAgentId(document.getAgentId());
        belief.setStatement(document.getStatement());
        belief.setConfidence(document.getConfidence());
        belief.setCategory(document.getCategory());
        belief.setReinforcementCount(document.getReinforcementCount());
        belief.setActive(document.getActive());

        // Timestamps
        belief.setCreatedAt(document.getCreatedAt());
        belief.setLastUpdated(document.getLastUpdated());

        // Evidence memory IDs
        if (document.getEvidenceMemoryIds() != null) {
            belief.setEvidenceMemoryIds(new HashSet<>(document.getEvidenceMemoryIds()));
        }

        // Tags
        if (document.getTags() != null) {
            belief.setTags(new HashSet<>(document.getTags()));
        }

        return belief;
    }

    /**
     * Extracts the vector embedding from a BeliefDocument as a double array.
     *
     * @param document The BeliefDocument containing the embedding
     * @return The embedding as double array, or null if not present
     */
    public static double[] extractEmbedding(BeliefDocument document) {
        if (document == null || document.getStatementEmbedding() == null) {
            return null;
        }

        return convertEmbeddingToArray(document.getStatementEmbedding());
    }

    /**
     * Updates a BeliefDocument with a new vector embedding.
     *
     * @param document The document to update
     * @param embedding The new embedding
     */
    public static void setEmbedding(BeliefDocument document, double[] embedding) {
        if (document != null) {
            document.setStatementEmbedding(convertEmbeddingToList(embedding));
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
     * Creates a minimal BeliefDocument with only required fields for indexing.
     * Useful for bulk operations where full document conversion is not needed.
     *
     * @param id The belief ID
     * @param agentId The agent ID
     * @param statement The belief statement
     * @return A minimal BeliefDocument
     */
    public static BeliefDocument createMinimalDocument(String id, String agentId, String statement) {
        BeliefDocument document = new BeliefDocument();
        document.setId(id);
        document.setAgentId(agentId);
        document.setStatement(statement);
        return document;
    }

    /**
     * Updates only the mutable fields of a BeliefDocument from a Belief.
     * This is useful for update operations where we want to preserve certain
     * Elasticsearch-specific fields while updating the business data.
     *
     * @param document The document to update
     * @param belief The source DTO with updated data
     */
    public static void updateMutableFields(BeliefDocument document, Belief belief) {
        if (document == null || belief == null) {
            return;
        }

        // Update mutable fields
        document.setStatement(belief.getStatement());
        document.setConfidence(belief.getConfidence());
        document.setCategory(belief.getCategory());
        document.setReinforcementCount(belief.getReinforcementCount());
        document.setActive(belief.isActive());
        document.setLastUpdated(belief.getLastUpdated());
        document.updateLastUpdated();

        // Update evidence memory IDs
        if (belief.getEvidenceMemoryIds() != null) {
            document.setEvidenceMemoryIds(new HashSet<>(belief.getEvidenceMemoryIds()));
        }

        // Update tags
        if (belief.getTags() != null) {
            document.setTags(new HashSet<>(belief.getTags()));
        }
    }

    /**
     * Adds evidence memory ID to a BeliefDocument.
     *
     * @param document The document to update
     * @param memoryId The memory ID to add as evidence
     */
    public static void addEvidence(BeliefDocument document, String memoryId) {
        if (document == null || memoryId == null || memoryId.trim().isEmpty()) {
            return;
        }

        Set<String> evidenceIds = document.getEvidenceMemoryIds();
        if (evidenceIds == null) {
            evidenceIds = new HashSet<>();
            document.setEvidenceMemoryIds(evidenceIds);
        }
        evidenceIds.add(memoryId);
        document.updateLastUpdated();
    }

    /**
     * Removes evidence memory ID from a BeliefDocument.
     *
     * @param document The document to update
     * @param memoryId The memory ID to remove from evidence
     * @return true if the evidence was removed
     */
    public static boolean removeEvidence(BeliefDocument document, String memoryId) {
        if (document == null || memoryId == null || document.getEvidenceMemoryIds() == null) {
            return false;
        }

        boolean removed = document.getEvidenceMemoryIds().remove(memoryId);
        if (removed) {
            document.updateLastUpdated();
        }
        return removed;
    }

    /**
     * Adds a tag to a BeliefDocument.
     *
     * @param document The document to update
     * @param tag The tag to add
     */
    public static void addTag(BeliefDocument document, String tag) {
        if (document == null || tag == null || tag.trim().isEmpty()) {
            return;
        }

        Set<String> tags = document.getTags();
        if (tags == null) {
            tags = new HashSet<>();
            document.setTags(tags);
        }
        tags.add(tag.trim());
        document.updateLastUpdated();
    }

    /**
     * Removes a tag from a BeliefDocument.
     *
     * @param document The document to update
     * @param tag The tag to remove
     * @return true if the tag was removed
     */
    public static boolean removeTag(BeliefDocument document, String tag) {
        if (document == null || tag == null || document.getTags() == null) {
            return false;
        }

        boolean removed = document.getTags().remove(tag);
        if (removed) {
            document.updateLastUpdated();
        }
        return removed;
    }
}
