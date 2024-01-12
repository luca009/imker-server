package com.luca009.imker.imkerserver.parser.model

import java.time.ZonedDateTime

interface WeatherDataParser {
    fun getDataSources(): Set<String>
    fun getAvailableRawVariables(): Set<RawWeatherVariable>
    fun getRawVariable(name: String): RawWeatherVariable?
    fun getGridEntireSlice(name: String): List<Array<*>>?
    fun getGridTimeSlice(name: String, timeIndex: Int = 0): Array<*>?
    fun getGridTimeAnd2dPositionSlice(name: String, timeIndex: Int = 0, coordinate: WeatherVariable2dCoordinate): Any?
    fun getGridTimeAnd3dPositionSlice(name: String, timeIndex: Int = 0, coordinate: WeatherVariable3dCoordinate): Any?

    fun gridTimeSliceExists(name: String, timeIndex: Int): Boolean
    fun gridTimeAnd2dPositionSliceExists(name: String, timeIndex: Int, coordinate: WeatherVariable2dCoordinate): Boolean
    fun gridTimeAnd3dPositionSliceExists(name: String, timeIndex: Int, coordinate: WeatherVariable3dCoordinate): Boolean

    fun containsLatLon(name: String, latitude: Double, longitude: Double): Boolean
    fun latLonToCoordinates(name: String, latitude: Double, longitude: Double): WeatherVariable2dCoordinate?
}

/**
 * Wrapper for any WeatherDataParser that also implements updating sources (like downloading new files).
 */
interface DynamicDataParser : WeatherDataParser {
    /**
     * Update all necessary sources. Returns whether the operation was a success.
     */
    suspend fun updateSources(dateTime: ZonedDateTime): Boolean
}

interface NetCdfParser : WeatherDataParser
