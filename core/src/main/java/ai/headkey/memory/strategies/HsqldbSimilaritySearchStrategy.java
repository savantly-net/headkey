package ai.headkey.memory.strategies;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.dto.Metadata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * HSQLDB-specific similarity search strategy using text-based matching.
 * 
 * Since HSQLDB doesn't have native vector similarity search capabilities,
 * this strategy falls back to text-based similarity using SQL LIKE operations
 * and full-text search features available in HSQLDB.
 * 
 * The strategy uses a combination of:
 * - Exact phrase matching
 * - Word-based matching with scoring
 * - Category and metadata relevance boost
 * 
 * @since 1.0
 */
public class HsqldbSimilaritySearchStrategy implements SimilaritySearchStrategy {
    
    private static final String SEARCH_QUERY = """
        SELECT 
            m.id,
            m.agent_id,
            m.content,
            m.category_primary,
            m.category_secondary,
            m.category_tags,
            m.category_confidence,
            m.metadata_json,
            m.created_at,
            m.last_accessed,
            m.relevance_score,
            m.version,
            m.vector_embedding,
            (
                CASE 
                    WHEN LOWER(m.content) LIKE LOWER(?) THEN 100.0
                    WHEN LOWER(m.content) LIKE LOWER(?) THEN 75.0
                    ELSE 0.0
                END +
                CASE 
                    WHEN LOWER(m.category_primary) LIKE LOWER(?) THEN 25.0
                    WHEN LOWER(m.category_tags) LIKE LOWER(?) THEN 15.0
                    ELSE 0.0
                END +
                COALESCE(m.relevance_score * 10, 0.0)
            ) AS similarity_score
        FROM memories m
        WHERE (
            LOWER(m.content) LIKE LOWER(?) 
            OR LOWER(m.content) LIKE LOWER(?) 
            OR LOWER(m.category_primary) LIKE LOWER(?)
            OR LOWER(m.category_tags) LIKE LOWER(?)
        )
        AND (? IS NULL OR m.agent_id = ?)
        ORDER BY similarity_score DESC, m.created_at DESC
        LIMIT ?
        """;
    
    @Override
    public List<MemoryRecord> searchSimilar(Connection connection, String queryContent, 
                                          double[] queryVector, String agentId, int limit) throws Exception {
        
        if (queryContent == null || queryContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Query content cannot be null or empty");
        }
        
        String normalizedQuery = queryContent.trim().toLowerCase();
        String exactMatch = "%" + normalizedQuery + "%";
        String wordMatch = "%" + normalizedQuery.replace(" ", "%") + "%";
        
        List<MemoryRecord> results = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(SEARCH_QUERY)) {
            // Set parameters for similarity scoring
            stmt.setString(1, exactMatch);  // Exact phrase match in content (highest score)
            stmt.setString(2, wordMatch);   // Word-based match in content (medium score)
            stmt.setString(3, exactMatch);  // Category primary match
            stmt.setString(4, exactMatch);  // Category tags match
            
            // Set parameters for WHERE clause filtering
            stmt.setString(5, exactMatch);  // Content exact match
            stmt.setString(6, wordMatch);   // Content word match
            stmt.setString(7, exactMatch);  // Category primary match
            stmt.setString(8, exactMatch);  // Category tags match
            
            // Agent ID filter (twice for IS NULL check and equality check)
            stmt.setString(9, agentId);
            stmt.setString(10, agentId);
            
            // Limit
            stmt.setInt(11, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    MemoryRecord record = mapResultSetToMemoryRecord(rs);
                    results.add(record);
                }
            }
        }
        
        return results;
    }
    
    @Override
    public boolean supportsVectorSearch() {
        return false;
    }
    
    @Override
    public String getStrategyName() {
        return "HSQLDB Text-Based Similarity Search";
    }
    
    @Override
    public void initialize(Connection connection) throws Exception {
        // Create simple indexes for HSQLDB (no function-based indexes)
        try (PreparedStatement stmt = connection.prepareStatement("""
            CREATE INDEX IF NOT EXISTS idx_memories_content 
            ON memories (content)
            """)) {
            stmt.executeUpdate();
        }
        
        try (PreparedStatement stmt = connection.prepareStatement("""
            CREATE INDEX IF NOT EXISTS idx_memories_category 
            ON memories (category_primary)
            """)) {
            stmt.executeUpdate();
        }
        
        try (PreparedStatement stmt = connection.prepareStatement("""
            CREATE INDEX IF NOT EXISTS idx_memories_agent_relevance 
            ON memories (agent_id, relevance_score)
            """)) {
            stmt.executeUpdate();
        }
    }
    
    @Override
    public boolean validateSchema(Connection connection) throws Exception {
        // Simple schema validation: just check if we can query the memories table
        try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM memories WHERE 1=0")) {
            stmt.executeQuery();
            return true; // If we can query the table, schema is OK
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Maps a ResultSet row to a MemoryRecord object.
     */
    private MemoryRecord mapResultSetToMemoryRecord(ResultSet rs) throws Exception {
        MemoryRecord record = new MemoryRecord();
        
        record.setId(rs.getString("id"));
        record.setAgentId(rs.getString("agent_id"));
        record.setContent(rs.getString("content"));
        
        // Map category information
        String primaryCategory = rs.getString("category_primary");
        String secondaryCategory = rs.getString("category_secondary");
        String tags = rs.getString("category_tags");
        Double confidence = rs.getObject("category_confidence") != null ? 
                           rs.getDouble("category_confidence") : null;
        
        if (primaryCategory != null) {
            CategoryLabel category = new CategoryLabel();
            category.setPrimary(primaryCategory);
            category.setSecondary(secondaryCategory);
            if (tags != null && !tags.trim().isEmpty()) {
                category.setTags(Set.of(tags.split(",")));
            }
            category.setConfidence(confidence);
            record.setCategory(category);
        }
        
        // Map metadata (assuming JSON string storage)
        String metadataJson = rs.getString("metadata_json");
        if (metadataJson != null && !metadataJson.trim().isEmpty()) {
            // Note: In a real implementation, you'd use a JSON parser here
            // For now, we'll create basic metadata
            Metadata metadata = new Metadata();
            record.setMetadata(metadata);
        }
        
        // Map timestamps
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            record.setCreatedAt(createdAt.toInstant());
        }
        
        Timestamp lastAccessed = rs.getTimestamp("last_accessed");
        if (lastAccessed != null) {
            record.setLastAccessed(lastAccessed.toInstant());
        }
        
        // Map other fields
        Double relevanceScore = rs.getObject("relevance_score") != null ? 
                               rs.getDouble("relevance_score") : null;
        record.setRelevanceScore(relevanceScore);
        
        Long version = rs.getObject("version") != null ? 
                      rs.getLong("version") : null;
        record.setVersion(version);
        
        return record;
    }
}