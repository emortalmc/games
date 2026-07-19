plugins {
    java
    id("com.gradleup.shadow") version "9.5.1"
}

group = "dev.emortal.minestom"
version = "1.0-SNAPSHOT"

repositories {
    maven(url = "https://central.sonatype.com/repository/maven-snapshots/") {
        content {
            includeModule("net.minestom", "minestom")
        }
    }
    mavenLocal()
    mavenCentral()

    maven("https://maven.draylar.dev/releases")

    maven("https://jitpack.io")
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    implementation(project(":core"))

    implementation("com.alibaba.fastjson2:fastjson2:2.0.61")
    implementation("dev.emortal:bbstom:local")
    implementation("org.joml:joml:1.10.9")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(26))

tasks {
    shadowJar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()

        manifest {
            attributes(
                "Main-Class" to "dev.emortal.minestom.lobby.Entrypoint",
                "Multi-Release" to true
            )
        }
    }

    withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    build {
        dependsOn(shadowJar)
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.isDeprecation = true
    }
}
