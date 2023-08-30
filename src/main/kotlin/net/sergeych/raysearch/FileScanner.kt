package net.sergeych.raysearch

import inDb
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
        val isEmpty = total.isEmpty && processed.isEmpty
    }

    private val _stats = MutableStateFlow<Stats>(Stats())
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    private val changeBouncer = Debouncer(GlobalScope, 100.milliseconds, 100.milliseconds) {
        inDb {
            val s1 = queryRow<Stat>(
                """
                select count(*) as files, coalesce(sum(detected_size),0) as size 
                from file_docs
                where processed_mtime is null
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

    fun startScanner() {
        globalLaunch {
            while(isActive) {
                val fd = FileDoc.firstNotProcessed()
                if (fd != null) {
                    println(fd.path)
                    cancel()
//                    delay(1000)
                }
                else delay(500)
            }
        }
    }


    fun pulseChanged() {
        changeBouncer.schedule()
    }

    init {
        pulseChanged()
        startScanner()
    }

}