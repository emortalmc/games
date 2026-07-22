plugins {
    java
    id("com.gradleup.shadow") version "9.5.1"
}

group = "dev.emortal.minestom.blocksumo"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":core"))
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
            attributes (
                "Main-Class" to "dev.emortal.minestom.blocksumo.Main",
                "Multi-Release" to true
            )
        }
    }

    build {
        dependsOn(shadowJar)
    }
}
