package net.sergeych.raysearch

import byIdOrThrow
import db
import dbs
import inDb
import inDbs
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sergeych.kotyara.db.DbContext
import net.sergeych.kotyara.db.Identifiable
import net.sergeych.kotyara.db.destroy
import net.sergeych.kotyara.db.updateAndReturn
import net.sergeych.mp_logger.LogTag
import net.sergeych.mp_logger.Loggable
import net.sergeych.mp_logger.debug
import net.sergeych.mp_logger.info
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

data class SearchFolder(
    override val id: Long,
    val parentId: Long? = 0,
    val name: String,
    val checkedMtime: Instant? = null,
) : Identifiable<Long>, Loggable by LogTag("SF:$id:$name") {


    val parent: SearchFolder? by lazy {
        parentId?.let { inDbs { byIdOrThrow(it) } }
    }

    val pathString: String by lazy {
        parent?.pathString?.let { "$it/$name" } ?: name
    }

    @Suppress("unused")
    val path: Path by lazy { Paths.get(pathString) }

    var isOk: Boolean = true
        private set

    suspend fun rescan(cachedPath: String = pathString): SearchFolder {
        val p = Paths.get(cachedPath)
        val rule by lazy { getRule(cachedPath) }
        if (!p.exists()) {
            isOk = false
            db { destroy(it) }
            info { "path $cachedPath is deleted, removing from the database" }
        } else {
            val mtime = p.getLastModifiedTime().toInstant().toKotlinInstant()
            if (checkedMtime == mtime) {
                debug { "mtime is not changed, skipping" }
            } else {
                for (n in p.listDirectoryEntries("*")) {
                    when {
                        n.name == "tmp" -> {
                            debug { "skiping tmp" }
                        }

                        n.name[0] == '.' -> {
                            debug { "skipping dot file $n" }
                        } // skip
                        n.isHidden() -> {
                            debug { "ignoring hidden file $n" }
                        }

                        n.isDirectory() -> {
                            if (!rule.shouldSkipDir(pathString, n.name)) {
                                debug { "processing directory $n" }
                                get(id, n).rescan(n.pathString)
                            }
                        }

                        n.isRegularFile() -> {
                            rule.textExtractor(n)?.let { checkFile(it, n) }
                        }
                    }
                }
                // todo: remove from index files that no longer exists!
                // todo: mark directory scanned by mtime to speedup next scan
                return inDb {
                    update(
                        "update search_folders set checked_mtime=? where id=?",
                        mtime, id
                    )
                    copy(checkedMtime = mtime)
                }
            }
        }
        return this
    }

    suspend fun checkFile(dd: DocDef, file: Path) {
        db { dbc ->
            dbc.findBy<FileDoc>(
                "search_folder_id" to id,
                "file_name" to file.fileName.toString()
            )?.also {
                val m1 = it.processedMtime?.epochSeconds
                val m2 = file.getLastModifiedTime().toMillis() / 1000
                if ((it.processedSize != null && it.processedSize != file.fileSize()) ||
                    (m1 != null && m1 != m2)
                )
                    it.requestRescan(dbc, file)
            }
                ?: dbc.updateAndReturn<FileDoc, Long>(
                    """
                    insert into file_docs(file_name, search_folder_id, doc_def, detected_size)
                    values(?,?,?,?)""".trimIndent(),
                    file.fileName.toString(), id, Json.encodeToString(dd), file.fileSize()
                ).also { it.requestRescan(dbc, file) }
        }
    }

    companion object {
        fun get(parentId: Long?, n: Path): SearchFolder = dbs { get(it, parentId, n) }
        fun get(dbc: DbContext, parentId: Long?, n: Path): SearchFolder {
            val name = if (parentId == null) n.toString() else n.name
            return dbc.findBy<SearchFolder>("parent_id" to parentId, "name" to name)
                ?: run {
                    val id = dbc.updateAndGetId<Long>(
                        "insert into search_folders(parent_id, name) values(?,?)",
                        parentId,
                        name
                    )!!
                    dbc.byIdOrThrow<SearchFolder>(id)
                }
        }

        private val directoryTypes = mutableMapOf<String, SearchRule>()

        fun getRule(parentPath: String): SearchRule {
            return directoryTypes.getOrPut(parentPath) {
                when {
                    fileExists(parentPath + "/build.gradle")
                            || fileExists(parentPath + "/build.gradle.kts") ->
                        GradleProjectRule

                    fileExists("$parentPath/node_modules") && maskExists(parentPath, "*.json") ->
                        NpmProjectRule

                    else ->
                        NoSearchRule
                }
            }
        }
    }
}

fun fileExists(pathString: String): Boolean = Paths.get(pathString).exists()
fun maskExists(pathString: String, mask: String): Boolean =
    Paths.get(pathString).listDirectoryEntries(mask).isNotEmpty()