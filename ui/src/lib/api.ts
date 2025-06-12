// API configuration and utility functions for HeadKey Memory System
const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

export const API_ENDPOINTS = {
  // Memory Operations
  MEMORY_INGEST: "/api/v1/memory/ingest",
  MEMORY_DRY_RUN: "/api/v1/memory/dry-run",
  MEMORY_VALIDATE: "/api/v1/memory/validate",
  MEMORY_SEARCH: "/api/v1/memory/search",
  MEMORY_RETRIEVE: "/api/v1/memory/retrieve",

  // System Monitoring
  SYSTEM_HEALTH: "/api/v1/memory/health",
  SYSTEM_HEALTH_COMPREHENSIVE: "/api/v1/system/health",
  SYSTEM_CONFIG: "/api/v1/system/config",
  SYSTEM_DATABASE_CAPABILITIES: "/api/v1/system/database/capabilities",
  SYSTEM_STATISTICS: "/api/v1/system/statistics",

  // Belief Operations
  BELIEF_RELATIONSHIPS_STATISTICS:
    "/api/v1/agents/{agentId}/belief-relationships/statistics",
  BELIEF_RELATIONSHIPS_SNAPSHOT:
    "/api/v1/agents/{agentId}/belief-relationships/snapshot-graph",
  BELIEF_RELATIONSHIPS_KNOWLEDGE_GRAPH:
    "/api/v1/agents/{agentId}/belief-relationships/knowledge-graph",
} as const;

export interface ApiError {
  message: string;
  status: number;
  details?: any;
}

export class ApiException extends Error {
  public status: number;
  public details?: any;

  constructor(message: string, status: number, details?: any) {
    super(message);
    this.name = "ApiException";
    this.status = status;
    this.details = details;
  }
}

// Generic API request function with error handling
async function makeRequest<T>(
  endpoint: string,
  options: RequestInit = {},
): Promise<T> {
  const url = `${API_BASE_URL}${endpoint}`;

  const defaultOptions: RequestInit = {
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
  };

  const requestOptions = { ...defaultOptions, ...options };

  try {
    const response = await fetch(url, requestOptions);

    if (!response.ok) {
      let errorMessage = `Request failed: ${response.status} ${response.statusText}`;
      let errorDetails;

      try {
        const errorBody = await response.text();
        if (errorBody) {
          try {
            errorDetails = JSON.parse(errorBody);
            errorMessage =
              errorDetails.message || errorDetails.error || errorMessage;
          } catch {
            errorMessage = errorBody;
          }
        }
      } catch {
        // Ignore errors when reading response body
      }

      throw new ApiException(errorMessage, response.status, errorDetails);
    }

    const contentType = response.headers.get("content-type");
    if (contentType && contentType.includes("application/json")) {
      return await response.json();
    } else {
      return (await response.text()) as T;
    }
  } catch (error) {
    if (error instanceof ApiException) {
      throw error;
    }

    // Network or other errors
    throw new ApiException(
      error instanceof Error ? error.message : "Unknown error occurred",
      0,
      error,
    );
  }
}

