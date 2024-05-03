package com.luca009.imker.server.receiver.model

import kotlinx.coroutines.flow.Flow
import java.nio.file.Path
import java.time.Duration
import java.time.ZonedDateTime

interface DataReceiver {
    val receiverGroup: String

    /**
     * Downloads the weather data for the specified [dateTime] and saves it with the name [downloadedFileName] (or uses default name if null).
     * If no file is found for the specified time is found, the next-earliest file will be chosen instead. If there is no earlier file available, no file is downloaded.
     * Returns a percentage flow of the progress.
     */
    suspend fun downloadData(dateTime: ZonedDateTime, downloadedFileName: String? = null, autoDisconnect: Boolean = false): Flow<Int?>

    fun disconnect()

    fun updateNecessary(dateTime: ZonedDateTime): Boolean
}

interface FtpSingleFileReceiver : DataReceiver

data class DataReceiverConfiguration(
    val modelName: String,
    val updateFrequency: Duration,
    val storageLocation: Path,
    val receiverGroup: String,
    val ftpClientConfiguration: FtpClientConfiguration, // TODO: support different configurations
    val subFolder: String
)