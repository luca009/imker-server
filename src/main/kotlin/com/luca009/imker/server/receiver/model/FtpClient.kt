package com.luca009.imker.server.receiver.model

import org.apache.commons.net.ftp.FTPFile
import java.nio.file.Path

interface FtpClient {
    /**
     * Connects to an FTP server with the given [serverUri], [username] and [password]. Default [username] and [password] should be fine for servers without any authentication.
     * Returns a boolean indicating whether the connection attempt was successful.
     */
    fun connect(serverUri: String, username: String = "anonymous", password: String = ""): Boolean

    /**
     * Connects to an FTP server with the given [ftpClientConfiguration]. Returns a boolean indicating whether the connection attempt was successful.
     */
    fun connect(ftpClientConfiguration: FtpClientConfiguration): Boolean

    fun isConnected(): Boolean

    fun disconnect(): Boolean

    fun listFiles(remotePath: String): Array<out FTPFile>?

    fun listDirectories(remotePath: String): Array<out FTPFile>?

    /**
     * Downloads a file from the FTP server from [remoteFilePath] and saves it to [downloadPath] with the filename [downloadName] (optional).
     * Returns a DownloadResult indicating whether the download was successful and the path to the downloaded file.
     */
    fun downloadFile(remoteFilePath: String, downloadPath: Path, downloadName: String?): DownloadResult
}

data class FtpClientConfiguration(
    val host: String,
    val username: String = "anonymous",
    val password: String = ""
)