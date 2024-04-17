package com.luca009.imker.server.queries

import java.time.Duration
import java.time.ZonedDateTime

object TimeQueryHelper {
    fun Collection<ZonedDateTime>.getEarliest(time: ZonedDateTime): ZonedDateTime? {
        return this.lastOrNull {
            it.isBefore(time) || it.isEqual(time)
        }
    }

    fun Collection<ZonedDateTime>.getClosest(time: ZonedDateTime): ZonedDateTime {
        return this.minBy {
            Duration.between(it, time).abs()
        }
    }

    fun Collection<ZonedDateTime>.getLatest(time: ZonedDateTime): ZonedDateTime? {
        return this.firstOrNull {
            it.isAfter(time) || it.isEqual(time)
        }
    }
}