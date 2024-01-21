package com.luca009.imker.imkerserver.updating

import com.luca009.imker.imkerserver.configuration.model.WeatherModel
import com.luca009.imker.imkerserver.configuration.properties.UpdateProperties
import com.luca009.imker.imkerserver.management.model.WeatherModelManagerService
import com.luca009.imker.imkerserver.updating.model.WeatherModelUpdateService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class WeatherModelUpdateServiceImpl(
    private val weatherModelManagerService: WeatherModelManagerService,
    private val updateProperties: UpdateProperties
) : WeatherModelUpdateService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(fixedRateString = "\${update.updateCheckInterval}", timeUnit = TimeUnit.MINUTES)
    override fun updateWeatherModels() {
        // TODO: Make this concurrent at some point
        // Issue with concurrency would be the fact that we might end up connecting to the same FTP server twice (like with INCA and AROME). This needs to be considered.

        //val updatedModels: MutableSet<WeatherModel> = mutableSetOf()
        val updatedModels: MutableSet<WeatherModel> = weatherModelManagerService.getWeatherModels().values.map { requireNotNull(it) }.toMutableSet()

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

        weatherModelManagerService.updateDataParsers(updatedModels)

        // Lazy caching is on, don't update the caches
        if (updateProperties.lazyCaching) {
            return
        }

        weatherModelManagerService.updateWeatherModelCaches(updatedModels)
    }
}