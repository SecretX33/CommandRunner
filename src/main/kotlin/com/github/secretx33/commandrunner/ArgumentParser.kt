package com.github.secretx33.commandrunner

import com.github.secretx33.commandrunner.exception.exitApp
import com.github.secretx33.commandrunner.model.Command
import com.github.secretx33.commandrunner.model.Settings
import com.github.secretx33.commandrunner.storage.FileModificationType
import com.github.secretx33.commandrunner.util.getList
import com.github.secretx33.commandrunner.util.getOrElse
import com.github.secretx33.commandrunner.util.isHelp
import joptsimple.OptionParser
import joptsimple.OptionSet
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.invoke.MethodHandles
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists

private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

fun parseArgs(args: Array<String>): OptionSet {
    val parser = OptionParser().apply {
        acceptsAll(listOf("?", "help"), "Show the available CLI option flags").forHelp()
        accepts("command", "Specifies what commands should run whenever a file is changed").withRequiredArg().required()
            .describedAs("[repeatable]")
        acceptsAll(listOf("folder", "path"), "Specifies the base folder. By default (or if not specified), it uses the current directory").withRequiredArg()
            .describedAs(Path("folder").resolve("anotherFolder").toString())
            .ofType(File::class.java)
        acceptsAll(listOf("dr", "disable-recursive"), "Watch only the specified folder and not any folders inside it (can only be used if using --folder)")
            .availableIf("folder")
        accepts("filter", "Ant matcher pattern that will be used to filter the relative path of the file (with extension)").withRequiredArg()
            .describedAs("[repeatable]")
        acceptsAll(listOf("ic", "ignore-case"), "Makes the provided filter ignore case")
            .availableIf("filter")
        accepts("fc", "Execute commands on file creation (if not specified, the commands will run only for file creation and modification)")
        accepts("fm", "Execute commands on file modification (if not specified, the commands will run only for file creation and modification)")
        accepts("fd", "Execute commands on file deletion (if not specified, the commands will run only for file creation and modification)")
        allowsUnrecognizedOptions()
    }
    return parser.parse(*args).also {
        // Print help if asked
        if (it.isHelp) {
            println("Command Runner help${System.lineSeparator().repeat(2)}")
            parser.printHelpOn(System.out)
        }
    }
}

fun buildSettings(options: OptionSet): Settings =
    Settings(
        commands = options.getList<String>("command").map { Command(it) },
        folder = options.getOrElse("folder") { File("") }.toPath().toAbsolutePath(),
        isRecursive = !options.has("disable-recursive"),
        filter = options.getList<String>("filter").toSet(),
        ignoreCase = options.has("ignore-case"),
        watchedModificationTypes = buildSet {
            if (options.has("fc")) add(FileModificationType.CREATE)
            if (options.has("fm")) add(FileModificationType.MODIFY)
            if (options.has("fd")) add(FileModificationType.DELETE)
        }.ifEmpty { FileModificationType.CREATE_AND_MODIFY }
    ).also(::validateSettings)

private fun validateSettings(settings: Settings) {
    val errors = mutableListOf<String>()

    if (settings.folder.notExists()) {
        errors += "Provided path '${settings.folder}' does not exist, please provide a valid folder and try again"
    }
    if (!settings.folder.isDirectory()) {
        errors += "Provided path '${settings.folder}' needs to be a folder, please provide a valid folder and try again"
    }
    if (settings.commands.any { it.value.isBlank() }) {
        errors += "Cannot execute an empty or blank command, please provide only valid command strings and try again"
    }

    if (errors.isNotEmpty()) {
        log.error(errors.first())
        exitApp(1)
    }
}
