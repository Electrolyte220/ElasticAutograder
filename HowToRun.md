# Testing Documentation

This document explains how the system is tested and how to run the tests locally. The project includes unit tests for individual components and a system test for the full autograder workflow.

---

## Unit Tests

Unit tests are used to verify that individual parts of the system work correctly on their own.

### What is tested

#### JobController Tests

These tests verify the file upload functionality in the system.

They check:

- A file can be uploaded successfully
- Duplicate file uploads are rejected
- The correct success and error messages are returned

---

#### JobRepository Tests

These tests verify that job data is correctly stored and managed in the database layer.

They check:

- Job records are saved correctly
- Job records can be retrieved when needed
- The system maintains consistent submission data

---

#### GraderRegistry Tests

These tests verify that grader definitions are correctly registered and returned by the system.

They check:

- A valid grader key returns the correct grader definition
- The returned grader contains the expected label, image name, and manifest path
- Invalid grader keys are handled properly

These tests help confirm that the backend can correctly look up the grader configuration needed before creating grading jobs.

---

#### Fabric8GradingOrchestrator Tests

These tests verify that Kubernetes job creation logic is built correctly before being sent to the cluster.

They check:

- The grading job is created with the correct job name
- The correct grader container image is used
- The correct submission path and manifest path are passed as container arguments

These tests help ensure that the backend constructs grading jobs correctly and sends the right configuration to Kubernetes.

---

## System Test

The system test verifies the full end-to-end autograder workflow.

### What is tested

This test simulates a real student submission and checks the full process:

- A sample submission file is selected
- The file is passed into the backend system
- The Python grading engine is executed
- A result and exit code are returned
- The system confirms the process completes successfully

This ensures that all components (backend + grading engine) work together correctly.

The fixture layout is problem-aware:

- Fibonacci fixtures are resolved from `mocksubmission/fib/`
- Other shared fixtures continue to live under `mocksubmission/`

---

## Why Testing Matters

Testing ensures the system is reliable and continues to work as changes are made.

- Unit tests ensure individual components work correctly on their own
- System tests ensure all components work together as a full workflow

Without testing, issues in file handling, storage, or grading execution could go unnoticed.

---

## Running Tests

All backend tests are run from the `backend` directory.

### 1. Move into the backend folder

```bash
cd backend
```

### 2. Run the backend test suite

```bash
./gradlew test
```

### 3. Choose the backend profile you want to verify

Use `dev` when you want grader setup to rebuild and load images automatically on startup.

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Use `local` for faster day-to-day restarts without automatic grader rebuilds.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

If you need a one-off rebuild while staying on `local`, override the property directly:

```bash
./gradlew bootRun --args='--spring.profiles.active=local --graders.setup-on-startup=true'
```
