package com.luca009.imker.server.management.files.model

import java.nio.file.Path
import java.time.ZonedDateTime

/**
 * Interface for taking care of file name generation/parsing for weather models, as well as ensuring the validity of dates
 */
interface DataFileNameManager {
    /**
     * Get the file name associated with the specified [dateTime]
     */
    fun getFileNameForDateTime(dateTime: ZonedDateTime): String

    /**
     * Get the [ZonedDateTime] associated with the specified [file] name - or null if unparsable
     */
    fun getDateTimeForFile(file: Path): ZonedDateTime?

    /**
     * Round the specified [dateTime] down to the nearest valid one
     */
    fun roundDownToNearestValidDateTime(dateTime: ZonedDateTime): ZonedDateTime
}