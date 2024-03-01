package com.luca009.imker.server.management.models

import com.luca009.imker.server.caching.model.WeatherRasterCompositeCache
import com.luca009.imker.server.caching.model.WeatherRasterCompositeCacheConfiguration
import com.luca009.imker.server.configuration.model.WeatherModel
import com.luca009.imker.server.configuration.model.WeatherVariableFileNameMapper
import com.luca009.imker.server.configuration.model.WeatherVariableUnitMapper
import com.luca009.imker.server.management.models.model.WeatherModelManagerService
import com.luca009.imker.server.parser.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.util.*

class WeatherModelManagerServiceImpl(
    private val availableWeatherModels: SortedMap<Int, WeatherModel>,
    private val weatherRasterCompositeCacheFactory: (WeatherRasterCompositeCacheConfiguration, WeatherDataParser, WeatherVariableFileNameMapper, WeatherVariableUnitMapper) -> WeatherRasterCompositeCache
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

    override fun updateWeatherModelCaches() {
        weatherModelCaches.forEach {
            it.value.updateCaches()
            logger.info("Updated cache for ${it.key.name}")
        }
    }

    override fun updateWeatherModelCaches(weatherModels: Set<WeatherModel>) {
        weatherModels.forEach {
            val cache = weatherModelCaches[it]
            requireNotNull(cache) {
                logger.error("Could not update cache ${it.name}. Did not find cache.")
                return
            }

            cache.updateCaches()
            logger.info("Updated cache for ${it.name}")
        }
    }

    override fun updateDataParsers() {
        val dateTime = ZonedDateTime.now()

        weatherModelCaches.forEach {
            updateDataParser(it.key, dateTime)
        }
    }

    override fun updateDataParsers(weatherModels: Set<WeatherModel>) {
        val dateTime = ZonedDateTime.now()

        weatherModels
            .filter { weatherModelCaches.containsKey(it) }
            .forEach {
                updateDataParser(it, dateTime)
        }
    }

    private fun updateDataParser(weatherModel: WeatherModel, dateTime: ZonedDateTime) {
        if (weatherModel.parser !is DynamicDataParser) {
            logger.info("Parser for ${weatherModel.name} was not a DynamicDataParser. Skipping in update process.")
            return
        }

        weatherModel.parser.updateParser(dateTime)
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