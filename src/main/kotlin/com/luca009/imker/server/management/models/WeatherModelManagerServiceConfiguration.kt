package com.luca009.imker.server.management.models

import com.luca009.imker.server.caching.model.WeatherRasterCompositeCache
import com.luca009.imker.server.caching.model.WeatherRasterCompositeCacheConfiguration
import com.luca009.imker.server.configuration.model.WeatherModelPropertyMapperService
import com.luca009.imker.server.configuration.model.WeatherVariableFileNameMapper
import com.luca009.imker.server.configuration.model.WeatherVariableUnitMapper
import com.luca009.imker.server.management.files.model.LocalFileManagerService
import com.luca009.imker.server.management.models.model.WeatherModelManagerService
import com.luca009.imker.server.parser.model.WeatherDataParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WeatherModelManagerServiceConfiguration(
    private val weatherRasterCompositeCacheFactory: (WeatherRasterCompositeCacheConfiguration, WeatherDataParser, WeatherVariableFileNameMapper, WeatherVariableUnitMapper) -> WeatherRasterCompositeCache,
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

//        val weatherModels: SortedMap<Int, WeatherModel> = sortedMapOf(
//            0 to WeatherModel(
//                "INCA",
//                "INCA",
//                "GeoSphere Austria under CC BY-SA 4.0",
//
//                incaReceiver,
//                dynamicParserFactory(netCdfParserFactory, fileManagerService.getWeatherDataLocation("default", "inca").toAbsolutePath().toString(), incaFileNameManagerService), // TODO: replace this with the actual files we have downloaded
//                weatherVariableFileNameMapperFactory(File("src/test/resources/inca/inca_map.csv")),
//                weatherVariableUnitMapperFactory(File("src/test/resources/unit_map.csv")),
//
//                WeatherRasterCompositeCacheConfiguration(
//                    setOf(
//                        // variables in memory
//                        WeatherVariableType.Temperature2m
//                    ),
//                    setOf() // ignored variables
//                )
//            ),
//            1 to WeatherModel(
//                "AROME",
//                "AROME",
//                "GeoSphere Austria under CC BY-SA 4.0",
//
//                aromeReceiver,
//                dynamicParserFactory(netCdfParserFactory, fileManagerService.getWeatherDataLocation("default", "arome").toAbsolutePath().toString(), aromeFileNameManager), // TODO: replace this with the actual files we have downloaded
//                weatherVariableFileNameMapperFactory(File("src/test/resources/arome/arome_map.csv")),
//                weatherVariableUnitMapperFactory(File("src/test/resources/unit_map.csv")),
//
//                WeatherRasterCompositeCacheConfiguration(
//                    setOf(
//                        // variables in memory
//                        WeatherVariableType.Temperature2m
//                    ),
//                    setOf() // ignored variables
//                )
//            )
//        )

        return WeatherModelManagerServiceImpl(
            weatherModels,
            weatherRasterCompositeCacheFactory,
            fileManagerService
        )
    }
}