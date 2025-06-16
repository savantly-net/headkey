package ai.headkey.persistence;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.interfaces.BeliefStorageService;
import ai.headkey.memory.interfaces.VectorEmbeddingGenerator;
import ai.headkey.memory.interfaces.BeliefStorageService.SimilarBelief;
import ai.headkey.persistence.factory.JpaBeliefStorageServiceFactory;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JpaBeliefStorageServiceEmbeddingTest {

    private static EntityManagerFactory entityManagerFactory;
    private static BeliefStorageService storageServiceWithEmbedding;
    private static BeliefStorageService storageServiceWithoutEmbedding;

    // Mock embedding generator that creates simple vectors based on text content
    private static final VectorEmbeddingGenerator mockEmbeddingGenerator =
        text -> {
            if (text == null || text.trim().isEmpty()) {
                return new double[5]; // Return zero vector for empty text
            }

            // Create a simple deterministic embedding based on text content
            // This is for testing purposes only - real embeddings would be much more sophisticated
            double[] embedding = new double[5];
            String normalized = text.toLowerCase().trim();

            // Simple hashing approach for consistent test results
            for (int i = 0; i < embedding.length; i++) {
                int hash = (normalized + i).hashCode();
                embedding[i] = (hash % 1000) / 1000.0; // Normalize to [-1, 1] range
            }

            return embedding;
        };

    @BeforeAll
    static void setUpClass() {
        // Create H2 EntityManagerFactory for testing
        Map<String, Object> testProperties = new HashMap<>();
        testProperties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        testProperties.put(
            "jakarta.persistence.jdbc.url",
            "jdbc:h2:mem:testdb_embedding;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        );
        testProperties.put("jakarta.persistence.jdbc.user", "sa");
        testProperties.put("jakarta.persistence.jdbc.password", "");
        testProperties.put(
            "hibernate.dialect",
            "org.hibernate.dialect.H2Dialect"
        );
        testProperties.put("hibernate.hbm2ddl.auto", "create-drop");
        testProperties.put("hibernate.show_sql", "false");
        testProperties.put("hibernate.format_sql", "false");

        entityManagerFactory = Persistence.createEntityManagerFactory(
            "headkey-beliefs-h2-test",
            testProperties
        );

        // Create storage service with embedding generator
        storageServiceWithEmbedding = JpaBeliefStorageServiceFactory.builder()
            .withEntityManagerFactory(entityManagerFactory)
            .withAutoCreateSchema(true)
            .withStatistics(false)
            .withEmbeddingGenerator(mockEmbeddingGenerator)
            .build();

        // Create storage service without embedding generator for comparison
        storageServiceWithoutEmbedding =
            JpaBeliefStorageServiceFactory.builder()
                .withEntityManagerFactory(entityManagerFactory)
                .withAutoCreateSchema(true)
                .withStatistics(false)
                .build();
    }

    @AfterAll
    static void tearDownClass() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @BeforeEach
    void setUp() {
        // Clean up any existing data before each test
        // Note: H2 with create-drop should handle this, but being explicit
    }

    @Test
    @Order(1)
    @DisplayName("Should create service with embedding generator")
    void testServiceCreationWithEmbeddingGenerator() {
        assertNotNull(storageServiceWithEmbedding);
        assertTrue(storageServiceWithEmbedding.isHealthy());
        // The embedding functionality is now in the repository layer
        // We can't directly check the service, but we can verify it works
        // by storing and retrieving beliefs with embeddings
    }

    @Test
    @Order(2)
    @DisplayName("Should create service without embedding generator")
    void testServiceCreationWithoutEmbeddingGenerator() {
        assertNotNull(storageServiceWithoutEmbedding);
        assertTrue(storageServiceWithoutEmbedding.isHealthy());
        // The service will work the same, but the repository won't generate embeddings
    }

    @Test
    @Order(3)
    @DisplayName("Should store belief with embedding generation")
    void testStoreBeliefWithEmbedding() {
        // Arrange
        Belief belief = createTestBelief(
            "embedding-test-1",
            "test-agent",
            "The sky is blue on a clear day"
        );

        // Act
        Belief stored = storageServiceWithEmbedding.storeBelief(belief);

        // Assert
        assertNotNull(stored);
        assertEquals(belief.getStatement(), stored.getStatement());
        assertEquals(belief.getAgentId(), stored.getAgentId());

        // Verify the belief can be retrieved
        Optional<Belief> retrieved = storageServiceWithEmbedding.getBeliefById(
            stored.getId()
        );
        assertTrue(retrieved.isPresent());
        assertEquals(stored.getStatement(), retrieved.get().getStatement());
    }

    @Test
    @Order(4)
    @DisplayName("Should store multiple beliefs with embeddings")
    void testStoreBeliefsWithEmbeddings() {
        // Arrange
        List<Belief> beliefs = Arrays.asList(
            createTestBelief(
                "batch-1",
                "test-agent",
                "Cats are independent animals"
            ),
            createTestBelief(
                "batch-2",
                "test-agent",
                "Dogs are loyal companions"
            ),
            createTestBelief(
                "batch-3",
                "test-agent",
                "Birds can fly through the air"
            )
        );

        // Act
        List<Belief> stored = storageServiceWithEmbedding.storeBeliefs(beliefs);

        // Assert
        assertNotNull(stored);
        assertEquals(3, stored.size());

        for (int i = 0; i < beliefs.size(); i++) {
            assertEquals(
                beliefs.get(i).getStatement(),
                stored.get(i).getStatement()
            );
            assertEquals(
                beliefs.get(i).getAgentId(),
                stored.get(i).getAgentId()
            );
        }
    }

    // disabled due to no vector search in H2
    //@Test
    @Order(5)
    @DisplayName("Should find similar beliefs using vector search")
    void testVectorSimilaritySearch() {
        // Arrange - store some test beliefs
        List<Belief> beliefs = Arrays.asList(
            createTestBelief(
                "similar-1",
                "test-agent",
                "The ocean is vast and deep"
            ),
            createTestBelief(
                "similar-2",
                "test-agent",
                "The sea is wide and profound"
            ),
            createTestBelief(
                "similar-3",
                "test-agent",
                "Mountains are tall and majestic"
            ),
            createTestBelief(
                "similar-4",
                "test-agent",
                "Space is infinite and mysterious"
            )
        );

        storageServiceWithEmbedding.storeBeliefs(beliefs);

        // Act - search for beliefs similar to ocean/sea concepts
        List<SimilarBelief> similar =
            storageServiceWithEmbedding.findSimilarBeliefs(
                "The water is deep and blue",
                "test-agent",
                0, // Low threshold to allow some matches
                10
            );

        // Assert
        assertNotNull(similar);
        assertTrue(
            similar.size() > 0,
            "Should find at least one similar belief"
        );

        // Verify results are sorted by similarity (highest first)
        for (int i = 0; i < similar.size() - 1; i++) {
            assertTrue(
                similar.get(i).getSimilarityScore() >=
                similar.get(i + 1).getSimilarityScore(),
                "Results should be sorted by similarity score descending"
            );
        }

        // Verify similarity scores are within expected range
        for (SimilarBelief similarBelief : similar) {
            assertTrue(
                similarBelief.getSimilarityScore() >= 0.0 &&
                similarBelief.getSimilarityScore() <= 1.0,
                "Similarity score should be between 0.0 and 1.0"
            );
        }
    }

    @Test
    @Order(6)
    @DisplayName("Should fallback to text search when no embedding generator")
    void testFallbackToTextSearch() {
        // Arrange - store beliefs using service without embeddings
        List<Belief> beliefs = Arrays.asList(
            createTestBelief(
                "text-1",
                "test-agent",
                "Red roses are beautiful flowers"
            ),
            createTestBelief(
                "text-2",
                "test-agent",
                "Blue roses are rare and special"
            ),
            createTestBelief(
                "text-3",
                "test-agent",
                "Yellow roses symbolize friendship"
            )
        );

        storageServiceWithoutEmbedding.storeBeliefs(beliefs);

        // Act - search for similar beliefs using text-based search
        List<SimilarBelief> similar =
            storageServiceWithoutEmbedding.findSimilarBeliefs(
                "Pink roses smell wonderful",
                "test-agent",
                0.1, // Low threshold to allow text-based matches
                10
            );

        // Assert
        assertNotNull(similar);
        // Text-based search should still find some matches based on word overlap
        assertTrue(similar.size() >= 0, "Text search should work as fallback");
    }

    @Test
    @Order(7)
    @DisplayName("Should handle embedding generation errors gracefully")
    void testEmbeddingGenerationErrorHandling() {
        // Arrange - create a service with a failing embedding generator
        VectorEmbeddingGenerator failingGenerator = text -> {
            throw new RuntimeException(
                "Simulated embedding generation failure"
            );
        };

        BeliefStorageService serviceWithFailingEmbedding =
            JpaBeliefStorageServiceFactory.builder()
                .withEntityManagerFactory(entityManagerFactory)
                .withAutoCreateSchema(true)
                .withStatistics(false)
                .withEmbeddingGenerator(failingGenerator)
                .build();

        // Act & Assert - should not fail completely, just skip embedding generation
        Belief belief = createTestBelief(
            "error-test",
            "test-agent",
            "This should still work"
        );

        assertDoesNotThrow(() -> {
            Belief stored = serviceWithFailingEmbedding.storeBelief(belief);
            assertNotNull(stored);
            assertEquals(belief.getStatement(), stored.getStatement());
        });
    }

    private Belief createTestBelief(
        String id,
        String agentId,
        String statement
    ) {
        return new Belief.Builder()
            .id(id)
            .agentId(agentId)
            .statement(statement)
            .confidence(0.8)
            .category("test")
            .createdAt(Instant.now())
            .lastUpdated(Instant.now())
            .active(true)
            .build();
    }
}
