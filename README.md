# Home Assistant AI API

CasaOS-ready Spring Boot API for exposing Home Assistant data to Open WebUI/GPT/Ollama.

## Deploy

Update `docker-compose.casaos.yml`:

```yaml
HA_URL: "http://YOUR_HOME_ASSISTANT_IP:8123"
HA_TOKEN: "YOUR_LONG_LIVED_ACCESS_TOKEN"
```

Then deploy in CasaOS as a Custom App, or run:

```bash
docker compose -f docker-compose.casaos.yml up -d --build
```

Open Swagger:

```text
http://SERVER_IP:8085/swagger-ui.html
```

Open the proposal UI:

```text
http://SERVER_IP:8085/
```

## Option A behavior

`/api/automation/apply` does not directly edit Home Assistant internals. It approves the proposal and returns the YAML plus manual import steps.
