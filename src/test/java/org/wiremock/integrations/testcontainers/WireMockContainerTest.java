package org.wiremock.integrations.testcontainers;

import org.junit.Rule;
import org.junit.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class WireMockContainerTest {

    @Rule
    public WireMockContainer wiremockServer = new WireMockContainer("2.35.0")
            .withStubResource("hello", WireMockContainerTest.class, "hello-world.json");

    @Test
    public void smokeTest() throws Exception {
        final HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(wiremockServer.getRequestURI("hello"))
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
