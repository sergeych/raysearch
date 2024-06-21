package net.sergeych.raysearch

import kotlinx.datetime.Instant
import net.sergeych.kotyara.db.DbContext
import net.sergeych.kotyara.db.Identifiable
import net.sergeych.kotyara.db.hasOne
import net.sergeych.kotyara.db.updateCheck
import net.sergeych.mp_logger.LogTag
import net.sergeych.mp_logger.Loggable
import net.sergeych.mp_logger.debug
import net.sergeych.mp_logger.info
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime

class FileDoc(
    override val id: Long,
    val fileName: String,
    val searchFolderId: Long,
    val docType: DocType,
    val markedAsBad: Int? = null,
    val processedMtime: Instant? = null,
    @Suppress("unused") val detectedSize: Long = 0,
    val processedSize: Long? = null,
) : Identifiable<Long>, Loggable by LogTag("FD${id}:$fileName") {

    val isBad: Boolean get() = markedAsBad != null && markedAsBad >= currentBadMark

    val folderRef = hasOne<SearchFolder> { searchFolderId }

    val folder: SearchFolder by lazy {
        dbs { folderRef.get(it) }!!
    }

    val path: Path by lazy {
        folder.path.resolve(fileName)
    }

    fun extractText(): String =
        docType.extractTextFrom(path)

    fun requestRescan(dbc: DbContext, file: Path) {
        dbc.update(
            """
            update file_docs
            set processed_mtime = null, processed_size = null, marked_as_bad = null, detected_size = ?
            where id=?
        """.trimIndent(), file.fileSize(), id
        )
        Scanner.pulseChanged()
    }

    fun markProcessed() {
        dbs { dbc ->
            dbc.update(
                """update file_docs 
                |set processed_size=detected_size, processed_mtime=?
                |where id=?""".trimMargin(), path.getLastModifiedTime().toInstant(), id
            )
        }

    }

    override fun toString(): String = logTag
    suspend fun delete() {
        debug { "deleting, no more needed ;)" }
        indexer.deleteDocument(this)
        inDb { update("delete from file_docs where id=?", id) }
        debug { "deleted" }
    }

    suspend fun markInvalid() {
        inDb { updateCheck(1,
            "update file_docs set processed_mtime=now(),marked_as_bad=? where id=?",
            currentBadMark, id)
            info { "marked as invalid ($currentBadMark)"}
        }
    }

    companion object {

        const val currentBadMark = 1
        const val currentMaxSize = 3_145_726

        const val notBadCondition = "(marked_as_bad is null or marked_as_bad < $currentBadMark)"
        const val sizeLimitCondition = "(detected_size <= $currentMaxSize and detected_size > 0)"

        const val processableCondition = "($notBadCondition and $sizeLimitCondition)"

        suspend fun firstNotProcessed(count: Int = 10): List<FileDoc> =
            inDb {
                query(
                    """select * from file_docs 
                       where processed_mtime is null and
                            $notBadCondition and $sizeLimitCondition
                       limit ?""".trimIndent(),
                    count
                )
            }

        suspend fun get(folder: SearchFolder, name: String): FileDoc? =
            inDb { findBy("search_folder_id" to folder.id, "file_name" to name) }
    }
}