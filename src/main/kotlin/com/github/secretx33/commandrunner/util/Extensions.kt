package com.github.secretx33.commandrunner.util

import joptsimple.OptionSet
import java.lang.invoke.MethodHandles
import java.util.Locale

private val thisClass = MethodHandles.lookup().lookupClass()

/**
 * If this `OptionSet` has the flag asking for help.
 */
val OptionSet.isHelp: Boolean get() = has("?")

@Suppress("UNCHECKED_CAST")
fun <T> OptionSet.get(option: String): T = valueOf(option) as T

@Suppress("UNCHECKED_CAST")
fun <T> OptionSet.getList(option: String): List<T> = valuesOf(option) as List<T>

fun <T> OptionSet.getOrDefault(option: String, default: T): T = if (has(option)) get(option) else default

inline fun <T> OptionSet.getOrElse(option: String, default: () -> T): T = if (has(option)) get(option) else default()

fun <T> OptionSet.getOrNull(option: String): T? = if (has(option)) get(option) else null

fun <T> unsyncLazy(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)

fun String.capitalize(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

fun String.suffixIfNotEmpty(suffix: String): String = if (isNotEmpty()) this + suffix else this

fun String.pluralize(number: Int): String = if (number != 1) "${this}s" else this

fun getTextResource(path: String): String = thisClass.classLoader.getResourceAsStream(path)
    ?.bufferedReader()
    ?.use { it.readText() }
    ?: throw IllegalArgumentException("$path was not found in classpath")

fun Long.bytesToHumanReadableSize(): String = when {
    this == Long.MIN_VALUE || this < 0 -> "N/A"
    this < 1024L -> "$this bytes"
    this <= 0xfffccccccccccccL shr 40 -> "%.1f KB".format(Locale.ROOT, toDouble() / (0x1 shl 10))
    this <= 0xfffccccccccccccL shr 30 -> "%.1f MB".format(Locale.ROOT, toDouble() / (0x1 shl 20))
    this <= 0xfffccccccccccccL shr 20 -> "%.1f GB".format(Locale.ROOT, toDouble() / (0x1 shl 30))
    this <= 0xfffccccccccccccL shr 10 -> "%.1f TB".format(Locale.ROOT, toDouble() / (0x1 shl 40))
    this <= 0xfffccccccccccccL -> "%.1f PB".format(Locale.ROOT, (this shr 10).toDouble() / (0x1 shl 40))
    else -> "%.1f EB".format(Locale.ROOT, (this shr 20).toDouble() / (0x1 shl 40))
}
