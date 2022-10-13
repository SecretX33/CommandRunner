package com.github.secretx33.commandrunner.exception

/**
 * Simple exception to exit the app running the necessary finalizers. Does not produce any stacktrace whatsoever.
 */
class FinalizeAppThrowable(val exitCode: Int = 0) : Throwable(null, null, false, false)

fun exitApp(exitCode: Int = 0): Nothing = throw FinalizeAppThrowable(exitCode)
