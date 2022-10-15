/*
 * Copyright 2012-2022 the original author or authors.
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

import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * Duration format styles.
 *
 * @author Phillip Webb
 * @author Valentine Wu
 */
enum class DurationStyle(pattern: String) {

    /**
     * Simple formatting, for example '1s'.
     */
    SIMPLE("^([+-]?\\d+)([a-zA-Z]{0,2})$") {
        override fun parse(value: String, unit: ChronoUnit?): Duration =
            try {
                val matcher = requireNotNull(matcher(value)) { "'$value' does not match simple duration pattern" }
                val suffix = matcher.groupValues.getOrNull(2)
                val unit = if (!suffix.isNullOrEmpty()) Unit.fromSuffix(suffix) else Unit.fromChronoUnit(unit)
                unit.parse(matcher.groupValues[1])
            } catch (e: Exception) {
                throw IllegalArgumentException("'$value' is not a valid simple duration", e)
            }

        override fun print(value: Duration, unit: ChronoUnit?): String = Unit.fromChronoUnit(unit).print(value)
    },

    /**
     * ISO-8601 formatting.
     */
    ISO8601("^[+-]?[pP].*$") {
        override fun parse(value: String, unit: ChronoUnit?): Duration =
            try {
                Duration.parse(value)
            } catch (e: Exception) {
                throw IllegalArgumentException("'$value' is not a valid ISO-8601 duration", e)
            }

        override fun print(value: Duration, unit: ChronoUnit?): String = value.toString()
    };

    private val pattern: Regex = pattern.toRegex()

    protected infix fun matches(value: String): Boolean = value matches pattern

    protected infix fun matcher(value: String): MatchResult? = pattern.matchEntire(value)

    /**
     * Parse the given value to a duration (attemps to autodetects the time unit used on [value]).
     *
     * @param value the value to parse
     * @param unit the duration unit to use if the value doesn't specify one (`null` will default to ms)
     * @return a duration
     */
    abstract fun parse(value: String, unit: ChronoUnit? = null): Duration

    /**
     * Print the specified duration using the given unit.
     *
     * @param value the value to print
     * @param unit the value to use for printing (`null` will default to ms)
     * @return the printed result
     */
    abstract fun print(value: Duration, unit: ChronoUnit? = null): String

    /**
     * Units that we support.
     */
    private enum class Unit(
        private val chronoUnit: ChronoUnit,
        private val suffix: String,
        private val longValue: Duration.() -> Long,
    ) {

        /**
         * Nanoseconds.
         */
        NANOS(ChronoUnit.NANOS, "ns", Duration::toNanos),

        /**
         * Microseconds.
         */
        MICROS(ChronoUnit.MICROS, "us", { toNanos() / 1000L }),

        /**
         * Milliseconds.
         */
        MILLIS(ChronoUnit.MILLIS, "ms", Duration::toMillis),

        /**
         * Seconds.
         */
        SECONDS(ChronoUnit.SECONDS, "s", Duration::toSeconds),

        /**
         * Minutes.
         */
        MINUTES(ChronoUnit.MINUTES, "m", Duration::toMinutes),

        /**
         * Hours.
         */
        HOURS(ChronoUnit.HOURS, "h", Duration::toHours),

        /**
         * Days.
         */
        DAYS(ChronoUnit.DAYS, "d", Duration::toDays);

        fun parse(value: String): Duration = Duration.of(value.toLong(), chronoUnit)

        fun print(value: Duration): String = "${longValue(value)}$suffix"

        fun longValue(value: Duration): Long = value.longValue()

        companion object {
            fun fromChronoUnit(chronoUnit: ChronoUnit?): Unit {
                if (chronoUnit == null) return MILLIS

                return values().firstOrNull { it.chronoUnit == chronoUnit }
                    ?: throw IllegalArgumentException("Unknown unit $chronoUnit")
            }

            fun fromSuffix(suffix: String): Unit = values().firstOrNull { it.suffix.equals(suffix, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown unit $suffix")
        }
    }

    companion object {
        /**
         * Detect the style then parse the value to return a duration.
         *
         * @param value the value to parse
         * @param unit the duration unit to use if the value doesn't specify one (`null`
         * will default to ms)
         * @return the parsed duration
         * @throws IllegalArgumentException if the value is not a known style or cannot be
         * parsed
         */
        fun detectAndParse(value: String, unit: ChronoUnit? = null): Duration =
            detect(value).parse(value, unit)

        /**
         * Detect the style from the given source value.
         *
         * @param value the source value
         * @return the duration style
         * @throws IllegalArgumentException if the value is not a known style
         */
        fun detect(value: String): DurationStyle = values().firstOrNull { it matches value }
            ?: throw IllegalArgumentException("'$value' is not a valid duration")
    }
}
