package io.github.klys.keycloak.loginwebhook;

import java.net.URI;
import java.time.Duration;
import org.keycloak.Config;

final class LoginWebhookConfig {

    static final String URL = "url";
    static final String CONNECT_TIMEOUT_MILLIS = "connect-timeout-millis";
    static final String REQUEST_TIMEOUT_MILLIS = "request-timeout-millis";
    static final String SHARED_SECRET = "shared-secret";
    static final String SECRET_HEADER = "secret-header";
    static final String INCLUDE_DETAILS = "include-details";

    private final URI webhookUri;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final String sharedSecret;
    private final String secretHeader;
    private final boolean includeDetails;

    private LoginWebhookConfig(
            URI webhookUri,
            Duration connectTimeout,
            Duration requestTimeout,
            String sharedSecret,
            String secretHeader,
            boolean includeDetails) {
        this.webhookUri = webhookUri;
        this.connectTimeout = connectTimeout;
        this.requestTimeout = requestTimeout;
        this.sharedSecret = sharedSecret;
        this.secretHeader = secretHeader;
        this.includeDetails = includeDetails;
    }

    static LoginWebhookConfig fromScope(Config.Scope scope) {
        String url = trimToNull(scope.get(URL));
        URI webhookUri = url == null ? null : URI.create(url);

        int connectTimeoutMillis = positive(scope.getInt(CONNECT_TIMEOUT_MILLIS, 2_000), 2_000);
        int requestTimeoutMillis = positive(scope.getInt(REQUEST_TIMEOUT_MILLIS, 5_000), 5_000);

        String configuredSecretHeader = trimToNull(scope.get(SECRET_HEADER, "X-Webhook-Secret"));
        String secretHeader = configuredSecretHeader == null ? "X-Webhook-Secret" : configuredSecretHeader;

        return new LoginWebhookConfig(
                webhookUri,
                Duration.ofMillis(connectTimeoutMillis),
                Duration.ofMillis(requestTimeoutMillis),
                trimToNull(scope.get(SHARED_SECRET)),
                secretHeader,
                scope.getBoolean(INCLUDE_DETAILS, true));
    }

    URI webhookUri() {
        return webhookUri;
    }

    Duration connectTimeout() {
        return connectTimeout;
    }

    Duration requestTimeout() {
        return requestTimeout;
    }

    String sharedSecret() {
        return sharedSecret;
    }

    String secretHeader() {
        return secretHeader;
    }

    boolean includeDetails() {
        return includeDetails;
    }

    boolean isConfigured() {
        return webhookUri != null;
    }

    private static int positive(Integer value, int defaultValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return value;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
