plugins {
    `java-library`
    `maven-publish`
    signing
}

description = "This Testcontainers module allows provisioning the WireMock server as a standalone container within your unit tests, based on WireMock Docker"
group = "org.wiremock.integrations.testcontainers"
version = "1.0-alpha-12-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}

val testcontainersVersion = "1.20.6"
val junitVersion = "5.12.1"
val assertjVersion = "3.26.3"
val awaitilityVersion = "4.2.2"
var logbackClassicVersion = "1.4.12"

repositories {
    mavenCentral()
    maven {
        name = "9c-releases"
        url = uri("https://raw.github.com/9cookies/mvn-repo/master/releases/")
    }
}

val testWiremockExtension by configurations.creating

dependencies {
    api(platform("org.testcontainers:testcontainers-bom:$testcontainersVersion"))
    api("org.testcontainers:testcontainers")
    compileOnly("ch.qos.logback:logback-classic:${logbackClassicVersion}")

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.vintage:junit-vintage-engine")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("org.awaitility:awaitility:$awaitilityVersion")

    testWiremockExtension("com.ninecookies.wiremock.extensions:wiremock-extensions:0.4.1:jar-with-dependencies@jar")
    testWiremockExtension("org.wiremock:wiremock-webhooks-extension:2.35.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Copy>("copyTestWiremockExtensions") {
    from(testWiremockExtension.resolve())
    into(layout.projectDirectory.dir("target").dir("test-wiremock-extension"))
    // into(layout.buildDirectory.dir("test-wiremock-extension")) // TODO use this after complete migration to Gradle
}

tasks.named("compileTestJava") {
    dependsOn("copyTestWiremockExtensions")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name = "WireMock module for Testcontainers Java"
                description = project.description
                url = "https://github.com/wiremock/wiremock-testcontainers-java"

                licenses {
                    license {
                        name = "Apache License Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }

                developers {
                    developer {
                        id = "oleg-nenashev"
                        name = "Oleg Nenashev"
                        url = "https://github.com/oleg-nenashev/"
                        organization = "WireMock Inc."
                        organizationUrl = "https://www.wiremock.io/"
                    }
                }

                scm {
                    connection = "scm:git:git://github.com/wiremock/wiremock-testcontainers-java.git"
                    developerConnection = "scm:git:https://github.com/wiremock/wiremock-testcontainers-java.git"
                    url = "https://github.com/wiremock/wiremock-testcontainers-java"
                }
            }
        }
    }

    repositories {
        maven {
            name = "MavenCentral"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")

            credentials {
                username = project.findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
                password = project.findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
            }

        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/wiremock/wiremock-testcontainers-java")
            credentials {
                username = project.findProperty("githubUsername") as String? ?: System.getenv("GITHUB_USERNAME")
                password = project.findProperty("githubToken") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(
        project.findProperty("signing.key") as String? ?: System.getenv("OSSRH_GPG_SECRET_KEY"),
        project.findProperty("signing.password") as String? ?: System.getenv("OSSRH_GPG_SECRET_KEY_PASSWORD")
    )
    sign(publishing.publications["mavenJava"])
}