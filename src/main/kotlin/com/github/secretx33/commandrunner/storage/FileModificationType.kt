package com.github.secretx33.commandrunner.storage

import com.github.secretx33.commandrunner.util.capitalize
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.util.Locale

enum class FileModificationType {
    CREATE,
    MODIFY,
    DELETE,
    OTHER;

    val displayName = name.lowercase(Locale.US).capitalize()
    val isCreateOrModify get() = this == CREATE || this == MODIFY

    companion object {
        val CREATE_AND_MODIFY = setOf(CREATE, MODIFY)
    }
}

val WatchEvent.Kind<Path>.modificationType: FileModificationType get() = when {
    this == StandardWatchEventKinds.ENTRY_CREATE -> FileModificationType.CREATE
    this == StandardWatchEventKinds.ENTRY_MODIFY -> FileModificationType.MODIFY
    this == StandardWatchEventKinds.ENTRY_DELETE -> FileModificationType.DELETE
    else -> FileModificationType.OTHER
}
