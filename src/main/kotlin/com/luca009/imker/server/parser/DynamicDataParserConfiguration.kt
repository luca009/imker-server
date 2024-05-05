package com.luca009.imker.server.parser

import com.luca009.imker.server.configuration.model.WeatherVariableTypeMapper
import com.luca009.imker.server.configuration.model.WeatherVariableUnitMapper
import com.luca009.imker.server.management.files.model.BestFileSearchService
import com.luca009.imker.server.management.files.model.DataFileNameManager
import com.luca009.imker.server.parser.model.DynamicDataParser
import com.luca009.imker.server.parser.model.WeatherDataParser
import com.luca009.imker.server.parser.model.WeatherVariableType
import com.luca009.imker.server.transformer.model.DataTransformer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path
import java.time.ZonedDateTime
import kotlin.io.path.isDirectory

@Configuration
class DynamicDataParserConfiguration {
    @Bean
    fun dynamicDataParserFactory(bestFileSearchService: BestFileSearchService) = {
            parserFactory: (Path, ZonedDateTime, WeatherVariableTypeMapper, WeatherVariableUnitMapper, Map<WeatherVariableType, List<DataTransformer>>) -> WeatherDataParser, initFilePath: Path, fileNameManager: DataFileNameManager, variableMapper: WeatherVariableTypeMapper, unitMapper: WeatherVariableUnitMapper, transformers: Map<WeatherVariableType, List<DataTransformer>> -> dynamicDataParser(parserFactory, initFilePath, bestFileSearchService, fileNameManager, variableMapper, unitMapper, transformers)
    }

     fun dynamicDataParser(
         parserFactory: (Path, ZonedDateTime, WeatherVariableTypeMapper, WeatherVariableUnitMapper, Map<WeatherVariableType, List<DataTransformer>>) -> WeatherDataParser,
         initFilePath: Path,
         bestFileSearchService: BestFileSearchService,
         fileNameManager: DataFileNameManager,
         variableMapper: WeatherVariableTypeMapper,
         unitMapper: WeatherVariableUnitMapper,
         transformers: Map<WeatherVariableType, List<DataTransformer>>
    ): DynamicDataParser {
        val initParser = if (initFilePath.isDirectory()) {
            null
        } else {
            parserFactory(
                initFilePath,
                fileNameManager.getDateTimeForFile(initFilePath)!!, // This shouldn't be null - otherwise something else would've failed before this
                variableMapper,
                unitMapper,
                transformers
            )
        }

        return DynamicDataParserImpl(
            initParser,
            parserFactory,
            initFilePath,
            bestFileSearchService,
            fileNameManager,
            variableMapper,
            unitMapper,
            transformers
        )
    }
}