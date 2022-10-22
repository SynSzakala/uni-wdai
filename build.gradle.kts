plugins {
    kotlin("jvm") version "1.7.20" apply false
    id("org.springframework.boot") version "2.7.5" apply false
    id("io.spring.dependency-management") version "1.0.15.RELEASE" apply false
    kotlin("plugin.spring") version "1.6.21" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}