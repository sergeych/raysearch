package net.sergeych.raysearch

import net.sergeych.appHomePath
import net.sergeych.kotyara.Database
import net.sergeych.kotyara.db.DbContext
import net.sergeych.mp_logger.Log
import net.sergeych.mp_logger.LogTag
import net.sergeych.mp_logger.info
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

val h2Home = appHomePath + "db"
val lucenHome = appHomePath + "lucen"

operator fun Path.plus(other: String): Path = resolve(other)
operator fun Path.plus(other: Path): Path = resolve(other)

@Suppress("unused")
fun deleteDb() {
    Files.deleteIfExists(h2Home + "raysearch.mv.db")
    Files.deleteIfExists(h2Home + "raysearch.trace.db")
    File(lucenHome.toString()).deleteRecursively()
}

private val ldb = LogTag("SDB")
val database by lazy {
    if (Params.dataSchemeVersion < 2) {
        ldb.info { "Old database  or no database at all, recreating:" }
        deleteDb()
        Params.copy(dataSchemeVersion = 2).save()
    }
//    else
//        deleteDb()
    Files.createDirectories(h2Home)
    Files.createDirectories(lucenHome)    // init lucene
    Class.forName("org.h2.Driver")
    Database("jdbc:h2:$appHomePath/db/raysearch", 10, null)
        .also {
            it.migrateWithResources(object {}::class.java)
            it.logLevel = Log.Level.INFO
        }
}

val indexer by lazy {
    // ensure it is ok
    database.withContext { }
    // now we can open indexer
    Indexer(lucenHome)
}

fun <T> dbs(f: (DbContext) -> T) = database.withContext(f)
suspend fun <T> db(f: suspend (DbContext) -> T) = database.asyncContext(f)
fun <T> inDbs(f: DbContext.() -> T) = database.inContext(f)
suspend fun <T> inDb(f: suspend DbContext.() -> T) = database.asyncContext(f)
inline fun <reified T : Any> DbContext.byIdOrThrow(id: Any): T =
    byId<T>(id) ?: throw IllegalArgumentException("record not found: ${T::class.simpleName}:$id")