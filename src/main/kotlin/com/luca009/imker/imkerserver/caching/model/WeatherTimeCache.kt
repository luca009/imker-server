package com.luca009.imker.imkerserver.caching.model

import com.luca009.imker.imkerserver.parser.model.WeatherVariableType
import java.time.ZonedDateTime

interface WeatherTimeCache {
    fun getEarliestIndex(weatherVariableName: String, time: ZonedDateTime): Int?
    fun getClosestIndex(weatherVariableName: String, time: ZonedDateTime): Int?
    fun getLatestIndex(weatherVariableName: String, time: ZonedDateTime): Int?
    fun getEarliestTime(weatherVariableName: String, time: ZonedDateTime): ZonedDateTime?
    fun getClosestTime(weatherVariableName: String, time: ZonedDateTime): ZonedDateTime?
    fun getLatestTime(weatherVariableName: String, time: ZonedDateTime): ZonedDateTime?
    fun getTime(weatherVariableName: String, index: Int): ZonedDateTime?

    fun setTimes(weatherVariableName: String, times: Set<Pair<Int, ZonedDateTime>>)
}