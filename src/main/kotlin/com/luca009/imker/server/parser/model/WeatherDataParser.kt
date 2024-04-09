package com.luca009.imker.server.parser.model

import java.nio.file.Path
import java.time.ZonedDateTime

interface WeatherDataParser {
    fun getDataSources(): Set<Path>
    fun getAvailableVariableTypes(): Set<WeatherVariableType>
    fun getAvailableVariables(): Set<WeatherVariable>
    fun getVariable(variableType: WeatherVariableType): WeatherVariable?
    fun getGridEntireSlice(variable: WeatherVariableType): WeatherVariableSlice?
    fun getGridTimeSlice(variable: WeatherVariableType, timeIndex: Int = 0): WeatherVariableRasterSlice?
    fun getGridTimeAnd2dPositionSlice(variable: WeatherVariableType, timeIndex: Int = 0, coordinate: WeatherVariable2dCoordinate): Any?
    fun getGridTimeAnd3dPositionSlice(variable: WeatherVariableType, timeIndex: Int = 0, coordinate: WeatherVariable3dCoordinate): Any?
    fun getTimes(variable: WeatherVariableType): Set<Pair<Int, ZonedDateTime>>?

    fun gridTimeSliceExists(variable: WeatherVariableType, timeIndex: Int): Boolean
    fun gridTimeAnd2dPositionSliceExists(variable: WeatherVariableType, timeIndex: Int, coordinate: WeatherVariable2dCoordinate): Boolean
    fun gridTimeAnd3dPositionSliceExists(variable: WeatherVariableType, timeIndex: Int, coordinate: WeatherVariable3dCoordinate): Boolean

    fun containsTime(variable: WeatherVariableType, time: ZonedDateTime): Boolean
    fun containsLatLon(variable: WeatherVariableType, latitude: Double, longitude: Double): Boolean
    fun latLonToCoordinates(variable: WeatherVariableType, latitude: Double, longitude: Double): WeatherVariable2dCoordinate?

    fun close()
}

/**
 * Wrapper for any WeatherDataParser that allows dynamically changing the underlying parser on the fly.
 */
interface DynamicDataParser : WeatherDataParser {
    fun updateParser(dateTime: ZonedDateTime): Boolean
}

interface NetCdfParser : WeatherDataParser {
    fun getAvailableRawVariables(): Set<RawWeatherVariable>
}
