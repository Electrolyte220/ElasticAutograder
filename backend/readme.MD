# Short introduction
This document is designed to help developers or people interested in the architecture to better understand what the backend responsiblities are. \
The two main directories we will go over are grading and src which contain the grading images + uploads and server logic respectively.
(We will cover how to read build.gradle and other parts later)



## grading
The grading folder serves two main purposes, creating the dockerized image of the grader with these being the requirement (subject to change):
- Must follow the manifest.json format (read the manifestGuide.md for more details on creation of your own for another job and what you need to do to add more stuff)

It contains the upload folder aswell, where during the server run time it uploads files to pass along to the kubernetes orchestrator which will pass it to the specific image it should be assigned to. 

## src
The source folder is the heart of the springboot server where a majority of the logic is designed by either Marcos, Chris, or Matt (usually together). 

src is broken up into two main folders

### main
Holds the main function calls for the server, because of the size of each function I'll only be describing the role of each subdirectory and the comments in each file should explain if more details are necessary. 

#### config
contains @config classes that setup shared infastructure components, currently only sets up the Fabric8 Kubernetes Client to talk to the cluster and create jobs. 

#### controller
handle main requests/response defining API routes

#### dto
stands for data transfer object, mostly exists as a nice way to send things to the frontend (like the current list of available jobs)

#### model
contains core data structures used by the rest of the server, like enum classes or classes

#### repository
contains the data access layer for the database, reads and writes from it

#### service (designed by yours truly which was very annoying to setup)
backend logic and orchestration for kubernetes 

### test
This is where the unit tests live and are used to ensure the infastructure doesn't have tiny errors in specific spots like controller or repository.

## rest
 to do 