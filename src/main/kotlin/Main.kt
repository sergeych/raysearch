import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import net.sergeych.mp_logger.Log
import net.sergeych.raysearch.Scanner
import net.sergeych.views.App

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    val root = System.getProperties().getProperty("user.home")// + "/dev/raysearch"

    Log.connectConsole(Log.Level.INFO)

    Scanner.setup(listOf(root))

    Window(onCloseRequest = ::exitApplication, title = "8 Rays search", onKeyEvent = {
        if (it.isCtrlPressed && it.key == Key.Q)
            System.exit(0)
        false
    },
        icon = painterResource("raysearch.svg")
    ) {
        App()
    }
}

