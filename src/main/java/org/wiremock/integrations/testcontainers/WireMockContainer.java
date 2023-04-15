/*
 * Copyright (C) 2023 WireMock Inc, Oleg Nenashev and all project contributors
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.shaded.com.google.common.io.Resources;
import org.testcontainers.utility.MountableFile;

/**
 * Provisions WireMock standalone server as a container.
 */
public class WireMockContainer extends GenericContainer<WireMockContainer> {
    private static final String DEFAULT_IMAGE_NAME = "wiremock/wiremock";
    private static final String DEFAULT_TAG = "latest";

    private static final String MAPPINGS_DIR = "/home/wiremock/mappings/";
    private static final String FILES_DIR = "/home/wiremock/__files/";

    private static final int PORT = 8080;

    private final StringBuilder wireMockArgs;

    private final Map<String, Stub> mappingStubs = new HashMap<>();
    private final Map<String, MountableFile> mappingFiles = new HashMap<>();

    public WireMockContainer() {
        this(DEFAULT_TAG);
    }

    public WireMockContainer(String version) {
        this(DEFAULT_IMAGE_NAME, version);
    }

    public WireMockContainer(String image, String version) {
        super(image + ":" + version);
        wireMockArgs = new StringBuilder();
    }

    /**
     * Adds CLI argument to the WireMock call.
     * @param arg Argument
     * @return this instance
     */
    public WireMockContainer withCliArg(String arg) {
        //TODO: Switch to framework with proper CLI escaping
        wireMockArgs.append(' ').append(arg);
        return this;
    }

    /**
     * Adds a JSON mapping stub to WireMock configuration
     * @param name Name of the mapping stub
     * @param json Configuration JSON
     * @return this instance
     */
    public WireMockContainer withMapping(String name, String json) {
        mappingStubs.put(name, new Stub(name, json));
        // TODO: Prevent duplication
        return this;
    }

    /**
     * Loads mapping stub from the class resource
     * @param name Name of the mapping stub
     * @param resource Resource class. Name of the class will be appended to the resource path
     * @param resourceJson Mapping definition file
     * @return this instance
     */
    public WireMockContainer withMapping(String name, Class<?> resource, String resourceJson) {
        try {
            URL url = Resources.getResource(resource, resource.getSimpleName() + "/" + resourceJson);
            String text = Resources.toString(url, StandardCharsets.UTF_8);
            return withMapping(name, text);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public WireMockContainer withFile(String name, File file) {
        mappingFiles.put(name, MountableFile.forHostPath(file.getPath()));
        // TODO: Prevent duplication
        return this;
    }

    public WireMockContainer withFileFromResource(String name, String classpathResource) {
        mappingFiles.put(name, MountableFile.forClasspathResource(classpathResource));
        // TODO: Prevent duplication
        return this;
    }

    public WireMockContainer withFileFromResource(String name, Class<?> resource, String filename) {
        return withFileFromResource(name, resource.getName().replace('.', '/') + "/" + filename);
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
        for (Stub stub : mappingStubs.values()) {
            withCopyToContainer(Transferable.of(stub.json), MAPPINGS_DIR + stub.name + ".json");
        }

        for (Map.Entry<String, MountableFile> mount : mappingFiles.entrySet()) {
            withCopyToContainer(mount.getValue(), FILES_DIR + mount.getKey());
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
