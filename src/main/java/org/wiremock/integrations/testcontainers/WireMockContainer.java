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
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.builder.Transferable;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provisions WireMock standalone server as a container.
 * Designed to follow the WireMock Docker image ({@code wiremock/wiremock}) structure and configuration,
 * but other images can be included too at your own risk.
 */
public class WireMockContainer extends GenericContainer<WireMockContainer> {

    public static final String OFFICIAL_IMAGE_NAME = "wiremock/wiremock";
    private static final String WIREMOCK_2_LATEST_TAG = "2.35.0-1";
    private static final String WIREMOCK_3_LATEST_TAG = "3.0.0-1";
    /*package*/ static final String WIREMOCK_2_MINIMUM_SUPPORTED_VERSION = "2.0.0";

    /**
     * @deprecated Not really guaranteed to be latest. Will be reworked
     */
    @Deprecated
    public static final DockerImageName WIREMOCK_2_LATEST =
            DockerImageName.parse(OFFICIAL_IMAGE_NAME).withTag(WIREMOCK_2_LATEST_TAG);

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
    private final Map<String, WireMockPlugin> plugins = new HashMap<>();
    private boolean isBannerDisabled = true;

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
        setWaitStrategy(DEFAULT_WAITER);
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
     * Add mapping JSON file from its value
     * @param json JSON sting
     * @return This instance
     */
    public WireMockContainer withMappingFromJSON(String json) {
        return withMappingFromJSON(Integer.toString(json.hashCode()), json);
    }

    /**
     * Adds a JSON mapping stub to WireMock configuration
     * @param name Name of the mapping stub
     * @param json Configuration JSON
     * @return this instance
     */
    public WireMockContainer withMappingFromJSON(String name, String json) {
        mappingStubs.put(name, new Stub(name, json));
        // TODO: Prevent duplication
        return this;
    }

    /**
     * @deprecated use {@link #withMappingFromJSON(String, String)}
     */
    @Deprecated
    public WireMockContainer withMapping(String name, String json) {
        return withMappingFromJSON(name, json);
    }

    /**
     * Loads mapping stub from the class resource
     * @param name Name of the mapping stub
     * @param resource Resource class. Name of the class will be appended to the resource path
     * @param resourceJson Reference to the mapping definition file, starting from the {@code resource} root
     *                     (normally package)
     * @return this instance
     */
    public WireMockContainer withMappingFromResource(String name, Class<?> resource, String resourceJson) {
        final URL url = Resources.getResource(resource, resourceJson);
        return withMappingFromResource(name, url);
    }

    /**
     * Loads mapping stub from the class resource
     * @param resource Resource class. Name of the class will be appended to the resource path
     * @param resourceJson Mapping definition file
     * @return this instance
     */
    public WireMockContainer withMappingFromResource(Class<?> resource, String resourceJson) {
        final String id = resource.getName() + "_" + resourceJson;
        return withMappingFromResource(id, resource.getSimpleName() + "/" + resourceJson);
    }

    /**
     * @deprecated use {@link #withMappingFromResource(String, Class, String)}.
     *                  Note that the new method scopes to the package, not to class
     */
    @Deprecated
    public WireMockContainer withMapping(String name, Class<?> resource, String resourceJson) {
        return withMappingFromResource(name, resource, resource.getSimpleName() + "/" + resourceJson);
    }

    /**
     * Loads mapping stub from the resource file
     * @param name Name of the mapping stub
     * @param resourceName Resource name and path
     * @return this instance
     */
    public WireMockContainer withMappingFromResource(String name, String resourceName) {
        final URL url = Resources.getResource(resourceName);
        return withMappingFromResource(name, url);
    }



    /**
     * Loads mapping stub from the resource file
     * @param resourceName Resource name and path
     * @return this instance
     */
    public WireMockContainer withMappingFromResource(String resourceName) {
        String id = resourceName.replace('/', '_');
        return withMappingFromResource(id, resourceName);
    }

