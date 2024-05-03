import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties
import java.net.URI

plugins {
    id("org.springframework.boot") version "3.1.3"
    id("io.spring.dependency-management") version "1.1.3"
    kotlin("jvm") version "1.8.22"
    kotlin("plugin.spring") version "1.8.22"
}

group = "com.luca009.imker"
version = "0.1-alpha1"

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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("commons-net:commons-net:3.9.0")
    implementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    implementation("edu.ucar:cdm-core:5.5.4-SNAPSHOT")
    runtimeOnly("edu.ucar:netcdf4:5.5.4-SNAPSHOT")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "18"
    }

    dependsOn("createProperties")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

fun getCheckedOutGitCommitHash(): String {
    val gitFolder = "$projectDir/.git/"
    val takeFromHash = 12
    /*
     * '.git/HEAD' contains either
     *      in case of detached head: the currently checked out commit hash
     *      otherwise: a reference to a file containing the current commit hash
     */
    val head = File(gitFolder, "HEAD").readText().split(":") // .git/HEAD
    val isCommit = head.count() == 1

    if (isCommit) {
        return head[0].trim().take(takeFromHash)
    }

    val refHead = File(gitFolder, head[1].trim()) // .git/refs/heads/master
    return refHead.readText().trim().take(takeFromHash)
}

task("createProperties") {
    doLast {
        with(File("$buildDir/resources/main/version.properties").writer()) {
            val p = Properties()
            p["version.versionString"] = project.version.toString()
            p["version.gitLastTag"] = getCheckedOutGitCommitHash()
            p.store(this, null)
        }
    }
}