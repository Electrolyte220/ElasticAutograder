# Elastic Autograder

We can add more details about it later :3

## One-Time Installs (per teammate / per machine)

Install these first before running anything in this repo.

### Required

#### 1) Docker Desktop/Docker
Used to run local containers (PostgreSQL + Redis). 
Docker is needed for running the localhost postgreSQL database.

- [Docker Desktop (official docs)](https://docs.docker.com/desktop/)
- [Install Docker Desktop on Windows (official)](https://docs.docker.com/desktop/setup/install/windows-install/)


#### 2) Node.js 
Used for running the frontend

#### 3) Java 21
Required for running the local version of springboot we use.
(As of now version 4.0.3 :))

#### 4) Python 
Used for the backend scripts 

#### 5) PostgreSQL 

### Steps for hosting locally 

#### 1) Git clone the main branch repository
```bash
git clone https://github.com/Electrolyte220/ElasticAutograder 
```

#### 2) Ensure you're inside of the main elastic_autograder directory
Change directories inside of the ElasticAutograder and run the following command
```bash
git switch local-host-setup
```

#### 3) Run the docker compose file to create an instance of a localhost postgreSQL database
```bash
docker compose up -d
docker exec -i ea-postgres psql -U postgres -d elastic_autograder < init/create_job.sql
```

#### Optional: Add mock data to databse
```bash
docker exec -i ea-postgres psql -U postgres -d elastic_autograder < init/seed_job.sql
```

#### 4) Open multiple terminals or command prompts
Inside of terminal 1
```bash 
cd frontend
npm install
npm run dev
```

Inside of terminal 2
```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=local' OR
gradlew bootRun --args='--spring.profiles.active=local' (if using command prompt)
```

#### 5) Navigate to the localhost website 
http://localhost:5173/
OR
just double check with the terminal hosting vite locally

#### 6) Upload files from /mocksubmission
submission1 is a failure
submission2 passes teh test cases
submission 3 & 4 should both fail (empty function and empty case scenarios)


### Required for the future (ignore this for now)
#### 1) kubectl
Kubernetes CLI used to verify and interact with the local cluster (e.g., `kubectl get nodes`, `kubectl logs`).

- [Install and Set Up kubectl on Windows (official Kubernetes docs)](https://kubernetes.io/docs/tasks/tools/install-kubectl-windows/)
- [Kubernetes Tools Install Page (official)](https://kubernetes.io/docs/tasks/tools/)

#### 2) kind
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
