package org.wiremock.integrations.testcontainers;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WireMockContainerUnitTest {

    @Test
    public void bannerIsByDefaultDisabled() {
        WireMockContainer wireMockContainer = new WireMockContainer("2.35.0");
        wireMockContainer.configure();

        String[] startUpArgs = wireMockContainer.getCommandParts();

        assertTrue(Arrays.asList(startUpArgs).contains("--disable-banner"));
    }

    @Test
    public void enableBanner() {
        WireMockContainer wireMockContainerSpy = new WireMockContainer("2.35.0").withBanner();
        wireMockContainerSpy.configure();

        String[] startUpArgs = wireMockContainerSpy.getCommandParts();

        assertFalse(Arrays.asList(startUpArgs).contains("--disable-banner"));
    }
}
