package org.wiremock.integrations.testcontainers;

import org.testcontainers.shaded.com.google.common.io.Files;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Unofficial notion of a WirteMock plugin.
 * WireMock at the moment operates only on the extension level,
 * and here we try to introduce a concept of a plugin that may span multiple JARs and extensions.
 * {@link #extensionClassNames} may be empty for WireMock 3 that supports auto-loading
 */
 /*package*/ class WireMockPlugin {
    private final String pluginId;
    private final List<File> jars = new ArrayList<>();
    private final List<String> extensionClassNames = new ArrayList<>();

    public WireMockPlugin(String id) {
        this.pluginId = id;
    }

    public String getPluginId() {
        return pluginId;
    }

    public WireMockPlugin withJars(Collection<File> jars) {
        this.jars.addAll(jars);
        return this;
    }

    public WireMockPlugin withJar(File jar) {
        return withJars(Collections.singleton(jar));
    }

    public WireMockPlugin withExtensions(Collection<String> extensionClassNames) {
        this.extensionClassNames.addAll(extensionClassNames);
        return this;
    }

    public WireMockPlugin withExtension(String className) {
        return withExtensions(Collections.singleton(className));
    }

    /**
     * Get JARs associated with the extension
     * @return List of JARs. Might be empty if the plugin/extension is a part of the WireMock core or already in the classpath
     */
    public List<File> getJars() {
        return jars;
    }

    /**
     * Get the list of extensions. Might be empty in WireMock 3
     * @return List of extension class names within the plugin
     */
    public List<String> getExtensionClassNames() {
        return extensionClassNames;
    }

    public static String guessPluginId(Collection<String> classNames, Collection<File> jars) {
        File jar = jars.stream().findFirst().orElse(null);
        if (jar != null) {
            return Files.getNameWithoutExtension(jar.getName());
        }

        String className = classNames.stream().findFirst().orElse(null);
        if (className != null && className.length() > 1) {
            return className.substring(className.lastIndexOf('.') + 1);
        }

        return "plugin_" + Math.random(); // Double is fun, right? :)
    }
}
