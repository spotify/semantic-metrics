# semantic-metrics
[![Build Status](https://github.com/spotify/semantic-metrics/workflows/JavaCI/badge.svg)](https://github.com/spotify/semantic-metrics/actions?query=workflow%3AJavaCI)

This project contains modifications to the
[dropwizard metrics](https://github.com/dropwizard/metrics) project.

The primary additions includes a replacement for `MetricRegistry` allowing for
metric names containing tags through
[MetricId](api/src/main/java/com/spotify/metrics/core/MetricId.java).

# Usage

The following are the interfaces and classes that _has_ to be used from this
package in order for MetricId to be used.

You will find these types in [com.spotify.metrics.core](
core/src/main/java/com/spotify/metrics/core).

* SemanticMetricRegistry
  &mdash; Replacement for MetricRegistry.
* MetricId
  &mdash; Replacement for string-based metric names.
* SemanticMetricFilter
  &mdash; Replacement for MetricFilter.
* SemanticMetricRegistryListener
  &mdash; Replacement for MetricRegistryListener.
* SemanticMetricSet
  &mdash; Replacement for MetricSet.

Care must be taken _not to_ use the upstream MetricRegistry because it does not
support the use of MetricId.
To ease this, all of the replacing classes follow the `Semantic*` naming
convention.

As an effect of this, pre-existing plugins for codahale metrics _will not_
work.

# Installation

Add a dependency in maven.

```
<dependency>
  <groupId>com.spotify.metrics</groupId>
  <artifactId>semantic-metrics-core</artifactId>
  <version>${semantic-metrics.version}</version>
</dependency>
```

# Provided Plugins

This project provide the following set of plugins;

* [com.spotify.metrics.ffwd](ffwd-reporter/src/main/java/com/spotify/metrics/ffwd)
  A reporter into [FastForward](https://github.com/spotify/ffwd).
* [com.spotify.metrics.jvm](core/src/main/java/com/spotify/metrics/jvm)
  Ported MetricSet's for internal java statistics.

See and run [examples](examples/src/main/java/com/spotify/metrics/example).

# Considerations

#### `MetricIdCache`

If you find yourself in a situation where you create many instances of this
class (i.e. when reporting metrics) and profiling/benchmarks show a significant 
amount of time spent constructing MetricId instances, considering making use of
a [MetricIdCache](api/src/main/java/com/spotify/metrics/core/MetricIdCache.java)

The following is an example integrating with Guava.

```java
// GuavaCache.java

public final class GuavaCache<T> implements MetricIdCache.Cache<T> {
    final Cache<T, MetricId> cache = CacheBuilder.newBuilder().expireAfterAccess(6, TimeUnit.HOURS)
            .build();

    private final MetricIdCache.Loader<T> loader;

    public GuavaCache(Loader<T> loader) {
        this.loader = loader;
    }

    @Override
    public MetricId get(final MetricId base, final T key) throws ExecutionException {
        return cache.get(key, new Callable<MetricId>() {
            @Override
            public MetricId call() throws Exception {
                return loader.load(base, key);
            }
        });
    }

    @Override
    public void invalidate(T key) {
        cache.invalidate(key);
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    public static MetricIdCache.Any setup() {
        return MetricIdCache.builder().cacheBuilder(new MetricIdCache.CacheBuilder() {
            @Override
            public <T> MetricIdCache.Cache<T> build(final Loader<T> loader) {
                return new GuavaCache<T>(loader);
            }
        });
    }
}
```

```java
// MyApplicationStatistics.java

public class MyApplicationStatistics() {
    private final MetricIdCache.Typed<String> endpoint = GuavaCache.setup()
        .loader(new MetricIdCache.Loader<String>() {
            @Override
            public MetricId load(MetricId base, String endpoint) {
                return base.tagged("endpoint", endpoint);
            }
        });

    private final MetricIdCache<String> requests = endpoint
        .metricId(MetricId.build().tagged("what", "endpoint-requests", "unit", "request"))
        .build();

    private final MetricIdCache<String> errors = endpoint
        .metricId(MetricId.build().tagged("what", "endpoint-errors", "unit", "error"))
        .build();

    private final SemanticMetricRegistry registry;

    public MyApplicationStatistics(SemanticMetricRegistry registry) {
        this.registry = registry;
    }

    public void reportRequest(String endpoint) {
        registry.meter(requests.get(endpoint)).mark();
    }

    public void reportError(String endpoint) {
        registry.meter(errors.get(endpoint)).mark();
    }
}
```

#### Don't assume that semantic-metrics will be around forever

Avoid performing deep integration of semantic-metrics into your library or
application.
This will prevent you, and third parties, from integrating your code with
different metric collectors.

As an alternative you should build a tree of interfaces that your application
uses to report metrics (e.g. `my-service-statistics`), and use these to
build an implementation using semantic metrics
(`my-service-semantic-statistics`).

This pattern greatly simplifies integrating your application with more than one
metric collector, or ditching semantic-metrics when it becomes superseded by
something better.

At configuration time your application can decide which implementation to use
by simply providing an instance of the statistics API which suits their
requirements.

##### Example

Build an interface describing all the _things_ that your application reports.

```java
public interface MyApplicationStatistics {
    /**
     * Report that a single request has been received by the application.
     */
    void reportRequest();
}
```

Provide a semantic-metrics implementation.

```java
public class SemanticMyApplicationStatistics implements MyApplicationStatistics {
    private final SemanticMetricRegistry registry;

    private final Meter request;

    public SemanticMyApplicationStatistics(SemanticMetricRegistry registry) {
        this.registry = registry;
        this.request = registry.meter(MetricId.build().tagged(
            "what", "requests", "unit", "request"));
    }

    @Override
    public void reportRequest() {
        request.mark();
    }
}
```

Now a user of your framework/application can do something like the following to
bootstrap your application.

```java
public class Entry {
    public static void main(String[] argv) {
        final SemanticMetricRegistry registry = new SemanticMetricRegistry();
        final MyApplicationStatistics statistics = new SemanticMyApplicationStatistics(registry);
        /* your application */
        final MyApplication app = MyApplication.builder().statistics(statistics).build();

        final FastForwardReporter reporter = FastForwardReporter.forRegistry(registry).build()

        reporter.start();
        app.start();

        app.join();
        System.exit(0);
    }
}
```

# Metric Types

There are different metric types that can be used depending on what it is that
we want to measure, e.g., queue length, or request time, etc.

## Gauge
A gauge is an instantaneous measurement of a value. For example if we want to measure the number of pending jobs in a queue.

```java
registry.register(metric.tagged("what", "job-queue-length"), new Gauge<Integer>() {
    @Override
    public Integer getValue() {
        // fetch the queue length the way you like
        final int queueLength = 10;
        // obviously this is gonna keep reporting 10, but you know ;)
        return queueLength;
    }
});
```
In addition to the tags that are specified (e.g., "what" in this example), FfwdReporter adds the following tags to each Gauge data point:

| tag         | values  | comment |
|-------------|---------|---------|
| metric_type | gauge   |         |

## Counter
A counter is just a gauge for an AtomicLong instance.
You can increment or decrement its value.

For example we want a more efficient way of measuring the pending job in a
queue.

```java
final Counter counter = registry.counter(metric.tagged("what", "job-count"));
// Somewhere in your code where you are adding new jobs to the queue you increment the counter as well
counter.inc();
// Somewhere in your code the job is going to be removed from the queue you decrement the counter
counter.dec();
```

In addition to the tags that are specified (e.g., "what" in this example), FfwdReporter adds the following tags to each Counter data point:

| tag         | values  | comment |
|-------------|---------|---------|
| metric_type | counter |         |

## Meter
A meter measures the rate of events over time (e.g., "requests per second").
In addition to the mean rate, meters also track 1- and 5-minute moving
averages.

For example we have an endpoint that we want to measure how frequent we receive
requests for it.

```java
Meter meter = registry.meter(metric.tagged("what", "incoming-requests").tagged("endpoint", "/v1/list"));
// Now a request comes and it's time to mark the meter
meter.mark();
```

In addition to the tags that are specified (e.g., "what" and "endpoint" in this example), FfwdReporter adds the following tags to each Meter data point:

| tag         | values   | comment |
|-------------|----------|---------|
| metric_type | meter    |         |
| unit        | \<unit\>/s |\<unit\> is what is originally specified as "unit" attribute during declaration. If missing, the value will be set as "n/s". For example if you originally specify .tagged("unit", "request") on a Meter, FfwdReporter emits Meter data points with "unit":"request/s"       |
| stat | 1m, 5m    | **1m** means the size of the time bucket of the calculated moving average of this data point is 1 minute. **5m** means 5 minutes.         |

**NOTE:** Meter also reports the meter counter value to allow platforms to derive rates using the monotonically increasing count instead of only aggregating the rate computed by the meter itself. It is useful for applications to be able to report both count and rate using a meter.

## Deriving Meter
A deriving meter takes the derivative of a value that is expected to be monotonically increasing.<BR><BR>
A typical use case is to get the rate of change of a counter of the total number of events.<BR><BR>
This implementation ignores updates that decrease the counter value.
The rationale is that the counter is expected to be monotonically increasing between
infrequent resets (when a process has been restarted, for example).
Thus, negative values should only happen on restart and should be safe to discard.

```java
DerivingMeter derivingMeter = registry.derivingMeter(metric.tagged("what", "incoming-requests").tagged("endpoint", "/v1/list"));
derivingMeter.mark();
```

In addition to the tags that are specified (e.g., "what" and "endpoint" in this example), FfwdReporter adds the following tags to each Meter data point:

| tag         | values   | comment |
|-------------|----------|---------|
| metric_type | deriving_meter |         |
| unit        | \<unit\>/s |\<unit\> is set to what is specified during declaration. For example, if you specify .tagged("unit", "request") on a DerivingMeter, FfwdReporter emits DerivingMeter data points with "unit":"request/s". Default: "n/s".|
| stat | 1m, 5m    | \<stat\> means the size of the time bucket of the calculated moving average of this data point. **1m** is 1 minute. **5m** means 5 minutes.         |


## Histogram
A histogram measures the statistical distribution of values in a stream of
data.
It measures minimum, maximum, mean, median, standard deviation, as well as 75th
and 99th percentiles.

For example this histogram will measure the size of responses in bytes.

```java
Histogram histogram = registry.histogram(metric.tagged("what", "response-size").tagged("endpoint", "/v1/content"));
// fetch the size of the response
final long responseSize = getResponseSize(response);
histogram.update(responseSize);
```
In addition to the tags that are specified (e.g., "what" and "endpoint" in this example), FfwdReporter adds the following tags to each Histogram data point:

| tag         | values                         | comment                       |
|-------------|--------------------------------|-------------------------------|
| metric_type | histogram                      |                               |
| stat        | min, max, mean, median, stddev, p75, p99 |**min:** the lowest value in the snapshot<br>**max:** the highest value in the snapshot<br>**mean:** the arithmetic mean of the values in the snapshot<br>**median:** the median value in the distribution<br>**stddev:** the standard deviation of the values in the snapshot<br>**p75:** the value at the 75th percentile in the distribution<br>**p99:** the value at the 99th percentile in the distribution |

Note that added custom percentiles will show up in the stat tag.

### Histogram with ttl
`HistogramWithTtl` changes the behavior of the default codahale histogram when update rate is low. If the update rate goes below a certain threshold for a certain time, all samples that have been received during that time are used instead of the random sample that is used in the default histogram implementation. When update rates are above the threshold, the default implementation is used.

**What problem does it solve?**

The default histogram implementation uses a random sampling algorithm with exponentially decaying probabilities over time. This works well if update rates are approximately 10 requests per second or above. When rates go below that, the metrics, especially p99 and above tends to flatline because the values are not replaced often enough. We solve this by using a different implementation whenever the update rate goes below 10 RPS. This gives much more dynamic percentile measurements during low update rates. When update rates go above the threshold we switch to the default implementation.

This was authored by Johan Buratti. 


## Distribution [DO NOT USE]

**Distributions are no longer supported. The code to create them and the Heroic code to query them still exists, however they are being retired and no further adoption should occur.** 

**Heroic is being retired in favor of OpenSource alternatives, and this distribution implementation will not be portable to the future TSDB/query interface. Since onle a few services with a few metrics had experimented with distributions, the choice was made to halt adoption now, to reduce the pain of conversion to a proper histrogram later.**

For questions about this, please reach out in the #monitoring-users slack channel. 


*For historical refrence only* 

**DO NOT USE**

Distribution is a simple interface that allows users to record measurements to compute rank statistics on data distribution, not just a local source.

Every implementation should produce a serialized data sketch in a byteBuffer as this metric point value.

Unlike traditional histograms, distribution doesn't require a predefined percentile value. Data recorded can be used upstream to compute any percentile.

Distribution doesn't require any binning configuration. Just get an instance through SemanticMetricBuilder and record data.

Distribution is a good choice if you care about percentile accuracy in a distributed environment and you want to rely on P99 to set SLOs.

For example, this distribution will measure the size of messages in bytes.

```java
Distribution distribution = registry.distribution(metric.tagged("what", "distribution-message-size", "unit", Units.BYTE));
// fetch the size of the message
int size = getMessageSize(response);
distribution.record(size);
```
In addition to the tags that are specified (e.g., "what" and "unit" in this example), FfwdReporter adds the following tags to each Histogram data point:

| tag         | values                         | comment                       |
|-------------|--------------------------------|-------------------------------|
| metric_type | distribution                   |                               |
| tdigeststat | P50, P75, P99                  |**P50:** the value at the 50th percentile in the distribution<br>**P75:** the value at the 75th percentile in the distribution<br>**P99:** the value at the 99th percentile in the distribution |


**What problem does it solve?**

* Accurate Aggregated Histogram Data

This can record data and send data sketches. A sketch of a dataset is a small data structure that lets you approximate certain characteristics of the original dataset. Sketches are
 used to compute rank based statistics such as percentile. Sketches are mergeable and can be used
to compute any percentile on the entire data distribution.

* Support Sophisticated Data-point Values

With distributions we are able to support sophisticated data point values, such as the Open-census metric distribution.

Authored by Adele Okoubo.

## Timer
A timer measures both the rate that a particular piece of code is called and
the distribution of its duration.

For example we want to measure the rate and handling duration of incoming
requests.

```java
Timer timer = registry.timer(metric.tagged("what", "incoming-request-time").tagged("endpoint", "/v1/get_stuff"));
// Do this before starting to do the thing. This creates a measurement context object that you can pass around.
final Context context = timer.time();
doStuff();
// Tell the context that it's done. This will register the duration and counts one occurrence.
context.stop();
```

In addition to the tags that are specified (e.g., "what" and "endpoint" in this example), FfwdReporter adds the following tags to each Timer data point:

| tag         | values                         | comment                       |
|-------------|--------------------------------|-------------------------------|
| metric_type | timer                          |                               |
| unit        | ns                             |                               |
  
**NOTE:** Timer is really just a combination of a Histogram and a Meter, so apart from the tags above, combination of both Histogram and Meter tags will be included.

# Why Semantic Metrics?

When dealing with thousands of similar timeseries over thousands of hosts,
classification becomes a big issue.

Classical systems organize metric names as strings, containing a lot of
information about the metric in question.

You will often see things like ```webserver.host.example.com.df.used./```.

The same metric expressed as a set of tags could look like.

```json
{"role": "webserver", "host": "host.example.com", "what": "disk-used",
 "mountpoint": "/"}
```

This system of classification from the host machine greatly simplifies any
metrics pipeline.
When transported with a stable serialization method (like JSON) it does not
matter if we add additional tags, or decide to change the order in which the
timeseries happens to be designated.

We can also easily index this timeseries by its tag using a system like
ElasticSearch and ask it interesting questions about which timeseries are
available.

If used with a metrics backend that supports efficient aggregation and
filtering across _tags_ you gain a flexible and intionistic pipeline that is
powerful and agnostic about what it sends, all the way from the service being
monitored to your metrics GUI.

# Contributing

This project adheres to the [Open Code of Conduct](https://github.com/spotify/code-of-conduct/blob/master/code-of-conduct.md).
By participating, you are expected to honor this code.

1. Fork semantic-metrics from
   [github](https://github.com/spotify/semantic-metrics) and clone your fork.
2. Hack.
3. Push the branch back to GitHub.
4. Send a pull request to our upstream repo.

# Releasing

Releasing is done via the `maven-release-plugin` and `nexus-staging-plugin` which are configured via the
`release` [profile](https://github.com/spotify/semantic-metrics/blob/master/pom.xml#L140). Deploys are staged in oss.sonatype.org before being deployed to Maven Central. Check out the [maven-release-plugin docs](http://maven.apache.org/maven-release/maven-release-plugin/) and the [nexus-staging-plugin docs](https://help.sonatype.com/repomanager2) for more information. 

To release, first run: 

``mvn -P release release:prepare``

You will be prompted for the release version and the next development version. On success, follow with:

``mvn -P release release:perform``
  
When you have finished these steps, please "Draft a new release" in Github and list the included PRs (aside from changes to documentation). 
