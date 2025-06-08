package ai.headkey.memory.langchain4j;

import ai.headkey.memory.interfaces.ContextualCategorizationEngine;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.Objects;
import java.util.Set;

/**
 * Factory class for creating LangChain4j-based HeadKey components.
 * 
 * This factory provides convenient methods for creating and configuring
 * LangChain4j implementations of HeadKey interfaces with common configurations
 * and best practices built-in.
 * 
 * Features:
 * - Pre-configured instances for common use cases
 * - Builder pattern for custom configurations
 * - Support for multiple LLM providers
 * - Reasonable defaults for production use
 * - Easy switching between providers
 * 
 * @since 1.0
 */
public class LangChain4JComponentFactory {
    
    // Default configuration constants
    private static final double DEFAULT_CONFIDENCE_THRESHOLD = 0.7;
    private static final double DEFAULT_TEMPERATURE = 0.3;
    private static final String DEFAULT_OPENAI_MODEL = "gpt-3.5-turbo";
    private static final int DEFAULT_MAX_TOKENS = 500;
    
    /**
     * Creates a ContextualCategorizationEngine using the provided ChatLanguageModel.
     * 
     * @param chatModel The LangChain4j ChatLanguageModel to use
     * @return Configured ContextualCategorizationEngine instance
     * @throws IllegalArgumentException if chatModel is null
     */
    public static ContextualCategorizationEngine createCategorizationEngine(ChatLanguageModel chatModel) {
        Objects.requireNonNull(chatModel, "ChatLanguageModel cannot be null");
        return new LangChain4JContextualCategorizationEngine(chatModel);
    }
    
    /**
     * Creates a ContextualCategorizationEngine with OpenAI using the provided API key.
     * Uses sensible defaults for production use.
     * 
     * @param openAiApiKey The OpenAI API key
     * @return Configured ContextualCategorizationEngine instance
     * @throws IllegalArgumentException if apiKey is null or empty
     */
    public static ContextualCategorizationEngine createOpenAiCategorizationEngine(String openAiApiKey) {
        return createOpenAiCategorizationEngine(openAiApiKey, DEFAULT_OPENAI_MODEL);
    }
    
    /**
     * Creates a ContextualCategorizationEngine with OpenAI using the specified model.
     * 
     * @param openAiApiKey The OpenAI API key
     * @param modelName The OpenAI model name (e.g., "gpt-3.5-turbo", "gpt-4")
     * @return Configured ContextualCategorizationEngine instance
     * @throws IllegalArgumentException if apiKey is null/empty or modelName is null/empty
     */
    public static ContextualCategorizationEngine createOpenAiCategorizationEngine(
            String openAiApiKey, String modelName) {
        
        if (openAiApiKey == null || openAiApiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("OpenAI API key cannot be null or empty");
        }
        if (modelName == null || modelName.trim().isEmpty()) {
            throw new IllegalArgumentException("Model name cannot be null or empty");
        }
        
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(openAiApiKey.trim())
                .modelName(modelName.trim())
                .temperature(DEFAULT_TEMPERATURE)
                .maxTokens(DEFAULT_MAX_TOKENS)
                .build();
        
        LangChain4JContextualCategorizationEngine engine = 
            new LangChain4JContextualCategorizationEngine(chatModel);
        engine.setConfidenceThreshold(DEFAULT_CONFIDENCE_THRESHOLD);
        
        return engine;
    }
    
