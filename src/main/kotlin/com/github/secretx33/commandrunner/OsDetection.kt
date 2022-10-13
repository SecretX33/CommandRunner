package com.github.secretx33.commandrunner

import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.Locale

private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

enum class OSType {
    WINDOWS,
    MAC_OS,
    LINUX,
}

val CURRENT_OS: OSType by lazy(LazyThreadSafetyMode.PUBLICATION) {
    val os = System.getProperty("os.name", "generic").lowercase(Locale.US)
    when {
        "mac" in os || "darwin" in os -> OSType.MAC_OS
        "win" in os -> OSType.WINDOWS
        else -> OSType.LINUX
    }.also { log.debug("Current detected OS: $it") }
}
