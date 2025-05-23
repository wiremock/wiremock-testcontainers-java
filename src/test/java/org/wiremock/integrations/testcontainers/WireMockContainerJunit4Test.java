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
import org.wiremock.integrations.testcontainers.testsupport.http.HttpResponse;
import org.wiremock.integrations.testcontainers.testsupport.http.TestHttpClient;

import static org.assertj.core.api.Assertions.assertThat;

public class WireMockContainerJunit4Test {

    private static final int WIREMOCK_DEFAULT_PORT = 8080;
    private static final int ADDITIONAL_MAPPED_PORT = 8443;

    @Rule
    public WireMockContainer wiremockServer = new WireMockContainer(TestConfig.WIREMOCK_DEFAULT_IMAGE)
            .withMapping("hello", WireMockContainerTest.class, "hello-world.json")
            .withMapping("hello-resource", WireMockContainerTest.class, "hello-world-resource.json")
            .withFileFromResource("hello-world-resource-response.xml", WireMockContainerTest.class, "hello-world-resource-response.xml")
            .withExposedPorts(ADDITIONAL_MAPPED_PORT);

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

    @Test
    public void helloWorldWithoutLeadingSlashInPath() throws Exception {
        // given
        String url = wiremockServer.getUrl("hello");

        // when
        HttpResponse response = new TestHttpClient().get(url);

        // then
        assertThat(response.getBody())
                .as("Wrong response body")
                .contains("Hello, world!");
    }

    @Test
    public void helloWorldFromFile() throws Exception {
        // given
        String url = wiremockServer.getUrl("/hello-from-file");

        // when
        HttpResponse response = new TestHttpClient().get(url);

        // then
        assertThat(response.getBody())
                .as("Wrong response body")
                .contains("Hello, world!");
    }

    @Test
    public void customPortsAreExposed() {
        //given

        //when

        //then
        assertThat(wiremockServer.getExposedPorts())
                .contains(WIREMOCK_DEFAULT_PORT, ADDITIONAL_MAPPED_PORT);
    }
}
