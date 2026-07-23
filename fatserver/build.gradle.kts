plugins {
    id("java")
    id("com.gradleup.shadow") version "9.5.1"
    id("org.graalvm.buildtools.native")
    application
}

group = "dev.emortal"
version = "1.0-SNAPSHOT"
application.mainClass = "dev.emortal.fatserver.Main"

repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":marathon"))
    implementation(project(":parkourtag"))
    implementation(project(":battle"))
    implementation(project(":blocksumo"))
    implementation(project(":lazertag"))
    implementation(project(":minesweeper"))
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

tasks.register<Copy>("stageMaps") {
    listOf("blocksumo", "battle", "parkourtag").forEach {
        from(project(":$it").layout.projectDirectory.dir("run/maps"))
    }
    into(layout.projectDirectory.dir("run/maps"))
}

tasks.named("nativeCompile") {
    dependsOn("stageMaps")
}