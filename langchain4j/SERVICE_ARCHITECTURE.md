# LangChain4j Service Architecture

## Overview

This document describes the refactored service architecture for the LangChain4j integration module. The refactoring follows SOLID principles to create a more maintainable, testable, and extensible design.

## Architecture Changes

### Before Refactoring
- `LangChain4JContextualCategorizationEngine` contained embedded service interfaces
- Tight coupling between the engine and AI service implementations
- Difficult to test and extend with different service implementations

### After Refactoring
- Extracted services into separate interfaces and implementations
- Dependency injection pattern for loose coupling
- Clear separation of concerns following SOLID principles

## SOLID Principles Applied

### Single Responsibility Principle (SRP)
- **CategoryExtractionService**: Responsible only for content categorization
- **TagExtractionService**: Responsible only for tag and entity extraction
- **LangChain4JContextualCategorizationEngine**: Orchestrates services and manages statistics

### Open/Closed Principle (OCP)
- Services can be extended through new implementations without modifying existing code
- Engine accepts any valid service implementations through interfaces

### Liskov Substitution Principle (LSP)
- Any implementation of `CategoryExtractionService` can replace another
- Any implementation of `TagExtractionService` can replace another

### Interface Segregation Principle (ISP)
- Services have focused, lean interfaces
- Clients depend only on methods they actually use

### Dependency Inversion Principle (DIP)
- Engine depends on service abstractions, not concrete implementations
- Services can be injected, making the system more flexible

## Component Structure

```
headkey/langchain4j/
├── dto/
│   ├── CategoryResponse.java     # Response DTO for categorization
│   └── TagResponse.java          # Response DTO for tag extraction
├── services/
│   ├── CategoryExtractionService.java           # Interface for categorization
│   ├── TagExtractionService.java               # Interface for tag extraction
│   ├── LangChain4jCategoryExtractionService.java  # LangChain4j implementation
│   ├── LangChain4jTagExtractionService.java       # LangChain4j implementation
│   └── ai/
│       ├── LangChain4jCategoryAiService.java       # AI service interface for categorization
│       └── LangChain4jTagAiService.java             # AI service interface for tag extraction
├── LangChain4JContextualCategorizationEngine.java # Main orchestrator
└── LangChain4JComponentFactory.java               # Factory for easy creation
```

## Service Interfaces

### CategoryExtractionService
- **Purpose**: AI-powered content categorization
- **Key Methods**:
  - `categorizeContent(content, categories, metadata)`
  - `isHealthy()`
  - `getServiceName()`

### TagExtractionService
- **Purpose**: Semantic tag and entity extraction
- **Key Methods**:
  - `extractTags(content)`
  - `extractTags(content, metadata)`
  - `extractTagsWithFocus(content, entityTypes)`
  - `isHealthy()`
  - `getServiceName()`
  - `getSupportedEntityTypes()`

## Implementation Classes

### LangChain4jCategoryExtractionService
- Implements `CategoryExtractionService`
- Uses `LangChain4jCategoryAiService` for AI operations
- Provides validation and fallback handling
- Includes health checks
- Supports custom AI service implementations via constructor injection

### LangChain4jTagExtractionService
- Implements `TagExtractionService`
- Uses `LangChain4jTagAiService` for AI operations
- Includes pattern-based fallback extraction
- Supports focused extraction for specific entity types
- Supports custom AI service implementations via constructor injection

## AI Service Interfaces

### LangChain4jCategoryAiService
- **Purpose**: Define AI service contract for categorization
- **Location**: `services/ai/`
- **Methods**:
  - `categorizeContent(content, categories, metadata)`
- **Usage**: Can be implemented by consumers for custom AI behavior

### LangChain4jTagAiService
- **Purpose**: Define AI service contract for tag extraction
- **Location**: `services/ai/`
- **Methods**:
  - `extractTags(content, metadata)`
  - `extractTagsWithFocus(content, entityTypes)`
- **Usage**: Can be implemented by consumers for custom AI behavior

## Data Transfer Objects

### CategoryResponse
- Contains categorization results
- Fields: primary, secondary, confidence, reasoning
- Includes validation and utility methods

### TagResponse
- Contains tag extraction results
- Fields: tags (List), entities (Map by category)
- Includes merging and utility methods

## Usage Examples

