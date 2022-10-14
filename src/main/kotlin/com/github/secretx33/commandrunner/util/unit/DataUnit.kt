/*
 * Copyright 2002-2019 the original author or authors.
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

/**
 * A standard set of [DataSize] units.
 *
 * The unit prefixes used in this class are
 * [binary prefixes](https://en.wikipedia.org/wiki/Binary_prefix)
 * indicating multiplication by powers of 2. The following table displays the
 * enum constants defined in this class and corresponding values.
 *
 * | Constant    | Data Size | Power of 2 | Size in Bytes     |
 * |-------------|-----------|------------|-------------------|
 * | [BYTES]     | 1B        | 2^0        | 1                 |
 * | [KILOBYTES] | 1KB       | 2^10       | 1,024             |
 * | [MEGABYTES] | 1MB       | 2^20       | 1,048,576         |
 * | [GIGABYTES] | 1GB       | 2^30       | 1,073,741,824     |
 * | [TERABYTES] | 1TB       | 2^40       | 1,099,511,627,776 |
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 5.1
 * @see DataSize
 */
enum class DataUnit(private val suffix: String, private val size: DataSize) {

    /**
     * Bytes, represented by suffix `B`.
     */
    BYTES("B", DataSize.ofBytes(1)),

    /**
     * Kilobytes, represented by suffix `KB`.
     */
    KILOBYTES("KB", DataSize.ofKilobytes(1)),

    /**
     * Megabytes, represented by suffix `MB`.
     */
    MEGABYTES("MB", DataSize.ofMegabytes(1)),

    /**
     * Gigabytes, represented by suffix `GB`.
     */
    GIGABYTES("GB", DataSize.ofGigabytes(1)),

    /**
     * Terabytes, represented by suffix `TB`.
     */
    TERABYTES("TB", DataSize.ofTerabytes(1));

    fun size(): DataSize {
        return size
    }

    companion object {
        /**
         * Return the [DataUnit] matching the specified `suffix`.
         *
         * @param suffix one of the standard suffixes
         * @return the [DataUnit] matching the specified `suffix`
         * @throws IllegalArgumentException if the suffix does not match the suffix of this enum's constants
         */
        fun fromSuffix(suffix: String): DataUnit = values().firstOrNull { it.suffix.equals(suffix, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unknown data unit suffix '$suffix'")
    }
}
