# LangChain4JBeliefExtractionService Implementation

## Overview

The `LangChain4JBeliefExtractionService` is a sophisticated AI-powered implementation of the `BeliefExtractionService` interface that leverages Large Language Models (LLMs) through the LangChain4J framework to extract structured beliefs from unstructured text content.

## Key Features

- **AI-Powered Extraction**: Uses LLMs for semantic understanding and context-aware belief extraction
- **Multi-Language Support**: Supports multiple languages depending on the underlying model
- **Advanced Conflict Detection**: Employs semantic similarity for sophisticated conflict identification
- **Confidence Scoring**: Provides AI-based confidence metrics for extracted beliefs
- **Fallback Mechanisms**: Includes simple fallback methods when AI services are unavailable
- **Comprehensive Testing**: Full unit test coverage with realistic expectations

## Architecture

The service follows SOLID principles and implements a clean separation of concerns:

```
LangChain4JBeliefExtractionService
├── Dependencies (constructor injection)
│   ├── ChatModel (Option 1)
│   └── AI Service Interfaces (Option 2)
│       ├── LangChain4jBeliefExtractionAiService
│       ├── LangChain4jSimilarityAiService
│       └── LangChain4jConflictDetectionAiService
├── Response DTOs (separate classes)
│   ├── BeliefExtractionResponse
│   ├── BeliefData
│   ├── CategoryExtractionResponse
│   ├── ConfidenceResponse
│   ├── SimilarityResponse
│   └── ConflictDetectionResponse
└── Fallback Methods (simple algorithms)
```

## Constructors

The service provides multiple constructor options for different use cases:

### 1. ChatModel Constructor
```java
public LangChain4JBeliefExtractionService(ChatModel chatModel)
```

### 2. AI Services Constructor
```java
public LangChain4JBeliefExtractionService(LangChain4jBeliefExtractionAiService beliefAiService,
                                         LangChain4jSimilarityAiService similarityService,
                                         LangChain4jConflictDetectionAiService conflictService)
```

### 3. AI Services Constructor with Custom Name
```java
public LangChain4JBeliefExtractionService(LangChain4jBeliefExtractionAiService beliefAiService,
                                         LangChain4jSimilarityAiService similarityService,
                                         LangChain4jConflictDetectionAiService conflictService,
                                         String serviceName)
```

### Usage Examples

```java
// Option 1: With ChatModel dependency injection
@Inject
private ChatModel chatModel;

@Produces
public BeliefExtractionService createBeliefExtractor() {
    return new LangChain4JBeliefExtractionService(chatModel);
}

// Option 2: With custom AI services
@Inject
private LangChain4jBeliefExtractionAiService beliefService;
@Inject
private LangChain4jSimilarityAiService similarityService;
@Inject
private LangChain4jConflictDetectionAiService conflictService;

@Produces
public BeliefExtractionService createCustomBeliefExtractor() {
    return new LangChain4JBeliefExtractionService(
        beliefService, similarityService, conflictService);
}

// Option 3: Direct usage
ChatModel model = OpenAiChatModel.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .modelName("gpt-3.5-turbo")
    .build();

BeliefExtractionService extractor = new LangChain4JBeliefExtractionService(model);
```

## Core Methods

### Belief Extraction

```java
List<ExtractedBelief> extractBeliefs(String content, String agentId, CategoryLabel category)
```

Extracts structured beliefs from memory content using AI analysis. The method:
- Analyzes content semantically
- Identifies preferences, facts, relationships, and opinions
- Returns beliefs with confidence scores and reasoning
- Includes fallback to exception handling for robustness

### Similarity Calculation

```java
double calculateSimilarity(String statement1, String statement2)
```

Calculates semantic similarity between belief statements:
- Uses AI for deep semantic understanding
- Falls back to simple word overlap calculation
- Returns scores between 0.0 (completely different) and 1.0 (identical)

### Conflict Detection

```java
boolean areConflicting(String statement1, String statement2, String category1, String category2)
```

Detects conflicts between belief statements:
- Identifies direct contradictions
- Recognizes mutually exclusive statements
- Handles logical inconsistencies and temporal conflicts
- Uses confidence thresholds for reliable detection

