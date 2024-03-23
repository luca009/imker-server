package com.luca009.imker.server.updating

import com.luca009.imker.server.configuration.properties.UpdateProperties
import com.luca009.imker.server.management.models.model.WeatherModelManagerService
import com.luca009.imker.server.updating.model.WeatherModelUpdateService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class WeatherModelUpdateServiceImpl(
    private val updateProperties: UpdateProperties,
    private val weatherModelManagerService: WeatherModelManagerService
) : WeatherModelUpdateService() {
    @Scheduled(fixedRateString = "\${update.updateCheckInterval}", initialDelayString = "\${update.updateCheckInterval}", timeUnit = TimeUnit.MINUTES)
    fun scheduledUpdateWeatherModels() {
        weatherModelManagerService.updateWeatherModels(!updateProperties.lazyUpdate)
    }

    override fun startupUpdate() {
        // Initialize all data parsers at startup by updating them, and also update any source data if lazyUpdate is off
        weatherModelManagerService.updateWeatherModels(!updateProperties.lazyUpdate, true)
    }
}