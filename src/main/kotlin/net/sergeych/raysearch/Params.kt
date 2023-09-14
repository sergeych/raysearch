package net.sergeych.raysearch

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

@Serializable
data class ParamsData(
    val dataSchemeVersion: Int = 0,
    val backgroundModeWarningShown: Boolean = false,
    val runInBackground: Boolean = true
) {
}

private val file: Path = appHomePath.resolve("raysearch_params.json")

val _paramsState = MutableStateFlow<ParamsData>(
    if (file.exists() && file.isReadable())
        Json.decodeFromString(file.readText())
    else
        ParamsData().also {
            it.save()
        }
)

val paramsStateFlow = _paramsState.asStateFlow()

fun ParamsData.save() {
    file.writeText(Json.encodeToString(this))
    // initialization issue -- it COULD happen and it HAPPENS on fresh new
    // installation:
    @Suppress("SENSELESS_COMPARISON")
    if( _paramsState != null )
        _paramsState.value = this
}

val Params: ParamsData get() = _paramsState.value

