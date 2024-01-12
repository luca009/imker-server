package com.luca009.imker.imkerserver.filemanager

import com.luca009.imker.imkerserver.IncaFileNameConstants
import com.luca009.imker.imkerserver.filemanager.model.IncaFileNameManager
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.io.path.Path

@Service
class IncaFileNameManagerImpl : IncaFileNameManager {
    override fun getFileNameForDateTime(dateTime: ZonedDateTime): String {
        val timeFormatter = DateTimeFormatter.ofPattern(IncaFileNameConstants.FILE_NAME_DATE_FORMAT)
        val timeString = dateTime.format(timeFormatter)

        return IncaFileNameConstants.FILE_NAME_PREFIX + timeString + IncaFileNameConstants.FILE_NAME_POSTFIX
    }

    override fun getDateTimeForFile(file: String): ZonedDateTime? {
        val fileNamePath = Path(file).fileName ?: return null
        val fileName = fileNamePath.toString()

        val dateString = fileName
            .removePrefix(IncaFileNameConstants.FILE_NAME_PREFIX)
            .removeSuffix(IncaFileNameConstants.FILE_NAME_POSTFIX)

        val timeFormatter = DateTimeFormatter.ofPattern(IncaFileNameConstants.FILE_NAME_DATE_FORMAT)

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

        // Use integer division to our advantage and round down to the nearest multiple of DATA_UPDATE_FREQUENCY_MINS
        val newMinute = utcTime.minute / IncaFileNameConstants.DATA_UPDATE_FREQUENCY_MINS * IncaFileNameConstants.DATA_UPDATE_FREQUENCY_MINS

        val truncatedTime = utcTime.truncatedTo(ChronoUnit.HOURS)
        return truncatedTime.plusMinutes(newMinute.toLong())
    }
}
