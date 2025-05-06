/*
 * Copyright (C) 2025 WireMock Inc, Oleg Nenashev and all project contributors
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

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(parallel = true)
class WireMockContainerRootDirTest {
    @Container
    WireMockContainer wiremockServer = new WireMockContainer(TestConfig.WIREMOCK_DEFAULT_IMAGE)
            .withMapping("hello", WireMockContainerRootDirTest.class, "hello.json")
            .withFileFromResource("file.json", WireMockContainerRootDirTest.class, "file.json")
            .withRootDir(new File("src/test/resources/root-dir"));

    @Test
    void testThatLoadsMappingFromRootDirectory() throws Exception {
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
    void testThatLoadsMappingFromNestedRootDirectory() throws Exception {
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
    void testThatServesFileFromRootDirectory() throws Exception {
        // given
        String url = wiremockServer.getUrl("root-dir-file.json");

        // when
        HttpResponse response = new TestHttpClient().get(url);

        // then
        assertThat(response.getBody())
                .as("response body")
                .isEqualTo("{ \"message\": \"file from root dir\" }");
    }

    @Test
    void testThatServesNestedFileFromRootDirectory() throws Exception {
        // given
        String url = wiremockServer.getUrl("nested/root-dir-nested-file.json");

        // when
        HttpResponse response = new TestHttpClient().get(url);

        // then
        assertThat(response.getBody())
                .as("response body")
                .isEqualTo("{ \"message\": \"nested file from root dir\" }");
    }

    @Test
    void testThatDirectMappingsAndFilesAreLoadedWithCustomRootDirEnabled() throws Exception {
        // given
        String url = wiremockServer.getUrl("/hello");

        // when
        HttpResponse response = new TestHttpClient().get(url);

        // then
        assertThat(response.getBody())
                .as("Wrong response body")
                .contains("file contents from direct mapping");
    }

    @Test
    void testThatDirectMappingsAndFilesAreLoadedEvenWhenRootDirIsSpecified() throws Exception {
        // given
        String url = wiremockServer.getUrl("/hello");

        // when
        HttpResponse response = new TestHttpClient().get(url);

        // then
        assertThat(response.getBody())
                .as("Wrong response body")
                .contains("file contents from direct mapping");
    }

    @Test
    void testThatMappingsAndFileAreLoadedFromDefaultRootDir() throws Exception {

        try (WireMockContainer wmc = new WireMockContainer(TestConfig.WIREMOCK_DEFAULT_IMAGE)) {
            wmc.start();

            // given
            String url = wmc.getUrl("/hello/default/root/dir");

            // when
            HttpResponse response = new TestHttpClient().get(url);

            // then
            assertThat(response.getBody())
                    .as("Wrong response body")
                    .contains("contents from default root dir file");
        }
    }

    @Test
    void testThatInvalidRootDirIsIgnored() throws Exception {

        try (WireMockContainer wmc = new WireMockContainer(TestConfig.WIREMOCK_DEFAULT_IMAGE)
                .withMapping("hello", WireMockContainerRootDirTest.class, "hello.json")
                .withFileFromResource("file.json", WireMockContainerRootDirTest.class, "file.json")
                .withRootDir(new File("invalid/root/dir"))) {
            wmc.start();

            // given
            String url = wmc.getUrl("/hello");

            // when
            HttpResponse response = new TestHttpClient().get(url);

            // then
            assertThat(response.getBody())
                    .as("Wrong response body")
                    .contains("file contents from direct mapping");
        }
    }

    @Test
    void testThatNullRootDirIsIgnored() throws Exception {

        try (WireMockContainer wmc = new WireMockContainer(TestConfig.WIREMOCK_DEFAULT_IMAGE)
                .withMapping("hello", WireMockContainerRootDirTest.class, "hello.json")
                .withFileFromResource("file.json", WireMockContainerRootDirTest.class, "file.json")
                .withRootDir(null)) {
            wmc.start();

            // given
            String url = wmc.getUrl("/hello");

            // when
            HttpResponse response = new TestHttpClient().get(url);

            // then
            assertThat(response.getBody())
                    .as("Wrong response body")
                    .contains("file contents from direct mapping");
        }
    }

}
