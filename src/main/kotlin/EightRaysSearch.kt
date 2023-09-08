import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import net.sergeych.Config
import net.sergeych.appHomePath
import net.sergeych.mp_logger.FileLogCatcher
import net.sergeych.mp_logger.Log
import net.sergeych.raysearch.Scanner
import net.sergeych.views.App
import kotlin.io.path.pathString

//private val log = LogTag("MAIN")

enum class PlatformType {
    Linux, Windows, Macos, Unknown;

    val isLinux: Boolean get() = this == Linux
}

val detectedPlatform: PlatformType by lazy {
    when (System.getProperty("os.name").lowercase()) {
        "linux" -> PlatformType.Linux
        "windows" -> PlatformType.Windows
        // I'm not sure about it as there is no reason to run it on mac
        "macos" -> PlatformType.Macos
        else -> PlatformType.Unknown
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    application {
        var isOpen by remember { mutableStateOf(true) }

        Log.connectConsole(Log.Level.INFO)
        FileLogCatcher(appHomePath.resolve("raysearch.log").pathString, Config.fileLogLevel, true)
        Log.defaultLevel = Log.Level.DEBUG


        val trayState = rememberTrayState()

        Tray(
            state = trayState,
            icon = painterResource(
                if (detectedPlatform.isLinux)
                    "linux_tray_icon.png"
                else
                    "tray_icon.svg"
            ),
            onAction = {
                       isOpen = true
            },
            menu = {
                Item("Open",
                    onClick = {
                        isOpen = true
                    })
                Separator()
                Item(
                    "Exit raysearch",
                    onClick = {
                        exitApplication()
                    }
                )
            }
        )
        // we don't want to start it if it failed tray initialization

        Scanner.setup(listOf(System.getProperties().getProperty("user.home")))

        if (isOpen) {
            Window(
                onCloseRequest = {
                    isOpen = false
                },
                title = "8 Rays search",
                onKeyEvent = {
                    if (it.isCtrlPressed && it.key == Key.Q)
                        isOpen = false
                    false
                },
                icon = painterResource("raysearch.svg")
            ) {
                App()
            }
        }
    }
}