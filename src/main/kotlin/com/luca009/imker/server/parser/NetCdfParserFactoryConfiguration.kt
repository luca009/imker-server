package com.luca009.imker.server.parser

import com.luca009.imker.server.parser.model.NetCdfParser
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path

@Configuration
class NetCdfParserFactoryConfiguration {
    @Bean
    fun netCdfParserFactory() = {
        netCdfFilePath: Path -> netCdfParser(netCdfFilePath)
    }

     fun netCdfParser(netCdfFilePath: Path): NetCdfParser {
        return NetCdfParserImpl(
            netCdfFilePath
        )
    }
}