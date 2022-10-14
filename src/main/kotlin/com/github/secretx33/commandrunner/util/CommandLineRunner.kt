package com.github.secretx33.commandrunner.util

import com.github.secretx33.commandrunner.CURRENT_OS
import com.github.secretx33.commandrunner.OSType
import com.github.secretx33.commandrunner.model.ParsedCommand
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Duration

class CommandLineRunner(
    private val directory: Path,
    private val command: ParsedCommand,
) {

    private var start = 0L

    fun run() {
        start = System.nanoTime()

        val process = ProcessBuilder()
            .directory(directory.toFile())
            .command(commandPrefixPerOS + command.value)
            .inheritIO()
            .start()

        process.use { it.waitFor() }
    }

    private val commandPrefixPerOS: List<String> get() = when (CURRENT_OS) {
        OSType.WINDOWS -> listOf("cmd", "/c")
        OSType.MAC_OS, OSType.LINUX -> listOf("/bin/bash", "-c")
    }

    private fun Process.use(block: (Process) -> Unit) {
        var exception: Throwable? = null
        try {
            block(this)
        } catch (e: Throwable) {
            exception = e
            throw e
        } finally {
            destroyFinally(exception)
            log.debug("Execution of command '${command.value}' took ${Duration.ofNanos(System.nanoTime() - start).toMillis()}ms")
        }
    }

    private fun Process.destroyFinally(cause: Throwable?) = when (cause) {
        null -> destroy()
        else -> try {
            destroy()
        } catch (closeException: Throwable) {
            cause.addSuppressed(closeException)
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
