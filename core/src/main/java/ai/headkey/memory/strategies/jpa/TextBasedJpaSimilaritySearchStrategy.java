package ai.headkey.memory.strategies.jpa;

import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.entities.MemoryEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Text-based JPA similarity search strategy for databases without vector support.
 * 
 * This implementation:
 * - Uses JPQL queries with LIKE operations for text matching
 * - Supports case-insensitive search
 * - Orders results by relevance score and recency
 * - Optimized for databases like H2, HSQLDB, MySQL without vector extensions
 * - Updates last accessed timestamps during search
 * 
 * @since 1.0
 */
public class TextBasedJpaSimilaritySearchStrategy implements JpaSimilaritySearchStrategy {
    
    private static final String TEXT_SEARCH_QUERY = """
        SELECT m FROM MemoryEntity m 
        WHERE LOWER(m.content) LIKE LOWER(:query) 
        AND (:agentId IS NULL OR m.agentId = :agentId)
        ORDER BY 
            CASE WHEN m.relevanceScore IS NOT NULL THEN m.relevanceScore ELSE 0.0 END DESC,
            m.lastAccessed DESC,
            m.createdAt DESC
        """;
    
    private static final String KEYWORD_SEARCH_QUERY = """
        SELECT m FROM MemoryEntity m 
        WHERE (:agentId IS NULL OR m.agentId = :agentId)
        AND (
            LOWER(m.content) LIKE LOWER(:keyword1) OR
            LOWER(m.content) LIKE LOWER(:keyword2) OR
            LOWER(m.content) LIKE LOWER(:keyword3) OR
            LOWER(m.content) LIKE LOWER(:keyword4) OR
            LOWER(m.content) LIKE LOWER(:keyword5)
        )
        ORDER BY 
            CASE WHEN m.relevanceScore IS NOT NULL THEN m.relevanceScore ELSE 0.0 END DESC,
            m.lastAccessed DESC,
            m.createdAt DESC
        """;
    
    @Override
    public List<MemoryRecord> searchSimilar(EntityManager entityManager, String queryContent, 
                                          double[] queryVector, String agentId, int limit,
                                          int maxSimilaritySearchResults, double similarityThreshold) throws Exception {
        
        // Extract keywords from query content for better matching
        String[] keywords = extractKeywords(queryContent);
        
        if (keywords.length <= 1) {
            return performSimpleTextSearch(entityManager, queryContent, agentId, limit);
        } else {
            return performKeywordSearch(entityManager, keywords, agentId, limit);
        }
    }
    
    private List<MemoryRecord> performSimpleTextSearch(EntityManager entityManager, String queryContent, 
                                                     String agentId, int limit) throws Exception {
        
        TypedQuery<MemoryEntity> query = entityManager.createQuery(TEXT_SEARCH_QUERY, MemoryEntity.class);
        query.setParameter("query", "%" + queryContent + "%");
        query.setParameter("agentId", agentId);
        query.setMaxResults(limit);
        
        List<MemoryEntity> entities = query.getResultList();
        
        return updateAccessTimesAndConvert(entityManager, entities);
    }
    
    private List<MemoryRecord> performKeywordSearch(EntityManager entityManager, String[] keywords, 
                                                  String agentId, int limit) throws Exception {
        
        TypedQuery<MemoryEntity> query = entityManager.createQuery(KEYWORD_SEARCH_QUERY, MemoryEntity.class);
        query.setParameter("agentId", agentId);
        
        // Set keyword parameters (pad with empty strings if fewer than 5 keywords)
        for (int i = 0; i < 5; i++) {
            String keyword = (i < keywords.length) ? "%" + keywords[i] + "%" : "%__EMPTY_KEYWORD__%";
            query.setParameter("keyword" + (i + 1), keyword);
        }
        
        query.setMaxResults(Math.min(limit * 2, 100)); // Get more results for better filtering
        
        List<MemoryEntity> entities = query.getResultList();
        
        // Score and rank results based on keyword matches
        List<MemoryEntity> scoredEntities = entities.stream()
            .map(entity -> {
                int matchCount = countKeywordMatches(entity.getContent().toLowerCase(), keywords);
                // Boost entities with more keyword matches
                if (matchCount > 1) {
                    // Temporarily boost relevance score for sorting
                    entity.setRelevanceScore((entity.getRelevanceScore() != null ? entity.getRelevanceScore() : 0.0) + matchCount * 0.1);
                }
                return entity;
            })
            .filter(entity -> countKeywordMatches(entity.getContent().toLowerCase(), keywords) > 0)
            .sorted((a, b) -> {
                // Sort by relevance score (including boost), then by recency
                int scoreComparison = Double.compare(
                    b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0,
                    a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0
                );
                if (scoreComparison != 0) {
                    return scoreComparison;
                }
                return b.getLastAccessed().compareTo(a.getLastAccessed());
            })
            .limit(limit)
            .collect(Collectors.toList());
        
        return updateAccessTimesAndConvert(entityManager, scoredEntities);
    }
    
    private List<MemoryRecord> updateAccessTimesAndConvert(EntityManager entityManager, List<MemoryEntity> entities) throws Exception {
        if (entities.isEmpty()) {
            return List.of();
        }
        
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
    
    private String[] extractKeywords(String queryContent) {
        if (queryContent == null || queryContent.trim().isEmpty()) {
            return new String[0];
        }
        
        // Simple keyword extraction: split by whitespace and filter out short words
        return Arrays.stream(queryContent.toLowerCase()
            .trim()
            .split("\\s+"))
            .filter(word -> word.length() > 2) // Ignore very short words
            .filter(word -> !isStopWord(word)) // Remove common stop words
            .distinct()
            .limit(5) // Limit to 5 keywords for performance
            .toArray(String[]::new);
    }
    
    private boolean isStopWord(String word) {
        // Simple stop word list - could be expanded
        String[] stopWords = {
            "the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by",
            "is", "are", "was", "were", "be", "been", "being", "have", "has", "had",
            "do", "does", "did", "will", "would", "could", "should", "may", "might",
            "this", "that", "these", "those", "a", "an", "as", "if", "it", "its",
            "from", "into", "onto", "upon", "over", "under", "above", "below"
        };
        
        for (String stopWord : stopWords) {
            if (stopWord.equals(word)) {
                return true;
            }
        }
        return false;
    }
    
    private int countKeywordMatches(String content, String[] keywords) {
        int count = 0;
        for (String keyword : keywords) {
            if (content.contains(keyword.toLowerCase())) {
                count++;
            }
        }
        return count;
    }
    
    @Override
    public boolean supportsVectorSearch() {
        return false;
    }
    
    @Override
    public String getStrategyName() {
        return "Text-based JPA Similarity Search";
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
}