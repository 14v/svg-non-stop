plugins {
    kotlin("jvm") version "1.3.61"
    application
}

group = ""
version = "1.0"

application {
    mainClassName = "NonStopKt"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}