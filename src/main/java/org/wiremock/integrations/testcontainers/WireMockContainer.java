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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.shaded.com.google.common.io.Resources;
import org.testcontainers.utility.MountableFile;

/**
 * Provisions WireMock standalone server as a container.
 * Designed to follow the WireMock Docker image ({@code wiremock/wiremock}) structure and configuration,
 * but other images can be included too at your own risk.
 */
public class WireMockContainer extends GenericContainer<WireMockContainer> {
    private static final String DEFAULT_IMAGE_NAME = "wiremock/wiremock";
    private static final String DEFAULT_TAG = "latest";

    private static final String MAPPINGS_DIR = "/home/wiremock/mappings/";
    private static final String FILES_DIR = "/home/wiremock/__files/";

    private static final String EXTENSIONS_DIR = "/var/wiremock/extensions/";

    private static final WaitStrategy DEFAULT_WAITER = Wait
            .forHttp("/__admin/mappings")
            .withMethod("GET")
            .forStatusCode(200);

    private static final int PORT = 8080;

    private final StringBuilder wireMockArgs;

    private final Map<String, Stub> mappingStubs = new HashMap<>();
    private final Map<String, MountableFile> mappingFiles = new HashMap<>();
    private final Map<String, Extension> extensions = new HashMap<>();

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

    /**
     * Add extension that will be loaded from the specified JAR file.
     * @param id Unique ID of the extension, for logging purposes
     * @param classNames Class names of the extension to be included
     * @param jars JARs to be included into the container
     * @return this instance
     */
    public WireMockContainer withExtension(String id, Collection<String> classNames, Collection<File> jars) {
        final Extension extension = new Extension(id);
        extension.extensionClassNames.addAll(classNames);
        extension.jars.addAll(jars);
        extensions.put(id, extension);
        return this;
    }

    /**
     * Add extension that will be loaded from the specified directory with JAR files.
     * @param id Unique ID of the extension, for logging purposes
     * @param classNames Class names of the extension to be included
     * @param jarDirectory Directory that stores all JARs
     * @return this instance
     */
    public WireMockContainer withExtension(String id, Collection<String> classNames, File jarDirectory) {
        final List<File> jarsInTheDirectory;
        try (Stream<Path> walk = Files.walk(jarDirectory.toPath())) {
            jarsInTheDirectory = walk
                    .filter(p -> !Files.isDirectory(p))
                    .map(Path::toFile)
                    .filter(f -> f.toString().endsWith(".jar"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot list JARs in the directory " + jarDirectory, e);
        }

        return withExtension(id, classNames, jarsInTheDirectory);
    }

    /**
     * Add extension that will be loaded from the classpath.
     * This method can be used if the extension is a part of the WireMock bundle,
     * or a Jar is already added via {@link #withExtension(String, Collection, Collection)}}
     * @param id Unique ID of the extension, for logging purposes
     * @param className Class name of the extension
     * @return this instance
     */
    public WireMockContainer withExtension(String id, String className) {
        return withExtension(id, Collections.singleton(className), Collections.emptyList());
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
        waitingFor(DEFAULT_WAITER);
        for (Stub stub : mappingStubs.values()) {
            withCopyToContainer(Transferable.of(stub.json), MAPPINGS_DIR + stub.name + ".json");
        }

        for (Map.Entry<String, MountableFile> mount : mappingFiles.entrySet()) {
            withCopyToContainer(mount.getValue(), FILES_DIR + mount.getKey());
        }

        final ArrayList<String> extensionClassNames = new ArrayList<>();
        for (Map.Entry<String, Extension> entry : extensions.entrySet()) {
            final Extension ext = entry.getValue();
            extensionClassNames.addAll(ext.extensionClassNames);
            for (File jar : ext.jars) {
                withCopyToContainer(MountableFile.forHostPath(jar.toPath()), EXTENSIONS_DIR + jar.getName());
            }
        }
        if (!extensionClassNames.isEmpty()) {
            wireMockArgs.append(" --extensions ");
            int counter = 0;
            for (String className : extensionClassNames) {
                wireMockArgs.append(className);
                wireMockArgs.append( (++counter != extensionClassNames.size()) ? ',' : ' ');
            }
        }

        // Add CLI arguments
        withCommand(wireMockArgs.toString());
    }

    private static final class Stub {
        final String name;
        final String json;

        public Stub (String name, String json) {
            this.name = name;
            this.json = json;
        }
    }

    private static final class Extension {
        final String id;
        final List<File> jars = new ArrayList<>();
        final List<String> extensionClassNames = new ArrayList<>();

        public Extension(String id) {
            this.id = id;
        }
    }

}
