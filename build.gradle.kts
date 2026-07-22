plugins {
    id("java")
    id("org.graalvm.buildtools.native") version "0.11.1" apply false
}

group = "dev.emortal"
version = "1.0-SNAPSHOT"

allprojects {
    apply(plugin = "java")

    tasks {
        withType<AbstractArchiveTask> {
            isPreserveFileTimestamps = false
            isReproducibleFileOrder = true
        }
        withType<JavaCompile> {
            options.isDeprecation = true
            options.encoding = "UTF-8"
        }
    }

    repositories {
        mavenCentral()
        maven("https://repo.hypera.dev/snapshots/") // spark-minestom
        maven("https://repo.lucko.me/") // spark-common
        maven("https://oss.sonatype.org/content/repositories/snapshots/") // spark-common's dependencies
    }

    dependencies {
        // Logger
        implementation("ch.qos.logback:logback-classic:1.5.18")
        implementation("net.logstash.logback:logstash-logback-encoder:8.1")

        compileOnly("org.jetbrains:annotations:26.1.0")
    }
}

subprojects {
    plugins.withId("org.graalvm.buildtools.native") {
        extensions.configure<org.graalvm.buildtools.gradle.dsl.GraalVMExtension>("graalvmNative") {
            binaries.named("main") {
                javaLauncher = javaToolchains.launcherFor {
                    languageVersion = JavaLanguageVersion.of(25)
                    nativeImageCapable = true
                }

                buildArgs.add("--enable-url-protocols=http,https")
                buildArgs.add("--initialize-at-build-time=com.alibaba.fastjson2")
                buildArgs.add("--enable-native-access=ALL-UNNAMED")
                buildArgs.add("-H:+UnlockExperimentalVMOptions")
                buildArgs.add("-H:+ReportExceptionStackTraces")
                buildArgs.add("-H:+SharedArenaSupport")

                quickBuild = providers.gradleProperty("nativeQuickBuild").isPresent
                fallback = false
            }
        }
    }
}

tasks.register<Copy>("collectLibs") {
    dependsOn(subprojects.map { it.tasks.named("build") })

    subprojects.forEach { subproject ->
        from(subproject.layout.buildDirectory.dir("libs")) {
            include("*-all.jar")
            exclude("*-sources.jar")
            rename({ a -> a.replace("-1.0-SNAPSHOT-all", "") })
        }
    }

    into(layout.buildDirectory.dir("libs"))
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.named("build") {
    dependsOn("collectLibs")
}

tasks.jar { enabled = false }

