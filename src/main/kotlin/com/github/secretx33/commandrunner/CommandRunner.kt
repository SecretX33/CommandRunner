package com.github.secretx33.commandrunner

import com.github.secretx33.commandrunner.exception.FinalizeAppThrowable
import com.github.secretx33.commandrunner.exception.exitApp
import com.github.secretx33.commandrunner.model.Command
import com.github.secretx33.commandrunner.model.ParsedCommand
import com.github.secretx33.commandrunner.model.Settings
import com.github.secretx33.commandrunner.storage.FileWatcher
import com.github.secretx33.commandrunner.util.isHelp
import io.github.azagniotov.matcher.AntPathMatcher
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.invoke.MethodHandles
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime

private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

private lateinit var fileWatcher: FileWatcher

fun runCommandParser(args: Array<String>) {
    var finalizeAppThrowable: FinalizeAppThrowable? = null
    try {
        execute(args)
    } catch (t: FinalizeAppThrowable) {
        finalizeAppThrowable = t
    } finally {
        finalize()
    }
    exitProcess(finalizeAppThrowable?.exitCode ?: 0)
}

private fun execute(args: Array<String>) {
    val options = parseArgs(args)

    if (options.isHelp) {
        // Do not proceed if the user is just asking for help
        exitApp()
    }
    log.debug("Args = ${args.joinToString()}\noptions = ${options.asMap()}")

    val settings = buildSettings(options)
    initiateFileWatcher(settings)

    log.info("==> Now monitoring folder for changes '${settings.folder}'")
    fileWatcher.join()
}

fun initiateFileWatcher(settings: Settings) {
    val pathMatcher = AntPathMatcher.Builder()
        .withPathSeparator('/')
        .apply { if (settings.ignoreCase) withIgnoreCase() }
        .build()

    fileWatcher = FileWatcher(settings.folder)
    fileWatcher.withRootWatcher { path, fileModificationType ->
        val normalizedPath = path.pathString.normalizeWindowsPathSeparator()

        if (fileModificationType !in settings.watchedModificationTypes
            || (settings.hasFilter && settings.filter.none { pathMatcher.isMatch(it, normalizedPath) })) return@withRootWatcher

        settings.commands.forEach {
            log.debug("Executing command '{}' for file '{}'", it.value, path)
            val parsedCommand = parseCommand(it, path, settings)
            CommandLineRunner(settings.folder, parsedCommand).run()
        }
    }
}

private fun parseCommand(
    command: Command,
    file: Path,
    settings: Settings
): ParsedCommand {
    val rawCommand = command.value
    val parsedCommand = rawCommand
        .replace("{filename}", file.name)
        .replace("{filenamenoextension}", file.nameWithoutExtension)
        .replace("{fileextension}", file.extension)
        .replace("{filepath}", settings.folder.resolve(file).pathString)
        .replace("{path}", settings.folder.pathString)
        .replace("{relativefilepath}", file.pathString)
        .replace("{relativepath}", file.parent?.pathString.orEmpty())
        .replace("{pathseparator}", File.separator)
    return ParsedCommand(parsedCommand)
}

private fun String.normalizeWindowsPathSeparator(): String = when (CURRENT_OS) {
    OSType.WINDOWS -> replace("\\", "/")
    else -> this  // No need for path normalization on Linux and MacOS
}

private fun finalize() {
    log.debug("Initiated execution of finalize tasks")
    val finalizeTime = measureNanoTime {
        if (::fileWatcher.isInitialized) {
            fileWatcher.close()
        }
    }
    log.debug("Finalization took ${Duration.ofNanos(finalizeTime).toMillis()}ms")
}
