Run the following command in the terminal in same directory as project to do the following
1. Create the table
2. Populate the table with mock data 
```bash
docker exec -i ea-postgres psql -U postgres -d elastic_autograder < init\create_job.sql
docker exec -i ea-postgres psql -U postgres -d elastic_autograder < init\seed_job.sql
```