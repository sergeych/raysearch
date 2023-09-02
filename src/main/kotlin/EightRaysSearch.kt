import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import net.sergeych.Config
import net.sergeych.appHomePath
import net.sergeych.mp_logger.FileLogCatcher
import net.sergeych.mp_logger.Log
import net.sergeych.raysearch.Scanner
import net.sergeych.views.App
import kotlin.io.path.pathString

//private val log = LogTag("MAIN")

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    Log.connectConsole(Log.Level.INFO)
    FileLogCatcher(appHomePath.resolve("raysearch.log").pathString, Config.fileLogLevel,true)
    Log.defaultLevel = Log.Level.DEBUG

    Scanner.setup(listOf(System.getProperties().getProperty("user.home")))

    Window(
        onCloseRequest = ::exitApplication, title = "8 Rays search", onKeyEvent = {
            if (it.isCtrlPressed && it.key == Key.Q)
                System.exit(0)
            false
        },
        icon = painterResource("raysearch.svg")
    ) {
        App()
    }
}

