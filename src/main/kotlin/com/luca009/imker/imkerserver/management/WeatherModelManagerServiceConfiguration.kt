package com.luca009.imker.imkerserver.management

import com.luca009.imker.imkerserver.caching.model.WeatherRasterCompositeCache
import com.luca009.imker.imkerserver.caching.model.WeatherRasterCompositeCacheConfiguration
import com.luca009.imker.imkerserver.configuration.model.WeatherModel
import com.luca009.imker.imkerserver.configuration.model.WeatherVariableFileNameMapper
import com.luca009.imker.imkerserver.filemanager.model.DataFileNameManager
import com.luca009.imker.imkerserver.filemanager.model.IncaFileNameManager
import com.luca009.imker.imkerserver.filemanager.model.LocalFileManagerService
import com.luca009.imker.imkerserver.management.model.WeatherModelManagerService
import com.luca009.imker.imkerserver.parser.model.DynamicDataParser
import com.luca009.imker.imkerserver.parser.model.NetCdfParser
import com.luca009.imker.imkerserver.parser.model.WeatherDataParser
import com.luca009.imker.imkerserver.parser.model.WeatherVariableType
import com.luca009.imker.imkerserver.receiver.model.IncaReceiver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import java.util.SortedMap

@Configuration
class WeatherModelManagerServiceConfiguration(
    val netCdfParserFactory: (String) -> NetCdfParser,
    val dynamicParserFactory: ((String) -> WeatherDataParser, String, DataFileNameManager) -> DynamicDataParser,
    val weatherVariableFileNameMapperFactory: (File) -> WeatherVariableFileNameMapper,
    val weatherRasterCompositeCacheFactory: (WeatherRasterCompositeCacheConfiguration, WeatherDataParser, WeatherVariableFileNameMapper) -> WeatherRasterCompositeCache,
    val incaFileNameManager: IncaFileNameManager,
    val incaReceiver: IncaReceiver,
    val fileManagerService: LocalFileManagerService
) {
    @Bean
    fun weatherModelManagerService(): WeatherModelManagerService {
        // TODO: this needs to be replaced with configuration files. For now, we only have one weather model

        val weatherModels: SortedMap<Int, WeatherModel> = sortedMapOf(
            0 to WeatherModel(
                "INCA",
                "INCA",
                "GeoSphere Austria under CC BY-SA 4.0",

                incaReceiver,
                dynamicParserFactory(netCdfParserFactory, fileManagerService.getWeatherDataLocation("default", "inca").toAbsolutePath().toString(), incaFileNameManager), // TODO: replace this with the actual files we have downloaded
                weatherVariableFileNameMapperFactory(File("src/test/resources/inca/inca_map.csv")),

                WeatherRasterCompositeCacheConfiguration(
                    setOf(
                        // variables in memory
                        WeatherVariableType.Temperature2m
                    ),
                    setOf() // ignored variables
                )
            )
        )

        return WeatherModelManagerServiceImpl(
            weatherModels,
            weatherRasterCompositeCacheFactory
        )
    }
}