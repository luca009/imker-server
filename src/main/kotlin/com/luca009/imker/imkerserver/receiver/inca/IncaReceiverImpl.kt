package com.luca009.imker.imkerserver.receiver.inca

import com.luca009.imker.imkerserver.IncaFtpServerConstants
import com.luca009.imker.imkerserver.filemanager.model.BestFileSearchService
import com.luca009.imker.imkerserver.filemanager.model.DataFileNameManager
import com.luca009.imker.imkerserver.receiver.model.DownloadResult
import com.luca009.imker.imkerserver.receiver.model.FtpClient
import com.luca009.imker.imkerserver.receiver.model.IncaReceiver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.io.path.Path

class IncaReceiverImpl(
    val fileNameManager: DataFileNameManager,
    val bestFileSearchService: BestFileSearchService,
    val ftpClient: FtpClient,
    val updateFrequencyMins: Int,
    val storageLocation: Path
    ) : IncaReceiver {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun downloadData(dateTime: ZonedDateTime, downloadedFileName: String?): DownloadResult {
        val dataLocation = storageLocation
        val roundedDateTime = fileNameManager.roundDownToNearestValidDateTime(dateTime)

        val connectionSuccess = ftpClient.connect(IncaFtpServerConstants.ADDRESS, IncaFtpServerConstants.USERNAME, IncaFtpServerConstants.PASSWORD)
        if (!connectionSuccess) {
            return DownloadResult(false, null)
        }

        val availableFiles = ftpClient.listFiles(IncaFtpServerConstants.SUB_FOLDER)
            ?: return DownloadResult(false, null)

        val availableFileNames = availableFiles
                .associateWith { it.name }
                .filterNot { it.value == null || !it.key.isFile }

        val bestFile = bestFileSearchService.getBestFile(availableFileNames, roundedDateTime, fileNameManager)
            ?: return DownloadResult(false, null)

        val filePath = Path(IncaFtpServerConstants.SUB_FOLDER, bestFile.name).toString()

        val downloadResult = ftpClient.downloadFile(filePath, dataLocation, downloadedFileName ?: bestFile.name)
        ftpClient.disconnect()
        return downloadResult
    }

    override fun updateNecessary(dateTime: ZonedDateTime): Boolean {
        // Do a local lookup to see if an update might be necessary
        val dataLocation = storageLocation.toFile()
        val roundedDateTime = fileNameManager.roundDownToNearestValidDateTime(dateTime)

        val availableFileNames = dataLocation.listFiles()?.associate { Pair(it, it.absolutePath) }

        requireNotNull(availableFileNames) { return true }

        val bestFile = bestFileSearchService.getBestFile(availableFileNames, roundedDateTime, fileNameManager) ?: return true
        val bestFileTime = fileNameManager.getDateTimeForFile(bestFile.name) ?: return true

        val bestFileTimeDifference = Duration.between(bestFileTime, dateTime)

        if (bestFileTimeDifference.isNegative) {
            logger.warn("Best downloaded INCA file was in the future")
        }

        // Calculate: (time since the last update) - (time between updates)
        // If this is negative, we haven't surpassed the update interval yet, so no update is necessary (return false)
        // If this is positive or zero, double-check online if a new file has been published yet
        if (bestFileTimeDifference.minus(updateFrequencyMins.toLong(), ChronoUnit.MINUTES).isNegative) {
            return false
        }

        // Check online
        val connectionSuccess = ftpClient.connect(IncaFtpServerConstants.ADDRESS, IncaFtpServerConstants.USERNAME, IncaFtpServerConstants.PASSWORD)
        if (!connectionSuccess) {
            return false
        }

        val availableOnlineFiles = ftpClient.listFiles(IncaFtpServerConstants.SUB_FOLDER)
            ?: return false

        val availableOnlineFileNames = availableOnlineFiles
            .associateWith { it.name }
            .filterNot { it.value == null || !it.key.isFile }

        val bestOnlineFile = bestFileSearchService.getBestFile(availableOnlineFileNames, roundedDateTime, fileNameManager)
            ?: return false

        // Note: we're only checking the difference between the local file's time and the remote file's time.
        // This is because using the current time to compare doesn't reflect the current state of our downloaded files.
        val bestOnlineFileTimeDifference = Duration.between(bestFileTime, fileNameManager.getDateTimeForFile(bestOnlineFile.name))
        if (bestOnlineFileTimeDifference.isNegative) {
            logger.warn("Best online INCA file was in the future")
        }

        ftpClient.disconnect()

        // Calculate: (time since the last update) - (time between updates)
        val bestOnlineFileTimeDifferenceMinusDataUpdateFrequency = bestOnlineFileTimeDifference.minus(updateFrequencyMins.toLong(), ChronoUnit.MINUTES)

        // If this is negative, the online file hasn't surpassed the update interval yet, so no update is necessary (return false)
        // If this is positive or zero, the online file is newer, return true
        return !bestOnlineFileTimeDifferenceMinusDataUpdateFrequency.isNegative
    }
}