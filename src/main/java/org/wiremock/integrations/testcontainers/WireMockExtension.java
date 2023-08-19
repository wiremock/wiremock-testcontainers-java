package org.wiremock.integrations.testcontainers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Basic class for WireMock extension management.
 * It supports both WireMock 2 extensions that require explicit classnames to be passed,
 * and WireMock 3 extensions with auto-discovery.
 * In both cases, the assumption is that an extension may span multiple JAR files,
 * or may have no JAR files at all if the extension comes from classpath (e.g. custom WireMock bundle).
 */
/*package*/ final class WireMockExtension {

    final String id;
    final List<File> jars = new ArrayList<>();
    Optional<String> extensionClassName = Optional.empty();

    public WireMockExtension(String id) {
        this.id = id;
    }

    public WireMockExtension withJarFile(File jarFile) {
        return withJarFiles(Collections.singleton(jarFile));
    }

    public WireMockExtension withJarFiles(Collection<File> jarFiles) {
        jars.addAll(jarFiles);
        return this;
    }

    public WireMockExtension withClassName(String className) {
        extensionClassName = Optional.ofNullable(className);
        return this;
    }

    public String getExtensionClassName() {
        return extensionClassName.orElse(null);
    }

    /**
     * Check whether class name is defined.
     * @return {@code true} if no extension specified
     */
    public boolean isClassNameDefined() {
        return extensionClassName.isPresent();
    }
}
