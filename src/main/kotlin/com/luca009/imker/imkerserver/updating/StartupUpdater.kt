package com.luca009.imker.imkerserver.updating

import com.luca009.imker.imkerserver.configuration.properties.UpdateProperties
import com.luca009.imker.imkerserver.updating.model.WeatherModelUpdateService
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component

@Component
class StartupUpdater(
    val weatherModelUpdateService: WeatherModelUpdateService,
    val updateProperties: UpdateProperties
) : ApplicationListener<ContextRefreshedEvent> {
    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        // Initialize all data parsers at startup by updating them, and also update any source data if lazyUpdate is off
        weatherModelUpdateService.updateWeatherModels(!updateProperties.lazyUpdate)
    }
}