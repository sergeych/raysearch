package net.sergeych.raysearch

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.sergeych.mp_logger.LogTag
import net.sergeych.mp_logger.exception
import net.sergeych.mp_logger.info
import net.sergeych.mp_logger.warning
import net.sergeych.mp_tools.globalLaunch
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

sealed class FSEvent {
    abstract val item: Path

    data class DirCreated(override val item: Path) : FSEvent()

    /**
     * We can't judge at this point whether it was a file or a directory
     */
    data class EntryDeleted(override val item: Path) : FSEvent()
    data class FileModified(override val item: Path) : FSEvent()
    data class FileCreated(override val item: Path) : FSEvent()
}

@Suppress("UNCHECKED_CAST")
object FSWatch : LogTag("FSWAT") {

    private val watcher: WatchService = FileSystems.getDefault().newWatchService()


    private val keys = mutableMapOf<Path, WatchKey>()
    private val paths = mutableMapOf<WatchKey, Path>()
    val access = Object()

    private val _eventsFlow = MutableSharedFlow<FSEvent>(0, 1000)
    val events: SharedFlow<FSEvent> = _eventsFlow.asSharedFlow()

//    fun registerTree(root: Path) {
//        if (!root.isDirectory())
//            throw IllegalArgumentException("expected a directory: $root")
//        register(root)
//        Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
//            override fun visitFile(path: Path?, attr: BasicFileAttributes?): FileVisitResult {
//                path?.let {
//                    if (path.isReadable() && path.isDirectory())
//                        register(path)
//                }
//                return FileVisitResult.CONTINUE
//            }
//        })
//    }

    fun unregister(dir: Path) {
        synchronized(access) {
            keys[dir]?.let { key ->
                key.cancel()
                paths.remove(key)
                keys.remove(dir)
                info { "cancelled watch on $dir" }
            }
        }
    }

    fun register(path: Path) {
        synchronized(access) {
            if (path in path) {
                warning { "path already watched: $path" }
            } else {
                val key = path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
                keys[path] = key
                paths[key] = path
                info { "watching: $path" }
            }
        }
    }

    init {
        globalLaunch {
            while (true) {
                try {
                    val key = watcher.take()
                    val root = paths[key]
                    if (root == null) {
                        warning { "watch key has no registered path" }
                        continue
                    }
                    for (ev in key.pollEvents()) {
                        ev as? WatchEvent<Path> ?: continue
                        val kind: WatchEvent.Kind<Path> = ev.kind()
                        val name: Path = ev.context()
                        val path = root.resolve(name)
                        ev.kind().type()
                        when (kind) {
                            ENTRY_CREATE -> {
                                if (path.isDirectory())
                                    FSEvent.DirCreated(path)
                                else
                                    FSEvent.FileCreated(path)
                            }

                            ENTRY_DELETE -> FSEvent.EntryDeleted(path)

                            ENTRY_MODIFY -> {
                                if (path.isRegularFile())
                                    FSEvent.FileModified(path)
                                else null
                            }

                            else -> {
                                info { "ignoring unknown FSEvent type: $kind" }
                                null
                            }
                        }?.let {
                            info { "posting event: $it" }
                            _eventsFlow.tryEmit(it)
                        } ?: info { "this event is ignored: ${kind.name()}: $path" }
                    }
                    if (!key.reset()) {
                        synchronized(access) {
                            keys.remove(root)
                            paths.remove(key)
                        }
                    }
                } catch (t: Throwable) {
                    exception { "while watching fs events" to t }
                }
            }

        }
    }

}