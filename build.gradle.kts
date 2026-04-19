plugins {
    id("java")
    id("maven-publish")
}

configurations.matching { it.isCanBeResolved }.configureEach {
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
    }
}

// Version of SkriptRegistration
val projectVersion = "1.0.1"

java.sourceCompatibility = JavaVersion.VERSION_25

repositories {
    mavenCentral()
    mavenLocal()

    // Paper
    maven("https://repo.papermc.io/repository/maven-public/")

    // Skript
    maven("https://repo.skriptlang.org/releases")
}

dependencies {
    // Paper
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")

    // Skript
    compileOnly("com.github.SkriptLang:Skript:2.15.0")

    // commons-io
    compileOnly("commons-io:commons-io:2.14.0")
    compileOnly("org.apache.commons:commons-text:1.10.0")
}

tasks {
    jar {
        archiveBaseName.set("SkriptRegistration-$projectVersion")
    }
    java {

        withJavadocJar()
        withSourcesJar()
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.shanebeee"
            artifactId = "SkriptRegistration"
            version = projectVersion

            artifact(tasks["jar"])

            // This adds the sources jar
            artifact(tasks["sourcesJar"])
        }
    }
}

