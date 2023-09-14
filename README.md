![Conductor](docs/docs/img/conductor-vector-x.png)
 
 
## Conductor
Conductor is an _orchestration_ engine that runs in the cloud.

## Documentation & Getting Started

[Getting Started](https://conductor.netflix.com/devguide/concepts/index.html) guide.

[Usage](https://bydeluxe.atlassian.net/wiki/spaces/ENG/pages/17288931838/Netflix+Conductor) guide.

[High Level Architecture](https://bydeluxe.atlassian.net/wiki/spaces/ENG/pages/17288925905/High+Level+Architecture+Diagram)

## Prerequisite
Conductor runs on Java 1.8

Refer to [this](https://bydeluxe.atlassian.net/wiki/spaces/ENG/pages/17661723086/Support+multiple+Java+versions+on+Mac+OS+using+sdkMan+tool) document for running multiple versions of Java on MacOs

## Local Setup

There are a couple of ways to run conductor locally.

1. Using containers (Conductor Docker Build/Start/Kill Scripts).Refer to [this](https://bydeluxe.atlassian.net/wiki/spaces/ENG/pages/17288930751)
2. Run through IDE (IntelliJ) running the gradle server task.Refer to [this](https://bydeluxe.atlassian.net/wiki/spaces/ENG/pages/17288929826/Setup+Local+Conductor)
3. Run through IDE (IntelliJ) by setting up an application to run.Refer to [this](https://bydeluxe.atlassian.net/wiki/spaces/ENG/pages/17288929826/Setup+Local+Conductor)
4. Run through IDE (IntelliJ) running as remote application.Refer to [this](https://bydeluxe.atlassian.net/wiki/spaces/ENG/pages/17288925271/Project+Setup+Trouble+shooting)

## Setup Local Database

1. Install PostgresQL on the local machine
2. The conductor-local.env file attached below uses the following db configs.. Please update the env file with your local db details if it differs
   database:conductor
   user:conductor
   password:conductor
3. Use conductor/postgresql-persistence/src/main/resources/initial_schema.sql to create the tables

## Starting Conductor UI
Refer  to [this](https://bydeluxe.atlassian.net/wiki/spaces/ENG/pages/17288929826/Setup+Local+Conductor) to setup and run conductor UI in local

