package net.sergeych.raysearch

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import net.sergeych.mp_tools.globalLaunch
import net.sergeych.tools.Debouncer
import kotlin.time.Duration.Companion.milliseconds

object FileScanner {

    data class Stat(val files: Long = 0, val size: Long = 0) {
        val isEmpty: Boolean = files == 0L && size == 0L
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
            """.trimIndent()
            )!!
            val s2 = queryRow<Stat>(
                """
                select count(*) as files, coalesce(sum(detected_size),0) as size
                from file_docs
                where processed_mtime is not null
            """.trimIndent()
            )!!
            _stats.value = Stats(s1, s2)
        }
    }

    private val scannerPulser = Channel<Unit>(0)

    fun startScanner() {
        globalLaunch {
            while(isActive) {
                val fds = FileDoc.firstNotProcessed(50)
                if( fds.isEmpty())
                    scannerPulser.receive()
                else {
                    for (fd in fds) {
                        indexer.addDocument(fd)
                        fd.markProcessed()
                        fd.loadText()
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

    init {
        pulseChanged()
        startScanner()
    }

}