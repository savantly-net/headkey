package ai.headkey.memory.langchain4j;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import ai.headkey.memory.interfaces.ContextualCategorizationEngine;
import dev.langchain4j.model.chat.ChatModel;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for LangChain4JComponentFactory.
 *
 * These tests verify the factory's ability to create properly configured
 * LangChain4j-based HeadKey components with various configurations.
 */
@ExtendWith(MockitoExtension.class)
class LangChain4JComponentFactoryTest {

    @Mock
    private ChatModel mockChatModel;

    @Test
    void testCreateCategorizationEngineWithChatModel() {
        ContextualCategorizationEngine engine =
            LangChain4JComponentFactory.createCategorizationEngine(
                mockChatModel
            );

        assertNotNull(engine);
        assertInstanceOf(
            LangChain4JContextualCategorizationEngine.class,
            engine
        );
        assertEquals(0.7, engine.getConfidenceThreshold(), 0.001);
    }

    @Test
    void testCreateCategorizationEngineWithNullChatModel() {
        assertThrows(NullPointerException.class, () ->
            LangChain4JComponentFactory.createCategorizationEngine(null)
        );
    }

    @Test
    void testBuilderWithChatModel() {
        ContextualCategorizationEngine engine =
            LangChain4JComponentFactory.builder()
                .withChatModel(mockChatModel)
                .withConfidenceThreshold(0.8)
                .build();

        assertNotNull(engine);
        assertEquals(0.8, engine.getConfidenceThreshold(), 0.001);
    }

    @Test
    void testBuilderWithCustomCategories() {
        Set<String> customCategories = Set.of(
            "CustomCategory1",
            "CustomCategory2"
        );

        ContextualCategorizationEngine engine =
            LangChain4JComponentFactory.builder()
                .withChatModel(mockChatModel)
                .withCustomCategories(customCategories)
                .build();

        assertNotNull(engine);
        assertTrue(engine.getAvailableCategories().contains("CustomCategory1"));
        assertTrue(engine.getAvailableCategories().contains("CustomCategory2"));
    }

    @Test
    void testBuilderWithInvalidConfidenceThreshold() {
        LangChain4JComponentFactory.CategorizationEngineBuilder builder =
            LangChain4JComponentFactory.builder();

        assertThrows(IllegalArgumentException.class, () ->
            builder.withConfidenceThreshold(-0.1)
        );
        assertThrows(IllegalArgumentException.class, () ->
            builder.withConfidenceThreshold(1.1)
        );
    }

    @Test
    void testBuilderWithoutChatModel() {
        LangChain4JComponentFactory.CategorizationEngineBuilder builder =
            LangChain4JComponentFactory.builder().withConfidenceThreshold(0.8);

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void testBuilderWithOpenAiValidApiKey() {
        // This test would require a real API key, so we test parameter validation
        LangChain4JComponentFactory.CategorizationEngineBuilder builder =
            LangChain4JComponentFactory.builder();

        assertThrows(IllegalArgumentException.class, () ->
            builder.withOpenAi(null)
        );
        assertThrows(IllegalArgumentException.class, () ->
            builder.withOpenAi("")
        );
        assertThrows(IllegalArgumentException.class, () ->
            builder.withOpenAi("   ")
        );
    }

    @Test
    void testBuilderWithOpenAiCustomSettings() {
        // Test that the builder accepts valid parameters without throwing
        LangChain4JComponentFactory.CategorizationEngineBuilder builder =
            LangChain4JComponentFactory.builder();

        assertDoesNotThrow(() ->
            builder.withOpenAi("test-api-key", "gpt-4", 0.5)
        );

        // Test with null model name (should use default)
        assertDoesNotThrow(() -> builder.withOpenAi("test-api-key", null, 0.5));
    }

    @Test
    void testCreateOpenAiCategorizationEngineWithInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () ->
            LangChain4JComponentFactory.createOpenAiCategorizationEngine(null)
        );
        assertThrows(IllegalArgumentException.class, () ->
            LangChain4JComponentFactory.createOpenAiCategorizationEngine("")
        );
        assertThrows(IllegalArgumentException.class, () ->
            LangChain4JComponentFactory.createOpenAiCategorizationEngine("   ")
        );

