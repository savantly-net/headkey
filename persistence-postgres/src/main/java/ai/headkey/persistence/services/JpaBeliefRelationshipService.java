package ai.headkey.persistence.services;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.dto.BeliefRelationship;
import ai.headkey.memory.dto.BeliefKnowledgeGraph;
import ai.headkey.memory.enums.RelationshipType;
import ai.headkey.memory.interfaces.BeliefRelationshipService;
import ai.headkey.persistence.entities.BeliefRelationshipEntity;
import ai.headkey.persistence.mappers.BeliefRelationshipMapper;
import ai.headkey.persistence.mappers.BeliefMapper;
import ai.headkey.persistence.repositories.BeliefRelationshipRepository;
import ai.headkey.persistence.repositories.BeliefRepository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA implementation of BeliefRelationshipService using PostgreSQL database.
 *
 * This implementation provides persistent storage for belief relationships
 * using JPA/Hibernate with PostgreSQL as the backend database. It includes
 * optimizations for performance, proper transaction management, and
 * comprehensive graph query capabilities.
 *
 * Key features:
 * - Full JPA/Hibernate integration
 * - PostgreSQL-specific optimizations
 * - Batch operations for performance
 * - Graph traversal capabilities
 * - Comprehensive indexing strategy
 * - Transaction management
 * - Connection pooling support
 *
 * @since 1.0
 */
public class JpaBeliefRelationshipService implements BeliefRelationshipService {

    private final BeliefRelationshipRepository relationshipRepository;
    private final BeliefRepository beliefRepository;

    // Statistics tracking
    private long totalCreateOperations = 0;
    private long totalUpdateOperations = 0;
    private long totalQueryOperations = 0;
    private final Instant createdAt;

    /**
     * Constructor for dependency injection.
     */
    public JpaBeliefRelationshipService(BeliefRelationshipRepository relationshipRepository,
                                       BeliefRepository beliefRepository) {
        this.relationshipRepository = relationshipRepository;
        this.beliefRepository = beliefRepository;
        this.createdAt = Instant.now();
    }

    // ========== Relationship Creation Methods ==========

    @Override
    public BeliefRelationship createRelationship(String sourceBeliefId, String targetBeliefId,
                                                RelationshipType relationshipType, double strength, String agentId) {
        validateRelationshipCreation(sourceBeliefId, targetBeliefId, relationshipType, strength, agentId);
        
        BeliefRelationship relationship = new BeliefRelationship(sourceBeliefId, targetBeliefId, relationshipType, strength, agentId);
        relationship.setId(generateRelationshipId());

        BeliefRelationshipEntity entity = BeliefRelationshipMapper.toEntity(relationship);
        BeliefRelationshipEntity savedEntity = relationshipRepository.save(entity);
        totalCreateOperations++;
        
        return BeliefRelationshipMapper.toDto(savedEntity);
    }

    @Override
    public BeliefRelationship createRelationshipWithMetadata(String sourceBeliefId, String targetBeliefId,
                                                           RelationshipType relationshipType, double strength,
                                                           String agentId, Map<String, Object> metadata) {
        validateRelationshipCreation(sourceBeliefId, targetBeliefId, relationshipType, strength, agentId);
        
        BeliefRelationship relationship = new BeliefRelationship(sourceBeliefId, targetBeliefId, relationshipType, strength, agentId);
        relationship.setId(generateRelationshipId());
        if (metadata != null) {
            relationship.setMetadata(new HashMap<>(metadata));
        }

        BeliefRelationshipEntity entity = BeliefRelationshipMapper.toEntity(relationship);
        BeliefRelationshipEntity savedEntity = relationshipRepository.save(entity);
        totalCreateOperations++;
        
        return BeliefRelationshipMapper.toDto(savedEntity);
    }

    @Override
    public BeliefRelationship createTemporalRelationship(String sourceBeliefId, String targetBeliefId,
                                                        RelationshipType relationshipType, double strength,
                                                        String agentId, Instant effectiveFrom, Instant effectiveUntil) {
        validateRelationshipCreation(sourceBeliefId, targetBeliefId, relationshipType, strength, agentId);
        validateTemporalConstraints(effectiveFrom, effectiveUntil);
        
        BeliefRelationship relationship = new BeliefRelationship(generateRelationshipId(), sourceBeliefId, targetBeliefId, relationshipType, strength, agentId, effectiveFrom, effectiveUntil, null);

        BeliefRelationshipEntity entity = BeliefRelationshipMapper.toEntity(relationship);
        BeliefRelationshipEntity savedEntity = relationshipRepository.save(entity);
        totalCreateOperations++;
        
        return BeliefRelationshipMapper.toDto(savedEntity);
    }

