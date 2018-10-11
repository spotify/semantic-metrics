# 1.0.0 (not yet released)

- update metrics library from `com.codahale.metrics:metrics-*:3.0.2` to
  `io.dropwizard.metrics:metrics-*:4.0.2` ([30][]), primarily to pull in fixes
  for some metrics (e.g. FileDescriptorGaugeSet) that are not working under
  Java 9 and above. The Java package of the `metrics-*` library have not
  changed - they remain under `com.codahale.metrics`, but because the
  underlying libraries have changed major versions, the major version of this
  library is being bumped as well.
- updates to build process to verify the project can be built on Java 10 and
  11. No functional changes to library included in this change. ([29][])

[30]: https://github.com/spotify/semantic-metrics/pull/30
[29]: https://github.com/spotify/semantic-metrics/pull/29
