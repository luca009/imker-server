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
        configuration: WeatherRasterCompositeCacheConfiguration, dataParser: WeatherDataParser -> weatherRasterCompositeCache(configuration, dataParser)
    }

    fun weatherRasterCompositeCache(configuration: WeatherRasterCompositeCacheConfiguration, dataParser: WeatherDataParser): WeatherRasterCompositeCache {
        return WeatherRasterCompositeCacheImpl(
            configuration,
            dataParser,
            weatherRasterMemoryCache(),
            weatherRasterDiskCache(dataParser),
            weatherTimeCache()
        )
    }

    fun weatherRasterDiskCache(dataParser: WeatherDataParser): WeatherRasterDiskCache {
        return WeatherRasterDiskCacheImpl(dataParser)
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
}
