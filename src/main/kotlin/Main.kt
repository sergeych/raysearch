import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay
import net.sergeych.kotyara.Database
import net.sergeych.kotyara.db.DbContext
import net.sergeych.mp_logger.Log
import net.sergeych.mp_tools.globalLaunch
import net.sergeych.raysearch.SearchFolder
import net.sergeych.views.InputLine
import net.sergeych.views.ScanProgressBar
import java.nio.file.Files
import java.nio.file.Paths

private object DataHolder

val dataBase by lazy {
    val home = Paths.get(System.getProperty("user.home") + "/.rayscan/db")
    Files.createDirectories(home)
//    Files.delete(home.resolve("rayscan.mv.db"))
    println("--- $home")
    Class.forName("org.h2.Driver")
    Database("jdbc:h2:$home/rayscan", 10, null)
        .also { it.migrateWithResources(DataHolder::class.java) }
}

fun <T> dbs(f: (DbContext) -> T) = dataBase.withContext(f)
suspend fun <T> db(f: suspend (DbContext) -> T) = dataBase.asyncContext(f)
fun <T> inDbs(f: DbContext.() -> T) = dataBase.inContext(f)
suspend fun <T> inDb(f: suspend DbContext.() -> T) = dataBase.asyncContext(f)

inline fun <reified T : Any> DbContext.byIdOrThrow(id: Any): T =
    byId<T>(id) ?: throw IllegalArgumentException("record not found: ${T::class.simpleName}:$id")

@Composable
@Preview
fun App() {
    MaterialTheme {
        Column(
            Modifier.padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            InputLine()
            ScanProgressBar()
        }
//        Button(onClick = {
//            text = "Hello, Desktop!"
//        }) {
//            Text(text)
//        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    println("starting")
    Log.connectConsole(Log.Level.INFO)
    val root = System.getProperties().getProperty("user.home") + "/dev"
    println(root)

    val sf = SearchFolder.get(null, Paths.get(root))
    println(sf)
    println(sf.parent)
    println(sf.pathString)

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
