package com.luca009.imker.server.parser.model

import java.nio.file.Path
import java.time.ZonedDateTime

interface WeatherDataParser {
    fun getDataSources(): Map<Path, ZonedDateTime>
    fun getAvailableVariableTypes(): Set<WeatherVariableType>
    fun getAvailableVariables(): Set<WeatherVariable>
    fun getVariable(variableType: WeatherVariableType): WeatherVariable?
    fun getGridEntireSlice(variable: WeatherVariableType): WeatherVariableTimeRasterSlice?
    fun getGridRasterSlice(variable: WeatherVariableType, time: ZonedDateTime): WeatherVariableRasterSlice?
    fun getGridTimeSeriesAt2dPosition(variable: WeatherVariableType, coordinate: WeatherVariable2dCoordinate, timeLimit: Int): WeatherVariableTimeSlice?
    fun getGridTimeSeriesAt3dPosition(variable: WeatherVariableType, coordinate: WeatherVariable3dCoordinate, timeLimit: Int): WeatherVariableTimeSlice?
    fun getGridTimeAnd2dPositionSlice(variable: WeatherVariableType, time: ZonedDateTime, coordinate: WeatherVariable2dCoordinate): Any?
    fun getGridTimeAnd3dPositionSlice(variable: WeatherVariableType, time: ZonedDateTime, coordinate: WeatherVariable3dCoordinate): Any?
    fun getTimes(variable: WeatherVariableType): List<ZonedDateTime>?

    fun gridTimeSliceExists(variable: WeatherVariableType, time: ZonedDateTime): Boolean
    fun gridTimeAnd2dPositionSliceExists(variable: WeatherVariableType, time: ZonedDateTime, coordinate: WeatherVariable2dCoordinate): Boolean
    fun gridTimeAnd3dPositionSliceExists(variable: WeatherVariableType, time: ZonedDateTime, coordinate: WeatherVariable3dCoordinate): Boolean

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
