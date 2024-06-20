package com.luca009.imker.server.parser.model

import java.nio.file.Path
import java.time.ZonedDateTime

/**
 * Interface for a parser of weather data files.
 */
interface WeatherDataParser {
    /**
     * Gets the data sources which the data is being read from.
     */
    fun getDataSources(): Map<Path, ZonedDateTime>

    /**
     * Gets all [WeatherVariableType]s that are available in the parsed dataset.
     */
    fun getAvailableVariableTypes(): Set<WeatherVariableType>

    /**
     * Gets all [WeatherVariable]s that are available in the parsed dataset.
     */
    fun getAvailableVariables(): Set<WeatherVariable>

    /**
     * Gets the [WeatherVariable] associated with the specified [variableType].
     */
    fun getVariable(variableType: WeatherVariableType): WeatherVariable?

    /**
     * Gets the entire grid belonging to the specified [variable] - in all dimensions.
     */
    fun getGridEntireSlice(variable: WeatherVariableType): WeatherVariableTimeRasterSlice?

    /**
     * Gets the entire grid of [variable] at the specified [time] (has to be exact).
     */
    fun getGridRasterSlice(variable: WeatherVariableType, time: ZonedDateTime): WeatherVariableRasterSlice?

    /**
     * Gets a series of values of [variable] at a specified 2d [coordinate] throughout time (with a maximum [timeLimit]).
     */
    fun getGridTimeSeriesAt2dPosition(variable: WeatherVariableType, coordinate: WeatherVariable2dCoordinate, timeLimit: Int): WeatherVariableTimeSlice?

    /**
     * Gets a series of values of [variable] at a specified 3d [coordinate] throughout time (with a maximum [timeLimit]).
     */
    fun getGridTimeSeriesAt3dPosition(variable: WeatherVariableType, coordinate: WeatherVariable3dCoordinate, timeLimit: Int): WeatherVariableTimeSlice?

    /**
     * Gets the value of [variable] at the specified [time] (has to be exact) and 2d [coordinate].
     */
    fun getGridTimeAnd2dPositionSlice(variable: WeatherVariableType, time: ZonedDateTime, coordinate: WeatherVariable2dCoordinate): Any?

    /**
     * Gets the value of [variable] at the specified [time] (has to be exact) and 3d [coordinate].
     */
    fun getGridTimeAnd3dPositionSlice(variable: WeatherVariableType, time: ZonedDateTime, coordinate: WeatherVariable3dCoordinate): Any?

    /**
     * Gets all defined timestamps for [variable].
     */
    fun getTimes(variable: WeatherVariableType): List<ZonedDateTime>?

    /**
     * Gets whether the slice of [variable] at the specified [time] exists.
     */
    fun gridTimeSliceExists(variable: WeatherVariableType, time: ZonedDateTime): Boolean

    /**
     * Gets whether the value of [variable] at the specified [time] and 2d [coordinate] exists.
     */
    fun gridTimeAnd2dPositionSliceExists(variable: WeatherVariableType, time: ZonedDateTime, coordinate: WeatherVariable2dCoordinate): Boolean

    /**
     * Gets whether the value of [variable] at the specified [time] and 3d [coordinate] exists.
     */
    fun gridTimeAnd3dPositionSliceExists(variable: WeatherVariableType, time: ZonedDateTime, coordinate: WeatherVariable3dCoordinate): Boolean

    /**
     * Gets whether the specified [variable] (optional, null for any) contains the specified [longitude] and [latitude].
     */
    fun containsLatLon(latitude: Double, longitude: Double, variable: WeatherVariableType? = null): Boolean

    /**
     * Gets the closest [WeatherVariable2dCoordinate] of [variable] to the specified [latitude] and [longitude].
     */
    fun latLonToCoordinates(variable: WeatherVariableType, latitude: Double, longitude: Double): WeatherVariable2dCoordinate?

    /**
     * Closes the parser and releases any reserved resources.
     */
    fun close()
}

/**
 * Wrapper for any [WeatherDataParser] that allows dynamically changing the underlying parser on the fly.
 */
interface DynamicDataParser : WeatherDataParser {
    /**
     * Updates the parser with data from the specified [dateTime] (or closest earliest alternative).
     */
    fun updateParser(dateTime: ZonedDateTime): Boolean
}

interface NetCdfParser : WeatherDataParser {
    fun getAvailableRawVariables(): Set<RawWeatherVariable>
}
