package org.wiremock.integrations.testcontainers.testsupport.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class TestHttpClient {
    private final HttpClient client;

    private TestHttpClient(HttpClient.Version version) {
        client = HttpClient.newBuilder().version(version).build();
    }

    public static TestHttpClient newInstance() {
        return new TestHttpClient(HttpClient.Version.HTTP_1_1);
    }

    public HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> get(String uri) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();
        return send(request);
    }

    public HttpResponse<String> post(String uri, String body) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder()
                .uri(URI.create(uri))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return send(request);
    }

    private static HttpRequest.Builder newRequestBuilder() {
        return HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(10));
    }
}
