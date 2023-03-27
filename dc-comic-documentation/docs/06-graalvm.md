---
sidebar_position: 6
---

# GraalVM (native-image)

To convert your application to a native binary - assuming your dependencies are native friendly like DC Comic, you can use Apache Geronimo Arthur maven plugin.

Assuming you use this main for example:

```java
package demo.dccomic;

import com.dremio.cloud.comic.api.ConfiguringContainer;
import com.dremio.cloud.comic.api.lifecycle.Start;
import com.dremio.cloud.comic.api.scope.ApplicationScoped;
import com.dremio.cloud.comic.build.api.event.OnEvent;
import com.dremio.cloud.comic.build.api.lifecycle.Init;

@ApplicationScoped
public class Greeter {
	@Init
	protected void init() {
		System.out.println("> Init");
	}

	public void onStart(@OnEvent final Start start) {
		System.out.println("> start: " + start);
	}

	public static void main(final String... args) {
		try (final var container = ConfiguringContainer.of().start()) {
			// no-op
		}
	}
}
```

You can just add this plugin:

```xml
<plugin>
	<groupId>org.apache.geronimo.arthur</groupId>
	<artifactId>arthur-maven-plugin</artifactId>
	<version>1.0.5</version>
	<configuration>
		<graalVersion>22.3.0.r17</graalVersion>
		<main>demo.dccomic.Greeter</main>	
	</configuration>
</plugin>
```

And run `mvn package arthur:native-image` and you will get your binary in `target/`.

---
**TIP**

If you are a purist, and depending your needs and Arthur version you can need to add the following configuration to avoid warnings:

```xml
<enableAllSecurityServices>false</enableAllSecurityServices>
<allowIncompleteClasspath>false</allowIncompleteClasspath>
```

---
