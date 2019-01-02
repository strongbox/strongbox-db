# strongbox-db

## How to build this project ?
`mvn clean install`
## What's the result of the build process ?
2 artifacts:
* `strongbox-db-liquibase-${version}.jar` located in `strongbox-db-liquibase/target` which contains current database liquibase changesets
* `strongbox-db-snapshot-${version}-db-snapshot.zip` located in `strongbox-db-snapshot/target` which contains zipped fresh database snapshot built from the above changesets 
