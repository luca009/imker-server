package com.luca009.imker.server.management.models

import com.luca009.imker.server.caching.model.WeatherRasterCompositeCache
import com.luca009.imker.server.caching.model.WeatherRasterCompositeCacheConfiguration
import com.luca009.imker.server.configuration.model.WeatherModel
import com.luca009.imker.server.configuration.model.WeatherVariableFileNameMapper
import com.luca009.imker.server.configuration.model.WeatherVariableUnitMapper
import com.luca009.imker.server.management.files.model.LocalFileManagerService
import com.luca009.imker.server.management.models.model.WeatherModelManagerService
import com.luca009.imker.server.parser.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.util.*
import kotlin.collections.ArrayDeque

class WeatherModelManagerServiceImpl(
    private val availableWeatherModels: SortedMap<Int, WeatherModel>,
    private val weatherRasterCompositeCacheFactory: (WeatherRasterCompositeCacheConfiguration, WeatherDataParser, WeatherVariableFileNameMapper, WeatherVariableUnitMapper) -> WeatherRasterCompositeCache,
    fileManagerService: LocalFileManagerService
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
    private val updateGroups: Map<String, WeatherModelUpdateGroup>

    init {
        updateGroups = availableWeatherModels.values.map {
            it.receiver.receiverGroup
        }.toSet().associateWith {
            WeatherModelUpdateGroup(fileManagerService)
        }
    }

    override fun getWeatherModels() = availableWeatherModels

    override suspend fun beginUpdateWeatherModels(
        updateSources: Boolean,
        forceUpdateParsers: Boolean
    ) = coroutineScope {
        // Queue all weather models for updating
        availableWeatherModels.values.forEach {
            queueUpdateWeatherModel(it, updateSources, forceUpdateParsers)
        }

        // Start updating all queues
        updateGroups.values.forEach {
            it.startQueue().collect()
        }
    }

    override fun queueUpdateWeatherModel(
        weatherModel: WeatherModel,
        updateSource: Boolean,
        forceUpdateParser: Boolean
    ) {
        val updateGroup = updateGroups[weatherModel.receiver.receiverGroup]!! // This returning null means that an illegal argument was passed
        val modelCache = weatherModelCaches[weatherModel]!! // Same as above

        updateGroup.queueUpdateJob(weatherModel, modelCache, updateSource, forceUpdateParser)
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

    private fun cleanupDataStorageLocation(weatherModel: WeatherModel, dateTime: ZonedDateTime = ZonedDateTime.now()): Nothing = TODO()

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

class WeatherModelUpdateGroup(
    private val fileManagerService: LocalFileManagerService
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val jobs: ArrayDeque<WeatherModelUpdateJob> = ArrayDeque()
    private var queueRunning: Boolean = false

    fun getJobs(): Iterable<WeatherModelUpdateJob> = jobs.asIterable()

    fun queueUpdateJob(weatherModel: WeatherModel, weatherModelCache: WeatherRasterCompositeCache, updateSource: Boolean, forceUpdateParser: Boolean): WeatherModelUpdateJob {
        val updateJob = WeatherModelUpdateJob(
            weatherModel,
            weatherModelCache,
            updateSource,
            forceUpdateParser,
            ZonedDateTime.now()
        )

        jobs.add(updateJob)
        return updateJob
    }

    suspend fun startQueue(): Flow<WeatherModelUpdateJob> = flow {
        if (queueRunning) {
            return@flow
        }
        queueRunning = true

        var job = jobs.removeFirstOrNull()
        while (job != null) {
            val success = updateWeatherModel(job)

            if (success) {
                emit(job)
            }

            job = jobs.removeFirstOrNull()
        }

        queueRunning = false
    }

    private suspend fun updateWeatherModel(updateJob: WeatherModelUpdateJob): Boolean {
        // Check if we're even supposed to update the weather model files (updateSource = true), or just use the last ones we downloaded (updateSource = false)
        if (updateJob.updateSource) {
            logger.info("Updating source for ${updateJob.weatherModel.name}...")

            try {
                // Try downloading the weather model
                updateOnlineWeatherModel(updateJob)

                // If we succeed, print this info message and carry on
                logger.info("Updated source for ${updateJob.weatherModel.name}")
            } catch (e: Exception) {
                // Error!
                if (!updateJob.forceUpdateParser) {
                    logger.error("Weather model for ${updateJob.weatherModel.name} could not be updated: ${e.message} Skipping in update process.")
                    return false
                }

                // Force update is enabled, don't return
                logger.warn("Weather model for ${updateJob.weatherModel.name} could not be updated: ${e.message} Force-updating parser and cache.")
            }
        } else {
            logger.info("Skipped updating source for ${updateJob.weatherModel.name}")
        }

        val parserSuccess = updateDataParser(updateJob, updateJob.dateTime)
        if (!parserSuccess) {
            logger.error("Parser for ${updateJob.weatherModel.name} could not be updated. Skipping in update process.")
            return false
        }

        updateWeatherModelCache(updateJob)

        val cleanupSuccess = cleanupDataStorageLocation(updateJob, updateJob.dateTime)
        if (!cleanupSuccess) {
            logger.error("Storage location for ${updateJob.weatherModel.name} could not be cleaned")
            return false
        }

        return true
    }

    private suspend fun updateOnlineWeatherModel(updateJob: WeatherModelUpdateJob) {
        if (!updateJob.receiver.updateNecessary(ZonedDateTime.now())) {
            // No update necessary
            return
        }

        updateJob.receiver.downloadData(ZonedDateTime.now()).collect { // TODO: dynamic file names
            if (it == null) {
                logger.warn("Download progress for ${updateJob.weatherModel.name} cannot be determined")
            } else {
                logger.info("Download progress for ${updateJob.weatherModel.name}: $it%")
            }
        }
    }

    private fun updateDataParser(updateJob: WeatherModelUpdateJob, dateTime: ZonedDateTime = ZonedDateTime.now()): Boolean {
        if (updateJob.weatherModel.parser !is DynamicDataParser) {
            return false
        }

        return updateJob.weatherModel.parser.updateParser(dateTime)
    }

    private fun updateWeatherModelCache(updateJob: WeatherModelUpdateJob) {
        updateJob.weatherModelCache.updateCaches()
        logger.info("Updated cache for ${updateJob.weatherModel.name}")
    }

    private fun cleanupDataStorageLocation(updateJob: WeatherModelUpdateJob, dateTime: ZonedDateTime = ZonedDateTime.now()) = fileManagerService.cleanupWeatherDataLocation(updateJob.weatherModel, dateTime)
}

data class WeatherModelUpdateJob(
    val weatherModel: WeatherModel,
    val weatherModelCache: WeatherRasterCompositeCache,
    val updateSource: Boolean,
    val forceUpdateParser: Boolean,
    val dateTime: ZonedDateTime
) {
    val receiver
        get() = weatherModel.receiver
}