// Memory API functions
export const memoryApi = {
  async ingest(memoryRecord: {
    agent_id: string;
    content: string;
    source: string;
    metadata: Record<string, any>;
  }) {
    return makeRequest<{
      success: boolean;
      memory_id?: string;
      message?: string;
      category?: {
        name: string;
        confidence: number;
        tags: string[];
      };
      relevance_score?: number;
      agent_id?: string;
      encoded?: boolean;
      dry_run?: boolean;
      updated_beliefs?: any[];
      processing_time_ms?: number;
      timestamp?: string;
      belief_update_result?: {
        reinforcedBeliefs: any[];
        conflicts: any[];
        newBeliefs: Array<{
          id: string;
          agentId: string;
          statement: string;
          confidence: number;
          evidenceMemoryIds: string[];
          category: string;
          createdAt: string;
          lastUpdated: string;
          reinforcementCount: number;
          active: boolean;
          tags: string[];
          highConfidence: boolean;
          ageInSeconds: number;
          evidenceCount: number;
        }>;
        weakenedBeliefs: any[];
        agentId: string;
        analysisTimestamp: string;
        processingTimeMs?: number;
        totalBeliefsExamined: number;
        memoriesAnalyzed: number;
        overallConfidence: number;
        summary: string;
        totalBeliefChanges: number;
        highSeverityConflictCount: number;
        unresolvedConflictCount: number;
      };
    }>(API_ENDPOINTS.MEMORY_INGEST, {
      method: "POST",
      body: JSON.stringify(memoryRecord),
    });
  },

  async dryRun(memoryRecord: {
    agent_id: string;
    content: string;
    source?: string;
    metadata?: Record<string, any>;
  }) {
    return makeRequest<{
      success: boolean;
      message?: string;
      category?: {
        name: string;
        confidence: number;
        tags: string[];
      };
      relevance_score?: number;
      agent_id?: string;
      encoded?: boolean;
      dry_run?: boolean;
      processing_time_ms?: number;
      timestamp?: string;
      processing_info?: Record<string, any>;
    }>(API_ENDPOINTS.MEMORY_DRY_RUN, {
      method: "POST",
      body: JSON.stringify(memoryRecord),
    });
  },

  async validate(input: { agent_id: string; content: string }) {
    return makeRequest<{
      valid: boolean;
      errors?: string[];
      warnings?: string[];
      suggestions?: string[];
    }>(API_ENDPOINTS.MEMORY_VALIDATE, {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  async search(query: {
    agent_id: string;
    query: string;
    limit?: number;
    similarity_threshold?: number;
  }) {
    return makeRequest<{
      results: Array<{
        memory_id: string;
        content: string;
        relevance_score: number;
        category: string;
        metadata: Record<string, any>;
        created_at: string;
      }>;
      total_count: number;
    }>(API_ENDPOINTS.MEMORY_SEARCH, {
      method: "POST",
      body: JSON.stringify(query),
    });
  },
};

// Belief API functions
export const beliefApi = {
  async getStatistics(agentId: string) {
    const endpoint = API_ENDPOINTS.BELIEF_RELATIONSHIPS_STATISTICS.replace(
      "{agentId}",
      agentId,
    );
    return makeRequest<BeliefStatistics>(endpoint);
  },

  async getSnapshot(agentId: string, includeInactive = false) {
    const endpoint = API_ENDPOINTS.BELIEF_RELATIONSHIPS_SNAPSHOT.replace(
      "{agentId}",
      agentId,
    );
    const url = includeInactive ? `${endpoint}?includeInactive=true` : endpoint;
    return makeRequest<BeliefSnapshot>(url);
  },

  async getKnowledgeGraph(agentId: string) {
    const endpoint = API_ENDPOINTS.BELIEF_RELATIONSHIPS_KNOWLEDGE_GRAPH.replace(
      "{agentId}",
      agentId,
    );
    return makeRequest<KnowledgeGraph>(endpoint);
  },
};

// System API functions
export const systemApi = {
  async getHealth() {
    return makeRequest<{
      status: "UP" | "DOWN";
      timestamp: string;
      details?: Record<string, any>;
    }>(API_ENDPOINTS.SYSTEM_HEALTH);
  },

  async getComprehensiveHealth() {
    return makeRequest<{
      status: "UP" | "DOWN";
      components: Record<
        string,
        {
          status: "UP" | "DOWN";
          details?: Record<string, any>;
        }
      >;
      timestamp: string;
    }>(API_ENDPOINTS.SYSTEM_HEALTH_COMPREHENSIVE);
  },

  async getConfig() {
    return makeRequest<{
      database: {
        type: string;
        url: string;
        status: string;
      };
      memory_system: {
        encoding_strategy: string;
        similarity_threshold: number;
        max_memories_per_agent: number;
      };
      langchain4j: {
        model_name: string;
        embedding_dimension: number;
      };
    }>(API_ENDPOINTS.SYSTEM_CONFIG);
  },

  async getDatabaseCapabilities() {
    return makeRequest<{
      database_type: string;
      supports_vector_operations: boolean;
      max_connections: number;
      current_connections: number;
      capabilities: string[];
    }>(API_ENDPOINTS.SYSTEM_DATABASE_CAPABILITIES);
  },

  async getStatistics() {
    return makeRequest<SystemStatistics>(API_ENDPOINTS.SYSTEM_STATISTICS);
  },
};

// Error handling utilities
export function handleApiError(error: unknown): ApiError {
  if (error instanceof ApiException) {
    return {
      message: error.message,
      status: error.status,
      details: error.details,
    };
  }

  if (error instanceof Error) {
    return {
      message: error.message,
      status: 0,
    };
  }

  return {
    message: "An unknown error occurred",
    status: 0,
  };
}

// Utility function to check if API is available
export async function checkApiAvailability(): Promise<boolean> {
  try {
    await systemApi.getHealth();
    return true;
  } catch {
    return false;
  }
}

// Response type utilities
export type MemoryRecord = {
  id: string;
  agent_id: string;
  content: string;
  category: string;
  source: string;
  metadata: Record<string, any>;
  relevance_score: number;
  created_at: string;
  updated_at: string;
};

export type SystemHealth = {
  status: "UP" | "DOWN";
  timestamp: string;
  components?: Record<
    string,
    {
      status: "UP" | "DOWN";
      details?: Record<string, any>;
    }
  >;
};

export type SystemStatistics = {
  database: {
    entityManagerFactoryOpen: boolean;
    configuredDatabaseKind: string;
  };
  memorySystem: {
    totalSearches: number;
    totalOperations: number;
    secondLevelCacheEnabled: boolean;
    maxSimilaritySearchResults: number;
    totalMemories: number;
    managedTypes: number;
    totalUpdates: number;
    startTime: string;
    similarityThreshold: number;
    totalDeletes: number;
    batchSize: number;
    uptimeSeconds: number;
  };
  strategy: {
    supportsVectorSearch: boolean;
    name: string;
  };
  timestamp: string;
};

export type BeliefStatistics = {
  totalBeliefs: number;
  activeBeliefs: number;
  inactiveBeliefs: number;
  totalRelationships: number;
  activeRelationships: number;
  beliefsByConfidenceLevel: {
    high: number;
    medium: number;
    low: number;
  };
  relationshipTypes: Record<string, number>;
  agentId: string;
  timestamp: string;
};

export type BeliefSnapshot = {
  beliefs: Array<{
    id: string;
    agentId: string;
    statement: string;
    confidence: number;
    active: boolean;
    createdAt: string;
    lastUpdated: string;
    category: string;
    tags: string[];
  }>;
  relationships: Array<{
    id: string;
    sourceBeliefId: string;
    targetBeliefId: string;
    relationshipType: string;
    strength: number;
    active: boolean;
    createdAt: string;
  }>;
  statistics: {
    totalBeliefs: number;
    totalRelationships: number;
    activeBeliefs: number;
    activeRelationships: number;
  };
  agentId: string;
  timestamp: string;
};

export type KnowledgeGraph = {
  nodes: Array<{
    id: string;
    label: string;
    confidence: number;
    category: string;
    active: boolean;
  }>;
  edges: Array<{
    source: string;
    target: string;
    type: string;
    strength: number;
    active: boolean;
  }>;
  metadata: {
    totalNodes: number;
    totalEdges: number;
    clusters: number;
    agentId: string;
    generatedAt: string;
  };
};
