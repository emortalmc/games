plugins {
    id("java-library")
}

group = "dev.emortal"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.lettuce:lettuce-core:7.6.0.RELEASE")
    implementation("com.alibaba.fastjson2:fastjson2:2.0.61")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}