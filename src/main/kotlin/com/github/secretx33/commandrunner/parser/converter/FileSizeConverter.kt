package com.github.secretx33.commandrunner.parser.converter

import com.github.secretx33.commandrunner.util.unit.DataSize
import com.github.secretx33.commandrunner.util.unit.DataUnit
import joptsimple.ValueConversionException
import joptsimple.ValueConverter

object FileSizeConverter : ValueConverter<DataSize> {

    override fun convert(value: String): DataSize =
        try {
            DataSize.parse(value, defaultUnit = DataUnit.KILOBYTES)
        } catch (e: IllegalArgumentException) {
            // Map conversion exception to JOpt type
            throw ValueConversionException(e.message)
        }

    override fun valueType(): Class<out DataSize> = DataSize::class.java

    override fun valuePattern(): String = "filesize [unit]"
}
