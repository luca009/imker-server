package com.luca009.imker.imkerserver.receiver.inca

import com.luca009.imker.imkerserver.IncaFileNameConstants
import com.luca009.imker.imkerserver.IncaFtpServerConstants
import com.luca009.imker.imkerserver.filemanager.model.BestFileSearchService
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
                       val bestFileSearchService: BestFileSearchService,
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

        val bestFile = bestFileSearchService.getBestFile(availableFiles, roundedDateTime, incaFileNameManager)
            ?: return DownloadResult(false, null)

        val filePath = Path(IncaFtpServerConstants.SUB_FOLDER, bestFile.name)
        return ftpClient.downloadFile(filePath.toString(), dataLocation, downloadedFileName ?: bestFile.name)
    }
}