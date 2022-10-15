package com.github.secretx33.commandrunner.parser.converter

import joptsimple.ValueConversionException
import joptsimple.ValueConverter
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.io.path.Path

object PathConverter : ValueConverter<Path> {

    override fun convert(value: String): Path =
        try {
            Path(value)
        } catch (e: InvalidPathException) {
            // Map conversion exception to JOpt type
            throw ValueConversionException("Provided path is invalid. Reason: ${e.message}")
        }

    override fun valueType(): Class<out Path> = Path::class.java

    override fun valuePattern(): String = Path("folder").resolve("anotherFolder").toString()
}
