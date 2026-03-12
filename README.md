# Elastic Autograder
This project is an open source framework to handle grading jobs concurrently. The framework is designed to be as extendible as possible while building a large majority of the components for you! 

## One-Time Installs (per machine)

Install these first before running anything in this repo.

### Required

#### 1) Docker Desktop/Docker
Used to run local containers (PostgreSQL + Redis). 
Docker is needed for running the localhost postgreSQL database.

- [Docker Desktop (official docs)](https://docs.docker.com/desktop/)
- [Install Docker Desktop on Windows (official)](https://docs.docker.com/get-started/get-docker/)


#### 2) Node.js 
Used for running the React + Vite frontend. 

Recommended: install a recent LTS version of Node.js, npm comes with Node.js 

Verify installation with the following in command prompt
```bash
node -v 
npm -v 
```

#### 3) Java 21
Required for running the local version of springboot we use.

- [Download from the official site](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)

After installation, verify: 
```bash
java -version 
```

#### 4) Python 3 
Used for the backend scripting

- [Download from the official site](https://www.python.org/downloads/)

After install, verify:
```bash
python --version
```


#### 5) PostgreSQL 
Used for the psql command line interace to interact/query databases hosted via render or other providers from terminals.\
The local development database itself runs through Docker Compose 
- [Download from the official site](https://www.postgresql.org/download/)
Note: When installing avoid setting up a postgreSQL database on localport actively 

#### 6) Run the following commands to double check everything was installed properly 

```bash
docker --version
node -v
npm -v
java -version
python --version
psql --version
```

### Steps for hosting locally 

#### 1) Git clone the main branch repository
```bash
git clone https://github.com/Electrolyte220/ElasticAutograder.git
cd ElasticAutograder
git switch local-host-setup
```
The local-host-setup branch is intended to provide a stable local development setup using a Docker-backed local
services.

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

#### 4) Open multiple terminals (preferably command prompt)
Inside of terminal 1
```bash 
cd frontend
npm install
npm run dev
```

Inside of terminal 2
```bash
cd backend
gradlew bootRun --args='--spring.profiles.active=local'
```

(If using another terminal like powershell use)
```bash
./gradlew bootRun --args='--spring.profiles.active=local' OR
```

#### 5) Open the local development site

Frontend: http://localhost:5173  
Backend API: http://localhost:8080

If the frontend URL is different, check the Vite terminal output.

#### 6) Upload files from mockSubmission folder
submission1 fails\
submission2 passes test cases\
submission 3 & 4 should both fail (empty function and empty case scenarios)

### 