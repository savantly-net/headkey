# HeadKey Memory Ingestion REST API

A RESTful API that exposes the HeadKey Information Ingestion Module with **JPA-based persistence**, providing endpoints for memory storage, categorization, and management with configurable database backends.

## Overview

This REST API provides a clean HTTP interface to the HeadKey memory system, allowing clients to:
- Ingest new memories with automatic categorization using pluggable similarity search strategies
- Perform dry-run validation of content
- Monitor system health and performance with JPA memory system statistics
- Validate input data before processing
- Configure database backends (H2, PostgreSQL, etc.) via application properties
- Leverage optimized similarity search strategies based on database capabilities

## ✨ Recent Updates: JPA Memory System Integration

The REST application has been **refactored to use the new JPA Memory System Factory** with the following improvements:

### 🏗️ Architecture Changes
- **JPA-based persistence** instead of in-memory storage
- **Pluggable similarity search strategies** (text-based, vector-based, database-optimized)
- **Configurable database backends** via application properties
- **Automatic strategy detection** based on database capabilities
- **Enhanced monitoring and health checks** for JPA components

### 🔧 Configuration Features
- **H2 in-memory database** for development (default)
- **PostgreSQL support** for production (configurable via environment variables)
- **Strategy selection**: auto, text, vector, postgres
- **Performance tuning**: batch sizes, cache settings, connection pooling
- **Embedding configuration**: dimension, model selection

### 📊 New Endpoints
- `/api/v1/system/health` - Comprehensive JPA system health check
- `/api/v1/system/config` - Current memory system configuration
- `/api/v1/system/database/capabilities` - Database capability analysis
- `/api/v1/system/statistics` - JPA memory system statistics

## Quick Start

### Starting the Application

```bash
cd headkey/rest
../gradlew quarkusDev
```

The API will be available at `http://localhost:8080` with **H2 in-memory database** by default.

### API Documentation

- **Swagger UI**: http://localhost:8080/swagger-ui
- **OpenAPI Spec**: http://localhost:8080/openapi
- **Health Check**: http://localhost:8080/health
- **JPA System Health**: http://localhost:8080/api/v1/system/health
- **System Configuration**: http://localhost:8080/api/v1/system/config

### Database Configuration

#### Development (Default)
```properties
# H2 in-memory database - automatic setup
quarkus.datasource.db-kind=h2
headkey.memory.strategy=auto
```

#### Production (PostgreSQL)
```bash
# Environment variables
export DATABASE_URL=jdbc:postgresql://localhost:5432/headkey
export DATABASE_USER=headkey_user
export DATABASE_PASSWORD=secure_password

# Start with production profile
../gradlew quarkusDev -Dquarkus.profile=prod
```

#### Custom Configuration
```properties
# Application properties
headkey.memory.strategy=postgres
headkey.memory.batch-size=200
headkey.memory.max-similarity-results=2000
headkey.memory.similarity-threshold=0.1
headkey.memory.enable-second-level-cache=true
```

## API Endpoints

### Memory Ingestion (JPA-backed)

#### POST /api/v1/memory/ingest
Ingest a new memory into the system.

**Request Body:**
```json
{
  "agent_id": "user-123",
  "content": "I love programming in Java and building REST APIs",
  "source": "conversation",
  "timestamp": "2023-12-01T10:30:00Z",
  "metadata": {
    "importance": "high",
    "tags": ["programming", "java"],
    "confidence": 0.95
  },
  "dry_run": false
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "memory_id": "mem-456789",
  "agent_id": "user-123",
  "category": {
    "name": "technical_knowledge",
    "confidence": 0.92,
    "tags": ["programming", "development"]
  },
  "encoded": true,
  "updated_beliefs": ["belief-1", "belief-2"],
  "processing_time_ms": 145,
  "timestamp": "2023-12-01T10:30:01.234Z"
}
```

#### POST /api/v1/memory/dry-run
Perform a dry run of memory ingestion without storing data.

**Request Body:** Same as `/ingest`

**Response (200 OK):**
```json
{
  "success": true,
  "dry_run": true,
  "category": {
    "name": "technical_knowledge",
    "confidence": 0.92,
    "tags": ["programming", "development"]
  },
  "encoded": false,
  "preview_data": {
    "category_confidence": 0.92,
    "estimated_storage_size": "245 bytes"
  },
  "processing_time_ms": 89,
  "timestamp": "2023-12-01T10:30:01.234Z"
}
```

