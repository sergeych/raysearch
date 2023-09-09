import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.util.*

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
}

group = "net.sergeych"
version = "1.1.1"

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
    implementation("net.sergeych:kotyara:1.4.3")
    implementation("net.sergeych:mp_stools:1.4.1-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("br.com.devsrsouza.compose.icons:feather:1.1.0")
    implementation("org.apache.lucene:lucene-core:9.7.0")
    implementation("org.odftoolkit:odfdom-java:0.11.0")
    implementation("org.apache.pdfbox:pdfbox:2.0.29")

    implementation("org.apache.tika:tika-core:2.9.0")
    implementation("org.apache.tika:tika-parsers-standard-package:2.9.0")

//    implementation("net.sergeych:mp_stools:1.4.1-SNAPSHOT")
    implementation("net.sergeych:mp_bintools:0.0.3-SNAPSHOT")

    testImplementation(kotlin("test"))
}

tasks.named<KotlinCompilationTask<*>>("compileKotlin").configure {
//    compilerOptions.freeCompilerArgs.add("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
//    compilerOptions.freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
    compilerOptions.freeCompilerArgs.add("-opt-in= kotlinx.coroutines.DelicateCoroutinesApi.class \n")
}

compose.desktop {
    application {
        mainClass = "EightRaysSearchKt"
        buildTypes.release.proguard {
            obfuscate.set(false)
            isEnabled.set(false)
        }
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "raysearch"
//            val iconsRoot = project.file("src/main/resources/launcher_icons")
//            modules("java.sql","java.instrument", "java.sql", "jdk.unsupported",
//                "java.xml.crypto", "jdk.xml.dom",
//                "jdk.zipfs")
            includeAllModules = true
            description = "File content indexing (text) and fast search"
            copyright = "© 2023 Sergey S. Chernov. All rights reserved."
            vendor = "real.sergeych@gmail.com"

            linux {
                menuGroup = "tools"
                appCategory = "search   "
                iconFile.set(project.file("src/main/resources/launcher_icons/raysearch.png"))
            }
            windows {
                menuGroup = "tools"
                upgradeUuid = "59e3dbfd-31b7-4ee1-995c-e7687760e400"
                iconFile.set(project.file("/src/main/resources/launcher_icons/raysearch.ico"))
            }
//            licenseFile.set(project.file("LICENSE.txt"))
        }
    }
}

tasks.withType<ProcessResources>() {
    doLast {
        val propertiesFile = file("$buildDir/resources/main/version.properties")
        propertiesFile.parentFile.mkdirs()
        val properties = Properties()
        properties.setProperty("version", rootProject.version.toString())
        propertiesFile.writer().use { properties.store(it, null) }
    }
}
