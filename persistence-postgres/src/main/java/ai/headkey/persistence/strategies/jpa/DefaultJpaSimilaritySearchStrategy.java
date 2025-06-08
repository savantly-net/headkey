package ai.headkey.persistence.strategies.jpa;

import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.persistence.entities.MemoryEntity;
import ai.headkey.memory.exceptions.StorageException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default JPA similarity search strategy that supports both vector-based and text-based similarity search.
 * 
 * This implementation:
 * - Uses cosine similarity for vector embeddings when available
 * - Falls back to text-based search using LIKE queries when no embeddings are present
 * - Retrieves entities in batches to prevent memory issues
 * - Updates last accessed timestamps during search
 * 
 * @since 1.0
 */
public class DefaultJpaSimilaritySearchStrategy implements JpaSimilaritySearchStrategy {
    
    private static final String VECTOR_QUERY = """
        SELECT m FROM MemoryEntity m 
        WHERE m.embedding IS NOT NULL 
        AND (:agentId IS NULL OR m.agentId = :agentId)
        ORDER BY m.lastAccessed DESC
        """;
    
    private static final String TEXT_QUERY = """
        SELECT m FROM MemoryEntity m 
        WHERE LOWER(m.content) LIKE LOWER(:query) 
        AND (:agentId IS NULL OR m.agentId = :agentId)
        ORDER BY m.lastAccessed DESC
        """;
    
    @Override
    public List<MemoryRecord> searchSimilar(EntityManager entityManager, String queryContent, 
                                          double[] queryVector, String agentId, int limit,
                                          int maxSimilaritySearchResults, double similarityThreshold) throws Exception {
        
        if (queryVector != null) {
            return performVectorSearch(entityManager, queryContent, queryVector, agentId, 
                                     limit, maxSimilaritySearchResults, similarityThreshold);
        } else {
            return performTextSearch(entityManager, queryContent, agentId, limit);
        }
    }
    
    private List<MemoryRecord> performVectorSearch(EntityManager entityManager, String queryContent, 
                                                 double[] queryVector, String agentId, int limit,
                                                 int maxSimilaritySearchResults, double similarityThreshold) throws Exception {
        
        // Retrieve entities with embeddings for similarity calculation
        TypedQuery<MemoryEntity> query = entityManager.createQuery(VECTOR_QUERY, MemoryEntity.class);
        query.setParameter("agentId", agentId);
        query.setMaxResults(maxSimilaritySearchResults);
        
        List<MemoryEntity> entities = query.getResultList();
        
        // Calculate similarities and sort
        List<SimilarityResult> similarities = entities.stream()
            .map(entity -> {
                double similarity = calculateCosineSimilarity(queryVector, entity.getEmbedding());
                return new SimilarityResult(entity, similarity);
            })
            .filter(result -> result.similarity >= similarityThreshold)
            .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
            .limit(limit)
            .collect(Collectors.toList());
        
        // Update access times and convert to DTOs
        EntityTransaction transaction = entityManager.getTransaction();
        boolean wasActive = transaction.isActive();
        
        if (!wasActive) {
            transaction.begin();
        }
        
        try {
            List<MemoryRecord> results = similarities.stream()
                .map(result -> {
                    result.entity.updateLastAccessed();
                    return result.entity.toMemoryRecord();
                })
                .collect(Collectors.toList());
            
            if (!wasActive) {
                transaction.commit();
            }
            
            return results;
        } catch (Exception e) {
            if (!wasActive && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }
    
    private List<MemoryRecord> performTextSearch(EntityManager entityManager, String queryContent, 
                                               String agentId, int limit) throws Exception {
        
        // Simple text-based search using LIKE operator
        TypedQuery<MemoryEntity> query = entityManager.createQuery(TEXT_QUERY, MemoryEntity.class);
        query.setParameter("query", "%" + queryContent + "%");
        query.setParameter("agentId", agentId);
        query.setMaxResults(limit);
        
        List<MemoryEntity> entities = query.getResultList();
        
        // Update access times and convert to DTOs
        EntityTransaction transaction = entityManager.getTransaction();
        boolean wasActive = transaction.isActive();
        
        if (!wasActive) {
            transaction.begin();
        }
        
        try {
            List<MemoryRecord> results = entities.stream()
                .peek(MemoryEntity::updateLastAccessed)
                .map(MemoryEntity::toMemoryRecord)
                .collect(Collectors.toList());
            
            if (!wasActive) {
                transaction.commit();
            }
            
            return results;
        } catch (Exception e) {
            if (!wasActive && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }
    
    @Override
    public boolean supportsVectorSearch() {
        return true;
    }
    
    @Override
    public String getStrategyName() {
        return "Default JPA Similarity Search";
    }
    
    @Override
    public boolean validateSchema(EntityManager entityManager) throws Exception {
        try {
            // Simple validation by attempting to query the MemoryEntity table
            TypedQuery<Long> countQuery = entityManager.createQuery(
                "SELECT COUNT(m) FROM MemoryEntity m", Long.class);
            countQuery.setMaxResults(1);
            countQuery.getSingleResult();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Internal class to hold similarity calculation results.
     */
    private static class SimilarityResult {
        final MemoryEntity entity;
        final double similarity;
        
        SimilarityResult(MemoryEntity entity, double similarity) {
            this.entity = entity;
            this.similarity = similarity;
        }
    }
}