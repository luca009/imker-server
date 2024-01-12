package com.luca009.imker.imkerserver.parser

import com.luca009.imker.imkerserver.parser.model.DynamicDataParser
import com.luca009.imker.imkerserver.parser.model.WeatherDataParser
import com.luca009.imker.imkerserver.receiver.model.DataReceiver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope

@Configuration
class DynamicDataParserConfiguration {
    @Bean
    fun dynamicDataParserFactory() = {
        weatherDataParser: WeatherDataParser, dataReceiver: DataReceiver -> dynamicDataParser(weatherDataParser, dataReceiver)
    }

    @Bean
    @Scope("prototype")
    fun dynamicDataParser(weatherDataParser: WeatherDataParser, dataReceiver: DataReceiver): DynamicDataParser {
        return DynamicDataParserImpl(
            weatherDataParser,
            dataReceiver
        )
    }
}