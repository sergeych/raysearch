package net.sergeych.raysearch

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sergeych.kotyara.db.*
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

    suspend fun files() = inDb {
        select<FileDoc>().where("search_folder_id=?", id).all
    }

    suspend fun rescan(cachedPath: String = pathString) {
        try {
            if( parentId == null)
                startScanning(id)
            val p = Paths.get(cachedPath)
            val rule by lazy { getRule(cachedPath) }
            val known = files().map { it.fileName to it }.toMap().toMutableMap()
            if (!p.exists()) {
                isOk = false
                db { destroy(it) }
                info { "path $cachedPath is deleted, removing from the database" }
            } else {
                for (n in p.listDirectoryEntries("*")) {
                    if (known[n.name]?.isBad == true)
                        debug { "skipping known bad file $n" }
                    else when {
                        n.name == "tmp" -> {
                            debug { "skipping tmp" }
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
                            // if the rule gives not dd - we can't process the file
                            rule.docDef(n)?.let {
                                // and event if it gives, it could be invalid in theory
                                if (it.textExtractor.isValid(n))
                                    checkFile(it, n)
                                else {
                                    info { "the file is invalid for ${it.typeName}: $n" }
                                    markInvalid(n)
                                }
                            }
//                                ?: run {
//                                markInvalid(n)
//                            }
                        }

                        else -> {
                            info { "no idea what to do with $n" }
                        }
                    }
                    known.remove(n.name)
                }
                for (fd in known.values) fd.delete()
            }
        }
        finally {
            if( parentId == null ) stopScanning(id)
        }
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

    suspend fun markInvalid(file: Path) {
        db { dbc ->
            val cnt = dbc.update(
                """
                    update file_docs 
                    set processed_mtime=now(), is_bad=true 
                    where search_folder_id=? and file_name=?""".trimIndent(),
                id, file.fileName.toString()
            )
            if (cnt == 1)
                debug { "updated existing file as bad: $file" }
            else
                dbc.updateCheck(
                    1, """
                    insert into file_docs(file_name, search_folder_id, doc_def, detected_size, is_bad, processed_mtime)
                    values(?,?,?,?,true,now())
                    """.trimIndent(),
                    file.fileName.toString(), id, Json.encodeToString(DocDef.Invalid as DocDef), file.fileSize()
                ).also {
                    debug { "created new invalid file doc" }
                }
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

                    fileExists("$parentPath/node_modules") &&
                            (maskExists(parentPath, "*.json")
                                    || fileExists(parentPath+"/yarn.lock")
                                    || fileExists(parentPath+"/kotlin_js_store")
                                    )
                    ->
                        NpmProjectRule

                    else ->
                        DefaultSearchRule
                }
            }
        }

        private val _isScanning = MutableStateFlow<Boolean>(false)
        val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

        private val scanningRootIds = mutableSetOf<Long>()
        protected fun startScanning(rootId: Long) {
            synchronized(scanningRootIds) {
                if( scanningRootIds.isEmpty() )
                    _isScanning.value = true
                scanningRootIds += rootId
            }
        }

        protected fun stopScanning(rootId: Long) {
            synchronized(scanningRootIds) {
                println("stops $scanningRootIds: stop $rootId")
                scanningRootIds -= rootId
                println("now $scanningRootIds")
                if( scanningRootIds.isEmpty()) {
                    _isScanning.value = false
                }
            }
        }
    }
}

fun fileExists(pathString: String): Boolean = Paths.get(pathString).exists()
fun maskExists(pathString: String, mask: String): Boolean =
    Paths.get(pathString).listDirectoryEntries(mask).isNotEmpty()