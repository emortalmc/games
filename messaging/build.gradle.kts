plugins {
    id("java-library")
}

group = "dev.emortal"
version = "1.0-SNAPSHOT"

dependencies {
    implementation("io.lettuce:lettuce-core:7.6.0.RELEASE")
    implementation("com.alibaba.fastjson2:fastjson2:2.0.61")

    compileOnly("org.graalvm.nativeimage:svm:25.0.3")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.test {
    useJUnitPlatform()
}