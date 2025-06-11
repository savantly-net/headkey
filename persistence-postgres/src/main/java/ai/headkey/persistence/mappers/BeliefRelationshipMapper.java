package ai.headkey.persistence.mappers;

import ai.headkey.memory.dto.BeliefRelationship;
import ai.headkey.persistence.entities.BeliefRelationshipEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for mapping between BeliefRelationship DTOs and BeliefRelationshipEntity JPA entities.
 * 
 * This mapper handles the conversion of belief relationship data between the API layer DTOs
 * and the persistence layer entities, including proper handling of metadata conversion
 * and null safety checks.
 * 
 * @since 1.0
 */
public class BeliefRelationshipMapper {
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private BeliefRelationshipMapper() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Converts a BeliefRelationship DTO to a BeliefRelationshipEntity.
     * 
     * @param dto The BeliefRelationship DTO to convert
     * @return The corresponding BeliefRelationshipEntity, or null if input is null
     */
    public static BeliefRelationshipEntity toEntity(BeliefRelationship dto) {
        if (dto == null) {
            return null;
        }
        
        BeliefRelationshipEntity entity = new BeliefRelationshipEntity();
        
        // Map basic fields
        entity.setId(dto.getId());
        entity.setSourceBeliefId(dto.getSourceBeliefId());
        entity.setTargetBeliefId(dto.getTargetBeliefId());
        entity.setAgentId(dto.getAgentId());
        entity.setRelationshipType(dto.getRelationshipType());
        entity.setStrength(dto.getStrength());
        entity.setEffectiveFrom(dto.getEffectiveFrom());
        entity.setEffectiveUntil(dto.getEffectiveUntil());
        entity.setDeprecationReason(dto.getDeprecationReason());
        entity.setPriority(dto.getPriority());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setLastUpdated(dto.getLastUpdated());
        entity.setActive(dto.isActive());
        
        // Convert metadata from Object map to String map
        if (dto.getMetadata() != null && !dto.getMetadata().isEmpty()) {
            Map<String, String> entityMetadata = new HashMap<>();
            for (Map.Entry<String, Object> entry : dto.getMetadata().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    entityMetadata.put(entry.getKey(), entry.getValue().toString());
                }
            }
            entity.setMetadata(entityMetadata);
        }
        
