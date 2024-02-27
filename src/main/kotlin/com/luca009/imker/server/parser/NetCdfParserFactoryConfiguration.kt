package com.luca009.imker.server.parser

import com.luca009.imker.server.parser.model.NetCdfParser
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NetCdfParserFactoryConfiguration {
    @Bean
    fun netCdfParserFactory() = {
        netCdfFilePath: String -> netCdfParser(netCdfFilePath)
    }

     fun netCdfParser(netCdfFilePath: String): NetCdfParser {
        return NetCdfParserImpl(
            netCdfFilePath
        )
    }
}