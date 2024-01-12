package com.luca009.imker.imkerserver.updating

import com.luca009.imker.imkerserver.management.model.WeatherModelManagerService
import com.luca009.imker.imkerserver.updating.model.WeatherModelUpdateService
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    override fun updateWeatherModels() = runBlocking {
        weatherModelManagerService.getWeatherModels().forEach {
            val weatherModel = requireNotNull(it.value) { return@forEach }

            if (!weatherModel.receiver.updateNecessary(ZonedDateTime.now().minusMinutes(15))) {
                // No update necessary
                return@forEach
            }

            val result = weatherModel.receiver.downloadData(ZonedDateTime.now(), null) // TODO: dynamic file names
            if (!result.successful) {
                logger.warn("Receiving data for ${weatherModel.name} failed")
            } else {
                logger.info("Received data for ${weatherModel.name}")
            }
        }

        if (!updateProperties.lazyCaching) {
            launch {
                // TODO: This should update the caches
            }
        }
    }
}