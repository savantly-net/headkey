# Headkey Memory System

**Intelligent Memory Management for AI Agents**

Headkey is a sophisticated memory management system built on the **Cognitive Ingestion & Belief Formation Engine (CIBFE)** architecture. It provides AI agents with the ability to intelligently store, categorize, retrieve, and selectively forget information while maintaining coherent belief systems.

## What is Headkey?

Headkey solves a critical challenge in AI systems: **autonomous memory management**. Without intelligent memory management, AI agents either become overloaded with irrelevant data or lose important contextual information over time. Headkey provides:

### Core Capabilities
- **🧠 Intelligent Ingestion**: Automatic categorization and semantic encoding of information
- **🔍 Advanced Search**: Multi-modal search with semantic similarity and contextual relevance
- **⚖️ Belief Management**: Maintains coherent knowledge with conflict detection and resolution
- **🗑️ Selective Forgetting**: Prunes irrelevant data while preserving crucial information
- **🏗️ Modular Architecture**: Six specialized modules following SOLID principles
- **🚀 Production Ready**: Built with Quarkus for cloud-native deployment

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        CIBFE Architecture                       │
├─────────────────┬───────────────┬───────────────┬─────────────────┤
│ Information     │ Contextual    │ Memory        │ Belief          │
│ Ingestion       │ Categorization│ Encoding      │ Reinforcement & │
│ Module (IIM)    │ Engine (CCE)  │ System (MES)  │ Conflict (BRCA) │
├─────────────────┼───────────────┼───────────────┼─────────────────┤
│ Relevance       │ Retrieval &   │               │                 │
│ Evaluation &    │ Response      │               │                 │
│ Forgetting (REFA)│ Engine (RRE) │               │                 │
└─────────────────┴───────────────┴───────────────┴─────────────────┘
                              │
                    ┌─────────────────┐
                    │   REST API      │
                    │  (Quarkus)      │
                    └─────────────────┘
```

## Project Structure

The Headkey system is organized into specialized modules:

- **`/api`** - Interface definitions and contracts for all memory operations
- **`/core`** - Reference implementations with in-memory storage and pattern-based processing
- **`/rest`** - RESTful HTTP API built with Quarkus for cloud-native deployment
- **`/persistence-postgres`** - JPA-based persistence layer with PostgreSQL optimization
- **`/langchain4j`** - AI-powered components using LangChain4J and OpenAI integration
- **`/ui`** - Web interface for system management and monitoring
- **`/docs`** - Comprehensive documentation including original specifications

## Quick Start

### Prerequisites
- Java 21 or higher
- Docker (optional, for PostgreSQL)
- OpenAI API key (optional, for AI features)

### Development Mode

```bash
# Start with a postgresql and the rest api server
make run

# Or start development server manually
./gradlew :rest:quarkusDev
```

Your API will be available at:
- **REST API**: http://localhost:8080/api/v1/memory
- **Swagger UI**: http://localhost:8080/swagger-ui
- **Dev UI**: http://localhost:8080/q/dev

### Test the System

```bash
# Run comprehensive API tests
cd rest
./test-api.sh

# Store a memory via API
curl -X POST http://localhost:8080/api/v1/memory/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "agent_id": "user-123",
    "content": "I love Italian food and cooking",
    "source": "conversation"
  }'
```

## Makefile Targets

The project includes a comprehensive Makefile for common development and deployment tasks:

### Development Commands
```bash
make help              # Show all available targets
make run               # Start the application with database
make compile           # Compile all projects
make test              # Run all tests across all modules
make test-debug        # Run tests with debug mode enabled
make test-quarkus      # Run Quarkus-specific integration tests
```

### Build and Release
```bash
make publish-local     # Build and publish to local Maven repository
make publish           # Build and publish release artifacts
make release           # Create and push a new release tag
```

### Release Process
The Makefile automates semantic versioning:
- Reads current version from `VERSION` file
- Creates release tags with proper version bumping
- Ensures git repository is clean before releases
- Automatically prepares next development version

Example release workflow:
```bash
# Ensure clean repo and create release
make release
# This will:
# 1. Check git repo is pristine
# 2. Create version tag (e.g., 1.0.0)
# 3. Bump to next snapshot (e.g., 1.0.1-SNAPSHOT)
# 4. Push changes and tags to origin
```

## Technology Stack

### Core Framework
- **Quarkus** - Cloud-native Java framework for fast startup and low memory usage
- **Hibernate ORM** - JPA-based persistence with support for H2 and PostgreSQL
- **Jackson** - JSON processing with customizable serialization
- **SmallRye OpenAPI** - Automatic API documentation generation

### AI Integration
- **LangChain4J** - Modern Java framework for AI/LLM integration
- **OpenAI** - Vector embeddings and language model capabilities
- **Vector Search** - Semantic similarity search with configurable strategies

### Database Support
- **H2** - In-memory database for development and testing
- **PostgreSQL** - Production database with vector search extensions
- **Automatic Strategy Selection** - Optimal search strategy based on database capabilities

## Configuration Profiles

### Development (`%dev`)
- H2 in-memory database
- Fast startup with minimal dependencies
- Debug logging enabled
- OpenAPI documentation included

### Testing (`%test`)
- Isolated test databases
- Deterministic behavior for CI/CD
- Minimal resource usage

### Production (`%prod`)
- PostgreSQL with connection pooling
- Vector embeddings enabled
- Performance optimizations
- Comprehensive monitoring

## API Examples

### Store Information
```bash
curl -X POST http://localhost:8080/api/v1/memory/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "agent_id": "user-123",
    "content": "Meeting with Sarah tomorrow at 2 PM",
    "source": "calendar",
    "metadata": {"importance": "high"}
  }'
