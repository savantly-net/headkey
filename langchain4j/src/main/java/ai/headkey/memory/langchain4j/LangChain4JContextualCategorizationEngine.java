package ai.headkey.memory.langchain4j;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.memory.interfaces.ContextualCategorizationEngine;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * LangChain4j implementation of the Contextual Categorization Engine (CCE).
 * 
 * This implementation uses LangChain4j AI services to perform intelligent categorization
 * of content using Large Language Models. It provides sophisticated understanding
 * of context, semantics, and domain-specific categorization.
 * 
 * Features:
 * - AI-powered content categorization using LLMs
 * - Semantic tag extraction
 * - Confidence scoring based on AI responses
 * - Batch processing optimization
 * - Configurable category schemas
 * - Performance metrics and health monitoring
 * 
 * @since 1.0
 */
public class LangChain4JContextualCategorizationEngine implements ContextualCategorizationEngine {
    
    private final ChatLanguageModel chatModel;
    private final CategoryExtractionService categoryService;
    private final TagExtractionService tagService;
    
    private volatile double confidenceThreshold = 0.7;
    
    // Default category schema - can be customized
    private final Set<String> availableCategories = new LinkedHashSet<>(Arrays.asList(
        "UserProfile", "WorldFact", "PersonalData", "BusinessRule", "TechnicalKnowledge",
        "EmotionalState", "Preference", "Goal", "Memory", "Communication", "Unknown"
    ));
    
    private final Map<String, Set<String>> categorySubcategories = new ConcurrentHashMap<>();
    
    // Performance tracking
    private long totalCategorizations = 0;
    private long totalBatchCategorizations = 0;
    private long totalTagExtractions = 0;
    private final Map<String, Long> categoryFrequency = new ConcurrentHashMap<>();
    private final Map<String, Double> categoryConfidenceSum = new ConcurrentHashMap<>();
    private final Instant startTime = Instant.now();
    
    // Entity extraction patterns
    private final Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private final Pattern phonePattern = Pattern.compile("\\b\\d{3}-\\d{3}-\\d{4}\\b|\\b\\(\\d{3}\\)\\s*\\d{3}-\\d{4}\\b");
    private final Pattern urlPattern = Pattern.compile("https?://[\\w\\.-]+\\.[a-zA-Z]{2,}[\\w\\.-]*/?[\\w\\.-?=%&]*");
    private final Pattern datePattern = Pattern.compile("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b|\\b\\d{4}-\\d{2}-\\d{2}\\b");
    
    /**
     * AI Service interface for category extraction using LangChain4j.
     */
    interface CategoryExtractionService {
        
        @UserMessage("""
            Analyze the following content and categorize it according to the available categories.
            
            Content: {{content}}
            Available Categories: {{categories}}
            Context Metadata: {{metadata}}
            
            Please respond with a JSON object containing:
            - "primary": the main category from the available categories
            - "secondary": an optional subcategory (can be null)
            - "confidence": a confidence score between 0.0 and 1.0
            - "reasoning": brief explanation for the categorization
            
            Example response:
            {
              "primary": "UserProfile",
              "secondary": "Preferences",
              "confidence": 0.85,
              "reasoning": "Content describes user preferences for food and activities"
            }
            
            If the content doesn't clearly fit any category, use "Unknown" with appropriate confidence.
            """)
        CategoryResponse categorizeContent(@V("content") String content, 
                                         @V("categories") String categories,
                                         @V("metadata") String metadata);
    }
    
    /**
     * AI Service interface for semantic tag extraction.
     */
    interface TagExtractionService {
        
        @UserMessage("""
            Extract semantic tags and entities from the following content.
            Focus on identifying:
            - Named entities (people, places, organizations)
            - Key concepts and topics
            - Temporal expressions
            - Important keywords
            - Relationships
            
            Content: {{content}}
            
            Return a JSON object with:
            - "tags": array of extracted tags/entities
            - "entities": object with categorized entities (person, place, organization, etc.)
            
            Example response:
            {
              "tags": ["John Doe", "software engineer", "Python", "San Francisco"],
              "entities": {
                "person": ["John Doe"],
                "profession": ["software engineer"],
                "technology": ["Python"],
                "location": ["San Francisco"]
              }
            }
            """)
        TagResponse extractTags(@V("content") String content);
    }
    
    /**
     * Response object for categorization results.
     */
    public static class CategoryResponse {
        public String primary;
        public String secondary;
        public double confidence;
        public String reasoning;
    }
    
    /**
     * Response object for tag extraction results.
     */
    public static class TagResponse {
        public List<String> tags = new ArrayList<>();
        public Map<String, List<String>> entities = new HashMap<>();
    }
    
    /**
     * Constructor that accepts a LangChain4j ChatLanguageModel.
     * 
     * @param chatModel The chat model to use for AI operations
     */
    public LangChain4JContextualCategorizationEngine(ChatLanguageModel chatModel) {
        this.chatModel = Objects.requireNonNull(chatModel, "ChatLanguageModel cannot be null");
        this.categoryService = AiServices.create(CategoryExtractionService.class, chatModel);
        this.tagService = AiServices.create(TagExtractionService.class, chatModel);
        
        initializeSubcategories();
    }
    
