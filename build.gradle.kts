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
val projectVersion = "1.0.7"

java.sourceCompatibility = JavaVersion.VERSION_21

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
    javadoc {
        val options = options as StandardJavadocDocletOptions
        options.docTitle = "SkriptRegistration API - $projectVersion"
        options.overview = "src/main/javadoc/overview.html"
        options.encoding = Charsets.UTF_8.name()
        options.links(
            "https://javadoc.io/doc/org.jetbrains/annotations/latest/",
            "https://jd.papermc.io/paper/1.21.11/",
            "https://docs.skriptlang.org/javadocs/",
            "https://jd.advntr.dev/api/4.25.0/"
        )
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

