-- Database schema for belief relationships (knowledge graph edges)
-- This schema supports rich relationships between beliefs with temporal constraints,
-- metadata, and comprehensive indexing for graph operations.

-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create belief_relationships table
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
    ),
    
    -- Foreign key constraints (assuming beliefs table exists)
    CONSTRAINT fk_rel_source_belief FOREIGN KEY (source_belief_id) 
        REFERENCES beliefs(id) ON DELETE CASCADE,
    CONSTRAINT fk_rel_target_belief FOREIGN KEY (target_belief_id) 
        REFERENCES beliefs(id) ON DELETE CASCADE
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

-- Performance indexes for graph traversal operations
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

-- Function to automatically update last_updated timestamp
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

-- Function for graph traversal: find all relationships for a belief
CREATE OR REPLACE FUNCTION get_belief_relationships(
    belief_id_param VARCHAR(100),
    agent_id_param VARCHAR(100) DEFAULT NULL,
    include_inactive BOOLEAN DEFAULT FALSE,
    max_depth INTEGER DEFAULT 3
)
RETURNS TABLE (
    relationship_id VARCHAR(100),
    source_belief_id VARCHAR(100),
    target_belief_id VARCHAR(100),
    relationship_type VARCHAR(50),
    strength DECIMAL(5,4),
    is_outgoing BOOLEAN,
    depth INTEGER
) AS $$
BEGIN
    RETURN QUERY
    WITH RECURSIVE belief_graph(rel_id, source_id, target_id, rel_type, rel_strength, is_out, current_depth) AS (
        -- Base case: direct relationships
        SELECT 
            r.id,
            r.source_belief_id,
            r.target_belief_id,
            r.relationship_type,
            r.strength,
            true as is_outgoing,
            1 as depth
        FROM belief_relationships r
        WHERE r.source_belief_id = belief_id_param
        AND (agent_id_param IS NULL OR r.agent_id = agent_id_param)
        AND (include_inactive OR r.active = true)
        AND (r.effective_from IS NULL OR r.effective_from <= CURRENT_TIMESTAMP)
        AND (r.effective_until IS NULL OR r.effective_until > CURRENT_TIMESTAMP)
        
        UNION
        
        SELECT 
            r.id,
            r.source_belief_id,
            r.target_belief_id,
            r.relationship_type,
            r.strength,
            false as is_outgoing,
            1 as depth
        FROM belief_relationships r
        WHERE r.target_belief_id = belief_id_param
        AND (agent_id_param IS NULL OR r.agent_id = agent_id_param)
        AND (include_inactive OR r.active = true)
        AND (r.effective_from IS NULL OR r.effective_from <= CURRENT_TIMESTAMP)
        AND (r.effective_until IS NULL OR r.effective_until > CURRENT_TIMESTAMP)
        
        UNION
        
        -- Recursive case: traverse the graph
        SELECT 
            r.id,
            r.source_belief_id,
            r.target_belief_id,
            r.relationship_type,
            r.strength,
            CASE WHEN r.source_belief_id = bg.target_id THEN true ELSE false END,
            bg.current_depth + 1
        FROM belief_relationships r
        JOIN belief_graph bg ON (
            (r.source_belief_id = bg.target_id AND bg.is_out = true) OR
            (r.target_belief_id = bg.source_id AND bg.is_out = false)
        )
        WHERE bg.current_depth < max_depth
        AND (agent_id_param IS NULL OR r.agent_id = agent_id_param)
        AND (include_inactive OR r.active = true)
        AND (r.effective_from IS NULL OR r.effective_from <= CURRENT_TIMESTAMP)
        AND (r.effective_until IS NULL OR r.effective_until > CURRENT_TIMESTAMP)
    )
    SELECT DISTINCT * FROM belief_graph
    ORDER BY current_depth, rel_strength DESC;
END;
$$ LANGUAGE plpgsql;

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

