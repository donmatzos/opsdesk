# Conventions (draft)
- Java 25: records for DTOs, sealed types where useful, no Lombok. Constructor injection only.
- Packages by feature (`incident`, `service`), not by layer.
- Config via `application.yml` + env vars; never a secret in a file, including local dev passwords and `${VAR:default}` fallbacks for credentials (`ApplicationConfigTest` enforces it for `application.yml`).
- Mocked cloud code: header comment `// MOCK: <real thing> — <why mocked>, see docs/context/cloud-mocks.md`.
- TypeScript: strict mode, standalone components, signals; no `any`.
- YAML/Helm/Terraform: formatted (`helm lint`, `terraform fmt`).
- Comments explain why, not what. Small, reviewable commits.

## Issue-proof code (SonarCloud found 37 issues in phase 6; avoid them up front)
- No nested ternaries: extract a small method or `if`. No unused imports, parameters, locals or catch variables (`catch (X _)`).
- Repeated string literals (3+) become a constant. Local variables, fields and parameters are camelCase; fields must not differ from a constant only by case (`ACTIVE` vs `active`).
- Functional types: `UnaryOperator<T>` instead of `Function<T, T>`, `BinaryOperator` likewise.
- Regex: group alternations with anchors `(^-)|(-$)`, no reluctant `.*?` (use a possessive/negated class or a loop). Prefer a small loop over a clever regex.
- Spring: never call a `@Transactional`/`@Cacheable` method through `this` (extract a private helper both use). `@GetMapping` templates like `{id}` need a bound parameter; for forwards use `*`.
- Text blocks only for multi-line strings. Do not use APIs marked deprecated (check the IDE/compiler hint, Angular `withFetch()` is default now).
- Suppressions (`@SuppressWarnings("java:Sxxxx")`, `// NOSONAR`, marking an issue in the Sonar UI) are NEVER applied in advance. Only after a Sonar scan has reported the issue AND the issue has been discussed and reviewed with the owner. Then the suppression carries a comment saying why (e.g. MCP tool parameter names are the wire names and stay snake_case).
- GitHub Actions/shell: `curl` always `--proto '=https' --tlsv1.2`; pin actions by SHA.

