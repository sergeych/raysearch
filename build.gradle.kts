import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
}

group = "net.sergeych"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://maven.universablockchain.com")
    mavenLocal()
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation("net.mamoe.yamlkt:yamlkt:0.13.0")
    implementation("com.h2database:h2:2.2.220")
    implementation("net.sergeych:kotyara:1.4.2-SNAPSHOT")
    implementation("net.sergeych:mp_stools:1.4.1-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("br.com.devsrsouza.compose.icons:feather:1.1.0")
    implementation("org.apache.lucene:lucene-core:9.7.0")
    implementation("org.apache.lucene:lucene-queries:9.7.0")

    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "raysearch"
            packageVersion = "1.0.0"
        }
    }
}

tasks.named<KotlinCompilationTask<*>>("compileKotlin").configure {
//    compilerOptions.freeCompilerArgs.add("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
//    compilerOptions.freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
    compilerOptions.freeCompilerArgs.add("-opt-in= kotlinx.coroutines.DelicateCoroutinesApi.class \n")
}
