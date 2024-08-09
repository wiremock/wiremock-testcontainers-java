package org.wiremock.integrations.testcontainers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

public class WireMockContainerUnitTest {

    @Test
    public void shouldInitWithDefault() {
        WireMockContainer container = new WireMockContainer(TestConfig.WIREMOCK_DEFAULT_IMAGE);
    }

    @Test
    public void shouldInitWithHigherCompatibleVersion() {
        WireMockContainer container = new WireMockContainer(
                new DockerImageName(WireMockContainer.OFFICIAL_IMAGE_NAME, "2.100.0")
        );
    }

    @Test
    public void shouldFailForOlderImage() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WireMockContainer container = new WireMockContainer(
                    new DockerImageName(WireMockContainer.OFFICIAL_IMAGE_NAME, "1.239.0"));
        });
        assertThat(ex.getMessage())
                .as("Wrong exception message")
                .contains("For the official image, the WireMock version must be >= " + WireMockContainer.WIREMOCK_2_MINIMUM_SUPPORTED_VERSION);
    }

    @Test
    public void shouldInitWithVersionedTestImagesWithSubstitution() {
        // TODO: Should it be accepted by default
        WireMockContainer container = new WireMockContainer(
                new DockerImageName(WireMockContainer.WIREMOCK_2_LATEST.getUnversionedPart(),
                        WireMockContainer.WIREMOCK_2_LATEST.getVersionPart()+ "-test")
                        .asCompatibleSubstituteFor(WireMockContainer.WIREMOCK_2_LATEST));
    }

    @Test
    public void shouldInitWithVersionSubstitution() {
        WireMockContainer container = new WireMockContainer(
                new DockerImageName(WireMockContainer.OFFICIAL_IMAGE_NAME, "test")
                        .asCompatibleSubstituteFor(WireMockContainer.WIREMOCK_2_LATEST));
    }

    @Test
    @Disabled("Requires https://github.com/testcontainers/testcontainers-java/issues/7305")
    public void shouldFailForUnversionedImage() {
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, () -> {
            WireMockContainer container = new WireMockContainer(
                    new DockerImageName(WireMockContainer.OFFICIAL_IMAGE_NAME, "test"));
        });
        assertThat(ex.getMessage())
                .as("Wrong exception message")
                .contains("Failed to verify that image")
                .contains("is a compatible substitute for '" + WireMockContainer.OFFICIAL_IMAGE_NAME + "'");
    }

    @Test
    public void shouldFailCustomImageWithoutSubstitution() {
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, () -> {
            WireMockContainer container = new WireMockContainer(
                    new DockerImageName("mycorp/mywiremockimage", WireMockContainer.WIREMOCK_2_LATEST.getVersionPart()));
        });
        assertThat(ex.getMessage())
                .as("Wrong exception message")
                .contains("Failed to verify that image")
                .contains("is a compatible substitute for '" + WireMockContainer.OFFICIAL_IMAGE_NAME + "'");
    }

}
