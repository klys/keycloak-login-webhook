# Keycloak Login Webhook Provider

This project provides a Keycloak `26.4.7` event listener SPI that sends an HTTP `POST` webhook after a user successfully logs in.

It is pinned to Java `17` for both compilation target and build runtime.

## What it sends

The provider listens for `EventType.LOGIN` and posts JSON like this:

```json
{
  "eventId": "0b89fe4c-6b57-49d0-8c8c-97dcb6c1f6ef",
  "eventType": "LOGIN",
  "timestamp": 1771024225123,
  "timestampIso": "2026-02-14T13:23:45.123Z",
  "realmId": "c6cebf7c-6906-4ef9-a24b-a5d4b4ae5376",
  "realmName": "myrealm",
  "clientId": "account-console",
  "userId": "f12cc534-017c-4725-8f1f-0d3197faa16a",
  "username": "alice",
  "sessionId": "67386316-b8db-492c-b0f2-f6ee9284b2c0",
  "ipAddress": "203.0.113.25",
  "error": null,
  "details": {
    "auth_method": "openid-connect",
    "auth_type": "code",
    "code_id": "12f1144f-190a-4790-a4f0-3834f77ecf4d",
    "redirect_uri": "https://app.example.com/callback",
    "username": "alice"
  }
}
```

The webhook is queued with Keycloak's `EventListenerTransaction`, so the outbound call only happens after the login transaction commits successfully.

## Build

Run from WSL:

```bash
mvn clean package
```

You need JDK `17` available on `JAVA_HOME` or on your shell path.

The JAR will be created under `target/login-webhook-event-listener-1.0.0-SNAPSHOT.jar`.

## Install in Keycloak

1. Copy the built JAR into your Keycloak `providers/` directory.
2. Rebuild Keycloak:

```bash
bin/kc.sh build \
  --spi-events-listener--login-webhook--url=https://example.com/keycloak/login \
  --spi-events-listener--login-webhook--shared-secret=super-secret \
  --spi-events-listener--login-webhook--secret-header=X-Webhook-Secret \
  --spi-events-listener--login-webhook--connect-timeout-millis=2000 \
  --spi-events-listener--login-webhook--request-timeout-millis=5000 \
  --spi-events-listener--login-webhook--include-details=true
```

3. Start Keycloak.
4. In the admin console, open your realm.
5. Go to `Realm settings` -> `Events` -> `Config`.
6. Add `login-webhook` to the `Event listeners` list and save.

## Notes

- The provider does not send webhooks for failed logins, only successful `LOGIN` events.
- If the webhook endpoint returns HTTP `>= 400` or the request fails, the error is logged and the user login is not rolled back.
- If `url` is not configured, the listener stays loaded but skips sending requests.
