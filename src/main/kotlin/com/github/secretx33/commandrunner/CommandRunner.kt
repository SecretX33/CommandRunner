@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.github.secretx33.commandrunner

import com.github.secretx33.commandrunner.exception.FinalizeAppThrowable
import com.github.secretx33.commandrunner.exception.exitApp
import com.github.secretx33.commandrunner.model.Command
import com.github.secretx33.commandrunner.model.ParsedCommand
import com.github.secretx33.commandrunner.model.Settings
import com.github.secretx33.commandrunner.storage.FileModificationType
import com.github.secretx33.commandrunner.storage.FileWatcher
import com.github.secretx33.commandrunner.util.ANSI_GREEN
import com.github.secretx33.commandrunner.util.ANSI_PURPLE
import com.github.secretx33.commandrunner.util.ANSI_RESET
import com.github.secretx33.commandrunner.util.CommandLineRunner
import com.github.secretx33.commandrunner.util.debounceUniqueBy
import com.github.secretx33.commandrunner.util.getTextResource
import com.github.secretx33.commandrunner.util.isHelp
import com.github.secretx33.commandrunner.util.suffixIfNotEmpty
import com.github.secretx33.commandrunner.util.unsyncLazy
import io.github.azagniotov.matcher.AntPathMatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.debounce
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.invoke.MethodHandles
import java.nio.file.Path
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.absolute
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime
import kotlin.time.Duration.Companion.milliseconds

private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

private lateinit var fileWatcher: FileWatcher
private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
private val nowWithCustomPatternRegex = "\\{now:([^\\}]+)\\}".toRegex()

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
    if (log.isDebugEnabled) log.debug("Args = ${args.joinToString()}\noptions = ${options.asMap()}")

    val settings = parseToSettings(options)
    initiateFileWatcher(settings)
    printGreetings(settings)

    fileWatcher.join()
}

fun initiateFileWatcher(settings: Settings) {
    val pathMatcher = AntPathMatcher.Builder()
        .withPathSeparator('/')
        .apply { if (settings.ignoreCase) withIgnoreCase() }
        .build()

    fileWatcher = FileWatcher(settings.folder, taskExecutor = Dispatchers.IO.limitedParallelism(2).asExecutor())

    val flow = MutableSharedFlow<Pair<Path, FileModificationType>>()
    fileWatcher.withRootWatcher { path, fileModificationType ->
        runBlocking { flow.emit(path to fileModificationType) }
    }

    coroutineScope.launch {
        flow.debounceUniqueBy(50.milliseconds) { it.first }
            .debounce(settings.commandDelay)
            .collectLatest { (path, fileModificationType) ->
                if (!shouldRun(path, fileModificationType, settings, pathMatcher)) {
                    if (log.isTraceEnabled) log.trace("Skipping file '$path' because it does not match the settings")
                    return@collectLatest
                }
                log.info("File '$path' was '${fileModificationType.name.lowercase()}', running commands")
                executeCommands(path, settings)
            }
    }
}

/**
 * Analyses the file modification type, the file and current settings to decide if this file modification
 * should trigger the commands.
 */
private fun shouldRun(
    path: Path,
    fileModificationType: FileModificationType,
    settings: Settings,
    pathMatcher: AntPathMatcher,
): Boolean {
    val normalizedPath by unsyncLazy { path.pathString.normalizeWindowsPathSeparator() }
    val fileSize by unsyncLazy { settings.folder.resolve(path).fileSize() }

    return fileModificationType in settings.watchedModificationTypes
        && (settings.isRecursive || path.parent == null)
        && (!settings.hasFilter || settings.filter.any { pathMatcher.isMatch(it, normalizedPath) })
        && (!fileModificationType.isCreateOrModify || settings.fileMinSizeBytes == null || fileSize >= settings.fileMinSizeBytes)
        && (!fileModificationType.isCreateOrModify || settings.fileMaxSizeBytes == null || fileSize <= settings.fileMaxSizeBytes)
}

private fun executeCommands(
    file: Path,
    settings: Settings,
) = settings.commands.forEach {
    log.debug("Executing command '{}' for file '{}'", it.value, file)
    val parsedCommand = parseCommand(it, file, settings)
    CommandLineRunner(settings.folder, parsedCommand).run()
}

private fun parseCommand(
    command: Command,
    file: Path,
    settings: Settings,
): ParsedCommand {
    val absoluteFile = settings.folder.resolve(file).absolute()
    val absolutePath = absoluteFile.parent?.pathString.orEmpty()
    val relativePath = file.parent?.pathString.orEmpty()
    val now = LocalDateTime.now()

    val placeholders = mapOf(
        "{filename}" to file.name,
        "{filenamenoextension}" to file.nameWithoutExtension,
        "{fileextension}" to file.extension,
        "{filepath}" to absoluteFile.pathString,
        "{path}" to absolutePath,
        "{path+}" to absolutePath.suffixIfNotEmpty(File.separator),
        "{relativefilepath}" to file.pathString,
        "{relativepath}" to relativePath,
        "{relativepath+}" to relativePath.suffixIfNotEmpty(File.separator),
        "{pathseparator}" to File.separator,
        "{now}" to now.format(dateTimeFormatter),
    )

    val parsedCommand = placeholders.entries.fold(command.value) { acc, (placeholder, value) ->
        acc.replace(placeholder, value)
    }.let {
        nowWithCustomPatternRegex.replace(it) { matchResult ->
            now.format(DateTimeFormatter.ofPattern(matchResult.groupValues[1]))
        }
    }
    return ParsedCommand(parsedCommand)
}

private fun printGreetings(settings: Settings) {
    val banner = "${ANSI_RESET}${getTextResource("banner_logo.txt")}${System.lineSeparator().repeat(2)}"
    val monitoringStatus = "${ANSI_PURPLE}==> Now monitoring folder for changes: ${ANSI_GREEN}'${settings.folder}'${ANSI_RESET}${System.lineSeparator()}"
    val optionsDescription = "${settings.oneLineSummary()}${System.lineSeparator()}"

    log.info("$banner$monitoringStatus$optionsDescription")
}

private fun String.normalizeWindowsPathSeparator(): String = when (CURRENT_OS) {
    OSType.WINDOWS -> replace("\\", "/")
    else -> this  // No need for path normalization on Linux and MacOS
}

private fun finalize() {
    log.debug("Initiated execution of finalize tasks")
    val finalizeTime = measureNanoTime {
        if (::fileWatcher.isInitialized) fileWatcher.close()
        coroutineScope.coroutineContext.job.cancel()
    }
    log.debug("Finalization took ${Duration.ofNanos(finalizeTime).toMillis()}ms")
}