    /**
     * Creates a ContextualCategorizationEngine from environment variables.
     * Looks for OPENAI_API_KEY environment variable.
     * 
     * @return Configured ContextualCategorizationEngine instance
     * @throws IllegalStateException if OPENAI_API_KEY environment variable is not set
     */
    public static ContextualCategorizationEngine createFromEnvironment() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "OPENAI_API_KEY environment variable must be set");
        }
        return createOpenAiCategorizationEngine(apiKey);
    }
    
    /**
     * Builder for creating customized ContextualCategorizationEngine instances.
     */
    public static class CategorizationEngineBuilder {
        private ChatLanguageModel chatModel;
        private double confidenceThreshold = DEFAULT_CONFIDENCE_THRESHOLD;
        private Set<String> customCategories;
        
        /**
         * Sets the ChatLanguageModel to use.
         * 
         * @param chatModel The LangChain4j ChatLanguageModel
         * @return This builder instance
         */
        public CategorizationEngineBuilder withChatModel(ChatLanguageModel chatModel) {
            this.chatModel = chatModel;
            return this;
        }
        
        /**
         * Sets the confidence threshold for categorization decisions.
         * 
         * @param threshold Confidence threshold (0.0 to 1.0)
         * @return This builder instance
         * @throws IllegalArgumentException if threshold is outside valid range
         */
        public CategorizationEngineBuilder withConfidenceThreshold(double threshold) {
            if (threshold < 0.0 || threshold > 1.0) {
                throw new IllegalArgumentException(
                    "Confidence threshold must be between 0.0 and 1.0");
            }
            this.confidenceThreshold = threshold;
            return this;
        }
        
        /**
         * Configures OpenAI with default settings.
         * 
         * @param apiKey The OpenAI API key
         * @return This builder instance
         */
        public CategorizationEngineBuilder withOpenAi(String apiKey) {
            return withOpenAi(apiKey, DEFAULT_OPENAI_MODEL, DEFAULT_TEMPERATURE);
        }
        
        /**
         * Configures OpenAI with custom settings.
         * 
         * @param apiKey The OpenAI API key
         * @param modelName The model name
         * @param temperature The temperature setting
         * @return This builder instance
         */
        public CategorizationEngineBuilder withOpenAi(String apiKey, String modelName, double temperature) {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new IllegalArgumentException("OpenAI API key cannot be null or empty");
            }
            
            this.chatModel = OpenAiChatModel.builder()
                    .apiKey(apiKey.trim())
                    .modelName(modelName != null ? modelName : DEFAULT_OPENAI_MODEL)
                    .temperature(temperature)
                    .maxTokens(DEFAULT_MAX_TOKENS)
                    .build();
            
            return this;
        }
        
        /**
         * Adds custom categories to the engine.
         * 
         * @param categories Set of custom category names
         * @return This builder instance
         */
        public CategorizationEngineBuilder withCustomCategories(Set<String> categories) {
            this.customCategories = categories;
            return this;
        }
        
        /**
         * Builds the ContextualCategorizationEngine with the configured settings.
         * 
         * @return Configured ContextualCategorizationEngine instance
         * @throws IllegalStateException if no ChatLanguageModel has been configured
         */
        public ContextualCategorizationEngine build() {
            if (chatModel == null) {
                throw new IllegalStateException("ChatLanguageModel must be configured");
            }
            
            LangChain4JContextualCategorizationEngine engine = 
                new LangChain4JContextualCategorizationEngine(chatModel);
            
            engine.setConfidenceThreshold(confidenceThreshold);
            
            if (customCategories != null && !customCategories.isEmpty()) {
                for (String category : customCategories) {
                    engine.addCustomCategory(category, null);
                }
            }
            
            return engine;
        }
    }
    
    /**
     * Creates a new builder for customized ContextualCategorizationEngine instances.
     * 
     * @return New CategorizationEngineBuilder instance
     */
    public static CategorizationEngineBuilder builder() {
        return new CategorizationEngineBuilder();
    }
    
    /**
     * Utility methods for common provider configurations.
     */
    public static class Providers {
        
        /**
         * Creates OpenAI ChatLanguageModel with production-ready settings.
         * 
         * @param apiKey OpenAI API key
         * @return Configured ChatLanguageModel
         */
        public static ChatLanguageModel openAi(String apiKey) {
            return openAi(apiKey, "gpt-3.5-turbo");
        }
        
        /**
         * Creates OpenAI ChatLanguageModel with specified model.
         * 
         * @param apiKey OpenAI API key
         * @param modelName Model name
         * @return Configured ChatLanguageModel
         */
        public static ChatLanguageModel openAi(String apiKey, String modelName) {
            return OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .temperature(DEFAULT_TEMPERATURE)
                    .maxTokens(DEFAULT_MAX_TOKENS)
                    .build();
        }
        
        /**
         * Creates OpenAI ChatLanguageModel optimized for accuracy.
         * Uses GPT-4 with lower temperature for consistent results.
         * 
         * @param apiKey OpenAI API key
         * @return Configured ChatLanguageModel
         */
        public static ChatLanguageModel openAiAccurate(String apiKey) {
            return OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName("gpt-4")
                    .temperature(0.1)
                    .maxTokens(DEFAULT_MAX_TOKENS)
                    .build();
        }
        
        /**
         * Creates OpenAI ChatLanguageModel optimized for speed and cost.
         * Uses GPT-3.5-turbo with optimized settings.
         * 
         * @param apiKey OpenAI API key
         * @return Configured ChatLanguageModel
         */
        public static ChatLanguageModel openAiFast(String apiKey) {
            return OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName("gpt-3.5-turbo")
                    .temperature(0.2)
                    .maxTokens(200) // Shorter responses for speed
                    .build();
        }
    }
    
    /**
     * Pre-configured factory methods for common scenarios.
     */
    public static class Presets {
        
        /**
         * Creates a categorization engine optimized for development and testing.
         * Uses fast, cost-effective settings.
         * 
         * @param apiKey OpenAI API key
         * @return Development-optimized ContextualCategorizationEngine
         */
        public static ContextualCategorizationEngine development(String apiKey) {
            return builder()
                    .withOpenAi(apiKey, "gpt-3.5-turbo", 0.3)
                    .withConfidenceThreshold(0.6)
                    .build();
        }
        
        /**
         * Creates a categorization engine optimized for production use.
         * Uses balanced accuracy and performance settings.
         * 
         * @param apiKey OpenAI API key
         * @return Production-optimized ContextualCategorizationEngine
         */
        public static ContextualCategorizationEngine production(String apiKey) {
            return builder()
                    .withOpenAi(apiKey, "gpt-3.5-turbo", 0.2)
                    .withConfidenceThreshold(0.7)
                    .build();
        }
        
        /**
         * Creates a categorization engine optimized for maximum accuracy.
         * Uses the most capable model with conservative settings.
         * 
         * @param apiKey OpenAI API key
         * @return Accuracy-optimized ContextualCategorizationEngine
         */
        public static ContextualCategorizationEngine highAccuracy(String apiKey) {
            return builder()
                    .withOpenAi(apiKey, "gpt-4", 0.1)
                    .withConfidenceThreshold(0.8)
                    .build();
        }
    }
    
    // Private constructor to prevent instantiation
    private LangChain4JComponentFactory() {
        throw new UnsupportedOperationException("Factory class cannot be instantiated");
    }
}