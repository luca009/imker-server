package com.luca009.imker.imkerserver.receiver.ftp

import com.luca009.imker.imkerserver.receiver.model.DownloadResult
import com.luca009.imker.imkerserver.receiver.model.FtpClient
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.springframework.stereotype.Component
import java.io.FileOutputStream
import java.nio.file.Path
import kotlin.io.path.Path

@Component
class FtpClientImpl : FtpClient {
    private val ftpClient: FTPClient = FTPClient()

    override fun connect(serverUri: String, username: String, password: String): Boolean {
        return try {
            ftpClient.connect(serverUri)
            ftpClient.login(username, password)
            true
        } catch (e: Exception) {
            false
        }
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

        val remoteFileName = Path(remoteFilePath).fileName.toString()

        // Combine the downloadPath with the downloadName, substituting it for the remoteFileName if needed
        val outputFilePath = downloadPath.resolve(downloadName ?: remoteFileName)
        val outputFile = outputFilePath.toFile()

        return try {
            ftpClient.retrieveFile(remoteFilePath, FileOutputStream(outputFile))

            DownloadResult(true, downloadPath)
        } catch (e: Exception) {
            DownloadResult(false, downloadPath)
        }
    }

}