### Category Extraction

```java
String extractCategory(String statement)
```

Categorizes belief statements into semantic categories:
- preference: likes, dislikes, personal choices
- fact: objective, verifiable information
- relationship: connections between entities
- location: spatial information
- opinion: subjective judgments
- general: default category

### Confidence Calculation

```java
double calculateConfidence(String content, String statement, ExtractionContext context)
```

Calculates confidence levels for extracted beliefs considering:
- Linguistic certainty markers
- Strength of evidence in content
- Clarity and explicitness
- Consistency with context

## AI Service Interfaces

The implementation uses separate AI service interface classes with LangChain4J annotations:

### LangChain4jBeliefExtractionAiService

Located in `ai.headkey.memory.langchain4j.services.ai.LangChain4jBeliefExtractionAiService`

```java
@UserMessage("""
    Analyze the following content and extract beliefs, preferences, facts, and relationships.
    
    Content: {{content}}
    Agent ID: {{agentId}}
    Category Context: {{categoryContext}}
    
    [Detailed prompt instructions...]
    """)
BeliefExtractionResponse extractBeliefs(@V("content") String content, 
                                       @V("agentId") String agentId,
                                       @V("categoryContext") String categoryContext);
```

### LangChain4jSimilarityAiService

Located in `ai.headkey.memory.langchain4j.services.ai.LangChain4jSimilarityAiService`

Calculates semantic similarity between statements using structured AI prompts.

### LangChain4jConflictDetectionAiService

Located in `ai.headkey.memory.langchain4j.services.ai.LangChain4jConflictDetectionAiService`

Identifies conflicts between belief statements with detailed reasoning.

## Response DTOs

All response DTOs are located in the `ai.headkey.memory.langchain4j.dto` package as separate classes:

### BeliefExtractionResponse

Located in `ai.headkey.memory.langchain4j.dto.BeliefExtractionResponse`

```java
public class BeliefExtractionResponse {
    private List<BeliefData> beliefs;
    // Helper methods for filtering and analysis
}
```

### BeliefData

Located in `ai.headkey.memory.langchain4j.dto.BeliefData`

```java
public class BeliefData {
    private String statement;
    private String category;
    private double confidence;
    private boolean positive;
    private String reasoning;
    private List<String> tags;
    // Utility methods for validation and manipulation
}
```

### Other Response Types

- `SimilarityResponse`: Contains similarity score and reasoning (with utility methods)
- `ConflictDetectionResponse`: Contains conflict status, confidence, type, and reasoning (with assessment helpers)
- `CategoryExtractionResponse`: Contains category and confidence (with validation methods)
- `ConfidenceResponse`: Contains confidence score and reasoning (with level descriptors)

## Error Handling

The service implements robust error handling:

```java
try {
    List<ExtractedBelief> beliefs = service.extractBeliefs(content, agentId, category);
    // Process beliefs...
} catch (BeliefExtractionException e) {
    // Log error and use fallback service
    logger.warn("AI extraction failed, using simple extractor", e);
    beliefs = fallbackService.extractBeliefs(content, agentId, category);
}
```

## Fallback Mechanisms

When AI services fail, the implementation provides simple fallback methods:

- **Simple Similarity**: Word overlap calculation
- **Simple Conflict Detection**: Basic negation pattern matching
- **Default Confidence**: Returns 0.5 when AI calculation fails
- **Default Category**: Returns "general" for unknown categorizations

## Health Monitoring

```java
boolean isHealthy()
Map<String, Object> getServiceInfo()
```

- Health checks verify AI service connectivity
- Service info provides metadata about capabilities and configuration
- Suitable for monitoring and diagnostics

## Testing

Comprehensive unit tests cover:

- Constructor validation
- Input parameter validation
- Fallback behavior verification
- DTO functionality
- Error handling scenarios
- Service health and information

## Integration Patterns

### Pattern 1: CDI Integration with ChatModel

```java
@ApplicationScoped
public class BeliefExtractionServiceProducer {
    
    @Inject
    ChatModel chatModel;
    
    @Produces
    @ApplicationScoped
    public BeliefExtractionService createService() {
        return new LangChain4JBeliefExtractionService(chatModel);
    }
}
```

