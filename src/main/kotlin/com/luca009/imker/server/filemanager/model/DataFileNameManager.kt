package com.luca009.imker.server.filemanager.model

import java.time.ZonedDateTime

interface DataFileNameManager {
    fun getFileNameForDateTime(dateTime: ZonedDateTime): String

    fun getDateTimeForFile(file: String): ZonedDateTime?

    fun roundDownToNearestValidDateTime(dateTime: ZonedDateTime): ZonedDateTime
}