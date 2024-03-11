package com.luca009.imker.server.updating.model

import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent

abstract class WeatherModelUpdateService : ApplicationListener<ContextRefreshedEvent> {
    protected abstract fun startupUpdate()
    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        startupUpdate()
    }
}