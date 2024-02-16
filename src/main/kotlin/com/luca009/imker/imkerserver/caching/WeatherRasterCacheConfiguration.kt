package com.luca009.imker.imkerserver.caching

import com.luca009.imker.imkerserver.caching.model.*
import com.luca009.imker.imkerserver.configuration.model.WeatherVariableFileNameMapper
import com.luca009.imker.imkerserver.configuration.model.WeatherVariableUnitMapper
import com.luca009.imker.imkerserver.parser.model.WeatherDataParser
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

    @Bean
    @Scope("prototype")
    fun weatherRasterCompositeCache(configuration: WeatherRasterCompositeCacheConfiguration, dataParser: WeatherDataParser, variableMapper: WeatherVariableFileNameMapper, unitMapper: WeatherVariableUnitMapper): WeatherRasterCompositeCache {
        return WeatherRasterCompositeCacheImpl(
            configuration,
            dataParser,
            variableMapper,
            unitMapper,
            weatherRasterMemoryCache(),
            weatherRasterDiskCache(dataParser, variableMapper),
            weatherRasterCacheMapper(),
            weatherTimeCache(),
            weatherVariableUnitCache()
        )
    }

    @Bean
    @Scope("prototype")
    fun weatherRasterDiskCache(dataParser: WeatherDataParser, variableMapper: WeatherVariableFileNameMapper): WeatherRasterDiskCache {
        return WeatherRasterDiskCacheImpl(
            dataParser,
            variableMapper,
            WeatherRasterCacheHelper()
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

    @Bean
    @Scope("prototype")
    fun weatherRasterCacheMapper(): WeatherRasterCacheHelper {
        return WeatherRasterCacheHelper()
    }
}
