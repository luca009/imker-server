package com.luca009.imker.server.management.files.model

import com.luca009.imker.server.configuration.model.WeatherModel
import java.io.File
import java.nio.file.Path
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Interface for managing storage locations for local files
 */
interface LocalFileManagerService {
    /**
     * Get the path to the specified [storageLocation] (and optionally, with a [subFolder])
     */
    fun getWeatherDataLocation(storageLocation: String, subFolder: String? = null): Path

    /**
     * Get files by the specified [weatherModel] that are to be cleaned up as based on the configured storage policy
     */
    fun getFilesForCleanup(weatherModel: WeatherModel, referenceDateTime: ZonedDateTime = ZonedDateTime.now()): Set<Pair<File, ZonedDateTime>>?

    /**
     * Cleanup files by the specified [weatherModel] based on the configured storage policy
     */
    fun cleanupWeatherDataLocation(weatherModel: WeatherModel, referenceDateTime: ZonedDateTime = ZonedDateTime.now()): Boolean
}

data class LocalFileManagementConfiguration(
    val storageLocation: Path,
    val maxAge: Duration?,
    val maxCount: UInt?
)