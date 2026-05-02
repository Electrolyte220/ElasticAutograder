# Elastic Autograder Admin Guide

This guide is for instructors, developers, or maintainers who run Elastic Autograder locally, manage grader definitions, and extend the framework with new graders.

## Admin Overview

In this project, an admin is the person responsible for:

- setting up the local platform
- running the frontend and backend
- preparing grading infrastructure
- adding or updating graders
- defining grader resource limits and runtime behavior

This guide focuses on operating the framework, not on student-facing usage patterns.

## Environment And Prerequisites

Elastic Autograder depends on several tools for local development and grader execution:

- Java 21
- Node.js and `npm`
- Python 3
- Docker Desktop or Docker
- `kind`
- `kubectl`

For the full installation and verification steps, use the existing setup guides:

- [README](../README.md)
- [HowToRun](../HowToRun.md)
- [Docker local setup](docker.md)
- [Setup help](setup-help.md)

## Run The Platform Locally

### 1. Start the database services

From the repository root, start the local infrastructure:

```bash
docker compose up -d
docker exec -i ea-postgres psql -U postgres -d elastic_autograder < init/create_job.sql
```

Optional seed data:

```bash
docker exec -i ea-postgres psql -U postgres -d elastic_autograder < init/seed_job.sql
```

### 2. Prepare the local Kubernetes and grader environment

On Windows:

```bash
scripts\setup-k8s.bat
```

On Linux or macOS:

```bash
bash scripts/setup-k8s.sh
```

If you need to rebuild or reload grader images directly, use:

```bash
python3 scripts/setup-graders.py
```

### 3. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

### 4. Start the backend

For regular development:

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

Use the `dev` profile when you want the backend to rebuild and load grader images automatically during startup:

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 5. When to use `--graders.setup-on-startup=true`

If you normally use the `local` profile but want one startup pass that rebuilds and reloads graders, use:

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=local --graders.setup-on-startup=true'
```

This is useful when:

- you added a new grader
- you changed grader manifests or grader images
- you want local mode plus one startup setup pass

## How Grader Configuration Works

Elastic Autograder loads available graders from `config/graders.json`.

Each grader entry registers metadata the system needs to expose and run a grader, including:

- `key`
- `label`
- `imageName`
- `manifestPath`
- `summary`
- `details`

The frontend grader dropdown is populated from this configuration. When a user chooses a grader, the backend uses that grader key to resolve the matching definition and launch the correct grading container.

## Add A New Grader

### 1. Add a grader entry to `config/graders.json`

Create a new grader object using the existing entries as examples. At minimum, provide:

- a unique `key`
- a human-readable `label`
- the grader Docker `imageName`
- the grader `manifestPath`
- a `summary`
- a `details` list

### 2. Create a matching grader folder

Create a folder under:

```text
backend/grading/image-build/<grader-key>/
```

The folder name should match the grader key so the grader assets stay aligned with the config entry.

### 3. Add `manifest.json`

Place a `manifest.json` file inside the grader folder. This manifest defines the grading contract the runtime will use.

### 4. Rebuild and load grader images

Use the existing grader setup script:

```bash
python3 scripts/setup-graders.py
```

You can also rely on backend startup setup behavior by using the `dev` profile or the `--graders.setup-on-startup=true` override.

### 5. Restart or reload backend startup

The backend must load the new grader configuration before the frontend can present it as a selectable grader.

## Manifest Authoring

The current manifest guide defines three core pieces of grader behavior.

### `entry_function`

`entry_function` is the function name or identifier the grader runtime expects to call in the user submission.

Important: a submission must match the function name expected by the manifest, or the runtime will not be able to evaluate it correctly.

### `comparisons`

The current documented comparison modes are:

- `exact`
- `unordered_exact`

Use `exact` when output must match the expected answer exactly, including list order.

Use `unordered_exact` when output may contain the same values in a different order.

### `test_cases`

`test_cases` defines the inputs that the runtime will pass to the grading function.

Each test case should reflect the format supported by the current runtime and manifest structure.

### Current manifest boundaries

The current documentation notes that this implementation has limits for certain more complex problem shapes. In particular, the existing guide calls out that the current test-case structure does not yet support every problem format, such as some complex tree-based cases.

Document graders according to what the current runtime supports today rather than inventing unsupported manifest shapes.

## Resource Limits And Kubernetes Behavior

Elastic Autograder supports per-grader runtime limits through the grader definition model.

### `timeoutSeconds`

`timeoutSeconds` controls how long a grading job can run before Kubernetes treats it as timed out. In the grading job definition, this maps to the Kubernetes active deadline for the job.

Use this to prevent graders from running indefinitely.

### CPU request vs CPU limit

These fields define CPU resources in millicores:

- `cpuRequestMilli`
- `cpuLimitMilli`

`cpuRequestMilli` is the minimum CPU resource requested for the grader container.

`cpuLimitMilli` is the hard CPU cap for the grader container.

### Memory request vs memory limit

These fields define memory resources in megabytes:

- `memoryRequestMb`
- `memoryLimitMb`

`memoryRequestMb` is the minimum requested memory allocation.

`memoryLimitMb` is the hard memory cap for the grader container.

### Requests vs limits

In Kubernetes terms:

- requests are the resources the container asks for and expects to have available
- limits are the maximum resources the container is allowed to consume

This means requests describe the guaranteed baseline, while limits describe the hard ceiling.

### How failures can appear

If a grader exceeds its limits, the job may fail. For example, memory pressure can surface as an out-of-memory kill, which the backend may classify as a resource-limit failure.

Timeout-related failures can also be recorded if the grading job exceeds `timeoutSeconds`.

## Operational Notes

- Uploaded submissions are staged temporarily under `grading/uploads` while jobs are being prepared
- The backend creates Kubernetes ConfigMaps and Kubernetes Jobs per grading run
- Job output is parsed and stored so the frontend can display summary data, job details, and downloadable result JSON
- Frontend grader choices come from backend-loaded grader configuration rather than from hardcoded frontend options

## Troubleshooting

### Cluster or image reset

If the setup scripts fail and you need to reset local cluster state, use the existing cleanup guidance from [setup-help.md](setup-help.md).

Examples from the current helper document include:

```bash
kind delete cluster --name elastic-autograder
docker image rm ea-grader-fibbonaci:v1 ea-grader-twosum:v1
```

### Missing setup tools

If local setup fails before the backend is ready, verify that the required tools are installed and available on your shell path:

- `docker`
- `python`
- `java`
- `node`
- `npm`
- `kind`
- `kubectl`

### Backend starts but graders are not ready

If the backend starts without usable grader infrastructure:

- verify whether you started with `local` or `dev`
- use `dev` when you want automatic grader setup during startup
- use `--graders.setup-on-startup=true` when you want one setup pass while staying in `local`
- review setup-script output for Python, Docker, cluster, or image build failures

### Config or manifest mistakes

If a grader does not appear or fails during runtime, review:

- the `config/graders.json` entry
- the grader folder name under `backend/grading/image-build/`
- the `manifest.json` file
- the image name used by the grader definition
- whether the expected `entry_function` matches the submission contract

## Current Limitations

- The current manifest system supports the documented comparison and test-case structure only
- The frontend is not an admin console for creating or editing graders
- Local grader automation depends on installed developer tools such as Python, Docker, `kind`, and `kubectl`
- This guide only documents features that exist in the current project and does not assume unsupported grader formats or management features
