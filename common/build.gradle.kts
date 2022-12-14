import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

group = "uni.wdai"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_14

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("io.projectreactor.kotlin:reactor-kotlin-extensions")
    api("org.jetbrains.kotlin:kotlin-reflect")
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("org.springframework.boot:spring-boot-starter-webflux")
    api("org.springdoc:springdoc-openapi-webflux-ui:1.6.13")
    api("org.springdoc:springdoc-openapi-kotlin:1.6.13")

    api(platform("software.amazon.awssdk:bom:2.18.16"))
    api("software.amazon.awssdk:s3")
    api("software.amazon.awssdk:sqs")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
    api("com.fasterxml.jackson.core:jackson-databind:2.14.0")
    api("io.jsonwebtoken:jjwt-api:0.11.5")
    api("io.jsonwebtoken:jjwt-impl:0.11.5")
    api("io.jsonwebtoken:jjwt-jackson:0.11.5")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "14"
    }
}
