package ai.headkey.persistence.elastic.mappers;

import ai.headkey.memory.dto.BeliefRelationship;
import ai.headkey.memory.enums.RelationshipType;
import ai.headkey.persistence.elastic.documents.BeliefRelationshipDocument;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapper class for converting between BeliefRelationship DTOs and BeliefRelationshipDocument Elasticsearch documents.
 *
 * This mapper handles the transformation of belief relationship domain objects to Elasticsearch-optimized documents
 * and vice versa, including proper handling of relationship types, metadata, temporal constraints,
 * and field mapping for efficient graph traversal and search operations.
 *
 * Features:
 * - Bidirectional mapping between DTO and document
 * - RelationshipType enum handling and derived field population
 * - Metadata map conversion
 * - Temporal relationship support
 * - Null-safe conversions with appropriate defaults
 * - Optimized field mapping for graph queries
 */
public class BeliefRelationshipDocumentMapper {

    /**
     * Converts a BeliefRelationship DTO to a BeliefRelationshipDocument for Elasticsearch storage.
     *
     * @param relationship The BeliefRelationship DTO to convert
     * @return The corresponding BeliefRelationshipDocument, or null if input is null
     */
    public static BeliefRelationshipDocument toDocument(BeliefRelationship relationship) {
        if (relationship == null) {
            return null;
        }

        BeliefRelationshipDocument document = new BeliefRelationshipDocument();

        // Basic fields
        document.setId(relationship.getId());
        document.setAgentId(relationship.getAgentId());
        document.setSourceBeliefId(relationship.getSourceBeliefId());
        document.setTargetBeliefId(relationship.getTargetBeliefId());
        document.setStrength(relationship.getStrength());
        document.setActive(relationship.isActive());
        document.setVersion(1L); // Default version for new documents

        // Relationship type with derived fields
        if (relationship.getRelationshipType() != null) {
            document.setRelationshipTypeEnum(relationship.getRelationshipType());
        }

        // Timestamps
        document.setCreatedAt(relationship.getCreatedAt());
        document.setLastUpdated(relationship.getLastUpdated());

        // Temporal fields
        document.setEffectiveFrom(relationship.getEffectiveFrom());
        document.setEffectiveUntil(relationship.getEffectiveUntil());

        // Reason field from deprecation reason
        document.setReason(relationship.getDeprecationReason());

        // Metadata
        if (relationship.getMetadata() != null) {
            Map<String, Object> metadata = new HashMap<>(relationship.getMetadata());

            // Add additional fields to metadata for searchability
            if (relationship.getPriority() != null) {
                metadata.put("priority", relationship.getPriority());
            }
            if (relationship.getDeprecationReason() != null) {
                metadata.put("deprecationReason", relationship.getDeprecationReason());
            }

            document.setMetadata(metadata);
        } else if (relationship.getPriority() != null || relationship.getDeprecationReason() != null) {
            // Create metadata map for additional fields even if no metadata exists
            Map<String, Object> metadata = new HashMap<>();
            if (relationship.getPriority() != null) {
                metadata.put("priority", relationship.getPriority());
            }
            if (relationship.getDeprecationReason() != null) {
                metadata.put("deprecationReason", relationship.getDeprecationReason());
            }
            document.setMetadata(metadata);
        }

        return document;
    }

    /**
     * Converts a BeliefRelationshipDocument back to a BeliefRelationship DTO.
     *
     * @param document The BeliefRelationshipDocument to convert
     * @return The corresponding BeliefRelationship, or null if input is null
     */
    public static BeliefRelationship fromDocument(BeliefRelationshipDocument document) {
        if (document == null) {
            return null;
        }

        BeliefRelationship relationship = new BeliefRelationship();

        // Basic fields
        relationship.setId(document.getId());
        relationship.setAgentId(document.getAgentId());
        relationship.setSourceBeliefId(document.getSourceBeliefId());
        relationship.setTargetBeliefId(document.getTargetBeliefId());
        relationship.setStrength(document.getStrength() != null ? document.getStrength() : 0.5);
        relationship.setActive(document.getActive() != null ? document.getActive() : true);

        // Relationship type
        if (document.getRelationshipType() != null) {
            RelationshipType type = RelationshipType.fromCode(document.getRelationshipType());
            relationship.setRelationshipType(type);
        }

        // Timestamps
        relationship.setCreatedAt(document.getCreatedAt());
        relationship.setLastUpdated(document.getLastUpdated());

        // Temporal fields
        relationship.setEffectiveFrom(document.getEffectiveFrom());
        relationship.setEffectiveUntil(document.getEffectiveUntil());

        // Reason field to deprecation reason
        relationship.setDeprecationReason(document.getReason());

        // Metadata reconstruction
        if (document.getMetadata() != null) {
            Map<String, Object> metadata = new HashMap<>(document.getMetadata());

            // Extract priority from metadata
            Object priority = metadata.remove("priority");
            if (priority instanceof Number) {
                relationship.setPriority(((Number) priority).intValue());
            }

            // Extract deprecation reason from metadata if not already set
            if (relationship.getDeprecationReason() == null) {
                Object deprecationReason = metadata.remove("deprecationReason");
                if (deprecationReason instanceof String) {
                    relationship.setDeprecationReason((String) deprecationReason);
                }
            } else {
                // Remove deprecation reason from metadata to avoid duplication
                metadata.remove("deprecationReason");
            }

            relationship.setMetadata(metadata);
        }

        return relationship;
    }

