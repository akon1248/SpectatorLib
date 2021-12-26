import kr.entree.spigradle.kotlin.spigot
import kr.entree.spigradle.attribute.Load
import kr.entree.spigradle.kotlin.protocolLib
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.5.10"
	id("com.github.johnrengelman.shadow") version "6.1.0"
	id("kr.entree.spigradle") version "1.2.4"
}

group = "com.akon"
version = "1.0"

repositories {
	mavenCentral()
	maven(url = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
	protocolLib()
}

dependencies {
	compileOnly(spigot("1.16.5"))
	compileOnly("org.jetbrains", "annotations", "22.0.0")
	compileOnly(protocolLib())
}

spigot {
	authors = listOf("akon")
	depends = listOf("ProtocolLib")
	apiVersion = "1.16"
	load = Load.STARTUP
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
