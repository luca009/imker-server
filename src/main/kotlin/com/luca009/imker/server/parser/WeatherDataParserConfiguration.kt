package com.luca009.imker.server.parser

import com.luca009.imker.server.configuration.model.WeatherVariableFileNameMapper
import com.luca009.imker.server.configuration.model.WeatherVariableUnitMapper
import com.luca009.imker.server.parser.model.NetCdfParser
import com.luca009.imker.server.parser.model.WeatherDataParser
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path

@Configuration
class WeatherDataParserConfiguration(
    val netCdfParserFactory: (Path, variableMapper: WeatherVariableFileNameMapper, unitMapper: WeatherVariableUnitMapper) -> NetCdfParser,
) {
    @Bean
    fun weatherDataParserFactoryFactory() = {
            parserName: String -> weatherDataParserFactory(parserName)
    }

    fun weatherDataParserFactory(name: String): ((Path, variableMapper: WeatherVariableFileNameMapper, unitMapper: WeatherVariableUnitMapper) -> WeatherDataParser)? {
        return when (name) {
            "netcdf" -> netCdfParserFactory // TODO: add more parsers if required
            else -> null
        }
    }
}