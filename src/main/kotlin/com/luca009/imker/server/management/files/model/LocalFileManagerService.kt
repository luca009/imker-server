package com.luca009.imker.server.management.files.model

import com.luca009.imker.server.configuration.model.WeatherModel
import java.nio.file.Path
import java.time.Duration

interface LocalFileManagerService {
    fun getWeatherDataLocation(storageLocation: String, subFolder: String? = null): Path
    fun cleanupWeatherDataLocation(weatherModel: WeatherModel): Boolean
}

data class LocalFileManagementConfiguration(
    val storageLocation: Path,
    val maxAge: Duration?,
    val maxCount: UInt?
)