# Frontend (draft)
- Angular (version pinned in plan), standalone components, signals, `HttpClient`.
- Views: services overview, incident list/detail, "Ask the runbooks" (RAG) box.
- Build: frontend-maven-plugin installs Node/npm locally, runs `npm ci`, tests, `ng build`; output copied into the `frontend` jar under `META-INF/resources/` so Spring Boot serves it. SPA fallback route in backend.
- Dev: `ng serve` with proxy to `localhost:8080`.