        assertThrows(IllegalArgumentException.class, () ->
            LangChain4JComponentFactory.createOpenAiCategorizationEngine(
                "valid-key",
                null
            )
        );
        assertThrows(IllegalArgumentException.class, () ->
            LangChain4JComponentFactory.createOpenAiCategorizationEngine(
                "valid-key",
                ""
            )
        );
        assertThrows(IllegalArgumentException.class, () ->
            LangChain4JComponentFactory.createOpenAiCategorizationEngine(
                "valid-key",
                "   "
            )
        );
    }

    @Test
    void testProvidersOpenAiMethods() {
        // Test that provider methods accept valid parameters
        assertDoesNotThrow(() ->
            LangChain4JComponentFactory.Providers.openAi("test-api-key")
        );
        assertDoesNotThrow(() ->
            LangChain4JComponentFactory.Providers.openAi(
                "test-api-key",
                "gpt-4"
            )
        );
        assertDoesNotThrow(() ->
            LangChain4JComponentFactory.Providers.openAiAccurate("test-api-key")
        );
        assertDoesNotThrow(() ->
            LangChain4JComponentFactory.Providers.openAiFast("test-api-key")
        );
    }

    @Test
    void testPresetMethods() {
        // Test that preset methods accept valid parameters
        assertDoesNotThrow(() ->
            LangChain4JComponentFactory.Presets.development("test-api-key")
        );
        assertDoesNotThrow(() ->
            LangChain4JComponentFactory.Presets.production("test-api-key")
        );
        assertDoesNotThrow(() ->
            LangChain4JComponentFactory.Presets.highAccuracy("test-api-key")
        );
    }

    @Test
    void testFactoryCannotBeInstantiated() {
        // Verify that the factory class cannot be instantiated
        assertThrows(UnsupportedOperationException.class, () -> {
            // Use reflection to try to create an instance
            try {
                var constructor =
                    LangChain4JComponentFactory.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                constructor.newInstance();
            } catch (Exception e) {
                if (e.getCause() instanceof UnsupportedOperationException) {
                    throw (UnsupportedOperationException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testBuilderFluentInterface() {
        // Test that the builder methods return the builder instance for chaining
        LangChain4JComponentFactory.CategorizationEngineBuilder builder =
            LangChain4JComponentFactory.builder();

        LangChain4JComponentFactory.CategorizationEngineBuilder result = builder
            .withChatModel(mockChatModel)
            .withConfidenceThreshold(0.9)
            .withCustomCategories(Set.of("TestCategory"));

        assertSame(builder, result);

        ContextualCategorizationEngine engine = builder.build();
        assertNotNull(engine);
        assertEquals(0.9, engine.getConfidenceThreshold(), 0.001);
    }

    @Test
    void testBuilderReset() {
        // Test that multiple builds from same builder work
        LangChain4JComponentFactory.CategorizationEngineBuilder builder =
            LangChain4JComponentFactory.builder().withChatModel(mockChatModel);

        ContextualCategorizationEngine engine1 = builder.build();
        ContextualCategorizationEngine engine2 = builder.build();

        assertNotNull(engine1);
        assertNotNull(engine2);
        assertNotSame(engine1, engine2); // Should be different instances
    }

    @Test
    void testDefaultValues() {
        ContextualCategorizationEngine engine =
            LangChain4JComponentFactory.builder()
                .withChatModel(mockChatModel)
                .build();

        // Test that default confidence threshold is applied
        assertEquals(0.7, engine.getConfidenceThreshold(), 0.001);

        // Test that default categories are available
        assertTrue(engine.getAvailableCategories().contains("UserProfile"));
        assertTrue(engine.getAvailableCategories().contains("WorldFact"));
        assertTrue(engine.getAvailableCategories().contains("Unknown"));
    }

    @Test
    void testEngineHealthAfterFactoryCreation() {
        ContextualCategorizationEngine engine =
            LangChain4JComponentFactory.createCategorizationEngine(
                mockChatModel
            );

        // Engine should be healthy if chat model is working
        // Note: This might return false due to mocking limitations, but shouldn't throw
        assertDoesNotThrow(() -> engine.isHealthy());
    }
}
