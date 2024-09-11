plugins {
	kotlin("jvm") version "2.0.0"
	id("org.openjfx.javafxplugin") version "0.1.0"
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
	mavenLocal()
}
kotlin.jvmToolchain(21)
javafx {
	version = "22.0.1"
	modules = listOf(
		"javafx.controls",
		"javafx.graphics",
	)
}

dependencies {
	implementation("moe.nea:neurepoparser:1.3.2")
	implementation("com.google.code.gson:gson:2.10.1")
	implementation("no.tornado:tornadofx:1.7.20")
	implementation("de.jensd:fontawesomefx:8.2")
	testImplementation(kotlin("test"))
}
application {
	mainClass.set("moe.nea.cittofirm.CitToFirm")
}
tasks.test {
	useJUnitPlatform()
}