package com.luca009.imker.server.configuration

import com.luca009.imker.server.configuration.model.WeatherVariableTypeMapper
import com.luca009.imker.server.parser.model.WeatherVariableType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WeatherVariableTypeMapperConfiguration {
    @Bean
    fun weatherVariableTypeMapperFactory() = {
            map: Map<WeatherVariableType, String> -> weatherVariableTypeMapper(map)
    }

    fun weatherVariableTypeMapper(map: Map<WeatherVariableType, String>): WeatherVariableTypeMapper {
        return WeatherVariableTypeMapperImpl(map)
    }
}