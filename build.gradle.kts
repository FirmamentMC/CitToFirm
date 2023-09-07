plugins {
    kotlin("jvm") version "1.9.0"
    application
}

group = "moe.nea"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.nea.moe/releases")
    maven("https://maven.notenoughupdates.org/releases")
    maven("https://jitpack.io/") {
        content {
            includeGroupByRegex("(com|io)\\.github\\..+")
        }
    }
}

dependencies {
    implementation("moe.nea:neurepoparser:1.3.2")
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation(kotlin("test"))
}
application {
    mainClass.set("moe.nea.cittofirm.CitToFirm")
}
tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}