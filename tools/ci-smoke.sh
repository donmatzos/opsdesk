#!/usr/bin/env bash
# Start an image next to a throwaway PostgreSQL and smoke-test it the way OpenShift runs it:
# random UID in group 0, read-only root filesystem, writable /tmp only.
# Usage: tools/ci-smoke.sh <image>        The DB password is generated per run and never stored.
set -euo pipefail

image=${1:?usage: ci-smoke.sh <image>}
db_secret=$(openssl rand -hex 16)
[[ -n ${GITHUB_ACTIONS:-} ]] && echo "::add-mask::${db_secret}"

cleanup() { docker rm -f smoke-app smoke-db >/dev/null 2>&1 || true; docker network rm smoke >/dev/null 2>&1 || true; }
trap cleanup EXIT
cleanup

docker network create smoke >/dev/null
docker run -d --name smoke-db --network smoke \
  -e POSTGRES_DB=opsdesk -e POSTGRES_USER=opsdesk -e POSTGRES_PASSWORD="${db_secret}" \
  postgres:18-alpine >/dev/null
until docker exec smoke-db pg_isready -U opsdesk -d opsdesk >/dev/null 2>&1; do sleep 1; done

docker run -d --name smoke-app --network smoke -p 8080:8080 \
  --user 123456:0 --read-only --tmpfs /tmp \
  -e SPRING_PROFILES_ACTIVE=demo \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://smoke-db:5432/opsdesk \
  -e SPRING_DATASOURCE_USERNAME=opsdesk \
  -e SPRING_DATASOURCE_PASSWORD="${db_secret}" \
  "${image}" >/dev/null

"$(dirname "$0")/smoke-test.sh" http://localhost:8080 120 || { docker logs smoke-app | tail -50; exit 1; }
