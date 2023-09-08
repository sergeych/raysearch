package net.sergeych

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml
import net.sergeych.mp_logger.*
import net.sergeych.tools.PlatformType
import net.sergeych.tools.detectedPlatform
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.*


@Serializable
data class ConfigData(
    val fileLogLevel: Log.Level = Log.Level.DEBUG
) : Loggable by LogTag("CONF") {

    private val versionProps by lazy {
        Properties().also {
            it.load(this.javaClass.getResourceAsStream("/version.properties"))
        }
    }

    val version by lazy {
        versionProps.getProperty("version") ?: "no version"
    }

    fun openFile(file: Path) {
        val cmd =
            when(detectedPlatform) {
                PlatformType.Linux -> arrayOf("gio", "open", file.pathString)
                PlatformType.Windows -> arrayOf("explorer.exe", file.pathString)
                else -> arrayOf("open", file.pathString)
            }

        info { "opening file with command ${cmd.toList()}" }
        try {
            Runtime.getRuntime().exec(cmd)
            info { "open ok" }
        } catch (t: Throwable) {
            exception { "failed to execute '${cmd.toList()}`" to t }
        }
    }

    fun openFolder(file: Path) {
        val cmd =
            when(detectedPlatform) {
                PlatformType.Linux -> arrayOf("gio", "open", file.parent.pathString)
                PlatformType.Windows -> arrayOf("explorer.exe", file.parent.pathString)
                else -> arrayOf("open", file.pathString)
            }
        info { "opening folder with command ${cmd.toList()}" }
        try {
            Runtime.getRuntime().exec(cmd)
            info { "open ok" }
        } catch (t: Throwable) {
            exception { "failed to execute '${cmd.toList()}'" to t }
        }
    }
}

val appHomePath: Path by lazy {
    Paths.get(System.getProperty("user.home") + "/.raysearch")
        .also { it.createDirectories() }
}

val Config: ConfigData by lazy {
    val file: Path = appHomePath.resolve("raysearch.yml")
    if (file.exists() && file.isReadable())
        Yaml.decodeFromString(file.readText())
    else
        ConfigData().also {
            file.writeText(Yaml.encodeToString(it) + "\n")
        }
}
