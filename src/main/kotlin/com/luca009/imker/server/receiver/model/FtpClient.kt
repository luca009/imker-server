package com.luca009.imker.server.receiver.model

import kotlinx.coroutines.flow.Flow
import org.apache.commons.net.ftp.FTPFile
import java.nio.file.Path

interface FtpClient {
    /**
     * Connects to an FTP server with the given [serverUri], [username] and [password]. Default [username] and [password] should be fine for servers without any authentication.
     */
    fun connect(serverUri: String, username: String = "anonymous", password: String = "")

    /**
     * Connects to an FTP server with the given [ftpClientConfiguration]. Returns a boolean indicating whether the connection attempt was successful.
     */
    fun connect(ftpClientConfiguration: FtpClientConfiguration)

    fun isConnected(): Boolean

    fun disconnect()

    fun listFiles(remotePath: String): Array<out FTPFile> // TODO: Don't use FTPFile, make it implementation-agnostic

    fun listDirectories(remotePath: String): Array<out FTPFile>

    /**
     * Downloads a file from the FTP server from [remoteFilePath] and saves it to [downloadPath] with the filename [downloadName] (optional).
     * Returns an [FtpClientProgress] with the download path and a flow emitting progress in percent.
     */
    suspend fun downloadFile(remoteFilePath: Path, downloadPath: Path, downloadName: String? = null): FtpClientProgress
}

data class FtpClientProgress(
    val percentageFlow: Flow<Int?>,
    val downloadedFilePath: Path
)

data class FtpClientConfiguration(
    val host: String,
    val username: String = "anonymous",
    val password: String = ""
)