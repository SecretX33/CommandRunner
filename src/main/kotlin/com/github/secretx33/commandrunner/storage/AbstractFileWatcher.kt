package com.github.secretx33.commandrunner.storage

import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystem
import java.nio.file.FileVisitResult
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.StandardWatchEventKinds.OVERFLOW
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.visitFileTree

/**
 * Utility for "watching" for file changes using a [WatchService].
 *
 * @property autoRegisterNewSubDirectories Boolean If this file watcher should discover directories
 */
@OptIn(ExperimentalPathApi::class)
abstract class AbstractFileWatcher (
    fileSystem: FileSystem,
    private val autoRegisterNewSubDirectories: Boolean,
    private val taskExecutor: Executor,  // must have at least 2 threads
) : AutoCloseable {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * The watch service.
     */
    private val watchService: WatchService = fileSystem.newWatchService()

    /**
     * A map of all registered watch keys.
     */
    private val keys = ConcurrentHashMap<WatchKey, Path>()

    /**
     * The thread currently being used to wait for and process watch events.
     * */
    private val processingTask = AtomicReference<CompletableFuture<Void>?>()

    /**
     * Register a watch key in the given directory.
     *
     * @param directory the directory
     * @throws IOException if unable to register a key
     */
    fun register(directory: Path) {
        val key = register(watchService, directory)
        keys[key] = directory
    }

    /**
     * Get a [WatchKey] from the given [WatchService] in the given [directory][Path].
     *
     * @param watchService the watch service
     * @param directory the directory
     * @return the watch key
     * @throws IOException if unable to register
     */
    private fun register(watchService: WatchService, directory: Path): WatchKey =
        directory.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)

    /**
     * Register a watch key recursively in the given directory.
     *
     * @param root the root directory
     * @throws IOException if unable to register a key
     */
    fun registerRecursively(root: Path) {
        root.visitFileTree(object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                register(dir)
                return super.preVisitDirectory(dir, attrs)
            }
        })
    }

    /**
     * Process an observed watch event.
     *
     * @param event the event
     * @param filePath the resolved event context
     */
    protected abstract fun processEvent(event: WatchEvent<Path>, filePath: Path, modificationType: FileModificationType)

    /**
     * Processes [WatchEvent]s from the watch service until it is closed, or until the coroutine job is interrupted.
     */
    @Suppress("UNCHECKED_CAST")
    fun runEventProcessingLoop() {
        taskExecutor.execute {
            val task = CompletableFuture<Void>()
            check(processingTask.compareAndSet(null, task)) { "A coroutine is already processing events for this watcher." }

            while (true) {
                // poll for a key from the watch service
                val key: WatchKey = try {
                    watchService.take()
                } catch (e: InterruptedException) {
                    break
                } catch (e: ClosedWatchServiceException) {
                    break
                }

                // find the directory the key is watching
                val directory = keys[key]
                if (directory == null) {
                    key.cancel()
                    continue
                }

                val validFileEvents = (key.pollEvents() as Iterable<WatchEvent<Path>>)
                    .filter { it.kind() != OVERFLOW && (it.context()?.nameCount ?: 0) > 0 }
                    .associateWith { directory.resolve(it.context()) }

                // process each watch event the key has
                validFileEvents.forEach { (event, file) ->
                    // if the file is being deleted or is a regular file, send the event on to be processed
                    if (event.kind() == ENTRY_DELETE || file.isRegularFile()) {
                        processEvent(event, file, event.kind().modificationType)
                    }

                    // handle recursive directory creation
                    if (autoRegisterNewSubDirectories && event.kind() == ENTRY_CREATE) {
                        try {
                            if (file.isDirectory(LinkOption.NOFOLLOW_LINKS)) {
                                registerRecursively(file)
                            }
                        } catch (e: IOException) {
                            log.error("Failed to register new created directory under a watched folder. Message: ${e.message}", e)
                        }
                    }
                }

                // reset the key
                val valid = key.reset()
                if (!valid) keys.remove(key)
            }
            processingTask.compareAndSet(task, null)
        }
    }

    fun join() {
        val task = processingTask.get()!!
        log.debug("Joining file event processing loop from ${this@AbstractFileWatcher::class.simpleName} now...")
        task.get()
    }

    override fun close() {
        try {
            watchService.close()
        } catch (e: IOException) {
            log.error("An exception has occurred when closing File Watch Service. Message: ${e.message}", e.takeIf { log.isDebugEnabled })
        }
    }
}
