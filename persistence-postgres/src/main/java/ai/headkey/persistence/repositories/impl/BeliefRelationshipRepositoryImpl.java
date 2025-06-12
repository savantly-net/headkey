package ai.headkey.persistence.repositories.impl;

import ai.headkey.memory.enums.RelationshipType;
import ai.headkey.persistence.entities.BeliefRelationshipEntity;
import ai.headkey.persistence.repositories.BeliefRelationshipRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * JPA implementation of BeliefRelationshipRepository.
 * 
 * This implementation provides data access operations for BeliefRelationshipEntity
 * using JPA EntityManager. It includes optimized queries for graph operations,
 * batch processing capabilities, and comprehensive relationship management.
 * 
 * @since 1.0
 */
public class BeliefRelationshipRepositoryImpl implements BeliefRelationshipRepository {

    private final EntityManager entityManager;

    public BeliefRelationshipRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // ========== Basic CRUD Operations ==========

    @Override
    public BeliefRelationshipEntity save(BeliefRelationshipEntity relationship) {
        if (relationship.getId() == null || !existsById(relationship.getId())) {
            entityManager.persist(relationship);
            return relationship;
        } else {
            return entityManager.merge(relationship);
        }
    }

    @Override
    public List<BeliefRelationshipEntity> saveAll(List<BeliefRelationshipEntity> relationships) {
        for (BeliefRelationshipEntity relationship : relationships) {
            save(relationship);
        }
        entityManager.flush();
        return relationships;
    }

    @Override
    public Optional<BeliefRelationshipEntity> findById(String id) {
        BeliefRelationshipEntity relationship = entityManager.find(BeliefRelationshipEntity.class, id);
        return Optional.ofNullable(relationship);
    }

