package com.luca009.imker.server.management.files

import com.luca009.imker.server.ArgumentNotNullHelper.requireArgumentNotNullOrDefault
import com.luca009.imker.server.configuration.model.WeatherModel
import com.luca009.imker.server.configuration.properties.StorageProperties
import com.luca009.imker.server.management.files.model.LocalFileManagerService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path
import java.time.Duration
import java.time.ZonedDateTime
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

    private fun Set<Pair<File, ZonedDateTime>>.filterByMaxAge(
        maxAge: Duration,
        referenceDateTime: ZonedDateTime
    ): Set<Pair<File, ZonedDateTime>> {
        return this.filter {
            // Filter by ensuring that the files are within the max age
            val duration = Duration.between(it.second, referenceDateTime)
            duration <= maxAge
        }.toSet()
    }

    override fun getFilesForCleanup(
        weatherModel: WeatherModel,
        referenceDateTime: ZonedDateTime
    ): Set<Pair<File, ZonedDateTime>>? {
        // Neither cleanup policy is configured, don't do anything
        if (weatherModel.fileManagementConfiguration.maxAge == null &&
            weatherModel.fileManagementConfiguration.maxCount == null) {
            return setOf()
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
                    it ?: return@mapNotNull null,
                    weatherModel.fileNameManager.getDateTimeForFile(it.toPath()) ?: return@mapNotNull null
                )
            }
            ?.toSet()

        requireNotNull(files) {
            return null
        }

        // Filter by max count. If the maxFiles storage policy is null, use files as default value
        val maxCountFilteredFiles = requireArgumentNotNullOrDefault(weatherModel.fileManagementConfiguration.maxCount, files) {
            files.filterByMaxCount(it)
        }

        // Same as above but with maxAge and using the previous set
        val maxAgeFilteredFiles = requireArgumentNotNullOrDefault(weatherModel.fileManagementConfiguration.maxAge, maxCountFilteredFiles) {
            maxCountFilteredFiles.filterByMaxAge(it, referenceDateTime)
        }

        // Negate the results because we've filtered out the files we want to keep, but we want the ones we want to delete
        return files.subtract(maxAgeFilteredFiles)
    }

    override fun cleanupWeatherDataLocation(
        weatherModel: WeatherModel,
        referenceDateTime: ZonedDateTime
    ): Boolean {
        // Neither cleanup policy is configured: don't do anything, but return a success
        if (weatherModel.fileManagementConfiguration.maxAge == null &&
            weatherModel.fileManagementConfiguration.maxCount == null) {
            return true
        }

        val filesToBeDeleted = getFilesForCleanup(weatherModel, referenceDateTime)

        requireNotNull(filesToBeDeleted) {
            return false
        }

        // We're using all instead of forEach to populate the success variable (file.delete() returns boolean indicating success)
        return filesToBeDeleted.all {
            it.first.delete()
        }
    }
}