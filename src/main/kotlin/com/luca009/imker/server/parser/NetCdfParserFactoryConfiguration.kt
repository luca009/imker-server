package com.luca009.imker.server.parser

import com.luca009.imker.server.configuration.model.WeatherVariableFileNameMapper
import com.luca009.imker.server.configuration.model.WeatherVariableUnitMapper
import com.luca009.imker.server.parser.model.NetCdfParser
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path

@Configuration
class NetCdfParserFactoryConfiguration {
    @Bean
    fun netCdfParserFactory() = {
        netCdfFilePath: Path, variableMapper: WeatherVariableFileNameMapper, unitMapper: WeatherVariableUnitMapper -> netCdfParser(netCdfFilePath, variableMapper, unitMapper)
    }

     fun netCdfParser(netCdfFilePath: Path, variableMapper: WeatherVariableFileNameMapper, unitMapper: WeatherVariableUnitMapper): NetCdfParser {
        return NetCdfParserImpl(
            netCdfFilePath,
            variableMapper,
            unitMapper
        )
    }
}