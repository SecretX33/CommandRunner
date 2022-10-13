package com.github.secretx33.commandrunner.util

import joptsimple.OptionSet

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
