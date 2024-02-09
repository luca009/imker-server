package com.luca009.imker.imkerserver.caching

import com.luca009.imker.imkerserver.caching.model.WeatherTimeCache
import com.luca009.imker.imkerserver.parser.model.WeatherVariable
import com.luca009.imker.imkerserver.parser.model.WeatherVariableType
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.ZonedDateTime
import java.util.SortedSet

@Component
class WeatherTimeCacheImpl : WeatherTimeCache {
    private val times: MutableMap<WeatherVariableType, SortedSet<Pair<Int, ZonedDateTime>>> = mutableMapOf()

    override fun getEarliestIndex(weatherVariable: WeatherVariableType, time: ZonedDateTime): Int? {
        return getEarliestTimeIndexPair(weatherVariable, time)?.first
    }

    override fun getClosestIndex(weatherVariable: WeatherVariableType, time: ZonedDateTime): Int? {
        return getClosestTimeIndexPair(weatherVariable, time)?.first
    }

    override fun getLatestIndex(weatherVariable: WeatherVariableType, time: ZonedDateTime): Int? {
        return getLatestTimeIndexPair(weatherVariable, time)?.first
    }

    override fun getEarliestTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): ZonedDateTime? {
        return getEarliestTimeIndexPair(weatherVariable, time)?.second
    }

    override fun getClosestTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): ZonedDateTime? {
        return getClosestTimeIndexPair(weatherVariable, time)?.second
    }

    override fun getLatestTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): ZonedDateTime? {
        return getLatestTimeIndexPair(weatherVariable, time)?.second
    }

    fun getEarliestTimeIndexPair(weatherVariable: WeatherVariableType, time: ZonedDateTime): Pair<Int, ZonedDateTime>? {
        val variableTimes = times[weatherVariable]
        requireNotNull(variableTimes) {
            return null
        }

        val earlierTimes = variableTimes.filter {
            it.second.isBefore(time) || it.second.isEqual(time)
        }

        return earlierTimes.lastOrNull()
    }

    fun getClosestTimeIndexPair(weatherVariable: WeatherVariableType, time: ZonedDateTime): Pair<Int, ZonedDateTime>? {
        val variableTimes = times[weatherVariable]
        requireNotNull(variableTimes) {
            return null
        }

        return variableTimes.minBy { Duration.between(it.second, time).abs() }
    }

    fun getLatestTimeIndexPair(weatherVariable: WeatherVariableType, time: ZonedDateTime): Pair<Int, ZonedDateTime>? {
        val variableTimes = times[weatherVariable]
        requireNotNull(variableTimes) {
            return null
        }

        val laterTimes = variableTimes.filter {
            it.second.isAfter(time) || it.second.isEqual(time)
        }

        return laterTimes.firstOrNull()
    }

    override fun getTime(weatherVariable: WeatherVariableType, index: Int): ZonedDateTime? {
        val variableTimes = times[weatherVariable]
        requireNotNull(variableTimes) {
            return null
        }

        return variableTimes.firstOrNull { it.first == index }?.second
    }

    override fun containsTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): Boolean {
        val variableTimes = times[weatherVariable]
        requireNotNull(variableTimes) {
            return false
        }

        val firstTime = variableTimes.firstOrNull()?.second ?: return false
        val lastTime = variableTimes.lastOrNull()?.second ?: return false

        return !time.isBefore(firstTime) && !time.isAfter(lastTime)
    }

    override fun containsTimeIndex(weatherVariable: WeatherVariableType, index: Int): Boolean {
        val variableTimes = times[weatherVariable]
        requireNotNull(variableTimes) {
            return false
        }

        val firstIndex = variableTimes.firstOrNull()?.first ?: return false
        val lastIndex = variableTimes.lastOrNull()?.first ?: return false

        return index in firstIndex..lastIndex
    }

    override fun setTimes(weatherVariable: WeatherVariableType, times: Set<Pair<Int, ZonedDateTime>>) {
        this.times[weatherVariable] = times.toSortedSet(compareBy { it.second })
    }
}