        return entity;
    }
    
    /**
     * Converts a BeliefRelationshipEntity to a BeliefRelationship DTO.
     * 
     * @param entity The BeliefRelationshipEntity to convert
     * @return The corresponding BeliefRelationship DTO, or null if input is null
     */
    public static BeliefRelationship toDto(BeliefRelationshipEntity entity) {
        if (entity == null) {
            return null;
        }
        
        BeliefRelationship dto = new BeliefRelationship();
        
        // Map basic fields
        dto.setId(entity.getId());
        dto.setSourceBeliefId(entity.getSourceBeliefId());
        dto.setTargetBeliefId(entity.getTargetBeliefId());
        dto.setAgentId(entity.getAgentId());
        dto.setRelationshipType(entity.getRelationshipType());
        dto.setStrength(entity.getStrength());
        dto.setEffectiveFrom(entity.getEffectiveFrom());
        dto.setEffectiveUntil(entity.getEffectiveUntil());
        dto.setDeprecationReason(entity.getDeprecationReason());
        dto.setPriority(entity.getPriority());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setLastUpdated(entity.getLastUpdated());
        dto.setActive(entity.getActive());
        
        // Convert metadata from String map to Object map
        if (entity.getMetadata() != null && !entity.getMetadata().isEmpty()) {
            Map<String, Object> dtoMetadata = new HashMap<>();
            for (Map.Entry<String, String> entry : entity.getMetadata().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    dtoMetadata.put(entry.getKey(), entry.getValue());
                }
            }
            dto.setMetadata(dtoMetadata);
        }
        
        return dto;
    }
    
    /**
     * Converts a list of BeliefRelationship DTOs to a list of BeliefRelationshipEntities.
     * 
     * @param dtos The list of BeliefRelationship DTOs to convert
     * @return The corresponding list of BeliefRelationshipEntities
     */
    public static List<BeliefRelationshipEntity> toEntities(List<BeliefRelationship> dtos) {
        if (dtos == null) {
            return null;
        }
        
        return dtos.stream()
                .map(BeliefRelationshipMapper::toEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Converts a list of BeliefRelationshipEntities to a list of BeliefRelationship DTOs.
     * 
     * @param entities The list of BeliefRelationshipEntities to convert
     * @return The corresponding list of BeliefRelationship DTOs
     */
    public static List<BeliefRelationship> toDtos(List<BeliefRelationshipEntity> entities) {
        if (entities == null) {
            return null;
        }
        
        return entities.stream()
                .map(BeliefRelationshipMapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Updates an existing BeliefRelationshipEntity with values from a BeliefRelationship DTO.
     * This method preserves the entity's version and other JPA-managed fields.
     * 
     * @param entity The existing entity to update
     * @param dto The DTO containing the new values
     * @return The updated entity
     */
    public static BeliefRelationshipEntity updateEntityFromDto(BeliefRelationshipEntity entity, BeliefRelationship dto) {
        if (entity == null || dto == null) {
            return entity;
        }
        
        // Update fields that can be modified
        entity.setStrength(dto.getStrength());
        entity.setEffectiveFrom(dto.getEffectiveFrom());
        entity.setEffectiveUntil(dto.getEffectiveUntil());
        entity.setDeprecationReason(dto.getDeprecationReason());
        entity.setPriority(dto.getPriority());
        entity.setActive(dto.isActive());
        
        // Update metadata
        if (dto.getMetadata() != null) {
            Map<String, String> entityMetadata = new HashMap<>();
            for (Map.Entry<String, Object> entry : dto.getMetadata().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    entityMetadata.put(entry.getKey(), entry.getValue().toString());
                }
            }
            entity.setMetadata(entityMetadata);
        } else {
            entity.setMetadata(new HashMap<>());
        }
        
        // Note: We don't update id, sourceBeliefId, targetBeliefId, agentId, relationshipType, 
        // createdAt as these are typically immutable after creation
        
        return entity;
    }
    
    /**
     * Creates a new BeliefRelationshipEntity from a DTO, setting only the fields that should be
     * set during initial creation. This method is useful for create operations where certain
     * fields should be auto-generated or managed by JPA.
     * 
     * @param dto The BeliefRelationship DTO to convert
     * @return A new BeliefRelationshipEntity with creation-appropriate fields set
     */
    public static BeliefRelationshipEntity createEntityFromDto(BeliefRelationship dto) {
        if (dto == null) {
            return null;
        }
        
        BeliefRelationshipEntity entity = new BeliefRelationshipEntity();
        
        // Set required fields for creation
        entity.setId(dto.getId());
        entity.setSourceBeliefId(dto.getSourceBeliefId());
        entity.setTargetBeliefId(dto.getTargetBeliefId());
        entity.setAgentId(dto.getAgentId());
        entity.setRelationshipType(dto.getRelationshipType());
        entity.setStrength(dto.getStrength());
        
        // Set optional fields
        entity.setEffectiveFrom(dto.getEffectiveFrom());
        entity.setEffectiveUntil(dto.getEffectiveUntil());
        entity.setDeprecationReason(dto.getDeprecationReason());
        entity.setPriority(dto.getPriority());
        entity.setActive(dto.isActive());
        
        // Convert metadata
        if (dto.getMetadata() != null && !dto.getMetadata().isEmpty()) {
            Map<String, String> entityMetadata = new HashMap<>();
            for (Map.Entry<String, Object> entry : dto.getMetadata().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    entityMetadata.put(entry.getKey(), entry.getValue().toString());
                }
            }
            entity.setMetadata(entityMetadata);
        }
        
        // Note: createdAt, lastUpdated, and version will be set by JPA lifecycle callbacks
        
        return entity;
    }
    
    /**
     * Converts metadata from Object map (DTO) to String map (Entity).
     * Handles null values and converts all values to strings.
     * 
     * @param objectMetadata The metadata map with Object values
     * @return The metadata map with String values
     */
    public static Map<String, String> convertMetadataToEntity(Map<String, Object> objectMetadata) {
        if (objectMetadata == null) {
            return new HashMap<>();
        }
        
        Map<String, String> stringMetadata = new HashMap<>();
        for (Map.Entry<String, Object> entry : objectMetadata.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                stringMetadata.put(entry.getKey(), entry.getValue().toString());
            }
        }
        
        return stringMetadata;
    }
    
    /**
     * Converts metadata from String map (Entity) to Object map (DTO).
     * 
     * @param stringMetadata The metadata map with String values
     * @return The metadata map with Object values
     */
    public static Map<String, Object> convertMetadataToDto(Map<String, String> stringMetadata) {
        if (stringMetadata == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> objectMetadata = new HashMap<>();
        for (Map.Entry<String, String> entry : stringMetadata.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                objectMetadata.put(entry.getKey(), entry.getValue());
            }
        }
        
        return objectMetadata;
    }
    
    /**
     * Validates that all required fields are present in a BeliefRelationship DTO
     * before conversion to entity.
     * 
     * @param dto The DTO to validate
     * @throws IllegalArgumentException if required fields are missing
     */
    public static void validateDtoForEntity(BeliefRelationship dto) {
        if (dto == null) {
            throw new IllegalArgumentException("BeliefRelationship DTO cannot be null");
        }
        
        if (dto.getSourceBeliefId() == null || dto.getSourceBeliefId().trim().isEmpty()) {
            throw new IllegalArgumentException("Source belief ID is required");
        }
        
        if (dto.getTargetBeliefId() == null || dto.getTargetBeliefId().trim().isEmpty()) {
            throw new IllegalArgumentException("Target belief ID is required");
        }
        
        if (dto.getAgentId() == null || dto.getAgentId().trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID is required");
        }
        
        if (dto.getRelationshipType() == null) {
            throw new IllegalArgumentException("Relationship type is required");
        }
        
        if (dto.getSourceBeliefId().equals(dto.getTargetBeliefId())) {
            throw new IllegalArgumentException("Self-referential relationships are not allowed");
        }
        
        if (dto.getStrength() < 0.0 || dto.getStrength() > 1.0) {
            throw new IllegalArgumentException("Relationship strength must be between 0.0 and 1.0");
        }
        
        // Validate temporal constraints
        if (dto.getEffectiveFrom() != null && dto.getEffectiveUntil() != null) {
            if (dto.getEffectiveFrom().isAfter(dto.getEffectiveUntil())) {
                throw new IllegalArgumentException("effectiveFrom cannot be after effectiveUntil");
            }
        }
    }
}