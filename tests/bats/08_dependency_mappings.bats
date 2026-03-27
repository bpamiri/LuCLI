#!/usr/bin/env bats

load './test_helper.bash'

setup_file() {
    require_lucli_artifacts
    setup_lucli_home
}

teardown_file() {
    cleanup_lucli_home
}

create_dependency_mapping_project() {
    local project_dir
    project_dir="$(mktemp -d "${BATS_TEST_TMPDIR}/dependency-mappings.XXXXXX")"

    cat > "${project_dir}/lucee.json" << 'EOF'
{
  "name": "dependency-mapping-test",
  "webroot": "./",
  "dependencies": {
    "fw1": {
      "source": "git",
      "url": "https://github.com/framework-one/fw1",
      "ref": "v4.3.0",
      "subPath": "framework",
      "installPath": "dependencies/fw1",
      "mapping": "/framework"
    },
    "testlib": {
      "source": "git",
      "url": "https://github.com/example/testlib",
      "installPath": "dependencies/testlib",
      "mapping": "/lib"
    }
  }
}
EOF

    printf '%s\n' "${project_dir}"
}

@test "server dry-run include-lucee shows CFConfig for dependency mapping project" {
    local project_dir
    project_dir="$(create_dependency_mapping_project)"

    run_lucli server start --dry-run --include-lucee "${project_dir}"
    assert_success
    assert_output_contains ".CFConfig.json"
}

@test "computed mappings include /framework/" {
    local project_dir
    project_dir="$(create_dependency_mapping_project)"

    run_lucli server start --dry-run --include-lucee "${project_dir}"
    assert_success
    assert_output_contains "/framework/"
}

@test "computed mappings include /lib/" {
    local project_dir
    project_dir="$(create_dependency_mapping_project)"

    run_lucli server start --dry-run --include-lucee "${project_dir}"
    assert_success
    assert_output_contains "/lib/"
}

@test "computed mapping includes physical path dependencies/fw1" {
    local project_dir
    project_dir="$(create_dependency_mapping_project)"

    run_lucli server start --dry-run --include-lucee "${project_dir}"
    assert_success
    assert_output_contains "dependencies/fw1"
}

@test "computed mapping includes physical path dependencies/testlib" {
    local project_dir
    project_dir="$(create_dependency_mapping_project)"

    run_lucli server start --dry-run --include-lucee "${project_dir}"
    assert_success
    assert_output_contains "dependencies/testlib"
}
