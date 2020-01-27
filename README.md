# strongbox-db

## The goal of this project
This project has been created in order speed up the building of the JanusGraph database snapshots during the initial [Strongbox](https://github.com/strongbox/strongbox) startup. Before this project was born, it took some time to do all of the database changes from the beginning of time up until the most recent version, so that the JanusGraph database structure would be populated properly.

## Architecture
This application consists of the following modules:
* `strongbox-db-schema` which creates a `jar` file consiting of the changesets that construct the [Strongbox](https://github.com/strongbox/strongbox) JanusGraph database schema
* `strongbox-db-import` which creates a `zip` file with packed built database snapshot built from the liquibase changesets

## How is this project used in [strongbox](https://github.com/strongbox/strongbox) ?
[strongbox](https://github.com/strongbox/strongbox) uses this project submodules as required dependencies. During the startup of [Strongbox](https://github.com/strongbox/strongbox), the application detects if a JanusGraph database already exists. If one doesn't exist, it uses an extracted empty JanusGraph database snapshot from the `strongbox-db-import` artifact; if a database exists, then the application applies all missing changesets from the `strongbox-db-schema` artifact.

## How to build this project ?
To build the code, simply execute:
`mvn clean install`

## What's the result of the build process ?
This project produces the following artifacts:
* `strongbox-db-schema-${version}.jar` : Located in `strongbox-db-schema/target`, which contains current database changesets
* `strongbox-db-import-${version}.zip` : Located in `strongbox-db-import/target`, which contains zipped fresh database snapshot built from the above changesets
