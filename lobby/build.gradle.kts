plugins {
    java
    application
    id("com.gradleup.shadow") version "9.5.1"
    id("org.graalvm.buildtools.native")
}

group = "dev.emortal.minestom"
version = "1.0-SNAPSHOT"
application.mainClass = "dev.emortal.minestom.lobby.Main"

repositories {
    mavenLocal()
}

dependencies {
    implementation(project(":core"))

    implementation("com.alibaba.fastjson2:fastjson2:2.0.61")
    implementation("dev.emortal:bbstom:local")
    implementation("org.joml:joml:1.10.8")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(25))

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

graalvmNative {
    binaries.named("main") {
        imageName = "server"
        mainClass = application.mainClass
    }
}