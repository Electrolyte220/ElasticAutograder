import \
    importlib.util
import \
    json
import \
    os
import \
    sys
import \
    traceback
from typing import \
    Any, \
    Dict, \
    List


def load_module_from_path(module_name: str, file_path: str):
    spec = importlib.util.spec_from_file_location(module_name, file_path)
    if spec is None or spec.loader is None:
        raise ImportError(f"Could not load spec for {file_path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


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


def main():
    if len(sys.argv) != 3:
        emit(
            build_payload(
                status="FAILED",
                validation_passed=False,
                tests_passed=0,
                tests_total=0,
                error_message="Usage: python main.py <submission_path> <key_path>",
                results=[]
            ),
            exit_code=1
        )

    submission_path = sys.argv[1]
    key_path = sys.argv[2]
    expected_function_name = "fib"

    results: List[Dict[str, Any]] = []

    if not os.path.exists(submission_path):
        emit(
            build_payload(
                status="FAILED",
                validation_passed=False,
                tests_passed=0,
                tests_total=0,
                error_message=f"Submission file not found: {submission_path}",
                results=[
                    make_result("validation", "validation_check", False, "submission file not found")
                ]
            ),
            exit_code=1
        )

    if not os.path.exists(key_path):
        emit(
            build_payload(
                status="FAILED",
                validation_passed=False,
                tests_passed=0,
                tests_total=0,
                error_message=f"Key file not found: {key_path}",
                results=[
                    make_result("validation", "validation_check", False, "key file not found")
                ]
            ),
            exit_code=1
        )

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

    try:
        key_module = load_module_from_path("key_module", key_path)
    except Exception as exc:
        emit(
            build_payload(
                status="FAILED",
                validation_passed=False,
                tests_passed=0,
                tests_total=0,
                error_message=f"Failed to import key: {exc}",
                results=[
                    make_result("validation", "validation_check", False, f"key import failed: {exc}")
                ]
            ),
            exit_code=1
        )

    submission_func = getattr(submission_module, expected_function_name, None)
    key_func = getattr(key_module, expected_function_name, None)

    validation_errors = []

    if not callable(submission_func):
        validation_errors.append(f"submission is missing callable function '{expected_function_name}'")

    if not callable(key_func):
        validation_errors.append(f"key is missing callable function '{expected_function_name}'")

    if validation_errors:
        message = "; ".join(validation_errors)
        emit(
            build_payload(
                status="FAILED",
                validation_passed=False,
                tests_passed=0,
                tests_total=0,
                error_message=message,
                results=[
                    make_result("validation", "validation_check", False, message)
                ]
            ),
            exit_code=0
        )

    results.append(
        make_result(
            "validation",
            "validation_check",
            True,
            f"submission and key imported successfully; found callable '{expected_function_name}'"
        )
    )

    test_cases = [
        {"name": "case_1", "args": [1]},
        {"name": "case_2", "args": [3]},
    ]

    tests_passed = 0
    tests_total = len(test_cases)

    for case in test_cases:
        case_name = case["name"]
        args = case["args"]

        try:
            expected = key_func(*args)
        except Exception as exc:
            results.append(
                make_result("test", case_name, False, f"key failed on args {args}: {exc}")
            )
            continue

        try:
            actual = submission_func(*args)
        except Exception as exc:
            results.append(
                make_result("test", case_name, False, f"submission raised exception on args {args}: {exc}")
            )
            continue

        if actual == expected:
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