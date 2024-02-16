package com.luca009.imker.imkerserver.filemanager

import com.luca009.imker.imkerserver.IncaFileNameConstants
import com.luca009.imker.imkerserver.filemanager.model.DataFileNameManager
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
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

        // Use integer division to our advantage and round down to the nearest multiple of updateFrequencyMins
        val newMinute = utcTime.minute / updateFrequencyMins * updateFrequencyMins

        val truncatedTime = utcTime.truncatedTo(ChronoUnit.HOURS)
        return truncatedTime.plusMinutes(newMinute.toLong())
    }
}
