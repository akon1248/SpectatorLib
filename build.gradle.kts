import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import kr.entree.spigradle.data.Load
import kr.entree.spigradle.kotlin.spigot
import kr.entree.spigradle.kotlin.protocolLib
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.9.20"
	id("com.github.johnrengelman.shadow") version "7.1.2"
	id("kr.entree.spigradle") version "2.4.3"
}

group = "com.akon"
version = "1.1"

repositories {
	mavenCentral()
	maven(url = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
	protocolLib()
}

dependencies {
	compileOnly(spigot("1.19.4"))
	compileOnly(protocolLib("5.1.0"))
}

spigot {
	authors = listOf("akon")
	depends = listOf("ProtocolLib")
	apiVersion = "1.19"
	load = Load.STARTUP
	libraries = listOf("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
}

tasks.withType<ShadowJar> {
	archiveVersion.set("")
	archiveClassifier.set("")
}

tasks {
	jar {
		enabled = false
	}
	build {
		dependsOn(shadowJar)
	}
	withType<KotlinCompile> {
		kotlinOptions {
			jvmTarget = "1.8"
		}
	}
}
