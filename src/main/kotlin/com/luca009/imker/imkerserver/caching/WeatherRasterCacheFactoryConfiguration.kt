package com.luca009.imker.imkerserver.caching

import com.luca009.imker.imkerserver.caching.model.WeatherRasterCompositeCache
import com.luca009.imker.imkerserver.caching.model.WeatherRasterCompositeCacheConfiguration
import com.luca009.imker.imkerserver.caching.model.WeatherRasterDiskCache
import com.luca009.imker.imkerserver.caching.model.WeatherRasterMemoryCache
import com.luca009.imker.imkerserver.configuration.model.WeatherVariableFileNameMapper
import com.luca009.imker.imkerserver.parser.model.WeatherDataParser
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope

@Configuration
class WeatherRasterCacheFactoryConfiguration {
    // Return a function for calling the weatherRasterCompositeCache() bean
    // This is meant to be injected somewhere else as a factory, so we don't have to provide direct access to this configuration class
    @Bean
    fun weatherRasterCompositeCacheFactory() = {
        configuration: WeatherRasterCompositeCacheConfiguration, dataParser: WeatherDataParser, variableMapper: WeatherVariableFileNameMapper -> weatherRasterCompositeCache(configuration, dataParser, variableMapper)
    }

    @Bean
    @Scope("prototype")
    fun weatherRasterCompositeCache(configuration: WeatherRasterCompositeCacheConfiguration, dataParser: WeatherDataParser, variableMapper: WeatherVariableFileNameMapper): WeatherRasterCompositeCache {
        return WeatherRasterCompositeCacheImpl(
            configuration,
            dataParser,
            variableMapper,
            weatherRasterMemoryCache(),
            weatherRasterDiskCache(dataParser, variableMapper),
            weatherRasterCacheMapper()
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
    fun weatherRasterCacheMapper(): WeatherRasterCacheHelper {
        return WeatherRasterCacheHelper()
    }
}
