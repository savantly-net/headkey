package ai.headkey.persistence.examples;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.headkey.memory.dto.Belief;
import ai.headkey.memory.interfaces.BeliefStorageService;
import ai.headkey.memory.interfaces.BeliefStorageService.SimilarBelief;
import ai.headkey.memory.interfaces.VectorEmbeddingGenerator;
import ai.headkey.persistence.factory.JpaBeliefStorageServiceFactory;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Example demonstrating how to use JpaBeliefStorageService with vector embeddings
 * for semantic similarity search.
 *
 * This example shows:
 * 1. How to create a JpaBeliefStorageService with repository-level embedding generation
 * 2. How to store beliefs that automatically get vector embeddings via repository
 * 3. How to perform semantic similarity searches using database-level vector operations
 * 4. Comparison between vector-based and text-based similarity search
 *
 * @since 1.0
 */
public class JpaBeliefStorageServiceEmbeddingExample {

    private static final String AGENT_ID = "demo-agent";

    /**
     * Mock embedding generator for demonstration purposes.
     * In a real application, you would use a proper embedding service like:
     * - OpenAI embeddings
     * - Sentence transformers
     * - LangChain4J embedding models
     * - HuggingFace transformers
     */
    private static final VectorEmbeddingGenerator mockEmbeddingGenerator =
        text -> {
            if (text == null || text.trim().isEmpty()) {
                return new double[1536]; // Standard embedding dimension
            }

            // Create a simple but deterministic embedding based on text content
            // This simulates what a real embedding model would do
            double[] embedding = new double[1536];
            String normalized = text.toLowerCase().trim();

            // Simple approach: use character codes and word patterns
            String[] words = normalized.split("\\s+");

            for (int i = 0; i < embedding.length; i++) {
                double value = 0.0;

                // Add influence from each word
                for (int wordIdx = 0; wordIdx < words.length; wordIdx++) {
                    String word = words[wordIdx];
                    if (!word.isEmpty()) {
                        // Use hash of word + position to create pseudo-semantic relationships
                        int hash = (word + i + wordIdx).hashCode();
                        value +=
                            Math.sin(hash * 0.001) *
                            (1.0 / Math.sqrt(wordIdx + 1));
                    }
                }

                // Add some text length influence
                value += Math.cos(normalized.length() * 0.01 + i * 0.1) * 0.1;

                // Normalize to reasonable range
                embedding[i] = Math.tanh(value);
            }

            // Normalize the vector to unit length (cosine similarity works better)
            double magnitude = 0.0;
            for (double value : embedding) {
                magnitude += value * value;
            }
            magnitude = Math.sqrt(magnitude);

            if (magnitude > 0) {
                for (int i = 0; i < embedding.length; i++) {
                    embedding[i] /= magnitude;
                }
            }

            return embedding;
        };