### System Management

#### GET /api/v1/memory/health
Check memory ingestion system health status.

**Response (200 OK):**
```json
{
  "healthy": true,
  "status": "UP",
  "service": "memory-ingestion",
  "timestamp": "2023-12-01T10:30:00Z"
}
```

#### GET /api/v1/system/health
**NEW** - Comprehensive JPA memory system health check.

**Response (200 OK):**
```json
{
  "healthy": true,
  "status": "UP",
  "service": "jpa-memory-system",
  "memorySystem": {
    "healthy": true,
    "strategy": "Text-based JPA Similarity Search",
    "supportsVectorSearch": false
  },
  "database": {
    "healthy": true,
    "entityManagerFactoryOpen": true,
    "configuredKind": "h2"
  },
  "similarityStrategy": {
    "healthy": true,
    "name": "Text-based JPA Similarity Search",
    "configured": "auto"
  },
  "timestamp": "2023-12-01T10:30:00Z"
}
```

#### GET /api/v1/system/config
**NEW** - Get current JPA memory system configuration.

**Response (200 OK):**
```json
{
  "memory": {
    "strategy": "auto",
    "batchSize": 100,
    "maxSimilarityResults": 1000,
    "similarityThreshold": 0.0,
    "enableSecondLevelCache": false
  },
  "database": {
    "kind": "h2",
    "autoCreateSchema": true,
    "pool": {
      "minSize": 5,
      "maxSize": 20,
      "timeoutMs": 30000
    }
  },
  "embedding": {
    "enabled": true,
    "dimension": 256,
    "model": "default"
  },
  "runtime": {
    "actualStrategy": "Text-based JPA Similarity Search",
    "supportsVectorSearch": false,
    "actualBatchSize": 100,
    "actualMaxSimilarityResults": 1000
  }
}
```

#### GET /api/v1/system/database/capabilities
**NEW** - Analyze database capabilities for similarity search.

**Response (200 OK):**
```json
{
  "databaseType": "H2",
  "version": "H2 2.1.214",
  "hasVectorSupport": false,
  "hasFullTextSupport": true,
  "recommendedStrategy": "Text-based JPA Similarity Search",
  "currentStrategy": "Text-based JPA Similarity Search",
  "strategyOptimal": true
}
```

#### GET /api/v1/memory/statistics
Get ingestion statistics and JPA memory system metrics.

**Response (200 OK):**
```json
{
  "totalIngestions": 1234,
  "totalDryRuns": 89,
  "totalValidations": 1456,
  "totalErrors": 12,
  "uptime": 3600000,
  "memorySystem": {
    "totalMemories": 1234,
    "implementationType": "JpaMemoryEncodingSystem",
    "totalOperations": 1500,
    "totalSearches": 234,
    "totalUpdates": 45,
    "totalDeletes": 12
  },
  "similarityStrategy": "Text-based JPA Similarity Search",
  "supportsVectorSearch": false,
  "batchSize": 100,
  "maxSimilarityResults": 1000,
  "similarityThreshold": 0.0,
  "secondLevelCacheEnabled": false
}
```

#### GET /api/v1/system/statistics
**NEW** - Comprehensive JPA memory system statistics.

**Response (200 OK):**
```json
{
  "memorySystem": {
    "totalMemories": 1234,
    "implementationType": "JpaMemoryEncodingSystem",
    "totalOperations": 1500,
    "uptime": 3600000
  },
  "strategy": {
    "name": "Text-based JPA Similarity Search",
    "supportsVectorSearch": false
  },
  "database": {
    "entityManagerFactoryOpen": true,
    "configuredDatabaseKind": "h2"
  },
  "timestamp": "2023-12-01T10:30:00Z"
}
```

#### POST /api/v1/memory/validate
Validate input data without processing.

**Request Body:** Same as `/ingest`

**Response (200 OK):**
```json
{
  "valid": true,
  "message": "Input is valid"
}
```

## Request Schema

