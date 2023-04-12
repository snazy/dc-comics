---
sidebar_position: 3
---

# Examples

## Defining a Bean __without__ injections

If a bean has an injection or an event listener it will be automatically defined as a bean, but in some cases, you want to define a bean on a plain class without any injection nor event listener.
For such a case, you can mark the class with `@Bean`:

```java
@Bean
public class MyService {
	public int magicNumber() {
		return 42;
	}
}
```

This simple definition enables you to inject this bean in other beans.

## Defining a Bean with injection(s)

The common use case of a framework is to get injected some bean without exactly knowing how it was created or initialized.
Here is how to get an injection with DC Comic:

```java
public class MyService {
	@Injection
	ConfigService config;

	// ...
}
```

---
**IMPORTANT**

Do **NOT** use `private` fields, it is not yet supported.

Alternatively, you can use constructor injections:

```java
@ApplicationScoped // any marker making it a bean, can be as simple as @Bean
public class MyService {
	private final ConfigService config; <1>

	public MyService(final ConfigService service) { <2>
		this.config = service;
	}

	protected MyService() { <3>
		this.config = null;
	}
}
```

1. The injected values can be defined normally since injections happen with the constructor, you can even precompute the data you need in the bean and not store the injection itself,
2. The constructor injection (the selected one is the most public one - `public` wins over `protected`) and with the most parameters,
3. The no-arg constructor - only needed for scopes using subclassing like `@ApplicationScoped`.

---

## Create a bean in a custom fashionm without a class

It can happen you need to reuse some custom factory and code the initialization of a bean.
For such use case, you can mark a method with `@Bean`.
Injections can be done in the enclosing bean if needed:

```java
@DefaultScoped // enclosing class is a bean
public class MyProducer {
	@Injection
	ConfigService conf;

	@Bean
	public DataSource dataSource() {
		return new DataSourceFactory(conf).create();
	}
}
```

---
**IMPORTANT**

As of today, you can mark the producer method with a scope but lazy scopes (like `@ApplicationScoped` are not really lazy until you implement yourself the lazyness - but scope is respected, i.e. if it is `@ApplicationScoped` it will be a singleton).

---

---
**TIP**

If the returned type implements `AutoCloseable`, `close()` will be called to destroy the bean instance(s).

---

## Injection of a list/set

---
**IMPORTANT**

Injections of `List` or `Set` are done by resolving the parameter type of the injection.

---

```java
public class MyService {
	@Injection
	List<MySpi> implementations;
}
```

---
**TIP**

You can put on the implementations (or beans) the annotation `@Order` to sort their position in the `List` (ignored for `Set`).

---

## Listen to an event

Beans can communicate loosely between them thanks to events.
The bus is synchronous and sorted using `@Order` annotation.

```java
public class MyListener {
	public void onStart(@Event final Start start) {
		System.out.println("Application started");
	}

	public void onStop(@Event final Stop stop) {
		System.out.println("Application stopped");
	}
}
```

Example of ordered event listener (default being `1000`):

```java
public void onStart(@Event @Order(990) final Start start);
```

---
**TIP**

Listening to `Start` event can enable a lazy instance (`@ApplicationScoped`) to be forced to be initilized.

---

## Emit an event

To emit an event simply inject the `Emitter` and send the needed event:

```java
public class CustomerService {
	@Injection
	Emitter emitter;

	public void createCustomer(final Customer customer) {
		emitter.emit(customer);
	}
}
```

## Create a configuration model

A configuration model is a record marked with `@RootConfiguration`:

```java
@RootConfiguration("server")
public record ServerConfiguration(int port, String accessLogPattern) {}
```

This simple configuration will read the system properties `server.port`, `server.accessLogPattern` (or environment variables `SERVER_PORT`, `SERVER_ACCESSLOGPATTERN`) to fill the values.
The instance of `ServerConfiguration` can be injected in any bean:

```java
@Bean
public class MyServer {
	private final ServerConfiguration conf;

	public MyServer(final ServerConfiguration conf) {
		this.conf = conf;
	}

	// ...
}
```

If you want to customize the name of the property you can use `@Property`.

Finally, you can register your own source of values creating s bean of type `ConfigurationSource`.

---
**IMPORTANT**

`List<OtherConfig>` are supported, but you must set in the configuration `<prefix for this list>.length` to the length value of the list then the nested instances are configured using `<prefix>.<index>` starting at index 0. For example: `myconf.mylist.0.name=foo`.

---

## Create a JSON model

A JSON model is a record marked with `@JsonModel`:

```java
@JsonModel
public record ServerConfiguration(int port, String accessLogPattern) {}
```

Then simply inject the `JsonMapper` in any bean to read/write such a model:

```java
@Bean
public class MyServer {
	private final JsonMapper mapper;

	public MyServer(final JsonMapper mapper) {
		this.mapper = mapper;
	}

	// ... mapper.toString(serverconf) / mapper.fromString(ServerConfiguration.class, "{}");
}
```

## Handle unknown JSON attributes

A JSON model is a record marked with `@JsonModel`:

```java
@JsonModel
public record MyModel(
	// known attribute
	String name,
	// unknown attributes/extensions
	@JsonOthers Map<String, Object> extensions) {}
```

This will match this JSON:

```java
{
	"name": "comic",
	"x-foo": true,
	"boney": "M"
}
```

And convert it to the following record mapping: `MyModel[name=comic, extensions={x-foo=true,bonery=M}]`.

## Define a custom HTTP endpoint

### Implement a custom explicit Endpoint bean

```java
@Bean
public class MyEndpoint implements Endpoint {
	...
}
```

### Implement a custom implicit Endpoint

```java
@HttpMatcher(...)
public CompletionStage<Response> myEndpoint(final Request request) {
	...
}

// or

@HttpMatcher(...)
public Response myEndpoint(final Request request) {
	...
}
```

## Start the container

To launch the application you need to start the container.
It is done in two phases:

* Configure the runtime.
* Launch the runtime.

Here is how to do it:

```java
try (
	final var container = ConfiguringContainer
			.of() <1>
			.start() <2>
	) {
		// use the container or just await for the end of the application
}
```

1. Get a `ConfiguringContainer` which enables you to disable bean autodiscovery, to replace beans, etc...
2. Launch the runtime container (you can look up beans there).

---
**TIP**

You can also just reuse `com.dremio.cloud.comic.api.main.Launcher` main which will start the default container.
You can implement a custom `Awaiter` to not let the container shutdown immediately if you need - webserver does it by default.
Finally you can also, using this launcher, inject `Args` to read the main arguments.

---

## Test with JUnit 5

```java
@ComicSupport <1>
class ComicSupportTest {
	@Test
	void run(@Comic final Emitter emitter) { <2>
		assertNotNull(emitter);
	}
}
```

1. Mark the class to run tests under a container context (it is started/stopped automatically).
2. Inject container beans in test parameters (mark them with `@Comic`).

```java
@MonoComicSupport
class ComicSupportTest {
	// same as before
}
```
