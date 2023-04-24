# TDP Hive Notes

The version 1.2.3-1.0-SNAPSHOT of Apache Hive is based on the `branch-1.2` tag of the Apache [repository](https://github.com/apache/hive/tree/branch-1.2).

**Note**: This is a branch that is only needed for the Spark Dependency. Apache Spark 2.x depends on version `org.spark-project.hive:1.2.1.spark2` which was a fork hosted on the Github of one of Spark's maintainers: https://github.com/JoshRosen/hive/tree/release-1.2.1-spark2.

Hortonworks had their own version of Hive 1.x as a dependency to Spark: https://github.com/hortonworks/spark_hive2-release/tree/HDP-3.1.5.0-152-tag (link is now dead).

The only addition to Apache Hive 1.2 is the support of Hadoop 3 which removes the `Unrecognized Hadoop major version number`. See https://github.com/TOSIT-FR/spark/blob/branch-2.3-TDP/tdp/test_notes.txt for details.

## Building

```
mvn clean install -Phadoop-2 -DskipTests
```

## Test execution notes

Should we care about testing this Hive version ?
