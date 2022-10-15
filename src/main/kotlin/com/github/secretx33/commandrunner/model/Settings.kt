package com.github.secretx33.commandrunner.model

import com.github.secretx33.commandrunner.storage.FileModificationType
import com.github.secretx33.commandrunner.util.ANSI_CYAN
import com.github.secretx33.commandrunner.util.ANSI_RESET
import com.github.secretx33.commandrunner.util.bytesToHumanReadableSize
import com.github.secretx33.commandrunner.util.pluralize
import com.github.secretx33.commandrunner.util.unit.DurationStyle
import java.nio.file.Path
import java.time.Duration

data class Settings(
    val commands: List<Command>,
    val commandDelay: Duration,
    val folder: Path,
    val isRecursive: Boolean,
    val filter: Set<String>,
    val ignoreCase: Boolean,
    val watchedModificationTypes: Set<FileModificationType>,
    val fileMinSizeBytes: Long?,
    val fileMaxSizeBytes: Long?,
) {
    init {
        require(commandDelay >= Duration.ZERO) { "invalid commandDelay (expected >= 0, actual: $commandDelay)" }
        require(folder.isAbsolute) { "folder must be an absolute path" }
        require(watchedModificationTypes.isNotEmpty()) { "watchedModificationTypes cannot be empty" }
    }

    val hasFilter = filter.isNotEmpty()

    fun oneLineSummary(): String =
        buildString {
            append("${ANSI_CYAN}Number of commands: ${commands.size}")
            if (commandDelay > Duration.ZERO) append(" (delay: ${DurationStyle.SIMPLE.print(commandDelay)})")
            if (!isRecursive) append(". Monitoring only provided path (non-recursive)")
            if (hasFilter) append(". Using ${filter.size} ${"filter".pluralize(filter.size)}")
            if (ignoreCase) append(" (ignoring case)")
            if (fileMinSizeBytes != null) append(". Min file size: ${fileMinSizeBytes.bytesToHumanReadableSize()}")
            if (fileMaxSizeBytes != null) append(". Max file size: ${fileMaxSizeBytes.bytesToHumanReadableSize()}")
            append(". Watching for: ${watchedModificationTypes.joinToString { it.displayName }}")
            append(ANSI_RESET)
        }
}
