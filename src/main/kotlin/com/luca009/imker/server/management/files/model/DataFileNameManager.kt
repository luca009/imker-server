package com.luca009.imker.server.management.files.model

import java.nio.file.Path
import java.time.ZonedDateTime

interface DataFileNameManager {
    fun getFileNameForDateTime(dateTime: ZonedDateTime): String

    fun getDateTimeForFile(file: Path): ZonedDateTime?

    fun roundDownToNearestValidDateTime(dateTime: ZonedDateTime): ZonedDateTime
}