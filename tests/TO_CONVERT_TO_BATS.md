# TO_CONVERT_TO_BATS
Checklist of high-value tests from `tests/test-old.sh` that are not yet covered in `tests/bats/`.
Use this file to track migration progress.

## Priority 1 (best next tests; deterministic and BATS-friendly)
- [x] `modules install without URL fails`
- [x] `Run module via modules run`
- [x] `server start --dry-run --include-lucee` shows CFConfig preview
- [x] `server start --dry-run --include-tomcat-server` shows `server.xml`
- [x] `server start --dry-run --include-tomcat-web` shows `web.xml`
- [x] `server start --dry-run --include-https-keystore-plan` shows keystore plan
- [x] `server start --dry-run --include-https-redirect-rules` shows redirect rules
- [x] `server start --dry-run --include-all` shows combined previews
- [x] Environment deep-merge assertion: prod shows password override
- [x] Environment deep-merge assertion: dev keeps base `jvm.maxMemory`
- [x] Environment deep-merge assertion: staging overrides `jvm.maxMemory`
- [x] Environment deep-merge assertion: staging keeps base `jvm.minMemory`
- [x] Environment deep-merge assertion: base `admin.enabled` preserved in prod
- [x] Computed dependency mappings in dry-run CFConfig include `/framework/`
- [x] Computed dependency mappings in dry-run CFConfig include `/lib/`
- [x] Computed dependency mappings include physical path `dependencies/fw1`
- [x] Computed dependency mappings include physical path `dependencies/testlib`
- [x] `run.cfm executes`
- [x] `run --whitespace` preserves additional whitespace vs default
- [x] `server --help` smoke test
- [x] `server list` smoke test
- [x] `server monitor --help` smoke test
- [x] Binary/JAR version parity (`--version` values match)

## Priority 2 (larger module lifecycle coverage)
- [ ] Install module from local zip URL
- [ ] Install module with `--name` alias
- [ ] Verify installed module directory and `module.json`
- [ ] Verify alias module directory
- [ ] Update module using stored URL
- [ ] Update alias module using stored URL
- [ ] Uninstall module removes directory
- [ ] Uninstall alias module removes directory

## Priority 3 (lock workflow, still BATS-capable)
- [ ] `server lock --help`
- [ ] `server lock` creates `lucee-lock.json`
- [ ] `server config set` blocked when locked
- [ ] `server unlock` succeeds
- [ ] `server config set` allowed after unlock

## Defer for now (keep in shell/integration suites)
- [ ] Full server lifecycle/integration scripts (`test-server-cfml.sh`, `test-urlrewrite-integration.sh`)
- [ ] Sandbox `server run --sandbox` cleanup assertions
- [ ] Performance/file-size checks (quick commands loop, JAR size thresholds)