    @Override
    public List<BeliefRelationshipEntity> findByIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        
        TypedQuery<BeliefRelationshipEntity> query = entityManager.createQuery(
            "SELECT r FROM BeliefRelationshipEntity r WHERE r.id IN :ids", 
            BeliefRelationshipEntity.class
        );
        query.setParameter("ids", ids);
        return query.getResultList();
    }

    @Override
    public boolean deleteById(String id) {
        BeliefRelationshipEntity relationship = entityManager.find(BeliefRelationshipEntity.class, id);
        if (relationship != null) {
            entityManager.remove(relationship);
            return true;
        }
        return false;
    }

    @Override
    public boolean existsById(String id) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(r) FROM BeliefRelationshipEntity r WHERE r.id = :id", 
            Long.class
        );
        query.setParameter("id", id);
        return query.getSingleResult() > 0;
    }

    // ========== Query Operations ==========

    @Override
    public List<BeliefRelationshipEntity> findByAgent(String agentId, boolean includeInactive) {
        TypedQuery<BeliefRelationshipEntity> query = entityManager.createNamedQuery(
            "BeliefRelationshipEntity.findByAgent", 
            BeliefRelationshipEntity.class
        );
        query.setParameter("agentId", agentId);
        query.setParameter("includeInactive", includeInactive);
        return query.getResultList();
    }

    @Override
    public List<BeliefRelationshipEntity> findBySourceBelief(String sourceBeliefId, String agentId, boolean includeInactive) {
        TypedQuery<BeliefRelationshipEntity> query = entityManager.createNamedQuery(
            "BeliefRelationshipEntity.findBySourceBelief", 
            BeliefRelationshipEntity.class
        );
        query.setParameter("sourceBeliefId", sourceBeliefId);
        query.setParameter("agentId", agentId);
        query.setParameter("includeInactive", includeInactive);
        return query.getResultList();
    }

    @Override
    public List<BeliefRelationshipEntity> findByTargetBelief(String targetBeliefId, String agentId, boolean includeInactive) {
        TypedQuery<BeliefRelationshipEntity> query = entityManager.createNamedQuery(
            "BeliefRelationshipEntity.findByTargetBelief", 
            BeliefRelationshipEntity.class
        );
        query.setParameter("targetBeliefId", targetBeliefId);
        query.setParameter("agentId", agentId);
        query.setParameter("includeInactive", includeInactive);
        return query.getResultList();
    }

    @Override
    public List<BeliefRelationshipEntity> findByBelief(String beliefId, String agentId, boolean includeInactive) {
        TypedQuery<BeliefRelationshipEntity> query = entityManager.createNamedQuery(
            "BeliefRelationshipEntity.findByBelief", 
            BeliefRelationshipEntity.class
        );
        query.setParameter("beliefId", beliefId);
        query.setParameter("agentId", agentId);
        query.setParameter("includeInactive", includeInactive);
        return query.getResultList();
    }

    @Override
    public List<BeliefRelationshipEntity> findByType(RelationshipType relationshipType, String agentId, boolean includeInactive) {
        TypedQuery<BeliefRelationshipEntity> query = entityManager.createNamedQuery(
            "BeliefRelationshipEntity.findByType", 
            BeliefRelationshipEntity.class
        );
        query.setParameter("relationshipType", relationshipType);
        query.setParameter("agentId", agentId);
        query.setParameter("includeInactive", includeInactive);
        return query.getResultList();
    }

    @Override
    public List<BeliefRelationshipEntity> findBetweenBeliefs(String sourceBeliefId, String targetBeliefId, String agentId, boolean includeInactive) {
        TypedQuery<BeliefRelationshipEntity> query = entityManager.createNamedQuery(
            "BeliefRelationshipEntity.findBetweenBeliefs", 
            BeliefRelationshipEntity.class
        );
        query.setParameter("sourceBeliefId", sourceBeliefId);
        query.setParameter("targetBeliefId", targetBeliefId);
        query.setParameter("agentId", agentId);
        query.setParameter("includeInactive", includeInactive);
        return query.getResultList();
    }

    @Override
    public List<BeliefRelationshipEntity> findDeprecating(String agentId) {
        TypedQuery<BeliefRelationshipEntity> query = entityManager.createNamedQuery(
            "BeliefRelationshipEntity.findDeprecating", 
            BeliefRelationshipEntity.class
        );
        query.setParameter("agentId", agentId);
        return query.getResultList();
    }

    @Override
    public List<BeliefRelationshipEntity> findCurrentlyEffective(String agentId) {
        TypedQuery<BeliefRelationshipEntity> query = entityManager.createNamedQuery(
            "BeliefRelationshipEntity.findCurrentlyEffective", 
            BeliefRelationshipEntity.class
        );
        query.setParameter("agentId", agentId);
        return query.getResultList();
    }

    @Override
    public List<BeliefRelationshipEntity> findByHighStrength(double threshold, String agentId) {
        TypedQuery<BeliefRelationshipEntity> query = entityManager.createNamedQuery(
            "BeliefRelationshipEntity.findHighStrength", 
            BeliefRelationshipEntity.class
        );
        query.setParameter("threshold", threshold);
        query.setParameter("agentId", agentId);
        return query.getResultList();
    }

    @Override
    public List<BeliefRelationshipEntity> findEffectiveAt(Instant timestamp, String agentId) {
        TypedQuery<BeliefRelationshipEntity> query = entityManager.createQuery(
            "SELECT r FROM BeliefRelationshipEntity r WHERE " +
            "r.active = true AND (:agentId IS NULL OR r.agentId = :agentId) AND " +
            "(r.effectiveFrom IS NULL OR r.effectiveFrom <= :timestamp) AND " +
            "(r.effectiveUntil IS NULL OR r.effectiveUntil > :timestamp) " +
            "ORDER BY r.strength DESC", 
            BeliefRelationshipEntity.class
        );
        query.setParameter("timestamp", timestamp);
        query.setParameter("agentId", agentId);
        return query.getResultList();
    }

    @Override
    public List<BeliefRelationshipEntity> findExpiredBefore(Instant timestamp, String agentId) {
        TypedQuery<BeliefRelationshipEntity> query = entityManager.createQuery(
            "SELECT r FROM BeliefRelationshipEntity r WHERE " +
            "r.effectiveUntil IS NOT NULL AND r.effectiveUntil < :timestamp AND " +
            "(:agentId IS NULL OR r.agentId = :agentId) " +
            "ORDER BY r.effectiveUntil DESC", 
            BeliefRelationshipEntity.class
        );
        query.setParameter("timestamp", timestamp);
        query.setParameter("agentId", agentId);
        return query.getResultList();
    }

    // ========== Batch Operations ==========

    @Override
    public int updateStrengthBatch(Set<String> relationshipIds, double newStrength) {
        if (relationshipIds == null || relationshipIds.isEmpty()) {
            return 0;
        }
        
        Query query = entityManager.createQuery(
            "UPDATE BeliefRelationshipEntity r SET r.strength = :newStrength, r.lastUpdated = :now " +
            "WHERE r.id IN :ids"
        );
        query.setParameter("newStrength", newStrength);
        query.setParameter("now", Instant.now());
        query.setParameter("ids", relationshipIds);
        return query.executeUpdate();
    }

    @Override
    public int deactivateBatch(Set<String> relationshipIds) {
        if (relationshipIds == null || relationshipIds.isEmpty()) {
            return 0;
        }
        
        Query query = entityManager.createQuery(
            "UPDATE BeliefRelationshipEntity r SET r.active = false, r.lastUpdated = :now " +
            "WHERE r.id IN :ids"
        );
        query.setParameter("now", Instant.now());
        query.setParameter("ids", relationshipIds);
        return query.executeUpdate();
    }

    @Override
    public int reactivateBatch(Set<String> relationshipIds) {
        if (relationshipIds == null || relationshipIds.isEmpty()) {
            return 0;
        }
        
        Query query = entityManager.createQuery(
            "UPDATE BeliefRelationshipEntity r SET r.active = true, r.lastUpdated = :now " +
            "WHERE r.id IN :ids"
        );
        query.setParameter("now", Instant.now());
        query.setParameter("ids", relationshipIds);
        return query.executeUpdate();
    }

    @Override
    public int deleteBatch(Set<String> relationshipIds) {
        if (relationshipIds == null || relationshipIds.isEmpty()) {
            return 0;
        }
        
        Query query = entityManager.createQuery(
            "DELETE FROM BeliefRelationshipEntity r WHERE r.id IN :ids"
        );
        query.setParameter("ids", relationshipIds);
        return query.executeUpdate();
    }

    @Override
    public int deleteOldInactiveRelationships(String agentId, int olderThanDays) {
        Instant cutoffTime = Instant.now().minusSeconds(olderThanDays * 24 * 60 * 60L);
        
        Query query = entityManager.createQuery(
            "DELETE FROM BeliefRelationshipEntity r WHERE " +
            "r.agentId = :agentId AND r.active = false AND r.lastUpdated < :cutoffTime"
        );
        query.setParameter("agentId", agentId);
        query.setParameter("cutoffTime", cutoffTime);
        return query.executeUpdate();
    }

    // ========== Count Operations ==========

    @Override
    public long countByAgent(String agentId, boolean includeInactive) {
        TypedQuery<Long> query = entityManager.createNamedQuery(
            "BeliefRelationshipEntity.countByAgent", 
            Long.class
        );
        query.setParameter("agentId", agentId);
        query.setParameter("includeInactive", includeInactive);
        return query.getSingleResult();
    }

    @Override
    public long countByAgentAndType(String agentId, RelationshipType relationshipType, boolean includeInactive) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(r) FROM BeliefRelationshipEntity r WHERE " +
            "r.agentId = :agentId AND r.relationshipType = :relationshipType AND " +
            "(:includeInactive = true OR r.active = true)", 
            Long.class
        );
        query.setParameter("agentId", agentId);
        query.setParameter("relationshipType", relationshipType);
        query.setParameter("includeInactive", includeInactive);
        return query.getSingleResult();
    }

    @Override
    public long countByBelief(String beliefId, String agentId, boolean includeInactive) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(r) FROM BeliefRelationshipEntity r WHERE " +
            "(r.sourceBeliefId = :beliefId OR r.targetBeliefId = :beliefId) AND " +
            "r.agentId = :agentId AND (:includeInactive = true OR r.active = true)", 
            Long.class
        );
        query.setParameter("beliefId", beliefId);
        query.setParameter("agentId", agentId);
        query.setParameter("includeInactive", includeInactive);
        return query.getSingleResult();
    }

    // ========== Statistics Operations ==========

    @Override
    public List<Object[]> getRelationshipTypeDistribution(String agentId, boolean includeInactive) {
        TypedQuery<Object[]> query = entityManager.createQuery(
            "SELECT r.relationshipType, COUNT(r) FROM BeliefRelationshipEntity r WHERE " +
            "r.agentId = :agentId AND (:includeInactive = true OR r.active = true) " +
            "GROUP BY r.relationshipType ORDER BY COUNT(r) DESC", 
            Object[].class
        );
        query.setParameter("agentId", agentId);
        query.setParameter("includeInactive", includeInactive);
        return query.getResultList();
    }

    @Override
    public List<Object[]> getAverageStrengthByType(String agentId) {
        TypedQuery<Object[]> query = entityManager.createQuery(
            "SELECT r.relationshipType, AVG(r.strength) FROM BeliefRelationshipEntity r WHERE " +
            "r.agentId = :agentId AND r.active = true " +
            "GROUP BY r.relationshipType ORDER BY AVG(r.strength) DESC", 
            Object[].class
        );
        query.setParameter("agentId", agentId);
        return query.getResultList();
    }

    @Override
    public Object[] getRelationshipStatistics(String agentId) {
        TypedQuery<Object[]> query = entityManager.createQuery(
            "SELECT COUNT(r), SUM(CASE WHEN r.active = true THEN 1 ELSE 0 END), AVG(CASE WHEN r.active = true THEN r.strength ELSE NULL END) " +
            "FROM BeliefRelationshipEntity r WHERE r.agentId = :agentId", 
            Object[].class
        );
        query.setParameter("agentId", agentId);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return new Object[]{0L, 0L, 0.0};
        }
    }

    // ========== Graph Operations ==========

    @Override
    public List<String> findConnectedBeliefs(String beliefId, String agentId, int maxDepth, boolean includeInactive) {
        // For simplicity, implementing a basic 1-level connection
        // A more sophisticated implementation would use recursive CTEs or multiple queries
        TypedQuery<String> query = entityManager.createQuery(
            "SELECT DISTINCT CASE " +
            "WHEN r.sourceBeliefId = :beliefId THEN r.targetBeliefId " +
            "ELSE r.sourceBeliefId END " +
            "FROM BeliefRelationshipEntity r WHERE " +
            "(r.sourceBeliefId = :beliefId OR r.targetBeliefId = :beliefId) AND " +
            "r.agentId = :agentId AND (:includeInactive = true OR r.active = true)", 
            String.class
        );
        query.setParameter("beliefId", beliefId);
        query.setParameter("agentId", agentId);
        query.setParameter("includeInactive", includeInactive);
        return query.getResultList();
    }

    @Override
    public List<List<String>> findStronglyConnectedClusters(String agentId, double strengthThreshold) {
        // This is a simplified implementation
        // A full implementation would use graph algorithms like Tarjan's or Kosaraju's
        TypedQuery<String> query = entityManager.createQuery(
            "SELECT DISTINCT r.sourceBeliefId FROM BeliefRelationshipEntity r WHERE " +
            "r.agentId = :agentId AND r.active = true AND r.strength >= :threshold", 
            String.class
        );
        query.setParameter("agentId", agentId);
        query.setParameter("threshold", strengthThreshold);
        List<String> beliefs = query.getResultList();
        
        // For now, return each belief as its own cluster
        return beliefs.stream().map(belief -> List.of(belief)).toList();
    }

    @Override
    public List<BeliefRelationshipEntity> findShortestPath(String sourceBeliefId, String targetBeliefId, String agentId) {
        // Simplified implementation - direct path only
        TypedQuery<BeliefRelationshipEntity> query = entityManager.createQuery(
            "SELECT r FROM BeliefRelationshipEntity r WHERE " +
            "r.sourceBeliefId = :sourceBeliefId AND r.targetBeliefId = :targetBeliefId AND " +
            "r.agentId = :agentId AND r.active = true " +
            "ORDER BY r.strength DESC", 
            BeliefRelationshipEntity.class
        );
        query.setParameter("sourceBeliefId", sourceBeliefId);
        query.setParameter("targetBeliefId", targetBeliefId);
        query.setParameter("agentId", agentId);
        query.setMaxResults(1);
        return query.getResultList();
    }

    @Override
    public List<BeliefRelationshipEntity> findOrphanedRelationships(String agentId) {
        TypedQuery<BeliefRelationshipEntity> query = entityManager.createQuery(
            "SELECT r FROM BeliefRelationshipEntity r WHERE " +
            "r.agentId = :agentId AND (" +
            "r.sourceBeliefId NOT IN (SELECT b.id FROM BeliefEntity b WHERE b.agentId = :agentId) OR " +
            "r.targetBeliefId NOT IN (SELECT b.id FROM BeliefEntity b WHERE b.agentId = :agentId))", 
            BeliefRelationshipEntity.class
        );
        query.setParameter("agentId", agentId);
        return query.getResultList();
    }

    @Override
    public List<BeliefRelationshipEntity> findSelfReferencingRelationships(String agentId) {
        TypedQuery<BeliefRelationshipEntity> query = entityManager.createQuery(
            "SELECT r FROM BeliefRelationshipEntity r WHERE " +
            "r.agentId = :agentId AND r.sourceBeliefId = r.targetBeliefId", 
            BeliefRelationshipEntity.class
        );
        query.setParameter("agentId", agentId);
        return query.getResultList();
    }

    @Override
    public List<BeliefRelationshipEntity> findTemporallyInvalidRelationships(String agentId) {
        TypedQuery<BeliefRelationshipEntity> query = entityManager.createQuery(
            "SELECT r FROM BeliefRelationshipEntity r WHERE " +
            "r.agentId = :agentId AND r.effectiveFrom IS NOT NULL AND r.effectiveUntil IS NOT NULL AND " +
            "r.effectiveFrom >= r.effectiveUntil", 
            BeliefRelationshipEntity.class
        );
        query.setParameter("agentId", agentId);
        return query.getResultList();
    }
}