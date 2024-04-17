package com.luca009.imker.server.configuration.model

import com.luca009.imker.server.caching.model.WeatherRasterCompositeCacheConfiguration
import com.luca009.imker.server.management.files.model.DataFileNameManager
import com.luca009.imker.server.management.files.model.LocalFileManagementConfiguration
import com.luca009.imker.server.parser.model.WeatherDataParser
import com.luca009.imker.server.receiver.model.DataReceiver

data class WeatherModel(
    val name: String,
    val friendlyName: String,
    val copyright: String,
    val receiver: DataReceiver,
    val parser: WeatherDataParser,
    val mapper: WeatherVariableTypeMapper,
    val fileNameManager: DataFileNameManager,
    val unitMapper: WeatherVariableUnitMapper,
    val cacheConfiguration: WeatherRasterCompositeCacheConfiguration,
    val fileManagementConfiguration: LocalFileManagementConfiguration
)