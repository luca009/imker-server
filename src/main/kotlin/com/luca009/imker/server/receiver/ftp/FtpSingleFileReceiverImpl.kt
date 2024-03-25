package com.luca009.imker.server.receiver.ftp

import com.luca009.imker.server.management.files.model.BestFileNotFoundException
import com.luca009.imker.server.management.files.model.BestFileSearchService
import com.luca009.imker.server.management.files.model.DataFileNameManager
import com.luca009.imker.server.receiver.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.io.path.Path

class FtpSingleFileReceiverImpl(
    private val weatherModelName: String,
    private val fileNameManager: DataFileNameManager,
    private val bestFileSearchService: BestFileSearchService,
    private val ftpClient: FtpClient,
    private val ftpClientConfiguration: FtpClientConfiguration,
    private val subFolder: String,
    private val updateFrequency: Duration,
    private val storageLocation: Path
) : FtpSingleFileReceiver {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun downloadData(dateTime: ZonedDateTime, downloadedFileName: String?, autoDisconnect: Boolean): Flow<Int?> {
        val dataLocation = storageLocation
        val roundedDateTime = fileNameManager.roundDownToNearestValidDateTime(dateTime)

        if (!ftpClient.isConnected()) {
            ftpClient.connect(ftpClientConfiguration)
        }

        val availableFiles = ftpClient.listFiles(subFolder)

        val availableFileNames = availableFiles
            .associateWith { it.name }
            .filterNot { it.value == null || !it.key.isFile }

        val bestFile = bestFileSearchService.getBestFile(availableFileNames, roundedDateTime, fileNameManager)
            ?: throw BestFileNotFoundException("Best file could not be found")

        val filePath = Path(subFolder, bestFile.name)

        return ftpClient
            .downloadFile(filePath, dataLocation, downloadedFileName ?: bestFile.name)
            .percentageFlow
            .apply {
                this.onCompletion {
                    // Don't disconnect if autoDisconnect is off
                    if (!autoDisconnect) {
                        return@onCompletion
                    }

                    disconnect()
                }
            }

    }

    override fun disconnect() = ftpClient.disconnect()

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
            logger.warn("Best downloaded $weatherModelName file was in the future")
        }

        // Calculate: (time since the last update) - (time between updates)
        // If this is negative, we haven't surpassed the update interval yet, so no update is necessary (return false)
        // If this is positive or zero, double-check online if a new file has been published yet
        if (bestFileTimeDifference.minus(updateFrequency).isNegative) {
            return false
        }

        // Check online
        ftpClient.connect(ftpClientConfiguration)

        val availableOnlineFiles = ftpClient.listFiles(subFolder)
        val availableOnlineFileNames = availableOnlineFiles
            .associateWith { it.name }
            .filterNot { it.value == null || !it.key.isFile }

        val bestOnlineFile = bestFileSearchService.getBestFile(availableOnlineFileNames, roundedDateTime, fileNameManager)
            ?: return false

        // Note: we're only checking the difference between the local file's time and the remote file's time.
        // This is because using the current time to compare doesn't reflect the current state of our downloaded files.
        val bestOnlineFileTimeDifference = Duration.between(bestFileTime, fileNameManager.getDateTimeForFile(bestOnlineFile.name))
        if (bestOnlineFileTimeDifference.isNegative) {
            logger.warn("Best online $weatherModelName file was in the future")
        }

        ftpClient.disconnect()

        // Calculate: (time since the last update) - (time between updates)
        val bestOnlineFileTimeDifferenceMinusDataUpdateFrequency = bestOnlineFileTimeDifference.minus(updateFrequency)

        // If this is negative, the online file hasn't surpassed the update interval yet, so no update is necessary (return false)
        // If this is positive or zero, the online file is newer, return true
        return !bestOnlineFileTimeDifferenceMinusDataUpdateFrequency.isNegative
    }
}