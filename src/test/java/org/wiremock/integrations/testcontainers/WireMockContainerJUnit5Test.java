package org.wiremock.integrations.testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class WireMockContainerJUnit5Test {

  @Container
  public WireMockContainer wiremockServer = new WireMockContainer("2.35.0")
      .withMapping("hello", WireMockContainerTest.class, "hello-world.json")
      .withMapping("hello-resource", WireMockContainerTest.class, "hello-world-resource.json")
      .withFileFromResource("hello-world-resource-response.xml", WireMockContainerTest.class,
          "hello-world-resource-response.xml");

  @Test
  public void helloWorld() throws Exception {
    final HttpClient client = HttpClient.newBuilder().build();
    final HttpRequest request = HttpRequest.newBuilder()
        .uri(wiremockServer.getRequestURI("hello"))
        .timeout(Duration.ofSeconds(10))
        .header("Content-Type", "application/json")
        .GET().build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.body())
        .as("Wrong response body")
        .contains("Hello, world!");
  }

  @Test
  public void helloWorldFromFile() throws Exception {
    final HttpClient client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .build();

    HttpRequest request = HttpRequest.newBuilder()
        .uri(wiremockServer.getRequestURI("hello-from-file"))
        .timeout(Duration.ofSeconds(10))
        .header("Content-Type", "application/json")
        .GET()
        .build();

    HttpResponse<String> response =
        client.send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.body())
        .as("Wrong response body")
        .contains("Hello, world!");
  }
}
