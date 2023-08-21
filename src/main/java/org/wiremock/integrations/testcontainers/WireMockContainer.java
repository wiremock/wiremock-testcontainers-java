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

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.google.common.io.Resources;
import org.testcontainers.utility.ComparableVersion;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provisions WireMock standalone server as a container.
 * Designed to follow the WireMock Docker image ({@code wiremock/wiremock}) structure and configuration,
 * but other images can be included too at your own risk.
 */
public class WireMockContainer extends GenericContainer<WireMockContainer> {

    public static final String OFFICIAL_IMAGE_NAME = "wiremock/wiremock";
    private static final String WIREMOCK_2_LATEST_TAG = "2.35.0";
    /*package*/ static final String WIREMOCK_2_MINIMUM_SUPPORTED_VERSION = "2.0.0";

    public static final DockerImageName WIREMOCK_2_LATEST =
            DockerImageName.parse(OFFICIAL_IMAGE_NAME).withTag(WIREMOCK_2_LATEST_TAG);

    private static final String MAPPINGS_DIR = "/home/wiremock/mappings/";
    private static final String FILES_DIR = "/home/wiremock/__files/";

    private static final String EXTENSIONS_DIR = "/var/wiremock/extensions/";
    private static final int PORT = 8080;
    private final StringBuilder wireMockArgs;
    private final Map<String, Stub> mappingStubs = new HashMap<>();
    private final Map<String, MountableFile> mappingFiles = new HashMap<>();
    private final Map<String, Extension> extensions = new HashMap<>();
    private boolean isBannerDisabled = true;
    private boolean ignoreEmptyMappings = false;

    /**
     * Create image from the specified full image name (repo, image, tag)
     */
    public WireMockContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public WireMockContainer(DockerImageName dockerImage) {
        super(dockerImage);
        dockerImage.assertCompatibleWith(new DockerImageName(OFFICIAL_IMAGE_NAME));

        // Verify the minimum version for the official image
        final ComparableVersion version = new ComparableVersion(dockerImage.getVersionPart());
        if (!version.isSemanticVersion()) { // Accept only images when compatibility is declared explicitly
            // TODO: We cannot extract compatibleSubstituteFor from Testcontainers API - https://github.com/testcontainers/testcontainers-java/issues/7305
        } else {
            boolean isLessThanBaseVersion = version.isLessThan(WIREMOCK_2_MINIMUM_SUPPORTED_VERSION);
            if (OFFICIAL_IMAGE_NAME.equals(dockerImage.getUnversionedPart()) && isLessThanBaseVersion) {
                throw new IllegalArgumentException("For the official image, the WireMock version must be >= " + WIREMOCK_2_MINIMUM_SUPPORTED_VERSION);
            }
        }

        wireMockArgs = new StringBuilder();
        setWaitStrategy(Wait
                .forHttp("/__admin/mappings")
                .withMethod("GET")
                .forStatusCode(200)
                .forResponsePredicate(new WireMockMappingsResponsePredicate())
        );
    }

    /**
     * Disables the banner when starting the WireMock container.
     * @return this instance
     */
    public WireMockContainer withoutBanner() {
        isBannerDisabled = true;
        return this;
    }

    /**
     * Enable the banner when starting the WireMock container.
     * @return this instance
     */
    public WireMockContainer withBanner() {
        isBannerDisabled = false;
        return this;
    }

    /**
     * Instructs the container to ignore empty mappings.
     * Otherwise the startup will be failed if there are no mappings,
     * and the test will be forced to fail too
     * @param ignore value to set
     * @return this instance
     */
    public WireMockContainer withIgnoreEmptyMappings(boolean ignore) {
       this.ignoreEmptyMappings = ignore;
       return this;
    }

    public boolean isIgnoreEmptyMappings() {
        return ignoreEmptyMappings;
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

    public String getBaseUrl() {
        return String.format("http://%s:%d", getHost(), getPort());
    }

    public String getUrl(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return String.format("%s%s", getBaseUrl(), path);
    }

    public Integer getPort() {
        return getMappedPort(PORT);
    }

    @Override
    protected void configure() {
        super.configure();
        withExposedPorts(PORT);
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
            wireMockArgs.append(String.join(",", extensionClassNames));
        }

        if (isBannerDisabled) {
            this.withCliArg("--disable-banner");
        }

        // Add CLI arguments
        withCommand(wireMockArgs.toString());
    }

    private static final class Stub {
        final String name;
        final String json;

        public Stub(String name, String json) {
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

    /**
     * Verifies that the Test container is started up correctly
     */
    private class WireMockMappingsResponsePredicate implements Predicate<String> {

        @Override
        public boolean test(String s) {
            final int mappingsNumber;
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode obj = mapper.readTree(s);
                if (!obj.hasNonNull("mappings")) {
                    mappingsNumber = 0;
                } else {
                    JsonNode mappings = obj.get("mappings");
                    mappingsNumber = mappings.isArray() ? mappings.size() : 0;
                    if (mappingsNumber == 0) {
                        // The container initialized but failed to load mappings
                        if (!WireMockContainer.this.isIgnoreEmptyMappings()) {
                            throw new IllegalStateException("WireMock container has initialized, but the mappings are empty. " +
                                    "If that's desired state in the test, use WireMockContainer#withIgnoreEmptyMappings()");
                        }
                    }
                }
            } catch (IOException ex) {
                return false;
            }

            return mappingsNumber > 0;
        }
    }

    private static final class WireMockMappings {
        public List<Object> mappings;
    }
}
