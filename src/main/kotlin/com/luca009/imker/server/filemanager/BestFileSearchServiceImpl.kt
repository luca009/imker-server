package com.luca009.imker.server.filemanager

import com.luca009.imker.server.filemanager.model.BestFileSearchService
import com.luca009.imker.server.filemanager.model.DataFileNameManager
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.ZonedDateTime

@Service
@Scope("singleton")
class BestFileSearchServiceImpl : BestFileSearchService {
    override fun <T> getBestFile(
        files: Map<T, String?>,
        referenceDateTime: ZonedDateTime,
        fileNameManager: DataFileNameManager
    ): T? {
        var bestFile: T? = null
        var bestDifference: Duration? = null
        for (file in files) {
            val fileName = file.value ?: continue
            val fileDateTime = fileNameManager.getDateTimeForFile(fileName) ?: continue

            // Calculate difference between reference time (goal) and the file's timestamp
            val dateTimeDifference = Duration.between(fileDateTime, referenceDateTime)

            // Difference is negative, this file too young
            if (dateTimeDifference.isNegative) {
                continue
            }

            // Difference is 0 (perfect match), doesn't get any better than that
            if (dateTimeDifference.isZero) {
                bestFile = file.key
                bestDifference = dateTimeDifference
                break
            }

            // Difference is larger than the best one, skip
            if (bestDifference != null && dateTimeDifference > bestDifference) {
                continue
            }

            // This is the best file so far
            bestFile = file.key
            bestDifference = dateTimeDifference
        }

        return bestFile
    }
}