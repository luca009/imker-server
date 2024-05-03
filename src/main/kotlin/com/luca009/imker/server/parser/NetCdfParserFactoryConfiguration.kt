package com.luca009.imker.server.parser

import com.luca009.imker.server.configuration.model.WeatherVariableTypeMapper
import com.luca009.imker.server.configuration.model.WeatherVariableUnitMapper
import com.luca009.imker.server.parser.model.NetCdfParser
import com.luca009.imker.server.parser.model.WeatherVariableType
import com.luca009.imker.server.transformer.model.DataTransformer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path

@Configuration
class NetCdfParserFactoryConfiguration {
    @Bean
    fun netCdfParserFactory() = {
            netCdfFilePath: Path, variableMapper: WeatherVariableTypeMapper, unitMapper: WeatherVariableUnitMapper, transformers: Map<WeatherVariableType, List<DataTransformer>> -> netCdfParser(netCdfFilePath, variableMapper, unitMapper, transformers)
    }

     fun netCdfParser(netCdfFilePath: Path, variableMapper: WeatherVariableTypeMapper, unitMapper: WeatherVariableUnitMapper, transformers: Map<WeatherVariableType, List<DataTransformer>>): NetCdfParser {
        return NetCdfParserImpl(
            netCdfFilePath,
            variableMapper,
            unitMapper,
            transformers
        )
    }
}