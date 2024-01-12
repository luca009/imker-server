package com.luca009.imker.imkerserver.configuration.model

import com.luca009.imker.imkerserver.parser.model.WeatherDataParser
import com.luca009.imker.imkerserver.receiver.model.DataReceiver

class WeatherModel(
    val name: String,
    val friendlyName: String,
    val copyright: String,
    val mapperFile: String,
    val receiver: DataReceiver, // TODO: Update these
    val parser: WeatherDataParser
)