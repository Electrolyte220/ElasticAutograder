# Elastic Autograder
This project is an open source framework to handle grading jobs concurrently. The framework is designed to be as extendible as possible while building a large majority of the components for you! 

## One-Time Installs (per machine)

Install these first before running anything in this repo.

### Required

#### Docker Desktop/Docker
Used to run local containers (PostgreSQL + Redis). 
Docker is needed for running the localhost postgreSQL database.

- [Docker Desktop (official docs)](https://docs.docker.com/desktop/)
- [Install Docker Desktop on Windows (official)](https://docs.docker.com/get-started/get-docker/)


#### Node.js 
Used for running the React + Vite frontend. 

Recommended: install a recent LTS version of Node.js, npm comes with Node.js 

Verify installation with the following in command prompt
```bash
node -v 
npm -v 
```

#### Java 21
Required for running the local version of springboot we use.

- [Download from the official site](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)

After installation, verify: 
```bash
java -version 
```

#### Python 3 
Used for the backend scripting

- [Download from the official site](https://www.python.org/downloads/)

After install, verify:
```bash
python --version
```


#### PostgreSQL 
Used for the psql command line interace to interact/query databases hosted via render or other providers from terminals.\
The local development database itself runs through Docker Compose 
- [Download from the official site](https://www.postgresql.org/download/)
Note: When installing avoid setting up a postgreSQL database on localport actively 

#### Run the following commands to double check everything was installed properly 

```bash
docker --version
node -v
npm -v
java -version
python --version
psql --version
```

### Steps for hosting locally 

#### Git clone the main branch repository
```bash
git clone https://github.com/Electrolyte220/ElasticAutograder.git
cd ElasticAutograder
git switch k8s
```
The local-host-setup branch is intended to provide a stable local development setup using a Docker-backed local
services.

#### Ensure you're inside of the main elastic_autograder directory
Change directories inside of the ElasticAutograder and run the following command
```bash
git switch k8s
```

#### The next few steps can be done in minimum two terminals but having 2-4 open helps alot for setup

#### Create the kind cluster for the k8s side
Depends on operating system,
(IMPORTANT: This assumes you have no existing cluster or images pre-built, if you do delete them before running scripts)

If on windows, open up a command prompt terminal and run the following
```bash
backend\scripts\setup-k8s.bat
```

If on linux/unix based operating systems run the following
(note this one needs testing I havent ran this one yet)
```bash
chmod +x scripts/setup-k8s.sh
./scripts/setup-k8s.sh
```

If for some reason it fails and only parts of it are created like for example only the cluster or images are done and you need to re run it, delete them to make sure the fresh install works properly

##### For the cluster
```bash
kind delete cluster --name elastic-autograder
```

##### For the docker images
If you want to delete both docker images, then run this
```bash
docker image rm ea-grader-fibbonaci:v1 ea-grader-twosum:v1
```
OR independently remove both docker images (this works for just deleting one in case one fails and one succeeds)
```bash
docker image rm ea-grader-fibbonaci:v1 
docker image rm ea-grader-twosum:v1 
```

#### Run the docker compose file to create an instance of a localhost postgreSQL database
```bash
docker compose up -d
docker exec -i ea-postgres psql -U postgres -d elastic_autograder < init/create_job.sql
```

#### Optional: Add mock data to databse
```bash
docker exec -i ea-postgres psql -U postgres -d elastic_autograder < init/seed_job.sql
```

#### Open multiple terminals (preferably command prompt)
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

#### Open the local development site

Frontend: http://localhost:5173  
Backend API: http://localhost:8080

If the frontend URL is different, check the Vite terminal output.

#### Upload files from mockSubmission folder
Feel free to test the submission files from each respective function to other ones like brokenfib into twosum etc

### 