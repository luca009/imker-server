package com.luca009.imker.imkerserver.caching

import com.luca009.imker.imkerserver.caching.model.WeatherTimeCache
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.ZonedDateTime
import java.util.SortedSet

@Component
class WeatherTimeCacheImpl : WeatherTimeCache {
    private val times: MutableMap<String, SortedSet<Pair<Int, ZonedDateTime>>> = mutableMapOf()

    override fun getEarliestIndex(weatherVariableName: String, time: ZonedDateTime): Int? {
        return getEarliestTimeIndexPair(weatherVariableName, time)?.first
    }

    override fun getClosestIndex(weatherVariableName: String, time: ZonedDateTime): Int? {
        return getClosestTimeIndexPair(weatherVariableName, time)?.first
    }

    override fun getLatestIndex(weatherVariableName: String, time: ZonedDateTime): Int? {
        return getLatestTimeIndexPair(weatherVariableName, time)?.first
    }

    override fun getEarliestTime(weatherVariableName: String, time: ZonedDateTime): ZonedDateTime? {
        return getEarliestTimeIndexPair(weatherVariableName, time)?.second
    }

    override fun getClosestTime(weatherVariableName: String, time: ZonedDateTime): ZonedDateTime? {
        return getClosestTimeIndexPair(weatherVariableName, time)?.second
    }

    override fun getLatestTime(weatherVariableName: String, time: ZonedDateTime): ZonedDateTime? {
        return getLatestTimeIndexPair(weatherVariableName, time)?.second
    }

    fun getEarliestTimeIndexPair(weatherVariableName: String, time: ZonedDateTime): Pair<Int, ZonedDateTime>? {
        val variableTimes = times[weatherVariableName]
        requireNotNull(variableTimes) {
            return null
        }

        val earlierTimes = variableTimes.filter {
            it.second.isBefore(time) || it.second.isEqual(time)
        }

        return earlierTimes.lastOrNull()
    }

    fun getClosestTimeIndexPair(weatherVariableName: String, time: ZonedDateTime): Pair<Int, ZonedDateTime>? {
        val variableTimes = times[weatherVariableName]
        requireNotNull(variableTimes) {
            return null
        }

        return variableTimes.minBy { Duration.between(it.second, time).abs() }
    }

    fun getLatestTimeIndexPair(weatherVariableName: String, time: ZonedDateTime): Pair<Int, ZonedDateTime>? {
        val variableTimes = times[weatherVariableName]
        requireNotNull(variableTimes) {
            return null
        }

        val laterTimes = variableTimes.filter {
            it.second.isAfter(time) || it.second.isEqual(time)
        }

        return laterTimes.firstOrNull()
    }

    override fun getTime(weatherVariableName: String, index: Int): ZonedDateTime? {
        val variableTimes = times[weatherVariableName]
        requireNotNull(variableTimes) {
            return null
        }

        return variableTimes.firstOrNull { it.first == index }?.second
    }

    override fun containsTime(weatherVariableName: String, time: ZonedDateTime): Boolean {
        val variableTimes = times[weatherVariableName]
        requireNotNull(variableTimes) {
            return false
        }

        val firstTime = variableTimes.firstOrNull()?.second ?: return false
        val lastTime = variableTimes.lastOrNull()?.second ?: return false

        return !time.isBefore(firstTime) && !time.isAfter(lastTime)
    }

    override fun containsTimeIndex(weatherVariableName: String, index: Int): Boolean {
        val variableTimes = times[weatherVariableName]
        requireNotNull(variableTimes) {
            return false
        }

        val firstIndex = variableTimes.firstOrNull()?.first ?: return false
        val lastIndex = variableTimes.lastOrNull()?.first ?: return false

        return index in firstIndex..lastIndex
    }

    override fun setTimes(weatherVariableName: String, times: Set<Pair<Int, ZonedDateTime>>) {
        this.times[weatherVariableName] = times.toSortedSet(compareBy { it.second })
    }
}