    /**
     * Initialize default subcategories for each primary category.
     */
    private void initializeSubcategories() {
        categorySubcategories.put("UserProfile", Set.of("Biography", "Preferences", "Skills", "Contacts"));
        categorySubcategories.put("WorldFact", Set.of("Geography", "History", "Science", "Culture"));
        categorySubcategories.put("PersonalData", Set.of("Identity", "Health", "Financial", "Family"));
        categorySubcategories.put("BusinessRule", Set.of("Policy", "Process", "Regulation", "Standard"));
        categorySubcategories.put("TechnicalKnowledge", Set.of("Programming", "Architecture", "Tools", "Documentation"));
        categorySubcategories.put("EmotionalState", Set.of("Mood", "Feeling", "Reaction", "Sentiment"));
        categorySubcategories.put("Preference", Set.of("Likes", "Dislikes", "Priorities", "Values"));
        categorySubcategories.put("Goal", Set.of("Short-term", "Long-term", "Personal", "Professional"));
        categorySubcategories.put("Memory", Set.of("Event", "Experience", "Learning", "Association"));
        categorySubcategories.put("Communication", Set.of("Message", "Meeting", "Call", "Email"));
    }
    
    @Override
    public CategoryLabel categorize(String content, Metadata meta) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        
        try {
            totalCategorizations++;
            
            String categories = String.join(", ", availableCategories);
            String metadata = meta != null ? meta.toString() : "No metadata provided";
            
            CategoryResponse response = categoryService.categorizeContent(content, categories, metadata);
            
            // Validate and sanitize the response
            String primaryCategory = validateCategory(response.primary);
            String secondaryCategory = validateSubcategory(primaryCategory, response.secondary);
            double confidence = Math.max(0.0, Math.min(1.0, response.confidence));
            
            // Extract additional tags
            Set<String> tags = extractTags(content);
            
            // Create the category label
            CategoryLabel label = new CategoryLabel(primaryCategory, secondaryCategory, tags, confidence);
            
            // Update statistics
            updateStatistics(primaryCategory, confidence);
            
            return label;
            
        } catch (Exception e) {
            // Fallback to basic categorization on AI service failure
            return createFallbackCategory(content, meta);
        }
    }
    
    @Override
    public Set<String> extractTags(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        
        try {
            totalTagExtractions++;
            
            Set<String> allTags = new LinkedHashSet<>();
            
            // Extract tags using AI service
            TagResponse response = tagService.extractTags(content);
            if (response.tags != null) {
                allTags.addAll(response.tags);
            }
            
            // Add pattern-based entity extraction as backup
            allTags.addAll(extractPatternBasedTags(content));
            
            return allTags;
            
        } catch (Exception e) {
            // Fallback to pattern-based extraction only
            return extractPatternBasedTags(content);
        }
    }
    
    @Override
    public Map<String, CategoryLabel> categorizeBatch(Map<String, String> contentItems, Metadata commonMeta) {
        if (contentItems == null || contentItems.isEmpty()) {
            throw new IllegalArgumentException("Content items cannot be null or empty");
        }
        
        totalBatchCategorizations++;
        
        Map<String, CategoryLabel> results = new HashMap<>();
        
        // Process items individually for now
        // TODO: Implement true batch processing with AI service optimization
        for (Map.Entry<String, String> entry : contentItems.entrySet()) {
            try {
                CategoryLabel label = categorize(entry.getValue(), commonMeta);
                results.put(entry.getKey(), label);
            } catch (Exception e) {
                // Add fallback category for failed items
                results.put(entry.getKey(), createFallbackCategory(entry.getValue(), commonMeta));
            }
        }
        
        return results;
    }
    
    @Override
    public List<CategoryLabel> suggestAlternativeCategories(String content, Metadata meta, int maxSuggestions) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        if (maxSuggestions < 1) {
            throw new IllegalArgumentException("maxSuggestions must be at least 1");
        }
        
        List<CategoryLabel> alternatives = new ArrayList<>();
        
        try {
            // Get primary categorization
            CategoryLabel primary = categorize(content, meta);
            alternatives.add(primary);
            
            // For now, provide rule-based alternatives
            // TODO: Enhance with AI-powered alternative suggestions
            for (String category : availableCategories) {
                if (!category.equals(primary.getPrimary()) && alternatives.size() < maxSuggestions) {
                    double alternativeConfidence = Math.max(0.1, primary.getConfidence() - 0.2);
                    CategoryLabel alternative = new CategoryLabel(category, null, 
                        extractTags(content), alternativeConfidence);
                    alternatives.add(alternative);
                }
            }
            
        } catch (Exception e) {
            // Provide fallback alternatives
            alternatives.add(createFallbackCategory(content, meta));
        }
        
        return alternatives.stream()
                .limit(maxSuggestions)
                .collect(Collectors.toList());
    }
    
    @Override
    public void provideFeedback(String content, CategoryLabel assignedCategory, CategoryLabel correctCategory) {
        if (content == null || assignedCategory == null || correctCategory == null) {
            throw new IllegalArgumentException("All parameters must be non-null");
        }
        
        // TODO: Implement feedback learning mechanism
        // This could involve:
        // 1. Storing feedback examples for fine-tuning
        // 2. Adjusting confidence thresholds
        // 3. Learning from correction patterns
        // 4. Building a feedback corpus for model improvement
    }
    
    @Override
    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }
    
    @Override
    public void setConfidenceThreshold(double threshold) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Confidence threshold must be between 0.0 and 1.0");
        }
        this.confidenceThreshold = threshold;
    }
    
    @Override
    public List<String> getAvailableCategories() {
        return new ArrayList<>(availableCategories);
    }
    
    @Override
    public Set<String> getSubcategories(String primaryCategory) {
        if (primaryCategory == null) {
            throw new IllegalArgumentException("Primary category cannot be null");
        }
        return categorySubcategories.getOrDefault(primaryCategory, Set.of());
    }
    
    @Override
    public Map<String, Object> getCategorizationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalCategorizations", totalCategorizations);
        stats.put("totalBatchCategorizations", totalBatchCategorizations);
        stats.put("totalTagExtractions", totalTagExtractions);
        stats.put("startTime", startTime);
        stats.put("uptimeSeconds", Instant.now().getEpochSecond() - startTime.getEpochSecond());
        stats.put("confidenceThreshold", confidenceThreshold);
        
        // Category frequency distribution
        Map<String, Object> categoryStats = new HashMap<>();
        for (Map.Entry<String, Long> entry : categoryFrequency.entrySet()) {
            String category = entry.getKey();
            long count = entry.getValue();
            double avgConfidence = categoryConfidenceSum.getOrDefault(category, 0.0) / count;
            
            Map<String, Object> catStat = new HashMap<>();
            catStat.put("count", count);
            catStat.put("averageConfidence", avgConfidence);
            catStat.put("percentage", (double) count / totalCategorizations * 100);
            
            categoryStats.put(category, catStat);
        }
        stats.put("categoryDistribution", categoryStats);
        
        // Model information
        Map<String, Object> modelInfo = new HashMap<>();
        modelInfo.put("modelClass", chatModel.getClass().getSimpleName());
        modelInfo.put("healthy", isHealthy());
        stats.put("model", modelInfo);
        
        return stats;
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Test the AI service with a simple categorization
            CategoryResponse response = categoryService.categorizeContent(
                "Test content for health check", 
                String.join(", ", availableCategories),
                "Health check metadata"
            );
            return response != null && response.primary != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validates that a category is in the available categories list.
     */
    private String validateCategory(String category) {
        if (category == null || !availableCategories.contains(category)) {
            return "Unknown";
        }
        return category;
    }
    
    /**
     * Validates that a subcategory is valid for the given primary category.
     */
    private String validateSubcategory(String primaryCategory, String subcategory) {
        if (subcategory == null) {
            return null;
        }
        
        Set<String> validSubcategories = categorySubcategories.get(primaryCategory);
        if (validSubcategories != null && validSubcategories.contains(subcategory)) {
            return subcategory;
        }
        
        return null; // Invalid subcategory
    }
    
    /**
     * Creates a fallback category when AI service fails.
     */
    private CategoryLabel createFallbackCategory(String content, Metadata meta) {
        Set<String> tags = extractPatternBasedTags(content);
        return new CategoryLabel("Unknown", null, tags, 0.1);
    }
    
    /**
     * Extracts tags using pattern matching as backup/supplement to AI extraction.
     */
    private Set<String> extractPatternBasedTags(String content) {
        Set<String> tags = new LinkedHashSet<>();
        
        // Extract emails
        emailPattern.matcher(content).results()
            .forEach(match -> tags.add("email:" + match.group()));
        
        // Extract phone numbers
        phonePattern.matcher(content).results()
            .forEach(match -> tags.add("phone:" + match.group()));
        
        // Extract URLs
        urlPattern.matcher(content).results()
            .forEach(match -> tags.add("url:" + match.group()));
        
        // Extract dates
        datePattern.matcher(content).results()
            .forEach(match -> tags.add("date:" + match.group()));
        
        return tags;
    }
    
    /**
     * Updates internal statistics tracking.
     */
    private void updateStatistics(String category, double confidence) {
        categoryFrequency.merge(category, 1L, Long::sum);
        categoryConfidenceSum.merge(category, confidence, Double::sum);
    }
    
    /**
     * Adds a custom category to the available categories.
     * 
     * @param category The category to add
     * @param subcategories Optional subcategories for the new category
     */
    public void addCustomCategory(String category, Set<String> subcategories) {
        availableCategories.add(category);
        if (subcategories != null && !subcategories.isEmpty()) {
            categorySubcategories.put(category, new HashSet<>(subcategories));
        }
    }
    
    /**
     * Gets the underlying ChatLanguageModel for advanced configuration.
     */
    public ChatLanguageModel getChatModel() {
        return chatModel;
    }
}