import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin/JVM: SMS parsing, categories, summaries and the Claude client.
// No Android dependencies, so it can be unit-tested on any machine.
plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation("com.anthropic:anthropic-java:3.3.0")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}