    public static void main(String[] args) {
        System.out.println(
            "=== JpaBeliefStorageService with Vector Embeddings Example ===\n"
        );

        try {
            // 1. Create EntityManagerFactory for H2 database
            EntityManagerFactory emf = createEntityManagerFactory();

            // 2. Create storage service with embedding generator
            BeliefStorageService storageService =
                JpaBeliefStorageServiceFactory.create(
                    emf,
                    mockEmbeddingGenerator
                );

            System.out.println(
                "✓ Created JpaBeliefStorageService with embedding generator"
            );
            System.out.println(
                "  Embedding functionality is now handled by the repository layer"
            );
            System.out.println();

            // 3. Store some test beliefs
            System.out.println(
                "Storing beliefs with automatic embedding generation..."
            );
            List<Belief> beliefs = createTestBeliefs();
            List<Belief> storedBeliefs = storageService.storeBeliefs(beliefs);

            System.out.println(
                "✓ Stored " + storedBeliefs.size() + " beliefs with embeddings"
            );
            System.out.println();

            // 4. Demonstrate vector-based similarity search
            System.out.println("=== Vector-Based Similarity Search ===");
            demonstrateVectorSearch(storageService);
            System.out.println();

            // 5. Compare with text-based search
            System.out.println("=== Comparison: Vector vs Text Search ===");
            compareSearchMethods(emf);
            System.out.println();

            // 6. Show service statistics
            System.out.println("=== Service Statistics ===");
            showServiceStatistics(storageService);

            // Cleanup
            emf.close();
            System.out.println("\n✓ Example completed successfully");
        } catch (Exception e) {
            System.err.println("Error running example: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static EntityManagerFactory createEntityManagerFactory() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        properties.put(
            "jakarta.persistence.jdbc.url",
            "jdbc:h2:mem:beliefstore_demo;DB_CLOSE_DELAY=-1"
        );
        properties.put("jakarta.persistence.jdbc.user", "sa");
        properties.put("jakarta.persistence.jdbc.password", "");
        properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.put("hibernate.hbm2ddl.auto", "create-drop");
        properties.put("hibernate.show_sql", "false");

        return Persistence.createEntityManagerFactory(
            "headkey-beliefs-h2-test",
            properties
        );
    }

    private static List<Belief> createTestBeliefs() {
        return Arrays.asList(
            // Animals and pets
            createBelief(
                "belief-1",
                "Dogs are loyal and faithful companions to humans"
            ),
            createBelief(
                "belief-2",
                "Cats are independent creatures that enjoy solitude"
            ),
            createBelief(
                "belief-3",
                "Birds have the amazing ability to fly through the sky"
            ),
            // Technology and AI
            createBelief(
                "belief-4",
                "Artificial intelligence is transforming how we work"
            ),
            createBelief(
                "belief-5",
                "Machine learning algorithms can recognize patterns in data"
            ),
            createBelief(
                "belief-6",
                "Computers process information much faster than humans"
            ),
            // Nature and environment
            createBelief(
                "belief-7",
                "The ocean contains countless mysteries and life forms"
            ),
            createBelief(
                "belief-8",
                "Mountains provide fresh air and beautiful landscapes"
            ),
            createBelief(
                "belief-9",
                "Forests are essential for maintaining Earth's ecosystem"
            ),
            // Food and cooking
            createBelief(
                "belief-10",
                "Fresh vegetables are important for a healthy diet"
            ),
            createBelief(
                "belief-11",
                "Homemade meals taste better than processed food"
            ),
            createBelief(
                "belief-12",
                "Spices and herbs enhance the flavor of any dish"
            )
        );
    }

    private static Belief createBelief(String id, String statement) {
        return new Belief.Builder()
            .id(id)
            .agentId(AGENT_ID)
            .statement(statement)
            .confidence(0.8)
            .category("knowledge")
            .createdAt(Instant.now())
            .lastUpdated(Instant.now())
            .active(true)
            .build();
    }

    private static void demonstrateVectorSearch(
        BeliefStorageService storageService
    ) {
        String[] queries = {
            "Pets and animal companions",
            "Computer technology and automation",
            "Natural environments and ecosystems",
            "Cooking and nutrition",
        };

        for (String query : queries) {
            System.out.println("Query: \"" + query + "\"");

            List<SimilarBelief> results = storageService.findSimilarBeliefs(
                query,
                AGENT_ID,
                0.3,
                3 // threshold 0.3, limit 3
            );

            if (results.isEmpty()) {
                System.out.println("  No similar beliefs found");
            } else {
                for (int i = 0; i < results.size(); i++) {
                    SimilarBelief similar = results.get(i);
                    System.out.printf(
                        "  %d. [%.3f] %s%n",
                        i + 1,
                        similar.getSimilarityScore(),
                        similar.getBelief().getStatement()
                    );
                }
            }
            System.out.println();
        }
    }

    private static void compareSearchMethods(EntityManagerFactory emf) {
        // Create services with and without embeddings
        BeliefStorageService vectorService =
            JpaBeliefStorageServiceFactory.create(emf, mockEmbeddingGenerator);
        BeliefStorageService textService =
            JpaBeliefStorageServiceFactory.create(emf);

        // Store same beliefs in both
        List<Belief> beliefs = Arrays.asList(
            createBelief(
                "comp-1",
                "Artificial intelligence will revolutionize medicine"
            ),
            createBelief(
                "comp-2",
                "Smart algorithms help doctors diagnose diseases"
            ),
            createBelief("comp-3", "Cats prefer to hunt during nighttime hours")
        );

        vectorService.storeBeliefs(beliefs);
        textService.storeBeliefs(beliefs);

        String query = "AI and healthcare technology";

        System.out.println("Query: \"" + query + "\"");
        System.out.println();

        // Vector-based search
        List<SimilarBelief> vectorResults = vectorService.findSimilarBeliefs(
            query,
            AGENT_ID,
            0.1,
            5
        );
        System.out.println("Vector-based results:");
        if (vectorResults.isEmpty()) {
            System.out.println("  No results found");
        } else {
            for (SimilarBelief result : vectorResults) {
                System.out.printf(
                    "  [%.3f] %s%n",
                    result.getSimilarityScore(),
                    result.getBelief().getStatement()
                );
            }
        }
        System.out.println();

        // Text-based search
        List<SimilarBelief> textResults = textService.findSimilarBeliefs(
            query,
            AGENT_ID,
            0.1,
            5
        );
        System.out.println("Text-based results:");
        if (textResults.isEmpty()) {
            System.out.println("  No results found");
        } else {
            for (SimilarBelief result : textResults) {
                System.out.printf(
                    "  [%.3f] %s%n",
                    result.getSimilarityScore(),
                    result.getBelief().getStatement()
                );
            }
        }

        System.out.println(
            "Note: Vector search uses database-level similarity calculations"
        );
        System.out.println(
            "      Text search uses application-level Jaccard similarity"
        );
        System.out.println(
            "      Vector search can find semantic relationships that text search might miss"
        );
    }

    private static void showServiceStatistics(
        BeliefStorageService storageService
    ) {
        Map<String, Object> stats = storageService.getStorageStatistics();

        System.out.println("Service Information:");
        System.out.println("  Service Type: " + stats.get("serviceType"));
        System.out.println("  Storage Type: " + stats.get("storageType"));
        System.out.println("  Embedding handled by: Repository layer");
        System.out.println();

        System.out.println("Operations Count:");
        System.out.println(
            "  Store Operations: " + stats.get("totalStoreOperations")
        );
        System.out.println(
            "  Query Operations: " + stats.get("totalQueryOperations")
        );
        System.out.println(
            "  Search Operations: " + stats.get("totalSearchOperations")
        );
        System.out.println();

        System.out.println("Data Statistics:");
        System.out.println("  Total Beliefs: " + stats.get("totalBeliefs"));
        System.out.println("  Active Beliefs: " + stats.get("activeBeliefs"));
        System.out.println("  Agent Count: " + stats.get("agentCount"));
    }
}
