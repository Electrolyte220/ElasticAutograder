import importlib.util
import json
import os
import sys
import traceback
from typing import Any, Dict, List


def load_module_from_path(module_name: str, file_path: str):
    spec = importlib.util.spec_from_file_location(module_name, file_path)
    if spec is None or spec.loader is None:
        raise ImportError(f"Could not load spec for {file_path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def load_json_file(file_path: str) -> Dict[str, Any]:
    with open(file_path, "r", encoding="utf-8") as f:
        return json.load(f)


def make_result(kind: str, name: str, passed: bool, message: str) -> Dict[str, Any]:
    return {
        "kind": kind,
        "name": name,
        "passed": passed,
        "message": message
    }


def emit(payload: Dict[str, Any], exit_code: int = 0) -> None:
    print(json.dumps(payload))
    sys.exit(exit_code)


def build_payload(
    status: str,
    validation_passed: bool,
    tests_passed: int,
    tests_total: int,
    error_message: str | None,
    results: List[Dict[str, Any]]
) -> Dict[str, Any]:
    score = round((tests_passed / tests_total) * 100, 2) if tests_total > 0 else 0.0
    return {
        "status": status,
        "validation_passed": validation_passed,
        "tests_passed": tests_passed,
        "tests_total": tests_total,
        "score": score,
        "error_message": error_message,
        "results": results
    }


def fail_validation(message: str, exit_code: int = 1) -> None:
    emit(
        build_payload(
            status="FAILED",
            validation_passed=False,
            tests_passed=0,
            tests_total=0,
            error_message=message,
            results=[make_result("validation", "validation_check", False, message)]
        ),
        exit_code=exit_code
    )


def compare_values(actual: Any, expected: Any, mode: str) -> bool:
    if mode == "exact":
        return actual == expected

    if mode == "unordered_exact":
        if not isinstance(actual, list) or not isinstance(expected, list):
            return False
        try:
            return sorted(actual) == sorted(expected)
        except TypeError:
            return False

    raise ValueError(f"Unsupported comparison mode: {mode}")


def validate_manifest(manifest: Dict[str, Any]) -> tuple[str, str, List[Dict[str, Any]]]:
    entry_function = manifest.get("entry_function")
    comparison = manifest.get("comparison", {})
    test_cases = manifest.get("test_cases")

    errors = []

    if not isinstance(entry_function, str) or not entry_function.strip():
        errors.append("manifest.json is missing a valid 'entry_function'")

    if not isinstance(comparison, dict):
        errors.append("manifest.json has invalid 'comparison'")
        comparison = {}

    comparison_mode = comparison.get("mode", "exact")
    if comparison_mode not in {"exact", "unordered_exact"}:
        errors.append("manifest.json has unsupported comparison.mode")

    if not isinstance(test_cases, list):
        errors.append("manifest.json is missing a valid 'test_cases' list")
        test_cases = []
    else:
        for i, case in enumerate(test_cases):
            if not isinstance(case, dict):
                errors.append(f"test_cases[{i}] is not an object")
                continue
            if "name" not in case or not isinstance(case["name"], str):
                errors.append(f"test_cases[{i}] is missing valid 'name'")
            if "args" not in case or not isinstance(case["args"], list):
                errors.append(f"test_cases[{i}] is missing valid 'args' list")
            if "expected" not in case:
                errors.append(f"test_cases[{i}] is missing 'expected'")

    if errors:
        fail_validation("; ".join(errors), exit_code=1)

    return entry_function, comparison_mode, test_cases


def main():
    if len(sys.argv) != 3:
        fail_validation("Usage: python main.py <submission_path> <manifest_path>", exit_code=1)

    submission_path = sys.argv[1]
    manifest_path = sys.argv[2]

    if not os.path.exists(submission_path):
        fail_validation(f"Submission file not found: {submission_path}", exit_code=1)

    if not os.path.exists(manifest_path):
        fail_validation(f"Manifest file not found: {manifest_path}", exit_code=1)

    try:
        manifest = load_json_file(manifest_path)
    except Exception as exc:
        fail_validation(f"Failed to load manifest: {exc}", exit_code=1)

    entry_function, comparison_mode, test_cases = validate_manifest(manifest)

    results: List[Dict[str, Any]] = []

    try:
        submission_module = load_module_from_path("submission_module", submission_path)
    except Exception as exc:
        emit(
            build_payload(
                status="FAILED",
                validation_passed=False,
                tests_passed=0,
                tests_total=0,
                error_message=f"Failed to import submission: {exc}",
                results=[
                    make_result("validation", "validation_check", False, f"submission import failed: {exc}")
                ]
            ),
            exit_code=0
        )

    submission_func = getattr(submission_module, entry_function, None)

    if not callable(submission_func):
        emit(
            build_payload(
                status="FAILED",
                validation_passed=False,
                tests_passed=0,
                tests_total=0,
                error_message=f"submission is missing callable function '{entry_function}'",
                results=[
                    make_result(
                        "validation",
                        "validation_check",
                        False,
                        f"submission is missing callable function '{entry_function}'"
                    )
                ]
            ),
            exit_code=0
        )

    results.append(
        make_result(
            "validation",
            "validation_check",
            True,
            f"submission imported successfully; found callable '{entry_function}'"
        )
    )

    tests_passed = 0
    tests_total = len(test_cases)

    for case in test_cases:
        case_name = case["name"]
        args = case["args"]
        expected = case["expected"]

        try:
            actual = submission_func(*args)
        except Exception as exc:
            results.append(
                make_result("test", case_name, False, f"submission raised exception on args {args}: {exc}")
            )
            continue

        try:
            passed = compare_values(actual, expected, comparison_mode)
        except Exception as exc:
            results.append(
                make_result("test", case_name, False, f"comparison failed for args {args}: {exc}")
            )
            continue

        if passed:
            tests_passed += 1
            results.append(
                make_result("test", case_name, True, f"Expected {expected}, got {actual}")
            )
        else:
            results.append(
                make_result("test", case_name, False, f"Expected {expected}, got {actual}")
            )

    status = "SUCCEEDED" if tests_passed == tests_total else "FAILED"

    emit(
        build_payload(
            status=status,
            validation_passed=True,
            tests_passed=tests_passed,
            tests_total=tests_total,
            error_message=None,
            results=results
        ),
        exit_code=0
    )


if __name__ == "__main__":
    try:
        main()
    except Exception:
        emit(
            build_payload(
                status="FAILED",
                validation_passed=False,
                tests_passed=0,
                tests_total=0,
                error_message=traceback.format_exc(),
                results=[
                    make_result("validation", "validation_check", False, "unexpected grader failure")
                ]
            ),
            exit_code=1
        )