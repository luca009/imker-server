package com.luca009.imker.imkerserver.filemanager

import com.luca009.imker.imkerserver.filemanager.model.BestFileSearchService
import com.luca009.imker.imkerserver.filemanager.model.DataFileNameManager
import org.apache.commons.net.ftp.FTPFile
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.ZonedDateTime

@Service
@Scope("singleton")
class BestFileSearchServiceImpl : BestFileSearchService {
    override fun getBestFile(
        files: Array<out FTPFile>,
        referenceDateTime: ZonedDateTime,
        fileNameManager: DataFileNameManager
    ): FTPFile? {
        var bestFile: FTPFile? = null
        var bestDifference: Duration? = null
        for (file in files) {
            if (file.name == null ||
                !file.isFile) {
                continue
            }

            val fileDateTime = fileNameManager.getDateTimeForFileName(file.name)

            // Calculate difference between reference time (goal) and the file's timestamp
            val dateTimeDifference = Duration.between(fileDateTime ?: continue, referenceDateTime)

            // Difference is negative, this file too young
            if (dateTimeDifference.isNegative) {
                continue
            }

            // Difference is 0 (perfect match), doesn't get any better than that
            if (dateTimeDifference.isZero) {
                bestFile = file
                bestDifference = dateTimeDifference
                break
            }

            // Difference is larger than the best one, skip
            if (bestDifference != null && dateTimeDifference > bestDifference) {
                continue
            }

            // This is the best file so far
            bestFile = file
            bestDifference = dateTimeDifference
        }

        return bestFile
    }
}