To run a benchmark, build the module with:

```
mvn -am -pl jmh-benchmarks package
```

and run

```
java -classpath jmh-benchmarks/target/semantic-metrics-jmh-benchmarks.jar org.openjdk.jmh.Main [arguments]
```

add `-h` to see all possible arguments for JMH, and `-l` to list the benchmarks.
