package ai.headkey.persistence.elastic.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;

/**
 * Configuration class for Elasticsearch client and connection management.
 *
 * This class provides factory methods for creating Elasticsearch clients with various
 * configuration options including authentication, clustering, and performance tuning.
 *
 * Features:
 * - Multiple Elasticsearch host support for clustering
 * - Authentication support (username/password, API key)
 * - Connection pooling and timeout configuration
 * - SSL/TLS support
 * - Jackson integration for JSON serialization
 * - Health checking and connection validation
 *
 * The configuration supports both development (single node) and production (cluster)
 * deployments with appropriate defaults and validation.
 */
public class ElasticsearchConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchConfiguration.class);

    // Default configuration values
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 9200;
    public static final String DEFAULT_SCHEME = "http";
    public static final int DEFAULT_CONNECT_TIMEOUT = 5000; // 5 seconds
    public static final int DEFAULT_SOCKET_TIMEOUT = 30000; // 30 seconds
    public static final int DEFAULT_MAX_CONNECTIONS = 100;
    public static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 10;

    // Index configuration
    public static final String MEMORY_INDEX_PREFIX = "headkey-memory";
    public static final String BELIEF_INDEX_PREFIX = "headkey-belief";
    public static final String RELATIONSHIP_INDEX_PREFIX = "headkey-relationship";
    public static final String DEFAULT_INDEX_SUFFIX = "v1";

    private final ElasticsearchClient client;
    private final RestClient restClient;
    private final ElasticsearchConnectionConfig config;

    /**
     * Creates a new Elasticsearch configuration with the given client and config.
     */
    private ElasticsearchConfiguration(ElasticsearchClient client, RestClient restClient,
                                     ElasticsearchConnectionConfig config) {
        this.client = client;
        this.restClient = restClient;
        this.config = config;
    }

    /**
     * Creates a default Elasticsearch configuration for localhost development.
     */
    public static ElasticsearchConfiguration createDefault() {
        return create(ElasticsearchConnectionConfig.builder().build());
    }

    /**
     * Creates an Elasticsearch configuration with the specified connection config.
     */
    public static ElasticsearchConfiguration create(ElasticsearchConnectionConfig config) {
        try {
            logger.info("Creating Elasticsearch configuration with hosts: {}",
                       Arrays.toString(config.getHosts()));

            // Create HTTP hosts
            HttpHost[] httpHosts = Arrays.stream(config.getHosts())
                .map(host -> new HttpHost(host.getHost(), host.getPort(), host.getScheme()))
                .toArray(HttpHost[]::new);

            // Build REST client
            RestClientBuilder builder = RestClient.builder(httpHosts);

            // Configure timeouts
            builder.setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder
                    .setConnectTimeout(config.getConnectTimeoutMs())
                    .setSocketTimeout(config.getSocketTimeoutMs())
            );

            // Configure connection pooling
            builder.setHttpClientConfigCallback(httpClientBuilder -> {
                httpClientBuilder
                    .setMaxConnTotal(config.getMaxConnections())
                    .setMaxConnPerRoute(config.getMaxConnectionsPerRoute());

                // Configure authentication if provided
                if (config.getUsername() != null && config.getPassword() != null) {
                    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(config.getUsername(), config.getPassword()));
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    logger.info("Configured basic authentication for user: {}", config.getUsername());
                }

                return httpClientBuilder;
            });

            RestClient restClient = builder.build();

            // Create Jackson mapper with Java time support
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());

            // Create transport and client
            ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper(mapper));
            ElasticsearchClient client = new ElasticsearchClient(transport);

            // Validate connection
            try {
                client.info();
                logger.info("Successfully connected to Elasticsearch cluster");
            } catch (Exception e) {
                logger.warn("Failed to validate Elasticsearch connection: {}", e.getMessage());
                // Don't fail here - allow the application to start and handle connection issues later
            }

            return new ElasticsearchConfiguration(client, restClient, config);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create Elasticsearch configuration", e);
        }
    }

    /**
     * Gets the Elasticsearch client.
     */
    public ElasticsearchClient getClient() {
        return client;
    }

    /**
     * Gets the underlying REST client.
     */
    public RestClient getRestClient() {
        return restClient;
    }

    /**
     * Gets the connection configuration.
     */
    public ElasticsearchConnectionConfig getConfig() {
        return config;
    }

    /**
     * Gets the memory index name for the specified agent.
     */
    public String getMemoryIndexName(String agentId) {
        return buildIndexName(MEMORY_INDEX_PREFIX, agentId);
    }

    /**
     * Gets the belief index name for the specified agent.
     */
    public String getBeliefIndexName(String agentId) {
        return buildIndexName(BELIEF_INDEX_PREFIX, agentId);
    }

    /**
     * Gets the relationship index name for the specified agent.
     */
    public String getRelationshipIndexName(String agentId) {
        return buildIndexName(RELATIONSHIP_INDEX_PREFIX, agentId);
    }

    /**
     * Builds an index name with the given prefix and agent ID.
     */
    private String buildIndexName(String prefix, String agentId) {
        String sanitizedAgentId = agentId != null ?
            agentId.toLowerCase().replaceAll("[^a-z0-9-]", "-") : "default";
        return String.format("%s-%s-%s", prefix, sanitizedAgentId, DEFAULT_INDEX_SUFFIX);
    }

    /**
     * Checks if the Elasticsearch cluster is healthy and accessible.
     */
    public boolean isHealthy() {
        try {
            client.info();
            return true;
        } catch (Exception e) {
            logger.debug("Health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Closes the Elasticsearch client and releases resources.
     */
    public void close() {
        try {
            if (restClient != null) {
                restClient.close();
                logger.info("Elasticsearch client closed successfully");
            }
        } catch (Exception e) {
            logger.warn("Error closing Elasticsearch client: {}", e.getMessage());
        }
    }

    /**
     * Configuration class for Elasticsearch connection parameters.
     */
    public static class ElasticsearchConnectionConfig {

        private final ElasticsearchHost[] hosts;
        private final String username;
        private final String password;
        private final String apiKey;
        private final int connectTimeoutMs;
        private final int socketTimeoutMs;
        private final int maxConnections;
        private final int maxConnectionsPerRoute;
        private final boolean enableSniffer;
        private final int snifferIntervalMs;

        private ElasticsearchConnectionConfig(Builder builder) {
            this.hosts = builder.hosts.length > 0 ? builder.hosts :
                new ElasticsearchHost[]{new ElasticsearchHost(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_SCHEME)};
            this.username = builder.username;
            this.password = builder.password;
            this.apiKey = builder.apiKey;
            this.connectTimeoutMs = builder.connectTimeoutMs;
            this.socketTimeoutMs = builder.socketTimeoutMs;
            this.maxConnections = builder.maxConnections;
            this.maxConnectionsPerRoute = builder.maxConnectionsPerRoute;
            this.enableSniffer = builder.enableSniffer;
            this.snifferIntervalMs = builder.snifferIntervalMs;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public ElasticsearchHost[] getHosts() { return hosts; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getApiKey() { return apiKey; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public int getSocketTimeoutMs() { return socketTimeoutMs; }
        public int getMaxConnections() { return maxConnections; }
        public int getMaxConnectionsPerRoute() { return maxConnectionsPerRoute; }
        public boolean isEnableSniffer() { return enableSniffer; }
        public int getSnifferIntervalMs() { return snifferIntervalMs; }

        /**
         * Builder for ElasticsearchConnectionConfig.
         */
        public static class Builder {
            private ElasticsearchHost[] hosts = new ElasticsearchHost[0];
            private String username;
            private String password;
            private String apiKey;
            private int connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT;
            private int socketTimeoutMs = DEFAULT_SOCKET_TIMEOUT;
            private int maxConnections = DEFAULT_MAX_CONNECTIONS;
            private int maxConnectionsPerRoute = DEFAULT_MAX_CONNECTIONS_PER_ROUTE;
            private boolean enableSniffer = false;
            private int snifferIntervalMs = 30000; // 30 seconds

            public Builder hosts(ElasticsearchHost... hosts) {
                this.hosts = hosts != null ? hosts : new ElasticsearchHost[0];
                return this;
            }

            public Builder host(String host, int port) {
                return host(host, port, DEFAULT_SCHEME);
            }

            public Builder host(String host, int port, String scheme) {
                return hosts(new ElasticsearchHost(host, port, scheme));
            }

            public Builder authentication(String username, String password) {
                this.username = username;
                this.password = password;
                return this;
            }

            public Builder apiKey(String apiKey) {
                this.apiKey = apiKey;
                return this;
            }

            public Builder timeouts(int connectTimeoutMs, int socketTimeoutMs) {
                this.connectTimeoutMs = connectTimeoutMs;
                this.socketTimeoutMs = socketTimeoutMs;
                return this;
            }

            public Builder connectionPooling(int maxConnections, int maxConnectionsPerRoute) {
                this.maxConnections = maxConnections;
                this.maxConnectionsPerRoute = maxConnectionsPerRoute;
                return this;
            }

            public Builder enableSniffer(boolean enable, int intervalMs) {
                this.enableSniffer = enable;
                this.snifferIntervalMs = intervalMs;
                return this;
            }

            public ElasticsearchConnectionConfig build() {
                return new ElasticsearchConnectionConfig(this);
            }
        }
    }

    /**
     * Represents an Elasticsearch host configuration.
     */
    public static class ElasticsearchHost {
        private final String host;
        private final int port;
        private final String scheme;

        public ElasticsearchHost(String host, int port, String scheme) {
            this.host = Objects.requireNonNull(host, "Host cannot be null");
            this.port = port;
            this.scheme = Objects.requireNonNull(scheme, "Scheme cannot be null");

            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535");
            }
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new IllegalArgumentException("Scheme must be 'http' or 'https'");
            }
        }

        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getScheme() { return scheme; }

        @Override
        public String toString() {
            return scheme + "://" + host + ":" + port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ElasticsearchHost that = (ElasticsearchHost) o;
            return port == that.port &&
                   Objects.equals(host, that.host) &&
                   Objects.equals(scheme, that.scheme);
        }

        @Override
        public int hashCode() {
            return Objects.hash(host, port, scheme);
        }
    }
}
