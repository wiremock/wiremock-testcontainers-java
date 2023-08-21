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

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.wiremock.integrations.testcontainers.util.SimpleHttpResponse;
import org.wiremock.integrations.testcontainers.util.SimpleHttpClient;

import java.nio.file.Paths;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the WireMock extension loading.
 * It uses multiple external Jars supplied by the Maven Dependency Plugin.
 */
@Testcontainers
class WireMockContainerExtensionsCombinationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockContainerExtensionsCombinationTest.class);

    @Container
    WireMockContainer wiremockServer = new WireMockContainer(WireMockContainer.WIREMOCK_2_LATEST)
            .withLogConsumer(new Slf4jLogConsumer(LOGGER))
            .withMapping("json-body-transformer", WireMockContainerExtensionsCombinationTest.class, "json-body-transformer.json")
            .withExtension("Webhook",
                    Collections.singleton("org.wiremock.webhooks.Webhooks"),
                    Collections.singleton(Paths.get("target", "test-wiremock-extension", "wiremock-webhooks-extension-2.35.0.jar").toFile()))
            .withExtension("JSON Body Transformer",
                    Collections.singleton("com.ninecookies.wiremock.extensions.JsonBodyTransformer"),
                    Collections.singleton(Paths.get("target", "test-wiremock-extension", "wiremock-extensions-0.4.1-jar-with-dependencies.jar").toFile()));

    @Test
    void testJSONBodyTransformer() throws Exception {
        // given
        String url = wiremockServer.getUrl("/json-body-transformer");
        String body = "{\"name\":\"John Doe\"}";

        // when
        SimpleHttpResponse response = new SimpleHttpClient().post(url, body);

        // then
        assertThat(response.getBody())
                .as("Wrong response body")
                .contains("Hello, John Doe!");
    }
}
