\# Elastic Autograder - Beta Release Notes

Version: 0.0.1-beta

Release Date: April 2, 2026



\## Overview

Elastic Autograder is an open source automated grading pipeline for course staff.

Upload student submissions, run graders, and view results in real time.



\## What's Working

\- Uploading a student submission (.py file) via the web interface

\- Running the grader against the submission automatically

\- Viewing job status in real time (Queued, Running, Succeeded, Failed)

\- Viewing score and test results for completed jobs

\- Recent jobs board showing the last 5 jobs



\## Known Issues

\- Multi-file/zip submission is not yet stable in this release

\- Redis queue is present but jobs are currently run synchronously

\- Kubernetes job execution is implemented but requires kind and kubectl 

&#x20; to be installed separately to use

\- The Render free tier backend may have a 50+ second cold start delay 

&#x20; if it has not been used recently



\## Prerequisites

Install the following before running:

1\. \*\*Docker Desktop\*\* - https://docs.docker.com/desktop/

2\. \*\*Java 21\*\* - https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html

3\. \*\*Python 3\*\* - https://www.python.org/downloads/

4\. \*\*Node.js (LTS)\*\* - https://nodejs.org/



Verify installations:

```bash

docker --version

java -version

python --version

node -v

npm -v

```



\## Option 1: Use the Live Deployment (Recommended)

No installation required. Simply visit:



\*\*Frontend:\*\* https://elastic-autograder.vercel.app

\*\*Backend API:\*\* https://elastic-autograder-backend.onrender.com



Note: The backend is hosted on Render's free tier and may take 50+ seconds

to respond on the first request if it has been inactive.



\## Option 2: Run Locally Using the JAR



\### Step 1: Start the Database

From the root of this package, run:

```bash

docker compose up -d

docker exec -i ea-postgres psql -U postgres -d elastic\\\_autograder < init/create\\\_job.sql

```



\### Step 2: Run the Backend JAR

```bash

java -jar backend/backend-0.0.1.jar --spring.profiles.active=local

```



\### Step 3: Run the Frontend

```bash

cd frontend

npm install

npm run dev

```



\### Step 4: Open the App

Frontend: http://localhost:5173

Backend API: http://localhost:8080



\## Option 3: Run from Source

See README.md for full source setup instructions.



\## Troubleshooting

\- \*\*Port 5432 already in use:\*\* You may have PostgreSQL installed locally.

&#x20; Stop the local PostgreSQL service before running Docker.

&#x20; On Windows: `net stop postgresql-x64-17` (run as administrator)

&#x20; On Mac/Linux: `sudo service postgresql stop`



\- \*\*Backend fails to connect to database:\*\* Make sure Docker is running and

&#x20; the containers are up. Run `docker ps` to verify `ea-postgres` is listed.



\- \*\*gradlew not recognized in PowerShell:\*\* Use `.\\\\gradlew` instead of `gradlew`



\- \*\*npm not recognized in PowerShell:\*\* Run this first:

&#x20; `Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned`



\- \*\*Frontend shows "Failed to fetch":\*\* The backend is not running.

&#x20; Make sure you started the JAR or ran bootRun before opening the frontend.



\- \*\*Cold start delay on live site:\*\* The Render free tier spins down after

&#x20; inactivity. Wait 60 seconds and refresh if the site appears unresponsive.



\## Package Contents

\- `backend/` - Spring Boot backend source code + runnable JAR (backend-0.0.1.jar)

\- `frontend/` - React + Vite frontend source code

\- `init/` - Database schema and seed SQL scripts

\- `k8s/` - Kubernetes configuration files

\- `mocksubmission/` - Sample submissions for testing

\- `docker-compose.yaml` - Local PostgreSQL + Redis setup

\- `README.md` - Full developer setup instructions



\## Source Repository

https://github.com/Electrolyte220/ElasticAutograder



\## Issue Tracker

https://github.com/Electrolyte220/ElasticAutograder/issues