    /**
     * Loads mapping stub from the resource file
     * @param name Name of the mapping stub
     * @param url Resource file URL
     * @return this instance
     */
    public WireMockContainer withMappingFromResource(String name, URL url) {
        try {
            String text = Resources.toString(url, StandardCharsets.UTF_8);
            return withMapping(name, text);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Adds file
     * @param name ID to be used
     * @param file File to add
     * @return This instance
     */
    public WireMockContainer withFile(String name, File file) {
        mappingFiles.put(name, MountableFile.forHostPath(file.getPath()));
        // TODO: Prevent duplication
        return this;
    }

    /**
     * Adds file
     * @param file File to add
     * @return This instance
     */
    public WireMockContainer withFile(File file) {
        mappingFiles.put(file.getName(), MountableFile.forHostPath(file.getPath()));
        // TODO: Prevent duplication
        return this;
    }

    public WireMockContainer withFileFromResource(String name, String classpathResource) {
        mappingFiles.put(name, MountableFile.forClasspathResource(classpathResource));
        // TODO: Prevent duplication
        return this;
    }

    public WireMockContainer withFileFromResource(String classpathResource) {
        String id = classpathResource.replace('/', '_');
        // TODO: Prevent duplication
        return withFileFromResource(id, classpathResource);
    }

    public WireMockContainer withFileFromResource(String name, Class<?> resource, String filename) {
        return withFileFromResource(name, resource.getName().replace('.', '/') + "/" + filename);
    }

    public WireMockContainer withFileFromResource(Class<?> resource, String filename) {
        String id = resource.getSimpleName() + "_" + filename;
        return withFileFromResource(id, resource, filename);
    }

    /**
     * Add extension that will be loaded from the specified JAR files.
     * In the internal engine, it will be handled as a single plugin.
     * @param classNames Class names of the extension to be included
     * @param jars JARs to be included into the container
     * @return this instance
     */
    public WireMockContainer withExtensions(Collection<String> classNames, Collection<File> jars) {
        return withExtensions(WireMockPlugin.guessPluginId(classNames, jars), classNames, jars);
    }

    /**
     * Add extension that will be loaded from the specified JAR files.
     * In the internal engine, it will be handled as a single plugin.
     * @param id Identifier top use
     * @param classNames Class names of the extension to be included
     * @param jars JARs to be included into the container
     * @return this instance
     */
    public WireMockContainer withExtensions(String id, Collection<String> classNames, Collection<File> jars) {
        final WireMockPlugin extension = new WireMockPlugin(id)
                .withExtensions(classNames)
                .withJars(jars);
        return withPlugin(extension);
    }

    /**
     * Add extension that will be loaded from the specified directory with JAR files.
     * In the internal engine, it will be handled as a single plugin.
     * @param classNames Class names of the extension to be included
     * @param jarDirectory Directory that stores all JARs
     * @return this instance
     */
    public WireMockContainer withExtensions(Collection<String> classNames, File jarDirectory) {
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

        return withExtensions(classNames, jarsInTheDirectory);
    }

    /**
     * Add extension that will be loaded from the classpath.
     * This method can be used if the extension is a part of the WireMock bundle,
     * or a Jar is already added via {@link #withExtensions(Collection, Collection)}}.
     * In the internal engine, it will be handled as a single plugin.
     * @param className Class name of the extension
     * @return this instance
     */
    public WireMockContainer withExtension(String className) {
        return withExtensions(Collections.singleton(className), Collections.emptyList());
    }

    private WireMockContainer withPlugin(WireMockPlugin plugin) {
        String pluginId = plugin.getPluginId();
        if (plugins.containsKey(pluginId)) {
            throw new IllegalArgumentException("The plugin is already included: " + pluginId);
        }
        plugins.put(pluginId, plugin);
        return this;
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
        for (Map.Entry<String, WireMockPlugin> entry : plugins.entrySet()) {
            final WireMockPlugin ext = entry.getValue();
            extensionClassNames.addAll(ext.getExtensionClassNames());
            for (File jar : ext.getJars()) {
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


}
