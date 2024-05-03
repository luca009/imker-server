import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    id("org.springframework.boot") version "3.1.3"
    id("io.spring.dependency-management") version "1.1.3"
    kotlin("jvm") version "1.8.22"
    kotlin("plugin.spring") version "1.8.22"
}

group = "com.luca009.imker"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_18
}

repositories {
    mavenCentral()
    maven {
        url = URI("https://artifacts.unidata.ucar.edu/repository/unidata-all/")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("commons-net:commons-net:3.9.0")
    implementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    implementation("edu.ucar:cdm-core:5.5.3")
    runtimeOnly("edu.ucar:netcdf4:5.5.3")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "18"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
