package com.luca009.imker.server.configuration

import com.luca009.imker.server.configuration.model.WeatherVariableUnitMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class WeatherVariableUnitMapperConfiguration {
    @Bean
    fun weatherVariableUnitMapperFactory() = {
            mappingFile: File -> weatherVariableUnitMapper(mappingFile)
    }

    fun weatherVariableUnitMapper(mappingFile: File): WeatherVariableUnitMapper {
        return WeatherVariableUnitMapperImpl(
            mappingFile
        )
    }
}