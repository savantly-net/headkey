package ai.headkey.persistence.mappers;

import ai.headkey.memory.dto.Belief;
import ai.headkey.persistence.entities.BeliefEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Mapper for converting between Belief DTOs and BeliefEntity JPA entities.
 * 
 * This mapper handles the conversion between the domain model (Belief DTO)
 * and the persistence model (BeliefEntity) ensuring that all fields are
 * properly mapped and type conversions are handled correctly.
 * 
 * The mapper is stateless and thread-safe, using only static methods
 * for conversion operations.
 * 
 * @since 1.0
 */
public class BeliefMapper {

    /**
     * Converts a Belief DTO to a BeliefEntity.
     * 
     * @param belief The Belief DTO to convert
     * @return The corresponding BeliefEntity, or null if input is null
     */
    public static BeliefEntity toEntity(Belief belief) {
        if (belief == null) {
            return null;
        }

        BeliefEntity entity = new BeliefEntity();
        
        // Copy basic fields
        entity.setId(belief.getId());
        entity.setAgentId(belief.getAgentId());
        entity.setStatement(belief.getStatement());
        entity.setConfidence(belief.getConfidence());
        entity.setCategory(belief.getCategory());
        entity.setCreatedAt(belief.getCreatedAt());
        entity.setLastUpdated(belief.getLastUpdated());
        entity.setReinforcementCount(belief.getReinforcementCount());
        entity.setActive(belief.isActive());
        
        // Copy collections
        if (belief.getEvidenceMemoryIds() != null) {
            entity.setEvidenceMemoryIds(new HashSet<>(belief.getEvidenceMemoryIds()));
        }
        
        if (belief.getTags() != null) {
            entity.setTags(new HashSet<>(belief.getTags()));
        }
        
        return entity;
    }

    /**
     * Converts a BeliefEntity to a Belief DTO.
     * 
     * @param entity The BeliefEntity to convert
     * @return The corresponding Belief DTO, or null if input is null
     */
    public static Belief toDto(BeliefEntity entity) {
        if (entity == null) {
            return null;
        }

        Belief belief = new Belief();
        
        // Copy basic fields
        belief.setId(entity.getId());
        belief.setAgentId(entity.getAgentId());
        belief.setStatement(entity.getStatement());
        belief.setConfidence(entity.getConfidence());
        belief.setCategory(entity.getCategory());
        belief.setCreatedAt(entity.getCreatedAt());
        belief.setLastUpdated(entity.getLastUpdated());
        belief.setReinforcementCount(entity.getReinforcementCount());
        belief.setActive(entity.getActive());
        
        // Copy collections
        if (entity.getEvidenceMemoryIds() != null) {
            belief.setEvidenceMemoryIds(new HashSet<>(entity.getEvidenceMemoryIds()));
        }
        
        if (entity.getTags() != null) {
            belief.setTags(new HashSet<>(entity.getTags()));
        }
        
        return belief;
    }

    /**
     * Updates an existing BeliefEntity with data from a Belief DTO.
     * 
     * This method updates all mutable fields while preserving JPA-managed
     * fields like version and timestamps that should not be overwritten.
     * 
     * @param entity The BeliefEntity to update
     * @param belief The Belief DTO containing the new data
     */
    public static void updateEntity(BeliefEntity entity, Belief belief) {
        if (entity == null || belief == null) {
            return;
        }

        // Update basic fields (ID should not change)
        entity.setAgentId(belief.getAgentId());
        entity.setStatement(belief.getStatement());
        entity.setConfidence(belief.getConfidence());
        entity.setCategory(belief.getCategory());
        entity.setReinforcementCount(belief.getReinforcementCount());
        entity.setActive(belief.isActive());
        
        // Update timestamps from DTO if they're newer or if entity doesn't have them
        if (belief.getCreatedAt() != null && entity.getCreatedAt() == null) {
            entity.setCreatedAt(belief.getCreatedAt());
        }
        if (belief.getLastUpdated() != null) {
            entity.setLastUpdated(belief.getLastUpdated());
        }
        
        // Update collections
        if (belief.getEvidenceMemoryIds() != null) {
            entity.setEvidenceMemoryIds(new HashSet<>(belief.getEvidenceMemoryIds()));
        }
        
        if (belief.getTags() != null) {
            entity.setTags(new HashSet<>(belief.getTags()));
        }
    }

    /**
     * Creates a new BeliefEntity from a Belief DTO, preserving the entity's
     * existing JPA-managed fields if the entity already exists.
     * 
     * @param belief The Belief DTO to convert
     * @param existingEntity Optional existing entity to preserve JPA fields from
     * @return A new or updated BeliefEntity
     */
    public static BeliefEntity toEntityPreservingJpaFields(Belief belief, BeliefEntity existingEntity) {
        if (belief == null) {
            return null;
        }

        BeliefEntity entity;
        
        if (existingEntity != null) {
            // Update existing entity
            entity = existingEntity;
            updateEntity(entity, belief);
        } else {
            // Create new entity
            entity = toEntity(belief);
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
    public static void copyBusinessFields(BeliefEntity source, BeliefEntity target) {
        if (source == null || target == null) {
            return;
        }

        target.setStatement(source.getStatement());
        target.setConfidence(source.getConfidence());
        target.setCategory(source.getCategory());
        target.setReinforcementCount(source.getReinforcementCount());
        target.setActive(source.getActive());
        
        // Copy collections
        if (source.getEvidenceMemoryIds() != null) {
            target.setEvidenceMemoryIds(new HashSet<>(source.getEvidenceMemoryIds()));
        }
        
        if (source.getTags() != null) {
            target.setTags(new HashSet<>(source.getTags()));
        }
    }

    /**
     * Creates a shallow copy of a BeliefEntity.
     * 
     * @param source The entity to copy
     * @return A new BeliefEntity with the same data
     */
    public static BeliefEntity copy(BeliefEntity source) {
        if (source == null) {
            return null;
        }

        BeliefEntity copy = new BeliefEntity();
        
        copy.setId(source.getId());
        copy.setAgentId(source.getAgentId());
        copy.setStatement(source.getStatement());
        copy.setConfidence(source.getConfidence());
        copy.setCategory(source.getCategory());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setLastUpdated(source.getLastUpdated());
        copy.setReinforcementCount(source.getReinforcementCount());
        copy.setActive(source.getActive());
        
        // Copy collections
        if (source.getEvidenceMemoryIds() != null) {
            copy.setEvidenceMemoryIds(new HashSet<>(source.getEvidenceMemoryIds()));
        }
        
        if (source.getTags() != null) {
            copy.setTags(new HashSet<>(source.getTags()));
        }
        
        return copy;
    }

    // Private constructor to prevent instantiation
    private BeliefMapper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}