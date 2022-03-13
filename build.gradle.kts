plugins {
    kotlin("jvm") version "1.6.10"
    application
    `java-library`
    `maven-publish`
}

group = "com.github.14v"
version = "1.1.2"

application {
    mainClass.set("NonStopKt")
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

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("svg-non-stop")
                url.set("https://github.com/14v/svg-non-stop")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/14v/svg-non-stop/blob/master/LICENSE")
                    }
                }
            }
        }
    }
}
