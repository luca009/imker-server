package com.luca009.imker.server.management.files

import com.luca009.imker.server.configuration.model.WeatherModel
import com.luca009.imker.server.configuration.properties.StorageProperties
import com.luca009.imker.server.management.files.model.LocalFileManagerService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.*
import kotlin.io.path.Path

@Service
class LocalFileManagerServiceImpl(
    val storageProperties: StorageProperties
) : LocalFileManagerService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun getWeatherDataLocation(storageLocation: String, subFolder: String?): Path {
        val preferredStorageLocationPath = storageProperties.storageLocations[storageLocation]

        val storageLocationPath =
            if (preferredStorageLocationPath == null) {
                logger.warn("$storageLocation is not a valid storage location. Defaulting to default storage location. Are the storage locations configured correctly?")
                storageProperties.storageLocations["default"]
            } else {
                preferredStorageLocationPath
            }

        requireNotNull(storageLocationPath) {
            throw IllegalArgumentException("There is no default storage location configured. Are the storage locations configured correctly?")
        }

        return if (subFolder == null) {
            Path(storageLocationPath)
        } else {
            Path(storageLocationPath, subFolder)
        }
    }

    private inline fun <T, R> requireArgumentNotNullOrDefault(argument: R?, defaultValue: T, function: (R) -> T): T {
        requireNotNull(argument) {
            return defaultValue
        }

        return function(argument)
    }

    private fun Set<Pair<File, ZonedDateTime>>.filterByMaxCount(maxCount: UInt): Set<Pair<File, ZonedDateTime>> {
        val cutoffIndex = this.count() - maxCount.toInt()
        if (cutoffIndex < 0) {
            return this
        }

        // Sort the set, otherwise filtering by maxCount doesn't make much sense
        val sortedSet = this.toSortedSet(compareBy { it.second })

        val fromElement = sortedSet.elementAt(cutoffIndex)
        return sortedSet.tailSet(fromElement)
    }

    private fun Set<Pair<File, ZonedDateTime>>.filterByMaxAge(maxAge: Duration): Set<Pair<File, ZonedDateTime>> {
        val currentTime = ZonedDateTime.now()

        return this.filter {
            // Filter by ensuring that the files are within the max age
            val duration = Duration.between(it.second, currentTime)
            duration <= maxAge
        }.toSet()
    }

    override fun cleanupWeatherDataLocation(
        weatherModel: WeatherModel
    ): Boolean {
        // Neither cleanup policy is configured, don't do anything
        if (weatherModel.fileManagementConfiguration.maxAge == null &&
            weatherModel.fileManagementConfiguration.maxCount == null) {
            return true
        }

        val storageLocationFile = weatherModel.fileManagementConfiguration.storageLocation.toFile()
        val files = storageLocationFile
            .listFiles()
            ?.mapNotNull {
                // Don't look at directories
                if (!it.isFile) {
                    return@mapNotNull null
                }

                // Ensure that the file and its date isn't null
                Pair(
                    requireNotNull(it),
                    weatherModel.fileNameManager.getDateTimeForFile(it.name) ?: return@mapNotNull null
                )
            }
            ?.toSet()

        requireNotNull(files) {
            return false
        }

        // Filter by max count. If the maxFiles storage policy is null, use files as default value
        val maxCountFilteredFiles = requireArgumentNotNullOrDefault(weatherModel.fileManagementConfiguration.maxCount, files) {
            files.filterByMaxCount(it)
        }

        // Same as above but with maxAge and using the previous set
        val maxAgeFilteredFiles = requireArgumentNotNullOrDefault(weatherModel.fileManagementConfiguration.maxAge, maxCountFilteredFiles) {
            maxCountFilteredFiles.filterByMaxAge(it)
        }

        // Negate the results because we've filtered out the files we want to keep, but we want the ones we want to delete
        val filesToBeDeleted = files.subtract(maxAgeFilteredFiles)

        // We're using all instead of forEach to populate the success variable (file.delete() returns boolean indicating success)
        return filesToBeDeleted.all {
            it.first.delete()
        }
    }
}