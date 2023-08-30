package net.sergeych.raysearch

import dbs
import inDb
import kotlinx.datetime.Instant
import net.sergeych.kotyara.db.DbContext
import net.sergeych.kotyara.db.Identifiable
import net.sergeych.kotyara.db.hasOne
import net.sergeych.mp_logger.LogTag
import net.sergeych.mp_logger.Loggable
import net.sergeych.mp_tools.toDataSize
import net.sergeych.mp_tools.trimToEllipsis
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime

class FileDoc(
    override val id: Long,
    val fileName: String,
    val searchFolderId: Long,
    val docDef: DocDef,
    val processedMtime: Instant? = null,
    @Suppress("unused") val detectedSize: Long = 0,
    val processedSize: Long? = null,
) : Identifiable<Long>, Loggable by LogTag("FD${id}:$fileName") {

    val folderRef = hasOne<SearchFolder> { searchFolderId }

    val folder: SearchFolder by lazy {
        dbs { folderRef.get(it) }!!
    }

    val path: Path by lazy {
        folder.path.resolve(fileName)
    }

    fun requestRescan(dbc: DbContext, file: Path) {
        dbc.update(
            """
            update file_docs
            set processed_mtime = null, processed_size = null, detected_size = ?
            where id=?
        """.trimIndent(), file.fileSize(), id
        )
        FileScanner.pulseChanged()
    }

    fun markProcessed() {
        dbs { dbc ->
            dbc.update("""update file_docs 
                |set processed_size=detected_size, processed_mtime=?
                |where id=?""".trimMargin(), path.getLastModifiedTime().toInstant(), id)
        }

    }

    fun loadText(): String {
        val src = docDef.textExtractor.extractTextFrom(path)
        println("$path: ${src.length.toDataSize()}")
        println(src.trimToEllipsis(70))
        return src
    }

    companion object {
        suspend fun firstNotProcessed(): FileDoc? = inDb { findWhere<FileDoc>("processed_mtime is null") }
    }
}