### MemoryIngestionRequest

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `agent_id` | string | Yes | Agent identifier (1-100 chars) |
| `content` | string | Yes | Memory content (1-10000 chars) |
| `source` | string | No | Source type (e.g., "conversation") |
| `timestamp` | ISO 8601 | No | When the memory occurred |
| `metadata` | object | No | Additional contextual data |
| `dry_run` | boolean | No | Preview mode flag (default: false) |

### Metadata Structure

The `metadata` field accepts any JSON object with additional context:

```json
{
  "importance": "high|medium|low",
  "tags": ["tag1", "tag2"],
  "confidence": 0.95,
  "category_hint": "reminder",
  "priority": 9,
  "custom_field": "any value"
}
```

## Response Schema

### MemoryIngestionResponse

| Field | Type | Description |
|-------|------|-------------|
| `success` | boolean | Operation success status |
| `memory_id` | string | Generated memory ID (if stored) |
| `agent_id` | string | Associated agent ID |
| `category` | object | Assigned category information |
| `encoded` | boolean | Whether memory was stored |
| `dry_run` | boolean | Whether this was a preview |
| `updated_beliefs` | array | IDs of updated beliefs |
| `processing_time_ms` | number | Processing duration |
| `timestamp` | ISO 8601 | Response timestamp |
| `error_message` | string | Error description (if failed) |
| `error_details` | object | Additional error context |

## Error Handling

### HTTP Status Codes

- `200 OK` - Successful operation
- `201 Created` - Memory successfully ingested
- `400 Bad Request` - Invalid input data
- `405 Method Not Allowed` - Unsupported HTTP method
- `500 Internal Server Error` - System error
- `503 Service Unavailable` - System unhealthy

### Error Response Format

```json
{
  "success": false,
  "error_message": "Validation failed: Content cannot be empty",
  "error_details": {
    "validation_errors": [
      "Agent ID is required and cannot be empty",
      "Content is required and cannot be empty"
    ],
    "error_type": "VALIDATION_ERROR"
  },
  "timestamp": "2023-12-01T10:30:00Z"
}
```

## Examples

### cURL Examples

```bash
# Basic memory ingestion
curl -X POST http://localhost:8080/api/v1/memory/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "agent_id": "user-123",
    "content": "Remember to buy groceries tomorrow",
    "source": "user_input"
  }'

# Dry run with metadata
curl -X POST http://localhost:8080/api/v1/memory/dry-run \
  -H "Content-Type: application/json" \
  -d '{
    "agent_id": "user-123",
    "content": "Meeting with client at 3 PM",
    "source": "calendar",
    "metadata": {
      "importance": "high",
      "tags": ["meeting", "work"]
    }
  }'

# Health check
curl http://localhost:8080/api/v1/memory/health

# Get statistics
curl http://localhost:8080/api/v1/memory/statistics
```

### JavaScript/Node.js Example

```javascript
const axios = require('axios');

async function ingestMemory(agentId, content, options = {}) {
  try {
    const response = await axios.post('http://localhost:8080/api/v1/memory/ingest', {
      agent_id: agentId,
      content: content,
      source: options.source || 'api',
      metadata: options.metadata,
      dry_run: options.dryRun || false
    });
    
    return response.data;
  } catch (error) {
    console.error('Memory ingestion failed:', error.response?.data || error.message);
    throw error;
  }
}

// Usage
ingestMemory('user-456', 'I completed the project successfully', {
  source: 'achievement',
  metadata: {
    importance: 'high',
    tags: ['project', 'success'],
    confidence: 1.0
  }
}).then(result => {
  console.log('Memory stored:', result.memory_id);
}).catch(error => {
  console.error('Failed to store memory');
});
```

### Python Example

