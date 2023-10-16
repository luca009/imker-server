package com.luca009.imker.imkerserver.filemanager.model

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDateTime
import java.time.ZonedDateTime

interface DataFileNameManager {
    fun getFileNameForDateTime(dateTime: ZonedDateTime): String

    fun getDateTimeForFileName(fileName: String): ZonedDateTime?

    fun roundDownToNearestValidDateTime(dateTime: ZonedDateTime): ZonedDateTime
}

interface IncaFileNameManager : DataFileNameManager

interface AromeFileNameManager : DataFileNameManager