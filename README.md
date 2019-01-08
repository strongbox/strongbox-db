# strongbox-db

## The goal of this project
This project has been created to build OrientDB database snapshots to speed-up initial [strongbox](https://github.com/strongbox/strongbox) startup. Before this project was born, it took some time to do all of the database changes from the beginning of time up until today so that the OrientDB database structure gets populated and upgrades work. It was quite inefficient and this project is an answer for this issue.

## Architecture
This application consists of 2 modules:
* `strongbox-db-liquibase` which creates a `jar` file consiting of the liquibase changesets that construct the [strongbox](https://github.com/strongbox/strongbox) OrientDB database schema
* `strongbox-db-snapshot` which creates a `zip` file with packed built database snapshot built from the liquibase changesets

## How is this project used in [strongbox](https://github.com/strongbox/strongbox) ?
[strongbox](https://github.com/strongbox/strongbox) uses this project submodules as required dependencies. During [strongbox](https://github.com/strongbox/strongbox) startup the application detects if OrientDB database already exists. If not then the application use extracted empty OrientDB database snapshot from `strongbox-db-snapshot` artifact. If the OrientDB database already exists then the application applies all missing liquibase changesets from `strongbox-db-liquibase` artifact.

## How to build this project ?
`mvn clean install`
## What's the result of the build process ?
2 artifacts:
* `strongbox-db-liquibase-${version}.jar` located in `strongbox-db-liquibase/target` which contains current database liquibase changesets
* `strongbox-db-snapshot-${version}-db-snapshot.zip` located in `strongbox-db-snapshot/target` which contains zipped fresh database snapshot built from the above changesets 
