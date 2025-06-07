# HeadKey Memory Ingestion REST API

A RESTful API that exposes the HeadKey Information Ingestion Module, providing endpoints for memory storage, categorization, and management.

## Overview

This REST API provides a clean HTTP interface to the HeadKey memory system, allowing clients to:
- Ingest new memories with automatic categorization
- Perform dry-run validation of content
- Monitor system health and performance
- Validate input data before processing

## Quick Start

### Starting the Application

```bash
cd headkey/rest
../gradlew quarkusDev
```

The API will be available at `http://localhost:8080`

### API Documentation

- **Swagger UI**: http://localhost:8080/swagger-ui
- **OpenAPI Spec**: http://localhost:8080/openapi
- **Health Check**: http://localhost:8080/health

## API Endpoints

### Memory Ingestion

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
Check system health status.

**Response (200 OK):**
```json
{
  "healthy": true,
  "status": "UP",
  "service": "memory-ingestion",
  "timestamp": "2023-12-01T10:30:00Z"
}
```

#### GET /api/v1/memory/statistics
Get ingestion statistics and metrics.

**Response (200 OK):**
```json
{
  "total_memories_ingested": 1234,
  "success_rate": 0.98,
  "average_processing_time_ms": 125.5,
  "most_common_category": "personal",
  "error_rate": 0.02
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

This REST API follows clean architecture principles:

- **Controller Layer**: HTTP request/response handling
- **Service Layer**: Business logic and validation
- **Integration Layer**: Memory system integration
- **DTO Layer**: Data transfer objects for API contracts

Key design patterns:
- Dependency Injection (CDI)
- Factory Pattern (Memory System Factory)
- Mapper Pattern (DTO conversion)
- Builder Pattern (Configuration)

## Performance Considerations

- All endpoints are designed to be stateless
- Memory ingestion is synchronous but optimized for low latency
- Health checks are lightweight and cached
- Statistics are computed efficiently
- Dry runs avoid expensive storage operations

## Security

- CORS is configured for cross-origin requests
- Input validation prevents injection attacks
- Error messages avoid sensitive information leakage
- All endpoints support standard HTTP security headers

## Monitoring

The API provides several monitoring endpoints:

- `/health` - Basic health check
- `/api/v1/memory/health` - Detailed system health
- `/api/v1/memory/statistics` - Performance metrics
- `/openapi` - API specification
- `/swagger-ui` - Interactive documentation

## Support

For issues, questions, or contributions, please refer to the main HeadKey project documentation.