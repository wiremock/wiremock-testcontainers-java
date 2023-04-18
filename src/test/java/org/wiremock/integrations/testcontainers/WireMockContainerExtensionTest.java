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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the WireMock extension loading.
 * It uses the external Jar supplied by the Maven Dependency Plugin.
 */
public class WireMockContainerExtensionTest {

    public static final Logger LOGGER =  LoggerFactory.getLogger(WireMockContainerExtensionTest.class);

    @Rule
    public WireMockContainer wiremockServer = new WireMockContainer("2.35.0")
            .withStartupTimeout(Duration.ofSeconds(60))
            .withMapping("json-body-transformer", WireMockContainerExtensionTest.class, "json-body-transformer.json")
            .withExtension("JSON Body Transformer", Collections.singleton("com.ninecookies.wiremock.extensions.JsonBodyTransformer"),
                    Collections.singleton(Paths.get("target", "test-wiremock-extension", "wiremock-extensions-0.4.1-jar-with-dependencies.jar").toFile()));

    @Before
    public void before() {
        wiremockServer.followOutput(new Slf4jLogConsumer(LOGGER));
    }

    @Test
    public void testJSONBodyTransformer() throws Exception {
        final HttpClient client = HttpClient.newBuilder().build();
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(wiremockServer.getRequestURI("json-body-transformer"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"John Doe\"}")).build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.body())
                .as("Wrong response body")
                .contains("Hello, John Doe!");
    }

}
