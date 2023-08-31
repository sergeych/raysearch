package net.sergeych.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import net.sergeych.mp_logger.LogTag
import net.sergeych.mp_logger.exception
import net.sergeych.mp_tools.globalLaunch
import net.sergeych.mptools.Now
import kotlin.time.Duration

/**
 * Class to delayed ativate a block after a timeout after a [schedule] was called.
 *
 * known problems:
 *
 * - if start is called repeatedly and fast, the block will never been called, we need another
 * parameter to control it.
 *
 * - [executeNow] could actually wait up to [pause] time, we need to find a way to gracefully cancel delay
 */
class Debouncer(scope: CoroutineScope,
                val pause: Duration,
                val maxPause: Duration = pause*10,
                block: suspend () -> Unit) : LogTag("DEBNCR") {

    private var activateAt: Instant? = null
    private var activateNoLaterThan: Instant? = null
    private val mutex = Mutex()

    fun schedule() {
        globalLaunch {
            mutex.withLock {
                if (activateNoLaterThan == null)
                    activateNoLaterThan = Now() + maxPause
                val nextActivation = Now() + pause
                activateAt = if (nextActivation > activateNoLaterThan!!) activateNoLaterThan else nextActivation
            }
        }
    }

    fun executeNow() {
        activateAt = Now()
    }

    private var job: Job? = null

    /**
     * Cancels the debouncer. Does not execute even if scheduled. Execute it manually if needed.
     */
    fun cancel() {
        job?.cancel()
        job = null
    }

    init {
        job = scope.launch {
            while (true) {
                val left = activateAt?.let { t ->
                    t - Now()
                } ?: pause
//                if (left != pause) debug { "extra pause $left" }
                delay(left)
                val at = activateAt
                if (at != null) {
                    if (at <= Now()) {
                        try {
                            block()
                        }
                        catch(t: Throwable) {
                            exception { "unexpected error in debouncer block" to t }
                        }
                        mutex.withLock {
                            activateAt = null
                            activateNoLaterThan = null
                        }
                    }
                }
            }
        }
    }

}

/**
 * Composable-aware debouncer.
 * @param pause pause between [Debouncer.schedule] calls after which it executes the block
 * @param maxDelay maximum delay between first [Debouncer.schedule] invocation after which debouncer will
 *                 be executed despite any [Debouncer.schedule] calls.
 * @param executeOnDispose if true (default), when composable will be finally disposed
 *        debouncer will be executed. See [DisposableEffect] to understand composable lifetime
 * @return debouncer instance remembere in the context of the composable.
 */
@Composable
fun rememberDebouncer(
    pause: Duration,
    maxDelay: Duration = pause * 10,
    executeOnDispose: Boolean = true,
    block: suspend () -> Unit,
): Debouncer {
    val scope = rememberCoroutineScope()
    val debouncer = remember { Debouncer(scope, pause, maxDelay, block) }

    DisposableEffect(true) {
        onDispose {
            // and we check our param only there
            if( executeOnDispose ) debouncer.executeNow()
            debouncer.cancel()
        }
    }
    return debouncer
}
