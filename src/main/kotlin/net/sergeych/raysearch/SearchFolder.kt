package net.sergeych.raysearch

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sergeych.kotyara.db.DbContext
import net.sergeych.kotyara.db.Identifiable
import net.sergeych.kotyara.db.updateAndReturn
import net.sergeych.kotyara.db.updateCheck
import net.sergeych.mp_logger.*
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

    suspend fun folders() = inDb {
        select<SearchFolder>().where("parent_id=?", id).all
    }

    suspend fun rescan(cachedPath: String = pathString) {
        try {
            val myPath = Paths.get(cachedPath)
            if (parentId == null) {
                startScanning(id)
                // it also mean that this filder is not in FSWatch yet
                FSWatch.register(myPath)
            }
            val rule by lazy { getRule(cachedPath) }
            val knownFiles = files().map { it.fileName to it }.toMap().toMutableMap()
            val knownDirs = folders().map { it.name to it }.toMap().toMutableMap()
            if (!myPath.exists()) {
                isOk = false
//                db { destroy(it) }
//                debug { "path $cachedPath is deleted, removing from the database" }
                delete()
            } else {
                for (n in myPath.listDirectoryEntries("*")) {
                    if (knownFiles[n.name]?.isBad == true)
                        debug { "skipping known bad file $n" }
                    else when {
                        n.isDirectory() -> {
                            if (!rule.shouldSkipDir(n)) {
                                debug { "processing directory $n" }
                                val x = get(id, n)
                                x.rescan(n.pathString)
                                FSWatch.register(n)
                            }
                        }

                        n.isRegularFile() -> {
                            // if the rule gives not dd - we can't process the file
                            rule.detectDocType(n)?.let {
                                try {
                                    checkFile(it, n)
                                } catch (x: Exception) {
                                    info { "text extraction failed, marking as bad?" }
                                }
                            }
                        }

                        else -> {
                            debug { "no idea what to do with $n" }
                        }
                    }
                    knownFiles.remove(n.name)
                    knownDirs.remove(n.name)
                }
                for (fd in knownFiles.values) fd.delete()
                for (sf in knownDirs.values) sf.delete()
            }
        } finally {
            if (parentId == null) stopScanning(id)
        }
    }

    suspend fun checkFile(dd: DocType, file: Path) {
        db { dbc ->
            try {
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
                    insert into file_docs(file_name, search_folder_id, doc_type, detected_size)
                    values(?,?,?,?)""".trimIndent(),
                        file.fileName.toString(), id, dd, file.fileSize()
                    ).also { it.requestRescan(dbc, file) }
            } catch (x: Exception) {
                exception { "file checking file $file" to x }
            }
        }
    }

    suspend fun delete() {
        debug { "starting removing folder $this" }
        for (f in files())
            f.delete()
        for (d in folders()) {
            d.delete()
        }
        FSWatch.unregister(path)
        inDb { updateCheck(1, "delete from search_folders where id=?", id) }
    }

    companion object : LogTag("SRCHF") {
        fun get(parentId: Long?, n: Path): SearchFolder = dbs { get(it, parentId, n) }
        fun get(dbc: DbContext, parentId: Long?, n: Path): SearchFolder {
            val name = if (parentId == null) n.toString() else n.name
            try {
                return dbc.findBy<SearchFolder>("parent_id" to parentId, "name" to name)
                    ?: run {
                        val id = dbc.updateAndGetId<Long>(
                            "insert into search_folders(parent_id, name) values(?,?)",
                            parentId,
                            name
                        )!!
                        dbc.byIdOrThrow<SearchFolder>(id)
                    }
            } catch (x: Throwable) {
                exception { "exception while performing SearchFolder.get($parentId,$n)" to x }
                throw x
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
                                    || fileExists(parentPath + "/yarn.lock")
                                    || fileExists(parentPath + "/kotlin_js_store")
                                    )
                    ->
                        NpmProjectRule

                    parentPath.lowercase().endsWith("android/sdk") ->
                        SkipAllRule

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
                if (scanningRootIds.isEmpty())
                    _isScanning.value = true
                scanningRootIds += rootId
            }
        }

        protected fun stopScanning(rootId: Long) {
            synchronized(scanningRootIds) {
                scanningRootIds -= rootId
                if (scanningRootIds.isEmpty()) {
                    _isScanning.value = false
                }
            }
        }

        fun roots(dbc: DbContext) =
            dbc.query<SearchFolder>("select * from search_folders where parent_id is null")

        /**
         * Get the chain of searchFolders that leads to the path or as close to it as possible
         */
        suspend fun findFolderChain(item: Path): List<SearchFolder> {
            return inDb {
                val root = roots(this).firstOrNull { item.pathString.startsWith(it.name) }
                    ?: return@inDb listOf()
                val result = mutableListOf(root)
                var rest = Paths.get(item.pathString.substring(root.pathString.length))
                var currentId = root.id
                do {
                    val path = rest.subpath(0, 1)
                    val sf = queryRow<SearchFolder>(
                        """
                        select * from search_folders 
                        where parent_id=? and name=?
                        """.trimIndent(),
                        currentId, path.pathString
                    )
                    if (sf == null) {
                        debug { "only partial path was found: $result" }
                        break
                    }
                    result += sf
                    currentId = sf.id
                    if (rest.nameCount > 1)
                        rest = rest.subpath(1, rest.nameCount)
                    else
                        break
                } while (true)
                result
            }
        }

        suspend fun findFileDocChain(item: Path): Pair<List<SearchFolder>, FileDoc?> {
            val chain = findFolderChain(item.parent)
            if (chain.isEmpty()) return listOf<SearchFolder>() to null
            val last = chain.last()
            if (last.path != item.parent)
                return chain to null
            // path is ok. there could be a file
            val doc: FileDoc? = inDb {
                findBy("search_folder_id" to last.id, "file_name" to item.name)
            }
            return chain to doc
        }

        fun addNewFolder(chain: List<SearchFolder>, item: Path): SearchFolder? {
            debug { "adding new folder: chain: $chain existing: $item" }
            val last = chain.last()
            if (!item.pathString.startsWith(last.pathString)) {
                throw IllegalArgumentException("paths diverge: chain is ${last.pathString} new path is $item")
            }
            if (last.pathString == item.pathString) return last
            val nextPart = item.subpath(last.path.nameCount, last.path.nameCount + 1)
            val rule = getRule(item.parent.pathString)
            return if (rule.shouldSkipDir(item)) {
                debug { "we should not add folder to the index: $item" }
                null
            } else {
                debug { "will create search folder: $nextPart" }
                get(last.id, nextPart)
            }
        }

        private suspend fun deleteFolder(item: Path): Boolean {
            // folder chain will
            val searchFolder = findFolderChain(item).last()
            return if (item == searchFolder.path) {
                debug { "deleteFolder: found: $item" }
                searchFolder.delete()
                true
            } else {
                debug { "delete folder: not found, nothing to do" }
                false
            }
        }

        suspend fun deleteObject(item: Path) {
            if (!item.exists()) {
                if (deleteFolder(item)) return
                debug { "fonlder not found, trying to delete file: $item" }
                val (_, doc) = findFileDocChain(item)
                if (doc != null) {
                    debug { "deletefile: found: $doc, will delete" }
                    doc.delete()
                } else {
                    debug { "deleteFile: not found, nothing to do: $item" }
                }
            } else
                debug { "can't object from index as it exists in FS: $item" }
        }

        suspend fun actualize(item: Path) {
            if (isScanning.value) {
                warning { "can't actualize while scanning: $item" }
                return
            }
            if (!item.isReadable()) {
                warning { "can't actualize non-readable path: $item" }
            }
            if (item.isDirectory()) {
                val chain = findFolderChain(item)
                if (chain.isEmpty()) {
                    warning { "actualize: does not belong to any of our chains: $item" }
                } else {
                    val last = chain.last()
                    if (last.path == item) {
                        info { "found exact folder, what to do?" }
                    } else {
                        info { "create and add first level folder and rescan it" }
                        addNewFolder(chain, item)?.also { sf ->
                            info { "rescanning new folder $sf" }
                            sf.rescan()
                        }
                    }
                }
            } else {
                val (chain, doc) = findFileDocChain(item)
                info { "actualizing file plain file $item, chain is: ${chain.joinToString { it.name }}, doc is $doc" }
                if (chain.isEmpty()) return
                if (doc != null) {
                    info { "rescanning existing file" }
                    db { doc.requestRescan(it, item) }
                    return
                }
                // probably we already have a folder for it
                val last = chain.last()
                if (last.path == item.parent) {
                    info { "new file is created in existing folder: $last, creating file doc for it" }
                    last.createDoc(item)
                } else {
                    info { "we need to create and rescan floder structure: ${last.path} -> $item" }
                    addNewFolder(chain, item.parent)
                }
            }
        }
    }

    /**
     * Create fileDoc in the current folder, it it match the rules
     */
    private suspend fun createDoc(item: Path) {
        getRule(pathString).let { rule ->
            debug { "got a rule for a file: $item -> $rule" }
            rule.detectDocType(item)?.let { checkFile(it, item) }
                ?: debug { "file should not be included into index: $item" }
        }
    }
}

fun fileExists(pathString: String): Boolean = Paths.get(pathString).exists()
fun maskExists(pathString: String, mask: String): Boolean =
    Paths.get(pathString).listDirectoryEntries(mask).isNotEmpty()