    /**
     * Creates a minimal BeliefRelationshipDocument with only required fields for indexing.
     * Useful for bulk operations where full document conversion is not needed.
     *
     * @param id The relationship ID
     * @param agentId The agent ID
     * @param sourceBeliefId The source belief ID
     * @param targetBeliefId The target belief ID
     * @param relationshipType The relationship type
     * @return A minimal BeliefRelationshipDocument
     */
    public static BeliefRelationshipDocument createMinimalDocument(String id, String agentId,
                                                                  String sourceBeliefId, String targetBeliefId,
                                                                  RelationshipType relationshipType) {
        BeliefRelationshipDocument document = new BeliefRelationshipDocument();
        document.setId(id);
        document.setAgentId(agentId);
        document.setSourceBeliefId(sourceBeliefId);
        document.setTargetBeliefId(targetBeliefId);
        if (relationshipType != null) {
            document.setRelationshipTypeEnum(relationshipType);
        }
        return document;
    }

    /**
     * Updates only the mutable fields of a BeliefRelationshipDocument from a BeliefRelationship.
     * This is useful for update operations where we want to preserve certain
     * Elasticsearch-specific fields while updating the business data.
     *
     * @param document The document to update
     * @param relationship The source DTO with updated data
     */
    public static void updateMutableFields(BeliefRelationshipDocument document, BeliefRelationship relationship) {
        if (document == null || relationship == null) {
            return;
        }

        // Update mutable fields
        document.setStrength(relationship.getStrength());
        document.setActive(relationship.isActive());
        document.setLastUpdated(relationship.getLastUpdated());
        document.updateLastUpdated();

        // Update relationship type if changed
        if (relationship.getRelationshipType() != null) {
            document.setRelationshipTypeEnum(relationship.getRelationshipType());
        }

        // Update temporal fields
        document.setEffectiveFrom(relationship.getEffectiveFrom());
        document.setEffectiveUntil(relationship.getEffectiveUntil());
        document.setReason(relationship.getDeprecationReason());

        // Update metadata
        if (relationship.getMetadata() != null) {
            Map<String, Object> metadata = new HashMap<>(relationship.getMetadata());

            // Add additional fields to metadata
            if (relationship.getPriority() != null) {
                metadata.put("priority", relationship.getPriority());
            }
            if (relationship.getDeprecationReason() != null) {
                metadata.put("deprecationReason", relationship.getDeprecationReason());
            }

            document.setMetadata(metadata);
        }
    }

    /**
     * Creates a temporal relationship document with effective period.
     *
     * @param relationship The base relationship
     * @param effectiveFrom When the relationship becomes effective
     * @param effectiveUntil When the relationship expires
     * @param reason The reason for the temporal constraint
     * @return The temporal relationship document
     */
    public static BeliefRelationshipDocument createTemporalDocument(BeliefRelationship relationship,
                                                                   java.time.Instant effectiveFrom,
                                                                   java.time.Instant effectiveUntil,
                                                                   String reason) {
        BeliefRelationshipDocument document = toDocument(relationship);
        if (document != null) {
            document.setEffectiveFrom(effectiveFrom);
            document.setEffectiveUntil(effectiveUntil);
            document.setReason(reason);
        }
        return document;
    }

    /**
     * Checks if a relationship document is currently effective based on temporal constraints.
     *
     * @param document The relationship document to check
     * @return true if the relationship is currently effective
     */
    public static boolean isCurrentlyEffective(BeliefRelationshipDocument document) {
        if (document == null || !Boolean.TRUE.equals(document.getActive())) {
            return false;
        }
        return document.isCurrentlyEffective();
    }

    /**
     * Updates the strength of a relationship document.
     *
     * @param document The document to update
     * @param newStrength The new strength value
     */
    public static void updateStrength(BeliefRelationshipDocument document, double newStrength) {
        if (document != null) {
            document.setStrength(newStrength);
            document.updateLastUpdated();
        }
    }

    /**
     * Deactivates a relationship document.
     *
     * @param document The document to deactivate
     */
    public static void deactivateRelationship(BeliefRelationshipDocument document) {
        if (document != null) {
            document.deactivate();
        }
    }

    /**
     * Reactivates a relationship document.
     *
     * @param document The document to reactivate
     */
    public static void reactivateRelationship(BeliefRelationshipDocument document) {
        if (document != null) {
            document.reactivate();
        }
    }

    /**
     * Adds metadata to a relationship document.
     *
     * @param document The document to update
     * @param key The metadata key
     * @param value The metadata value
     */
    public static void addMetadata(BeliefRelationshipDocument document, String key, Object value) {
        if (document == null || key == null || value == null) {
            return;
        }

        Map<String, Object> metadata = document.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
            document.setMetadata(metadata);
        }
        metadata.put(key, value);
        document.updateLastUpdated();
    }

    /**
     * Removes metadata from a relationship document.
     *
     * @param document The document to update
     * @param key The metadata key to remove
     * @return The removed value, or null if not found
     */
    public static Object removeMetadata(BeliefRelationshipDocument document, String key) {
        if (document == null || key == null || document.getMetadata() == null) {
            return null;
        }

        Object removed = document.getMetadata().remove(key);
        if (removed != null) {
            document.updateLastUpdated();
        }
        return removed;
    }
}
