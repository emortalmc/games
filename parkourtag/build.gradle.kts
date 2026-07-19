plugins {
    java
    id("com.gradleup.shadow") version "9.5.1"
}

group = "dev.emortal.minestom.parkourtag"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven("https://jitpack.io")
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    implementation(project(":core"))

    // jolt-jni
    runtimeOnly("com.github.oshi:oshi-core:7.3.2")
    implementation("com.github.stephengold:jolt-jni-Linux_ARM64:4.3.0")
    runtimeOnly("com.github.stephengold:jolt-jni-Linux64:4.3.0:ReleaseSp")
    runtimeOnly("com.github.stephengold:jolt-jni-Linux_ARM64:4.3.0:ReleaseSp")
    implementation("io.github.electrostat-lab:snaploader:1.1.1-stable")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(26))
    }
}

tasks {
    shadowJar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()

        manifest {
            attributes(
                "Main-Class" to "dev.emortal.minestom.parkourtag.Main",
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
