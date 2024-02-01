plugins {
    `java-library`
    alias(libs.plugins.gradleJavaConventions)
}

repositories { mavenCentral() }

group = "com.opencastsoftware"

description = "A library for running pgTAP tests using Testcontainers Java"

spotless { ratchetFrom("") }

java { toolchain.languageVersion.set(JavaLanguageVersion.of(11)) }

dependencies {
    api(platform(libs.testcontainersBom))
    api(libs.testcontainersPostgres)
    api(libs.junitJupiter)

    implementation(libs.tap4j) { exclude("org.yaml", "snakeyaml") }
}

mavenPublishing {
    coordinates("com.opencastsoftware", "pgtap-java", project.version.toString())

    pom {
        name.set("pgtap-java")
        description.set(project.description)
        url.set("https://github.com/opencastsoftware/pgtap-java")
        inceptionYear.set("2024")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        organization {
            name.set("Opencast Software Europe Ltd")
            url.set("https://opencastsoftware.com")
        }
        developers {
            developer {
                id.set("DavidGregory084")
                name.set("David Gregory")
                organization.set("Opencast Software Europe Ltd")
                organizationUrl.set("https://opencastsoftware.com/")
                timezone.set("Europe/London")
                url.set("https://github.com/DavidGregory084")
            }
        }
        ciManagement {
            system.set("Github Actions")
            url.set("https://github.com/opencastsoftware/pgtap-java/actions")
        }
        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/opencastsoftware/pgtap-java/issues")
        }
        scm {
            connection.set("scm:git:https://github.com/opencastsoftware/pgtap-java.git")
            developerConnection.set("scm:git:git@github.com:opencastsoftware/pgtap-java.git")
            url.set("https://github.com/opencastsoftware/pgtap-java")
        }
    }
}

testing {
    suites {
        val test by
            getting(JvmTestSuite::class) {
                useJUnitJupiter(libs.versions.junit)
                dependencies {
                    implementation(libs.testcontainersJunit)
                    implementation(libs.flywayCore)
                    implementation(libs.logback)
                    runtimeOnly(libs.postgresql)
                }
            }
    }
}

tasks.withType<JavaCompile> {
    // Target Java 11
    options.release.set(11)
}
