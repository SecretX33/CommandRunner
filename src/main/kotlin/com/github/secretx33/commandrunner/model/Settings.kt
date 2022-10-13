package com.github.secretx33.commandrunner.model

import com.github.secretx33.commandrunner.storage.FileModificationType
import java.nio.file.Path

data class Settings(
    val commands: List<Command>,
    val folder: Path,
    val isRecursive: Boolean,
    val filter: Set<String>,
    val ignoreCase: Boolean,
    val watchedModificationTypes: Set<FileModificationType>,
) {
    init {
        require(folder.isAbsolute) { "folder must be an absolute path" }
        require(watchedModificationTypes.isNotEmpty()) { "watchedModificationTypes cannot be empty!" }
    }

    val hasFilter = filter.isNotEmpty()
}
