package ai.headkey.persistence.mappers;

import ai.headkey.memory.dto.BeliefConflict;
import ai.headkey.persistence.entities.BeliefConflictEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper for converting between BeliefConflict DTOs and BeliefConflictEntity JPA entities.
 * 
 * This mapper handles the conversion between the domain model (BeliefConflict DTO)
 * and the persistence model (BeliefConflictEntity) ensuring that all fields are
 * properly mapped and type conversions are handled correctly.
 * 
 * The mapper is stateless and thread-safe, using only static methods
 * for conversion operations.
 * 
 * @since 1.0
 */
public class BeliefConflictMapper {

    /**
     * Converts a BeliefConflict DTO to a BeliefConflictEntity.
     * 
     * @param conflict The BeliefConflict DTO to convert
     * @return The corresponding BeliefConflictEntity, or null if input is null
     */
    public static BeliefConflictEntity toEntity(BeliefConflict conflict) {
        if (conflict == null) {
            return null;
        }

        BeliefConflictEntity entity = new BeliefConflictEntity();
        
        // Copy basic fields
        entity.setId(conflict.getConflictId());
        entity.setAgentId(conflict.getAgentId());
        entity.setDescription(conflict.getDescription());
        entity.setConflictType("GENERAL"); // Default type since BeliefConflict doesn't have getConflictType()
        entity.setDetectedAt(conflict.getDetectedAt());
        entity.setResolved(conflict.isResolved());
        entity.setResolvedAt(conflict.getResolvedAt());
        entity.setResolutionStrategy(conflict.getResolution() != null ? conflict.getResolution().toString() : null);
        entity.setResolutionNotes(conflict.getResolutionDetails());
        entity.setNewEvidenceMemoryId(conflict.getMemoryId()); // Use memoryId as new evidence
        entity.setSeverity(conflict.getSeverity());
        entity.setAutoResolvable(false); // Default value since BeliefConflict doesn't have isAutoResolvable()
        
        // Copy collections - convert single conflicting belief ID to list
        if (conflict.getConflictingBeliefId() != null) {
            List<String> conflictingIds = new ArrayList<>();
            conflictingIds.add(conflict.getConflictingBeliefId());
            entity.setConflictingBeliefIds(conflictingIds);
        }
        
        return entity;
    }

    /**
     * Converts a BeliefConflictEntity to a BeliefConflict DTO.
     * 
     * @param entity The BeliefConflictEntity to convert
     * @return The corresponding BeliefConflict DTO, or null if input is null
     */
    public static BeliefConflict toDto(BeliefConflictEntity entity) {
        if (entity == null) {
            return null;
        }

        BeliefConflict conflict = new BeliefConflict();
        
        // Copy basic fields
        conflict.setConflictId(entity.getId());
        conflict.setAgentId(entity.getAgentId());
        conflict.setDescription(entity.getDescription());
        // Note: BeliefConflict doesn't have setConflictType() method
        conflict.setDetectedAt(entity.getDetectedAt());
        conflict.setResolved(entity.getResolved());
        conflict.setResolvedAt(entity.getResolvedAt());
        // Convert resolution strategy string back to enum if needed
        if (entity.getResolutionStrategy() != null) {
            try {
                conflict.setResolution(ai.headkey.memory.enums.ConflictResolution.valueOf(entity.getResolutionStrategy()));
            } catch (IllegalArgumentException e) {
                // Handle invalid enum values gracefully
                conflict.setResolution(null);
            }
        }
        conflict.setResolutionDetails(entity.getResolutionNotes());
        conflict.setMemoryId(entity.getNewEvidenceMemoryId()); // Map back to memoryId
        conflict.setSeverity(entity.getSeverity());
        // Note: BeliefConflict doesn't have setAutoResolvable() method
        
        // Copy collections - take first conflicting belief ID if available
        if (entity.getConflictingBeliefIds() != null && !entity.getConflictingBeliefIds().isEmpty()) {
            conflict.setConflictingBeliefId(entity.getConflictingBeliefIds().get(0));
        }
        
        return conflict;
    }

