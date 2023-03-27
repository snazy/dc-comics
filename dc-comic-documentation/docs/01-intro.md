---
sidebar_position: 1
---

# DC Comic

DC (Dremio Cloud) Comic provides:

* a very light and fast IoC framework, build oriented
* a set of features that any cloud application can leverage with no-code

## Core Values

For cloud applications, being the most reactive possible is a key criteria. So DC Comic chose to:

* Be build time oriented: only delegate to runtime the bean resolution (to enable dynamic module aggregation) and not the bean and model discovery nor proxy generation,
* Stay flexible: even if the model is generated at build time, you can generally still customize it by removing a bean and adding your own one,
* Be native friendly: some applications need to be native to start very fast and bypass the classloading and a bunch of JVM init. FOr that purpose, we ensure the framework is GraalVM friendly.

## Features

* Field/constructor injections
* Contexts/scopes
** Default scope (`@DefaultScoped`) creates an instance per lookup/injection
** Application scope (`@ApplicationScoped`) creates an instance per IoC container and instantiated it lazily (at first call)
* Event bus
** `Start`/`Stop` events are fired zith IoC container related lifecycle hooks
* `@Init`/`@Destroy` reacts to the bean lifecycle
* `Optional<MyClass>` injections
* Cloud friendly (kubernetes configmap compliant) configuration
* Additional modules that applications can add to:
** [`dc-comic-observability`](observability) adds common observability primitives (logging, metrics, healthcheck, ...).
** [`dc-comic-grpc`](grpc) provides [gRPC](https://grpc.io/) integration, including telemetry observer.
** `dc-comic-pubsub` provides abtractions layer on PubSub providers (Google PubSub, Apache Kafka, Apache ActiveMQ, ...) including Saga pattern implementation.
** `dc-comic-identity` provides cloud identity service integration (such as [auth0](https://auth0.com/), and authentication manager provider (with different token mechanism). 
** [`dc-comic-testing`](testing) provides JUnit 5 integration enabling to test easily your code.
** [`dc-comic-json`](json) provides JSON records mapping support without reflection
** [`dc-comic-http-server`](http-server) provides an Apache Tomcat abstraction enabling to write any HTTP endpoint in an efficient and GraalVM fiendly manner

TIP: take a look on the [examples](examples) page.

### No interceptor support

Since some years we got used to see declarative interceptors (annotations) like in this snippet:

```java
public class MyBean {

	@Traced
	public void doSomething() {
		// ...
	}

}
```

These are great and the IoC container is actually linking the annotation to an implementation (a bean in general) which intercepts the call.
This is not bad but has some design pitfalls:

* Most interceptors will use parameters and for such a generic approach to work, it needs an `Object[]` (or `List`) of parameters. This is really not fast (it requires to allocate an array for that purpose).
* It requires to know and understand the rules between class interceptors, method interceptors, appending/overriding when relevant plus the same with parent classes. All that can quickly become complex.
* It is often static: once put on a method disabling an interceptor requires the underlying library to be able to do that or to use some advanced customization at startup to do it.

For these reasons, we think that we don't need an interceptor solution in DC Comic, but we don't say the underlying feature is pointless, not at all.
However, thanks to a more modern programming style, we can use a more functional approach to solve the same problem.
Therefore, previous example would rather become:

```java
public class MyBean {
	public void doSomething() {
		tracing(() -> {
			// ...
		});
	}
}
```

The big advantage is you can use some static utility if you want but also rely on beans and even combine more efficiently interceptions in a custom and configurable fashion:

```java
public class MyBean {
	public void doSomething() {
		tracing(() -> timed(() -> logged(() -> {
			// ...
		})));
}
```

can become:

```java
public class MyBean {
	@Injection
	MyObjervabilityService obs;

	public void doSomething() {
		obs.instrumented(() -> {
			// ...
		});
	}
}
```

If you compare the case with parameters it is way more efficient in general since you just do a standard parameter passing call:

```java
public class MyBean {
	@Injection
	EntityManager em; // assume the application used JPA - not required, just for the example

	@Injection
	JpaService jpa; // custom bean to handle transactions for example

	public void store(final Transaction tx) {
		tracing(
			// no Object[] created for an interceptor
			// and no reflection to extract the id
			tx.id(),
			() -> jpa.tx(() -> em.persist(tx)));
	}
}
```

---
**TIP**

Going with this solution can, however, get the _chaining lambda_ pitfall (a.k.a. _callback hell_ in JavaScript).
To Solve this one, we encourage you to ensure your "interceptor" can be chained properly using the same kind of callback.

Here is an example (the important part is more the signature than the fact it is a `static` method or a bean method):

```java
public static <T> Supplier<T> interceptor1(String marker, Map<String, String> data, Supplier<T> nested) {
	return () -> {
		logger.info(message(marker, data)); // interceptor role
		return task.get(); // intercepted business, "ic.proceed()" in Jakarta interceptor API
	};
}

public static <T> Supplier<T> interceptor12(Params params, Supplier<T> nested) {
	// same kind of logic for the impl
}
```

Thanks this definition which commonly agreed to use `Supplier<T>` as the intercepted call and the fact interceptor methods return a call and not execute it directly, you can chain them more easily:

```java
public void storeCustomer(final Customer customer) {
	interceptor2(
		Params.of(customer),
		interceptor1(
			"incoming-customer", Map.of("id", customer.id()),
			() -> {
				// business code
			}))
	.get(); // trigger the actual execution, it is the terminal operation for the chain
}
```

If you want to go further you can use a `Stream` to represent that.
Now an interceptor is a `Function<Supplier<T>, Supplier<T>>` so if you define the list of interceptors in a `Stream`, then you can just reduce them using the business function/logic as identity to have the actual invocation and execute it.

```java
public void storeCustomer(final Customer customer) {
    Stream.<Function<Supplier<Void>, Supplier<Void>>>of(
                // reversed chain of interceptor (i1 will be executed before i2)
                delegate -> interceptor2(Params.of(customer), delegate),
                delegate -> interceptor1("incoming-customer", Map.of("id", customer.id()), delegate)
        )
        // merge the stream of interceptors as one execution wrapper
        .reduce(identity(), Function::andThen)
        .apply(() -> { // apply to the actual business logic
            System.out.println(">Business");
            return null;
        })
        .get(); // execute it	
}
```

Indeed in practice you can extract that kind of code in an utility and use something like:

```java
// utility
public static <T> T intercepted(final Supplier<T> execution, final Function<Supplier<T>, Supplier<T>>... interceptors) {
    return Stream.of(interceptors)
            .reduce(identity(), Function::andThen)
            .apply(execution)
            .get();
}

// usage
intercepted(
    () -> { // business logic
        System.out.println(">Business");
        return null;
    },
    // interceptors
    delegate -> interceptor2(Params.of(customer), delegate),
    delegate -> interceptor1("incoming-customer", Map.of("id", customer.id()), delegate)
);
```

This is what the class `com.dremio.cloud.comic.api.composable.Wraps` does.

---

---
**TIP**

Your interceptor can work with `CompletionStage` to add some behavior before/after the call even if the result is not computed synchronously.

---

## Limitations

---
**NOTE**

There are limitations _as of today_, none are _technically_ strong limitations. We can fix at a later point if needed.

---

* A no-arg constructor must be available for any class bean.
* If a method producer bean is `AutoCloseable` then it will be automatically closed.
* Event methods can not be package scope if the enclosing bean uses a subclass proxy (like `@ApplicationScoped` context).
* Constructor injections are supported but for proxied scopes (`@ApplicationScoped` for example), it requires a default no-arg constructor (in scope `protected` or `public`) in the class (if not existing the instantiation constructor will be called with null parameters).
* Event bus listeners can only have the event as method parameter.
* Only classes are supported exception for method producers which can return a `ParameterizedType` (e.g. `List<String>`) but injections must exactly match this type and `List`/`Set` injections are handled by looking up all beans matching the parameter.

## Setup

See [setup](setup) page to see how to get your cloud application project started.

## Extensions

* [Logging & Observability](observability)
* [JSON](json)
* [HTTP Server](http-server)
* [HTTP Client](http-client)
* [Identity](identity)
* [gRPC](grpc)
