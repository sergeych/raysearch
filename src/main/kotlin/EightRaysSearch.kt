import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.*
import net.sergeych.Config
import net.sergeych.appHomePath
import net.sergeych.mp_logger.FileLogCatcher
import net.sergeych.mp_logger.Log
import net.sergeych.raysearch.Params
import net.sergeych.raysearch.Scanner
import net.sergeych.raysearch.paramsStateFlow
import net.sergeych.tools.PlatformType
import net.sergeych.views.App
import kotlin.io.path.pathString

val detectedPlatform: PlatformType by lazy {
    val p = System.getProperty("os.name").lowercase()
    println("system reports: $p")
    when {
        p == "linux" -> PlatformType.Linux
        p.startsWith("windows") -> PlatformType.Windows
        // I'm not sure about it as there is no reason to run it on mac
        p == "macos" -> PlatformType.Macos
        else -> PlatformType.Unknown
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    println("detected platform is: $detectedPlatform")

    Log.connectConsole(Log.Level.INFO)
    FileLogCatcher(appHomePath.resolve("raysearch.log").pathString, Config.fileLogLevel, true)
    Log.defaultLevel = Log.Level.DEBUG
    Scanner.setup(listOf(System.getProperties().getProperty("user.home")))

    application {

        var isOpen by remember { mutableStateOf(true) }
        val trayState = rememberTrayState()
        var showTray by remember { mutableStateOf(Params.runInBackground) }

        LaunchedEffect(true) {
            paramsStateFlow.collect {
                showTray = it.runInBackground
                println("show tray set to $showTray")
            }
        }

        val windowState = rememberWindowState()

        if (isOpen) {
            Window(
                onCloseRequest = {
                    if (Params.runInBackground)
                        isOpen = false
                    else
                        exitApplication()
                },
                state = windowState,
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

        if (showTray) {
            Tray(
                state = trayState,
                icon = painterResource(
                    if (detectedPlatform.isLinux)
                        "linux_tray_icon.png"
                    else
                        "tray_icon.svg"
                ),
                onAction = {
                    // There is a huge bug in tray icon support
                    // it will call again the action when the main window will
                    // receive focus. Therefore, we shouldn't do here nothing
                    // that should be done once:
                    isOpen = true
                },
            )
        }

    }
}