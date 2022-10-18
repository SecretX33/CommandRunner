/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.secretx33.commandrunner.util.unit

import java.io.Serializable

/**
 * A data size, such as '12MB'.
 *
 * This class models data size in terms of bytes and is immutable and thread-safe.
 *
 * The terms and units used in this class are based on [binary prefixes](https://en.wikipedia.org/wiki/Binary_prefix)
 * indicating multiplication by powers of 2. Consult the following table and the Javadoc for [DataUnit] for details.
 *
 * | Term     | Data Size | Size in Bytes     |
 * |----------|-----------|-------------------|
 * | byte     | 1B        | 1                 |
 * | kilobyte | 1KB       | 1,024             |
 * | megabyte | 1MB       | 1,048,576         |
 * | gigabyte | 1GB       | 1,073,741,824     |
 * | terabyte | 1TB       | 1,099,511,627,776 |
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 5.1
 * @see DataUnit
 */
class DataSize(private val bytes: Long) : Comparable<DataSize>, Serializable {

    /**
     * Checks if this size is negative, excluding zero.
     * @return true if this size has a size less than zero bytes
     */
    val isNegative: Boolean
        get() = bytes < 0

    /**
     * Return the number of bytes in this instance.
     * @return the number of bytes
     */
    fun toBytes(): Long = bytes

    /**
     * Return the number of kilobytes in this instance.
     * @return the number of kilobytes
     */
    fun toKilobytes(): Long = bytes / BYTES_PER_KB

    /**
     * Return the number of megabytes in this instance.
     * @return the number of megabytes
     */
    fun toMegabytes(): Long = bytes / BYTES_PER_MB

    /**
     * Return the number of gigabytes in this instance.
     * @return the number of gigabytes
     */
    fun toGigabytes(): Long = bytes / BYTES_PER_GB

    /**
     * Return the number of terabytes in this instance.
     * @return the number of terabytes
     */
    fun toTerabytes(): Long = bytes / BYTES_PER_TB

    override fun compareTo(other: DataSize): Int = bytes.compareTo(other.bytes)

    override fun toString(): String = String.format("%dB", bytes)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val otherSize = other as DataSize
        return bytes == otherSize.bytes
    }

    override fun hashCode(): Int = bytes.hashCode()

    companion object {
        /**
         * Defaults to `KILOBYTES` if the default unit is not specified.
         */
        val DEFAULT_UNIT = DataUnit.KILOBYTES

        /**
         * Bytes per Kilobyte.
         */
        private const val BYTES_PER_KB: Long = 1024

        /**
         * Bytes per Megabyte.
         */
        private const val BYTES_PER_MB = BYTES_PER_KB * 1024

        /**
         * Bytes per Gigabyte.
         */
        private const val BYTES_PER_GB = BYTES_PER_MB * 1024

        /**
         * Bytes per Terabyte.
         */
        private const val BYTES_PER_TB = BYTES_PER_GB * 1024

        /**
         * The pattern for parsing.
         */
        private val PATTERN by lazy { "^([+\\-]?\\d+)([a-zA-Z]{0,2})$".toRegex() }

        /**
         * Obtain a [DataSize] representing the specified number of bytes.
         * @param bytes the number of bytes, positive or negative
         * @return a [DataSize]
         */
        fun ofBytes(bytes: Long): DataSize = DataSize(bytes)

        /**
         * Obtain a [DataSize] representing the specified number of kilobytes.
         * @param kilobytes the number of kilobytes, positive or negative
         * @return a [DataSize]
         */
        fun ofKilobytes(kilobytes: Long): DataSize = DataSize(Math.multiplyExact(kilobytes, BYTES_PER_KB))

        /**
         * Obtain a [DataSize] representing the specified number of megabytes.
         * @param megabytes the number of megabytes, positive or negative
         * @return a [DataSize]
         */
        fun ofMegabytes(megabytes: Long): DataSize = DataSize(Math.multiplyExact(megabytes, BYTES_PER_MB))

        /**
         * Obtain a [DataSize] representing the specified number of gigabytes.
         * @param gigabytes the number of gigabytes, positive or negative
         * @return a [DataSize]
         */
        fun ofGigabytes(gigabytes: Long): DataSize = DataSize(Math.multiplyExact(gigabytes, BYTES_PER_GB))

        /**
         * Obtain a [DataSize] representing the specified number of terabytes.
         * @param terabytes the number of terabytes, positive or negative
         * @return a [DataSize]
         */
        fun ofTerabytes(terabytes: Long): DataSize = DataSize(Math.multiplyExact(terabytes, BYTES_PER_TB))

        /**
         * Obtain a [DataSize] representing an amount in the specified [DataUnit].
         * @param amount the amount of the size, measured in terms of the unit,
         * positive or negative
         * @return a corresponding [DataSize]
         */
        fun of(amount: Long, unit: DataUnit): DataSize = DataSize(Math.multiplyExact(amount, unit.size().toBytes()))

        /**
         * Obtain a [DataSize] from a text string such as `12MB` using
         * the specified default [DataUnit] if no unit is specified.
         *
         * The string starts with a number followed optionally by a unit matching one of the
         * supported [suffixes][DataUnit].
         *
         * Examples:
         * ```
         * "12KB" -- parses as "12 kilobytes"
         * "5MB"  -- parses as "5 megabytes"
         * "20"   -- parses as "20 kilobytes" (where the `defaultUnit` is [DataUnit.KILOBYTES])
         * ```
         *
         * @param text the text to parse
         * @return the parsed [DataSize]
         */
        fun parse(text: String, defaultUnit: DataUnit = DEFAULT_UNIT): DataSize =
            try {
                parseInternal(text, defaultUnit)
            } catch (e: Exception) {
                throw IllegalArgumentException("'$text' is not a valid data size", e)
            }

        /**
         * Does the same thing as [parse], but returns `null` instead of throwing when the parse fails.
         */
        fun parseOrNull(text: String, defaultUnit: DataUnit = DEFAULT_UNIT): DataSize? =
            try {
                parseInternal(text, defaultUnit)
            } catch (e: Exception) {
                null
            }

        private fun parseInternal(text: String, defaultUnit: DataUnit): DataSize {
            val matcher = PATTERN.matchEntire(text.toList().filterNot { it.isWhitespace() }.joinToString(""))
            checkNotNull(matcher) { "Does not match data size pattern" }
            val unit = determineDataUnit(matcher.groups[2]!!.value, defaultUnit)
            val amount = matcher.groups[1]!!.value.toLong()
            return of(amount, unit)
        }

        private fun determineDataUnit(suffix: String, defaultUnit: DataUnit): DataUnit =
            if (suffix.isNotEmpty()) DataUnit.fromSuffix(suffix) else defaultUnit
    }
}
