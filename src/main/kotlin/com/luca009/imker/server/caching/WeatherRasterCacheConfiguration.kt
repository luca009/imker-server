package com.luca009.imker.server.caching

import com.luca009.imker.server.caching.model.*
import com.luca009.imker.server.configuration.model.WeatherVariableFileNameMapper
import com.luca009.imker.server.configuration.model.WeatherVariableUnitMapper
import com.luca009.imker.server.parser.model.WeatherDataParser
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope

@Configuration
class WeatherRasterCacheConfiguration {
    // Return a function for calling the weatherRasterCompositeCache() bean
    // This is meant to be injected somewhere else as a factory, so we don't have to provide direct access to this configuration class
    @Bean
    fun weatherRasterCompositeCacheFactory() = {
        configuration: WeatherRasterCompositeCacheConfiguration, dataParser: WeatherDataParser, variableMapper: WeatherVariableFileNameMapper, unitMapper: WeatherVariableUnitMapper -> weatherRasterCompositeCache(configuration, dataParser, variableMapper, unitMapper)
    }

    fun weatherRasterCompositeCache(configuration: WeatherRasterCompositeCacheConfiguration, dataParser: WeatherDataParser, variableMapper: WeatherVariableFileNameMapper, unitMapper: WeatherVariableUnitMapper): WeatherRasterCompositeCache {
        return WeatherRasterCompositeCacheImpl(
            configuration,
            dataParser,
            variableMapper,
            unitMapper,
            weatherRasterMemoryCache(),
            weatherRasterDiskCache(dataParser, variableMapper),
            weatherTimeCache(),
            weatherVariableUnitCache()
        )
    }

    fun weatherRasterDiskCache(dataParser: WeatherDataParser, variableMapper: WeatherVariableFileNameMapper): WeatherRasterDiskCache {
        return WeatherRasterDiskCacheImpl(
            dataParser,
            variableMapper
        )
    }

    @Bean
    @Scope("prototype")
    fun weatherRasterMemoryCache(): WeatherRasterMemoryCache {
        return WeatherRasterMemoryCacheImpl()
    }

    @Bean
    @Scope("prototype")
    fun weatherTimeCache(): WeatherTimeCache {
        return WeatherTimeCacheImpl()
    }

    @Bean
    @Scope("prototype")
    fun weatherVariableUnitCache(): WeatherVariableUnitCache {
        return WeatherVariableUnitCacheImpl()
    }
}
