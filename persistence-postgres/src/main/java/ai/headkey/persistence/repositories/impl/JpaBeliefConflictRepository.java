package ai.headkey.persistence.repositories.impl;

import ai.headkey.persistence.entities.BeliefConflictEntity;
import ai.headkey.persistence.repositories.BeliefConflictRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;

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
public class JpaBeliefConflictRepository implements BeliefConflictRepository {

    private final EntityManagerFactory entityManagerFactory;

    /**
     * Constructor with EntityManagerFactory dependency.
     */
    public JpaBeliefConflictRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Checks if an entity exists by ID within a transaction context.
     * Used by batch operations that already have an EntityManager.
     * 
     * @param id The ID to check
     * @param em The EntityManager to use
     * @return true if entity exists, false otherwise
     */
    private boolean existsByIdInTransaction(String id, EntityManager em) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }

        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(c) FROM BeliefConflictEntity c WHERE c.id = :id",
                Long.class
            );
            query.setParameter("id", id);
            return query.getSingleResult() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ========== Basic CRUD Operations ==========

    @Override
    public BeliefConflictEntity save(BeliefConflictEntity conflict) {
        if (conflict == null) {
            throw new IllegalArgumentException("Conflict entity cannot be null");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            
            BeliefConflictEntity result;
            if (conflict.getId() != null && existsById(conflict.getId())) {
                result = em.merge(conflict);
            } else {
                em.persist(conflict);
                result = conflict;
            }
            
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save conflict entity: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<BeliefConflictEntity> saveAll(List<BeliefConflictEntity> conflicts) {
        if (conflicts == null) {
            throw new IllegalArgumentException("Conflicts list cannot be null");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        List<BeliefConflictEntity> savedEntities = new ArrayList<>();
        
        try {
            em.getTransaction().begin();
            
            for (int i = 0; i < conflicts.size(); i++) {
                BeliefConflictEntity conflict = conflicts.get(i);
                
                BeliefConflictEntity saved;
                if (conflict.getId() != null && existsByIdInTransaction(conflict.getId(), em)) {
                    saved = em.merge(conflict);
                } else {
                    em.persist(conflict);
                    saved = conflict;
                }
                savedEntities.add(saved);
                
                // Flush periodically for large batches
                if (i % 50 == 0) {
                    em.flush();
                    em.clear();
                }
            }
            
            em.getTransaction().commit();
            return savedEntities;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to save conflict entities: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public Optional<BeliefConflictEntity> findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            BeliefConflictEntity entity = em.find(BeliefConflictEntity.class, id);
            return Optional.ofNullable(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find conflict by ID: " + e.getMessage(), e);
        } finally {
            em.close();
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

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<BeliefConflictEntity> query = em.createQuery(
                "SELECT c FROM BeliefConflictEntity c WHERE c.id IN :ids ORDER BY c.detectedAt DESC",
                BeliefConflictEntity.class
            );
            query.setParameter("ids", ids);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find conflicts by IDs: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            
            BeliefConflictEntity entity = em.find(BeliefConflictEntity.class, id);
            if (entity != null) {
                em.remove(entity);
                em.getTransaction().commit();
                return true;
            }
            em.getTransaction().commit();
            return false;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete conflict by ID: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public void delete(BeliefConflictEntity conflict) {
        if (conflict == null) {
            throw new IllegalArgumentException("Conflict entity cannot be null");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            
            if (em.contains(conflict)) {
                em.remove(conflict);
            } else {
                BeliefConflictEntity managed = em.find(BeliefConflictEntity.class, conflict.getId());
                if (managed != null) {
                    em.remove(managed);
                }
            }
            
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete conflict entity: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public boolean existsById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(c) FROM BeliefConflictEntity c WHERE c.id = :id",
                Long.class
            );
            query.setParameter("id", id);
            return query.getSingleResult() > 0;
        } catch (Exception e) {
            return false;
        } finally {
            em.close();
        }
    }

    // ========== Query Operations ==========

    @Override
    public List<BeliefConflictEntity> findUnresolved(String agentId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<BeliefConflictEntity> query = em.createNamedQuery(
                "BeliefConflictEntity.findUnresolved", BeliefConflictEntity.class);
            query.setParameter("agentId", agentId);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find unresolved conflicts: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<BeliefConflictEntity> findByAgent(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<BeliefConflictEntity> query = em.createNamedQuery(
                "BeliefConflictEntity.findByAgent", BeliefConflictEntity.class);
            query.setParameter("agentId", agentId);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find conflicts by agent: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<BeliefConflictEntity> findByResolutionStrategy(String strategy) {
        if (strategy == null || strategy.trim().isEmpty()) {
            throw new IllegalArgumentException("Strategy cannot be null or empty");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<BeliefConflictEntity> query = em.createNamedQuery(
                "BeliefConflictEntity.findByResolutionStrategy", BeliefConflictEntity.class);
            query.setParameter("strategy", strategy);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find conflicts by resolution strategy: " + e.getMessage(), e);
        } finally {
            em.close();
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

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            // This is a more complex query since conflicting belief IDs are stored as a collection
            TypedQuery<BeliefConflictEntity> query = em.createQuery(
                "SELECT DISTINCT c FROM BeliefConflictEntity c JOIN c.conflictingBeliefIds beliefId WHERE beliefId IN :beliefIds ORDER BY c.detectedAt DESC",
                BeliefConflictEntity.class
            );
            query.setParameter("beliefIds", beliefIds);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find conflicts by belief IDs: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<BeliefConflictEntity> findByType(String conflictType, String agentId) {
        if (conflictType == null || conflictType.trim().isEmpty()) {
            throw new IllegalArgumentException("Conflict type cannot be null or empty");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            String jpql = "SELECT c FROM BeliefConflictEntity c WHERE c.conflictType = :conflictType" +
                         (agentId != null ? " AND c.agentId = :agentId" : "") +
                         " ORDER BY c.detectedAt DESC";
            
            TypedQuery<BeliefConflictEntity> query = em.createQuery(jpql, BeliefConflictEntity.class);
            query.setParameter("conflictType", conflictType);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find conflicts by type: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<BeliefConflictEntity> findBySeverity(String severity, String agentId) {
        if (severity == null || severity.trim().isEmpty()) {
            throw new IllegalArgumentException("Severity cannot be null or empty");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            String jpql = "SELECT c FROM BeliefConflictEntity c WHERE c.severity = :severity" +
                         (agentId != null ? " AND c.agentId = :agentId" : "") +
                         " ORDER BY c.detectedAt DESC";
            
            TypedQuery<BeliefConflictEntity> query = em.createQuery(jpql, BeliefConflictEntity.class);
            query.setParameter("severity", severity);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find conflicts by severity: " + e.getMessage(), e);
        } finally {
            em.close();
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

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            String jpql = "SELECT c FROM BeliefConflictEntity c WHERE c.detectedAt BETWEEN :startTime AND :endTime" +
                         (agentId != null ? " AND c.agentId = :agentId" : "") +
                         " ORDER BY c.detectedAt DESC";
            
            TypedQuery<BeliefConflictEntity> query = em.createQuery(jpql, BeliefConflictEntity.class);
            query.setParameter("startTime", startTime);
            query.setParameter("endTime", endTime);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find conflicts by detection time range: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<BeliefConflictEntity> findAutoResolvable(String agentId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            String jpql = "SELECT c FROM BeliefConflictEntity c WHERE c.autoResolvable = true AND c.resolved = false" +
                         (agentId != null ? " AND c.agentId = :agentId" : "") +
                         " ORDER BY c.detectedAt DESC";
            
            TypedQuery<BeliefConflictEntity> query = em.createQuery(jpql, BeliefConflictEntity.class);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find auto-resolvable conflicts: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<BeliefConflictEntity> findRequiringManualReview(String agentId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            String jpql = "SELECT c FROM BeliefConflictEntity c WHERE c.autoResolvable = false AND c.resolved = false" +
                         (agentId != null ? " AND c.agentId = :agentId" : "") +
                         " ORDER BY c.detectedAt DESC";
            
            TypedQuery<BeliefConflictEntity> query = em.createQuery(jpql, BeliefConflictEntity.class);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find conflicts requiring manual review: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // ========== Statistics and Analytics ==========

    @Override
    public long countByAgent(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(c) FROM BeliefConflictEntity c WHERE c.agentId = :agentId",
                Long.class
            );
            query.setParameter("agentId", agentId);
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count conflicts by agent: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public long countUnresolved(String agentId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<Long> query = em.createNamedQuery(
                "BeliefConflictEntity.countUnresolved", Long.class);
            query.setParameter("agentId", agentId);
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count unresolved conflicts: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public long count() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(c) FROM BeliefConflictEntity c",
                Long.class
            );
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count all conflicts: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<ConflictTypeDistribution> getConflictDistributionByType(String agentId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            String jpql = "SELECT c.conflictType, COUNT(c) FROM BeliefConflictEntity c " +
                         "WHERE c.conflictType IS NOT NULL " +
                         (agentId != null ? "AND c.agentId = :agentId " : "") +
                         "GROUP BY c.conflictType ORDER BY COUNT(c) DESC";
            
            TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            
            return query.getResultList().stream()
                .map(result -> new ConflictTypeDistribution((String) result[0], (Long) result[1]))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get conflict distribution by type: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<ConflictSeverityDistribution> getConflictDistributionBySeverity(String agentId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            String jpql = "SELECT c.severity, COUNT(c) FROM BeliefConflictEntity c " +
                         "WHERE c.severity IS NOT NULL " +
                         (agentId != null ? "AND c.agentId = :agentId " : "") +
                         "GROUP BY c.severity ORDER BY COUNT(c) DESC";
            
            TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            
            return query.getResultList().stream()
                .map(result -> new ConflictSeverityDistribution((String) result[0], (Long) result[1]))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get conflict distribution by severity: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public List<ConflictResolutionDistribution> getConflictDistributionByResolution(String agentId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            String jpql = "SELECT " +
                         "CASE WHEN c.resolved = true THEN 'resolved' ELSE 'unresolved' END, " +
                         "COUNT(c) FROM BeliefConflictEntity c " +
                         (agentId != null ? "WHERE c.agentId = :agentId " : "") +
                         "GROUP BY c.resolved ORDER BY COUNT(c) DESC";
            
            TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            
            return query.getResultList().stream()
                .map(result -> new ConflictResolutionDistribution((String) result[0], (Long) result[1]))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get conflict distribution by resolution: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public double getAverageResolutionTime(String agentId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            String jpql = "SELECT AVG(EXTRACT(EPOCH FROM (c.resolvedAt - c.detectedAt))) " +
                         "FROM BeliefConflictEntity c " +
                         "WHERE c.resolved = true AND c.resolvedAt IS NOT NULL AND c.detectedAt IS NOT NULL " +
                         (agentId != null ? "AND c.agentId = :agentId" : "");
            
            TypedQuery<Double> query = em.createQuery(jpql, Double.class);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            
            Double result = query.getSingleResult();
            return result != null ? result : 0.0;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get average resolution time: " + e.getMessage(), e);
        } finally {
            em.close();
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

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            
            int updated = 0;
            for (int i = 0; i < conflicts.size(); i++) {
                em.merge(conflicts.get(i));
                updated++;
                
                // Flush periodically for large batches
                if (i % 50 == 0) {
                    em.flush();
                    em.clear();
                }
            }
            
            em.getTransaction().commit();
            return updated;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to update conflicts in batch: " + e.getMessage(), e);
        } finally {
            em.close();
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

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            
            String jpql = "UPDATE BeliefConflictEntity c SET " +
                         "c.resolved = true, " +
                         "c.resolvedAt = CURRENT_TIMESTAMP, " +
                         "c.resolutionStrategy = :strategy, " +
                         "c.resolutionNotes = :notes, " +
                         "c.lastUpdated = CURRENT_TIMESTAMP " +
                         "WHERE c.id IN :ids";
            
            TypedQuery<Integer> query = em.createQuery(jpql, Integer.class);
            query.setParameter("strategy", resolutionStrategy);
            query.setParameter("notes", resolutionNotes);
            query.setParameter("ids", conflictIds);
            int result = query.executeUpdate();
            
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to mark conflicts as resolved in batch: " + e.getMessage(), e);
        } finally {
            em.close();
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

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            
            TypedQuery<Integer> query = em.createQuery(
                "DELETE FROM BeliefConflictEntity c WHERE c.id IN :ids",
                Integer.class
            );
            query.setParameter("ids", conflictIds);
            int result = query.executeUpdate();
            
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete conflicts in batch: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    @Override
    public int deleteOldResolvedConflicts(Instant olderThan) {
        if (olderThan == null) {
            throw new IllegalArgumentException("Older than timestamp cannot be null");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            
            TypedQuery<Integer> query = em.createQuery(
                "DELETE FROM BeliefConflictEntity c WHERE c.resolved = true AND c.resolvedAt < :olderThan",
                Integer.class
            );
            query.setParameter("olderThan", olderThan);
            int result = query.executeUpdate();
            
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Failed to delete old resolved conflicts: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    // ========== Maintenance Operations ==========

    @Override
    public void flush() {
        // Note: flush() doesn't make sense with EntityManagerFactory pattern
        // since each operation uses its own EntityManager
        // This method is kept for interface compatibility but is essentially a no-op
    }

    @Override
    public void clear() {
        // Note: clear() doesn't make sense with EntityManagerFactory pattern
        // since each operation uses its own EntityManager
        // This method is kept for interface compatibility but is essentially a no-op
    }

    @Override
    public void detach(BeliefConflictEntity conflict) {
        // Note: detach() doesn't make sense with EntityManagerFactory pattern
        // since each operation uses its own EntityManager
        // This method is kept for interface compatibility but is essentially a no-op
    }

    @Override
    public void refresh(BeliefConflictEntity conflict) {
        if (conflict == null) {
            throw new IllegalArgumentException("Conflict entity cannot be null");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            BeliefConflictEntity managedEntity = em.find(BeliefConflictEntity.class, conflict.getId());
            if (managedEntity != null) {
                em.refresh(managedEntity);
                // Copy refreshed values back to the parameter
                conflict.setResolved(managedEntity.isResolved());
                conflict.setResolvedAt(managedEntity.getResolvedAt());
                conflict.setResolutionStrategy(managedEntity.getResolutionStrategy());
                conflict.setResolutionNotes(managedEntity.getResolutionNotes());
                conflict.setLastUpdated(managedEntity.getLastUpdated());
                // Copy other fields as needed
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh conflict entity: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }
}