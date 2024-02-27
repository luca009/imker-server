package com.luca009.imker.server.filemanager

import com.luca009.imker.server.filemanager.model.DataFileNameManager
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.io.path.Path

class DataFileNameManagerImpl(
    val prefix: String,
    val postfix: String,
    val dateFormat: String,
    val updateFrequencyMins: Int
) : DataFileNameManager {
    override fun getFileNameForDateTime(dateTime: ZonedDateTime): String {
        val timeFormatter = DateTimeFormatter.ofPattern(dateFormat)
        val timeString = dateTime.format(timeFormatter)

        return prefix + timeString + postfix
    }

    override fun getDateTimeForFile(file: String): ZonedDateTime? {
        val fileNamePath = Path(file).fileName ?: return null
        val fileName = fileNamePath.toString()

        val dateString = fileName
            .removePrefix(prefix)
            .removeSuffix(postfix)

        val timeFormatter = DateTimeFormatter.ofPattern(dateFormat)

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

         if (updateFrequencyMins <= 60) {
            // Update frequency is less than an hour, round down to the nearest minute
            // Use integer division to our advantage and round down to the nearest multiple of updateFrequencyMins
            val newMinute = utcTime.minute / updateFrequencyMins * updateFrequencyMins

            val truncatedTime = utcTime.truncatedTo(ChronoUnit.HOURS)
            return truncatedTime.plusMinutes(newMinute.toLong())
        } else if (updateFrequencyMins <= 86400) {
            // Update frequency is more than an hour, but less than a day (86400 minutes), round down to the nearest hour
            // Integer division as above
            val newHour = utcTime.hour / (updateFrequencyMins / 60) * (updateFrequencyMins / 60)

            val truncatedTime = utcTime.truncatedTo(ChronoUnit.DAYS)
            return truncatedTime.plusHours(newHour.toLong())
        } else {
             // Update frequency is more than a day, round down to the nearest day
             // Integer division as above
             val newDay = utcTime.dayOfMonth / (updateFrequencyMins / 86400) * (updateFrequencyMins / 86400)

             val truncatedTime = utcTime.truncatedTo(ChronoUnit.MONTHS)
             return truncatedTime.plusDays(newDay.toLong())
         }
    }
}
