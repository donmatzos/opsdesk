# Frontend (draft)
- Angular (version pinned in plan), standalone components, signals, `HttpClient`.
- Views: services overview, incident list/detail, "Ask the runbooks" (RAG) box; phase 7 adds a project guide (architecture tour, skills, glossary with sources; content from `docs/guide/*.md`) and a static GitHub Pages build with an in-browser API mock.
- Build: frontend-maven-plugin installs Node (`frontend/node/`, git-ignored), runs `npm ci`, `npm run build` (output straight to `target/classes/META-INF/resources`, see `angular.json`), `npm test -- --watch=false` (Vitest). The `backend` jar nests the `frontend` jar.
- **New Angular route -> also add it to `backend/.../config/SpaForwardController`** (deep-link refresh), covered by `SpaRoutingTest`.
- API types live next to their service (`catalog/catalog-api.ts`); components are standalone, data via `toSignal`.
- Dev: `ng serve` with proxy to `localhost:8080`.
