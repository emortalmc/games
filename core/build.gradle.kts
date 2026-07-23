plugins {
    `java-library`
}

group = "dev.emortal.minestom"
version = "1.0-SNAPSHOT"

dependencies {
    api(project(":messaging"))

    // Minestom
    api("net.minestom:minestom:2026.07.22-26.2")
    api("net.kyori:adventure-text-minimessage:5.2.0")
    compileOnlyApi("it.unimi.dsi:fastutil:8.5.18")
    api("dev.hollowcube:polar:1.16.0")

    implementation("org.jline:jline:4.3.1")

    api("com.alibaba.fastjson2:fastjson2:2.0.61")

//    implementation("dev.lu15:spark-minestom:1.10-SNAPSHOT")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }

    withSourcesJar()
}

tasks {
    test {
        useJUnitPlatform()
    }
}
