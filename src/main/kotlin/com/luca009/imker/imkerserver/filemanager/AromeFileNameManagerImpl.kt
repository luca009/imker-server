package com.luca009.imker.imkerserver.filemanager

import com.luca009.imker.imkerserver.AromeFileNameConstants
import com.luca009.imker.imkerserver.filemanager.model.AromeFileNameManager
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class AromeFileNameManagerImpl : AromeFileNameManager {
    override fun getFileNameForDateTime(dateTime: ZonedDateTime): String {
        val timeFormatter = DateTimeFormatter.ofPattern(AromeFileNameConstants.FILE_NAME_DATE_FORMAT)
        val timeString = dateTime.format(timeFormatter)

        return AromeFileNameConstants.FILE_NAME_PREFIX + timeString + AromeFileNameConstants.FILE_NAME_POSTFIX
    }

    override fun getDateTimeForFile(file: String): ZonedDateTime? {
        val dateString = file
            .removePrefix(AromeFileNameConstants.FILE_NAME_PREFIX)
            .removeSuffix(AromeFileNameConstants.FILE_NAME_POSTFIX)

        val timeFormatter = DateTimeFormatter.ofPattern(AromeFileNameConstants.FILE_NAME_DATE_FORMAT)

        return try {
            LocalDateTime
                .parse(dateString, timeFormatter)
                .atZone(ZoneOffset.UTC)
        } catch (e: Exception) {
            null
        }
    }

    override fun roundDownToNearestValidDateTime(dateTime: ZonedDateTime): ZonedDateTime {
        val utcTime = dateTime.withZoneSameInstant(ZoneOffset.UTC)

        // Use integer division to our advantage and round down to the nearest multiple of DATA_UPDATE_FREQUENCY_HRS
        val newHour = utcTime.hour / AromeFileNameConstants.DATA_UPDATE_FREQUENCY_HRS * AromeFileNameConstants.DATA_UPDATE_FREQUENCY_HRS

        val truncatedTime = utcTime.truncatedTo(ChronoUnit.DAYS)
        return truncatedTime.plusHours(newHour.toLong())
    }
}