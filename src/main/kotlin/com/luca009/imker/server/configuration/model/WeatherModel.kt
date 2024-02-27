package com.luca009.imker.server.configuration.model

import com.luca009.imker.server.caching.model.WeatherRasterCompositeCacheConfiguration
import com.luca009.imker.server.parser.model.WeatherDataParser
import com.luca009.imker.server.receiver.model.DataReceiver

class WeatherModel(
    val name: String,
    val friendlyName: String,
    val copyright: String,
    val receiver: DataReceiver, // TODO: Update these
    val parser: WeatherDataParser,
    val mapper: WeatherVariableFileNameMapper,
    val unitMapper: WeatherVariableUnitMapper,
    val cacheConfiguration: WeatherRasterCompositeCacheConfiguration
)