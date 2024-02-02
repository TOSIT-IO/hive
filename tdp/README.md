# TDP Hive Notes

The version 2.3.9-1.0-SNAPSHOT of Apache Hive is based on the `branch-2.3` tag of the Apache [repository](https://github.com/apache/hive/tree/branch-2.3).

**Note**: This is a branch that is only needed for the Spark Dependency. Apache Spark 3.2 depends on version `org.spark-project.hive:2.3.9`.

## Building

```
mvn clean install -DskipTests
```

## Test execution notes

Should we care about testing this Hive version ?
