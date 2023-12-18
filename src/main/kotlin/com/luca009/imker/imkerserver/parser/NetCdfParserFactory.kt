package com.luca009.imker.imkerserver.parser

import com.luca009.imker.imkerserver.parser.model.NetCdfParser
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Async

@Configuration
class NetCdfParserFactory {
    @Bean
    fun netCdfParserFactory() = {
            netCdfFilePath: String -> netCdfParser(netCdfFilePath)
    }

    @Bean
    @Scope("prototype")
    @Async
    fun netCdfParser(netCdfFilePath: String): NetCdfParser {
        return NetCdfParserImpl(
            netCdfFilePath
        )
    }

}