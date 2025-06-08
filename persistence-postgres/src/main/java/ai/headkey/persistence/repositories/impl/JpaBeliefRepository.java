package ai.headkey.persistence.repositories.impl;

import ai.headkey.persistence.entities.BeliefEntity;
import ai.headkey.persistence.repositories.BeliefRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA implementation of BeliefRepository.
 * 
 * This implementation provides data access operations for BeliefEntity objects
 * using JPA EntityManager. It includes optimized queries, batch operations,
 * and comprehensive search capabilities for PostgreSQL backend.
 * 
 * Key features:
 * - Named queries for performance optimization
 * - Batch operations for bulk updates
 * - Full-text search support
 * - Comprehensive indexing utilization
 * - Transaction management integration
 * 
 * @since 1.0
 */
@Transactional
public class JpaBeliefRepository implements BeliefRepository {

    private final EntityManager entityManager;

    /**
     * Constructor with EntityManager dependency.
     */
    public JpaBeliefRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // ========== Basic CRUD Operations ==========

    @Override
    public BeliefEntity save(BeliefEntity belief) {
        if (belief == null) {
            throw new IllegalArgumentException("Belief entity cannot be null");
        }

        try {
            if (belief.getId() != null && existsById(belief.getId())) {
                return entityManager.merge(belief);
            } else {
                entityManager.persist(belief);
                return belief;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save belief entity: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BeliefEntity> saveAll(List<BeliefEntity> beliefs) {
        if (beliefs == null) {
            throw new IllegalArgumentException("Beliefs list cannot be null");
        }

        List<BeliefEntity> savedEntities = new ArrayList<>();
        
        try {
            for (int i = 0; i < beliefs.size(); i++) {
                BeliefEntity saved = save(beliefs.get(i));
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
            throw new RuntimeException("Failed to save belief entities: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<BeliefEntity> findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }

        try {
            BeliefEntity entity = entityManager.find(BeliefEntity.class, id);
            return Optional.ofNullable(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find belief by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BeliefEntity> findByIds(Set<String> ids) {
        if (ids == null) {
            throw new IllegalArgumentException("IDs set cannot be null");
        }

        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            TypedQuery<BeliefEntity> query = entityManager.createQuery(
                "SELECT b FROM BeliefEntity b WHERE b.id IN :ids ORDER BY b.lastUpdated DESC",
                BeliefEntity.class
            );
            query.setParameter("ids", ids);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find beliefs by IDs: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }

        try {
            BeliefEntity entity = entityManager.find(BeliefEntity.class, id);
            if (entity != null) {
                entityManager.remove(entity);
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete belief by ID: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(BeliefEntity belief) {
        if (belief == null) {
            throw new IllegalArgumentException("Belief entity cannot be null");
        }

        try {
            if (entityManager.contains(belief)) {
                entityManager.remove(belief);
            } else {
                BeliefEntity managed = entityManager.find(BeliefEntity.class, belief.getId());
                if (managed != null) {
                    entityManager.remove(managed);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete belief entity: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }

        try {
            TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(b) FROM BeliefEntity b WHERE b.id = :id",
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
    public List<BeliefEntity> findByAgent(String agentId, boolean includeInactive) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }

        try {
            TypedQuery<BeliefEntity> query = entityManager.createNamedQuery("BeliefEntity.findByAgent", BeliefEntity.class);
            query.setParameter("agentId", agentId);
            query.setParameter("includeInactive", includeInactive);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find beliefs by agent: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BeliefEntity> findByCategory(String category, String agentId, boolean includeInactive) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be null or empty");
        }

        try {
            TypedQuery<BeliefEntity> query = entityManager.createNamedQuery("BeliefEntity.findByCategory", BeliefEntity.class);
            query.setParameter("category", category);
            query.setParameter("agentId", agentId);
            query.setParameter("includeInactive", includeInactive);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find beliefs by category: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BeliefEntity> findAllActive() {
        try {
            TypedQuery<BeliefEntity> query = entityManager.createNamedQuery("BeliefEntity.findAllActive", BeliefEntity.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find all active beliefs: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BeliefEntity> findAll() {
        try {
            TypedQuery<BeliefEntity> query = entityManager.createQuery(
                "SELECT b FROM BeliefEntity b ORDER BY b.lastUpdated DESC",
                BeliefEntity.class
            );
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find all beliefs: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BeliefEntity> findLowConfidenceBeliefs(double threshold, String agentId) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0");
        }

        try {
            TypedQuery<BeliefEntity> query = entityManager.createNamedQuery("BeliefEntity.findLowConfidence", BeliefEntity.class);
            query.setParameter("threshold", threshold);
            query.setParameter("agentId", agentId);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find low confidence beliefs: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BeliefEntity> searchByText(String searchText, String agentId, int limit) {
        if (searchText == null || searchText.trim().isEmpty()) {
            throw new IllegalArgumentException("Search text cannot be null or empty");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be at least 1");
        }

        try {
            TypedQuery<BeliefEntity> query = entityManager.createNamedQuery("BeliefEntity.searchByText", BeliefEntity.class);
            query.setParameter("searchText", searchText);
            query.setParameter("agentId", agentId);
            query.setMaxResults(limit);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to search beliefs by text: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BeliefEntity> findSimilarBeliefs(String statement, String agentId, double similarityThreshold, int limit) {
        if (statement == null || statement.trim().isEmpty()) {
            throw new IllegalArgumentException("Statement cannot be null or empty");
        }
        if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
            throw new IllegalArgumentException("Similarity threshold must be between 0.0 and 1.0");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be at least 1");
        }

        try {
            // For now, use simple text search as a fallback
            // In a real implementation, this would use vector similarity with embeddings
            String searchText = extractKeywordsFromStatement(statement);
            return searchByText(searchText, agentId, limit);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find similar beliefs: " + e.getMessage(), e);
        }
    }

    // ========== Statistics and Analytics ==========

    @Override
    public long countByAgent(String agentId, boolean includeInactive) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }

        try {
            TypedQuery<Long> query = entityManager.createNamedQuery("BeliefEntity.countByAgent", Long.class);
            query.setParameter("agentId", agentId);
            query.setParameter("includeInactive", includeInactive);
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count beliefs by agent: " + e.getMessage(), e);
        }
    }

    @Override
    public long count() {
        try {
            TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(b) FROM BeliefEntity b",
                Long.class
            );
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count all beliefs: " + e.getMessage(), e);
        }
    }

    @Override
    public long countActive() {
        try {
            TypedQuery<Long> query = entityManager.createQuery(
                "SELECT COUNT(b) FROM BeliefEntity b WHERE b.active = true",
                Long.class
            );
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Failed to count active beliefs: " + e.getMessage(), e);
        }
    }

    @Override
    public List<CategoryDistribution> getBeliefDistributionByCategory(String agentId) {
        try {
            String jpql = "SELECT b.category, COUNT(b) FROM BeliefEntity b " +
                         "WHERE b.active = true AND b.category IS NOT NULL " +
                         (agentId != null ? "AND b.agentId = :agentId " : "") +
                         "GROUP BY b.category ORDER BY COUNT(b) DESC";
            
            TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            
            return query.getResultList().stream()
                .map(result -> new CategoryDistribution((String) result[0], (Long) result[1]))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get belief distribution by category: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ConfidenceDistribution> getBeliefDistributionByConfidence(String agentId) {
        try {
            String jpql = "SELECT " +
                         "CASE " +
                         "  WHEN b.confidence >= 0.8 THEN 'high' " +
                         "  WHEN b.confidence >= 0.5 THEN 'medium' " +
                         "  ELSE 'low' " +
                         "END, COUNT(b) " +
                         "FROM BeliefEntity b " +
                         "WHERE b.active = true " +
                         (agentId != null ? "AND b.agentId = :agentId " : "") +
                         "GROUP BY " +
                         "CASE " +
                         "  WHEN b.confidence >= 0.8 THEN 'high' " +
                         "  WHEN b.confidence >= 0.5 THEN 'medium' " +
                         "  ELSE 'low' " +
                         "END";
            
            TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class);
            if (agentId != null) {
                query.setParameter("agentId", agentId);
            }
            
            return query.getResultList().stream()
                .map(result -> new ConfidenceDistribution((String) result[0], (Long) result[1]))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get belief distribution by confidence: " + e.getMessage(), e);
        }
    }

    // ========== Batch Operations ==========

    @Override
    public int updateBatch(List<BeliefEntity> beliefs) {
        if (beliefs == null) {
            throw new IllegalArgumentException("Beliefs list cannot be null");
        }

        if (beliefs.isEmpty()) {
            return 0;
        }

        try {
            int updated = 0;
            for (int i = 0; i < beliefs.size(); i++) {
                entityManager.merge(beliefs.get(i));
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
            throw new RuntimeException("Failed to update beliefs in batch: " + e.getMessage(), e);
        }
    }

    @Override
    public int deactivateBatch(List<String> beliefIds) {
        if (beliefIds == null) {
            throw new IllegalArgumentException("Belief IDs list cannot be null");
        }

        if (beliefIds.isEmpty()) {
            return 0;
        }

        try {
            TypedQuery<Integer> query = entityManager.createQuery(
                "UPDATE BeliefEntity b SET b.active = false, b.lastUpdated = CURRENT_TIMESTAMP WHERE b.id IN :ids",
                Integer.class
            );
            query.setParameter("ids", beliefIds);
            return query.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to deactivate beliefs in batch: " + e.getMessage(), e);
        }
    }

    @Override
    public int deleteBatch(List<String> beliefIds) {
        if (beliefIds == null) {
            throw new IllegalArgumentException("Belief IDs list cannot be null");
        }

        if (beliefIds.isEmpty()) {
            return 0;
        }

        try {
            TypedQuery<Integer> query = entityManager.createQuery(
                "DELETE FROM BeliefEntity b WHERE b.id IN :ids",
                Integer.class
            );
            query.setParameter("ids", beliefIds);
            return query.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete beliefs in batch: " + e.getMessage(), e);
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
    public void detach(BeliefEntity belief) {
        if (belief == null) {
            throw new IllegalArgumentException("Belief entity cannot be null");
        }

        try {
            entityManager.detach(belief);
        } catch (Exception e) {
            throw new RuntimeException("Failed to detach belief entity: " + e.getMessage(), e);
        }
    }

    @Override
    public void refresh(BeliefEntity belief) {
        if (belief == null) {
            throw new IllegalArgumentException("Belief entity cannot be null");
        }

        try {
            entityManager.refresh(belief);
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh belief entity: " + e.getMessage(), e);
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * Extracts keywords from a statement for similarity search.
     * 
     * @param statement The statement to extract keywords from
     * @return Keywords for search
     */
    private String extractKeywordsFromStatement(String statement) {
        if (statement == null || statement.trim().isEmpty()) {
            return "";
        }

        // Simple keyword extraction - remove common words and keep important terms
        String[] words = statement.toLowerCase().split("\\s+");
        Set<String> stopWords = Set.of("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "is", "are", "was", "were");
        
        return Arrays.stream(words)
            .filter(word -> word.length() > 2)
            .filter(word -> !stopWords.contains(word))
            .limit(5) // Take top 5 keywords
            .collect(Collectors.joining(" "));
    }
}