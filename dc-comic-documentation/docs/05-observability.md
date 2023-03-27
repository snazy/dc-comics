---
sidebar_position: 5
---

# Observability

Observability stack is composed of these primitives:

* Logging: it enables to configure your logging using system properties even using GraalVM `native-image`. It is also a good companion for container based deployments (Kubernetes) since you can switch very easily logging to JSON (`-Djava.util.logging.manager=com.dremio.cloud.comic.logging.jul.ComicLogManager -Dcom.dremio.cloud.comic.logging.jul.handler.StandardHandler.formatter=json`).
* Healthcheck(s): this is the capacity to test through HTTP the server state, mainly used by Kubernetes to check if the application is ready (can get traffic) and if it is in a broken state (pod should be killed and restarted for example).
* Metrics: often coupled to opentelemetry, it enables to collect metrics (think time series) about your application. It can be technical (CPU usage for example) or business (number of queries, ...).
* Tracing: this is the ability to trace a business request (a _trace_) end to end through all the system. Main collectors/UI are Jaegger. DC Comic supports that through `dc-comic-tracing` module.

## Logging

DC Comic Logging mainly provides a Java Util Logging (JUL) LogManager which is graalVM friendly - you can reconfigure your JUL loggers at runtime - plus some utilities like structured logging, advanced formatters or handlers.

### Java Util Logging (JUL) integration

Java Util Logging integration is provided as part of the artifact:

```xml
<dependency>
    <groupId>com.dremio.cloud.comic</groupId>
    <artifactId>dc-comic-logging</artifactId>
    <version>${dc-comic.version}</version>
</dependency>
```

#### Use globally

```bash
java ... -Djava.util.logging.manager=com.dremio.cloud.comic.logging.jul.ComicLogManager ...
```

#### Configuration

Configuration is close to default JUL one with small difference:

1. handlers don't need to configure themselves in their constructor or so for common properties (formatter, level, ...), the framework does it automatically
2. `InlineFormatter` and `JsonFormatter` support aliases (reflection free instantiation) with `inline` and `json` you can use in the configuration

#### Standard handler

`com.dremio.cloud.comic.logging.jul.handler.StandardHandler` enables to log on stdout and stderr. For all level from finest/debug to info it will go on stdout and others (warnings, errors/severes) will go on stderr.

It can be configured with the `std` or `standard` alias too.

There is also its companion `stdout` or `com.dremio.cloud.comic.logging.jul.handler.StdoutHandler` which only outputs on stdout.

#### File handler

A `com.dremio.cloud.comic.logging.jul.handler.LocalFileHandler` is also provided to be able to log in a file and rotate the log file. Indeed you can use JVM `FileHandler` but this one is a bit more powerful in practice if you don't run in container and can't use a docker logging driver or equivalent.

Here is its configuration - all are prefixed with `com.dremio.cloud.comic.logging.jul.handler.LocalFileHandler.` and the standard configuration (encoding, level, ...) is still supported even if not listed.

