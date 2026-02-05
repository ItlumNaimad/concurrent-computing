plugins {
    kotlin("jvm") version "2.2.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.projectreactor:reactor-core:3.6.0")
    
    // Nowoczesny wygląd Swing (FlatLaf)
    implementation("com.formdev:flatlaf:3.3")
    implementation("com.formdev:flatlaf-extras:3.3")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}