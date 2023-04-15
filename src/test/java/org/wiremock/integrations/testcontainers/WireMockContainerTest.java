/*
 * Copyright (C) 2023 WireMock Inc, Oleg Nenashev and all project contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
            .withMapping("hello", WireMockContainerTest.class, "hello-world.json")
            .withMapping("hello-resource", WireMockContainerTest.class, "hello-world-resource.json")
            .withFileFromResource("hello-world-resource-response.xml", WireMockContainerTest.class, "hello-world-resource-response.xml");

    @Test
    public void helloWorld() throws Exception {
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
