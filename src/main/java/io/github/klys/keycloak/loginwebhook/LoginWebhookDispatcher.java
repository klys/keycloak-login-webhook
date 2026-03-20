package io.github.klys.keycloak.loginwebhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.jboss.logging.Logger;

final class LoginWebhookDispatcher {

    private static final Logger LOGGER = Logger.getLogger(LoginWebhookDispatcher.class);

    private final LoginWebhookConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    LoginWebhookDispatcher(LoginWebhookConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .build();
        this.objectMapper = new ObjectMapper();
    }

    void dispatch(Map<String, Object> payload) {
        if (!config.isConfigured()) {
            LOGGER.debug("Login webhook skipped because no webhook URL is configured");
            return;
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(config.webhookUri())
                .timeout(config.requestTimeout())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(serialize(payload)));

        if (config.sharedSecret() != null) {
            builder.header(config.secretHeader(), config.sharedSecret());
        }

        httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.discarding())
                .thenAccept(response -> {
                    if (response.statusCode() >= 400) {
                        LOGGER.warnf(
                                "Login webhook returned HTTP %d for realm=%s userId=%s",
                                response.statusCode(),
                                payload.get("realmName"),
                                payload.get("userId"));
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.errorf(
                            throwable,
                            "Failed to deliver login webhook for realm=%s userId=%s",
                            payload.get("realmName"),
                            payload.get("userId"));
                    return null;
                });
    }

    private String serialize(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize login webhook payload", exception);
        }
    }
}
