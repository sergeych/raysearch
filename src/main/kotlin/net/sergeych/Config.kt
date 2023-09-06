package net.sergeych

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml
import net.sergeych.mp_logger.*
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*


@Serializable
data class ConfigData(
    val fileLogLevel: Log.Level = Log.Level.DEBUG
) : Loggable by LogTag("CONF") {

    fun openFile(file: Path) {
        val cmd = arrayOf("gio", "open", file.pathString)
        info { "opening file with command $cmd" }
        try {
            Runtime.getRuntime().exec(cmd)
            info { "open ok" }
        } catch (t: Throwable) {
            exception { "failed to execute '$cmd`" to t }
        }
    }

    fun openFolder(file: Path) {
        val cmd = arrayOf("gio", "open", file.parent.pathString)
        info { "opening folder with command $cmd" }
        try {
            Runtime.getRuntime().exec(cmd)
            info { "open ok" }
        } catch (t: Throwable) {
            exception { "failed to execute '$cmd`" to t }
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
