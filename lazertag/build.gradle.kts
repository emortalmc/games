plugins {
    id("java")
    id("com.gradleup.shadow") version "9.5.1"
}

group = "dev.emortal.minestom"
version = "1.0-SNAPSHOT"


dependencies {
    implementation(project(":core"))

    implementation("org.joml:joml:1.10.9")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(26))

tasks {
    shadowJar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()

        manifest {
            attributes (
                "Main-Class" to "dev.emortal.minestom.lazertag.Main",
                "Multi-Release" to true
            )
        }
    }

    build {
        dependsOn(shadowJar)
    }
}
