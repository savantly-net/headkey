package ai.headkey.rest.config;

import jakarta.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CDI qualifier annotation for Elasticsearch persistence implementations.
 *
 * This qualifier is used to distinguish Elasticsearch-specific implementations
 * of persistence services from other implementations (e.g., PostgreSQL).
 *
 * Usage:
 * - Apply to producer methods that create Elasticsearch-specific beans
 * - Use in injection points to request Elasticsearch implementations
 * - Enables conditional bean activation based on persistence configuration
 *
 * Example:
 * <pre>
 * {@code
 * @Produces
 * @Singleton
 * @ElasticsearchPersistence
 * public BeliefStorageService beliefStorageService() {
 *     return new ElasticsearchBeliefStorageService(...);
 * }
 *
 * @Inject
 * @ElasticsearchPersistence
 * BeliefStorageService beliefStorageService;
 * }
 * </pre>
 *
 * This follows the CDI standard for creating custom qualifiers and enables
 * type-safe dependency injection with multiple implementations of the same interface.
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
public @interface ElasticsearchPersistence {
}
