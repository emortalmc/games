plugins {
    java
    application
    id("com.gradleup.shadow") version "9.5.1"
//    id("org.graalvm.buildtools.native") version "0.11.0"
}

group = "dev.emortal.minestom"
version = "1.0-SNAPSHOT"
application.mainClass = "dev.emortal.minestom.marathon.Main"

repositories {
    mavenCentral()

    maven("https://repo.emortal.dev/snapshots")
    maven("https://repo.emortal.dev/releases")

    maven("https://jitpack.io")
    maven("https://packages.confluent.io/maven/")
}

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

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.isDeprecation = true
    }

    withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    build {
        dependsOn(shadowJar)
    }
}

//graalvmNative {
//    binaries {
//        named("main") {
//            imageName.set("marathon")
//            mainClass.set(application.mainClass)
//
////            buildArgs.add("-march=native")
//            quickBuild.set(true)
//            buildArgs.add("--enable-url-protocols=https")
//            buildArgs.add("--gc=G1")
//
//            verbose.set(true)
//            fallback.set(false)
//        }
//
//        all {
//            resources.autodetect()
//        }
//    }
//}