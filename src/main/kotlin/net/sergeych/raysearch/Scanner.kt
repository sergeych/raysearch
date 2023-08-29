package net.sergeych.raysearch

import byIdOrThrow
import db
import inDb
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import net.sergeych.kotyara.db.Identifiable
import net.sergeych.kotyara.db.destroy
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
        parentId?.let { inDb { byIdOrThrow(it) } }
    }

    val pathString: String by lazy {
        parent?.pathString?.let { "$it/$name" } ?: name
    }

    val path by lazy { Paths.get(pathString) }

    var isOk: Boolean = true
        private set

    fun rescan(cachedPath: String = pathString): SearchFolder {
        val p = Paths.get(cachedPath)
        if (!p.exists()) {
            isOk = false
            db { destroy(it) }
            info { "path $cachedPath is deleted, removing from the database" }
        } else {
            val mtime = p.getLastModifiedTime().toInstant().toKotlinInstant()
            if (checkedMtime == mtime) {
                info { "mitime is not changed, skipping" }
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
                            if (shouldScanDir(pathString, n.name)) {
                                debug { "processing directory $n" }
                                get(parentId, n).rescan(n.pathString)
                            }
                        }

                        n.isRegularFile() -> {
                            info { "need to scan regular file $n" }
                        }
                    }
                }
                // The directory is scanned: we save it mtime
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

    companion object {
        fun get(parentId: Long?, n: Path) = db { dbc ->
            dbc.queryRow<SearchFolder>(
                "select * from search_folders where parent_id=? and name=?",
                parentId,
                n.name
            ) ?: run {
                val id = dbc.updateAndGetId<Long>(
                    "insert into search_folders(parent_id, name) values(?,?)",
                    parentId,
                    if (parentId == null) n.pathString else n.name
                )!!
                dbc.byIdOrThrow<SearchFolder>(id)
            }
        }

        fun shouldScanDir(parentPath: String, dir: String): Boolean {
            // We primarily have rules for development libraries or like
            if (sholuldSkipDir(parentPath, dir)) return false
            return true
        }

        private enum class DirType {
            Normal,
            Gradle,
            Npm,
        }

        private val directoryTypes = mutableMapOf<String, DirType>()

        fun sholuldSkipDir(parentPath: String, dir: String): Boolean {
            val t = directoryTypes.getOrPut(parentPath) {
                when {
                    fileExists(parentPath + "/build.gradle")
                            || fileExists(parentPath + "/build.gradle.kts") ->
                                DirType.Gradle

                    fileExists("$parentPath/node_modules") && maskExists(parentPath, "*.json") ->
                        DirType.Npm

                    else ->
                        DirType.Normal
                }
            }
            return when (t) {
                DirType.Normal -> false
                DirType.Npm -> dir == "node_modules"
                DirType.Gradle -> dir == "build" || dir == "gradle"
            }
        }
    }
}

fun fileExists(pathString: String): Boolean = Paths.get(pathString).exists()
fun maskExists(pathString: String, mask: String): Boolean =
    Paths.get(pathString).listDirectoryEntries(mask).isNotEmpty()