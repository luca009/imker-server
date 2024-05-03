package com.luca009.imker.server.parser

import com.luca009.imker.server.configuration.model.WeatherVariableTypeMapper
import com.luca009.imker.server.configuration.model.WeatherVariableUnitMapper
import com.luca009.imker.server.management.files.model.BestFileSearchService
import com.luca009.imker.server.management.files.model.DataFileNameManager
import com.luca009.imker.server.parser.model.DynamicDataParser
import com.luca009.imker.server.parser.model.WeatherDataParser
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path
import kotlin.io.path.isDirectory

@Configuration
class DynamicDataParserConfiguration {
    @Bean
    fun dynamicDataParserFactory(bestFileSearchService: BestFileSearchService) = {
            parserFactory: (Path, WeatherVariableTypeMapper, WeatherVariableUnitMapper) -> WeatherDataParser, initFilePath: Path, fileNameManager: DataFileNameManager, variableMapper: WeatherVariableTypeMapper, unitMapper: WeatherVariableUnitMapper -> dynamicDataParser(parserFactory, initFilePath, bestFileSearchService, fileNameManager, variableMapper, unitMapper)
    }

     fun dynamicDataParser(
         parserFactory: (Path, WeatherVariableTypeMapper, WeatherVariableUnitMapper) -> WeatherDataParser,
         initFilePath: Path,
         bestFileSearchService: BestFileSearchService,
         fileNameManager: DataFileNameManager,
         variableMapper: WeatherVariableTypeMapper,
         unitMapper: WeatherVariableUnitMapper
    ): DynamicDataParser {
        val initParser = if (initFilePath.isDirectory()) {
            null
        } else {
            parserFactory(initFilePath, variableMapper, unitMapper)
        }

        return DynamicDataParserImpl(
            initParser,
            parserFactory,
            initFilePath,
            bestFileSearchService,
            fileNameManager,
            variableMapper,
            unitMapper
        )
    }
}