```python
import requests
import json
from datetime import datetime

def ingest_memory(agent_id, content, source=None, metadata=None, dry_run=False):
    url = "http://localhost:8080/api/v1/memory/ingest"
    
    payload = {
        "agent_id": agent_id,
        "content": content,
        "dry_run": dry_run
    }
    
    if source:
        payload["source"] = source
    if metadata:
        payload["metadata"] = metadata
        
    headers = {"Content-Type": "application/json"}
    
    try:
        response = requests.post(url, json=payload, headers=headers)
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        print(f"Error ingesting memory: {e}")
        if hasattr(e, 'response') and e.response:
            print(f"Response: {e.response.text}")
        raise

# Usage
result = ingest_memory(
    agent_id="python-user",
    content="Learned how to use the HeadKey API",
    source="tutorial",
    metadata={
        "skill_level": "beginner",
        "topic": "api_usage",
        "confidence": 0.8
    }
)

print(f"Memory ID: {result['memory_id']}")
print(f"Category: {result['category']['name']}")
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `QUARKUS_HTTP_PORT` | 8080 | HTTP port |
| `QUARKUS_LOG_LEVEL` | INFO | Logging level |
| `QUARKUS_HTTP_CORS` | true | Enable CORS |
| `DATABASE_URL` | (H2 in-memory) | Production database URL |
| `DATABASE_USER` | sa | Database username |
| `DATABASE_PASSWORD` | (empty) | Database password |
| `HEADKEY_MEMORY_STRATEGY` | auto | Similarity search strategy |

### JPA Memory System Properties

| Property | Default | Description |
|----------|---------|-------------|
| `headkey.memory.strategy` | auto | Similarity search strategy (auto, text, vector, postgres) |
| `headkey.memory.batch-size` | 100 | Batch size for JPA operations |
| `headkey.memory.max-similarity-results` | 1000 | Maximum similarity search results |
| `headkey.memory.similarity-threshold` | 0.0 | Minimum similarity score (0.0-1.0) |
| `headkey.memory.enable-second-level-cache` | true | Enable JPA second-level cache |

### Database Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `headkey.memory.database.kind` | h2 | Database type (h2, postgresql) |
| `headkey.memory.database.auto-create-schema` | true | Auto-create database schema |
| `headkey.memory.database.pool.min-size` | 5 | Minimum connection pool size |
| `headkey.memory.database.pool.max-size` | 20 | Maximum connection pool size |
| `headkey.memory.database.pool.timeout-ms` | 30000 | Connection timeout (milliseconds) |

### Embedding Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `headkey.memory.embedding.enabled` | true | Enable vector embedding generation |
| `headkey.memory.embedding.dimension` | 384 | Embedding vector dimension |
| `headkey.memory.embedding.model` | default | Embedding model/strategy |

### Performance Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `headkey.memory.performance.enable-statistics` | true | Enable performance statistics |
| `headkey.memory.performance.cache-size` | 1000 | Memory cache size |
| `headkey.memory.performance.enable-async` | false | Enable async processing |

### Profile-based Configuration

#### Development Profile
```properties
%dev.quarkus.datasource.db-kind=h2
%dev.quarkus.datasource.jdbc.url=jdbc:h2:mem:headkey;DB_CLOSE_DELAY=-1
%dev.headkey.memory.enable-second-level-cache=false
%dev.headkey.memory.max-similarity-results=500
%dev.headkey.memory.embedding.dimension=256
```

#### Test Profile
```properties
%test.quarkus.datasource.db-kind=h2
%test.quarkus.datasource.jdbc.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1
%test.headkey.memory.max-similarity-results=100
%test.headkey.memory.embedding.dimension=128
%test.headkey.memory.performance.enable-statistics=false
```

#### Production Profile
```properties
%prod.quarkus.datasource.db-kind=postgresql
%prod.quarkus.datasource.jdbc.url=${DATABASE_URL}
%prod.headkey.memory.database.pool.min-size=10
%prod.headkey.memory.database.pool.max-size=50
%prod.headkey.memory.embedding.dimension=512
%prod.headkey.memory.performance.cache-size=2000
```

### Application Properties

See `src/main/resources/application.properties` for full configuration options.

## Development

### Running Tests

```bash
# Unit tests
../gradlew test

# Integration tests
../gradlew integrationTest

# All tests with coverage
../gradlew test jacocoTestReport
```

### Building

```bash
# Development build
../gradlew build

# Production build
../gradlew build -Dquarkus.package.type=uber-jar
```

### Docker

```bash
# Build container
docker build -t headkey-rest-api .

