package com.github.secretx33.commandrunner

import com.github.secretx33.commandrunner.exception.exitApp
import com.github.secretx33.commandrunner.model.Command
import com.github.secretx33.commandrunner.model.Settings
import com.github.secretx33.commandrunner.parser.converter.DurationConverter
import com.github.secretx33.commandrunner.parser.converter.FileSizeConverter
import com.github.secretx33.commandrunner.parser.converter.PathConverter
import com.github.secretx33.commandrunner.storage.FileModificationType
import com.github.secretx33.commandrunner.util.ANSI_RESET
import com.github.secretx33.commandrunner.util.getList
import com.github.secretx33.commandrunner.util.getOrDefault
import com.github.secretx33.commandrunner.util.getOrElse
import com.github.secretx33.commandrunner.util.getOrNull
import com.github.secretx33.commandrunner.util.getTextResource
import com.github.secretx33.commandrunner.util.isHelp
import com.github.secretx33.commandrunner.util.unit.DataSize
import joptsimple.OptionParser
import joptsimple.OptionSet
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.time.Duration
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists

private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

fun parseArgs(args: Array<String>): OptionSet {
    val parser = OptionParser().apply {
        acceptsAll(listOf("?", "help"), "Show the available CLI option flags").forHelp()
        accepts("command", "What commands should run whenever a file is changed").withRequiredArg().required()
            .describedAs("[repeatable]")
        acceptsAll(listOf("d", "delay"), "Delay for X amount of time before running the commands (default unit is ms)").withRequiredArg()
            .withValuesConvertedBy(DurationConverter)
        accepts("path", "Specifies the base folder. By default (or if not specified) it watches the current directory").withRequiredArg()
            .withValuesConvertedBy(PathConverter)
        acceptsAll(listOf("dr", "disable-recursive"), "Only watch the root path and not any folders inside it")
        acceptsAll(listOf("f", "filter"), "Ant matcher pattern that will be used to filter the relative path of the file (with extension)").withRequiredArg()
            .describedAs("[repeatable]")
        acceptsAll(listOf("ic", "ignore-case"), "Makes the provided filter ignore case")
            .availableIf("filter")
        accepts("min-size", "Only run commands for files with at least specified size").withRequiredArg()
            .withValuesConvertedBy(FileSizeConverter)
        accepts("max-size", "Only run commands for files with at most specified size").withRequiredArg()
            .withValuesConvertedBy(FileSizeConverter)
        accepts("fc", "Execute commands on file creation (default: run on file creation and modification only)")
        accepts("fm", "Execute commands on file modification (default: run on file creation and modification only)")
        accepts("fd", "Execute commands on file deletion (default: run on file creation and modification only). IMPORTANT: There is NO WAY of knowing whether a deleted thing was a file or folder, so if you enable this feature be aware that your commands will run for BOTH folder and files")
    }

    return parser.parse(*args).also {
        // Print help if asked
        if (it.isHelp) {
            val helpBanner = "$ANSI_RESET${getTextResource("banner_help.txt")}${System.lineSeparator()}"
            println(helpBanner)
            parser.printHelpOn(System.out)
        }
    }
}

/**
 * Parse a `OptionSet` into a valid [Settings] object.
 */
fun parseToSettings(options: OptionSet): Settings {
    val settings = buildSettings(options)
    validateSettings(settings)
    return settings
}

private fun buildSettings(options: OptionSet): Settings =
    Settings(
        commands = options.getList<String>("command").map { Command(it) },
        commandDelay = options.getOrDefault("delay", Duration.ZERO),
        folder = options.getOrElse("path") { Path("") }.absolute(),
        isRecursive = !options.has("disable-recursive"),
        filter = options.getList<String>("filter").toSet(),
        ignoreCase = options.has("ignore-case"),
        watchedModificationTypes = buildSet {
            if (options.has("fc")) add(FileModificationType.CREATE)
            if (options.has("fm")) add(FileModificationType.MODIFY)
            if (options.has("fd")) add(FileModificationType.DELETE)
        }.ifEmpty { FileModificationType.CREATE_AND_MODIFY },
        fileMinSizeBytes = options.getOrNull<DataSize>("min-size")?.toBytes(),
        fileMaxSizeBytes = options.getOrNull<DataSize>("max-size")?.toBytes(),
    )

private fun validateSettings(settings: Settings) {
    val errors = mutableListOf<String>()

    if (settings.folder.notExists()) {
        errors += "Path '${settings.folder}' does not exist, please specify a valid folder and try again"
    }
    if (!settings.folder.isDirectory()) {
        errors += "Path '${settings.folder}' is not a folder, please specify a valid folder and try again"
    }
    if (settings.commands.any { it.value.isBlank() }) {
        errors += "Cannot execute an empty or blank command, please provide only valid command strings and try again"
    }

    if (errors.isNotEmpty()) {
        log.error(errors.first())
        exitApp(1)
    }
}
