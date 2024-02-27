package com.luca009.imker.server.parser

import com.luca009.imker.server.parser.model.NetCdfParser
import com.luca009.imker.server.parser.model.WeatherDataParser
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WeatherDataParserConfiguration(
    val netCdfParserFactory: (String) -> NetCdfParser,
) {
    @Bean
    fun weatherDataParserFactoryFactory() = {
            parserName: String -> weatherDataParserFactory(parserName)
    }

    fun weatherDataParserFactory(name: String): ((String) -> WeatherDataParser)? {
        return when (name) {
            "netcdf" -> netCdfParserFactory // TODO: add more parsers if required
            else -> null
        }
    }
}