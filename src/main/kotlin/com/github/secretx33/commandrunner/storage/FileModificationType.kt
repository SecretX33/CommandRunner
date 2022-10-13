package com.github.secretx33.commandrunner.storage

import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent

enum class FileModificationType {
    CREATE,
    MODIFY,
    DELETE,
    OTHER;

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
