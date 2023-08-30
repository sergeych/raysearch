package net.sergeych.raysearch

import dbs
import inDb
import kotlinx.datetime.Instant
import net.sergeych.kotyara.db.DbContext
import net.sergeych.kotyara.db.Identifiable
import net.sergeych.kotyara.db.hasOne
import net.sergeych.mp_logger.LogTag
import net.sergeych.mp_logger.Loggable
import java.nio.file.Path
import kotlin.io.path.fileSize

class FileDoc(
    override val id: Long,
    val fileName: String,
    val searchFolderId: Long,
    val docDef: DocDef,
    val processedMtime: Instant? = null,
    val detectedSize: Long = 0,
    val processedSize: Long? = null,
) : Identifiable<Long>, Loggable by LogTag("FD${id}:$fileName") {

    val folderRef = hasOne<SearchFolder> { searchFolderId }

    val folder: SearchFolder by lazy {
        dbs { folderRef.get(it) }!!
    }

    val path: Path by lazy {
        println(folder.path)
        println(fileName)
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

    companion object {
        suspend fun firstNotProcessed(): FileDoc? = inDb { findWhere<FileDoc>("processed_mtime is null") }
    }
}