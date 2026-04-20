\# Elastic Autograder - Beta Release Notes

Version: 0.1.1 

This version adds a new feature of dynamic grading!
The dynamic grading feature is mainly based off the config/graders.json file.
We changed the hardcoded interpretation to reading the graders.json and then injecting it to pass both to the frontend and backend to properly add new members. 

Short description of how to add a custom grader below!

1. Add the problem description under graders.json
2. Add a new entry
- Fill in key, label, imageName with custom inputs
- Keep manifestPath the same as the rest ONLY change this if you know what you're doing and would like to change the app location 
3. Create a folder in backend/image-build with the same name as the key in the entry you created
4. Add a manifest.json file to the folder with the main json contract (read others to get an idea of what it should look like) 
- Also important please make sure you have the right answers for the grading (I wasted an hour trying to debug this before)
5. Run the main script to build every dynamic member of graders.json 
- Linux/MacOS:
```bash
python3 scripts/setup_graders.py
```

Windows:
```bash
python .\scripts\setup_graders.py
```
6. Start (or restart) the server for the backend and it should properly display graders