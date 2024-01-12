package com.luca009.imker.imkerserver.updating

import com.luca009.imker.imkerserver.management.model.WeatherModelManagerService
import com.luca009.imker.imkerserver.updating.model.WeatherModelUpdateService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
class WeatherModelUpdateServiceConfiguration {
    @Bean
    @ConditionalOnProperty(value = ["update.lazyUpdate"], matchIfMissing = true, havingValue = "false")
    fun weatherModelUpdateService(
        weatherModelManagerService: WeatherModelManagerService, updateProperties: UpdateProperties
    ): WeatherModelUpdateService {
        return WeatherModelUpdateServiceImpl(weatherModelManagerService, updateProperties)
    }
}