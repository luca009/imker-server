package com.luca009.imker.server.management.models

import com.luca009.imker.server.caching.model.WeatherRasterCompositeCache
import com.luca009.imker.server.caching.model.WeatherRasterCompositeCacheConfiguration
import com.luca009.imker.server.configuration.model.WeatherModel
import com.luca009.imker.server.configuration.model.WeatherVariableFileNameMapper
import com.luca009.imker.server.configuration.model.WeatherVariableUnitMapper
import com.luca009.imker.server.management.files.model.LocalFileManagerService
import com.luca009.imker.server.management.models.model.WeatherModelManagerService
import com.luca009.imker.server.parser.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.util.*

class WeatherModelManagerServiceImpl(
    private val availableWeatherModels: SortedMap<Int, WeatherModel>,
    private val weatherRasterCompositeCacheFactory: (WeatherRasterCompositeCacheConfiguration, WeatherDataParser, WeatherVariableFileNameMapper, WeatherVariableUnitMapper) -> WeatherRasterCompositeCache,
    private val fileManagerService: LocalFileManagerService
) : WeatherModelManagerService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val weatherModelCaches: Map<WeatherModel, WeatherRasterCompositeCache> = availableWeatherModels.mapNotNull {
        Pair(
            it.value,
            weatherRasterCompositeCacheFactory(
                it.value.cacheConfiguration,
                it.value.parser,
                it.value.mapper,
                it.value.unitMapper
            )
        )
    }.toMap()

    override fun getWeatherModels() = availableWeatherModels

    override fun updateWeatherModels(updateSources: Boolean, forceUpdateParsers: Boolean, dateTime: ZonedDateTime): Set<WeatherModel> {
        // TODO: Make this concurrent at some point
        // Issue with concurrency would be the fact that we might end up connecting to the same FTP server twice (like with INCA and AROME). This needs to be considered.
        return availableWeatherModels.values.filter {
            updateWeatherModel(it, updateSources, forceUpdateParsers, dateTime)
        }.toSet()
    }

    override fun updateWeatherModel(weatherModel: WeatherModel, updateSource: Boolean, forceUpdateParser: Boolean, dateTime: ZonedDateTime): Boolean {
        val sourceSuccess = if (updateSource) {
            updateOnlineWeatherModel(weatherModel)
        } else {
            null
        }

        when (sourceSuccess) {
            true -> logger.info("Updated source for ${weatherModel.name}.")
            false ->
                if (forceUpdateParser) {
                    logger.warn("Weather model for ${weatherModel.name} could not be updated. Force-updating parser and cache.")
                } else {
                    logger.warn("Weather model for ${weatherModel.name} could not be updated. Skipping in update process.")
                    return false
                }
            null -> logger.info("Skipped updating source for ${weatherModel.name}.")
        }

        val parserSuccess = updateDataParser(weatherModel)
        if (!parserSuccess) {
            logger.warn("Parser for ${weatherModel.name} could not be updated. Skipping in update process.")
            return false
        }

        val cacheSuccess = updateWeatherModelCache(weatherModel)
        if (!cacheSuccess) {
            logger.warn("Cache for ${weatherModel.name} could not be updated. Skipping in update process.")
            return false
        }

        val cleanupSuccess = cleanupDataStorageLocation(weatherModel)
        if (!cleanupSuccess) {
            logger.warn("Storage location for ${weatherModel.name} could not be cleaned.")
            return false
        }

        return true
    }

    private fun updateOnlineWeatherModel(weatherModel: WeatherModel): Boolean {
        if (!weatherModel.receiver.updateNecessary(ZonedDateTime.now())) {
            // No update necessary
            return true
        }

        return weatherModel.receiver.downloadData(ZonedDateTime.now(), null).successful // TODO: dynamic file names
    }

    override fun cleanupDataStorageLocations() = cleanupDataStorageLocationsFromCollection(availableWeatherModels.values)
    override fun cleanupDataStorageLocations(weatherModels: Set<WeatherModel>) = cleanupDataStorageLocationsFromCollection(weatherModels)

    private fun cleanupDataStorageLocationsFromCollection(weatherModels: Collection<WeatherModel>) {
        weatherModels.forEach {
            val success = cleanupDataStorageLocation(it)

            if (success) {
                logger.info("Purged weather model files for ${it.name}")
            } else {
                logger.error("Failed to purge weather model files for ${it.name}")
            }
        }
    }

    private fun cleanupDataStorageLocation(weatherModel: WeatherModel, dateTime: ZonedDateTime = ZonedDateTime.now()) = fileManagerService.cleanupWeatherDataLocation(weatherModel, dateTime)

    private fun updateWeatherModelCache(weatherModel: WeatherModel): Boolean {
        val cache = weatherModelCaches[weatherModel]
        requireNotNull(cache) {
            logger.error("Could not update cache ${weatherModel.name}. Did not find cache.")
            return false
        }

        cache.updateCaches()
        logger.info("Updated cache for ${weatherModel.name}")
        return true
    }

    private fun updateDataParser(weatherModel: WeatherModel, dateTime: ZonedDateTime = ZonedDateTime.now()): Boolean {
        if (weatherModel.parser !is DynamicDataParser) {
            return false
        }

        return weatherModel.parser.updateParser(dateTime)
    }

    override fun getAvailableWeatherModelsForLatLon(
        variable: WeatherVariableType,
        latitude: Double,
        longitude: Double
    ): SortedMap<Int, WeatherModel> {
        return availableWeatherModels
            .filter {
                val weatherVariableName = it.value.mapper.getWeatherVariableName(variable)
                requireNotNull(weatherVariableName) {
                    false
                }

                it.value.parser.containsLatLon(weatherVariableName, latitude, longitude)
            }
            .toSortedMap()
    }

    override fun getAvailableWeatherModelsForLatLon(variable: WeatherVariableType, latitude: Double, longitude: Double, time: ZonedDateTime): SortedMap<Int, WeatherModel> {
        return availableWeatherModels
            .filter {
                val cache = weatherModelCaches[it.value]
                requireNotNull(cache) {
                    // We don't have the right cache :(
                    false
                }

                if (!cache.containsTime(variable, time)) {
                    // Cache does not contain specified time
                    return@filter false
                }

                val coordinates = cache.latLonToCoordinates(variable, latitude, longitude)
                requireNotNull(coordinates) {
                    // Coordinates could not be determined
                    false
                }

                val timeIndex = cache.getEarliestTimeIndex(variable, time)
                requireNotNull(timeIndex) {
                    // Time index could not be determined
                    false
                }

                cache.variableExistsAtTimeAndPosition(variable, timeIndex, coordinates)
            }
            .toSortedMap()
    }

    override fun getPreferredWeatherModelForLatLon(
        variable: WeatherVariableType,
        latitude: Double,
        longitude: Double
    ): WeatherModel? {
        val filteredMap = getAvailableWeatherModelsForLatLon(variable, latitude, longitude)
        if (filteredMap.isEmpty()) {
            return null
        }

        return availableWeatherModels[filteredMap.firstKey()]
    }

    override fun getPreferredWeatherModelForLatLon(variable: WeatherVariableType, latitude: Double, longitude: Double, time: ZonedDateTime): WeatherModel? {
        val filteredMap = getAvailableWeatherModelsForLatLon(variable, latitude, longitude, time)
        if (filteredMap.isEmpty()) {
            return null
        }

        return availableWeatherModels[filteredMap.firstKey()]
    }

    override fun getCompositeCacheForWeatherModel(weatherModel: WeatherModel): WeatherRasterCompositeCache? = weatherModelCaches[weatherModel]
}