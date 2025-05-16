# TDP Hive Notes

The version 4.0.0-1.0 of Apache Hive is based on the `rel/release-4.0.0` tag of the Apache [repository](https://github.com/apache/hive/tree/rel/release-4.0.0).

## Making a release

```
mvn clean install -Pdist -DskipTests -Drat.skip=true
```

`-Pdist` generates a `.tar.gz` file of the release at `./packaging/target/apache-hive-4.0.0-1.0-bin.tar.gz`.

## Testing parameters

```
mvn test -T 4 -DforkCount=4 -Dsurefire.rerunFailingTestsCount=3 --fail-never -Drat.numUnapprovedLicenses=1000
```

- -T 4: Thread count
- -DforkCount=4: Fork count for the maven-surefire-plugin, defaults to 1
- -Dsurefire.rerunFailingTestsCount: Retries failed test
- --fail-never: Does not interrupt the tests if one module fails
