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

-- Create belief_relationships table for knowledge graph edges
CREATE TABLE IF NOT EXISTS belief_relationships (
    id VARCHAR(100) PRIMARY KEY,
    source_belief_id VARCHAR(100) NOT NULL,
    target_belief_id VARCHAR(100) NOT NULL,
    agent_id VARCHAR(100) NOT NULL,
    relationship_type VARCHAR(50) NOT NULL,
    strength DECIMAL(5,4) NOT NULL DEFAULT 1.0,
    effective_from TIMESTAMP WITH TIME ZONE,
    effective_until TIMESTAMP WITH TIME ZONE,
    deprecation_reason TEXT,
    priority INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT DEFAULT 0,
    
    -- Constraints
    CONSTRAINT rel_strength_check CHECK (strength >= 0.0 AND strength <= 1.0),
    CONSTRAINT rel_different_beliefs CHECK (source_belief_id != target_belief_id),
    CONSTRAINT rel_temporal_validity CHECK (
        effective_from IS NULL OR 
        effective_until IS NULL OR 
        effective_from <= effective_until
    ),
    CONSTRAINT rel_valid_type CHECK (
        relationship_type IN (
            'SUPERSEDES', 'UPDATES', 'DEPRECATES', 'REPLACES',
            'SUPPORTS', 'CONTRADICTS', 'IMPLIES', 'REINFORCES', 'WEAKENS',
            'RELATES_TO', 'SPECIALIZES', 'GENERALIZES', 'EXTENDS', 'DERIVES_FROM',
            'CAUSES', 'CAUSED_BY', 'ENABLES', 'PREVENTS',
            'DEPENDS_ON', 'PRECEDES', 'FOLLOWS', 'CONTEXT_FOR',
            'EVIDENCED_BY', 'PROVIDES_EVIDENCE_FOR', 'CONFLICTS_WITH',
            'SIMILAR_TO', 'ANALOGOUS_TO', 'CONTRASTS_WITH',
            'CUSTOM'
        )
    )
);

-- Metadata table for flexible relationship properties
CREATE TABLE IF NOT EXISTS belief_relationship_metadata (
    relationship_id VARCHAR(100) NOT NULL,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value TEXT,
    PRIMARY KEY (relationship_id, metadata_key),
    CONSTRAINT fk_rel_metadata FOREIGN KEY (relationship_id) 
        REFERENCES belief_relationships(id) ON DELETE CASCADE
);

-- Performance indexes for belief relationships
CREATE INDEX IF NOT EXISTS idx_rel_source_belief ON belief_relationships(source_belief_id);
CREATE INDEX IF NOT EXISTS idx_rel_target_belief ON belief_relationships(target_belief_id);
CREATE INDEX IF NOT EXISTS idx_rel_agent_id ON belief_relationships(agent_id);
CREATE INDEX IF NOT EXISTS idx_rel_type ON belief_relationships(relationship_type);
CREATE INDEX IF NOT EXISTS idx_rel_active ON belief_relationships(active);
CREATE INDEX IF NOT EXISTS idx_rel_strength ON belief_relationships(strength DESC);
CREATE INDEX IF NOT EXISTS idx_rel_created_at ON belief_relationships(created_at DESC);

-- Temporal relationship indexes
CREATE INDEX IF NOT EXISTS idx_rel_temporal ON belief_relationships(effective_from, effective_until);
CREATE INDEX IF NOT EXISTS idx_rel_effective_from ON belief_relationships(effective_from) WHERE effective_from IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_rel_effective_until ON belief_relationships(effective_until) WHERE effective_until IS NOT NULL;

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_rel_agent_active ON belief_relationships(agent_id, active);
CREATE INDEX IF NOT EXISTS idx_rel_agent_type ON belief_relationships(agent_id, relationship_type);
CREATE INDEX IF NOT EXISTS idx_rel_source_type ON belief_relationships(source_belief_id, relationship_type);
CREATE INDEX IF NOT EXISTS idx_rel_target_type ON belief_relationships(target_belief_id, relationship_type);
CREATE INDEX IF NOT EXISTS idx_rel_source_active ON belief_relationships(source_belief_id, active);
CREATE INDEX IF NOT EXISTS idx_rel_target_active ON belief_relationships(target_belief_id, active);

-- Specialized indexes for deprecation queries
CREATE INDEX IF NOT EXISTS idx_rel_deprecation ON belief_relationships(relationship_type, active, effective_until) 
    WHERE relationship_type IN ('SUPERSEDES', 'UPDATES', 'DEPRECATES', 'REPLACES');

-- Index for high-strength relationships
CREATE INDEX IF NOT EXISTS idx_rel_high_strength ON belief_relationships(agent_id, strength DESC) 
    WHERE strength >= 0.8 AND active = true;

-- Index for currently effective relationships
CREATE INDEX IF NOT EXISTS idx_rel_currently_effective ON belief_relationships(agent_id, relationship_type) 
    WHERE active = true 
    AND (effective_from IS NULL OR effective_from <= CURRENT_TIMESTAMP)
    AND (effective_until IS NULL OR effective_until > CURRENT_TIMESTAMP);

-- Metadata table indexes
CREATE INDEX IF NOT EXISTS idx_rel_metadata_relationship ON belief_relationship_metadata(relationship_id);
CREATE INDEX IF NOT EXISTS idx_rel_metadata_key ON belief_relationship_metadata(metadata_key);
CREATE INDEX IF NOT EXISTS idx_rel_metadata_value ON belief_relationship_metadata(metadata_value) WHERE length(metadata_value) < 100;

