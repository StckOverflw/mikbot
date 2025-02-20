plugins {
    `mikbot-plugin`
    `mikbot-module`
    kotlin("plugin.serialization")
}

group = "dev.schlaubi.mikbot"
version = "2.9.1"

repositories {
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    plugin(projects.utils.ktor)
    optionalPlugin(projects.core.gdpr)
    implementation(libs.github.repositories) {
        exclude("org.slf4j", "slf4j-api")
    }
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.sessions) {
        exclude("io.ktor", "ktor-server-core")
    }
    implementation(libs.ktor.server.auth) {
        exclude("io.ktor", "ktor-server-core")
    }
}

mikbotPlugin {
    description.set("User profiles.")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + listOf("-Xopt-in=io.ktor.locations.KtorExperimentalLocationsAPI")
        }
    }
}
