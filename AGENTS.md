# Repository Guidelines
## Project Structure & Module Organization
- Root `docker-compose*.yml` plus `start-dev.bat` bring up monitoring, Kong, CDC, and shop demos.
- `ojp/` hosts the Maven modules (`ojp-server`, `ojp-cache`, `ojp-server-common`, `ojp-grpc-commons`, `ojp-jdbc-driver`) with module docs/logs alongside source.
- `ojp/ojp-ui` delivers the React/Vite portal; infra manifests live in `docker/`, `.devcontainer/`, and `k8s/`, with operational notes in `docs/` and endpoints in `PORTAL.md`.

## Build, Test, and Development Commands
- `start-dev.bat [-build|-clean|-logs|-compile]` runs the dev compose overlays; mix flags to rebuild images, prune, or stream logs.
- `docker-compose --profile dev up -d` is CI-friendly; add `-f docker-compose-cdc-sync.yml` (or similar) to limit services.
- `cd ojp && mvnd install -DskipTests` builds all backend modules; use `mvnd -pl ojp-server test` for targeted cycles.
- `cd ojp/ojp-ui && npm run dev` starts the UI; `npm run dev:full` adds the proxy layer and `npm run build` produces the bundle.

## Coding Style & Naming Conventions
- Java uses 4-space indents, Lombok helpers, and `org.openjdbcproxy.*` packages mirroring feature directories; keep configuration in YAML under `src/main/resources`.
- React sticks to 2-space indents, single quotes, functional components, and PascalCase filenames; shared helpers belong in `src/services` or `src/config`.
- Run `npm run lint` before pushing UI work and keep credentials in `.env` overlays that stay untracked.

## Testing Guidelines
- Backend testing relies on JUnit 5 via `spring-boot-starter-test`; mirror `src/main` packages in `src/test` and name cases around behavior (`shouldHandleBulkConnect`).
- Execute suites with `cd ojp && mvnd test`; add Testcontainers only when compose exposes the dependent services.
- Frontend automation is pending—ensure `npm run lint` and `npm run build` pass and capture manual checks (Grafana embeds, Kong routes) in each PR.

## Commit & Pull Request Guidelines
- Commits follow history: short imperative lines (`添加grafana配置 支持iframe嵌入`) without trailing punctuation.
- PRs should outline scope, touched compose files or services, config changes, and attach screenshots or GIFs for UI-facing work.
- Request reviewers for every affected area and track follow-ups (migrations, new routes) in a checklist.

## Environment & Operations Notes
- Keep WSL routing guidance in `README.md` accurate when networks shift and refresh `PORTAL.md` endpoints accordingly.
- Document new services or credentials in `docs/`; store secrets in external `.env` files consumed by compose profiles.
- After removing services run `docker-compose ... down --remove-orphans` and update `start-dev.bat` if profiles or flags change.
