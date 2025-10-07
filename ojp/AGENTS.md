# Repository Guidelines

## Project Structure & Module Organization
Root `docker-compose*.yml` manifests and `start-dev.bat` coordinate monitoring, Kong, CDC, and demo services. Backend modules live under `ojp/` (`ojp-server`, `ojp-cache`, `ojp-server-common`, `ojp-grpc-commons`, `ojp-jdbc-driver`) with docs and logs beside sources. The React portal resides in `ojp/ojp-ui`; infrastructure manifests sit in `docker/`, `.devcontainer/`, and `k8s/`, with operational notes in `docs/` and endpoint references in `PORTAL.md`.

## Build, Test, and Development Commands
Use `start-dev.bat [-build|-clean|-logs|-compile]` from the repo root to manage the dev stack; mix flags to rebuild images or stream logs. `docker-compose --profile dev up -d` spins services locally or in CI, and add `-f docker-compose-cdc-sync.yml` when only CDC sync is needed. Backend builds run via `cd ojp && mvnd install -DskipTests`; target a module with `mvnd -pl ojp-server test`. For the UI, `cd ojp/ojp-ui && npm run dev` serves the portal, `npm run dev:full` enables the proxy, and `npm run build` produces the deployable bundle.

## Coding Style & Naming Conventions
Java modules use 4-space indents, Lombok helpers, and `org.openjdbcproxy.*` packages aligned with feature directories; keep configuration in YAML under `src/main/resources`. React components use 2-space indents, single quotes, functional components, and PascalCase filenames. Shared front-end utilities belong in `src/services` or `src/config`, and `npm run lint` must pass before publishing changes. Store credentials in untracked `.env` overlays.

## Testing Guidelines
JUnit 5 powers backend tests through `spring-boot-starter-test`; mirror `src/main` packages in `src/test` and name methods after behaviors (e.g., `shouldHandleBulkConnect`). Execute suites with `cd ojp && mvnd test`, introducing Testcontainers only when compose exposes the required services. Frontend automation is pending—ensure `npm run lint` and `npm run build` succeed and capture manual checks for Grafana embeds or Kong routes in review notes.

## Commit & Pull Request Guidelines
Commit messages follow the existing short, imperative style (e.g., `添加grafana配置 支持iframe嵌入`) without trailing punctuation. PRs should enumerate scope, affected compose files or services, configuration changes, and include screenshots or GIFs for UI updates. Request reviewers per affected area and track follow-up items (migrations, new routes) in a checklist.

## Security & Configuration Tips
Document new services or credentials in `docs/` and keep secrets in external `.env` files consumed by compose profiles. Update `README.md` with WSL routing guidance whenever network mappings change, and refresh `PORTAL.md` after endpoint adjustments. After removing services, run `docker-compose ... down --remove-orphans` and reflect profile changes in `start-dev.bat`.
