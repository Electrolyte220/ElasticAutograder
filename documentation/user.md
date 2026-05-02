# Elastic Autograder User Guide

This guide is for end users who submit grading jobs, monitor job progress, review job details, and download saved grading results through the Elastic Autograder frontend.

## Product Overview

Elastic Autograder is a web interface for uploading Python submissions, sending them to a selected grader, and reviewing the results of each grading run. The current user workflow is centered around three frontend areas:

- `Submit Job`
- `Recent Jobs`
- `Job Details`

## Before You Start

Before using the system, make sure the platform is already running.

- Frontend URL: `http://localhost:5173`
- Backend API URL: `http://localhost:8080`
- Accepted upload types: `.py` and `.zip`
- A grader must be selected before a job can be submitted

If the frontend address is different in your environment, use the URL shown by the Vite frontend terminal.

## Submit A Job

### 1. Open the Submit Job page

Open the frontend and navigate to the `Submit Job` page.

### 2. Choose a grader

Use the `Grader` dropdown to select the problem or assignment you want to run.

When you select a grader, the page shows:

- the grader label
- a short summary
- additional problem notes or expectations

Review this information before uploading so you know what the grader expects.

### 3. Choose a file

Use the upload area to select one of the supported formats:

- a single `.py` file for one submission
- a `.zip` archive for a batch of submissions

The page also shows the selected filename before submission.

### 4. Submit the job

Click `Submit Job`.

The page validates the request before sending it:

- if no file is selected, the page asks you to select a file
- if no grader is selected, the page asks you to select a grader

### 5. Understand single-file vs batch behavior

Elastic Autograder supports both single-file and batch submission flows.

- A single `.py` upload creates one grading job
- A `.zip` upload can create multiple grading jobs, one for each supported submission in the archive

After upload, the frontend redirects you to `Recent Jobs`. Jobs are then started automatically.

## Review Recent Jobs

Open the `Recent Jobs` page to track active and completed grading runs.

### Auto-refresh behavior

The page refreshes automatically while you are viewing it so new job states appear without manually reloading the browser.

### What the page shows

The jobs table shows recent grading runs, including:

- job ID
- grader type
- original filename
- status
- created time
- score
- test counts when available

### Status meanings

At a user level, these statuses mean:

- `QUEUED`: the job has been accepted and is waiting to run
- `RUNNING`: the grader is currently processing the submission
- `SUCCEEDED`: the grader completed successfully
- `FAILED`: the grading run ended with an error
- `PARTIAL`: the grader completed, but not all tests passed

### Optional summary panel

Use `Show Summary` to open a dashboard panel with recent counts such as:

- total jobs
- queued and running jobs
- failed jobs
- partial jobs
- invalid uploads
- success rate

Use `Hide Summary` to collapse it again.

## Open Job Details

There are two ways to open a job details page from `Recent Jobs`:

- click the job ID in the table
- click the `Details` button in the `Actions` column

The details page shows a fuller record of a single grading run.

### Summary section

The summary section includes:

- filename
- grader type
- status
- score
- tests passed vs total tests
- failure type

### Metadata section

The metadata section can include:

- grader image
- submission path
- Kubernetes job name

### Lifecycle section

The lifecycle section shows important timestamps:

- created time
- updated time
- started time
- finished time
- total run time when available

### Failure Details section

If a job fails or is only partially successful, this section helps explain why by showing:

- failure reason
- failure message

### Results section

If the grader saved structured result entries, the page displays them in a results table with:

- result name
- result kind
- pass/fail outcome
- message

If the job is still running or no saved result entries exist yet, the page explains that results are not yet available.

## Download Results

Elastic Autograder supports downloading saved result JSON for jobs that already have result data.

### Where download is available

You can download results from:

- the `Download Results` button in the jobs table
- the `Download Results` button on the job details page

### When the button is enabled

The button is enabled only when the backend already has saved result JSON for that job.

If result data does not exist yet, the button stays disabled.

### What the file contains

The downloaded file is a saved JSON result artifact for that grading run. It is useful when you want to:

- keep a local copy of the grading output
- inspect the structured result data outside the browser
- compare multiple job runs later

## Common User Issues

### No file selected

If you click `Submit Job` without choosing a file, the page shows a message asking you to select one first.

### No grader selected

If you upload a file but do not choose a grader, the page shows a message asking you to select a grader.

### Invalid upload

An invalid upload can happen if the submission format is unsupported or if the uploaded archive does not produce usable job entries. In these cases, review the file type and contents, then try again with a supported `.py` or `.zip` input.

### Failed job

If a job shows `FAILED`, open the job details page and check:

- `Failure Type`
- `Failure Details`
- any saved results or messages

These fields provide the best available explanation of the failure recorded by the backend.

### No results yet while a job is still active

If a job is `QUEUED` or `RUNNING`, the details page may not show results yet and the download button may remain disabled. Wait for the backend to finish processing the job, then review the page again.

## Current Limitations

- This project does not currently document a dedicated user authentication or role-management flow
- Results and downloads only become available after backend processing is complete
- Frontend verification for this release is based on linting and production build checks rather than a dedicated frontend unit-test suite
