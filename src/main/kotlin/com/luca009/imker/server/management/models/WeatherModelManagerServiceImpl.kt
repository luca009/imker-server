package com.luca009.imker.server.management.models

import com.luca009.imker.server.caching.model.WeatherRasterCompositeCache
import com.luca009.imker.server.caching.model.WeatherRasterCompositeCacheConfiguration
import com.luca009.imker.server.caching.model.WeatherRasterTimeSnappingType
import com.luca009.imker.server.configuration.model.WeatherModel
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
    private val weatherRasterCompositeCacheFactory: (WeatherRasterCompositeCacheConfiguration, WeatherDataParser) -> WeatherRasterCompositeCache,
    fileManagerService: LocalFileManagerService
) : WeatherModelManagerService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val weatherModelCaches: Map<WeatherModel, WeatherRasterCompositeCache> = availableWeatherModels.mapNotNull {
        Pair(
            it.value,
            weatherRasterCompositeCacheFactory(
                it.value.cacheConfiguration,
                it.value.parser
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
        updateSource: WeatherModelUpdateJobEnabled,
        updateParser: WeatherModelUpdateJobEnabled,
        updateCache: WeatherModelUpdateJobEnabled,
        cleanupStorage: WeatherModelUpdateJobEnabled
    ) = coroutineScope {
        // Queue all weather models for updating
        availableWeatherModels.values.forEach {
            queueUpdateWeatherModel(it, updateSource, updateParser, updateCache, cleanupStorage)
        }

        // Start updating all queues
        updateGroups.values.forEach {
            it.startQueue().collect()
        }
    }

    override fun queueUpdateWeatherModel(
        weatherModel: WeatherModel,
        updateSource: WeatherModelUpdateJobEnabled,
        updateParser: WeatherModelUpdateJobEnabled,
        updateCache: WeatherModelUpdateJobEnabled,
        cleanupStorage: WeatherModelUpdateJobEnabled
    ) {
        val updateGroup = updateGroups[weatherModel.receiver.receiverGroup]!! // This returning null means that an illegal argument was passed
        val modelCache = weatherModelCaches[weatherModel]!! // Same as above

        updateGroup.queueUpdateJob(weatherModel, modelCache, updateSource, updateParser, updateCache, cleanupStorage)
    }

    override fun queueCleanupDataStorageLocations() = availableWeatherModels.values.forEach { queueCleanupDataStorageLocation(it) }
    override fun queueCleanupDataStorageLocations(weatherModels: Set<WeatherModel>) = weatherModels.forEach { queueCleanupDataStorageLocation(it) }

    private fun queueCleanupDataStorageLocation(weatherModel: WeatherModel, dateTime: ZonedDateTime = ZonedDateTime.now()) {
        val updateGroup = updateGroups[weatherModel.receiver.receiverGroup]!! // This returning null means that an illegal argument was passed
        val modelCache = weatherModelCaches[weatherModel]!! // Same as above

        updateGroup.queueJob(weatherModel, modelCache, WeatherModelUpdateJobType.Cleanup)
    }

    override fun getAvailableWeatherModelsForLatLon(
        variable: WeatherVariableType,
        latitude: Double,
        longitude: Double
    ): SortedMap<Int, WeatherModel> {
        return availableWeatherModels
            .filter { it.value.parser.containsLatLon(variable, latitude, longitude) }
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
                    return@filter false
                }

                val snappedTime = cache.getSnappedTime(variable, time, WeatherRasterTimeSnappingType.Earliest)
                requireNotNull(snappedTime) {
                    // Time index could not be determined
                    return@filter false
                }

                cache.variableExistsAtTimeAndPosition(variable, snappedTime, coordinates)
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

    fun queueJob(weatherModel: WeatherModel, weatherModelCache: WeatherRasterCompositeCache, updateJobType: WeatherModelUpdateJobType, dependsOn: WeatherModelUpdateJob? = null): WeatherModelUpdateJob {
        val updateJob = WeatherModelUpdateJob(
            updateJobType,
            weatherModel,
            weatherModelCache,
            ZonedDateTime.now(),
            dependsOn
        )

        jobs.add(updateJob)
        return updateJob
    }

    fun queueUpdateJob(
        weatherModel: WeatherModel,
        weatherModelCache: WeatherRasterCompositeCache,
        updateSource: WeatherModelUpdateJobEnabled = WeatherModelUpdateJobEnabled.Enabled,
        updateParser: WeatherModelUpdateJobEnabled = WeatherModelUpdateJobEnabled.Enabled,
        updateCache: WeatherModelUpdateJobEnabled = WeatherModelUpdateJobEnabled.Enabled,
        cleanupStorage: WeatherModelUpdateJobEnabled = WeatherModelUpdateJobEnabled.Forced
    ) {
        val dateTime = ZonedDateTime.now()

        val sourceJob = createJob(updateSource, WeatherModelUpdateJobType.Source, weatherModel, weatherModelCache, dateTime)
        val parserJob = createJob(updateParser, WeatherModelUpdateJobType.Parser, weatherModel, weatherModelCache, dateTime, sourceJob)
        val cacheJob = createJob(updateCache, WeatherModelUpdateJobType.Cache, weatherModel, weatherModelCache, dateTime, parserJob)
        val cleanupJob = createJob(cleanupStorage, WeatherModelUpdateJobType.Cleanup, weatherModel, weatherModelCache, dateTime, cacheJob)

        jobs.addAll(listOfNotNull(sourceJob, parserJob, cacheJob, cleanupJob))
    }

    private fun createJob(
        enabled: WeatherModelUpdateJobEnabled,
        type: WeatherModelUpdateJobType,
        weatherModel: WeatherModel,
        weatherModelCache: WeatherRasterCompositeCache,
        dateTime: ZonedDateTime,
        potentialDependency: WeatherModelUpdateJob? = null
    ): WeatherModelUpdateJob? {
        return when (enabled) {
            WeatherModelUpdateJobEnabled.Disabled -> null

            WeatherModelUpdateJobEnabled.Enabled ->
                WeatherModelUpdateJob(
                    type,
                    weatherModel,
                    weatherModelCache,
                    dateTime,
                    potentialDependency
                )

            WeatherModelUpdateJobEnabled.Forced ->
                WeatherModelUpdateJob(
                    type,
                    weatherModel,
                    weatherModelCache,
                    dateTime
                )
        }
    }

    suspend fun startQueue(): Flow<WeatherModelUpdateJob> = flow {
        if (queueRunning) {
            return@flow
        }
        queueRunning = true

        var job = jobs.removeFirstOrNull()
        while (job != null) {
            val success = doJob(job)

            if (success) {
                emit(job)
            } else {
                // If the job wasn't successful, remove all the other jobs that depend on it
                jobs.removeAll {
                    it.dependsOn == job
                }
            }

            job = jobs.removeFirstOrNull()
        }

        queueRunning = false
    }

    private suspend fun doJob(updateJob: WeatherModelUpdateJob): Boolean {
        return when (updateJob.jobType) {
            WeatherModelUpdateJobType.Source -> updateWeatherModelSource(updateJob)
            WeatherModelUpdateJobType.Parser -> updateDataParser(updateJob)
            WeatherModelUpdateJobType.Cache -> updateWeatherModelCache(updateJob)
            WeatherModelUpdateJobType.Cleanup -> cleanupDataStorageLocation(updateJob)
        }
    }

    private suspend fun updateWeatherModelSource(updateJob: WeatherModelUpdateJob): Boolean {
        logger.info("Updating source for ${updateJob.weatherModel.name}...")

        try {
            // Try downloading the weather model
            updateOnlineWeatherModel(updateJob)

            // If we succeed, print this info message and carry on
            logger.info("Updated source for ${updateJob.weatherModel.name}")
        } catch (e: Exception) {
            logger.error("Weather model for ${updateJob.weatherModel.name} could not be updated: ${e.message}")
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

    private fun updateDataParser(updateJob: WeatherModelUpdateJob): Boolean {
        if (updateJob.weatherModel.parser !is DynamicDataParser) {
            return false
        }

        val success = updateJob.weatherModel.parser.updateParser(updateJob.dateTime)

        if (success) {
            logger.info("Updated parser for ${updateJob.weatherModel.name}")
        } else {
            logger.error("Parser for ${updateJob.weatherModel.name} could not be updated. Skipping in update process.")
        }

        return success
    }

    private fun updateWeatherModelCache(updateJob: WeatherModelUpdateJob): Boolean {
        updateJob.weatherModelCache.updateCaches()
        logger.info("Updated cache for ${updateJob.weatherModel.name}")
        return true
    }

    private fun cleanupDataStorageLocation(updateJob: WeatherModelUpdateJob): Boolean {
        val success = fileManagerService.cleanupWeatherDataLocation(updateJob.weatherModel, updateJob.dateTime)

        if (success) {
            logger.info("Purged weather model files for ${updateJob.weatherModel.name}")
        } else {
            logger.error("Failed to purge weather model files for ${updateJob.weatherModel.name}")
        }

        return success
    }
}

enum class WeatherModelUpdateJobEnabled {
    Disabled, Enabled, Forced
}

enum class WeatherModelUpdateJobType {
    /**
     * Update the source of the weather model data
     */
    Source,

    /**
     * Update the parser for the weather model data
     */
    Parser,

    /**
     * Update the cache for the weather model data
     */
    Cache,

    /**
     * Cleanup any redundant files
     */
    Cleanup
}

data class WeatherModelUpdateJob(
    val jobType: WeatherModelUpdateJobType,
    val weatherModel: WeatherModel,
    val weatherModelCache: WeatherRasterCompositeCache,
    val dateTime: ZonedDateTime,
    val dependsOn: WeatherModelUpdateJob? = null
) {
    val receiver
        get() = weatherModel.receiver
}