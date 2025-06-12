package ai.headkey.memory.langchain4j;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.langchain4j.dto.*;
import ai.headkey.memory.langchain4j.services.ai.*;
import ai.headkey.memory.spi.BeliefExtractionService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

import java.util.*;
import java.util.regex.Pattern;

/**
 * AI-powered belief extraction service using LangChain4J.
 * 
 * This implementation leverages large language models (LLMs) through the LangChain4J
 * framework to provide sophisticated belief extraction capabilities. It uses natural
 * language understanding to identify beliefs, preferences, facts, and relationships
 * from unstructured text with much higher accuracy than simple pattern matching.
 * 
 * Key features:
 * - Semantic understanding using transformer models
 * - Context-aware belief extraction
 * - Multi-language support (depending on model)
 * - Confidence scoring based on model certainty
 * - Advanced conflict detection using semantic similarity
 * - Integration with various LLM providers (OpenAI, Anthropic, etc.)
 * 
 * Requirements:
 * - LangChain4J dependency
 * - API key for chosen LLM provider
 * - Network connectivity for API calls
 * - Sufficient quota/credits for API usage
 * 
 * @since 1.0
 */
public class LangChain4JBeliefExtractionService implements BeliefExtractionService {

    private final LangChain4jBeliefExtractionAiService beliefAiService;
    private final LangChain4jSimilarityAiService similarityService;
    private final LangChain4jConflictDetectionAiService conflictService;
    private final String serviceName;

    /**
     * Constructor with ChatModel dependency.
     * Creates AI service instances using LangChain4J AiServices.
     * 
     * @param chatModel The LangChain4j ChatModel to use for AI operations
     * @throws IllegalArgumentException if chatModel is null
     */
    public LangChain4JBeliefExtractionService(ChatModel chatModel) {
        Objects.requireNonNull(chatModel, "ChatModel cannot be null");
        this.beliefAiService = AiServices.create(LangChain4jBeliefExtractionAiService.class, chatModel);
        this.similarityService = AiServices.create(LangChain4jSimilarityAiService.class, chatModel);
        this.conflictService = AiServices.create(LangChain4jConflictDetectionAiService.class, chatModel);
        this.serviceName = "LangChain4j-" + chatModel.getClass().getSimpleName();
    }

    /**
     * Constructor with AI service dependencies.
     * Allows consumers to provide their own AI service implementations.
     * 
     * @param beliefAiService The belief extraction AI service
     * @param similarityService The similarity calculation AI service  
     * @param conflictService The conflict detection AI service
     * @throws IllegalArgumentException if any service is null
     */
    public LangChain4JBeliefExtractionService(LangChain4jBeliefExtractionAiService beliefAiService,
                                             LangChain4jSimilarityAiService similarityService,
                                             LangChain4jConflictDetectionAiService conflictService) {
        this.beliefAiService = Objects.requireNonNull(beliefAiService, "BeliefAiService cannot be null");
        this.similarityService = Objects.requireNonNull(similarityService, "SimilarityService cannot be null");
        this.conflictService = Objects.requireNonNull(conflictService, "ConflictService cannot be null");
        this.serviceName = "LangChain4j-CustomServices";
    }

    /**
     * Constructor with AI service dependencies and custom service name.
     * 
     * @param beliefAiService The belief extraction AI service
     * @param similarityService The similarity calculation AI service  
     * @param conflictService The conflict detection AI service
     * @param serviceName The custom service name
     * @throws IllegalArgumentException if any service or serviceName is null
     */
    public LangChain4JBeliefExtractionService(LangChain4jBeliefExtractionAiService beliefAiService,
                                             LangChain4jSimilarityAiService similarityService,
                                             LangChain4jConflictDetectionAiService conflictService,
                                             String serviceName) {
        this.beliefAiService = Objects.requireNonNull(beliefAiService, "BeliefAiService cannot be null");
        this.similarityService = Objects.requireNonNull(similarityService, "SimilarityService cannot be null");
        this.conflictService = Objects.requireNonNull(conflictService, "ConflictService cannot be null");
        this.serviceName = Objects.requireNonNull(serviceName, "ServiceName cannot be null");
    }

    @Override
    public List<ExtractedBelief> extractBeliefs(String content, String agentId, CategoryLabel category) {
        if (content == null || content.trim().isEmpty()) {
            return new ArrayList<>();
        }
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent ID cannot be null or empty");
        }

