import tempfile
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "assignments", "test1"))

BASE = os.path.dirname(os.path.abspath(__file__))
ANSWER_KEY = os.path.join(BASE, "assignments", "test1", "answerKey.py")
SUBMISSION = os.path.join(BASE, "assignments", "test1", "studentSubmission.py")

from answerKey import FUNCTION_NAME, TEST_CASES
from main import grade

def make_submission(code):
    tmp = tempfile.NamedTemporaryFile(mode='w', suffix='.py', delete=False)
    tmp.write(code)
    tmp.close()
    return tmp.name

def test_perfect_score():
    passed, total, results = grade(SUBMISSION, ANSWER_KEY)
    failed_cases = [
        f"\n  Test {r['test_num']} Failed | Input: {r['input']} | Expected: {r['expected']} | Got: {r['got']}"
        for r in results if not r.get("passed")
    ]
    assert passed == total, f"Expected {total}/{total} but got {passed}/{total}{''.join(failed_cases)}"

def test_all_wrong():
    path = make_submission(f"def {FUNCTION_NAME}(*args, **kwargs):\n    return None\n")
    passed, total, _ = grade(path, ANSWER_KEY)
    assert passed == 0, f"Expected 0/{total} but got {passed}/{total}"

def test_missing_function():
    path = make_submission(f"def wrong_function_name(*args, **kwargs):\n    return None\n")
    passed, total, _ = grade(path, ANSWER_KEY)
    assert passed == 0, f"Expected 0/{total} but got {passed}/{total}"

def test_submission_crashes():
    path = make_submission(f"def {FUNCTION_NAME}(*args, **kwargs):\n    raise Exception('oops')\n")
    passed, total, _ = grade(path, ANSWER_KEY)
    assert passed == 0, f"Expected 0/{total} but got {passed}/{total}"

def test_score_never_exceeds_total():
    passed, total, _ = grade(SUBMISSION, ANSWER_KEY)
    assert passed <= total, f"Score {passed} exceeded total {total}"

def test_score_never_negative():
    path = make_submission(f"def {FUNCTION_NAME}(*args, **kwargs):\n    return None\n")
    passed, total, _ = grade(path, ANSWER_KEY)
    assert passed >= 0, f"Score was negative: {passed}"

def test_total_matches_answer_key():
    _, total, _ = grade(SUBMISSION, ANSWER_KEY)
    assert total == len(TEST_CASES), f"Expected {len(TEST_CASES)} test cases but got {total}"

def test_debug_paths():
    assert os.path.exists(ANSWER_KEY), f"ANSWER_KEY not found: {ANSWER_KEY}"
    assert os.path.exists(SUBMISSION), f"SUBMISSION not found: {SUBMISSION}"