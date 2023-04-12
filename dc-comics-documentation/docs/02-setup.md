---
sidebar_position: 2
---

# Setup

DC Comic framework uses three main modules:

* API: the runtime API, it is the runtime API, mainly resolution/look-up oriented.
* Build API: it is the API only required at build time. It's intended to be used to trigger the generation of the runtime classes using processor module.
* Processor: it contains the magic generating most of the runtime and making the framework efficient and light.

Therefore the project will generally get the **api** in scope **compile**, the **build api** in scope **provided** or **optional** and the **processor** either in scope **provided**/**optional** or just defined as an annotation processor in your compiler configuration.

---
**IMPORTANT**

The generation process assumes the annotation processor is aware of all classes, depending the tools used for generation you may need to disable incremental compilation to ensure all classes are seen by the generator.

---

## Maven

### Simplest

The simplest is to just add the API (scope `compile`) and processor (scope `provided`):

```xml
<dependencies>
	<dependency>
		<groupId>com.dremio.cloud.comic</groupId>
		<artifactId>dc-comic-api</artifactId>
		<version>${dc-comic.version}</version>
	</dependency>
	<dependency>
		<groupId>com.dremio.cloud.comic</groupId>
		<artifactId>dc-comic-processor</artifactId>
		<version>${dc-comic.version}</version>
		<scope>provided</scope>
	</dependency>
</dependencies>
```

---
**TIP**

It can be sane to compile your project with Maven (`mvn compile` or `mvn process-classes`) instead of relying on your IDE.
This is indeed a general rule but, in this case, it will avoif the pitfalls of a fake incremental compilation (compiling only a few source files using the precompiled project output).
This last case can lead to missing bean, you can obviously delete the `target` folder of your project to force your IDE to recompile but it is saner to just rely on a property compile phase.

---

### IDE/Jetbrains Idea

Until you configure IDEA to use Maven to compile, it can happen it compiles a single source (at least not the whole module property like Maven by default) so the output can miss some beans.
If it happens (`java: java.lang.IllegalArgumentException: Unsupported type: 'com.superbiz.MyStuff', ...` at compile time or `NoClassDefFoundError`/`No bean matching type '...'` at test/runtime for example),
then just `Rebuild` the project, command is in `Build` menu.

Ultimately just drop the `target`/`out` folder if it is not about adding a file but more about removing a file (incremental support of such a change is not great as of today - but this not specific to this project)).

### Use ECJ compiler (Eclipse)

For ECJ to work you need to ensure the argument `-sourcepath` is set in compiler configuration and import `plexus-compiler-eclipse` (Maven):

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <version>3.10.1</version>
  <configuration>
    <compilerId>eclipse</compilerId>
    <compilerArguments>
      <sourcepath>${project.basedir}/src/main/java</sourcepath>
    </compilerArguments>
  </configuration>
  <dependencies>
    <dependency>
      <groupId>org.codehaus.plexus</groupId>
      <artifactId>plexus-compiler-eclipse</artifactId>
      <version>2.12.1</version>
    </dependency>
  </dependencies>
</plugin>
```

### Do not expose processor in code completion

A more advanced option would be to define the api in scope `compile`, the build API in scope `provided` and the processor only in `maven-compiler-plugin`.

This option is more complex in terms of configuration but has the advantage to not expose the processor in the IDE (completion).

Here is what it can look like:

```xml
<project>
	<dependencies>
		<dependency>
			<groupId>com.dremio.cloud.comic</groupId>
			<artifactId>dc-comic-api</artifactId>
			<version>${dc-comic.version}</version>
		</dependency>
		<dependency>
			<groupId>com.dremio.cloud.comic</groupId>
			<artifactId>dc-comic-build-api</artifactId>
			<version>${dc-comic.version}</version>
			<scope>provided</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<version>3.10.1</version>
				<configuration>
					<annotationProcessorPaths>
						<annotationProcessorPath>
							<groupId>com.dremio.cloud.comic</groupId>
							<artifactId>dc-comic-processor</artifactId>
							<version>${dc-comic.version}</version>
						</annotationProcessorPath>
					</annotationProcessorPaths>
				</configuration>
			</plugin>
		</plugins>
	</build>
</project>
```

---
**IMPORTANT**

Disabling the incremental compilation there is generally a good idea, in particular on CI but not having the processor in provided scope will make your IDE no more able to generate properly classes in general.
A better option can be to stick to previous dependencies only option (by default Maven recompiles properly the module - don't set `<useIncrementalCompilation>false</useIncrementalCompilation>` e.g. it means do not use incrementation compilation).

---
