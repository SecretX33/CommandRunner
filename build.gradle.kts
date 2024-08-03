import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("jvm") version "2.0.0"
    application
}

group = "com.github.secretx33"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val javaVersion = JvmTarget.JVM_21

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.8.1"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("net.sf.jopt-simple:jopt-simple:5.0.4")
    implementation("ch.qos.logback:logback-classic:1.4.4")
    implementation("org.codehaus.janino:janino:3.1.8")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.1")
    implementation("io.github.azagniotov:ant-style-path-matcher:1.0.0")
    testImplementation(kotlin("test-junit5"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar { enabled = false }

artifacts.archives(tasks.shadowJar)

tasks.shadowJar {
    archiveFileName.set("${rootProject.name}.jar")
}

tasks.withType<JavaCompile> {
    options.apply {
        release = javaVersion.target.toInt()
        encoding = "UTF-8"
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all")
        jvmTarget = javaVersion
    }
}

application {
    mainClass.set("${project.name}BootstrapKt")
}
