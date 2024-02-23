package com.luca009.imker.imkerserver.configuration

import com.luca009.imker.imkerserver.configuration.model.WeatherVariableUnitMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
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