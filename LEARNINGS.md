# LEARNINGS.md

Codebase-specific patterns, gotchas, and useful knowledge accumulated over time.
Append new entries at the bottom under the appropriate date/session.

---

## 2026-03-18

- `CHANGELOG.md` uses an `## Unreleased` section at the top (no version/date) for in-progress work. Versioned releases follow below it (e.g. `## 0.2.23`). Each entry is a bullet with a **Bold Title:** followed by a short description.
- The test suite entry point is `./tests/test.sh`. A lightweight alternative is `./tests/test-simple.sh`. Always run the full suite before and after making changes.
- `./dev-lucli.sh` is the fast development loop — it runs LuCLI via `mvn exec:java` without rebuilding the JAR, so changes to source are picked up quickly for manual testing.
- The `#env:VAR#` syntax (not `${VAR}`) is the preferred variable substitution in `lucee.json`. `${VAR}` is reserved for Lucee/JVM runtime resolution and is intentionally left untouched in protected zones (`configuration`, `jvm.additionalArgs`).

## 2026-03-20

- BATS migration is being introduced in parallel via `tests/bats/` and `tests/test-bats.sh`; existing shell suites remain the source of truth until parity is reached.
- The current safe conversion target for BATS is deterministic tests (help/version/error code and `server start --dry-run` assertions) that avoid starting long-running server processes.
- A reliable second-pass BATS target is module and run-command smoke behavior (`modules help/list/init`, `.cfm` run, `.cfc` run-blocking, `.lucli` shortcut/run) plus `--config` dry-run selection tests.
- `.sdkmanrc` values can include trailing whitespace; trim parsed `java=` values in scripts before resolving `${HOME}/.sdkman/candidates/java/<version>` to avoid false toolchain mismatches.
- `tests/test-bats.sh` should own BATS-native JUnit output (`--report-formatter junit`) and publish a stable artifact path (`test-bats-results.xml` or `BATS_JUNIT_XML_OUTPUT`) for CI consumers.
- Keep BATS integration in `tests/test-all.sh` opt-in (`RUN_BATS=true`) during migration to avoid changing default runtime/coverage expectations for existing pipelines.
- In strict shell scripts (`set -euo pipefail`), avoid `sdk env` in hooks; prefer parsing `.sdkmanrc` and exporting `JAVA_HOME/PATH` directly to prevent silent early exits from SDKMAN shell functions.
