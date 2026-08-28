plugins {
    java
    id("com.gradleup.shadow") version "9.5.1"
}

group = "dev.emortal.minestom.battle"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://maven.skylite.gg/releases")
}

dependencies {
    implementation(project(":core"))

    implementation("rocks.minestom:pvp:2026.08.28-26.2") {
        exclude(group = "net.minestom", module = "minestom")
    }
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
                "Main-Class" to "dev.emortal.minestom.battle.Main",
                "Multi-Release" to true
            )
        }
    }

    build {
        dependsOn(shadowJar)
    }
}
