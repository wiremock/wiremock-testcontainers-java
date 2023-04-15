package org.wiremock.integrations.testcontainers;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.shaded.com.github.dockerjava.core.util.FilePathUtil;
import org.testcontainers.shaded.com.google.common.io.Resources;
import org.testcontainers.shaded.org.apache.commons.io.filefilter.WildcardFileFilter;
import org.testcontainers.utility.MountableFile;

/**
 * Provisions WireMock standalone server as a container.
 */
public class WireMockContainer extends GenericContainer<WireMockContainer> {
    private static final String DEFAULT_IMAGE_NAME = "wiremock/wiremock";
    private static final String DEFAULT_TAG = "latest";

    private static final String SNIPPETS_DIR = "/home/wiremock/mappings/";

    private static final int PORT = 8080;

    private final StringBuilder wireMockArgs;

    private final Map<String, Stub> stubs = new HashMap<>();

    public WireMockContainer() {
        this(DEFAULT_TAG);
    }

    public WireMockContainer(String version) {
        this(DEFAULT_IMAGE_NAME, version);
    }

    public WireMockContainer(String image,String version) {
        super(image + ":" + version);
        wireMockArgs = new StringBuilder();
    }

    public WireMockContainer withStub(String name, String json) {
        stubs.put(name, new Stub(name, json));
        // TODO: Prevent duplication
        return this;
    }

    public WireMockContainer withStubResource(String name, Class<?> resource, String resourceFile) {
        try {
            URL url = Resources.getResource(resource, resource.getSimpleName() + "/" + resourceFile);
            String text = Resources.toString(url, StandardCharsets.UTF_8);
            return withStub(name, text);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public String getEndpoint() {
        return String.format("http://%s:%d", getHost(), getMappedPort(PORT));
    }

    public URI getRequestURI(String relativePath) throws URISyntaxException {
        return new URI(getEndpoint() + "/" + relativePath);
    }

    public Integer getServerPort() {
        return getMappedPort(PORT);
    }

    @Override
    protected void configure() {
        super.configure();
        withExposedPorts(PORT);
        withCommand(wireMockArgs.toString());
        for (Stub stub : stubs.values()) {
            withCopyToContainer(Transferable.of(stub.json), SNIPPETS_DIR + stub.name + ".json");
        }
    }

    private static final class Stub {
        final String name;
        final String json;

        public Stub (String name, String json) {
            this.name = name;
            this.json = json;
        }
    }

}
