package org.wiremock.integrations.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class WireMockContainerEmptyMapppingTest {

    @Container
    WireMockContainer wiremockServer = new WireMockContainer(WireMockContainer.WIREMOCK_2_LATEST)
            .withIgnoreEmptyMappings(true);

    @Test
    public void shouldStartupWhenEmptyIsIgnored() {
        // noop, all logic is inside the container instance
    }

}
