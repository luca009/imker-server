package com.luca009.imker.imkerserver.queries.model

import com.luca009.imker.imkerserver.caching.model.WeatherRasterCompositeCache
import com.luca009.imker.imkerserver.parser.model.RawWeatherVariable
import com.luca009.imker.imkerserver.parser.model.WeatherVariable2dCoordinate
import com.luca009.imker.imkerserver.parser.model.WeatherVariableUnit

data class WeatherVariableProperties(
    val rawWeatherVariable: RawWeatherVariable,
    val weatherModelCache: WeatherRasterCompositeCache,
    val coordinates: WeatherVariable2dCoordinate,
    val timeIndex: Int,
    val units: WeatherVariableUnit?
)
