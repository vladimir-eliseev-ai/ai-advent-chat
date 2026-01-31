plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk:0.8.3")
    implementation(platform("io.ktor:ktor-bom:3.2.3"))
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-sse")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jsoup:jsoup:1.18.1")
    implementation("org.slf4j:slf4j-simple:2.0.17")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

application {
    mainClass.set("eliseev.aiadvent.chat.mcp.articlereader.ArticleReaderMcpServerKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) }
}
