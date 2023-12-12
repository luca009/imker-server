package com.luca009.imker.imkerserver.caching

import com.luca009.imker.imkerserver.caching.model.WeatherVariable2dRasterSlice
import com.luca009.imker.imkerserver.caching.model.WeatherVariableSlice
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class WeatherRasterCacheHelper {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun arrayToWeatherVariableSlice(input: Array<*>): WeatherVariableSlice? {
        if (!input.isArrayOf<Array<FloatArray>>()) {
            logger.warn("Weather variable was of unexpected type: ${input::class.java.simpleName}")
            return null
        }

        return WeatherVariableSlice(
            input.mapNotNull {
                WeatherVariable2dRasterSlice(it as? Array<FloatArray> ?: return null)
            }.toTypedArray()
        )
    }
}