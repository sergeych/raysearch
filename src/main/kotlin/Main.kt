import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.application
import net.sergeych.kotyara.Database
import net.sergeych.kotyara.db.DbContext
import net.sergeych.mp_logger.Log
import net.sergeych.raysearch.SearchFolder
import java.nio.file.Paths

private object DataHolder

val dataBase by lazy {
    Class.forName("org.h2.Driver")
    Database("jdbc:h2:mem:kotyara-test", 10, null)
        .also { it.migrateWithResources(DataHolder::class.java )}
}

fun <T>db(f: (DbContext) -> T) = dataBase.withContext(f)
fun <T>inDb(f: DbContext.() -> T) = dataBase.inContext(f)

inline fun <reified T: Any>DbContext.byIdOrThrow(id: Any): T = byId<T>(id) ?: throw IllegalArgumentException("record not found: ${T::class.simpleName}:$id")

@Composable
@Preview
fun App() {
    var text by remember { mutableStateOf("Hello, World!") }

    MaterialTheme {
        Button(onClick = {
            text = "Hello, Desktop!"
        }) {
            Text(text)
        }
    }
}

fun main() = application {
    println("starting")
    Log.connectConsole(Log.Level.INFO)
    val root = System.getProperties().getProperty("user.home") + "/dev"
    println(root)

    val sf = SearchFolder.get(null, Paths.get(root))
    println(sf)
    println(sf.parent)
    println(sf.pathString)
    sf.rescan()
//    Window(onCloseRequest = ::exitApplication) {
//        App()
//    }
}
