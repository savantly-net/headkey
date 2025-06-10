# Headkey REST

The Headkey REST module provides a RESTful HTTP API for the Memory System, exposing the core memory management capabilities through standardized endpoints. Built with Quarkus for cloud-native deployment, it offers high-performance, production-ready access to memory ingestion, system monitoring, and configuration management.

## Purpose

The REST module serves as the primary interface layer for the Headkey Memory System, providing:

- **HTTP API Gateway**: RESTful endpoints for memory ingestion and management operations
- **System Monitoring**: Health checks, statistics, and configuration inspection endpoints
- **Multi-Environment Support**: Development, testing, and production configurations
- **AI Integration**: Optional LangChain4J and OpenAI integration for enhanced capabilities
- **Cloud-Native Deployment**: Built on Quarkus for fast startup and low memory usage

## API Overview

### Memory Ingestion Endpoints (`/api/v1/memory`)

#### Core Operations
- **`POST /ingest`** - Store new memories with full processing pipeline
- **`POST /dry-run`** - Preview categorization without storing data  
- **`POST /validate`** - Validate input format and requirements
- **`GET /health`** - Health status of memory ingestion system
- **`GET /statistics`** - Performance metrics and operation counts

#### Example Usage
```bash
# Store a memory
curl -X POST http://localhost:8080/api/v1/memory/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "agent_id": "user-123",
    "content": "I love Italian food and cooking",
    "source": "conversation",
    "metadata": {"importance": "medium"}
  }'

# Preview categorization
curl -X POST http://localhost:8080/api/v1/memory/dry-run \
  -H "Content-Type: application/json" \
  -d '{
    "agent_id": "user-123",
    "content": "Learning about machine learning",
    "source": "educational"
  }'
```

### System Management Endpoints (`/api/v1/system`)

#### Monitoring & Configuration
- **`GET /health`** - Comprehensive system health including database connectivity
- **`GET /config`** - Current system configuration and runtime settings
- **`GET /statistics`** - System-wide performance metrics and usage data
- **`GET /database/capabilities`** - Database feature analysis and recommendations

## Technology Stack

### Core Framework
- **Quarkus** - Cloud-native Java framework for fast startup and low memory usage
- **REST/JAX-RS** - Standard Java REST API with Jackson JSON processing
- **Hibernate ORM** - JPA-based persistence with multiple database support
- **SmallRye OpenAPI** - Automatic API documentation generation

### Database Support
- **H2** - In-memory database for development and testing
- **PostgreSQL** - Production database with vector search capabilities
- **Connection Pooling** - Agroal connection pool for optimal performance

### AI Integration (Optional)
- **LangChain4J** - AI-powered categorization and belief extraction
- **OpenAI Embeddings** - Vector embeddings for semantic similarity search

## Configuration

### Environment Profiles
The system supports multiple deployment profiles with automatic configuration:

#### Development (`%dev`)
- H2 in-memory database
- SQL logging enabled
- Reduced cache sizes for faster startup
- OpenAI integration disabled by default

#### Testing (`%test`)
- Isolated H2 database per test
- Minimal caching for deterministic behavior
- Statistics disabled for performance

#### Production (`%prod`)
- PostgreSQL database
- Optimized connection pooling
- Enhanced caching and performance settings
- Full AI integration enabled

### Key Configuration Properties

#### Memory System Settings
```properties
# Strategy selection (auto, text, vector, postgres)
headkey.memory.strategy=auto

# Performance tuning
headkey.memory.batch-size=100
headkey.memory.max-similarity-results=1000
headkey.memory.enable-second-level-cache=true

# Database configuration  
headkey.memory.database.kind=postgresql
headkey.memory.database.pool.max-size=50
```

#### AI Integration
```properties
# OpenAI configuration
quarkus.langchain4j.openai.api-key=${OPENAI_API_KEY}
quarkus.langchain4j.openai.embedding-model.model-name=text-embedding-3-small

# Embedding settings
headkey.memory.embedding.enabled=true
headkey.memory.embedding.dimension=1536
```

## Quick Start

