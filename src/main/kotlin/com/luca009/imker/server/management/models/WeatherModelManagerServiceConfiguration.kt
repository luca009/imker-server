package com.luca009.imker.server.management.models

import com.luca009.imker.server.caching.model.WeatherRasterCompositeCache
import com.luca009.imker.server.caching.model.WeatherRasterCompositeCacheConfiguration
import com.luca009.imker.server.configuration.model.WeatherModelPropertyMapperService
import com.luca009.imker.server.management.files.model.LocalFileManagerService
import com.luca009.imker.server.management.models.model.WeatherModelManagerService
import com.luca009.imker.server.parser.model.WeatherDataParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WeatherModelManagerServiceConfiguration(
    private val weatherRasterCompositeCacheFactory: (WeatherRasterCompositeCacheConfiguration, WeatherDataParser) -> WeatherRasterCompositeCache,
    private val weatherModelPropertyMapperService: WeatherModelPropertyMapperService,
    private val fileManagerService: LocalFileManagerService
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Bean
    fun weatherModelManagerService(): WeatherModelManagerService {
        val weatherModels = weatherModelPropertyMapperService.getWeatherModels()

        if (weatherModels.isEmpty()) {
            logger.error("No weather models were assembled/defined. Every request will be considered a bad request. Please check your configuration.")
        }

        return WeatherModelManagerServiceImpl(
            weatherModels,
            weatherRasterCompositeCacheFactory,
            fileManagerService
        )
    }
}