package ai.headkey.memory.interfaces;

/**
 * Functional interface for generating vector embeddings from text content.
 */
@FunctionalInterface
public interface VectorEmbeddingGenerator {
    double[] generateEmbedding(String content) throws Exception;
}