| Name        | Default Value         | Description                                                                                                                                                                                                                |
|-------------|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| filenamePattern | ${application.base}/logs/logs.%s.%03d.log | where log files are created, it uses String.format() and gives you the date and file number - in this order.                                                                                                               |
| noRotation | false | if `true`, once a log file is opened, it is kept for the life of the process.                                                                                                                                              |
| overwrite | false | if `true`, existing files are reused.                                                                                                                                                                                      |
| truncateIfExists | false | if `true`, opening a file is not in append mode (previous content is erased).                                                                                                                                              |
| limit | 10 Megabytes | limit size indicating the file should be rotated - in long format                                                                                                                                                          |
| dateCheckInterval | 5 seconds | how often the date should be computed to rotate the file (don't do it each time for performances reason, means you can get few records of next day in a file name with current day). In Java Duration format (ex: `PT5S`). |
| bufferSize | -1 | if positive, the in memory buffer used to store data before flushing them to disk (in bytes)                                                                                                                               |
| archiveDirectory | ${application.base}/logs/archives/ | where compressed logs are put                                                                                                                                                                                              |
| archiveFormat | gzip | zip or gzip                                                                                                                                                                                                                |
| archiveOlderThan | -1 | how many days files are kept before being compressed                                                                                                                                                                       |
| purgeOlderThan | -1 | how many days files are kept before being deleted, note: it applies on archives and not log files so 2 days of archiving and 3 days of purge makes it deleted after 5 days                                                 |
| compressionLevel | -1 | in case of zip archiving the zip compression level (-1 for off or 0-9)                                                                                                                                                     |

#### Pattern formatter

The library also provides a pattern formatter. It does not use `String.format` as `SimpleFormatter` does and can be configured either with `com.dremio.cloud.comic.logging.jul.formatter.PatternFormatter.pattern` property or passing the pattern to the formatter alias (reflection free mode): `pattern(<pattern to use>)`. Its syntax uses `%` to mark elements of the log record. Here is the list:

| Name | Description |
|------|-------------|
| %%   | Escapes `%` character |
| %n   | End of line |
| %l or %level | Log record level |
| %m or %message | Log record message |
| %c or %logger | Logger name |
| %C or %class | Class name if exists or empty |
| %M or %method | Method name if exists or empty |
| %d or %date | The instance value. Note it can be followed by a date time formatter pattern between brace |
| %x or %exception | The exception. It will be preceeded by a new line if existing to ensure it integrates well in log output |
| %T or %threadId | Thread ID |
| %t or %thread or %threadName | Thread name - only works in synchronous mode |
| %uuid | Random UUID |

Pattern example value: `%d [%l][%c][%C][%M] %m%x%n`, it will output lines like `1970-01-01T00:00:00Z [INFO][the.logger][the.source][the.method] test message\n`.

#### JSON formatter (structured logging)

JSON formatter relies on JSON-P so ensure to add the related dependencies. It can be done with this list for example:

```xml
<dependency>
    <groupId>org.apache.geronimo.specs</groupId>
    <artifactId>geronimo-json_1.1_spec</artifactId>
    <version>1.5</version>
</dependency>
<dependency>
    <groupId>org.apache.johnzon</groupId>
    <artifactId>johnzon-core</artifactId>
    <version>1.2.16</version>
</dependency>
```

---
**TIP**

The JSON formatter can be configured passing `json(useUUID=[false|true],formatMessage=[true|false])` value instead of just `json`. `formatMessage` enables to skip the message formatting when your application does not rely on it - faster and uses less the CPU, `useUUID` enables to force an unique ID in the record.

---

#### Sample configuration files

As with native JUL `LogManager`, you can configure the runtime logging with the following system property: `-Djava.util.logging.config.file=<path to config file>`.

---
**NOTE**

Don't forget `-Djava.util.logging.manager=com.dremio.cloud.comic.logging.jul.ComicLogManager` too.

---

Here is a sample configuration switching to JSON logging:

```
.handlers = com.dremio.cloud.comic.logging.jul.handler.StandardHandler
com.dremio.cloud.comic.logging.jul.handler.StandardHandler.formatter = json
```

The same configuration for a standard inline logging (text style) but tuning the log level:

```
.handlers = com.dremio.cloud.comic.logging.jul.handler.StandardHandler
com.dremio.cloud.comic.logging.jul.handler.StandardHandler.level = FINEST
com.app.level = FINEST
```

Here is a configuration using a pattern:

```
.handlers = standard
standard.formatter = pattern(%d [%l][%c][%C][%M] %m%x%n)
```

And finally a configuration using file output instead of standard one:

```
.handlers = file
file.formatter = inline
```

### GraalVM

DC Comic Logging JUL is integrated with GraalVM native image feature. To enable it you must set in `native-image` command line the following system property: `-Djava.util.logging.manager=com.dremio.cloud.comic.logging.jul.ComicLogManager`.
Other required setup is done in the jar and automatically picked up by `native-image`.

---
**NOTE**

dc-comic-logging will set itself as JUL LogManager during the build time so ensure it does not compete with another logging framework.

---

Once done you can run with no logging configuration or override it through the standard `java.util.logging.config.file` system property.

## HTTP Server

By default observability module adds *another* web server for observability purposes.
Default port is 8181 but you can set `dc.comic.observability.server.port` configuration (system property, environment variable using underscores and uppercasing it) to override it.

The goal to not reuse the same server is to not have to secure this one (it will stay an internal port in your cluster/infrastructure).
Since some Kubernetes tooling does not like adding headers to gather the data (prometheus for example) it is a good compromise.

## Health checks

A health check implements `com.dremio.cloud.comic.observability.health.HealthCheck` API.

Then the observability server will expose a `/health` endpoint which will return a HTTP 200 if all health checks are successful and a HTTP 503 if there is at least one failure.

If you need to distinguish between health check types, you can implement `type()` method and return something different than `live`.

Then you can call the particular endpoints using `/health?type=<my type>`.
To get the live checks, for example, use `/health?type=live`.
Without `type` query parameter, all checks are executed.

## Metrics

Metrics exposes an endpoint `/metrics` on observability server which renders the openmetrics stored in `com.dremio.cloud.comic.observability.metrics.MetricsRegistry`.

It supports `Gauge` and `Counter` metric types.

Here is a common way to use it in your application:

```java
@DefaultScoped
public class MyService {
	private final LongAdder myCounter;

	public MyService(final MetricsRegistry registry) {
		this.myCounter = registry.registerCounter("my-service-counter");
	}

	public void save(final MyEntity entity) {
		// do the normal business code
		myCounter.increment();
	}
}
```

---
**TIP**

You can unregister your counter on the registry if it is a short live counter (to use with a session for example), it will then no more be available but using that with prometheus, you have no guarantee it will be polled so it can be neat to delay the un-registration until next polling.

Gauge can make it easily using `registerReadOnlyGauge` since you rhen pass a `LongSupplier` you control.

---
