package net.sergeych.raysearch

import net.sergeych.appHomePath
import net.sergeych.kotyara.Database
import net.sergeych.kotyara.db.DbContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

val h2Home = appHomePath + "db"
val lucenHome = appHomePath + "lucen"

operator fun Path.plus(other: String): Path = resolve(other)
operator fun Path.plus(other: Path): Path = resolve(other)

@Suppress("unused")
fun deleteDb() {
    Files.deleteIfExists(h2Home + "rayscan.mv.db")
    Files.deleteIfExists(h2Home + "rayscan.trace.db")
    File(lucenHome.toString()).deleteRecursively()
}

val database by lazy {
    deleteDb()
    Files.createDirectories(h2Home)
    Files.createDirectories(lucenHome)    // init lucene
    Class.forName("org.h2.Driver")
    Database("jdbc:h2:$appHomePath/db/rayscan", 10, null)
        .also { it.migrateWithResources(object {}::class.java) }
}

val indexer by lazy {
    // ensure it is ok
    database.withContext {  }
    // now we can open indexer
    Indexer(lucenHome)
}

fun <T> dbs(f: (DbContext) -> T) = database.withContext(f)
suspend fun <T> db(f: suspend (DbContext) -> T) = database.asyncContext(f)
fun <T> inDbs(f: DbContext.() -> T) = database.inContext(f)
suspend fun <T> inDb(f: suspend DbContext.() -> T) = database.asyncContext(f)
inline fun <reified T : Any> DbContext.byIdOrThrow(id: Any): T =
    byId<T>(id) ?: throw IllegalArgumentException("record not found: ${T::class.simpleName}:$id")