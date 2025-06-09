package ai.headkey.memory.langchain4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ai.headkey.memory.dto.CategoryLabel;
import ai.headkey.memory.dto.Metadata;
import ai.headkey.memory.interfaces.ContextualCategorizationEngine;
import ai.headkey.memory.langchain4j.dto.CategoryResponse;
import ai.headkey.memory.langchain4j.dto.TagResponse;
import ai.headkey.memory.langchain4j.services.CategoryExtractionService;
import ai.headkey.memory.langchain4j.services.LangChain4jCategoryExtractionService;
import ai.headkey.memory.langchain4j.services.LangChain4jTagExtractionService;
import ai.headkey.memory.langchain4j.services.TagExtractionService;
import dev.langchain4j.model.chat.ChatModel;

/**
 * LangChain4j implementation of the Contextual Categorization Engine (CCE).
 * 
 * This implementation uses LangChain4j AI services to perform intelligent categorization
 * of content using Large Language Models. It provides sophisticated understanding
 * of context, semantics, and domain-specific categorization.
 * 
 * The implementation follows SOLID principles:
 * - Single Responsibility: Focuses on orchestrating categorization and tag extraction
 * - Open/Closed: Can be extended with different service implementations
 * - Liskov Substitution: Can accept any valid service implementations
 * - Interface Segregation: Depends on focused service interfaces
 * - Dependency Inversion: Depends on service abstractions, not concrete implementations
 * 
 * Features:
 * - AI-powered content categorization using LLMs
 * - Semantic tag extraction
 * - Confidence scoring based on AI responses
 * - Batch processing optimization
 * - Configurable category schemas
 * - Performance metrics and health monitoring
 * - Pattern-based fallback extraction
 * 
 * @since 1.0
 */
public class LangChain4JContextualCategorizationEngine implements ContextualCategorizationEngine {
    
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
    
    // Entity extraction patterns for fallback
    private final Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private final Pattern phonePattern = Pattern.compile("\\b\\d{3}-\\d{3}-\\d{4}\\b|\\b\\(\\d{3}\\)\\s*\\d{3}-\\d{4}\\b");
    private final Pattern urlPattern = Pattern.compile("https?://[\\w\\.-]+\\.[a-zA-Z]{2,}[\\w\\.-]*/?[\\w\\.-?=%&]*");
    private final Pattern datePattern = Pattern.compile("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b|\\b\\d{4}-\\d{2}-\\d{2}\\b");
    
    /**
     * Constructor with dependency injection of services.
     * 
     * @param categoryService The category extraction service
     * @param tagService The tag extraction service
     */
    public LangChain4JContextualCategorizationEngine(CategoryExtractionService categoryService, 
                                                     TagExtractionService tagService) {
        this.categoryService = Objects.requireNonNull(categoryService, "CategoryExtractionService cannot be null");
        this.tagService = Objects.requireNonNull(tagService, "TagExtractionService cannot be null");
        
        initializeSubcategories();
    }
    
    /**
     * Convenience constructor that accepts a LangChain4j ChatLanguageModel.
     * Creates the default service implementations.
     * 
     * @param chatModel The chat model to use for AI operations
     */
    public LangChain4JContextualCategorizationEngine(ChatModel chatModel) {
        this(
            new LangChain4jCategoryExtractionService(chatModel),
            new LangChain4jTagExtractionService(chatModel)
        );
    }
    
    /**
     * Initialize subcategories for the default categories.
     */
    private void initializeSubcategories() {
        categorySubcategories.put("UserProfile", Set.of("Demographics", "Preferences", "Skills", "Interests"));
        categorySubcategories.put("WorldFact", Set.of("Science", "History", "Geography", "Current Events"));
        categorySubcategories.put("PersonalData", Set.of("Contact", "Identity", "Financial", "Health"));
        categorySubcategories.put("BusinessRule", Set.of("Policy", "Procedure", "Regulation", "Standard"));
        categorySubcategories.put("TechnicalKnowledge", Set.of("Programming", "Architecture", "Tools", "Frameworks"));
        categorySubcategories.put("EmotionalState", Set.of("Mood", "Feelings", "Reactions", "Sentiments"));
        categorySubcategories.put("Preference", Set.of("Likes", "Dislikes", "Choices", "Priorities"));
        categorySubcategories.put("Goal", Set.of("Short-term", "Long-term", "Personal", "Professional"));
        categorySubcategories.put("Memory", Set.of("Episodic", "Semantic", "Procedural", "Working"));
        categorySubcategories.put("Communication", Set.of("Message", "Request", "Response", "Notification"));
    }
    
