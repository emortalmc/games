plugins {
    id("java")
    id("xyz.jpenilla.run-velocity") version "3.0.2"
    id("com.gradleup.shadow") version "9.5.1"
}

group = "dev.emortal"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":messaging"))

    compileOnly("com.velocitypowered:velocity-api:4.1.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:4.1.0-SNAPSHOT")
//    compileOnly("com.velocitypowered:velocity-proxy:4.1.0-SNAPSHOT")
    compileOnly("net.luckperms:api:5.5")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    test {
        useJUnitPlatform()
    }

    runVelocity {
        velocityVersion("4.1.0-SNAPSHOT")

        downloadPlugins {
            modrinth("LuckPerms", "v5.5.53-velocity")
        }
    }

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
