# Testcontainers Java module for WireMock

[![GitHub release (latest by date)](https://img.shields.io/github/v/release/wiremock/wiremock-testcontainers-java)](https://github.com/wiremock/wiremock-testcontainers-java/releases)
[![Slack](https://img.shields.io/badge/slack-slack.wiremock.org-brightgreen?style=flat&logo=slack)](https://slack.wiremock.org/)
[![GitHub contributors](https://img.shields.io/github/contributors/wiremock/wiremock-testcontainers-java)](https://github.com/wiremock/wiremock-testcontainers-java/graphs/contributors)

> NOTE: This project is under development, the GitHub Packages release is coming soon.
> Not all WireMock features are supported at the moment,
> and there might be incompatible changes before the 1.0 release.
> Contributions are welcome!

This module allows provisioning the WireMock server 
as a standalone container
within your unit test, based on [WireMock Docker](https://github.com/wiremock/wiremock-docker).

While you can run [WireMock Java](https://github.com/wiremock/wiremock)
with the same result for the most of the use-cases,
it might be helpful to isolate JVMs or to run on
Java versions and platforms not supported by WireMock.
A common example is using Wiremock 3.x with Java 1.8.

## Compatibility

The module is compatible with the following WireMock versions:

- WireMock (aka WireMock Java) `2.0.0` and above
- WireMock (aka WireMock Java) `3.x` versions.
  Note that the official image for WireMock 3 is yet to be released and verified ([issue #59](https://github.com/wiremock/wiremock-testcontainers-java/issues/59))

Other WireMock implementations may work but have not been tested yet.
Please feel free to contribute the integration tests and compatibility layers!

## Usage

### Importing the dependency

At the moment the module is published to GitHub Packages only,
see [Issue #56](https://github.com/wiremock/wiremock-testcontainers-java/issues/56)
for publishing to Maven Central.
For the moment, you can use the authenticated GitHub Packages server or
[JitPack](https://jitpack.io/) to add the dependency in your projects.

#### Maven / JitPack

```xml
  <dependencies>
    <dependency>
      <groupId>com.github.wiremock</groupId>
      <artifactId>wiremock-testcontainers-java</artifactId>
      <version>${wiremock-testcontainers.version}</version>
      <scope>test</scope>
    </dependency>
    <!-- .... Other Dependencies -->
  </dependencies>

  <repositories>
    <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
    </repository>
  </repositories>
```

<details>
<summary>
Gradle / JitPack
</summary>

#### Gradle / JitPack

```groovy
  allprojects {
		repositories {
			maven { url 'https://jitpack.io' }
		}
	}

  dependencies {
		testImplementation 'com.github.wiremock:wiremock-testcontainers-java:${wiremock-testcontainers.version}'
	}

```
</details>

<details>
<summary>
Maven / GitHub Packages
</summary>

#### Maven / GitHub Packages

GitHub Packages uses the official Maven coordinates,
but you will need to configure the server and authentication.

```xml
    <dependency>
      <groupId>org.wiremock.integrations.testcontainers</groupId>
      <artifactId>wiremock-testcontainers-module</artifactId>
      <version>${see the releases}</version>
      <scope>test</scope>
    </dependency>
```

</details>

### Using the test container in JUnit 4/5

P.S: Javadoc is coming soon!

#### Sample Code using JUnit 5

```java
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.*;
import org.wiremock.integrations.testcontainers.testsupport.http.*;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class WireMockContainerJunit5Test {

    @Container
    WireMockContainer wiremockServer = new WireMockContainer("wiremock/wiremock:2.35.0")
            .withMapping("hello", WireMockContainerJunit5Test.class, "hello-world.json");

    @Test
    void helloWorld() throws Exception {
        // given
        String url = wiremockServer.getUrl("/hello");

        // when
        HttpResponse response = new TestHttpClient().get(url);

        // then
        assertThat(response.getBody())
                .as("Wrong response body")
                .contains("Hello, world!");
    }
}
```

#### Sample Code using JUnit 4

<details>
<summary>
Show Code
</summary>

```java
import org.junit.*;
import org.wiremock.integrations.testcontainers.testsupport.http.*;

import static org.assertj.core.api.Assertions.assertThat;

public class WireMockContainerJunit4Test {

    @Rule
    public WireMockContainer wiremockServer = new WireMockContainer("wiremock/wiremock:2.35.0")
            .withMapping("hello", WireMockContainerJunit4Test.class, "hello-world.json");

    @Test
    public void helloWorld() throws Exception {
        // given
        String url = wiremockServer.getUrl("/hello");

        // when
        HttpResponse response = new TestHttpClient().get(url);

        // then
        assertThat(response.getBody())
                .as("Wrong response body")
                .contains("Hello, world!");
    }
}
```
</details>    
    
### Using WireMock extensions

The API supports adding [WireMock extensions](https://wiremock.org/docs/extending-wiremock/)
to the test container.
The extension can be sourced from the classpath for bundled extensions,
or added from the JAR file in the initializer.

#### Using external extensions

For the external extensions,
an extension Jar should be pulled to the test directory before running the test.
[Apache Maven Dependency Plugin](https://maven.apache.org/plugins/maven-dependency-plugin/) can be used for this purpose.
Make sure that all dependencies of the extension JAR, if any,
are also included.

Below you can see an examples of using the _JSON Body Transformer_ extension
from the [9cookies/wiremock-extensions](https://github.com/9cookies/wiremock-extensions).

Copying the dependency:

```xml
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <version>3.5.0</version>
        <executions>
          <execution>
            <id>copy</id>
            <phase>package</phase>
            <goals>
              <goal>copy</goal>
            </goals>
            <configuration>
              <artifactItems>
                <artifactItem>
                  <groupId>com.ninecookies.wiremock.extensions</groupId>
                  <artifactId>wiremock-extensions</artifactId>
                  <version>0.4.1</version>
                  <classifier>jar-with-dependencies</classifier>
                </artifactItem>
              </artifactItems>
              <outputDirectory>${project.build.directory}/test-wiremock-extension</outputDirectory>
            </configuration>
          </execution>
        </executions>
      </plugin>
```

Mapping definition:

```json
{
  "request": {
    "method": "POST",
    "url": "/json-body-transformer"
  },
  "response": {
    "status": 201,
    "headers": {
      "content-type": "application/json"
    },
    "jsonBody": {
      "message": "Hello, $(name)!"
    },
    "transformers" : ["json-body-transformer"]
  }
}
```

Test sample:

##### Sample code using JUnit 5

```java
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.*;
import org.wiremock.integrations.testcontainers.testsupport.http.*;

import java.nio.file.Paths;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class WireMockContainerExtensionJunit5Test {

    @Container
    WireMockContainer wiremockServer = new WireMockContainer("wiremock/wiremock:2.35.0")
            .withMapping("json-body-transformer", WireMockContainerExtensionJunit5Test.class, "json-body-transformer.json")
            .withExtension("JSON Body Transformer",
                    Collections.singleton("com.ninecookies.wiremock.extensions.JsonBodyTransformer"),
                    Collections.singleton(Paths.get("target", "test-wiremock-extension", "wiremock-extensions-0.4.1-jar-with-dependencies.jar").toFile()));

    @Test
    void testJSONBodyTransformer() throws Exception {
        // given
        String url = wiremockServer.getUrl("/json-body-transformer");
        String body = "{\"name\":\"John Doe\"}";

        // when
        HttpResponse response = new TestHttpClient().post(url, body);

        // then
        assertThat(response.getBody()).as("Wrong response body")
                .contains("Hello, John Doe!");
    }
}
```

##### Sample code using JUnit 4

<details>
<summary>
Show Code
</summary> 
    
```java
import org.junit.*;
import org.wiremock.integrations.testcontainers.testsupport.http.*;

import java.nio.file.Paths;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class WireMockContainerExtensionJunit4Test {

    @Rule
    public WireMockContainer wiremockServer = new WireMockContainer("wiremock/wiremock:2.35.0")
            .withMapping("json-body-transformer", WireMockContainerExtensionJunit4Test.class, "json-body-transformer.json")
            .withExtension("JSON Body Transformer",
                    Collections.singleton("com.ninecookies.wiremock.extensions.JsonBodyTransformer"),
                    Collections.singleton(Paths.get("target", "test-wiremock-extension", "wiremock-extensions-0.4.1-jar-with-dependencies.jar").toFile()));

    @Test
    public void testJSONBodyTransformer() throws Exception {
        // given
        String url = wiremockServer.getUrl("/json-body-transformer");
        String body = "{\"name\":\"John Doe\"}";

        // when
        HttpResponse response = new TestHttpClient().post(url, body);

        // then
        assertThat(response.getBody()).as("Wrong response body")
                .contains("Hello, John Doe!");
    }
}
```  
</details>

## Contributing

This repository is implemented as a standard Maven project.
All contributions are welcome!
Just submit a pull request.

See [this page](https://wiremock.org/docs/participate/) for a generic WireMock Contributor Guide
    
