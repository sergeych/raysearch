package net.sergeych.raysearch

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import net.sergeych.mp_logger.LogTag
import net.sergeych.mp_logger.debug
import net.sergeych.mp_logger.info
import net.sergeych.mp_logger.warning
import net.sergeych.mptools.withReentrantLock
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.*
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.search.*
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import java.nio.file.Path
import kotlin.io.path.pathString

class Indexer(path: Path) : LogTag("INDX") {

    data class Result(val fd: FileDoc)

    private val changedChannel = Channel<Unit>(0)

    private var indexDirectory: Directory = FSDirectory.open(path)
    private val writer = IndexWriter(
        indexDirectory,
        IndexWriterConfig(StandardAnalyzer())
            .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND)
    )

    private val access = Mutex()

    //    private val reAsterisk = Regex("""(?:^|[^\\])\*""")
//    private val reQuestion = Regex("""(?:^|[^\\])\?""")
    private val reIsLucenMask = Regex("""((?:^|[^\\])\*|(?:^|[^\\])\?)""")

    suspend fun search(pattern: String, maxHits: Int = 100): List<Result> =
        access.withReentrantLock {
            writer.commit()
            val reader = DirectoryReader.open(writer)
            val searcher = IndexSearcher(reader)

            val query = BooleanQuery.Builder().apply {
                pattern.split(" ").map { it.trim() }.forEach { src ->
                    if (src.isNotBlank()) {
                        if (src.contains(reIsLucenMask)) {
                            add(WildcardQuery(Term(FN_CONTENT, src)), BooleanClause.Occur.SHOULD)
                        } else {
                            debug { "detected search mask: $src" }
                            add(TermQuery(Term(FN_CONTENT, src)), BooleanClause.Occur.SHOULD)
                        }
                    }
                }
            }.build()
            searcher.search(query,maxHits).scoreDocs.mapNotNull {
                val doc = reader.storedFields().document(it.doc)
                val id = doc.getField(FN_ID).numericValue().toLong()
                inDb { byId<FileDoc>(id) }?.let { fd -> Result(fd) }
                    ?: run {
                        warning { "filedoc not found $id" }
                        null
                    }
            }
        }

    suspend fun commit() {
        access.withReentrantLock { writer.commit() }
    }

    suspend fun addDocument(fdoc: FileDoc) {
        val contentField = TextField(FN_CONTENT, fdoc.extractText(), Field.Store.NO)
        val pathField = TextField(FN_PATH, fdoc.path.pathString, Field.Store.NO)
        val idField = LongField(FN_ID, fdoc.id, Field.Store.YES)
        val doc = Document()
        doc.add(contentField)
        doc.add(pathField)
        doc.add(idField)
        access.withReentrantLock {
            deleteDocument(fdoc)
            writer.addDocument(doc)
        }
        changedChannel.trySend(Unit)
        debug { "indexed: $fdoc" }
    }

    suspend fun deleteDocument(fdoc: FileDoc) {
        access.withReentrantLock {
            writer.deleteDocuments(LongPoint.newRangeQuery(FN_ID,fdoc.id, fdoc.id))
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