        try {
            String categoryContext = category != null ? 
                String.format("Primary: %s, Secondary: %s", category.getPrimary(), category.getSecondary()) : 
                "No specific category";

            BeliefExtractionResponse response = beliefAiService.extractBeliefs(content, agentId, categoryContext);
            
            return parseBeliefResponse(response, agentId);
        } catch (Exception e) {
            throw new BeliefExtractionException("Failed to extract beliefs using AI: " + e.getMessage(), e);
        }
    }

    @Override
    public double calculateSimilarity(String statement1, String statement2) {
        if (statement1 == null || statement2 == null) {
            return 0.0;
        }

        try {
            SimilarityResponse response = similarityService.calculateSimilarity(statement1, statement2);
            return Math.max(0.0, Math.min(1.0, response.getSimilarityScore()));
        } catch (Exception e) {
            // Fallback to simple similarity
            return calculateSimpleSimilarity(statement1, statement2);
        }
    }

    @Override
    public boolean areConflicting(String statement1, String statement2, String category1, String category2) {
        if (statement1 == null || statement2 == null) {
            return false;
        }

        try {
            String categoryInfo = String.format("Category1: %s, Category2: %s", 
                category1 != null ? category1 : "unknown", 
                category2 != null ? category2 : "unknown");
            
            ConflictDetectionResponse response = conflictService.detectConflict(statement1, statement2, categoryInfo);
            return response.isConflicting() && response.getConfidence() > 0.6;
        } catch (Exception e) {
            // Fallback to simple conflict detection
            return detectSimpleConflict(statement1, statement2);
        }
    }

    @Override
    public String extractCategory(String statement) {
        if (statement == null || statement.trim().isEmpty()) {
            return "general";
        }

        try {
            CategoryExtractionResponse response = beliefAiService.extractCategory(statement);
            return response.getCategory() != null ? response.getCategory() : "general";
        } catch (Exception e) {
            return "general";
        }
    }

    @Override
    public double calculateConfidence(String content, String statement, ExtractionContext context) {
        if (content == null || statement == null) {
            return 0.5;
        }

        try {
            String contextInfo = buildContextInfo(context);
            ConfidenceResponse response = beliefAiService.calculateConfidence(content, statement, contextInfo);
            return Math.max(0.0, Math.min(1.0, response.getConfidence()));
        } catch (Exception e) {
            return 0.5; // Default confidence
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            // Perform a simple health check
            BeliefExtractionResponse response = beliefAiService.extractBeliefs(
                "I like coffee", "test-agent", "Test category");
            return response != null && response.getBeliefs() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Map<String, Object> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("serviceType", "LangChain4JBeliefExtractionService");
        info.put("version", "1.0");
        info.put("description", "AI-powered belief extraction using LangChain4J");
        info.put("capabilities", Arrays.asList(
            "semantic_understanding",
            "context_awareness", 
            "multi_language_support",
            "advanced_conflict_detection",
            "confidence_scoring"
        ));
        info.put("serviceName", serviceName);
        info.put("aiServices", Map.of(
            "beliefExtraction", beliefAiService.getClass().getSimpleName(),
            "similarity", similarityService.getClass().getSimpleName(),
            "conflictDetection", conflictService.getClass().getSimpleName()
        ));
        return info;
    }

    // ========== Private Helper Methods ==========

    private List<ExtractedBelief> parseBeliefResponse(BeliefExtractionResponse response, String agentId) {
        List<ExtractedBelief> beliefs = new ArrayList<>();
        
        if (response == null || response.getBeliefs() == null) {
            return beliefs;
        }

        for (BeliefData beliefData : response.getBeliefs()) {
            ExtractedBelief belief = new ExtractedBelief(
                beliefData.getStatement(),
                agentId,
                beliefData.getCategory(),
                beliefData.getConfidence(),
                beliefData.isPositive()
            );
            
            // Add tags if present
            if (beliefData.getTags() != null) {
                beliefData.getTags().forEach(belief::addTag);
            }
            
            // Add metadata
            belief.addMetadata("reasoning", beliefData.getReasoning());
            belief.addMetadata("extractionMethod", "langchain4j-ai");
            
            beliefs.add(belief);
        }
        
        return beliefs;
    }

    private String buildContextInfo(ExtractionContext context) {
        if (context == null) {
            return "No additional context";
        }
        
        StringBuilder info = new StringBuilder();
        
        if (context.getSourceMemory() != null) {
            info.append("Source: ").append(context.getSourceMemory().getId()).append("; ");
        }
        
        if (!context.getExistingBeliefs().isEmpty()) {
            info.append("Existing beliefs count: ").append(context.getExistingBeliefs().size()).append("; ");
        }
        
        if (!context.getAdditionalContext().isEmpty()) {
            info.append("Additional context: ").append(context.getAdditionalContext().toString());
        }
        
        return info.length() > 0 ? info.toString() : "No additional context";
    }

    private double calculateSimpleSimilarity(String statement1, String statement2) {
        String s1 = statement1.toLowerCase().trim();
        String s2 = statement2.toLowerCase().trim();
        
        if (s1.equals(s2)) {
            return 1.0;
        }
        
        String[] words1 = s1.split("\\s+");
        String[] words2 = s2.split("\\s+");
        
        Set<String> set1 = new HashSet<>(Arrays.asList(words1));
        Set<String> set2 = new HashSet<>(Arrays.asList(words2));
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private boolean detectSimpleConflict(String statement1, String statement2) {
        String s1 = statement1.toLowerCase();
        String s2 = statement2.toLowerCase();
        
        Pattern negationPattern = Pattern.compile("(?i)(not|never|no|don't|doesn't|isn't|aren't)");
        boolean hasNegation1 = negationPattern.matcher(s1).find();
        boolean hasNegation2 = negationPattern.matcher(s2).find();
        
        if (hasNegation1 != hasNegation2) {
            return calculateSimpleSimilarity(s1, s2) > 0.6;
        }
        
        return false;
    }

    // ========== Getter Methods for Testing ==========

    /**
     * Gets the belief AI service instance.
     * Package-private for testing purposes.
     * 
     * @return The belief AI service
     */
    LangChain4jBeliefExtractionAiService getBeliefAiService() {
        return beliefAiService;
    }

    /**
     * Gets the similarity AI service instance.
     * Package-private for testing purposes.
     * 
     * @return The similarity AI service
     */
    LangChain4jSimilarityAiService getSimilarityService() {
        return similarityService;
    }

    /**
     * Gets the conflict detection AI service instance.
     * Package-private for testing purposes.
     * 
     * @return The conflict detection AI service
     */
    LangChain4jConflictDetectionAiService getConflictService() {
        return conflictService;
    }

    /**
     * Gets the service name.
     * 
     * @return The service name
     */
    public String getServiceName() {
        return serviceName;
    }
}