### Basic Usage (Convenience Constructor)
```java
ChatModel chatModel = OpenAiChatModel.builder()
    .apiKey("your-api-key")
    .modelName("gpt-3.5-turbo")
    .build();

ContextualCategorizationEngine engine = 
    new LangChain4JContextualCategorizationEngine(chatModel);
```

### Advanced Usage (Dependency Injection)
```java
ChatModel chatModel = OpenAiChatModel.builder()
    .apiKey("your-api-key")
    .modelName("gpt-4")
    .build();

CategoryExtractionService categoryService = 
    new LangChain4jCategoryExtractionService(chatModel);
TagExtractionService tagService = 
    new LangChain4jTagExtractionService(chatModel);

ContextualCategorizationEngine engine = 
    new LangChain4JContextualCategorizationEngine(categoryService, tagService);
```

### Custom AI Service Implementation
```java
// Custom AI service implementation
public class CustomCategoryAiService implements LangChain4jCategoryAiService {
    @Override
    public CategoryResponse categorizeContent(String content, String categories, String metadata) {
        // Custom categorization logic
        return new CategoryResponse("CustomCategory", null, 0.9, "Custom logic applied");
    }
}

// Use custom service
ChatModel chatModel = OpenAiChatModel.builder().apiKey("key").build();
LangChain4jCategoryAiService customAiService = new CustomCategoryAiService();
CategoryExtractionService categoryService = 
    new LangChain4jCategoryExtractionService(chatModel, customAiService, "CustomService");
```

### Using the Factory
```java
// Simple creation
ContextualCategorizationEngine engine = 
    LangChain4JComponentFactory.createOpenAiCategorizationEngine("api-key");

// Builder pattern
ContextualCategorizationEngine engine = 
    LangChain4JComponentFactory.builder()
        .withOpenAi("api-key", "gpt-4", 0.1)
        .withConfidenceThreshold(0.8)
        .build();
```

## Benefits of This Architecture

### Testability
- Services can be mocked independently
- Unit tests can focus on specific functionality
- Integration tests can use test implementations

### Extensibility
- New service implementations can be added easily
- Different AI providers can be supported through new implementations
- Services can be enhanced without affecting the engine
- Custom AI service interfaces allow complete control over AI interactions
- Consumers can implement their own prompt engineering and response handling

### Maintainability
- Clear separation of concerns
- Easier to debug and modify specific functionality
- Reduced coupling between components

### Flexibility
- Services can be swapped at runtime
- Different configurations for different use cases
- Support for custom implementations
- Custom AI service implementations for specialized use cases
- Mix and match different AI providers for different operations

## Pattern-Based Fallback

The tag extraction service includes pattern-based fallback extraction for:
- Email addresses
- Phone numbers
- URLs
- Dates

This ensures basic entity extraction even if the AI service fails.

## Health Monitoring

Both services include health check capabilities:
- `isHealthy()` method for service status
- Service name identification for monitoring
- Error handling with graceful degradation

## Error Handling

- Services throw specific exceptions for different failure types
- Fallback mechanisms for service failures
- Graceful degradation when AI services are unavailable

## Future Enhancements

### Potential Service Implementations
- Azure OpenAI Service implementation
- Google Vertex AI implementation
- Local model implementations (Ollama, etc.)
- Hybrid implementations combining multiple providers

### Additional Services
- `SimilaritySearchService` for semantic search
- `ContentSummarizationService` for text summarization
- `ContextEnrichmentService` for metadata enhancement

### Configuration Management
- Service discovery and registration
- Configuration-driven service selection
- Runtime service switching based on load or performance

## Migration Notes

### For Existing Code
- The convenience constructor maintains backward compatibility
- Factory methods remain unchanged
- Public API of the engine is preserved

### Deprecated Patterns
- Direct access to internal AI services is no longer recommended
- Use the service interfaces for extending functionality
- Prefer dependency injection over convenience constructors for new code

### AI Service Interface Extraction
- AI service interfaces are now in separate files in `services/ai/` package
- Consumers can provide custom implementations of these interfaces
- Default LangChain4j implementations are created automatically when using ChatModel constructor
- Custom implementations can be injected via the extended constructors

This architecture provides a solid foundation for building sophisticated AI-powered memory systems while maintaining clean, testable, and extensible code.