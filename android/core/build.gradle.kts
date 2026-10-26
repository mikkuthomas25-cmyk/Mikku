import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin/JVM: SMS parsing, categories, summaries and the on-device learner.
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
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    // Desktop edition of the PDF library the app uses (same API): builds real statement PDFs in tests.
    testImplementation("org.apache.pdfbox:pdfbox:2.0.37")
}
