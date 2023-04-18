---
sidebar_position: 9
---

# HTTP Client

DC Comic HTTP Client is based on `java.net.http.HttpClient`.
It is configured with `ExtendedHttpClientConfiguration` which enables to:

* optionally provide a configured `HttpClient` instance - otherwise the default JVM one is used
* optionally provide a set `RequestListener` which listen for requests.

## Request Listeners

Request listener is a callback triggered before and after each request.
It enables to store data before the requests in the caller context and execute some callback once the request is finished.
To pass data between both steps (since the request can be asynchronous or not) it uses a `State` which can hold any data the listener needs.

---
**TIP**

If you write custom listeners (to add OpenTelemetry capabilities for example), you can make them implement `AutoCloseable` and when closing the HTTP client the method will be called automatically.

---

### Default Listeners

#### `DefaultTimeout`

This listener is pretty strqight forward, if the request does not have a timeout, it sets it to the default one configured in the listener.
It enables to enforce a global timeout to all requests.

#### `SetUserAgent`

This listener enforces a custom user-agent value.
It defaults to chrome one.

#### `ExchangeLogger`

This listener enables to force all exchanges to be logged.

#### `FilteringListener`

This listener wraps another listener to filter the calls to `before`/`after` callbacks either based on the request or on the response.
It can be useful to ignore some data (for example to only capture errors in `HARDumperListener`).

---
**TIP**

For `ignoredPaths` you can use the syntax `regex:<java regex>` to match more than an exact path at once.

---

#### `HARDumperListener`

This listener enables to capture a `HAR` dump of all exchanges which went through the client.
It can be very useful to generate some tests data you replay with a HAR server in a test or a demo environment.

It comes with its companion `HARHttpClient` which enables to replay a HAR without actually doing the requests.

---
**TIP**

A sibling listener called `NDJSONDumperListener` exists and allows to log each entry in a ND-JSON output (better in case of error but not standard).
It can be combined to `NDJSONHttpClient` to replay captured requests (in order).

---

---
**IMPORTANT**

The `HttpClient` companions of these "capture" listeners must be used in sequential order (until you know you can parallelize them all) because there is no matching logic as of today of requests/responses to enable a wider reuse of captures.

---

### Sample usage

```java
final var conf = new ExtendedHttpClientConfiguration();

// (optional) force a custom SSL context
conf.setDelegate(
  HttpClient.newBuilder()
    .sslContext(getSSLContext())
    .build());

// (optional) force custom listeners
conf.setListeners(List.of(
      new AutoTimeoutSSetter(Duration.ofSeconds(3600)),
      new AutoUserAgentSetter(),
      new ExchangeLogger(
            Logger.getLogger(getClas().getName()),
            Clock.systemUTC(),
            false)));
```

### (Open) Tracing

---
**IMPORTANT**

This is not in the same module, you must add `dc-comic-tracing` module to get this feature.

---

`tracing` module provides a Tomcat valve you can set up on your web container to add tracing capabilities to your Tomcat:

```java
final var listener = new TracingListener(
  new ClientTracingConfiguration(), <1>
  accumulator, <2>
  new IdGenerator(IdGenerator.Type.HEX), <3>
  new ServletContextAttributeEvaluator(servletRequest), <4>
  systemUTC()); <5>

<6>
final var configuration = new ExtendedHttpClientConfiguration()
      .setRequestListeners(List.of(listener));
final var client = new ExtendedHttpClient(configuration);
```

1. The configuration enables to customize the span tags and headers to enrich the request with
2. The accumulator is what will send/log/... the spans once aggregated, ensure to configure it as needed
3. The `IdGenerator` provides the span/trace identifiers, it must be compatible with your collector (`hex` for zipkin for example)
4. Actually a `Supplier<Span>`, the span evaluator enables to get parent span, here from the `request` in a `webserver-tomcat` using `Tracingvalve` but any evaluation will work
5. The clock enables to timestamp the span and compute its duration
6. Finally, add the listener to your http client configuration and create your client

---
**IMPORTANT**

The accumulator should generally be closed if you reuse `AccumulatingSpanCollector`. You can combine it with `ZipkinFlusher` to flush to a zipkin collector v2.

---

## Kubernetes client

`kubernetes-client` module provides a HTTP Client already configured for Kubernetes in cluster connection (from a pod).
This will typically be used from an operator or cloud native application to call Kubernetes API using a plain and very light HTTP client from the JVM.

---
**TIP**

Indeed, you can combine it with the enhanced HTTP Client configuring it in the `KubernetesClientConfiguration`.
However, it is recommended to do it using `setClientWrapper` on the configuration and pass the automatically created client to `ExtendedHttpClientConfiguration.setDelegate` to avoid to have to handle the `SSLContext` yourself.

---

Usage:

```java
final var conf = new KubernetesClientConfiguration()
    .setClientWrapper(client -> new ExtendedHttpClient(new ExtendedHttpClientConfiguration().setDelegate(client)));
final var k8s = new KubernetesClient(conf);

// now call any API you need:
final var response = k8s.send(
    HttpRequest.newBuilder()
        .GET()
        .uri(URI.create(
            "https://kubernetes.api/api/v1/namespaces/" + k8s.namespace().orElse("default") + "/configmaps?" +
                "includeUninitializedd=false&" +
                "limit=1000&" +
                "timeoutSeconds=600")
        .header("Accept", "application/json")
        .build(),
    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
// handle the response
```

---
**IMPORTANT**

As you can see, there is no need to pas the token to the request, it is done under the hood by the `KubernetesClient`.
The other important note is that `https://kubernetes.api` is automatically replaced by the `conf.getMaster()` value.
This enables your code to stay more straight forward in general but if you pass them, the client will handle it propertly too.

---