```

### Search Memories
```bash
curl -X GET "http://localhost:8080/api/v1/memory/search?query=meeting&agent_id=user-123"
```

### System Health
```bash
curl http://localhost:8080/api/v1/memory/health
curl http://localhost:8080/api/v1/system/config
```

## Deployment Options

### Local Development
```bash
make run  # Starts with Docker Compose for database
```

### Docker Deployment
```bash
# Build JVM container
./gradlew :rest:build
docker build -f rest/src/main/docker/Dockerfile.jvm -t headkey:latest .

# Build native container (minimal footprint)
./gradlew :rest:build -Dquarkus.package.type=native
docker build -f rest/src/main/docker/Dockerfile.native -t headkey:native .
```

### Kubernetes
```yaml
# Example deployment with ConfigMap
apiVersion: apps/v1
kind: Deployment
metadata:
  name: headkey
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: headkey
        image: headkey:latest
        env:
        - name: DATABASE_URL
          value: "jdbc:postgresql://postgres:5432/headkey"
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: openai-secret
              key: api-key
```

## Monitoring and Observability

### Health Endpoints
- **Application Health**: `/q/health`
- **Memory System Health**: `/api/v1/memory/health`
- **System Status**: `/api/v1/system/health`

### Metrics and Statistics
- Operation counts and success rates
- Processing time distributions
- Memory usage and capacity metrics
- Database performance indicators

### Configuration Inspection
```bash
# View current configuration
curl http://localhost:8080/api/v1/system/config

# Check database capabilities
curl http://localhost:8080/api/v1/system/database/capabilities

# Get performance statistics
curl http://localhost:8080/api/v1/memory/statistics
```

## Documentation

- **[API Reference](api/README.md)** - Interface definitions and contracts
- **[Core Implementation](core/README.md)** - Reference implementations and patterns
- **[REST API](rest/README.md)** - HTTP endpoints and deployment guide
- **[Developer Cheat Sheet](HEADKEY_CHEAT_SHEET.md)** - Quick reference for common tasks
- **[Original Specification](docs/ORIGINAL_SPECIFICATION.md)** - Complete system design document

## Use Cases

### AI Agent Memory
- **Conversational AI**: Maintain context across long conversations
- **Personal Assistants**: Remember user preferences and adapt to changes
- **Knowledge Workers**: Accumulate expertise while pruning outdated information

### Enterprise Applications
- **Document Processing**: Automatically categorize and index organizational knowledge
- **Expert Systems**: Build and maintain knowledge bases with conflict detection
- **Content Curation**: Intelligently filter and organize information streams

### Research and Analytics
- **Information Synthesis**: Combine multiple sources while detecting contradictions
- **Trend Analysis**: Track evolving knowledge and belief changes over time
- **Quality Assurance**: Identify inconsistencies and gaps in knowledge bases

## Getting Help

- **Documentation**: Check module-specific README files
- **API Documentation**: Visit `/swagger-ui` when running locally
- **Health Checks**: Use `/api/v1/memory/health` for system status
- **Configuration**: Review `/api/v1/system/config` for current settings

## Contributing

The project follows SOLID principles and 12-factor app methodology:

1. **Interface-driven design** - All functionality defined through interfaces
2. **Pluggable implementations** - Easy to swap components and strategies
3. **Comprehensive testing** - Unit, integration, and performance tests
4. **Cloud-native architecture** - Built for containerized deployment
5. **Observability first** - Extensive monitoring and health checks

---

**Version**: `1.0.0-SNAPSHOT`  
**Framework**: Quarkus (Supersonic Subatomic Java)  
**License**: [Project License]

For more information about Quarkus, visit: https://quarkus.io/