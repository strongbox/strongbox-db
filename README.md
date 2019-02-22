# strongbox-db
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2Fstrongbox%2Fstrongbox-db.svg?type=shield)](https://app.fossa.io/projects/git%2Bgithub.com%2Fstrongbox%2Fstrongbox-db?ref=badge_shield)


## The goal of this project
This project has been created in order speed up the building of the OrientDB database snapshots during the initial [Strongbox](https://github.com/strongbox/strongbox) startup. Before this project was born, it took some time to do all of the database changes from the beginning of time up until the most recent version, so that the OrientDB database structure would be populated properly.

## Architecture
This application consists of the following modules:
* `strongbox-db-liquibase` which creates a `jar` file consiting of the liquibase changesets that construct the [Strongbox](https://github.com/strongbox/strongbox) OrientDB database schema
* `strongbox-db-import` which creates a `zip` file with packed built database snapshot built from the liquibase changesets

## How is this project used in [strongbox](https://github.com/strongbox/strongbox) ?
[strongbox](https://github.com/strongbox/strongbox) uses this project submodules as required dependencies. During the startup of [Strongbox](https://github.com/strongbox/strongbox), the application detects if an OrientDB database already exists. If not, then the application uses an extracted empty OrientDB database snapshot from the `strongbox-db-import` artifact. If the OrientDB database already exists, then the application applies all missing liquibase changesets from the `strongbox-db-liquibase` artifact.

## How to build this project ?
To build the code, simply execute:
`mvn clean install`

## What's the result of the build process ?
This project produces the following artifacts:
* `strongbox-db-liquibase-${version}.jar` : Located in `strongbox-db-liquibase/target`, which contains current database liquibase changesets
* `strongbox-db-import-${version}.zip` : Located in `strongbox-db-import/target`, which contains zipped fresh database snapshot built from the above changesets


## License
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2Fstrongbox%2Fstrongbox-db.svg?type=large)](https://app.fossa.io/projects/git%2Bgithub.com%2Fstrongbox%2Fstrongbox-db?ref=badge_large)