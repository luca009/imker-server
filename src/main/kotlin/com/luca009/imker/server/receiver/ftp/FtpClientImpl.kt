package com.luca009.imker.server.receiver.ftp

import com.luca009.imker.server.receiver.model.FtpClient
import com.luca009.imker.server.receiver.model.FtpClientConfiguration
import com.luca009.imker.server.receiver.model.FtpClientProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.flowOn
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPConnectionClosedException
import org.apache.commons.net.ftp.FTPFile
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path

@Component
class FtpClientImpl : FtpClient {
    private val ftpClient: FTPClient = FTPClient()

    override fun connect(serverUri: String, username: String, password: String) {
        ftpClient.connect(serverUri)
        ftpClient.login(username, password)
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
    }

    override fun connect(ftpClientConfiguration: FtpClientConfiguration) = connect(
        ftpClientConfiguration.host,
        ftpClientConfiguration.username,
        ftpClientConfiguration.password
    )

    override fun isConnected() = ftpClient.isConnected

    override fun disconnect() = ftpClient.disconnect()

    override fun listFiles(remotePath: String): Array<FTPFile> = ftpClient.listFiles(remotePath)

    override fun listDirectories(remotePath: String): Array<FTPFile> = ftpClient.listDirectories(remotePath)

    override suspend fun downloadFile(remoteFilePath: Path, downloadPath: Path, downloadName: String?): FtpClientProgress {
        if (!ftpClient.isConnected) {
            throw FTPConnectionClosedException("FTP Client was not connected")
        }

        val remoteFileName = remoteFilePath.fileName.toString()

        // Combine the downloadPath with the downloadName, substituting it for the remoteFileName if needed
        val outputFile = downloadPath.resolve(downloadName ?: remoteFileName)

        val remoteFilePathString = remoteFilePath.toString().replace('\\', '/') // Janky fix for Windows file paths :/

        return FtpClientProgress(
            downloadFileAsFlow(remoteFilePathString, outputFile.toFile()),
            outputFile
        )
    }

    private suspend fun downloadFileAsFlow(remoteFile: String, outputFile: File): Flow<Int?> = flow {
        val file = ftpClient.mlistFile(remoteFile)

        if (!file.isFile) {
            throw IllegalArgumentException("Specified file path did not resolve to a file")
        }

        val fileSize = file.size

        val ftpFileStream = ftpClient.retrieveFileStream(remoteFile)
        val localFileStream = FileOutputStream(outputFile)

        ftpFileStream
            .copyToAsFlow(localFileStream)
            .onCompletion {
                // Close the streams on completion
                ftpFileStream.close()
                localFileStream.close()
            }.collect {
                // Emit the percentage of copied bytes
                emit(it.approximatePercentageOf(fileSize).toInt())
            }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()


    // Courtesy of https://gist.github.com/naddeoa/4172e84aca533319a1cc5663d2fab39e?permalink_comment_id=4751332#gistcomment-4751332
    private fun InputStream.copyToAsFlow(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Flow<Long> {
        return flow {
            val buffer = ByteArray(bufferSize)
            var bytesCopied: Long = 0
            var bytes = read(buffer)

            while (bytes >= 0) {
                out.write(buffer, 0, bytes)
                bytesCopied += bytes

                // Emit stream progress as bytes copied
                emit(bytesCopied)
                bytes = read(buffer)
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun Long.approximatePercentageOf(max: Long) = (this.toDouble() / max.toDouble()) * 100
}