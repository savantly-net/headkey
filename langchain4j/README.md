# HeadKey LangChain4j Module

This module provides LangChain4j-based implementations of HeadKey's AI-powered components, leveraging Large Language Models for intelligent content processing and categorization.

## Overview

The LangChain4j module implements HeadKey interfaces using LangChain4j AI services, enabling sophisticated natural language understanding and processing capabilities through integration with various LLM providers.

## Components

### LangChain4JContextualCategorizationEngine

An AI-powered implementation of the `ContextualCategorizationEngine` interface that uses Large Language Models to intelligently categorize content and extract semantic tags.

**Features:**
- AI-powered content categorization using LLMs
- Semantic tag extraction with entity recognition
- Configurable confidence thresholds
- Batch processing optimization
- Custom category schema support
- Pattern-based fallback for reliability
- Comprehensive performance metrics
- Health monitoring and diagnostics

## Quick Start

### 1. Add Dependencies

The module uses LangChain4j BOM for version management:

```gradle
dependencies {
    implementation project(':langchain4j')
    // LangChain4j provider of your choice
    implementation 'dev.langchain4j:langchain4j-openai'
}
```

### 2. Basic Usage with OpenAI

```java
import ai.headkey.memory.langchain4j.LangChain4JContextualCategorizationEngine;
import dev.langchain4j.model.openai.OpenAiChatModel;

// Configure OpenAI model
ChatLanguageModel chatModel = OpenAiChatModel.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .modelName("gpt-3.5-turbo")
    .temperature(0.3)
    .build();

// Create categorization engine
LangChain4JContextualCategorizationEngine engine = 
    new LangChain4JContextualCategorizationEngine(chatModel);

// Categorize content
CategoryLabel result = engine.categorize(
    "I love hiking in the mountains and taking photos", 
    null
);

System.out.println("Category: " + result.getPrimary());
System.out.println("Confidence: " + result.getConfidence());
```

### 3. Extract Semantic Tags

```java
Set<String> tags = engine.extractTags(
    "Contact John Doe at john.doe@example.com or call 555-123-4567"
);

// Results include AI-extracted tags plus pattern-based entities:
// ["John Doe", "contact information", "email:john.doe@example.com", "phone:555-123-4567"]
```

### 4. Batch Processing

```java
Map<String, String> contentItems = Map.of(
    "item1", "I enjoy programming in Java",
    "item2", "Paris is the capital of France",
    "item3", "My birthday is December 25th"
);

Map<String, CategoryLabel> results = engine.categorizeBatch(contentItems, null);
```

## Supported LLM Providers

The engine works with any LangChain4j-supported provider:

### OpenAI
```java
ChatLanguageModel model = OpenAiChatModel.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .modelName("gpt-4")
    .build();
```

### Azure OpenAI
```java
ChatLanguageModel model = AzureOpenAiChatModel.builder()
    .apiKey(System.getenv("AZURE_OPENAI_KEY"))
    .endpoint(System.getenv("AZURE_OPENAI_ENDPOINT"))
    .deploymentName("gpt-35-turbo")
    .build();
```

### Anthropic Claude
```java
ChatLanguageModel model = AnthropicChatModel.builder()
    .apiKey(System.getenv("ANTHROPIC_API_KEY"))
    .modelName("claude-3-sonnet-20240229")
    .build();
```

### Local Models (Ollama)
```java
ChatLanguageModel model = OllamaChatModel.builder()
    .baseUrl("http://localhost:11434")
    .modelName("llama2")
    .build();
```

## Configuration

### Default Categories

The engine comes with predefined categories:
- `UserProfile` - Personal information and characteristics
- `WorldFact` - General knowledge and facts
- `PersonalData` - Sensitive personal information
- `BusinessRule` - Organizational policies and processes
- `TechnicalKnowledge` - Technical information and documentation
- `EmotionalState` - Emotions, feelings, and moods
- `Preference` - User preferences and choices
- `Goal` - Objectives and aspirations
- `Memory` - Past experiences and recollections
- `Communication` - Messages and interactions
- `Unknown` - Uncategorizable content

### Custom Categories

Add domain-specific categories:

```java
engine.addCustomCategory("ProjectManagement", 
    Set.of("Planning", "Execution", "Monitoring", "Documentation"));

engine.addCustomCategory("CustomerService", 
    Set.of("Inquiry", "Complaint", "Feedback", "Support"));
```

### Confidence Threshold

Configure minimum confidence for categorization:

```java
engine.setConfidenceThreshold(0.8); // Higher threshold = more conservative
```

## Advanced Features

### Alternative Suggestions

Get multiple categorization options:

```java
List<CategoryLabel> alternatives = engine.suggestAlternativeCategories(
    "Ambiguous content", null, 3
);
```

### Performance Monitoring

Track usage statistics:

```java
Map<String, Object> stats = engine.getCategorizationStatistics();
System.out.println("Total categorizations: " + stats.get("totalCategorizations"));
System.out.println("Category distribution: " + stats.get("categoryDistribution"));
```

