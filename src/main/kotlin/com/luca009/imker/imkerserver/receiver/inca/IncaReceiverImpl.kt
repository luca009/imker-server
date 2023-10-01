package com.luca009.imker.imkerserver.receiver.inca

import com.luca009.imker.imkerserver.IncaFileNameConstants
import com.luca009.imker.imkerserver.IncaFtpServerConstants
import com.luca009.imker.imkerserver.filemanager.model.IncaFileNameManager
import com.luca009.imker.imkerserver.filemanager.model.LocalFileManagerService
import com.luca009.imker.imkerserver.receiver.model.DownloadResult
import com.luca009.imker.imkerserver.receiver.model.FtpClient
import com.luca009.imker.imkerserver.receiver.model.IncaReceiver
import org.apache.commons.net.ftp.FTPFile
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.io.path.Path

class IncaReceiverImpl(val localFileManager: LocalFileManagerService,
                       val incaFileNameManager: IncaFileNameManager,
                       val ftpClient: FtpClient) : IncaReceiver {

    override fun downloadData(dateTime: ZonedDateTime, downloadedFileName: String?): DownloadResult {
        val dataLocation = localFileManager.getWeatherDataLocation(IncaFileNameConstants.FOLDER_NAME)
        val roundedDateTime = incaFileNameManager.roundDownToNearestValidDateTime(dateTime)

        val connectionSuccess = ftpClient.connect(IncaFtpServerConstants.ADDRESS, IncaFtpServerConstants.USERNAME, IncaFtpServerConstants.PASSWORD)
        if (!connectionSuccess) {
            return DownloadResult(false, null)
        }

        val availableFiles = ftpClient.listFiles(IncaFtpServerConstants.SUB_FOLDER)
            ?: return DownloadResult(false, null)

        val bestFile = getBestFile(availableFiles, roundedDateTime)
            ?: return DownloadResult(false, null)

        val filePath = Path(IncaFtpServerConstants.SUB_FOLDER, bestFile.name)
        return ftpClient.downloadFile(filePath.toString(), dataLocation, downloadedFileName ?: bestFile.name)
    }

    fun getBestFile(files: Array<out FTPFile>, referenceDateTime: ZonedDateTime): FTPFile? {
        var bestFile: FTPFile? = null
        var bestDifference: Duration? = null
        for (file in files) {
            if (file.name == null ||
                !file.isFile) {
                continue
            }

            val fileDateTime = incaFileNameManager.getDateTimeForFileName(file.name)

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