# AI Chat Web App

Simple chat app for a coding challenge. You type a message, the backend sends it to an OpenAI-compatible API, and the reply shows up without reloading the page.

## Stack

- **Backend:** Java 25 + Spring Boot 3.5. Uses `WebClient` only to call the upstream chat API; the rest is normal Spring MVC.
- **Frontend:** Plain HTML / CSS / JS, served from Spring Boot static resources (no React/Vue build).
- **Docker:** One multi-stage image, one container. `compose.yaml` for local runs.
- **CI:** GitHub Actions builds the project and pushes a tagged image to GHCR on `main`.

## Layout

```
src/main/java/com/raghad/aichat/
  AiChatApplication.java
  controller/ChatController.java   # POST /api/chat
  service/OpenAiChatService.java   # upstream call
  config/                          # WebClient, properties, rate limit
  dto/                             # request / response / error shapes
  exception/                       # shared error handling
src/main/resources/
  application.properties
  static/                          # index.html, app.js, style.css
.github/workflows/ci.yml
Dockerfile
compose.yaml
.env.example
```

## Environment variables

Copy `.env.example` to `.env` and fill in what you need. Nothing secret is hardcoded.

| Variable          | Required | Default                    | What it does |
|-------------------|----------|----------------------------|--------------|
| `OPENAI_API_KEY`  | No*      | empty                      | Upstream API key |
| `OPENAI_BASE_URL` | No       | `https://api.openai.com/v1`| OpenAI-compatible base URL |
| `OPENAI_MODEL`    | No       | `gpt-4o-mini`              | Model name |
| `OPENAI_TIMEOUT`  | No       | `30s`                      | Upstream timeout (bump this for local models) |
| `CHAT_RATE_LIMIT` | No       | `20`                       | Max chat requests per client per minute |
| `SERVER_PORT`     | No       | `8080`                     | App port |

\* Only needed for providers that require auth (e.g. OpenAI). Leave blank for Ollama / LM Studio.

## Why Ollama (not OpenAI / LM Studio)

I tested against **Ollama** as the main free path:

- **OpenAI** works, but it’s paid — fine for demos with a key, not ideal if you just want to verify the integration.
- **LM Studio** is OpenAI-compatible too, but on Apple Silicon only (M1 / M2 / M3). My machine is Intel, so that wasn’t an option.
- **Ollama** runs locally, is free, and exposes the same `/v1/chat/completions` shape — so the app code stays the same.

Switching providers is only env vars. No code changes.

## Run with Docker Compose (recommended)

```bash
cp .env.example .env
# edit .env — for Ollama on Docker Desktop (Mac/Windows):
# OPENAI_BASE_URL=http://host.docker.internal:11434/v1
# OPENAI_MODEL=llama3.2
# OPENAI_API_KEY=
# OPENAI_TIMEOUT=180s

docker compose up --build
```

Open http://localhost:8080

Handy commands:

```bash
docker compose up --build -d
docker compose logs -f app
docker compose down
```

Without Compose:

```bash
docker build -t ai-chat-app .
docker run --env-file .env -p 8080:8080 ai-chat-app
```

## Run locally (no Docker)

Needs JDK 25 + Maven.

```bash
set -a
source .env
set +a
mvn spring-boot:run
```

Or export vars one by one and run `mvn spring-boot:run`. Spring Boot does **not** load `.env` by itself.

## Pointing at Ollama / LM Studio / OpenAI

The backend always calls `{OPENAI_BASE_URL}/chat/completions`.

### Ollama (what I used)

1. Install [Ollama](https://ollama.com/) and pull a model: `ollama pull llama3.2`
2. Set in `.env`:
   ```env
   OPENAI_API_KEY=
   OPENAI_BASE_URL=http://host.docker.internal:11434/v1
   OPENAI_MODEL=llama3.2
   OPENAI_TIMEOUT=180s
   ```
3. If you run the app with Maven (not Docker), use `http://localhost:11434/v1` instead of `host.docker.internal`.

Local models can be slow on CPU — raise `OPENAI_TIMEOUT` if you see gateway timeout errors.

### LM Studio (Apple Silicon only)

1. Start the local server (usually `http://localhost:1234/v1`).
2. Set `OPENAI_BASE_URL` / `OPENAI_MODEL` to match LM Studio.
3. Leave `OPENAI_API_KEY` empty.
4. From Docker on Mac: `http://host.docker.internal:1234/v1`.

### Real OpenAI

```env
OPENAI_API_KEY=sk-...
OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_MODEL=gpt-4o-mini
```

## API

**POST** `/api/chat`

```json
{
  "prompt": "Hello, how are you?",
  "history": [
    { "role": "user", "content": "Hi" },
    { "role": "assistant", "content": "Hello! How can I help?" }
  ]
}
```

`history` is optional (defaults to `[]`).

Success (`200`):

```json
{ "reply": "I'm doing well, thanks for asking!" }
```

Error (`400` / `429` / `502` / `504`, etc.):

```json
{
  "error": "UPSTREAM_ERROR",
  "message": "Upstream API rate limit exceeded. Please try again shortly.",
  "timestamp": "2026-07-16T10:00:00Z"
}
```

## Assumptions / trade-offs

- Frontend lives in the same Spring Boot app on purpose — one container, simpler CI, good enough for the challenge.
- Conversation history is kept in the browser and sent back on each request. Backend stays stateless; refresh clears the chat.
- No automatic retries on chat POSTs. Retrying can double-charge or duplicate work; the UI shows the error and the user can send again.
- Light in-memory rate limit on `/api/chat` (default 20/min per client). Fine for a single instance; not shared across replicas.
- Java 25 Temurin images in Docker, with Maven installed in the build stage (no official Maven+25 image yet).

## Tests

```bash
mvn test
```

Covers validation on `/api/chat` and a few upstream error paths (mocked — no real API key needed). Docker builds also run the tests.

## If I had more time

- Stream replies (SSE) instead of waiting for the full answer.
- Shared rate limiting (e.g. Redis) if this ever ran as more than one instance.
