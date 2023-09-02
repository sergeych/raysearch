package net.sergeych

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml
import net.sergeych.mp_logger.LogTag
import net.sergeych.mp_logger.Loggable
import net.sergeych.mp_logger.exception
import net.sergeych.mp_logger.info
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*


@Serializable
data class ConfigData(
    val openFolderCommand: String = "gio open {path}",
    val openFileCommand: String = "gio open {path}/{file_name}"
): Loggable by LogTag("CONF") {
    fun openFile(file: Path) {
        val cmd = openFileCommand.interpolatePath(file)
        info { "opening file with command $cmd" }
        try {
            Runtime.getRuntime().exec(cmd)
            info { "open ok" }
        }
        catch(t: Throwable) {
            exception { "failed to execute '$cmd`" to t }
        }
    }
    fun openFolder(file: Path) {
        val cmd = openFolderCommand.interpolatePath(file)
        info { "opening folder with command $cmd" }
        try {
            Runtime.getRuntime().exec(cmd)
            info { "open ok" }
        }
        catch(t: Throwable) {
            exception { "failed to execute '$cmd`" to t }
        }
    }
}

fun String.interpolatePath(path: Path): String {
    return replace("{path}", path.parent.toString())
        .replace("{file_name}", path.fileName.toString())
}

val appHomePath: Path by lazy {
    Paths.get(System.getProperty("user.home") + "/.rayscan")
        .also { it.createDirectories() }
}

val Config: ConfigData by lazy {
    val file: Path = appHomePath.resolve("rayscan.yaml")
    if (file.exists() && file.isReadable())
        Yaml.decodeFromString(file.readText())
    else
        ConfigData().also {
            file.writeText(Yaml.encodeToString(it))
        }
}
