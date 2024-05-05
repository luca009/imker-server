package com.luca009.imker.server.management.files

import com.luca009.imker.server.management.files.model.BestFileSearchService
import com.luca009.imker.server.management.files.model.DataFileNameManager
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.ZonedDateTime
 import kotlin.io.path.Path

@Service
@Scope("singleton")
class BestFileSearchServiceImpl : BestFileSearchService {
    override fun <T> getBestFile(
        files: Map<T, String?>,
        referenceDateTime: ZonedDateTime,
        fileNameManager: DataFileNameManager
    ): Triple<T, ZonedDateTime, Duration>? {
        val bestFileAndDate = files.mapNotNull {
            val fileName = Path(it.value ?: return@mapNotNull null)
            val fileDateTime = fileNameManager.getDateTimeForFile(fileName) ?: return@mapNotNull null

            // Calculate difference between reference time (goal) and the file's timestamp
            val dateTimeDifference = Duration.between(fileDateTime, referenceDateTime)

            if (dateTimeDifference.isNegative) {
                // Difference is negative, this file too young
                return@mapNotNull null
            } else {
                // Note down the difference
                Triple(it.key, fileDateTime, dateTimeDifference)
            }
        }.minByOrNull {
            it.third
        }

        return bestFileAndDate
    }
}