### Pattern 1b: CDI Integration with Custom AI Services

```java
@ApplicationScoped
public class CustomBeliefExtractionServiceProducer {
    
    @Inject
    LangChain4jBeliefExtractionAiService beliefService;
    
    @Inject
    LangChain4jSimilarityAiService similarityService;
    
    @Inject
    LangChain4jConflictDetectionAiService conflictService;
    
    @Produces
    @ApplicationScoped
    public BeliefExtractionService createCustomService() {
        return new LangChain4JBeliefExtractionService(
            beliefService, similarityService, conflictService, "Production-AI-Service");
    }
}
```

### Pattern 2: Configuration-Based Creation

```java
@ConfigProperty(name = "ai.model.provider")
String modelProvider;

@ConfigProperty(name = "ai.model.name")
String modelName;

public BeliefExtractionService createService() {
    ChatModel model = switch (modelProvider) {
        case "openai" -> OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName(modelName)
            .build();
        case "anthropic" -> AnthropicChatModel.builder()
            .apiKey(System.getenv("ANTHROPIC_API_KEY"))
            .modelName(modelName)
            .build();
        default -> throw new IllegalArgumentException("Unsupported provider: " + modelProvider);
    };
    
    return new LangChain4JBeliefExtractionService(model);
}
```

### Pattern 3: Environment-Specific Configuration

```java
// Development: Use cheaper models
@Profile("dev")
@Produces
public ChatModel devChatModel() {
    return OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-3.5-turbo")
        .temperature(0.3)
        .build();
}

// Production: Use advanced models
@Profile("prod")
@Produces
public ChatModel prodChatModel() {
    return OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4")
        .temperature(0.2)
        .maxTokens(1000)
        .build();
}
```

## Best Practices

1. **Dependency Injection**: Use appropriate constructor based on your needs:
   - Use ChatModel constructor for simple setups
   - Use AI services constructor for custom implementations or testing
2. **Error Handling**: Implement proper fallback strategies
3. **Monitoring**: Track API usage, costs, and success rates
4. **Testing**: Use mock AI services for unit tests, real models for integration tests
5. **Configuration**: Use environment-specific model configurations
6. **Security**: Store API keys securely, never in code
7. **Performance**: Consider caching for frequently used extractions
8. **Limits**: Implement rate limiting and retry logic for API calls
9. **Architecture**: Avoid nested classes - use separate interface and DTO classes
10. **Modularity**: Inject AI services separately for better testability and flexibility

## Dependencies

Required dependencies in `build.gradle`:

```gradle
api 'dev.langchain4j:langchain4j-core:1.0.0'
api 'dev.langchain4j:langchain4j-open-ai:1.0.0'
api 'dev.langchain4j:langchain4j:1.0.0'
api 'com.fasterxml.jackson.core:jackson-core:2.17.0'
api 'com.fasterxml.jackson.core:jackson-databind:2.17.0'
```

## Environment Variables

Required environment variables:

```bash
# For OpenAI
OPENAI_API_KEY=your-openai-api-key

# For Anthropic
ANTHROPIC_API_KEY=your-anthropic-api-key

# For Azure OpenAI
AZURE_OPENAI_KEY=your-azure-key
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
```

## Performance Considerations

- **API Costs**: LLM calls can be expensive; consider caching strategies
- **Latency**: AI services have network latency; implement timeouts
- **Rate Limits**: Most AI providers have rate limits; implement backoff strategies
- **Batch Processing**: Consider batching multiple extractions when possible
- **Model Selection**: Balance cost vs. quality based on use case requirements

## Future Enhancements

- Support for additional LLM providers (HuggingFace, Ollama, etc.)
- Caching layer for repeated extractions
- Batch processing APIs
- Advanced prompt engineering and fine-tuning
- Integration with vector databases for semantic search
- Machine learning model training for custom belief extraction
- Enhanced AI service interface implementations
- Custom DTO validation and transformation capabilities
- Plugin architecture for custom belief extraction algorithms