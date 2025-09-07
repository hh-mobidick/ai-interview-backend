#!/usr/bin/env bash
set -euo pipefail

# Build + run helper for ai-interview-backend

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
CONTAINER_NAME="ai-interviewer-postgres"
DB_URL="jdbc:postgresql://localhost:5432/ai_interviewer"
DB_USERNAME="ai"
DB_PASSWORD="ai"

echo "[1/5] Checking prerequisites..."
if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: Docker is required. Install Docker Desktop first: https://www.docker.com/products/docker-desktop" >&2
  exit 1
fi

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  echo "ERROR: docker compose (v2) or docker-compose (v1) is required." >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: Java runtime is required (Java 21 recommended)." >&2
  exit 1
fi

echo "[2/5] Starting Postgres via Docker Compose..."
cd "$PROJECT_ROOT"
"${COMPOSE_CMD[@]}" up -d postgres

echo "[3/5] Waiting for Postgres to become healthy..."
MAX_ATTEMPTS=60
SLEEP_SECONDS=2
attempt=1
until [[ "$(docker inspect -f '{{.State.Health.Status}}' "$CONTAINER_NAME" 2>/dev/null || echo starting)" == "healthy" ]]; do
  if (( attempt > MAX_ATTEMPTS )); then
    echo "\nERROR: Postgres did not become healthy in time. Check container logs: docker logs $CONTAINER_NAME" >&2
    exit 1
  fi
  printf "."
  sleep "$SLEEP_SECONDS"
  ((attempt++))
done
echo " healthy"

echo "[4/5] Building the application (this may take a while on first run)..."
chmod +x ./gradlew
./gradlew clean build

JAR_FILE="build/libs/ai-interviewer-0.0.1-SNAPSHOT.jar"
if [[ ! -f "$JAR_FILE" ]]; then
  echo "ERROR: Jar file not found at $JAR_FILE. Check the Gradle build output." >&2
  exit 1
fi

echo "[5/5] Running the application..."
export DB_URL
export DB_USERNAME
export DB_PASSWORD

if [[ -z "${OPENAI_API_KEY:-}" ]]; then
  echo "WARNING: OPENAI_API_KEY is not set. LLM features may not work until you export it." >&2
fi

PROXY_ARG=""
for arg in "$@"; do
  case "$arg" in
    --proxy|proxy=true)
      PROXY_ARG="--proxy=true"
      ;;
    --no-proxy|proxy=false)
      PROXY_ARG="--proxy=false"
      ;;
  esac
done

echo "App starting with DB_URL=$DB_URL ${PROXY_ARG:+and proxy enabled}" 
echo "Press Ctrl+C to stop the application. Postgres will remain running in Docker."

java -jar "$JAR_FILE" $PROXY_ARG


