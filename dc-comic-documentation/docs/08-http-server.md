---
sidebar_position: 8
---

# HTTP Server

HTTP Server module provides an abstraction over an Apache Tomcat server.

## Dependency

```xml
<dependency>
	<groupId>com.dremio.cloud.comic</groupId>
	<artifactId>dc-comic-http-server</artifactId>
	<version>${dc-comic.version}</version>
</dependency>
```

---
**IMPORTANT**

Annotations (design API) are in `dc-comic-build-api` and is only useful at build time.

---

## Usage

By default, if you use DC Comic IoC, the webservice will be started.
You can customize the configuration listening for `WebServer.Configuration` event.

Defining an endpoint can be done creating an `Endpoint` bean and implementing the matcher (`matches`) and handler which will return a `Response` thanks to the builder:

```java
@Bean
public class Greeting implements Endpoint {

	@Override
	public boolean matches(final Request request) {
		return "GET".equals(request.method());
	}

	@Override
	public CompletionStage<Response> handle(final Request request) {
		return completedFuture(Response.of()
							.body("{\"hello\":true}")
							.build());
	}
}
```

## Configuration

Most of the configuration can be customized using an event listener on `WebServer.Configuration` and unwrapping the instance as a `TomcatWebServerConfiguration` you have access to a full Tomcat server customization (HTTP/2.0, WebSocket and so on).

However, there are a few system properties/environment variables (uppercased and with underscores instead of dots) you can set:

* To skip the initialization of the server at startup: `dc.comic.http-server.start=[true|false]`. This can be useful to not start the server in tests for example
* To set the HTTP port to use: `dc.comic.http-server.port=<port>`, note that setting 0 will make the port random and you can inject `WebServer.Configuration` to read its value
* To set the host to use: `dc.comic.http-server.host=<host>`
* To set the access log pattern to use: `dc.comic.http-server.accessLogPattern=<...>`, see https://tomcat.apache.org/tomcat-11.0-doc/config/valve.html#Access_Logging for details
* To set the webapp directory: `dc.comic.http-server.base=/path/to/www`. It can be useful to serve static websites if you configure the right servlets
* To set the DC Comic default servlet mapping: `dc.comic.http-server.dcComicServletMapping=/`. It can be useful if you want to bind it to a subcontext to use standard servlets for other things like serving base directory
* To set if UTF-8 is enforced (default) or not over the requests/responses: `dc.comic.http-server.utf8Setup=true`

## High level API

The first option to define an endpoint as a bean - automatically picked - is to use `Endpoint.of` API:

```java
@Bean
public Endpoint myGreetingEndpoint() {
	return Endpoint.of(
					// matching impl
					request -> "GET".equals(request.method()) &&
						request.path().startsWith("/greet") &&
						request.query() != null &&
						request.query().startsWith("name="),
					// endpoint impl - can be delegated to any bean
					request -> completableState(Response.of()	
						.status(200)
						.header("content-type", "application/json")
						.body(jsonMapper.toString(
										new Greet(request.query().substring("name=".length()))))
						.build()));
}
```

The alternative is to use `@HttpMatcher` API:

```java
@HttpMatcher(method = "GET", pathMatching = EXACT, path = "/greet")
public CompletionStage<Response> myGreetingEndpoint() {
	return completableStage(Response.of()
						.status(200)
						.header("content-type", "application/json")
						.body(jsonMapper.toString(
										new Greet(request.query().substring("name=".length()))))
						.build());
}
```

---
**TIP**

If your endpoint is fully synchronous you can drop the `CompletionStage` wrapper: `public CompletionStage<Response> myGreetingEndpoint();`.
You can also pass as first parameter a `Request` parameter.

---

## (Open) Tracing

`dc-comic-tracing` module provides a Tomcat valve you can set up on your web container to add tracing capabilities to your Tomcat:

```java
serverConfiguration
	.unwrap(TomcatWebServerConfiguration.class)
	.setContextCustomizers(List.of(c -> c.getPipeline() <1>
		.addValve(new TracingValve( <1>
				new ServerTracingConfiguration(), <2>
				new AccumulatingSpanCollector().setOnFlush(...), <3>
				new IdGenerator(IdGenerator.Type.HEX), <4>
				systemUTC())))); <5>
```

1. Add the valve to the context pipeline, it is recommended to add it as early as possible (just after error report and access log valve in general)
2. The configuration enables the customize the span tags and headers to read for span propagation
3. The accumulator is what will send/log/... the spans once aggregated, ensure to configure it as needed
4. The `IdGenerator` provides the span/trace identifiers, it must be compatible with your collector (`hex` for zipkin for example)
5. Finally the clock enables to timestamp the span and compute its duration

---
**IMPORTANT**

If you reuse `AccumulatingSpanCollector`, it is automatically closed with the valve "stop" phase.
You can combine the accumulator with `ZipkinFlusher` `onFlush` implementation to flush to a zipkin collector v2.

---
