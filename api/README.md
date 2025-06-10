# Headkey Memory API

The Headkey Memory API is a sophisticated cognitive memory management system built on the **Cognitive Ingestion & Belief Formation Engine (CIBFE)** architecture. This API provides intelligent memory management capabilities for AI agents, enabling them to store, categorize, encode, retrieve, and selectively forget information while maintaining coherent belief systems.

## Purpose

The Memory API addresses the critical challenge of autonomous memory management in AI systems. Without intelligent memory management, AI agents either become overloaded with irrelevant data or lose important contextual information over time. This system provides:

- **Intelligent Information Processing**: Automated categorization and semantic encoding of incoming data
- **Belief System Management**: Maintenance of coherent knowledge representations with conflict detection and resolution
- **Selective Forgetting**: Pruning of irrelevant or outdated information while preserving crucial knowledge
- **Contextual Retrieval**: Efficient search and retrieval of relevant memories based on semantic similarity and contextual relevance

## Architecture Overview

The CIBFE architecture consists of six primary modules, each with well-defined interfaces following SOLID principles and 12-factor app methodology:

### Core Modules

1. **Information Ingestion Module (IIM)** - `InformationIngestionModule`
   - Orchestrates the complete ingestion pipeline
   - Validates input data and coordinates downstream processing
   - Entry point for all new information into the system

2. **Contextual Categorization Engine (CCE)** - `ContextualCategorizationEngine`
   - Classifies content into contextual categories and topics
   - Extracts semantic tags and entities from content
   - Supports multiple categorization strategies (ML models, rules-based, ontologies)

3. **Memory Encoding System (MES)** - `MemoryEncodingSystem`
   - Encodes and persists information into long-term storage
   - Generates vector embeddings for semantic search
   - Abstracts underlying storage implementations (SQL, NoSQL, vector databases)

4. **Belief Reinforcement & Conflict Analyzer (BRCA)** - `BeliefReinforcementConflictAnalyzer`
   - Maintains coherent belief systems derived from memories
   - Detects and resolves conflicts between new and existing beliefs
   - Reinforces beliefs with consistent evidence

5. **Relevance Evaluation & Forgetting Agent (REFA)** - `RelevanceEvaluationForgettingAgent`
   - Evaluates memory relevance using multiple factors (recency, frequency, importance)
   - Implements configurable forgetting strategies
   - Prevents memory overload while preserving crucial information

6. **Retrieval & Response Engine (RRE)** - `RetrievalResponseEngine`
   - Provides multi-modal search capabilities (full-text, semantic similarity)
   - Supports contextual filtering and relevance ranking
   - Optional response composition from retrieved memories

## Data Models

### Core DTOs

- **`MemoryInput`** - Input data for memory ingestion with agent context and metadata
- **`MemoryRecord`** - Complete memory representation with categorization and relevance scoring
- **`CategoryLabel`** - Classification result with primary/secondary categories and confidence scores
- **`Belief`** - Distilled knowledge representation with evidence links and confidence tracking
- **`Metadata`** - Extensible metadata container for additional context and annotations

### Supporting DTOs

- **`IngestionResult`** - Result of memory ingestion operations
- **`BeliefUpdateResult`** - Outcome of belief analysis and updates
- **`BeliefConflict`** - Detected conflicts between beliefs with resolution information
- **`ForgettingStrategy`** - Configuration for forgetting operations
- **`ForgettingReport`** - Summary of forgetting operation results
- **`FilterOptions`** - Search and retrieval filtering criteria

## Extensibility

The API is designed for maximum extensibility through:

### Interface-Based Architecture
All core functionality is defined through Java interfaces, enabling:
- Pluggable implementations for different algorithms and storage backends
- Easy testing with mock implementations
- Runtime strategy switching for categorization, storage, and forgetting

### Configurable Strategies
- **Categorization**: Support for ML models, rule-based systems, and hybrid approaches
- **Storage**: Abstracted storage layer supporting SQL, NoSQL, and vector databases
- **Forgetting**: Multiple strategies including age-based, frequency-based, and relevance-based
- **Search**: Multi-modal search with configurable weights and algorithms

### Metadata Extension
The `Metadata` system allows arbitrary key-value annotations, supporting:
- Domain-specific tagging and classification
- External system integration markers
- Custom importance and priority indicators
- Application-specific context information

## Use Cases

### AI Agent Memory Management
- **Conversational AI**: Maintain context across long conversations while forgetting irrelevant details
- **Personal Assistants**: Remember user preferences and habits while adapting to changes
- **Knowledge Workers**: Accumulate domain expertise while pruning outdated information

### Enterprise Knowledge Management
- **Document Processing**: Automatically categorize and index organizational knowledge
- **Expert Systems**: Build and maintain knowledge bases with conflict detection
- **Content Curation**: Intelligently filter and organize information streams

### Research and Analytics
- **Information Synthesis**: Combine multiple sources while detecting contradictions
- **Trend Analysis**: Track evolving knowledge and belief changes over time
- **Quality Assurance**: Identify inconsistencies and gaps in knowledge bases

### Multi-Agent Systems
- **Distributed Intelligence**: Share and synchronize knowledge across agent networks
- **Collaborative Learning**: Combine insights from multiple agents while maintaining coherence
- **Conflict Resolution**: Handle disagreements between different information sources

## Key Features

### Intelligent Categorization
- Multi-level categorization with primary and secondary classifications
- Semantic tag extraction for enhanced searchability
- Confidence scoring for categorization decisions
- Support for batch processing and alternative suggestions

### Belief Management
- Automatic belief extraction from memories
- Confidence tracking with reinforcement learning
- Conflict detection and resolution strategies
- Evidence linking between beliefs and supporting memories

### Advanced Retrieval
- Semantic similarity search using vector embeddings
- Multi-modal search combining different techniques
- Contextual relevance ranking
- Search suggestions and query completion

### Selective Forgetting
- Configurable forgetting strategies (age, usage, relevance)
- Protection rules for critical information
- Archive and restore capabilities
- Batch processing for efficient memory management

### Performance and Scalability
- Stateless service design following 12-factor principles
- Optimized indexes for fast retrieval
- Batch operations for bulk processing
- Health monitoring and statistics collection

## Integration Patterns

### Microservice Architecture
Each module can be deployed as an independent microservice, communicating through well-defined interfaces. This enables:
- Independent scaling of compute-intensive operations
- Technology diversity across different processing stages
- Fault isolation and resilience

### Monolithic Deployment
All modules can be bundled into a single application for simpler deployment scenarios while maintaining the same interface contracts.

### Hybrid Approaches
Mix of embedded and external services based on performance and operational requirements.

## Exception Handling

The API defines specific exception types for different failure scenarios:

- **`InvalidInputException`** - Input validation failures
- **`MemoryNotFoundException`** - Memory retrieval failures
- **`StorageException`** - Storage operation failures
- **`BeliefAnalysisException`** - Belief processing failures
- **`RelevanceEvaluationException`** - Relevance scoring failures
- **`ForgettingException`** - Forgetting operation failures
- **`SearchException`** - Search and retrieval failures
- **`ResponseCompositionException`** - Response generation failures

This comprehensive error handling enables robust error recovery and detailed diagnostics for system monitoring and debugging.