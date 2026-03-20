#!/usr/bin/env bats

load './test_helper.bash'

setup_file() {
    require_lucli_artifacts
}

setup() {
    setup_lucli_home
}

teardown() {
    cleanup_lucli_home
}

@test "modules help works" {
    run_lucli modules --help
    assert_help_exit_code
    assert_output_contains "modules"
}

@test "modules list works when empty" {
    run_lucli modules list
    assert_success
    [[ "${output}" != *"Error"* ]]
}

@test "modules init creates expected files" {
    local module_name="bats_module_${BATS_TEST_NUMBER}"
    local module_dir="${LUCLI_HOME}/modules/${module_name}"

    run_lucli modules init "${module_name}" --no-git
    assert_success
    [ -d "${module_dir}" ]
    [ -f "${module_dir}/Module.cfc" ]
    [ -f "${module_dir}/module.json" ]
    [ -f "${module_dir}/README.md" ]
}

@test "modules list includes initialized module" {
    local module_name="bats_module_${BATS_TEST_NUMBER}"

    run_lucli modules init "${module_name}" --no-git
    assert_success

    run_lucli modules list
    assert_success
    assert_output_contains "${module_name}"
}

@test "run command executes cfm script" {
    run_lucli run "${LUCLI_ROOT_DIR}/tests/cfml/run.cfm"
    assert_success
    assert_output_contains "Hello from a tag based file"
}

@test "run command blocks direct cfc execution" {
    run_lucli run "${LUCLI_ROOT_DIR}/tests/cfml/Run.cfc"
    assert_failure
    assert_output_contains ".cfc"
}

@test "lucli script works via shortcut and run" {
    local script_path
    script_path="$(mktemp "${BATS_TEST_TMPDIR}/test_run.XXXXXX.lucli")"
    cat > "${script_path}" << 'EOF'
echo "Run .lucli works"
EOF

    run_lucli "${script_path}"
    assert_success
    assert_output_contains "Run .lucli works"

    run_lucli run "${script_path}"
    assert_success
    assert_output_contains "Run .lucli works"
}
