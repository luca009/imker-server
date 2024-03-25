package com.luca009.imker.server.parser

import com.luca009.imker.server.management.files.model.BestFileSearchService
import com.luca009.imker.server.management.files.model.DataFileNameManager
import com.luca009.imker.server.parser.model.DynamicDataParser
import com.luca009.imker.server.parser.model.WeatherDataParser
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.io.path.Path
import kotlin.io.path.isDirectory

@Configuration
class DynamicDataParserConfiguration {
    @Bean
    fun dynamicDataParserFactory(bestFileSearchService: BestFileSearchService) = {
        parserFactory: (String) -> WeatherDataParser, initFilePath: String, fileNameManager: DataFileNameManager -> dynamicDataParser(parserFactory, initFilePath, bestFileSearchService, fileNameManager)
    }

     fun dynamicDataParser(
         parserFactory: (String) -> WeatherDataParser,
         initFilePath: String,
         bestFileSearchService: BestFileSearchService,
         fileNameManager: DataFileNameManager
    ): DynamicDataParser {
        val initParser = if (Path(initFilePath).isDirectory()) {
            null
        } else {
            parserFactory(initFilePath)
        }

        return DynamicDataParserImpl(
            initParser,
            parserFactory,
            initFilePath,
            bestFileSearchService,
            fileNameManager
        )
    }
}