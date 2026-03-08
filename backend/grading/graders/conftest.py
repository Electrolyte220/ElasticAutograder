import sys
import io
import os

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "assignments", "test1"))

from answerKey import FUNCTION_NAME

def pytest_sessionstart(session):
    print(f"\n{FUNCTION_NAME} - Test Results")
    print("─" * 45)

def pytest_runtest_setup(item):
    item._stdout_capture = io.StringIO()
    sys._stdout_bak = sys.stdout
    sys.stdout = item._stdout_capture

def pytest_runtest_teardown(item, nextitem):
    sys.stdout = sys._stdout_bak

def pytest_runtest_logreport(report):
    if report.when == "call":
        test_name = report.nodeid.split("test_")[1].replace("_", " ").title()
        if report.passed:
            print(f"✓ {test_name} Passed")
        elif report.failed:
            error = str(report.longrepr).split("AssertionError:")[-1].strip()
            print(f"✗ {test_name} Failed  →  {error}")

def pytest_sessionfinish(session, exitstatus):
    passed = session.testscollected - session.testsfailed
    total = session.testscollected
    print("─" * 45)
    print(f"Score: {passed}/{total} | {passed} passed · {session.testsfailed} failed\n")