    @Override
    public BeliefRelationship deprecateBeliefWith(String oldBeliefId, String newBeliefId,
                                                 String reason, String agentId) {
        validateRelationshipCreation(oldBeliefId, newBeliefId, RelationshipType.SUPERSEDES, 1.0, agentId);
        
        BeliefRelationship relationship = new BeliefRelationship(generateRelationshipId(), newBeliefId, oldBeliefId, RelationshipType.SUPERSEDES, 1.0, agentId, null, null, reason);

        BeliefRelationshipEntity entity = BeliefRelationshipMapper.toEntity(relationship);
        BeliefRelationshipEntity savedEntity = relationshipRepository.save(entity);
        totalCreateOperations++;
        
        return BeliefRelationshipMapper.toDto(savedEntity);
    }

    // ========== Relationship Management Methods ==========

    @Override
    public BeliefRelationship updateRelationship(String relationshipId, double strength,
                                               Map<String, Object> metadata) {
        Optional<BeliefRelationshipEntity> entityOpt = relationshipRepository.findById(relationshipId);
        if (entityOpt.isEmpty()) {
            throw new IllegalArgumentException("Relationship not found: " + relationshipId);
        }

        BeliefRelationshipEntity entity = entityOpt.get();
        entity.setStrength(strength);
        if (metadata != null) {
            // Convert Object metadata to String metadata for entity
            Map<String, String> entityMetadata = new HashMap<>();
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    entityMetadata.put(entry.getKey(), entry.getValue().toString());
                }
            }
            entity.setMetadata(entityMetadata);
        }
        entity.setLastUpdated(Instant.now());

        BeliefRelationshipEntity savedEntity = relationshipRepository.save(entity);
        totalUpdateOperations++;
        
        return BeliefRelationshipMapper.toDto(savedEntity);
    }

    @Override
    public boolean deactivateRelationship(String relationshipId, String agentId) {
        Optional<BeliefRelationshipEntity> entityOpt = relationshipRepository.findById(relationshipId);
        if (entityOpt.isEmpty()) {
            return false;
        }

        BeliefRelationshipEntity entity = entityOpt.get();
        if (!entity.getAgentId().equals(agentId)) {
            return false;
        }

        entity.deactivate();
        relationshipRepository.save(entity);
        totalUpdateOperations++;
        
        return true;
    }

    @Override
    public boolean reactivateRelationship(String relationshipId, String agentId) {
        Optional<BeliefRelationshipEntity> entityOpt = relationshipRepository.findById(relationshipId);
        if (entityOpt.isEmpty()) {
            return false;
        }

        BeliefRelationshipEntity entity = entityOpt.get();
        if (!entity.getAgentId().equals(agentId)) {
            return false;
        }

        entity.reactivate();
        relationshipRepository.save(entity);
        totalUpdateOperations++;
        
        return true;
    }

    @Override
    public boolean deleteRelationship(String relationshipId, String agentId) {
        Optional<BeliefRelationshipEntity> entityOpt = relationshipRepository.findById(relationshipId);
        if (entityOpt.isEmpty()) {
            return false;
        }

        BeliefRelationshipEntity entity = entityOpt.get();
        if (!entity.getAgentId().equals(agentId)) {
            return false;
        }

        relationshipRepository.deleteById(relationshipId);
        totalUpdateOperations++;
        
        return true;
    }

    // ========== Query Methods ==========

    @Override
    public Optional<BeliefRelationship> findRelationshipById(String relationshipId, String agentId) {
        totalQueryOperations++;
        Optional<BeliefRelationshipEntity> entityOpt = relationshipRepository.findById(relationshipId);
        
        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }

        BeliefRelationshipEntity entity = entityOpt.get();
        if (!entity.getAgentId().equals(agentId)) {
            return Optional.empty();
        }

        return Optional.of(BeliefRelationshipMapper.toDto(entity));
    }

    @Override
    public List<BeliefRelationship> findRelationshipsForBelief(String beliefId, String agentId) {
        totalQueryOperations++;
        List<BeliefRelationshipEntity> entities = relationshipRepository.findByBelief(beliefId, agentId, false);
        return entities.stream()
                .map(BeliefRelationshipMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BeliefRelationship> findOutgoingRelationships(String beliefId, String agentId) {
        totalQueryOperations++;
        List<BeliefRelationshipEntity> entities = relationshipRepository.findBySourceBelief(beliefId, agentId, false);
        return entities.stream()
                .map(BeliefRelationshipMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BeliefRelationship> findIncomingRelationships(String beliefId, String agentId) {
        totalQueryOperations++;
        List<BeliefRelationshipEntity> entities = relationshipRepository.findByTargetBelief(beliefId, agentId, false);
        return entities.stream()
                .map(BeliefRelationshipMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BeliefRelationship> findRelationshipsByType(RelationshipType relationshipType, String agentId) {
        totalQueryOperations++;
        List<BeliefRelationshipEntity> entities = relationshipRepository.findByType(relationshipType, agentId, false);
        return entities.stream()
                .map(BeliefRelationshipMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BeliefRelationship> findRelationshipsBetween(String sourceBeliefId, String targetBeliefId, String agentId) {
        totalQueryOperations++;
        List<BeliefRelationshipEntity> entities = relationshipRepository.findBetweenBeliefs(sourceBeliefId, targetBeliefId, agentId, false);
        return entities.stream()
                .map(BeliefRelationshipMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findDeprecatedBeliefs(String agentId) {
        totalQueryOperations++;
        List<BeliefRelationshipEntity> deprecatingRelationships = relationshipRepository.findDeprecating(agentId);
        return deprecatingRelationships.stream()
                .map(BeliefRelationshipEntity::getTargetBeliefId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Belief> findSupersedingBeliefs(String beliefId, String agentId) {
        totalQueryOperations++;
        List<BeliefRelationshipEntity> supersedingRelationships = relationshipRepository.findByTargetBelief(beliefId, agentId, false)
                .stream()
                .filter(rel -> rel.getRelationshipType() == RelationshipType.SUPERSEDES)
                .collect(Collectors.toList());

        List<String> supersedingBeliefIds = supersedingRelationships.stream()
                .map(BeliefRelationshipEntity::getSourceBeliefId)
                .collect(Collectors.toList());

        return beliefRepository.findByIds(new HashSet<>(supersedingBeliefIds))
                .stream()
                .map(BeliefMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<Belief> findDeprecationChain(String beliefId, String agentId) {
        totalQueryOperations++;
        List<String> chainIds = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        findDeprecationChainRecursive(beliefId, agentId, chainIds, visited);

        return beliefRepository.findByIds(new HashSet<>(chainIds))
                .stream()
                .map(BeliefMapper::toDto)
                .collect(Collectors.toList());
    }

    private void findDeprecationChainRecursive(String beliefId, String agentId, List<String> chain, Set<String> visited) {
        if (visited.contains(beliefId)) {
            return;
        }
        visited.add(beliefId);
        chain.add(beliefId);

        List<BeliefRelationshipEntity> supersedingRelationships = relationshipRepository.findByTargetBelief(beliefId, agentId, false)
                .stream()
                .filter(rel -> rel.getRelationshipType() == RelationshipType.SUPERSEDES)
                .collect(Collectors.toList());

        for (BeliefRelationshipEntity rel : supersedingRelationships) {
            findDeprecationChainRecursive(rel.getSourceBeliefId(), agentId, chain, visited);
        }
    }

    @Override
    public Set<String> findRelatedBeliefs(String beliefId, String agentId, int maxDepth) {
        totalQueryOperations++;
        return new HashSet<>(relationshipRepository.findConnectedBeliefs(beliefId, agentId, maxDepth, false));
    }

    @Override
    public List<Map<String, Object>> findSimilarBeliefs(String beliefId, String agentId, double similarityThreshold) {
        totalQueryOperations++;
        // Simplified implementation - find beliefs with similar relationship patterns
        List<BeliefRelationshipEntity> beliefRelationships = relationshipRepository.findByBelief(beliefId, agentId, false);
        Set<RelationshipType> relationshipTypes = beliefRelationships.stream()
                .map(BeliefRelationshipEntity::getRelationshipType)
                .collect(Collectors.toSet());

        List<Map<String, Object>> similar = new ArrayList<>();
        
        for (RelationshipType type : relationshipTypes) {
            List<BeliefRelationshipEntity> sameTypeRelationships = relationshipRepository.findByType(type, agentId, false);
            for (BeliefRelationshipEntity rel : sameTypeRelationships) {
                String otherBeliefId = rel.getSourceBeliefId().equals(beliefId) ? rel.getTargetBeliefId() : rel.getSourceBeliefId();
                if (!otherBeliefId.equals(beliefId)) {
                    Map<String, Object> similarBelief = new HashMap<>();
                    similarBelief.put("beliefId", otherBeliefId);
                    similarBelief.put("similarity", rel.getStrength());
                    similarBelief.put("relationshipType", type);
                    similar.add(similarBelief);
                }
            }
        }

        return similar.stream()
                .filter(belief -> (Double) belief.get("similarity") >= similarityThreshold)
                .sorted((a, b) -> Double.compare((Double) b.get("similarity"), (Double) a.get("similarity")))
                .collect(Collectors.toList());
    }

    // ========== Deprecated Graph Methods ==========

    @Override
    @Deprecated
    public BeliefKnowledgeGraph getKnowledgeGraph(String agentId) {
        return createSnapshotGraph(agentId, false);
    }

    @Override
    @Deprecated
    public BeliefKnowledgeGraph getActiveKnowledgeGraph(String agentId) {
        return createSnapshotGraph(agentId, false);
    }

    @Override
    @Deprecated
    public Map<String, Object> getKnowledgeGraphStatistics(String agentId) {
        return getEfficientGraphStatistics(agentId);
    }

    @Override
    @Deprecated
    public List<String> validateKnowledgeGraph(String agentId) {
        return performEfficientGraphValidation(agentId);
    }

    // ========== Advanced Graph Operations ==========

    @Override
    public Map<String, Set<String>> findBeliefClusters(String agentId, double strengthThreshold) {
        totalQueryOperations++;
        List<List<String>> clusters = relationshipRepository.findStronglyConnectedClusters(agentId, strengthThreshold);
        Map<String, Set<String>> clusterMap = new HashMap<>();
        
        for (int i = 0; i < clusters.size(); i++) {
            clusterMap.put("cluster_" + i, new HashSet<>(clusters.get(i)));
        }
        
        return clusterMap;
    }

    @Override
    public List<Map<String, Object>> findPotentialConflicts(String agentId) {
        totalQueryOperations++;
        List<Map<String, Object>> conflicts = new ArrayList<>();
        
        // Find contradictory relationships
        List<BeliefRelationshipEntity> contradictoryRels = relationshipRepository.findByType(RelationshipType.CONTRADICTS, agentId, false);
        
        for (BeliefRelationshipEntity rel : contradictoryRels) {
            Map<String, Object> conflict = new HashMap<>();
            conflict.put("type", "contradiction");
            conflict.put("sourceBeliefId", rel.getSourceBeliefId());
            conflict.put("targetBeliefId", rel.getTargetBeliefId());
            conflict.put("strength", rel.getStrength());
            conflicts.add(conflict);
        }
        
        return conflicts;
    }

    // ========== Efficient Graph Operations ==========

    @Override
    public Map<String, Object> getEfficientGraphStatistics(String agentId) {
        totalQueryOperations++;
        Object[] stats = relationshipRepository.getRelationshipStatistics(agentId);
        List<Object[]> typeDistribution = relationshipRepository.getRelationshipTypeDistribution(agentId, false);
        List<Object[]> avgStrengthByType = relationshipRepository.getAverageStrengthByType(agentId);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalRelationships", stats[0]);
        statistics.put("activeRelationships", stats[1]);
        statistics.put("averageStrength", stats[2]);
        statistics.put("typeDistribution", typeDistribution);
        statistics.put("averageStrengthByType", avgStrengthByType);
        statistics.put("generatedAt", Instant.now());
        
        return statistics;
    }

    @Override
    public List<String> performEfficientGraphValidation(String agentId) {
        totalQueryOperations++;
        List<String> issues = new ArrayList<>();
        
        // Check for orphaned relationships
        List<BeliefRelationshipEntity> orphaned = relationshipRepository.findOrphanedRelationships(agentId);
        if (!orphaned.isEmpty()) {
            issues.add("Found " + orphaned.size() + " orphaned relationships");
        }
        
        // Check for self-referencing relationships
        List<BeliefRelationshipEntity> selfReferencing = relationshipRepository.findSelfReferencingRelationships(agentId);
        if (!selfReferencing.isEmpty()) {
            issues.add("Found " + selfReferencing.size() + " self-referencing relationships");
        }
        
        // Check for temporally invalid relationships
        List<BeliefRelationshipEntity> temporallyInvalid = relationshipRepository.findTemporallyInvalidRelationships(agentId);
        if (!temporallyInvalid.isEmpty()) {
            issues.add("Found " + temporallyInvalid.size() + " temporally invalid relationships");
        }
        
        return issues;
    }

    @Override
    public BeliefKnowledgeGraph createSnapshotGraph(String agentId, boolean includeInactive) {
        totalQueryOperations++;
        List<BeliefRelationshipEntity> relationships = relationshipRepository.findByAgent(agentId, includeInactive);
        List<BeliefRelationship> relationshipDtos = relationships.stream()
                .map(BeliefRelationshipMapper::toDto)
                .collect(Collectors.toList());

        // Get all unique belief IDs
        Set<String> beliefIds = new HashSet<>();
        relationships.forEach(rel -> {
            beliefIds.add(rel.getSourceBeliefId());
            beliefIds.add(rel.getTargetBeliefId());
        });

        List<Belief> beliefs = beliefRepository.findByIds(beliefIds)
                .stream()
                .map(BeliefMapper::toDto)
                .collect(Collectors.toList());

        BeliefKnowledgeGraph graph = new BeliefKnowledgeGraph(agentId, beliefs, relationshipDtos);
        return graph;
    }

    @Override
    public BeliefKnowledgeGraph createFilteredSnapshot(String agentId, Set<String> beliefIds,
                                                      Set<RelationshipType> relationshipTypes, int maxBeliefs) {
        totalQueryOperations++;
        List<BeliefRelationshipEntity> allRelationships = relationshipRepository.findByAgent(agentId, false);
        
        // Filter by belief IDs if specified
        if (beliefIds != null && !beliefIds.isEmpty()) {
            allRelationships = allRelationships.stream()
                    .filter(rel -> beliefIds.contains(rel.getSourceBeliefId()) || beliefIds.contains(rel.getTargetBeliefId()))
                    .collect(Collectors.toList());
        }
        
        // Filter by relationship types if specified
        if (relationshipTypes != null && !relationshipTypes.isEmpty()) {
            allRelationships = allRelationships.stream()
                    .filter(rel -> relationshipTypes.contains(rel.getRelationshipType()))
                    .collect(Collectors.toList());
        }

        List<BeliefRelationship> relationshipDtos = allRelationships.stream()
                .map(BeliefRelationshipMapper::toDto)
                .collect(Collectors.toList());

        // Get filtered belief IDs
        Set<String> filteredBeliefIds = new HashSet<>();
        allRelationships.forEach(rel -> {
            filteredBeliefIds.add(rel.getSourceBeliefId());
            filteredBeliefIds.add(rel.getTargetBeliefId());
        });

        List<Belief> beliefs = beliefRepository.findByIds(filteredBeliefIds)
                .stream()
                .map(BeliefMapper::toDto)
                .limit(maxBeliefs)
                .collect(Collectors.toList());

        BeliefKnowledgeGraph graph = new BeliefKnowledgeGraph(agentId, beliefs, relationshipDtos);
        return graph;
    }

    @Override
    public BeliefKnowledgeGraph createExportGraph(String agentId, String format) {
        // For now, return a complete snapshot regardless of format
        return createSnapshotGraph(agentId, false);
    }

    @Override
    public List<BeliefRelationship> findShortestPath(String sourceBeliefId, String targetBeliefId, String agentId) {
        totalQueryOperations++;
        List<BeliefRelationshipEntity> pathEntities = relationshipRepository.findShortestPath(sourceBeliefId, targetBeliefId, agentId);
        return pathEntities.stream()
                .map(BeliefRelationshipMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> suggestRelationships(String agentId, int maxSuggestions) {
        totalQueryOperations++;
        // Simplified implementation - suggest based on common patterns
        List<Map<String, Object>> suggestions = new ArrayList<>();
        
        List<Object[]> typeDistribution = relationshipRepository.getRelationshipTypeDistribution(agentId, false);
        for (Object[] typeCount : typeDistribution) {
            if (suggestions.size() >= maxSuggestions) break;
            
            RelationshipType type = (RelationshipType) typeCount[0];
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("suggestionType", "common_relationship_type");
            suggestion.put("relationshipType", type);
            suggestion.put("frequency", typeCount[1]);
            suggestions.add(suggestion);
        }
        
        return suggestions;
    }

    @Override
    public List<BeliefRelationship> createRelationshipsBulk(List<BeliefRelationship> relationships, String agentId) {
        List<BeliefRelationshipEntity> entities = relationships.stream()
                .map(rel -> {
                    if (rel.getId() == null) {
                        rel.setId(generateRelationshipId());
                    }
                    if (rel.getAgentId() == null) {
                        rel.setAgentId(agentId);
                    }
                    return BeliefRelationshipMapper.toEntity(rel);
                })
                .collect(Collectors.toList());

        List<BeliefRelationshipEntity> savedEntities = relationshipRepository.saveAll(entities);
        totalCreateOperations += savedEntities.size();
        
        return savedEntities.stream()
                .map(BeliefRelationshipMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public String exportKnowledgeGraph(String agentId, String format) {
        BeliefKnowledgeGraph graph = createSnapshotGraph(agentId, false);
        
        switch (format.toLowerCase()) {
            case "json":
                return exportAsJson(graph);
            case "dot":
                return exportAsDot(graph);
            default:
                throw new IllegalArgumentException("Unsupported export format: " + format);
        }
    }

    @Override
    public int importRelationships(String data, String format, String agentId) {
        // Simplified implementation - would need proper parsing based on format
        return 0;
    }

    @Override
    public int cleanupKnowledgeGraph(String agentId, int olderThanDays) {
        return relationshipRepository.deleteOldInactiveRelationships(agentId, olderThanDays);
    }

    @Override
    public Map<String, Object> getHealthInfo() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("implementation", "JpaBeliefRelationshipService");
        health.put("totalCreateOperations", totalCreateOperations);
        health.put("totalUpdateOperations", totalUpdateOperations);
        health.put("totalQueryOperations", totalQueryOperations);
        health.put("uptime", System.currentTimeMillis() - createdAt.toEpochMilli());
        health.put("createdAt", createdAt);
        return health;
    }

    // ========== Private Helper Methods ==========

    private void validateRelationshipCreation(String sourceBeliefId, String targetBeliefId,
                                             RelationshipType relationshipType, double strength, String agentId) {
        if (sourceBeliefId == null || sourceBeliefId.trim().isEmpty()) {
            throw new IllegalArgumentException("Source belief ID cannot be null or empty");
        }
        if (targetBeliefId == null || targetBeliefId.trim().isEmpty()) {
            throw new IllegalArgumentException("Target belief ID cannot be null or empty");
        }
        if (relationshipType == null) {
            throw new IllegalArgumentException("Relationship type cannot be null");
        }
        if (strength < 0.0 || strength > 1.0) {
            throw new IllegalArgumentException("Strength must be between 0.0 and 1.0");
        }
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }

        // Verify beliefs exist
        if (!beliefRepository.existsById(sourceBeliefId)) {
            throw new IllegalArgumentException("Source belief not found: " + sourceBeliefId);
        }
        if (!beliefRepository.existsById(targetBeliefId)) {
            throw new IllegalArgumentException("Target belief not found: " + targetBeliefId);
        }
    }

    private void validateTemporalConstraints(Instant effectiveFrom, Instant effectiveUntil) {
        if (effectiveFrom != null && effectiveUntil != null && effectiveFrom.isAfter(effectiveUntil)) {
            throw new IllegalArgumentException("Effective from must be before effective until");
        }
    }

    private String generateRelationshipId() {
        return "rel_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String exportAsJson(BeliefKnowledgeGraph graph) {
        // Simplified JSON export - would use proper JSON library in real implementation
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"agentId\":\"").append(graph.getAgentId()).append("\",");
        json.append("\"beliefCount\":").append(graph.getBeliefs().size()).append(",");
        json.append("\"relationshipCount\":").append(graph.getRelationships().size());
        json.append("}");
        return json.toString();
    }

    private String exportAsDot(BeliefKnowledgeGraph graph) {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph KnowledgeGraph {\n");
        dot.append("  rankdir=LR;\n");
        
        // Add beliefs as nodes
        for (Belief belief : graph.getBeliefs().values()) {
            dot.append("  \"").append(belief.getId()).append("\" [label=\"")
               .append(belief.getStatement().replace("\"", "\\\"")).append("\"];\n");
        }
        
        // Add relationships as edges
        for (BeliefRelationship rel : graph.getRelationships().values()) {
            dot.append("  \"").append(rel.getSourceBeliefId()).append("\" -> \"")
               .append(rel.getTargetBeliefId()).append("\" [label=\"")
               .append(rel.getRelationshipType()).append(" (").append(rel.getStrength())
               .append(")\"];\n");
        }
        
        dot.append("}\n");
        return dot.toString();
    }
}
