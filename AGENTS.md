# AGENTS.md

## What this repo is
Ktor/JVM POC of the PEPPOL Service Metadata Locator (SML). State lives in SQLite; on participant create/delete/migrate it also pushes RFC2136 + TSIG updates to a BIND9 server. Explicitly not production-ready, and the SOAP endpoints have no auth.

## Commands
- Build: `./gradlew build`
- All tests: `./gradlew test` (Gradle caches results; add `--rerun-tasks` to force a real run)
- Single test class: `./gradlew test --tests "be.endevops.ManageBusinessIdentifierDispatchTest"`
- Single test method: `./gradlew test --tests "be.endevops.ApplicationTest.testRoot"`
- Run server: `./gradlew run` (port 8080)
- Lint: `./gradlew ktlintCheck` / `./gradlew ktlintFormat`. Report-only (`ignoreFailures=true`); the codebase is not currently clean, so don't fix unrelated findings.
- Container: `./gradlew buildFatJar`, `buildImage`, `runDocker` (Ktor plugin tasks).

## Toolchain
- Always use the wrapper `./gradlew`. `mise.toml` pins Gradle 9 / Java 26 and aliases `gradle` -> `./gradlew`; Kotlin targets JVM toolchain 25.
- Plugin versions are set in `build.gradle.kts` (Kotlin `2.4.0-RC`, Ktor plugin `3.5.0`). `kotlin_version` / `ktor_version` in `gradle.properties` only feed a couple of dependencies and can lag behind.

## Test prerequisites (easy to miss)
- `ManageBusinessIdentifierDispatchTest` performs real DNS writes. Test config (`src/test/resources/application.yaml`) points at `127.0.0.1:15354`, which is where `docker compose up -d dns` publishes BIND9. Start it first, or those tests get a 500/fault and fail.
- Publisher-only tests (`ApplicationTest`, `ServiceMetadataDispatchTest`, `ServiceMetadataEndpointsTest`) do not touch DNS.
- Every test starts the app with `testModule()`: throwaway temp SQLite DB + test config. Don't use `module()` in tests.
- CI does not run tests. `.github/workflows/ci.yml` only computes a version and builds/pushes the Docker image, so run `./gradlew test` locally.

## Runtime wiring
- Entrypoint is `be.endevops.ApplicationKt` via Ktor `EngineMain`. `module()` reads `dbfile` from config; `testModule()` injects a temp DB. Both call `configureApplication()` and `configureServices()`.
- `configureApplication()` installs ContentNegotiation (XML only), StatusPages, CallLogging, AutoHeadResponse, then the route modules.
- Routes:
  - `GET /` debug HTML UI listing publishers/participants/migrations.
  - `POST /manage-service-metadata` (alias `/manageservicemetadata`) publisher CRUD SOAP.
  - `POST /manage-business-identifier` (alias `/manageparticipantidentifier`) participant create/delete/list/batch/migrate SOAP.
  - `GET` on both SOAP paths serves the WSDL when `?wsdl` is present.
- DI is Ktor's `io.ktor.server.plugins.di`: services are provided in `configureServices()` and injected with `by dependencies` or constructor `@Property("dns.*")`. Register new services there.
- SOAP dispatch picks the operation from the first child of `soap:Body` (its `localName`), falling back to the `SOAPAction` header.
- Faults are typed `FaultError` carrying the SML-mandated HTTP status (400 bad request, 404 not found, 500 internal). Successes and faults are always serialized as a `Soap11Envelope`; routes never hand-build XML or return plain strings.

## Gotchas
- XML uses pdvrieze `xmlutil` (`nl.adaptivity.xmlutil.*`), not kotlinx XML. The shared format instance is `APPLICATION_XML` in `Application.kt`.
- XML request/response POJOs are hand-written under `src/main/kotlin/xml/`. The WSDL/XSD under `src/main/resources/wsdl/` are reference material and `?wsdl` payloads, not codegen input; update the POJOs manually when the schema changes.
- DB layer uses Exposed v1 (`org.jetbrains.exposed.v1.*`); package paths differ from most Exposed examples. Tables are created in each service's `init` with `SchemaUtils.create` and there is no migration tool, so schema changes need manual handling of existing SQLite files.
- Config keys are `SML_*` (`SML_DB_FILE`, `SML_DNS_SERVER/PORT/ZONE/DOMAIN/KEY_NAME/KEY_SECRET`) in `application.yaml`. `compose.yml` and `Dockerfile` set `DNS_*` / `DB_FILE`, which the app ignores (defaults happen to work); don't propagate those names.
- Versioning is GitVersion with GitFlow (`master` = release, `develop` = beta, `feature/*`, `hotfix/*`) and conventional-commit prefixes drive bumps. CI triggers on pushes to `master` / `develop`.

## Style
- Kotlin official style, with `.editorconfig` overrides: `ktlint_official`, `max_line_length=140`, wildcard imports disabled.
- Compiler flags in `build.gradle.kts`: `-Xannotation-default-target=param-property` and `-Xreturn-value-checker=check` (don't silently drop meaningful return values).
- Local runtime artifacts `data/` and `kls_database.db` are gitignored; don't commit SQLite files.