-- Unique constraint to prevent duplicate active relationships of the same type
CREATE UNIQUE INDEX IF NOT EXISTS idx_rel_unique_active ON belief_relationships(
    source_belief_id, target_belief_id, relationship_type, agent_id
) WHERE active = true;

-- Function to automatically update last_updated timestamp for relationships
CREATE OR REPLACE FUNCTION update_belief_relationship_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update last_updated on updates
DROP TRIGGER IF EXISTS update_belief_relationships_last_updated ON belief_relationships;
CREATE TRIGGER update_belief_relationships_last_updated 
    BEFORE UPDATE ON belief_relationships 
    FOR EACH ROW 
    EXECUTE FUNCTION update_belief_relationship_timestamp();

-- Function to find deprecated beliefs
CREATE OR REPLACE FUNCTION find_deprecated_beliefs(
    agent_id_param VARCHAR(100) DEFAULT NULL
)
RETURNS TABLE (
    deprecated_belief_id VARCHAR(100),
    superseding_belief_id VARCHAR(100),
    relationship_type VARCHAR(50),
    deprecation_reason TEXT,
    deprecated_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        r.target_belief_id,
        r.source_belief_id,
        r.relationship_type,
        r.deprecation_reason,
        r.created_at
    FROM belief_relationships r
    WHERE r.relationship_type IN ('SUPERSEDES', 'UPDATES', 'DEPRECATES', 'REPLACES')
    AND (agent_id_param IS NULL OR r.agent_id = agent_id_param)
    AND r.active = true
    AND (r.effective_from IS NULL OR r.effective_from <= CURRENT_TIMESTAMP)
    AND (r.effective_until IS NULL OR r.effective_until > CURRENT_TIMESTAMP)
    ORDER BY r.created_at DESC;
END;
$$ LANGUAGE plpgsql;

-- Statistics view for relationship analytics
CREATE OR REPLACE VIEW belief_relationship_statistics AS
SELECT 
    COUNT(*) as total_relationships,
    COUNT(DISTINCT agent_id) as unique_agents,
    COUNT(CASE WHEN active = true THEN 1 END) as active_relationships,
    COUNT(CASE WHEN relationship_type IN ('SUPERSEDES', 'UPDATES', 'DEPRECATES', 'REPLACES') THEN 1 END) as temporal_relationships,
    AVG(strength) as avg_strength,
    MIN(created_at) as oldest_relationship,
    MAX(created_at) as newest_relationship,
    COUNT(DISTINCT relationship_type) as unique_relationship_types
FROM belief_relationships;

-- Agent-specific relationship statistics
CREATE OR REPLACE VIEW agent_relationship_stats AS
SELECT 
    agent_id,
    COUNT(*) as total_relationships,
    COUNT(CASE WHEN active = true THEN 1 END) as active_relationships,
    COUNT(DISTINCT relationship_type) as unique_types,
    AVG(strength) as avg_strength,
    COUNT(CASE WHEN relationship_type IN ('SUPERSEDES', 'UPDATES', 'DEPRECATES', 'REPLACES') THEN 1 END) as deprecation_relationships,
    MIN(created_at) as oldest_relationship,
    MAX(created_at) as newest_relationship
FROM belief_relationships 
GROUP BY agent_id;

-- Relationship type distribution view
CREATE OR REPLACE VIEW relationship_type_distribution AS
SELECT 
    relationship_type,
    COUNT(*) as relationship_count,
    COUNT(DISTINCT agent_id) as agent_count,
    AVG(strength) as avg_strength,
    COUNT(CASE WHEN active = true THEN 1 END) as active_count
FROM belief_relationships 
GROUP BY relationship_type
ORDER BY relationship_count DESC;

-- Comments for documentation
COMMENT ON TABLE memories IS 'Main table storing memory records with vector embeddings for similarity search';
COMMENT ON COLUMN memories.vector_embedding IS 'Vector embedding for semantic similarity search (1536 dimensions for OpenAI embeddings)';
COMMENT ON COLUMN memories.metadata_json IS 'JSONB field for flexible metadata storage';
COMMENT ON FUNCTION similarity_search_hybrid IS 'Hybrid similarity search combining vector and text-based scoring';
COMMENT ON VIEW memory_statistics IS 'Overall statistics about the memory system';
COMMENT ON VIEW agent_memory_stats IS 'Per-agent memory statistics';
COMMENT ON VIEW category_distribution IS 'Distribution of memories across categories';

-- Belief relationship comments
COMMENT ON TABLE belief_relationships IS 'Stores directed relationships between beliefs forming a knowledge graph';
COMMENT ON COLUMN belief_relationships.strength IS 'Confidence or strength of the relationship (0.0 to 1.0)';
COMMENT ON COLUMN belief_relationships.effective_from IS 'When this relationship becomes effective (NULL = immediately)';
COMMENT ON COLUMN belief_relationships.effective_until IS 'When this relationship expires (NULL = never)';
COMMENT ON COLUMN belief_relationships.deprecation_reason IS 'Human-readable reason for deprecation relationships';
COMMENT ON FUNCTION find_deprecated_beliefs IS 'Finds all beliefs that are currently deprecated by temporal relationships';
COMMENT ON VIEW belief_relationship_statistics IS 'Overall statistics about the belief relationship system';
COMMENT ON VIEW agent_relationship_stats IS 'Per-agent relationship statistics and metrics';
COMMENT ON VIEW relationship_type_distribution IS 'Distribution of relationships across different types';