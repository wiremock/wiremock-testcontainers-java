package org.wiremock.integrations.testcontainers;

import org.testcontainers.utility.DockerImageName;

public class TestConfig {

    private static final String DEFAULT_TEST_TAG =
            System.getProperty("wiremock.testcontainer.defaultTag", "3.1.0-1");
    private static final String WIREMOCK_2_TEST_TAG =
            System.getProperty("wiremock.testcontainer.wiremock2Tag", "2.35.1-1");

    public static final DockerImageName WIREMOCK_DEFAULT_IMAGE =
            DockerImageName.parse(WireMockContainer.OFFICIAL_IMAGE_NAME).withTag(DEFAULT_TEST_TAG);

    public static final DockerImageName WIREMOCK_2_IMAGE =
            DockerImageName.parse(WireMockContainer.OFFICIAL_IMAGE_NAME).withTag(WIREMOCK_2_TEST_TAG);
}
