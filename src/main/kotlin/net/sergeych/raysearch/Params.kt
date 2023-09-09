package net.sergeych.raysearch

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sergeych.appHomePath
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isReadable
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val file: Path = appHomePath.resolve("raysearch_params.json")

@Serializable
data class ParamsData(
    val dataSchemeVersion: Int = 0,
    val backgroundModeWarningShown: Boolean = false
) {
    fun save() {
        file.writeText(Json.encodeToString(this))
    }
}


val Params: ParamsData by lazy {
    if (file.exists() && file.isReadable())
        Json.decodeFromString(file.readText())
    else
        ParamsData().also { it.save() }
}

