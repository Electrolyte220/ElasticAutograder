import sys
import os
import json
import traceback
import importlib.util
from typing import Any, Dict, List


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


def build_payload(
    status: str,
    validation_passed: bool,
    tests_passed: int,
    tests_total: int,
    error_message,
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


def emit(payload: Dict[str, Any], exit_code: int = 0) -> None:
    print(json.dumps(payload, indent=2))
    sys.exit(exit_code)


def grade(submission_path: str, answer_path: str):
    if not os.path.exists(submission_path):
        return 0, 0, [{"error": f"Submission file not found: {submission_path}"}]
    if not os.path.exists(answer_path):
        return 0, 0, [{"error": f"Answer key file not found: {answer_path}"}]

    try:
        answer_module = load_module_from_path("answerKey_module", answer_path)
    except Exception as e:
        return 0, 0, [{"error": f"Could not load answer key: {e}"}]

    function_name = answer_module.FUNCTION_NAME
    test_cases = answer_module.TEST_CASES
    total = len(test_cases)

    try:
        student_module = load_module_from_path("studentSubmission_module", submission_path)
    except Exception as e:
        return 0, total, [{"error": f"Could not load submission: {e}"}]

    if not hasattr(student_module, function_name):
        return 0, total, [{"error": f"Function '{function_name}' not found in submission."}]

    student_func = getattr(student_module, function_name)

    passed = 0
    results = []
    for i, case in enumerate(test_cases):
        test_input = case["input"]
        expected = case["expected"]

        try:
            result = student_func(*test_input)
            success = result == expected
        except Exception as e:
            result = f"ERROR: {e}"
            success = False

        if success:
            passed += 1

        results.append({
            "test_num": i + 1,
            "input": test_input,
            "expected": expected,
            "got": result,
            "passed": success
        })

    return passed, total, results


def main():
    if len(sys.argv) != 3:
        emit(
            build_payload(
                status="FAILED",
                validation_passed=False,
                tests_passed=0,
                tests_total=0,
                error_message="Usage: python main.py <submission_path> <answer_path>",
                results=[]
            ),
            exit_code=1
        )

    submission_path = sys.argv[1]
    answer_path = sys.argv[2]

    passed, total, results = grade(submission_path, answer_path)

    if results and "error" in results[0]:
        emit(
            build_payload(
                status="FAILED",
                validation_passed=False,
                tests_passed=0,
                tests_total=total,
                error_message=results[0]["error"],
                results=[
                    make_result("validation", "validation_check", False, results[0]["error"])
                ]
            ),
            exit_code=1
        )

    json_results = [make_result("validation", "validation_check", True, "submission loaded successfully")]
    for r in results:
        json_results.append(
            make_result(
                "test",
                f"case_{r['test_num']}",
                r["passed"],
                f"Expected {r['expected']}, got {r['got']}"
            )
        )

    status = "SUCCEEDED" if passed == total else "FAILED"

    emit(
        build_payload(
            status=status,
            validation_passed=True,
            tests_passed=passed,
            tests_total=total,
            error_message=None,
            results=json_results
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