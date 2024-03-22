package com.luca009.imker.server.management.files

import com.luca009.imker.server.management.files.model.DataFileNameManager
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path

class DataFileNameManagerImpl(
    val prefix: String,
    val postfix: String,
    val dateFormat: String,
    val updateFrequency: Duration
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
        val durationSinceEpoch = Duration.between(Instant.EPOCH, utcTime)

        // Use integer division to our advantage and round down to the nearest multiple of updateFrequency
        val roundedDurationSinceEpoch = updateFrequency.multipliedBy(durationSinceEpoch.dividedBy(updateFrequency))

        // Convert our duration (since epoch) back to an instant
        val newInstant = Instant.EPOCH.plus(roundedDurationSinceEpoch)

        // Convert that instant to a ZonedDateTime at UTC
        return ZonedDateTime.ofInstant(newInstant, ZoneOffset.UTC)
    }
}
