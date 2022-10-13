@file:Suppress("FunctionName")

package com.github.secretx33.commandrunner.util

import com.github.benmanes.caffeine.cache.Caffeine
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * A simple expiring set implementation using Caffeine cache.
 *
 * @param E : Any The set cache type
 * @param duration Long How long should each item be kept
 * @param unit TimeUnit The time unit of the duration parameter
 */
fun <E : Any> ExpiringSet(duration: Long, unit: TimeUnit): MutableSet<E> = Collections.newSetFromMap(
    Caffeine.newBuilder()
        .expireAfterWrite(duration, unit)
        .build<E, Boolean>()
        .asMap()
)
