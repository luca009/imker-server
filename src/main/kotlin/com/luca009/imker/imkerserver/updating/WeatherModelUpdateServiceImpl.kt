package com.luca009.imker.imkerserver.updating

import com.luca009.imker.imkerserver.configuration.model.WeatherModel
import com.luca009.imker.imkerserver.configuration.properties.UpdateProperties
import com.luca009.imker.imkerserver.management.model.WeatherModelManagerService
import com.luca009.imker.imkerserver.updating.model.WeatherModelUpdateService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

@Component
class WeatherModelUpdateServiceImpl(
    private val weatherModelManagerService: WeatherModelManagerService,
    private val updateProperties: UpdateProperties
) : WeatherModelUpdateService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(fixedRateString = "\${update.updateCheckInterval}", initialDelayString = "\${update.updateCheckInterval}", timeUnit = TimeUnit.MINUTES)
    fun scheduledUpdateWeatherModels() {
        if (updateProperties.lazyUpdate) {
            return
        }

        updateWeatherModels(true)
    }

    override fun updateWeatherModels(updateSources: Boolean) {
        val updatedModels = if (updateSources) {
            updateOnlineWeatherModels()
        } else {
            weatherModelManagerService.getWeatherModels().values.map { requireNotNull(it) }.toMutableSet() // all available weather models
        }

        weatherModelManagerService.updateDataParsers(updatedModels)

        // Lazy caching is on, don't update the caches
        if (updateProperties.lazyCaching) {
            return
        }

        weatherModelManagerService.updateWeatherModelCaches(updatedModels)
    }

    private fun updateOnlineWeatherModels(): Set<WeatherModel> {
        // TODO: Make this concurrent at some point
        // Issue with concurrency would be the fact that we might end up connecting to the same FTP server twice (like with INCA and AROME). This needs to be considered.

        val updatedModels: MutableSet<WeatherModel> = mutableSetOf()

        weatherModelManagerService.getWeatherModels().forEach {
            val weatherModel = requireNotNull(it.value) { return@forEach }

            if (!weatherModel.receiver.updateNecessary(ZonedDateTime.now())) {
                // No update necessary
                return@forEach
            }

            val result = weatherModel.receiver.downloadData(ZonedDateTime.now(), null) // TODO: dynamic file names
            if (!result.successful) {
                logger.warn("Receiving data for ${weatherModel.name} failed")
                return@forEach
            }

            logger.info("Received data for ${weatherModel.name}")
            updatedModels.add(weatherModel)
        }

        return updatedModels
    }
}