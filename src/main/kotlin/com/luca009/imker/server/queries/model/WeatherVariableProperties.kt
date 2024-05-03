package com.luca009.imker.server.queries.model

import com.luca009.imker.server.caching.model.WeatherRasterCompositeCache
import com.luca009.imker.server.parser.model.WeatherVariable2dCoordinate
import com.luca009.imker.server.parser.model.WeatherVariableUnit
import java.time.ZonedDateTime

data class WeatherVariableProperties(
    val weatherModelCache: WeatherRasterCompositeCache,
    val coordinates: WeatherVariable2dCoordinate,
    val snappedTime: ZonedDateTime,
    val units: WeatherVariableUnit?
)
