---
sidebar_position: 2
---

# Setup

DC Comics framework uses three main modules:

* BoM: the main Bill Of Meterial defining all versions of the framework
* Extension: it's the DC Comics Meta Extension bringing all 
* Extensions: additional custom extensions bringing new DC Comics specific features


## Maven

```xml
<dependencyManagement>
	<dependencies>
		<dependency>
			<groupId>com.dremio.cloud.comics</groupId>
			<artifactId>dc-comics-bom</artifactId>
			<version>${dc-comics.version}</version>
			<type>pom</type>
			<scope>import</scope>
		</dependency>
	</dependencies>
</dependencyManagement>

<dependencies>
	<dependency>
		<groupId>com.dremio.cloud.comics</groupId>
		<artifactId>dc-comics-extensions</artifactId>
	</dependency>
</dependencies>
```

