plugins {
    java
    id("com.gradleup.shadow") version "9.5.1"
}

group = "dev.emortal.minestom.battle"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://jitpack.io")
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

    build {
        dependsOn(shadowJar)
    }
}