# Run container
docker run -p 8080:8080 headkey-rest-api
```

## Architecture

This REST API follows clean architecture principles with **JPA-based persistence**:

### Layered Architecture
- **Controller Layer**: HTTP request/response handling (JAX-RS)
- **Service Layer**: Business logic and validation
- **Persistence Layer**: JPA-based memory encoding system
- **Strategy Layer**: Pluggable similarity search strategies
- **DTO Layer**: Data transfer objects for API contracts

### Key Components

#### JPA Memory System
- **JpaMemoryEncodingSystem**: Core persistence using JPA/Hibernate
- **JpaMemorySystemFactory**: Factory for creating configured memory systems
- **Similarity Search Strategies**: Pluggable algorithms (text-based, vector-based, database-optimized)
- **EntityManagerFactory**: JPA persistence unit management

#### Configuration Management
- **MemorySystemProperties**: Type-safe configuration mapping
- **Profile-based Configuration**: Development, test, production profiles
- **Environment Variable Support**: 12-factor app compliance

#### Monitoring & Health
- **Health Check Endpoints**: Comprehensive system health monitoring
- **Statistics Collection**: Performance metrics and usage analytics
- **Database Capability Analysis**: Automatic database feature detection

### Design Patterns
- **Strategy Pattern**: Pluggable similarity search algorithms
- **Factory Pattern**: JPA memory system creation and configuration
- **Dependency Injection (CDI)**: Component lifecycle management
- **Adapter Pattern**: InformationIngestionModule integration
- **Builder Pattern**: Complex configuration assembly
- **Configuration Mapping**: Type-safe property binding

### Database Strategy Selection

The system automatically selects optimal similarity search strategies:

```
Database Type → Strategy Selection
├── PostgreSQL → PostgresJpaSimilaritySearchStrategy (with pgvector support)
├── H2/HSQLDB → TextBasedJpaSimilaritySearchStrategy (LIKE queries)
├── MySQL → TextBasedJpaSimilaritySearchStrategy (text-based search)
└── Unknown → DefaultJpaSimilaritySearchStrategy (fallback)
```

## Performance Considerations

### JPA Optimizations
- **Connection Pooling**: Agroal connection pool with configurable sizing
- **Second-Level Cache**: Hibernate caching for frequently accessed entities
- **Batch Processing**: Configurable batch sizes for bulk operations
- **Lazy Loading**: Optimized entity loading strategies
- **Query Optimization**: Hibernate query plan caching

### Similarity Search Performance
- **Database-Specific Strategies**: Optimized for each database type
- **Vector Search**: Native database vector operations (PostgreSQL + pgvector)
- **Text Search**: Efficient LIKE queries with indexing
- **Result Limiting**: Configurable maximum results to prevent memory issues
- **Threshold Filtering**: Similarity score filtering for relevance

### REST API Performance
- All endpoints are designed to be stateless
- Memory ingestion is synchronous but optimized for low latency
- Health checks are lightweight and cached
- Statistics are computed efficiently with minimal database impact
- Dry runs avoid expensive storage operations
- Connection reuse through JPA persistence context

### Monitoring & Tuning
- **Performance Statistics**: Built-in metrics collection
- **Query Logging**: Configurable SQL logging for optimization
- **Health Metrics**: Database connection and performance monitoring
- **Configuration Tuning**: Profile-based performance settings

## Security

- CORS is configured for cross-origin requests
- Input validation prevents injection attacks
- Error messages avoid sensitive information leakage
- All endpoints support standard HTTP security headers

## Monitoring

The API provides comprehensive monitoring for the JPA memory system:

### Health Monitoring
- `/health` - Basic Quarkus health check
- `/api/v1/memory/health` - Memory ingestion system health
- `/api/v1/system/health` - **NEW** - Comprehensive JPA system health

### Performance Monitoring
- `/api/v1/memory/statistics` - Memory ingestion performance metrics
- `/api/v1/system/statistics` - **NEW** - JPA memory system statistics

### Configuration Monitoring
- `/api/v1/system/config` - **NEW** - Current system configuration
- `/api/v1/system/database/capabilities` - **NEW** - Database capability analysis

### Documentation
- `/openapi` - API specification
- `/swagger-ui` - Interactive documentation

### Metrics Available

#### JPA Memory System Metrics
- Total operations, searches, updates, deletes
- Memory encoding system uptime and health
- Similarity search strategy information
- Database connection status
- Second-level cache statistics

#### Database Metrics
- Connection pool utilization
- Query execution times
- Entity manager factory status
- Database type and version information
- Vector/full-text search capabilities

#### Performance Metrics
- Average ingestion processing time
- Success/error rates
- Memory usage and cache efficiency
- Batch operation performance

## Support

For issues, questions, or contributions, please refer to the main HeadKey project documentation.