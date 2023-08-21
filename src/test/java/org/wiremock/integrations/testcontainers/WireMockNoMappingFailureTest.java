package org.wiremock.integrations.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class WireMockNoMappingFailureTest {

    // We deliberately pass
    @Container
    WireMockContainer wiremockServer = new WireMockContainer(WireMockContainer.WIREMOCK_2_LATEST);

    @Test
    void shouldFailOnStartup() {
        // No op
    }
}
