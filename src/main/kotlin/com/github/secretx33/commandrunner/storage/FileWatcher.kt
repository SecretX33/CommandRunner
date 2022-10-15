package com.github.secretx33.commandrunner.storage

import com.github.secretx33.commandrunner.util.ExpiringSet
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.WatchEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.relativeTo

/**
 * Simple implementation of [AbstractFileWatcher].
 *
 * @property basePath Path The base watched path, needs to be absolute
 * @property autoRegisterNewSubDirectories Boolean If this file watcher should discover directories
 */
class FileWatcher(
    private val basePath: Path,
    autoRegisterNewSubDirectories: Boolean = true,
    taskExecutor: Executor,  // must have at least 2 threads
) : AbstractFileWatcher(basePath.fileSystem, autoRegisterNewSubDirectories, taskExecutor) {

    /** A map of watched locations with corresponding listeners  */
    private val watchedLocations = ConcurrentHashMap<Path, WatchedLocation>()

    init {
        require(basePath.exists()) { "basePath needs to exist, current it doesn't" }
        require(basePath.isDirectory()) { "basePath needs to be a directory, but it isn't" }
        require(basePath.isAbsolute) { "basePath needs to be absolute" }
        super.registerRecursively(basePath)
        super.runEventProcessingLoop()
    }

    /**
     * Gets a [WatchedLocation] instance for the base path.
     *
     * @return WatchedLocation The watcher for the base path
     */
    fun getRootWatcher(): WatchedLocation = getWatcher(basePath)

    /**
     * Gets a [WatchedLocation] instance for a given path.
     *
     * @param path Path The path to get a watcher for
     * @return WatchedLocation The watched location
     */
    fun getWatcher(path: Path): WatchedLocation {
        val relativePath = when {
            path.isAbsolute -> path.relativeTo(basePath)
            path.startsWith(basePath) -> path
            else -> basePath.resolve(path).relativeTo(basePath)
        }
        return watchedLocations.computeIfAbsent(relativePath) { WatchedLocation(it) }
    }

    /**
     * Gets a [WatchedLocation] instance for the given path
     *
     * @param path String A path relative to the already provided basePath
     * @return WatchedLocation
     */
    fun getWatcher(path: String): WatchedLocation = getWatcher(Path(path))

    /**
     * Gets a [WatchedLocation] instance for a given path.
     *
     * @param path Path the path to get a watcher for
     * @param listener FileConsumer A shortcut to add a listener for when a file is updated
     * @return WatchedLocation the watched location
     */
    fun getWatcher(path: Path, listener: FileConsumer): WatchedLocation
        = getWatcher(path).apply { listen(listener) }

    fun withRootWatcher(listener: FileConsumer): WatchedLocation
        = getRootWatcher().apply { listen(listener) }

    fun getWatcher(path: String, listener: FileConsumer)
        = getWatcher(path).apply { listen(listener) }

    override fun processEvent(event: WatchEvent<Path>, filePath: Path, modificationType: FileModificationType) {
        // get the relative path of the event
        val fileRelativePath = filePath.relativeTo(basePath).takeIf { it.nameCount > 0 } ?: return
        // pass the event onto all watched locations that match
        watchedLocations.filter { filePath.startsWith(basePath.resolve(it.key).absolute()) }
            .forEach { (_, value) -> value.onEvent(fileRelativePath, modificationType) }
    }

    /**
     * Encapsulates a "watcher" in a specific directory.
     *
     * @property path Path The directory or file being watched by this instance.
     */
    data class WatchedLocation internal constructor(val path: Path) {

        private val log = LoggerFactory.getLogger(this::class.java)

        /** A set of files which have been modified recently  */
        private val recentlyModifiedFiles: MutableSet<String> = ExpiringSet(500, TimeUnit.MILLISECONDS)

        /**
         * The listener callback functions.
         * Path is the relative path of the file, from the basePath perspective
         */
        private val callbacks: MutableList<FileConsumer> = CopyOnWriteArrayList()

        /**
         * Triggered when a file is modified.
         *
         * @param event WatchEvent<Path>
         * @param filePath Path The path relative to the basePath
         */
        fun onEvent(filePath: Path, modificationType: FileModificationType) {
            // check if the file has been modified recently, process only file was not modified recently
            if (!recentlyModifiedFiles.add(filePath.toString())) return

            // pass the event onto registered listeners
            callbacks.forEach {
                try {
                    it(filePath, modificationType)
                } catch (e: Exception) {
                    log.error("", e)
                }
            }
        }

        /**
         * Record that a file has been changed recently.
         *
         * @param fileName the name of the file
         */
        fun recordChange(fileName: String) {
            recentlyModifiedFiles.add(fileName)
        }

        /**
         * Register a listener.
         *
         * @param listener the listener
         */
        fun listen(listener: FileConsumer) {
            callbacks.add(listener)
        }
    }
}

typealias FileConsumer = (Path, FileModificationType) -> Unit
