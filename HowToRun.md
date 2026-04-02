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

---

## Why Testing Matters

Testing ensures the system is reliable and continues to work as changes are made.

- Unit tests ensure individual components work correctly on their own  
- System tests ensure all components work together as a full workflow  

Without testing, issues in file handling, storage, or grading execution could go unnoticed.

---

## Running Tests

### Run all tests

```bash
./gradlew test