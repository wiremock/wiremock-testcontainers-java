package org.wiremock.integrations.testcontainers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.wiremock.integrations.testcontainers.testsupport.http.TestHttpClient;

import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Testcontainers
public class WireMockContainerJUnit5Test {

    @Container
    public WireMockContainer wiremockServer = new WireMockContainer("2.35.0")
            .withMapping("hello", WireMockContainerTest.class, "hello-world.json")
            .withMapping("hello-resource", WireMockContainerTest.class, "hello-world-resource.json")
            .withFileFromResource("hello-world-resource-response.xml", WireMockContainerTest.class,
                    "hello-world-resource-response.xml");


    @ParameterizedTest
    @ValueSource(strings = {
            "hello",
            "/hello"
    })
    public void helloWorld(String path) throws Exception {
        // given
        String url = wiremockServer.getUrl(path);

        // when
        HttpResponse<String> response = TestHttpClient.newInstance().get(url);

        // then
        assertThat(response.body())
                .as("Wrong response body")
                .contains("Hello, world!");
    }

    @Test
    public void helloWorldFromFile() throws Exception {
        // given
        String url = wiremockServer.getUrl("/hello-from-file");

        // when
        HttpResponse<String> response = TestHttpClient.newInstance().get(url);

        // then
        assertThat(response.body())
                .as("Wrong response body")
                .contains("Hello, world!");
    }

    @Test
    public void defaultBannerDisabled() {
        WireMockContainer wireMockContainerSpy = Mockito.spy(new WireMockContainer("2.35.0"));

        wireMockContainerSpy.configure();

        verify(wireMockContainerSpy).withCliArg("--disable-banner");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "--verbose",
            " --verbose ",
            "--verbose --help"
    })
    public void showBannerWhenStartingVerbose(String verboseArg) {
        WireMockContainer wireMockContainerSpy = Mockito.spy(new WireMockContainer("2.35.0"));
        wireMockContainerSpy.withCliArg(verboseArg);

        wireMockContainerSpy.configure();

        verify(wireMockContainerSpy, times(0)).withCliArg("--disable-banner");
    }
}
