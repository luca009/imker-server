package com.luca009.imker.server.receiver.ftp

import com.luca009.imker.server.receiver.model.DownloadResult
import com.luca009.imker.server.receiver.model.FtpClient
import com.luca009.imker.server.receiver.model.FtpClientConfiguration
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.FileOutputStream
import java.nio.file.Path
import kotlin.io.path.Path

@Component
class FtpClientImpl : FtpClient {
    private val ftpClient: FTPClient = FTPClient()
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun connect(serverUri: String, username: String, password: String): Boolean {
        return try {
            ftpClient.connect(serverUri)
            ftpClient.login(username, password)
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun connect(ftpClientConfiguration: FtpClientConfiguration): Boolean {
        return connect(
            ftpClientConfiguration.host,
            ftpClientConfiguration.username,
            ftpClientConfiguration.password
        )
    }

    override fun isConnected(): Boolean {
        return ftpClient.isConnected
    }

    override fun disconnect(): Boolean {
        return try {
            ftpClient.disconnect()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun listFiles(remotePath: String): Array<out FTPFile>? {
        if (!ftpClient.isConnected) {
            return null
        }

        return try {
            ftpClient.listFiles(remotePath)
        } catch (e: Exception) {
            null
        }
    }

    override fun listDirectories(remotePath: String): Array<out FTPFile>? {
        if (!ftpClient.isConnected) {
            return null
        }

        return try {
            ftpClient.listDirectories(remotePath)
        } catch (e: Exception) {
            null
        }
    }

    override fun downloadFile(remoteFilePath: String, downloadPath: Path, downloadName: String?): DownloadResult {
        if (!ftpClient.isConnected) {
            return DownloadResult(false, null)
        }

        // Janky way to fix Windows file paths, ughhhhh
        val remoteFilePath = remoteFilePath.replace('\\', '/')

        val remoteFileName = Path(remoteFilePath).fileName.toString()

        // Combine the downloadPath with the downloadName, substituting it for the remoteFileName if needed
        val outputFilePath = downloadPath.resolve(downloadName ?: remoteFileName)
        val outputFile = outputFilePath.toFile()

        return try {
            val ftpFileStream = ftpClient.retrieveFileStream(remoteFilePath)
            val localFileStream = FileOutputStream(outputFile)
            ftpFileStream.copyTo(localFileStream)
            val success = ftpClient.completePendingCommand()

            ftpFileStream.close()
            localFileStream.close()

            DownloadResult(success, downloadPath)
        } catch (e: Exception) {
            logger.error("Error while downloading $remoteFilePath: ${e.message}")

            DownloadResult(false, downloadPath)
        }
    }

}