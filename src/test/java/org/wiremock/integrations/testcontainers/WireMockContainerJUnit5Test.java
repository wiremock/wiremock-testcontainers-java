package org.wiremock.integrations.testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.wiremock.integrations.testcontainers.testsupport.http.TestHttpClient;

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
}
