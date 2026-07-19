plugins {
    id("java")
}

group = "dev.emortal"
version = "1.0-SNAPSHOT"

allprojects {
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

    apply(plugin = "java")

    repositories {
        mavenCentral()
    }

    dependencies {
        // Logger
        implementation("ch.qos.logback:logback-classic:1.5.18")
        implementation("net.logstash.logback:logstash-logback-encoder:8.1")

        compileOnly("org.jetbrains:annotations:26.1.0")
    }
}

tasks.register<Copy>("collectLibs") {
    dependsOn(subprojects.map { it.tasks.named("build") })

    subprojects.forEach { subproject ->
        from(subproject.layout.buildDirectory.dir("libs")) {
            include("*-all.jar")
            exclude("*-sources.jar")
        }
    }

    into(layout.buildDirectory.dir("libs"))
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.named("build") {
    dependsOn("collectLibs")
}

