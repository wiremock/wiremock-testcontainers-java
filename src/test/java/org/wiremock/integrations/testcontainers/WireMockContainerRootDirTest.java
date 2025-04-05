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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.wiremock.integrations.testcontainers.testsupport.http.HttpResponse;
import org.wiremock.integrations.testcontainers.testsupport.http.TestHttpClient;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(parallel = true)
class WireMockContainerRootDirTest {

    @Container
    WireMockContainer wiremockServer = new WireMockContainer(TestConfig.WIREMOCK_DEFAULT_IMAGE);

    @Test
    void testThatLoadMappingFromDefaultRootDirectory() throws Exception {
        // given
        String url = wiremockServer.getUrl("hello/root/dir");

        // when
        HttpResponse response = new TestHttpClient().get(url);

        // then
        assertThat(response.getBody())
                .as("response body")
                .contains("Hello root dir");
    }

    @Test
    void testThatLoadMappingFromNestedDefaultRootDirectory() throws Exception {
        // given
        String url = wiremockServer.getUrl("hello/nested/root/dir");

        // when
        HttpResponse response = new TestHttpClient().get(url);

        // then
        assertThat(response.getBody())
                .as("response body")
                .contains("Hello nested root dir");
    }

    @Test
    void testThatServesFileFromDefaultRootDirectory() throws Exception {
        // given
        String url = wiremockServer.getUrl("default-root-dir-file.json");

        // when
        HttpResponse response = new TestHttpClient().get(url);

        // then
        assertThat(response.getBody())
                .as("response body")
                .isEqualTo("{ \"message\": \"file from default root dir\" }");
    }

    @Test
    void testThatServesNestedFileFromDefaultRootDirectory() throws Exception {
        // given
        String url = wiremockServer.getUrl("nested/default-root-dir-nested-file.json");

        // when
        HttpResponse response = new TestHttpClient().get(url);

        // then
        assertThat(response.getBody())
                .as("response body")
                .isEqualTo("{ \"message\": \"nested file from default root dir\" }");
    }

}