### Health Monitoring

Check engine health:

```java
boolean healthy = engine.isHealthy();
if (!healthy) {
    System.err.println("Categorization engine is unhealthy!");
}
```

### Feedback Learning

Provide correction feedback (for future enhancement):

```java
CategoryLabel assigned = /* assigned category */;
CategoryLabel correct = /* correct category */;
engine.provideFeedback("content", assigned, correct);
```

## Error Handling and Fallbacks

The engine includes robust error handling:

1. **AI Service Failures**: Falls back to pattern-based categorization
2. **Invalid Responses**: Validates and sanitizes AI responses
3. **Network Issues**: Graceful degradation with local processing
4. **Rate Limiting**: Automatic retry with exponential backoff

Example fallback behavior:
```java
// If AI service fails, content is categorized as "Unknown" 
// with pattern-based tag extraction still working
CategoryLabel fallback = engine.categorize("content", null);
// fallback.getPrimary() == "Unknown"
// fallback.getConfidence() <= 0.2
// fallback.getTags() still contains pattern-extracted entities
```

## Best Practices

### 1. Model Selection
- **Development/Testing**: Use faster, cheaper models like `gpt-3.5-turbo`
- **Production**: Use more capable models like `gpt-4` for better accuracy
- **Cost Optimization**: Consider local models for high-volume scenarios

### 2. Temperature Settings
- **Consistent Categorization**: Use low temperature (0.1-0.3)
- **Creative Applications**: Use higher temperature (0.7-0.9)

### 3. Content Preprocessing
```java
// Clean and normalize content before categorization
String cleanContent = content.trim()
    .replaceAll("\\s+", " ")
    .replaceAll("[\\p{Cntrl}]", "");
```

### 4. Batch Processing
```java
// Process large datasets efficiently
Map<String, String> batch = contentItems.entrySet().stream()
    .limit(100) // Process in chunks
    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
```

### 5. Error Monitoring
```java
try {
    CategoryLabel result = engine.categorize(content, metadata);
    if (result.getConfidence() < engine.getConfidenceThreshold()) {
        // Handle low-confidence results
        logger.warn("Low confidence categorization: {}", result);
    }
} catch (Exception e) {
    logger.error("Categorization failed for content: {}", content, e);
    // Handle fallback behavior
}
```

## Testing

The module includes comprehensive tests:

```bash
./gradlew :langchain4j:test
```

### Mock Testing
For testing without real AI services:
```java
ChatLanguageModel mockModel = mock(ChatLanguageModel.class);
when(mockModel.generate(any())).thenReturn(mockResponse);

LangChain4JContextualCategorizationEngine engine = 
    new LangChain4JContextualCategorizationEngine(mockModel);
```

## Examples

See `LangChain4JCategorizationEngineExample.java` for comprehensive usage examples including:
- Basic categorization
- Batch processing
- Custom categories
- Statistics monitoring
- Different provider configurations

## Dependencies

- **LangChain4j Core**: AI service abstractions and implementations
- **Jackson**: JSON processing for AI responses
- **HeadKey API**: Core interfaces and DTOs
- **HeadKey Core**: Base implementations and utilities

## Performance Considerations

### Latency
- **Single categorization**: 100-500ms (depends on model and provider)
- **Batch processing**: More efficient than individual calls
- **Local models**: Lower latency but may have reduced accuracy

### Cost Optimization
- Use appropriate model size for your accuracy requirements
- Implement caching for repeated content
- Consider local models for high-volume scenarios
- Monitor token usage and implement rate limiting

### Scalability
- Engine instances are thread-safe
- Consider connection pooling for high-concurrency scenarios
- Implement circuit breakers for external AI services
- Use async processing for non-blocking operations

## Security Considerations

- **API Keys**: Store securely, never commit to version control
- **Content Privacy**: Be aware that content is sent to external AI services
- **Rate Limiting**: Implement to prevent abuse and manage costs
- **Input Validation**: Sanitize content before processing

## Troubleshooting

### Common Issues

1. **API Key Issues**
   ```
   Exception: Invalid API key
   Solution: Verify API key is correctly set in environment variables
   ```

2. **Rate Limiting**
   ```
   Exception: Rate limit exceeded
   Solution: Implement exponential backoff and retry logic
   ```

3. **Model Not Found**
   ```
   Exception: Model not available
   Solution: Check model name and availability in your region
   ```

4. **Low Accuracy**
   ```
   Issue: Poor categorization results
   Solution: Try more capable models, adjust temperature, improve prompts
   ```

## Contributing

When contributing to this module:
1. Ensure compatibility with LangChain4j version specified in BOM
2. Add comprehensive tests for new features
3. Update documentation for API changes
4. Follow HeadKey coding standards and best practices
5. Consider backward compatibility for interface changes

## License

This module is part of the HeadKey project and follows the same licensing terms.