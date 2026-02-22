# Elastic Autograder

We can add more details about it later :3

## One-Time Installs (per teammate / per machine)

Install these first before running anything in this repo.

### Required

#### 1) Docker Desktop
Used to run local containers (PostgreSQL + Redis). `kind` also depends on Docker to create local Kubernetes nodes.

- [Docker Desktop (official docs)](https://docs.docker.com/desktop/)
- [Install Docker Desktop on Windows (official)](https://docs.docker.com/desktop/setup/install/windows-install/)

#### 2) kubectl
Kubernetes CLI used to verify and interact with the local cluster (e.g., `kubectl get nodes`, `kubectl logs`).

- [Install and Set Up kubectl on Windows (official Kubernetes docs)](https://kubernetes.io/docs/tasks/tools/install-kubectl-windows/)
- [Kubernetes Tools Install Page (official)](https://kubernetes.io/docs/tasks/tools/)

#### 3) kind
Tool for running a **local Kubernetes cluster** using Docker container “nodes.”

- [kind Quick Start (official)](https://kind.sigs.k8s.io/docs/user/quick-start/)
- [kind Documentation Home (official)](https://kind.sigs.k8s.io/)


### Recommended (first 2 are for people who use this in the future so not really us)
- **Git** (clone/pull the repo)
- **VS Code** (editing code + Markdown preview)
- **DBeaver / pgAdmin** (optional, for viewing PostgreSQL visually you can also run other commands just to check it out)

## Quick Start (High-Level)
After installing dependencies:

1. Start local services (PostgreSQL + Redis) with Docker Compose
2. Initialize database schema (jobs table + optional seed data)
3. Create local Kubernetes cluster with `kind`
4. Verify cluster with `kubectl`
5. Run a simple test pod to prove K8s works

Detailed commands are documented in the sub-readmes below.