-- Function to find relationship paths between two beliefs
CREATE OR REPLACE FUNCTION find_relationship_path(
    source_belief_param VARCHAR(100),
    target_belief_param VARCHAR(100),
    agent_id_param VARCHAR(100) DEFAULT NULL,
    max_depth INTEGER DEFAULT 5
)
RETURNS TABLE (
    path_length INTEGER,
    relationship_path TEXT[]
) AS $$
BEGIN
    RETURN QUERY
    WITH RECURSIVE path_search(
        current_belief, 
        target_belief, 
        path, 
        path_length, 
        visited
    ) AS (
        -- Base case
        SELECT 
            source_belief_param,
            target_belief_param,
            ARRAY[]::TEXT[],
            0,
            ARRAY[source_belief_param]
            
        UNION
        
        -- Recursive case
        SELECT 
            r.target_belief_id,
            ps.target_belief,
            ps.path || r.id,
            ps.path_length + 1,
            ps.visited || r.target_belief_id
        FROM path_search ps
        JOIN belief_relationships r ON r.source_belief_id = ps.current_belief
        WHERE ps.current_belief != ps.target_belief
        AND ps.path_length < max_depth
        AND NOT (r.target_belief_id = ANY(ps.visited))
        AND (agent_id_param IS NULL OR r.agent_id = agent_id_param)
        AND r.active = true
        AND (r.effective_from IS NULL OR r.effective_from <= CURRENT_TIMESTAMP)
        AND (r.effective_until IS NULL OR r.effective_until > CURRENT_TIMESTAMP)
    )
    SELECT 
        ps.path_length,
        ps.path
    FROM path_search ps
    WHERE ps.current_belief = ps.target_belief
    AND ps.path_length > 0
    ORDER BY ps.path_length
    LIMIT 1;
END;
$$ LANGUAGE plpgsql;

-- Function for cleanup of old inactive relationships
CREATE OR REPLACE FUNCTION cleanup_belief_relationships(
    days_threshold INTEGER DEFAULT 365,
    agent_id_param VARCHAR(100) DEFAULT NULL
)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM belief_relationships 
    WHERE active = false
    AND last_updated < NOW() - INTERVAL '1 day' * days_threshold
    AND (agent_id_param IS NULL OR agent_id = agent_id_param);
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
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
COMMENT ON TABLE belief_relationships IS 'Stores directed relationships between beliefs forming a knowledge graph';
COMMENT ON COLUMN belief_relationships.strength IS 'Confidence or strength of the relationship (0.0 to 1.0)';
COMMENT ON COLUMN belief_relationships.effective_from IS 'When this relationship becomes effective (NULL = immediately)';
COMMENT ON COLUMN belief_relationships.effective_until IS 'When this relationship expires (NULL = never)';
COMMENT ON COLUMN belief_relationships.deprecation_reason IS 'Human-readable reason for deprecation relationships';
COMMENT ON FUNCTION get_belief_relationships IS 'Recursively finds all relationships connected to a belief within specified depth';
COMMENT ON FUNCTION find_deprecated_beliefs IS 'Finds all beliefs that are currently deprecated by temporal relationships';
COMMENT ON FUNCTION find_relationship_path IS 'Finds the shortest path between two beliefs in the knowledge graph';
COMMENT ON VIEW belief_relationship_statistics IS 'Overall statistics about the belief relationship system';
COMMENT ON VIEW agent_relationship_stats IS 'Per-agent relationship statistics and metrics';
COMMENT ON VIEW relationship_type_distribution IS 'Distribution of relationships across different types';

-- Grant permissions (adjust as needed for your security model)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON belief_relationships TO memory_app_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON belief_relationship_metadata TO memory_app_user;
-- GRANT EXECUTE ON FUNCTION get_belief_relationships TO memory_app_user;
-- GRANT EXECUTE ON FUNCTION find_deprecated_beliefs TO memory_app_user;
-- GRANT EXECUTE ON FUNCTION find_relationship_path TO memory_app_user;
-- GRANT EXECUTE ON FUNCTION cleanup_belief_relationships TO memory_admin_user;