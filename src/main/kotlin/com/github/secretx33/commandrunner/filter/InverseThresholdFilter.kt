package com.github.secretx33.commandrunner.filter

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply

/**
 * Counterpart of [ThresholdFilter][ch.qos.logback.classic.filter.ThresholdFilter].
 *
 * @property level Level what level is the maximum level allowed to be logged
 */
class InverseThresholdFilter : Filter<ILoggingEvent>() {

    private lateinit var level: Level

    override fun decide(event: ILoggingEvent): FilterReply {
        if (!isStarted) {
            return FilterReply.NEUTRAL
        }
        return if (!event.level.isGreaterOrEqual(level)) {
            FilterReply.NEUTRAL
        } else {
            FilterReply.DENY
        }
    }

    fun setLevel(level: String) {
        this.level = Level.toLevel(level)
    }

    override fun start() {
        if (::level.isInitialized) {
            super.start()
        }
    }
}
