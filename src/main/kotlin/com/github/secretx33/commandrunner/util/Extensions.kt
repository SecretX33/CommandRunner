package com.github.secretx33.commandrunner.util

import joptsimple.OptionSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import java.lang.invoke.MethodHandles
import java.time.Duration
import java.util.Locale
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration as KotlinDuration

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

fun ScheduledExecutorService.schedule(delay: Duration, runnable: Runnable) {
    schedule(runnable, delay.toMillis(), TimeUnit.MILLISECONDS)
}

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


fun <T, U> Flow<T>.debounceUniqueBy(
    delay: KotlinDuration,
    areEquals: (current: U, previous: U?) -> Boolean = { a, b -> a == b },
    selector: (T) -> U,
): Flow<T> {
    val lastValue = AtomicReference<T?>()
    val emitNextTask = AtomicReference<Job?>()
    return channelFlow {
        coroutineScope {
            collect { value ->
                val previousValue = lastValue.get()
                lastValue.set(value)
                if (areEquals(selector(value), previousValue?.let(selector))) return@collect

                val job = launch {
                    delay(delay)
                    lastValue.compareAndSet(value, null)
                    send(value)
                }
                emitNextTask.getAndSet(job)?.cancel()
                job.invokeOnCompletion { emitNextTask.compareAndSet(job, null) }
            }
        }
    }
}
