# TDP Hive Notes

The version 3.1.3-3.0 of Apache Hive is based on the `branch-3.1` tag of the Apache [repository](https://github.com/apache/hive/tree/branch-3.1).

## Jenkinfile

The file `./Jenkinsfile-sample` can be used in a Jenkins / Kubernetes environment to build and execute the unit tests of the Hive project. See []() for details on the environment.

## Making a release

```
mvn clean install -Pdist -DskipTests
```

`-Pdist` generates a `.tar.gz` file of the release at `./packaging/target/apache-hive-3.1.3-3.0-bin.tar.gz`.

## Testing parameters

```
mvn test -T 4 -DforkCount=4 -Dsurefire.rerunFailingTestsCount=3 --fail-never -Drat.numUnapprovedLicenses=1000
```

- -T 4: Thread count
- -DforkCount=4: Fork count for the maven-surefire-plugin, defaults to 1
- -Dsurefire.rerunFailingTestsCount: Retries failed test
- --fail-never: Does not interrupt the tests if one module fails

## Test execution notes

See `./test_notes.txt`

## Build notes

The following error was encountered after adding the Ranger Hive plugin JARs to Hive server2:

```
2021-05-20T15:09:14,571  WARN [main] server.HiveServer2: Error starting HiveServer2 on attempt 1, will retry in 60000ms
java.lang.IncompatibleClassChangeError: com.sun.jersey.json.impl.provider.entity.JSONRootElementProvider and com.sun.jersey.json.impl.provider.entity.JSONRootElementProvider$Wadl disagree on InnerClasses attribute
```

The issue was that the version of `jersey-json` embedded by Tez is too old compared to the one used in Ranger (see `tez/tdp/README.md` for details).

- Commit `6f8bc5d87c758cd08c8a9a968d1effa65e6c38a9` exludes `pentaho-aggdesigner-algorithm` since it cannot be found in the `https://repo.maven.apache.org/maven2` repository and is not needed.

- Commit `43a06943f292e9fb0daa0f82b9dba2c3cc91841c` excludes `ldap-client-api` from the artifact `apacheds-server-integ` since it produces a warning due to a missing dependency.
