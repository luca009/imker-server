package com.luca009.imker.imkerserver.receiver.model

import org.apache.commons.net.ftp.FTPFile
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path

@Configuration
interface FtpClient {
    /**
     * Connects to an FTP server with the given [serverUri], [username] and [password]. Default [username] and [password] should be fine for servers without any authentication.
     * Returns a boolean indicating whether the connection attempt was successful.
     */
    @Bean
    fun connect(serverUri: String, username: String = "anonymous", password: String = ""): Boolean

    @Bean
    fun isConnected(): Boolean

    @Bean
    fun disconnect(): Boolean

    @Bean
    fun listFiles(remotePath: String): Array<out FTPFile>?

    @Bean
    fun listDirectories(remotePath: String): Array<out FTPFile>?

    /**
     * Downloads a file from the FTP server from [remoteFilePath] and saves it to [downloadPath] with the filename [downloadName] (optional).
     * Returns a DownloadResult indicating whether the download was successful and the path to the downloaded file.
     */
    @Bean
    fun downloadFile(remoteFilePath: String, downloadPath: Path, downloadName: String?): DownloadResult
}