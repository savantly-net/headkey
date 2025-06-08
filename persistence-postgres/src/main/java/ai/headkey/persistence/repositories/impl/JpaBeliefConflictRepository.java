package ai.headkey.persistence.repositories.impl;

import ai.headkey.persistence.entities.BeliefConflictEntity;
import ai.headkey.persistence.repositories.BeliefConflictRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA implementation of BeliefConflictRepository.
 * 
 * This implementation provides data access operations for BeliefConflictEntity objects
 * using JPA EntityManager. It includes optimized queries, batch operations,
 * and comprehensive conflict management capabilities for PostgreSQL backend.
 * 
 * Key features:
 * - Named queries for performance optimization
 * - Batch operations for bulk updates
 * - Conflict lifecycle management
 * - Resolution tracking and analytics
 * - Transaction management integration
 * 
 * @since 1.0
 */
@Transactional
public class JpaBeliefConflictRepository implements BeliefConflictRepository {

    private final EntityManager entityManager;

    /**
     * Constructor with EntityManager dependency.
     */
    public JpaBeliefConflictRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // ========== Basic CRUD Operations ==========

    @Override
    public BeliefConflictEntity save(BeliefConflictEntity conflict) {
        if (conflict == null) {
            throw new IllegalArgumentException("Conflict entity cannot be null");
        }

        try {
            if (conflict.getId() != null && existsById(conflict.getId())) {
                return entityManager.merge(conflict);
            } else {
                entityManager.persist(conflict);
                return conflict;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save conflict entity: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BeliefConflictEntity> saveAll(List<BeliefConflictEntity> conflicts) {
        if (conflicts == null) {
            throw new IllegalArgumentException("Conflicts list cannot be null");
        }

        List<BeliefConflictEntity> savedEntities = new ArrayList<>();
        
        try {
            for (int i = 0; i < conflicts.size(); i++) {
                BeliefConflictEntity saved = save(conflicts.get(i));
                savedEntities.add(saved);
                
                // Flush periodically for large batches
                if (i % 50 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
            
            entityManager.flush();
            return savedEntities;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save conflict entities: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<BeliefConflictEntity> findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }

        try {
            BeliefConflictEntity entity = entityManager.find(BeliefConflictEntity.class, id);
            return Optional.ofNullable(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find conflict by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BeliefConflictEntity> findByIds(Set<String> ids) {
        if (ids == null) {
            throw new IllegalArgumentException("IDs set cannot be null");
        }

        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            TypedQuery<BeliefConflictEntity> query = entityManager.createQuery(
                "SELECT c FROM BeliefConflictEntity c WHERE c.id IN :ids ORDER BY c.detectedAt DESC",
                BeliefConflictEntity.class
            );
            query.setParameter("ids", ids);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find conflicts by IDs: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }

        try {
            BeliefConflictEntity entity = entityManager.find(BeliefConflictEntity.class, id);
            if (entity != null) {
                entityManager.remove(entity);
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete conflict by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(BeliefConflictEntity conflict) {
        if (conflict == null) {
            throw new IllegalArgumentException("Conflict entity cannot be null");
        }

        try {
            if (entityManager.contains(conflict)) {
                entityManager.remove(conflict);
            } else {
                BeliefConflictEntity managed = entityManager.find(BeliefConflictEntity.class, conflict.getId());
                if (managed != null) {
                    entityManager.remove(managed);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete conflict entity: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }

        try {
            TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(c) FROM BeliefConflictEntity c WHERE c.id = :id",
                Long.class
            );
            query.setParameter("id", id);
            return query.getSingleResult() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ========== Query Operations ==========

    @Override
    public List<BeliefConflictEntity> findUnresolved(String agentId) {
        try {
            TypedQuery<BeliefConflictEntity> query = entityManager.createNamedQuery(
                "BeliefConflictEntity.findUnresolved", BeliefConflictEntity.class);
            query.setParameter("agentId", agentId);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find unresolved conflicts: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BeliefConflictEntity> findByAgent(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }

        try {
            TypedQuery<BeliefConflictEntity> query = entityManager.createNamedQuery(
                "BeliefConflictEntity.findByAgent", BeliefConflictEntity.class);
            query.setParameter("agentId", agentId);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find conflicts by agent: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BeliefConflictEntity> findByResolutionStrategy(String strategy) {
        if (strategy == null || strategy.trim().isEmpty()) {
            throw new IllegalArgumentException("Strategy cannot be null or empty");
        }

        try {
            TypedQuery<BeliefConflictEntity> query = entityManager.createNamedQuery(
                "BeliefConflictEntity.findByResolutionStrategy", BeliefConflictEntity.class);
            query.setParameter("strategy", strategy);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find conflicts by resolution strategy: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BeliefConflictEntity> findByBeliefIds(Set<String> beliefIds) {
        if (beliefIds == null) {
            throw new IllegalArgumentException("Belief IDs set cannot be null");
        }

        if (beliefIds.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // This is a more complex query since conflicting belief IDs are stored as a collection
            TypedQuery<BeliefConflictEntity> query = entityManager.createQuery(
                "SELECT DISTINCT c FROM BeliefConflictEntity c JOIN c.conflictingBeliefIds beliefId WHERE beliefId IN :beliefIds ORDER BY c.detectedAt DESC",
                BeliefConflictEntity.class
            );
            query.setParameter("beliefIds", beliefIds);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find conflicts by belief IDs: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BeliefConflictEntity> findByType(String conflictType, String agentId) {
        if (conflictType == null || conflictType.trim().isEmpty()) {
            throw new IllegalArgumentException("Conflict type cannot be null or empty");
        }

        try {
            String jpql = "SELECT c FROM BeliefConflictEntity c WHERE c.conflictType = :conflictType" +
                         (agentId != null ? " AND c.agentId = :agentId" : "") +
                         " ORDER BY c.detectedAt DESC";
            
            TypedQuery<BeliefConflictEntity> query = entityManager.createQuery(jpql, BeliefConflictEntity.class);
            query.setParameter("conflictType", conflictType);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find conflicts by type: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BeliefConflictEntity> findBySeverity(String severity, String agentId) {
        if (severity == null || severity.trim().isEmpty()) {
            throw new IllegalArgumentException("Severity cannot be null or empty");
        }

        try {
            String jpql = "SELECT c FROM BeliefConflictEntity c WHERE c.severity = :severity" +
                         (agentId != null ? " AND c.agentId = :agentId" : "") +
                         " ORDER BY c.detectedAt DESC";
            
            TypedQuery<BeliefConflictEntity> query = entityManager.createQuery(jpql, BeliefConflictEntity.class);
            query.setParameter("severity", severity);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find conflicts by severity: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BeliefConflictEntity> findByDetectionTimeRange(Instant startTime, Instant endTime, String agentId) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time cannot be after end time");
        }

        try {
            String jpql = "SELECT c FROM BeliefConflictEntity c WHERE c.detectedAt BETWEEN :startTime AND :endTime" +
                         (agentId != null ? " AND c.agentId = :agentId" : "") +
                         " ORDER BY c.detectedAt DESC";
            
            TypedQuery<BeliefConflictEntity> query = entityManager.createQuery(jpql, BeliefConflictEntity.class);
            query.setParameter("startTime", startTime);
            query.setParameter("endTime", endTime);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find conflicts by detection time range: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BeliefConflictEntity> findAutoResolvable(String agentId) {
        try {
            String jpql = "SELECT c FROM BeliefConflictEntity c WHERE c.autoResolvable = true AND c.resolved = false" +
                         (agentId != null ? " AND c.agentId = :agentId" : "") +
                         " ORDER BY c.detectedAt DESC";
            
            TypedQuery<BeliefConflictEntity> query = entityManager.createQuery(jpql, BeliefConflictEntity.class);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find auto-resolvable conflicts: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BeliefConflictEntity> findRequiringManualReview(String agentId) {
        try {
            String jpql = "SELECT c FROM BeliefConflictEntity c WHERE c.autoResolvable = false AND c.resolved = false" +
                         (agentId != null ? " AND c.agentId = :agentId" : "") +
                         " ORDER BY c.detectedAt DESC";
            
            TypedQuery<BeliefConflictEntity> query = entityManager.createQuery(jpql, BeliefConflictEntity.class);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find conflicts requiring manual review: " + e.getMessage(), e);
        }
    }

    // ========== Statistics and Analytics ==========

    @Override
    public long countByAgent(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }

        try {
            TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(c) FROM BeliefConflictEntity c WHERE c.agentId = :agentId",
                Long.class
            );
            query.setParameter("agentId", agentId);
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count conflicts by agent: " + e.getMessage(), e);
        }
    }

    @Override
    public long countUnresolved(String agentId) {
        try {
            TypedQuery<Long> query = entityManager.createNamedQuery(
                "BeliefConflictEntity.countUnresolved", Long.class);
            query.setParameter("agentId", agentId);
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count unresolved conflicts: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        try {
            TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(c) FROM BeliefConflictEntity c",
                Long.class
            );
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count all conflicts: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ConflictTypeDistribution> getConflictDistributionByType(String agentId) {
        try {
            String jpql = "SELECT c.conflictType, COUNT(c) FROM BeliefConflictEntity c " +
                         "WHERE c.conflictType IS NOT NULL " +
                         (agentId != null ? "AND c.agentId = :agentId " : "") +
                         "GROUP BY c.conflictType ORDER BY COUNT(c) DESC";
            
            TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            
            return query.getResultList().stream()
                .map(result -> new ConflictTypeDistribution((String) result[0], (Long) result[1]))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get conflict distribution by type: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ConflictSeverityDistribution> getConflictDistributionBySeverity(String agentId) {
        try {
            String jpql = "SELECT c.severity, COUNT(c) FROM BeliefConflictEntity c " +
                         "WHERE c.severity IS NOT NULL " +
                         (agentId != null ? "AND c.agentId = :agentId " : "") +
                         "GROUP BY c.severity ORDER BY COUNT(c) DESC";
            
            TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            
            return query.getResultList().stream()
                .map(result -> new ConflictSeverityDistribution((String) result[0], (Long) result[1]))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get conflict distribution by severity: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ConflictResolutionDistribution> getConflictDistributionByResolution(String agentId) {
        try {
            String jpql = "SELECT " +
                         "CASE WHEN c.resolved = true THEN 'resolved' ELSE 'unresolved' END, " +
                         "COUNT(c) FROM BeliefConflictEntity c " +
                         (agentId != null ? "WHERE c.agentId = :agentId " : "") +
                         "GROUP BY c.resolved ORDER BY COUNT(c) DESC";
            
            TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            
            return query.getResultList().stream()
                .map(result -> new ConflictResolutionDistribution((String) result[0], (Long) result[1]))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get conflict distribution by resolution: " + e.getMessage(), e);
        }
    }

    @Override
    public double getAverageResolutionTime(String agentId) {
        try {
            String jpql = "SELECT AVG(EXTRACT(EPOCH FROM (c.resolvedAt - c.detectedAt))) " +
                         "FROM BeliefConflictEntity c " +
                         "WHERE c.resolved = true AND c.resolvedAt IS NOT NULL AND c.detectedAt IS NOT NULL " +
                         (agentId != null ? "AND c.agentId = :agentId" : "");
            
            TypedQuery<Double> query = entityManager.createQuery(jpql, Double.class);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            
            Double result = query.getSingleResult();
            return result != null ? result : 0.0;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get average resolution time: " + e.getMessage(), e);
        }
    }

    // ========== Batch Operations ==========

    @Override
    public int updateBatch(List<BeliefConflictEntity> conflicts) {
        if (conflicts == null) {
            throw new IllegalArgumentException("Conflicts list cannot be null");
        }

        if (conflicts.isEmpty()) {
            return 0;
        }

        try {
            int updated = 0;
            for (int i = 0; i < conflicts.size(); i++) {
                entityManager.merge(conflicts.get(i));
                updated++;
                
                // Flush periodically for large batches
                if (i % 50 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
            
            entityManager.flush();
            return updated;
        } catch (Exception e) {
            throw new RuntimeException("Failed to update conflicts in batch: " + e.getMessage(), e);
        }
    }

    @Override
    public int markResolvedBatch(List<String> conflictIds, String resolutionStrategy, String resolutionNotes) {
        if (conflictIds == null) {
            throw new IllegalArgumentException("Conflict IDs list cannot be null");
        }

        if (conflictIds.isEmpty()) {
            return 0;
        }

        try {
            String jpql = "UPDATE BeliefConflictEntity c SET " +
                         "c.resolved = true, " +
                         "c.resolvedAt = CURRENT_TIMESTAMP, " +
                         "c.resolutionStrategy = :strategy, " +
                         "c.resolutionNotes = :notes, " +
                         "c.lastUpdated = CURRENT_TIMESTAMP " +
                         "WHERE c.id IN :ids";
            
            TypedQuery<Integer> query = entityManager.createQuery(jpql, Integer.class);
            query.setParameter("strategy", resolutionStrategy);
            query.setParameter("notes", resolutionNotes);
            query.setParameter("ids", conflictIds);
            return query.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to mark conflicts as resolved in batch: " + e.getMessage(), e);
        }
    }

    @Override
    public int deleteBatch(List<String> conflictIds) {
        if (conflictIds == null) {
            throw new IllegalArgumentException("Conflict IDs list cannot be null");
        }

        if (conflictIds.isEmpty()) {
            return 0;
        }

        try {
            TypedQuery<Integer> query = entityManager.createQuery(
                "DELETE FROM BeliefConflictEntity c WHERE c.id IN :ids",
                Integer.class
            );
            query.setParameter("ids", conflictIds);
            return query.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete conflicts in batch: " + e.getMessage(), e);
        }
    }

    @Override
    public int deleteOldResolvedConflicts(Instant olderThan) {
        if (olderThan == null) {
            throw new IllegalArgumentException("Older than timestamp cannot be null");
        }

        try {
            TypedQuery<Integer> query = entityManager.createQuery(
                "DELETE FROM BeliefConflictEntity c WHERE c.resolved = true AND c.resolvedAt < :olderThan",
                Integer.class
            );
            query.setParameter("olderThan", olderThan);
            return query.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete old resolved conflicts: " + e.getMessage(), e);
        }
    }

    // ========== Maintenance Operations ==========

    @Override
    public void flush() {
        try {
            entityManager.flush();
        } catch (Exception e) {
            throw new RuntimeException("Failed to flush EntityManager: " + e.getMessage(), e);
        }
    }

    @Override
    public void clear() {
        try {
            entityManager.clear();
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear EntityManager: " + e.getMessage(), e);
        }
    }

    @Override
    public void detach(BeliefConflictEntity conflict) {
        if (conflict == null) {
            throw new IllegalArgumentException("Conflict entity cannot be null");
        }

        try {
            entityManager.detach(conflict);
        } catch (Exception e) {
            throw new RuntimeException("Failed to detach conflict entity: " + e.getMessage(), e);
        }
    }

    @Override
    public void refresh(BeliefConflictEntity conflict) {
        if (conflict == null) {
            throw new IllegalArgumentException("Conflict entity cannot be null");
        }

        try {
            entityManager.refresh(conflict);
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh conflict entity: " + e.getMessage(), e);
        }
    }
}