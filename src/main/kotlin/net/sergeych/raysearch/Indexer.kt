package net.sergeych.raysearch

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import net.sergeych.mp_logger.LogTag
import net.sergeych.mp_logger.info
import net.sergeych.mptools.withReentrantLock
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.LongField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import java.nio.file.Path
import kotlin.io.path.pathString

class Indexer(path: Path) : LogTag("INDX") {

    private val changedChannel = Channel<Unit>(0)

    suspend fun waitChanges() { changedChannel.receive() }

    private var indexDirectory: Directory = FSDirectory.open(path)
    private val writer = IndexWriter(
        indexDirectory,
        IndexWriterConfig(StandardAnalyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND)
    )

    private val reader = DirectoryReader.open(writer)

    private val access = Mutex()

    suspend fun addDocument(fdoc: FileDoc) {
        val contentField = TextField(FN_CONTENT, fdoc.extractText(), Field.Store.NO)
        val pathField = TextField(FN_PATH, fdoc.path.pathString, Field.Store.NO)
        val idField = LongField(FN_ID, fdoc.id, Field.Store.YES)
        val doc = Document()
        doc.add(contentField)
        doc.add(pathField)
        doc.add(idField)
        access.withReentrantLock {
            writer.updateDocument(Term(FN_ID, fdoc.id.toString()), doc)
        }
        changedChannel.trySend(Unit)
        info { "indexed: $fdoc" }
    }

    suspend fun deleteDocument(fdoc: FileDoc) {
        access.withReentrantLock {
            writer.deleteDocuments(Term(FN_ID, fdoc.id.toString()))
        }
        changedChannel.trySend(Unit)
        info { "deleted: $fdoc" }
    }

    companion object {
        const val FN_CONTENT = "content"
        const val FN_PATH = "path"
        const val FN_ID = "id"
    }
}