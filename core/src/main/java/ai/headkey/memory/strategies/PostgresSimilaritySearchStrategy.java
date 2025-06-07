package ai.headkey.memory.strategies;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.MemoryRecord;
import ai.headkey.memory.dto.Metadata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * PostgreSQL-specific similarity search strategy using native vector operations.
 * 
 * This strategy leverages PostgreSQL's pgvector extension for efficient
 * vector similarity search using dot product operations. It provides
 * high-performance semantic search capabilities for large-scale memory storage.
 * 
 * The strategy uses:
 * - Native vector dot product operations (<#>) for similarity scoring
 * - Combined vector and text-based scoring for hybrid search
 * - Optimized indexes for vector operations (IVFFlat or HNSW)
 * 
 * Requires PostgreSQL with pgvector extension installed.
 * 
 * @since 1.0
 */
public class PostgresSimilaritySearchStrategy implements SimilaritySearchStrategy {
    
    private static final String VECTOR_SEARCH_QUERY = """
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
                -- Vector similarity score (higher is more similar for dot product)
                CASE 
                    WHEN m.vector_embedding IS NOT NULL AND ? IS NOT NULL THEN
                        GREATEST(0, (m.vector_embedding <#> ?::vector) * 100.0)
                    ELSE 0.0
                END +
                -- Text-based similarity boost
                CASE 
                    WHEN LOWER(m.content) LIKE LOWER(?) THEN 25.0
                    WHEN LOWER(m.content) LIKE LOWER(?) THEN 15.0
                    ELSE 0.0
                END +
                -- Category relevance boost
                CASE 
                    WHEN LOWER(m.category_primary) LIKE LOWER(?) THEN 10.0
                    WHEN LOWER(m.category_tags) LIKE LOWER(?) THEN 5.0
                    ELSE 0.0
                END +
                -- Relevance score boost
                COALESCE(m.relevance_score * 5, 0.0)
            ) AS similarity_score
        FROM memories m
        WHERE (
            (m.vector_embedding IS NOT NULL AND ? IS NOT NULL)
            OR LOWER(m.content) LIKE LOWER(?) 
            OR LOWER(m.content) LIKE LOWER(?)
            OR LOWER(m.category_primary) LIKE LOWER(?)
            OR LOWER(m.category_tags) LIKE LOWER(?)
        )
        AND (? IS NULL OR m.agent_id = ?)
        ORDER BY similarity_score DESC, m.created_at DESC
        LIMIT ?
        """;
    
    private static final String TEXT_FALLBACK_QUERY = """
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
        
        // Use vector search if vector is available, otherwise fall back to text search
        String query = (queryVector != null) ? VECTOR_SEARCH_QUERY : TEXT_FALLBACK_QUERY;
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            if (queryVector != null) {
                // Vector search parameters
                String vectorString = vectorToPostgresArray(queryVector);
                stmt.setString(1, vectorString);  // Vector availability check
                stmt.setString(2, vectorString);  // Actual vector for dot product
                stmt.setString(3, exactMatch);    // Text exact match
                stmt.setString(4, wordMatch);     // Text word match
                stmt.setString(5, exactMatch);    // Category primary match
                stmt.setString(6, exactMatch);    // Category tags match
                stmt.setString(7, vectorString);  // Vector availability for WHERE clause
                stmt.setString(8, exactMatch);    // Content exact match for WHERE
                stmt.setString(9, wordMatch);     // Content word match for WHERE
                stmt.setString(10, exactMatch);   // Category primary for WHERE
                stmt.setString(11, exactMatch);   // Category tags for WHERE
                stmt.setString(12, agentId);      // Agent ID filter
                stmt.setString(13, agentId);      // Agent ID filter (for IS NULL check)
                stmt.setInt(14, limit);           // Limit
            } else {
                // Text-only search parameters (same as HSQLDB strategy)
                stmt.setString(1, exactMatch);
                stmt.setString(2, wordMatch);
                stmt.setString(3, exactMatch);
                stmt.setString(4, exactMatch);
                stmt.setString(5, exactMatch);
                stmt.setString(6, wordMatch);
                stmt.setString(7, exactMatch);
                stmt.setString(8, exactMatch);
                stmt.setString(9, agentId);
                stmt.setString(10, agentId);
                stmt.setInt(11, limit);
            }
            
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
        return true;
    }
    
    @Override
    public String getStrategyName() {
        return "PostgreSQL Vector Similarity Search";
    }
    
    @Override
    public void initialize(Connection connection) throws Exception {
        // Check if pgvector extension is available
        try (PreparedStatement stmt = connection.prepareStatement("""
            SELECT 1 FROM pg_extension WHERE extname = 'vector'
            """)) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException(
                        "PostgreSQL pgvector extension is not installed. " +
                        "Please install it with: CREATE EXTENSION vector;"
                    );
                }
            }
        }
        
        // Create optimized indexes for vector operations
        try (PreparedStatement stmt = connection.prepareStatement("""
            CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_memories_vector_cosine 
            ON memories USING ivfflat (vector_embedding vector_cosine_ops) 
            WITH (lists = 100)
            """)) {
            stmt.executeUpdate();
        } catch (Exception e) {
            // If concurrent index creation fails, try without CONCURRENTLY
            try (PreparedStatement stmt = connection.prepareStatement("""
                CREATE INDEX IF NOT EXISTS idx_memories_vector_cosine 
                ON memories USING ivfflat (vector_embedding vector_cosine_ops) 
                WITH (lists = 100)
                """)) {
                stmt.executeUpdate();
            }
        }
        
        // Create index for dot product operations
        try (PreparedStatement stmt = connection.prepareStatement("""
            CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_memories_vector_dot_product 
            ON memories USING ivfflat (vector_embedding vector_ip_ops) 
            WITH (lists = 100)
            """)) {
            stmt.executeUpdate();
        } catch (Exception e) {
            try (PreparedStatement stmt = connection.prepareStatement("""
                CREATE INDEX IF NOT EXISTS idx_memories_vector_dot_product 
                ON memories USING ivfflat (vector_embedding vector_ip_ops) 
                WITH (lists = 100)
                """)) {
                stmt.executeUpdate();
            }
        }
        
        // Create text search indexes
        try (PreparedStatement stmt = connection.prepareStatement("""
            CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_memories_content_gin 
            ON memories USING gin (to_tsvector('english', content))
            """)) {
            stmt.executeUpdate();
        } catch (Exception e) {
            try (PreparedStatement stmt = connection.prepareStatement("""
                CREATE INDEX IF NOT EXISTS idx_memories_content_gin 
                ON memories USING gin (to_tsvector('english', content))
                """)) {
                stmt.executeUpdate();
            }
        }
        
        // Create composite index for agent filtering with vector operations
        try (PreparedStatement stmt = connection.prepareStatement("""
            CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_memories_agent_vector 
            ON memories (agent_id) WHERE vector_embedding IS NOT NULL
            """)) {
            stmt.executeUpdate();
        } catch (Exception e) {
            try (PreparedStatement stmt = connection.prepareStatement("""
                CREATE INDEX IF NOT EXISTS idx_memories_agent_vector 
                ON memories (agent_id) WHERE vector_embedding IS NOT NULL
                """)) {
                stmt.executeUpdate();
            }
        }
    }
    
    @Override
    public boolean validateSchema(Connection connection) throws Exception {
        // Check if the memories table exists with required columns including vector column
        try (PreparedStatement stmt = connection.prepareStatement("""
            SELECT COUNT(*) FROM information_schema.columns 
            WHERE table_name = 'memories' 
            AND column_name IN ('id', 'content', 'category_primary', 'agent_id', 'vector_embedding')
            """)) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) >= 5; // At least 5 required columns exist
                }
            }
        }
        
        // Also check if pgvector extension is available
        try (PreparedStatement stmt = connection.prepareStatement("""
            SELECT 1 FROM pg_extension WHERE extname = 'vector'
            """)) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    /**
     * Converts a double array to PostgreSQL vector format.
     */
    private String vectorToPostgresArray(double[] vector) {
        if (vector == null || vector.length == 0) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
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