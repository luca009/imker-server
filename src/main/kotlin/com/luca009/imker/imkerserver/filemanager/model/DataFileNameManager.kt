package com.luca009.imker.imkerserver.filemanager.model

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Configuration
interface DataFileNameManager {
    @Bean
    public fun getFileNameForDateTime(dateTime: ZonedDateTime): String

    @Bean
    public fun getDateTimeForFileName(fileName: String): ZonedDateTime?

    @Bean
    public fun roundDownToNearestValidDateTime(dateTime: ZonedDateTime): ZonedDateTime
}

@Configuration
interface IncaFileNameManager : DataFileNameManager

@Configuration
interface AromeFileNameManager : DataFileNameManager