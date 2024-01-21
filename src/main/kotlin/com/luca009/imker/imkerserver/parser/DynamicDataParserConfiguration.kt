package com.luca009.imker.imkerserver.parser

import com.luca009.imker.imkerserver.filemanager.model.BestFileSearchService
import com.luca009.imker.imkerserver.filemanager.model.DataFileNameManager
import com.luca009.imker.imkerserver.parser.model.DynamicDataParser
import com.luca009.imker.imkerserver.parser.model.NetCdfParser
import com.luca009.imker.imkerserver.parser.model.WeatherDataParser
import com.luca009.imker.imkerserver.receiver.model.DataReceiver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope

@Configuration
class DynamicDataParserConfiguration {
    @Bean
    fun dynamicDataParserFactory(bestFileSearchService: BestFileSearchService) = {
        parserFactory: (String) -> WeatherDataParser, initFilePath: String, fileNameManager: DataFileNameManager -> dynamicDataParser(parserFactory, initFilePath, bestFileSearchService, fileNameManager)
    }

    @Bean
    @Scope("prototype")
    fun dynamicDataParser(
        parserFactory: (String) -> WeatherDataParser,
        initFilePath: String,
        bestFileSearchService: BestFileSearchService,
        fileNameManager: DataFileNameManager
    ): DynamicDataParser {
        return DynamicDataParserImpl(
            parserFactory(initFilePath),
            parserFactory,
            initFilePath,
            bestFileSearchService,
            fileNameManager
        )
    }
}