    @Override
    public CategoryLabel categorize(String content, Metadata meta) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        
        try {
            totalCategorizations++;
            
            // Prepare context metadata
            String metadataJson = prepareMetadataJson(meta);
            String availableCategoriesStr = String.join(", ", availableCategories);
            
            // Get categorization from AI service
            CategoryResponse response = categoryService.categorizeContent(content, availableCategoriesStr, metadataJson);
            
            // Extract tags using tag service
            Set<String> tags = extractTags(content);
            
            // Validate and process the response
            String primaryCategory = validateCategory(response.getPrimary());
            String secondaryCategory = validateSubcategory(primaryCategory, response.getSecondary());
            double confidence = Math.max(0.0, Math.min(1.0, response.getConfidence()));
            
            // Update statistics
            updateStatistics(primaryCategory, confidence);
            
            return new CategoryLabel(primaryCategory, secondaryCategory, tags, confidence);
            
        } catch (Exception e) {
            // Fallback to pattern-based categorization
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
            TagResponse tagResponse = tagService.extractTags(content);
            if (tagResponse.getTags() != null) {
                allTags.addAll(tagResponse.getTags());
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
            double avgConfidence = count > 0 ? categoryConfidenceSum.getOrDefault(category, 0.0) / count : 0.0;
            
            Map<String, Object> catStat = new HashMap<>();
            catStat.put("count", count);
            catStat.put("averageConfidence", avgConfidence);
            catStat.put("percentage", totalCategorizations > 0 ? (double) count / totalCategorizations * 100 : 0.0);
            
            categoryStats.put(category, catStat);
        }
        stats.put("categoryDistribution", categoryStats);
        
        // Service information
        Map<String, Object> serviceInfo = new HashMap<>();
        serviceInfo.put("categoryService", categoryService.getServiceName());
        serviceInfo.put("tagService", tagService.getServiceName());
        serviceInfo.put("categoryServiceHealthy", categoryService.isHealthy());
        serviceInfo.put("tagServiceHealthy", tagService.isHealthy());
        serviceInfo.put("healthy", isHealthy());
        stats.put("services", serviceInfo);
        
        return stats;
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Test both services
            boolean categoryServiceHealthy = categoryService.isHealthy();
            boolean tagServiceHealthy = tagService.isHealthy();
            
            return categoryServiceHealthy && tagServiceHealthy;
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
     * Prepares metadata for JSON serialization.
     */
    private String prepareMetadataJson(Metadata meta) {
        if (meta == null) {
            return "{}";
        }
        
        // Simple JSON construction for metadata
        // TODO: Use proper JSON library for complex metadata
        Map<String, Object> metaMap = meta.getProperties();
        if (metaMap.isEmpty()) {
            return "{}";
        }
        
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : metaMap.entrySet()) {
            if (!first) {
                json.append(", ");
            }
            json.append("\"").append(entry.getKey()).append("\": \"")
                .append(entry.getValue().toString()).append("\"");
            first = false;
        }
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Adds a custom category to the available categories.
     * 
     * @param category The category to add
     * @param subcategories Optional subcategories for the new category
     */
    public void addCustomCategory(String category, Set<String> subcategories) {
        if (category != null && !category.trim().isEmpty()) {
            availableCategories.add(category);
            if (subcategories != null && !subcategories.isEmpty()) {
                categorySubcategories.put(category, new HashSet<>(subcategories));
            }
        }
    }
    
    /**
     * Gets the category extraction service.
     * 
     * @return The category extraction service
     */
    public CategoryExtractionService getCategoryService() {
        return categoryService;
    }
    
    /**
     * Gets the tag extraction service.
     * 
     * @return The tag extraction service
     */
    public TagExtractionService getTagService() {
        return tagService;
    }
}