# TestContainers module for WireMock

> NOTE: This project is under development.
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

Import the dependency:

```xml
    <dependency>
      <groupId>org.wiremock.integrations.testcontainers</groupId>
      <artifactId>wiremock-testcontainers-module</artifactId>
      <version>${see the releases}</version>
      <scope>test</scope>
    </dependency>
```

Use it in your Unit tests.
Javadoc is coming soon!

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
        final HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1).build();

        HttpRequest request = HttpRequest.newBuilder()
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

## Contributing

All contributions are welcome!
Just submit a pull request.

