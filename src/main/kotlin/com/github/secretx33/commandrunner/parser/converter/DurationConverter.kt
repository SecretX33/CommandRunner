package com.github.secretx33.commandrunner.parser.converter

import com.github.secretx33.commandrunner.util.unit.DurationStyle
import joptsimple.ValueConversionException
import joptsimple.ValueConverter
import java.time.Duration
import java.time.temporal.ChronoUnit

object DurationConverter : ValueConverter<Duration> {

    override fun convert(value: String): Duration =
        try {
            DurationStyle.detectAndParse(value, defaultUnit = ChronoUnit.MILLIS).also {
                require(!it.isNegative) { "Invalid duration: '$value'. Must be at least 0." }
            }
        } catch (e: IllegalArgumentException) {
            // Map conversion exception to JOpt type
            throw ValueConversionException(e.message)
        }

    override fun valueType(): Class<out Duration> = Duration::class.java

    override fun valuePattern(): String = "number [timeunit]"
}
