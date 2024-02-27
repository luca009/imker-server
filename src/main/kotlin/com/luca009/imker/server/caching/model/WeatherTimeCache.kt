package com.luca009.imker.server.caching.model

import com.luca009.imker.server.parser.model.WeatherVariableType
import java.time.ZonedDateTime

interface WeatherTimeCache {
    fun getEarliestIndex(weatherVariable: WeatherVariableType, time: ZonedDateTime): Int?
    fun getClosestIndex(weatherVariable: WeatherVariableType, time: ZonedDateTime): Int?
    fun getLatestIndex(weatherVariable: WeatherVariableType, time: ZonedDateTime): Int?
    fun getEarliestTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): ZonedDateTime?
    fun getClosestTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): ZonedDateTime?
    fun getLatestTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): ZonedDateTime?
    fun getTime(weatherVariable: WeatherVariableType, index: Int): ZonedDateTime?

    fun containsTime(weatherVariable: WeatherVariableType, time: ZonedDateTime): Boolean
    fun containsTimeIndex(weatherVariable: WeatherVariableType, index: Int): Boolean

    fun setTimes(weatherVariable: WeatherVariableType, times: Set<Pair<Int, ZonedDateTime>>)
}