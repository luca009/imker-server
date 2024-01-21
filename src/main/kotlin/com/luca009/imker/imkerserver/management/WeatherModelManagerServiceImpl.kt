package com.luca009.imker.imkerserver.management

import com.luca009.imker.imkerserver.caching.model.WeatherRasterCompositeCache
import com.luca009.imker.imkerserver.caching.model.WeatherRasterCompositeCacheConfiguration
import com.luca009.imker.imkerserver.configuration.model.WeatherModel
import com.luca009.imker.imkerserver.configuration.model.WeatherVariableFileNameMapper
import com.luca009.imker.imkerserver.management.model.WeatherModelManagerService
import com.luca009.imker.imkerserver.parser.model.DynamicDataParser
import com.luca009.imker.imkerserver.parser.model.WeatherDataParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.util.*

class WeatherModelManagerServiceImpl(
    private val availableWeatherModels: SortedMap<Int, WeatherModel>,
    private val weatherRasterCompositeCacheFactory: (WeatherRasterCompositeCacheConfiguration, WeatherDataParser, WeatherVariableFileNameMapper) -> WeatherRasterCompositeCache
) : WeatherModelManagerService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val weatherModelCaches: Map<WeatherModel, WeatherRasterCompositeCache> = availableWeatherModels.mapNotNull {
        Pair(
            it.value,
            weatherRasterCompositeCacheFactory(
                it.value.cacheConfiguration,
                it.value.parser,
                it.value.mapper
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

    override fun getAvailableWeatherModelsForLatLon(variableName: String, latitude: Double, longitude: Double): SortedMap<Int, WeatherModel> {
        return availableWeatherModels
            .filter { it.value.parser.containsLatLon(variableName, latitude, longitude) }
            .toSortedMap()
    }

    override fun getPreferredWeatherModelForLatLon(variableName: String, latitude: Double, longitude: Double): WeatherModel? {
        val filteredMap = availableWeatherModels
            .filter { it.value.parser.containsLatLon(variableName, latitude, longitude) }
            .toSortedMap()

        if (filteredMap.isEmpty()) {
            return null
        }

        return availableWeatherModels[filteredMap.firstKey()]
    }

    override fun getCompositeCacheForWeatherModel(weatherModel: WeatherModel): WeatherRasterCompositeCache? = weatherModelCaches[weatherModel]
}