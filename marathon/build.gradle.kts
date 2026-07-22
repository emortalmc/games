plugins {
    java
    application
    id("com.gradleup.shadow") version "9.5.1"
}

group = "dev.emortal.minestom"
version = "1.0-SNAPSHOT"
application.mainClass = "dev.emortal.minestom.marathon.Main"

dependencies {
    implementation(project(":core"))
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.9")
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
                "Multi-Release" to true
            )
        }
    }

    build {
        dependsOn(shadowJar)
    }
}