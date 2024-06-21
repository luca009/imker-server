package com.luca009.imker.server.updating

import com.luca009.imker.server.management.models.WeatherModelUpdateJobEnabled
import com.luca009.imker.server.management.models.model.WeatherModelManagerService
import com.luca009.imker.server.updating.model.WeatherModelUpdateService
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class WeatherModelUpdateServiceImpl(
    private val weatherModelManagerService: WeatherModelManagerService
) : WeatherModelUpdateService() {
    @Scheduled(fixedRateString = "\${update.updateCheckInterval}", initialDelayString = "\${update.updateCheckInterval}")
    fun scheduledUpdateWeatherModels() = runBlocking {
        // TODO: #23: Implement lazy updating
        weatherModelManagerService.beginUpdateWeatherModels()
    }

    override suspend fun startupUpdate() {
        // Initialize all data parsers at startup by updating them, and also update any source data if lazyUpdate is off
        // TODO: #23: Implement lazy updating
        weatherModelManagerService.beginUpdateWeatherModels(updateParser = WeatherModelUpdateJobEnabled.Forced)
    }
}