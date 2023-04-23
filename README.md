# Testcontainers Java module for WireMock

[![a](https://img.shields.io/badge/slack-slack.wiremock.org-brightgreen?style=flat&logo=slack)](http://slack.wiremock.org/)

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

## Usage

### Importing the dependency

```xml
    <dependency>
      <groupId>org.wiremock.integrations.testcontainers</groupId>
      <artifactId>wiremock-testcontainers-module</artifactId>
      <version>${see the releases}</version>
      <scope>test</scope>
    </dependency>
```

### Using the test container in JUnit 4/5

P.S: Javadoc is coming soon!

```java
import org.wiremock.integrations.testcontainers.WireMockContainer;
import org.junit.Rule;
import org.junit.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class WireMockContainerTest {

    @Rule
    public WireMockContainer wiremockServer = new WireMockContainer("2.35.0")
            .withMapping("hello", WireMockContainerTest.class, "hello-world.json");

    @Test
    public void helloWorld() throws Exception {
        final HttpClient client = HttpClient.newBuilder().build();
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(wiremockServer.getRequestURI("hello"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .GET().build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.body())
                .as("Wrong response body")
                .contains("Hello, world!");
    }
}
```

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

##### Sample code using JUnit 4
```java
public class WireMockContainerExtensionTest {
    @Rule
    public WireMockContainer wiremockServer = new WireMockContainer("2.35.0")
            .withMapping("json-body-transformer", WireMockContainerExtensionTest.class, "json-body-transformer.json")
            .withExtension("JSON Body Transformer", Collections.singleton("com.ninecookies.wiremock.extensions.JsonBodyTransformer"),
                    Collections.singleton(Paths.get("target", "test-wiremock-extension", "9cookies-wiremock-extensions.jar").toFile()));

    @Test
    public void testJSONBodyTransformer() throws Exception {
        final HttpClient client = HttpClient.newBuilder().build();
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(wiremockServer.getRequestURI("json-body-transformer"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"John Doe\"}")).build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.body()).as("Wrong response body")
                .contains("Hello, John Doe!");
    }
}
```

##### Sample code using JUnit 5
```java
@Testcontainers
public class WireMockContainerExtensionJUnit5Test {
    
    @Container
    public WireMockContainer wiremockServer = new WireMockContainer("2.35.0")
            .withMapping("json-body-transformer", WireMockContainerExtensionTest.class, "json-body-transformer.json")
            .withExtension("JSON Body Transformer", Collections.singleton("com.ninecookies.wiremock.extensions.JsonBodyTransformer"),
                    Collections.singleton(Paths.get("target", "test-wiremock-extension", "9cookies-wiremock-extensions.jar").toFile()));

    @Test
    public void testJSONBodyTransformer() throws Exception {
        final HttpClient client = HttpClient.newBuilder().build();
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(wiremockServer.getRequestURI("json-body-transformer"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"John Doe\"}")).build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.body()).as("Wrong response body")
                .contains("Hello, John Doe!");
    }
}
```
## Contributing

All contributions are welcome!
Just submit a pull request.

