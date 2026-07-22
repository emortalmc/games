plugins {
    java
    id("com.gradleup.shadow") version "9.5.1"
}

group = "dev.emortal"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":core"))
    implementation("com.github.luben:zstd-jni:1.5.7-4")
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
                "Main-Class" to "dev.emortal.minestom.minesweeper.Main",
                "Multi-Release" to true
            )
        }
    }

    build {
        dependsOn(shadowJar)
    }
}
