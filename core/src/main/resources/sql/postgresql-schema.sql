-- PostgreSQL schema for HeadKey Memory Encoding System
-- Requires pgvector extension for vector similarity search

-- Enable pgvector extension for vector operations
CREATE EXTENSION IF NOT EXISTS vector;

-- Main memories table with vector embedding support
CREATE TABLE IF NOT EXISTS memories (
    id VARCHAR(255) PRIMARY KEY,
    agent_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    category_primary VARCHAR(255),
    category_secondary VARCHAR(255),
    category_tags TEXT,
    category_confidence DOUBLE PRECISION,
    metadata_json JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    relevance_score DOUBLE PRECISION,
    version BIGINT NOT NULL DEFAULT 1,
    vector_embedding VECTOR(1536), -- OpenAI embedding dimension
    
    -- Constraints
    CONSTRAINT memories_relevance_score_check CHECK (relevance_score >= 0.0 AND relevance_score <= 1.0),
    CONSTRAINT memories_version_check CHECK (version > 0),
    CONSTRAINT memories_category_confidence_check CHECK (category_confidence >= 0.0 AND category_confidence <= 1.0)
);

-- Basic indexes for common queries
CREATE INDEX IF NOT EXISTS idx_memories_agent_id ON memories (agent_id);
CREATE INDEX IF NOT EXISTS idx_memories_created_at ON memories (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_memories_last_accessed ON memories (last_accessed DESC);
CREATE INDEX IF NOT EXISTS idx_memories_category_primary ON memories (category_primary);
CREATE INDEX IF NOT EXISTS idx_memories_relevance_score ON memories (relevance_score DESC);

-- Vector similarity indexes for efficient similarity search
-- IVFFlat index for cosine similarity (good for general use)
CREATE INDEX IF NOT EXISTS idx_memories_vector_cosine 
ON memories USING ivfflat (vector_embedding vector_cosine_ops) 
WITH (lists = 100);

-- IVFFlat index for dot product similarity (when vectors are normalized)
CREATE INDEX IF NOT EXISTS idx_memories_vector_dot_product 
ON memories USING ivfflat (vector_embedding vector_ip_ops) 
WITH (lists = 100);

-- L2 distance index (alternative similarity metric)
CREATE INDEX IF NOT EXISTS idx_memories_vector_l2 
ON memories USING ivfflat (vector_embedding vector_l2_ops) 
WITH (lists = 100);

-- Full-text search index for content
CREATE INDEX IF NOT EXISTS idx_memories_content_fts 
ON memories USING gin (to_tsvector('english', content));

-- Composite indexes for filtered searches
CREATE INDEX IF NOT EXISTS idx_memories_agent_relevance 
ON memories (agent_id, relevance_score DESC);

CREATE INDEX IF NOT EXISTS idx_memories_agent_category 
ON memories (agent_id, category_primary);

CREATE INDEX IF NOT EXISTS idx_memories_agent_created 
ON memories (agent_id, created_at DESC);

-- Index for vector queries filtered by agent
CREATE INDEX IF NOT EXISTS idx_memories_agent_vector 
ON memories (agent_id) WHERE vector_embedding IS NOT NULL;

-- JSONB indexes for metadata queries
CREATE INDEX IF NOT EXISTS idx_memories_metadata_gin 
ON memories USING gin (metadata_json);

-- Partial indexes for common filtering scenarios
CREATE INDEX IF NOT EXISTS idx_memories_high_relevance 
ON memories (agent_id, created_at DESC) 
WHERE relevance_score >= 0.7;

CREATE INDEX IF NOT EXISTS idx_memories_recent 
ON memories (agent_id, relevance_score DESC) 
WHERE created_at >= NOW() - INTERVAL '30 days';

-- Create a function to update last_accessed timestamp
CREATE OR REPLACE FUNCTION update_last_accessed_trigger()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_accessed = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Optional: Create trigger to automatically update last_accessed on SELECT
-- (Note: This would require custom implementation in application layer)

-- Create a function for similarity search with hybrid scoring
CREATE OR REPLACE FUNCTION similarity_search_hybrid(
    query_vector VECTOR(1536),
    query_text TEXT,
    agent_filter VARCHAR(255) DEFAULT NULL,
    result_limit INTEGER DEFAULT 10
)
RETURNS TABLE (
    memory_id VARCHAR(255),
    similarity_score DOUBLE PRECISION,
    content TEXT,
    agent_id VARCHAR(255),
    category_primary VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE,
    relevance_score DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        m.id,
        (
            -- Vector similarity (cosine distance converted to similarity)
            CASE 
                WHEN m.vector_embedding IS NOT NULL AND query_vector IS NOT NULL THEN
                    (1.0 - (m.vector_embedding <=> query_vector)) * 100.0
                ELSE 0.0
            END +
            -- Text similarity boost
            CASE 
                WHEN query_text IS NOT NULL THEN
                    CASE 
                        WHEN LOWER(m.content) LIKE LOWER('%' || query_text || '%') THEN 25.0
                        WHEN to_tsvector('english', m.content) @@ plainto_tsquery('english', query_text) THEN 15.0
                        ELSE 0.0
                    END
                ELSE 0.0
            END +
            -- Relevance score boost
            COALESCE(m.relevance_score * 10, 0.0) +
            -- Recency boost (newer memories get slight preference)
            CASE 
                WHEN m.created_at >= NOW() - INTERVAL '7 days' THEN 5.0
                WHEN m.created_at >= NOW() - INTERVAL '30 days' THEN 2.0
                ELSE 0.0
            END
        ) AS similarity_score,
        m.content,
        m.agent_id,
        m.category_primary,
        m.created_at,
        m.relevance_score
    FROM memories m
    WHERE 
        (agent_filter IS NULL OR m.agent_id = agent_filter)
        AND (
            (m.vector_embedding IS NOT NULL AND query_vector IS NOT NULL)
            OR (query_text IS NOT NULL AND (
                LOWER(m.content) LIKE LOWER('%' || query_text || '%')
                OR to_tsvector('english', m.content) @@ plainto_tsquery('english', query_text)
            ))
        )
    ORDER BY similarity_score DESC, m.created_at DESC
    LIMIT result_limit;
END;
$$ LANGUAGE plpgsql;

-- Create indexes to support the hybrid search function
CREATE INDEX IF NOT EXISTS idx_memories_hybrid_search 
ON memories (agent_id, created_at DESC) 
WHERE vector_embedding IS NOT NULL;

-- Statistics and monitoring views
CREATE OR REPLACE VIEW memory_statistics AS
SELECT 
    COUNT(*) as total_memories,
    COUNT(DISTINCT agent_id) as unique_agents,
    COUNT(DISTINCT category_primary) as unique_categories,
    AVG(relevance_score) as avg_relevance_score,
    COUNT(CASE WHEN vector_embedding IS NOT NULL THEN 1 END) as memories_with_vectors,
    MIN(created_at) as oldest_memory,
    MAX(created_at) as newest_memory,
    MAX(last_accessed) as most_recent_access
FROM memories;

CREATE OR REPLACE VIEW agent_memory_stats AS
SELECT 
    agent_id,
    COUNT(*) as memory_count,
    AVG(relevance_score) as avg_relevance_score,
    COUNT(DISTINCT category_primary) as unique_categories,
    MIN(created_at) as oldest_memory,
    MAX(created_at) as newest_memory,
    MAX(last_accessed) as most_recent_access
FROM memories 
GROUP BY agent_id;

CREATE OR REPLACE VIEW category_distribution AS
SELECT 
    category_primary,
    COUNT(*) as memory_count,
    COUNT(DISTINCT agent_id) as agent_count,
    AVG(relevance_score) as avg_relevance_score
FROM memories 
WHERE category_primary IS NOT NULL
GROUP BY category_primary
ORDER BY memory_count DESC;

-- Performance monitoring view
CREATE OR REPLACE VIEW vector_index_stats AS
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_tup_read,
    idx_tup_fetch,
    idx_blks_read,
    idx_blks_hit
FROM pg_stat_user_indexes 
WHERE tablename = 'memories' 
AND indexname LIKE 'idx_memories_vector%';

-- Cleanup and maintenance functions
CREATE OR REPLACE FUNCTION cleanup_old_memories(
    days_threshold INTEGER DEFAULT 365,
    min_relevance_score DOUBLE PRECISION DEFAULT 0.1
)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM memories 
    WHERE 
        created_at < NOW() - INTERVAL '1 day' * days_threshold
        AND relevance_score < min_relevance_score;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to rebuild vector indexes
CREATE OR REPLACE FUNCTION rebuild_vector_indexes()
RETURNS TEXT AS $$
BEGIN
    -- Reindex vector indexes concurrently
    EXECUTE 'REINDEX INDEX CONCURRENTLY idx_memories_vector_cosine';
    EXECUTE 'REINDEX INDEX CONCURRENTLY idx_memories_vector_dot_product';
    EXECUTE 'REINDEX INDEX CONCURRENTLY idx_memories_vector_l2';
    
    RETURN 'Vector indexes rebuilt successfully';
EXCEPTION
    WHEN OTHERS THEN
        RETURN 'Error rebuilding indexes: ' || SQLERRM;
END;
$$ LANGUAGE plpgsql;

-- Grant permissions (adjust as needed for your security model)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON memories TO memory_app_user;
-- GRANT USAGE ON SCHEMA public TO memory_app_user;
-- GRANT EXECUTE ON FUNCTION similarity_search_hybrid TO memory_app_user;
-- GRANT EXECUTE ON FUNCTION cleanup_old_memories TO memory_admin_user;
-- GRANT EXECUTE ON FUNCTION rebuild_vector_indexes TO memory_admin_user;

-- Comments for documentation
COMMENT ON TABLE memories IS 'Main table storing memory records with vector embeddings for similarity search';
COMMENT ON COLUMN memories.vector_embedding IS 'Vector embedding for semantic similarity search (1536 dimensions for OpenAI embeddings)';
COMMENT ON COLUMN memories.metadata_json IS 'JSONB field for flexible metadata storage';
COMMENT ON FUNCTION similarity_search_hybrid IS 'Hybrid similarity search combining vector and text-based scoring';
COMMENT ON VIEW memory_statistics IS 'Overall statistics about the memory system';
COMMENT ON VIEW agent_memory_stats IS 'Per-agent memory statistics';
COMMENT ON VIEW category_distribution IS 'Distribution of memories across categories';