    /**
     * Updates an existing BeliefConflictEntity with data from a BeliefConflict DTO.
     * 
     * This method updates all mutable fields while preserving JPA-managed
     * fields like version and timestamps that should not be overwritten.
     * 
     * @param entity The BeliefConflictEntity to update
     * @param conflict The BeliefConflict DTO containing the new data
     */
    public static void updateEntity(BeliefConflictEntity entity, BeliefConflict conflict) {
        if (entity == null || conflict == null) {
            return;
        }

        // Update basic fields (ID should not change)
        entity.setAgentId(conflict.getAgentId());
        entity.setDescription(conflict.getDescription());
        entity.setConflictType("GENERAL"); // Default type since BeliefConflict doesn't have getConflictType()
        entity.setResolved(conflict.isResolved());
        entity.setResolvedAt(conflict.getResolvedAt());
        entity.setResolutionStrategy(conflict.getResolution() != null ? conflict.getResolution().toString() : null);
        entity.setResolutionNotes(conflict.getResolutionDetails());
        entity.setNewEvidenceMemoryId(conflict.getMemoryId());
        entity.setSeverity(conflict.getSeverity());
        entity.setAutoResolvable(false); // Default value since BeliefConflict doesn't have isAutoResolvable()
        
        // Update timestamps from DTO if they're newer or if entity doesn't have them
        if (conflict.getDetectedAt() != null && entity.getDetectedAt() == null) {
            entity.setDetectedAt(conflict.getDetectedAt());
        }
        
        // Update collections - convert single conflicting belief ID to list
        if (conflict.getConflictingBeliefId() != null) {
            List<String> conflictingIds = new ArrayList<>();
            conflictingIds.add(conflict.getConflictingBeliefId());
            entity.setConflictingBeliefIds(conflictingIds);
        }
    }

    /**
     * Creates a new BeliefConflictEntity from a BeliefConflict DTO, preserving the entity's
     * existing JPA-managed fields if the entity already exists.
     * 
     * @param conflict The BeliefConflict DTO to convert
     * @param existingEntity Optional existing entity to preserve JPA fields from
     * @return A new or updated BeliefConflictEntity
     */
    public static BeliefConflictEntity toEntityPreservingJpaFields(BeliefConflict conflict, BeliefConflictEntity existingEntity) {
        if (conflict == null) {
            return null;
        }

        BeliefConflictEntity entity;
        
        if (existingEntity != null) {
            // Update existing entity
            entity = existingEntity;
            updateEntity(entity, conflict);
        } else {
            // Create new entity
            entity = toEntity(conflict);
        }
        
        return entity;
    }

    /**
     * Copies business-related fields from source entity to target entity.
     * 
     * This method is useful for merging entities while preserving JPA
     * metadata and audit fields.
     * 
     * @param source The source entity to copy from
     * @param target The target entity to copy to
     */
    public static void copyBusinessFields(BeliefConflictEntity source, BeliefConflictEntity target) {
        if (source == null || target == null) {
            return;
        }

        target.setDescription(source.getDescription());
        target.setConflictType(source.getConflictType());
        target.setResolved(source.getResolved());
        target.setResolvedAt(source.getResolvedAt());
        target.setResolutionStrategy(source.getResolutionStrategy());
        target.setResolutionNotes(source.getResolutionNotes());
        target.setNewEvidenceMemoryId(source.getNewEvidenceMemoryId());
        target.setSeverity(source.getSeverity());
        target.setAutoResolvable(source.getAutoResolvable());
        
        // Copy collections
        if (source.getConflictingBeliefIds() != null) {
            target.setConflictingBeliefIds(new ArrayList<>(source.getConflictingBeliefIds()));
        }
    }

    /**
     * Creates a shallow copy of a BeliefConflictEntity.
     * 
     * @param source The entity to copy
     * @return A new BeliefConflictEntity with the same data
     */
    public static BeliefConflictEntity copy(BeliefConflictEntity source) {
        if (source == null) {
            return null;
        }

        BeliefConflictEntity copy = new BeliefConflictEntity();
        
        copy.setId(source.getId());
        copy.setAgentId(source.getAgentId());
        copy.setDescription(source.getDescription());
        copy.setConflictType(source.getConflictType());
        copy.setDetectedAt(source.getDetectedAt());
        copy.setResolved(source.getResolved());
        copy.setResolvedAt(source.getResolvedAt());
        copy.setResolutionStrategy(source.getResolutionStrategy());
        copy.setResolutionNotes(source.getResolutionNotes());
        copy.setNewEvidenceMemoryId(source.getNewEvidenceMemoryId());
        copy.setSeverity(source.getSeverity());
        copy.setAutoResolvable(source.getAutoResolvable());
        
        // Copy collections
        if (source.getConflictingBeliefIds() != null) {
            copy.setConflictingBeliefIds(new ArrayList<>(source.getConflictingBeliefIds()));
        }
        
        return copy;
    }

    // Private constructor to prevent instantiation
    private BeliefConflictMapper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}