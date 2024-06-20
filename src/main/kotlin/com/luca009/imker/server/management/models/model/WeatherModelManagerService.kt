package com.luca009.imker.server.management.models.model

import com.luca009.imker.server.caching.model.WeatherRasterCompositeCache
import com.luca009.imker.server.configuration.model.WeatherModel
import com.luca009.imker.server.management.models.WeatherModelUpdateJobEnabled
import com.luca009.imker.server.parser.model.WeatherVariableType
import java.time.ZonedDateTime
import java.util.SortedMap

/**
 * Interface for a service managing various different weather models and support for queueing tasks regarding them.
 */
interface WeatherModelManagerService {
    /**
     * Get all managed weather models.
     */
    fun getWeatherModels(): SortedMap<Int, WeatherModel>

    /**
     * Queue a new update job for a specific [weatherModel].
     */
    fun queueUpdateWeatherModel(
        weatherModel: WeatherModel,
        updateSource: WeatherModelUpdateJobEnabled = WeatherModelUpdateJobEnabled.Enabled,
        updateParser: WeatherModelUpdateJobEnabled = WeatherModelUpdateJobEnabled.Enabled,
        updateCache: WeatherModelUpdateJobEnabled = WeatherModelUpdateJobEnabled.Enabled,
        cleanupStorage: WeatherModelUpdateJobEnabled = WeatherModelUpdateJobEnabled.Forced
    )

    /**
     * Queue a new update job for all weather models and start the queue.
     */
    suspend fun beginUpdateWeatherModels(
        updateSource: WeatherModelUpdateJobEnabled = WeatherModelUpdateJobEnabled.Enabled,
        updateParser: WeatherModelUpdateJobEnabled = WeatherModelUpdateJobEnabled.Enabled,
        updateCache: WeatherModelUpdateJobEnabled = WeatherModelUpdateJobEnabled.Enabled,
        cleanupStorage: WeatherModelUpdateJobEnabled = WeatherModelUpdateJobEnabled.Forced
    )

    /**
     * Queue a new cleanup job for all weather models.
     */
    fun queueCleanupDataStorageLocations()

    /**
     * Queue a new cleanup job for the specified [weatherModels].
     */
    fun queueCleanupDataStorageLocations(weatherModels: Set<WeatherModel>)

    /**
     * Get the available weather models at the specified [latitude] and [longitude], optionally of the specified [variable] and/or at the specified [time].
     */
    fun getAvailableWeatherModelsForLatLon(latitude: Double, longitude: Double, variable: WeatherVariableType? = null, time: ZonedDateTime? = null): SortedMap<Int, WeatherModel>

    /**
     * Get the preferred weather model for the specified [variable], at the specified [latitude] and [longitude].
     */
    fun getPreferredWeatherModelForLatLon(variable: WeatherVariableType, latitude: Double, longitude: Double): WeatherModel?

    /**
     * Get the preferred weather model for the specified [variable], at the specified [latitude], [longitude] and [time].
     */
    fun getPreferredWeatherModelForLatLon(variable: WeatherVariableType, latitude: Double, longitude: Double, time: ZonedDateTime): WeatherModel?

    /**
     * Get the [WeatherRasterCompositeCache] associated with the specified [weatherModel].
     */
    fun getCompositeCacheForWeatherModel(weatherModel: WeatherModel): WeatherRasterCompositeCache?
}