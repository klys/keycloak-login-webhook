package io.github.klys.keycloak.loginwebhook;

import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public final class LoginWebhookEventListenerProviderFactory implements EventListenerProviderFactory {

    public static final String ID = "login-webhook";

    private static final Logger LOGGER = Logger.getLogger(LoginWebhookEventListenerProviderFactory.class);

    private volatile LoginWebhookConfig config;
    private volatile LoginWebhookDispatcher dispatcher;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new LoginWebhookEventListenerProvider(session, dispatcher, config);
    }

    @Override
    public void init(Config.Scope scope) {
        this.config = LoginWebhookConfig.fromScope(scope);
        this.dispatcher = new LoginWebhookDispatcher(config);

        if (config.isConfigured()) {
            LOGGER.infov("Login webhook listener configured for {0}", config.webhookUri());
        } else {
            LOGGER.warn("Login webhook listener is enabled but no webhook URL is configured");
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        List<ProviderConfigProperty> properties = new ArrayList<>();
        properties.add(new ProviderConfigProperty(
                LoginWebhookConfig.URL,
                "Webhook URL",
                "HTTPS endpoint that receives a POST request after a successful LOGIN event is committed.",
                ProviderConfigProperty.URL_TYPE,
                null));
        properties.add(new ProviderConfigProperty(
                LoginWebhookConfig.CONNECT_TIMEOUT_MILLIS,
                "Connect timeout (ms)",
                "Connection timeout used by the outbound webhook client.",
                ProviderConfigProperty.INTEGER_TYPE,
                2000));
        properties.add(new ProviderConfigProperty(
                LoginWebhookConfig.REQUEST_TIMEOUT_MILLIS,
                "Request timeout (ms)",
                "Overall timeout for the outbound webhook request.",
                ProviderConfigProperty.INTEGER_TYPE,
                5000));
        properties.add(new ProviderConfigProperty(
                LoginWebhookConfig.SECRET_HEADER,
                "Secret header",
                "Header name used when sending the shared secret.",
                ProviderConfigProperty.STRING_TYPE,
                "X-Webhook-Secret"));
        properties.add(new ProviderConfigProperty(
                LoginWebhookConfig.SHARED_SECRET,
                "Shared secret",
                "Optional secret value sent with each webhook request.",
                ProviderConfigProperty.PASSWORD,
                null,
                true));
        properties.add(new ProviderConfigProperty(
                LoginWebhookConfig.INCLUDE_DETAILS,
                "Include details",
                "Include the Keycloak event details map in the JSON payload.",
                ProviderConfigProperty.BOOLEAN_TYPE,
                true));
        return properties;
    }
}
