import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay
import net.sergeych.mp_logger.Log
import net.sergeych.mp_tools.globalLaunch
import net.sergeych.raysearch.SearchFolder
import net.sergeych.views.App
import java.nio.file.Paths

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    Log.connectConsole(Log.Level.INFO)
    val root = System.getProperties().getProperty("user.home") + "/dev"

    val sf = SearchFolder.get(null, Paths.get(root))

    globalLaunch {
//        for( i in 1..20)
        delay(800)
        sf.rescan()
    }

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
