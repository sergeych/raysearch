package net.sergeych.raysearch

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import net.sergeych.mp_logger.*
import net.sergeych.mp_tools.globalLaunch
import net.sergeych.tools.Debouncer
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.time.Duration.Companion.milliseconds

object Scanner : LogTag("SCANR") {

    data class Stat(val files: Long = 0, val size: Long = 0) {
        val isEmpty: Boolean = files == 0L && size == 0L
        operator fun plus(s: Stat): Stat = Stat(files + s.files, size + s.size)
    }

    data class Stats(val total: Stat = Stat(), val processed: Stat = Stat()) {
        @Suppress("unused")
        val isEmpty = total.isEmpty && processed.isEmpty
    }

    private val _stats = MutableStateFlow<Stats>(Stats())
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    @OptIn(DelicateCoroutinesApi::class)
    private val changeBouncer = Debouncer(GlobalScope, 100.milliseconds, 100.milliseconds) {
        inDb {
            val s1 = queryRow<Stat>(
                """
                select count(*) as files, coalesce(sum(detected_size),0) as size 
                from file_docs
                where processed_mtime is null and ${FileDoc.processableCondition}
            """.trimIndent()
            )!!
            val s2 = queryRow<Stat>(
                """
                select count(*) as files, coalesce(sum(detected_size),0) as size
                from file_docs
                where processed_mtime is not null and ${FileDoc.processableCondition}
            """.trimIndent()
            )!!
            _stats.value = Stats(s1 + s2, s2)
        }
    }

    private val scannerPulser = Channel<Unit>(0)

    val okExts = mutableSetOf<String>()
    val badExts = mutableSetOf<String>()

    fun startScanner() {
        globalLaunch {
            while (isActive) {
                val fds = FileDoc.firstNotProcessed(50)
                if (fds.isEmpty()) {
                    info { "Scanner step done, ok extensions: $okExts" }
                    info { "bad extensions: $badExts" }
                    scannerPulser.receive()
                }
                else {
                    for (fd in fds) {
                        try {
                            if (fd.detectedSize == 0L || fd.path.fileSize() == 0L) {
                                info { "empty file, will watch and scan later: ${fd.path}" }
                            } else {
                                if (indexer.addDocument(fd)) {
                                    debug { "success indexing $fd" }
                                    fd.markProcessed()
                                    okExts += fd.path.extension
                                } else {
                                    info { "failed indexing ${fd.path}" }
                                    fd.markInvalid()
                                    badExts += fd.path.extension
                                }
                            }
                        } catch (t: Throwable) {
                            warning { "failed to index document (failed to read): ${fd.docType}: ${fd.path} " }
                            fd.markInvalid()
                            badExts += fd.path.extension

                        }
                    }
                    indexer.commit()
                    changeBouncer.schedule()
                }
            }
        }
    }


    fun pulseChanged() {
        changeBouncer.schedule()
        scannerPulser.trySend(Unit)
    }

    fun startTreeWatch(defaultRoots: List<String>) {
        globalLaunch {
            debug { "scanning root" }
            val roots = db {
                var rr = SearchFolder.roots(it)
                if (rr.isEmpty()) {
                    debug { "no roots in db, creating defaults" }
                    for (dr in defaultRoots) {
                        SearchFolder.get(null, Paths.get(dr))
                    }
                    rr = SearchFolder.roots(it)
                }
                if (rr.isEmpty()) {
                    throw RuntimeException("can't create roots")
                }
                rr
            }
            debug { "there are ${roots.size} roots" }
            for (r in roots) {
                debug { "found root: ${r.pathString}" }
                r.rescan()
            }

            // FSEvents are yet buffered, so we have no RC. Now when scanning
            // is done, we can process with events.
            debug { "watching changes" }
            FSWatch.events.collect { fe ->
                info { "Collected: $fe" }
                try {
                    when (fe) {
                        is FSEvent.DirCreated -> SearchFolder.actualize(fe.item)
                        is FSEvent.EntryDeleted -> SearchFolder.deleteObject(fe.item)
                        is FSEvent.FileModified -> SearchFolder.actualize(fe.item)
                        is FSEvent.FileCreated -> SearchFolder.actualize(fe.item)
                    }
                } catch (t: Throwable) {
                    exception { "failure processing event $fe" to t }
                }
            }
        }
    }

    // slouper11
    fun setup(defaultRoots: List<String>) {
        startTreeWatch(defaultRoots)
        pulseChanged()
        startScanner()
    }

}