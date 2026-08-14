plugins {
    java
    id("com.gradleup.shadow") version "9.5.1"
}

group = "dev.emortal.minestom.parkourtag"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":core"))

    // jolt-jni
    runtimeOnly("com.github.oshi:oshi-core:7.4.2")
    implementation("com.github.stephengold:jolt-jni-Linux_ARM64:6.0.2")
    runtimeOnly("com.github.stephengold:jolt-jni-Linux64:6.0.0:ReleaseSp")
    runtimeOnly("com.github.stephengold:jolt-jni-Linux_ARM64:6.0.2:ReleaseSp")
    implementation("io.github.electrostat-lab:snaploader:1.1.1-stable")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
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

    build {
        dependsOn(shadowJar)
    }
}
