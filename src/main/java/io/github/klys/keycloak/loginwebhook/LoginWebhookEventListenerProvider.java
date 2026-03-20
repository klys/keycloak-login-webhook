package io.github.klys.keycloak.loginwebhook;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;

public final class LoginWebhookEventListenerProvider implements EventListenerProvider {

    private final EventListenerTransaction transaction;
    private final LoginWebhookDispatcher dispatcher;
    private final LoginWebhookConfig config;

    public LoginWebhookEventListenerProvider(KeycloakSession session, LoginWebhookDispatcher dispatcher, LoginWebhookConfig config) {
        this.dispatcher = dispatcher;
        this.config = config;
        this.transaction = new EventListenerTransaction(this::handleAdminEvent, this::handleCommittedEvent);
        session.getTransactionManager().enlistAfterCompletion(transaction);
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() == EventType.LOGIN) {
            transaction.addEvent(event.clone());
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        transaction.addAdminEvent(adminEvent, includeRepresentation);
    }

    @Override
    public void close() {
    }

    private void handleCommittedEvent(Event event) {
        dispatcher.dispatch(buildPayload(event));
    }

    private void handleAdminEvent(AdminEvent adminEvent, Boolean includeRepresentation) {
        // This provider only cares about user login events.
    }

    private Map<String, Object> buildPayload(Event event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", event.getId());
        payload.put("eventType", event.getType().name());
        payload.put("timestamp", event.getTime());
        payload.put("timestampIso", Instant.ofEpochMilli(event.getTime()).toString());
        payload.put("realmId", event.getRealmId());
        payload.put("realmName", event.getRealmName());
        payload.put("clientId", event.getClientId());
        payload.put("userId", event.getUserId());
        payload.put("username", event.getDetails() == null ? null : event.getDetails().get("username"));
        payload.put("sessionId", event.getSessionId());
        payload.put("ipAddress", event.getIpAddress());
        payload.put("error", event.getError());

        if (config.includeDetails()) {
            payload.put("details", event.getDetails());
        }

        return payload;
    }
}
