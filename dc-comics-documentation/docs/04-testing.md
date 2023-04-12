---
sidebar_position: 4
---

# Testing

## Example

```java
@DCComicSupport
class MyTest {
	@Test
	void run(@DCComic final MyService service) {
		// ...
	}
}
```

## Runner flavors

There are two runner flavors:

* `DCComicSupport` which starts and stops the container per test class,
* `MonoDCComicSupport` which starts and stops the container per JVM - faster but does not isolate all classes.

---
**TIP**

Using a custom JUnit 5 `Extension` where you set system properties in a static block, you can configure the container before it starts.
It is recommended to combine it in a custom annotation to control the ordering and ease the usage:

```java
@Target(TYPE)
@Retention(RUNTIME)
@MonoDCComicSupport
@ExtendsWith(MyAppSupport.MyConf.class)
public @interface MyAppSupport {
	class MyConf implements Extension {
		static {
			// do the configuration
			System.setProperty("...", "...");
		}
	}
}
```

Then simply replace DCComic annotation by `MyAppSupport`.

Alternatively you can register a test `ConfigurationSource` bean if you prefer but this extension option enables to also start global services like dependencies mock or a database.

---
