package ai.headkey.rest.service;

import ai.headkey.memory.implementations.AbstractMemoryEncodingSystem.VectorEmbeddingGenerator;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test class for LangChain4JVectorEmbeddingGenerator.
 * 
 * This test class follows TDD principles and tests the integration
 * between the HeadKey VectorEmbeddingGenerator interface and the
 * LangChain4J EmbeddingModel.
 */
@ExtendWith(MockitoExtension.class)
class LangChain4JVectorEmbeddingGeneratorTest {

    @Mock
    private EmbeddingModel embeddingModel;

    private LangChain4JVectorEmbeddingGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new LangChain4JVectorEmbeddingGenerator(embeddingModel);
    }

    @Test
    void shouldImplementVectorEmbeddingGeneratorInterface() {
        assertInstanceOf(VectorEmbeddingGenerator.class, generator);
    }

    @Test
    void shouldGenerateEmbeddingFromText() throws Exception {
        // Given
        String inputText = "This is a test sentence for embedding generation.";
        float[] mockVector = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
        Embedding mockEmbedding = Embedding.from(mockVector);
        
        when(embeddingModel.embed(inputText)).thenReturn(Response.from(mockEmbedding));

        // When
        double[] result = generator.generateEmbedding(inputText);

        // Then
        assertNotNull(result);
        assertEquals(5, result.length);
        assertEquals(0.1, result[0], 0.001);
        assertEquals(0.2, result[1], 0.001);
        assertEquals(0.3, result[2], 0.001);
        assertEquals(0.4, result[3], 0.001);
        assertEquals(0.5, result[4], 0.001);
        
        verify(embeddingModel).embed(inputText);
    }

    @Test
    void shouldHandleEmptyText() throws Exception {
        // Given
        String emptyText = "";
        float[] mockVector = {0.0f, 0.0f, 0.0f};
        Embedding mockEmbedding = Embedding.from(mockVector);
        
        when(embeddingModel.embed(emptyText)).thenReturn(Response.from(mockEmbedding));

        // When
        double[] result = generator.generateEmbedding(emptyText);

        // Then
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(0.0, result[0], 0.001);
        assertEquals(0.0, result[1], 0.001);
        assertEquals(0.0, result[2], 0.001);
        
        verify(embeddingModel).embed(emptyText);
    }

    @Test
    void shouldHandleNullText() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            generator.generateEmbedding(null);
        });
        
        verify(embeddingModel, never()).embed(anyString());
    }

    @Test
    void shouldPropagateEmbeddingModelExceptions() {
        // Given
        String inputText = "Test text";
        RuntimeException embeddingException = new RuntimeException("Embedding service unavailable");
        
        when(embeddingModel.embed(inputText)).thenThrow(embeddingException);

        // When/Then
        Exception exception = assertThrows(Exception.class, () -> {
            generator.generateEmbedding(inputText);
        });
        
        assertEquals("Failed to generate embedding", exception.getMessage());
        assertEquals(embeddingException, exception.getCause());
        
        verify(embeddingModel).embed(inputText);
    }

    @Test
    void shouldHandleLargeText() throws Exception {
        // Given
        StringBuilder largeTextBuilder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeTextBuilder.append("This is sentence number ").append(i).append(". ");
        }
        String largeText = largeTextBuilder.toString();
        
        float[] mockVector = new float[384]; // Common embedding dimension
        for (int i = 0; i < mockVector.length; i++) {
            mockVector[i] = (float) Math.random();
        }
        Embedding mockEmbedding = Embedding.from(mockVector);
        
        when(embeddingModel.embed(largeText)).thenReturn(Response.from(mockEmbedding));

        // When
        double[] result = generator.generateEmbedding(largeText);

        // Then
        assertNotNull(result);
        assertEquals(384, result.length);
        
        verify(embeddingModel).embed(largeText);
    }

    @Test
    void shouldValidateConstructorArguments() {
        // When/Then
        assertThrows(NullPointerException.class, () -> {
            new LangChain4JVectorEmbeddingGenerator(null);
        });
    }

    @Test
    void shouldProvideToStringRepresentation() {
        // When
        String result = generator.toString();

        // Then
        assertNotNull(result);
        assertTrue(result.contains("LangChain4JVectorEmbeddingGenerator"));
        assertTrue(result.contains(embeddingModel.toString()));
    }
}