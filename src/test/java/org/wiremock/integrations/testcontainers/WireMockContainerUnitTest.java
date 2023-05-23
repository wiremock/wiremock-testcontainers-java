package org.wiremock.integrations.testcontainers;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class WireMockContainerUnitTest {

    @Test
    public void defaultBannerDisabled() {
        WireMockContainer wireMockContainerSpy = Mockito.spy(new WireMockContainer("2.35.0"));

        wireMockContainerSpy.configure();

        verify(wireMockContainerSpy).withCliArg("--disable-banner");
    }

    @Test
    public void enableBanner() {
        WireMockContainer wireMockContainerSpy = Mockito.spy(new WireMockContainer("2.35.0")).withBanner();

        wireMockContainerSpy.configure();

        verify(wireMockContainerSpy, times(0)).withCliArg("--disable-banner");
    }
}
