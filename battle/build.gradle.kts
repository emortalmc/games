plugins {
    java
    id("com.gradleup.shadow") version "9.5.1"
}

group = "dev.emortal.minestom.battle"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven("https://jitpack.io")
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    implementation(project(":core"))

    implementation("com.github.vibenilla:pvp:133ae66") {
        exclude(group = "net.minestom", module = "minestom")
    }
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
                "Main-Class" to "dev.emortal.minestom.battle.Main",
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
}