### 1. Development Mode
```bash
# Start in development mode with hot reload
./gradlew :rest:quarkusDev

# API available at http://localhost:8080
# Swagger UI at http://localhost:8080/swagger-ui
```

### 2. Test the API
```bash
# Run the comprehensive test script
cd rest
./test-api.sh
```

### 3. Production Deployment
```bash
# Build native executable
./gradlew :rest:build -Dquarkus.package.type=native

# Or build JVM application
./gradlew :rest:build

# Deploy with Docker
docker build -f rest/src/main/docker/Dockerfile.jvm .
```

## API Documentation

### Interactive Documentation
- **Swagger UI**: `http://localhost:8080/swagger-ui`
- **OpenAPI Spec**: `http://localhost:8080/openapi`

### Request/Response Format

#### Memory Ingestion Request
```json
{
  "agent_id": "user-123",
  "content": "The information to store",
  "source": "conversation",
  "timestamp": "2023-12-01T10:30:00Z",
  "metadata": {
    "importance": "high",
    "tags": ["learning", "api"],
    "confidence": 0.9
  },
  "dry_run": false
}
```

#### Memory Ingestion Response
```json
{
  "success": true,
  "memory_id": "mem_12345",
  "category": {
    "primary": "knowledge",
    "confidence": 0.85,
    "tags": ["learning", "technology"]
  },
  "beliefs_updated": 2,
  "processing_time_ms": 45
}
```

## Deployment Options

### Local Development
- Embedded H2 database
- Hot reload with Quarkus dev mode
- In-memory memory system
- No external dependencies

### Docker Deployment
```dockerfile
# Example Docker run
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://db:5432/headkey \
  -e OPENAI_API_KEY=your_key_here \
  headkey-rest:latest
```

### Kubernetes Deployment
- Native executable for minimal resource usage
- Health check endpoints for liveness/readiness probes
- ConfigMap support for environment-specific configuration
- Horizontal scaling support

## Monitoring & Operations

### Health Monitoring
- **Application Health**: `/q/health` (Quarkus standard)
- **Memory System Health**: `/api/v1/memory/health`
- **Database Health**: `/api/v1/system/health`

### Metrics & Statistics
- Operation counts and success rates
- Processing time distributions
- Memory usage and capacity metrics
- Database performance indicators

### Troubleshooting
```bash
# Check system health
curl http://localhost:8080/api/v1/system/health

# View configuration
curl http://localhost:8080/api/v1/system/config

# Get performance statistics
curl http://localhost:8080/api/v1/memory/statistics
```

## Security Considerations

### Input Validation
- Bean validation on all request DTOs
- Content length limits (configurable)
- Agent ID format validation
- Metadata sanitization

### CORS Configuration
- Configurable origins for cross-domain access
- Standard headers and methods supported
- Production-ready defaults

### Environment Variables
- Sensitive configuration via environment variables
- No secrets in configuration files
- Database credentials externalized

## Integration Examples

### Curl Examples
```bash
# Health check
curl http://localhost:8080/api/v1/memory/health

# Store memory with metadata
curl -X POST http://localhost:8080/api/v1/memory/ingest \
  -H "Content-Type: application/json" \
  -d '{"agent_id":"test","content":"Hello world","source":"api"}'

# Validate input format
curl -X POST http://localhost:8080/api/v1/memory/validate \
  -H "Content-Type: application/json" \
  -d '{"agent_id":"test","content":"Validation test"}'
```

### JavaScript/TypeScript
```typescript
const response = await fetch('/api/v1/memory/ingest', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    agent_id: 'user-123',
    content: 'Learning TypeScript integration',
    source: 'tutorial'
  })
});
const result = await response.json();
```

### Python
```python
import requests

response = requests.post('http://localhost:8080/api/v1/memory/ingest', 
  json={
    'agent_id': 'user-123',
    'content': 'Python integration test',
    'source': 'script'
  }
)
result = response.json()
```

The REST module provides a robust, scalable interface to the Headkey Memory System with comprehensive monitoring, flexible deployment options, and production-ready features for enterprise use.