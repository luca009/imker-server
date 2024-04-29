package com.luca009.imker.server.transformer

import com.luca009.imker.server.transformer.model.DataTransformer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DataTransformerConfiguration {
    @Bean
    fun dataTransformerFactory() = {
        name: String -> dataTransformer(name)
    }

    fun dataTransformer(name: String): DataTransformer? {
        return when (name) {
            "desum" -> DesumTransformer()
            else -> null
        }
    }
}