package com.luca009.imker.server.receiver.model

import kotlinx.coroutines.flow.Flow
import java.nio.file.Path
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Interface representing a data receiver, responsible for obtaining and updating weather data files.
 */
interface DataReceiver {
    /**
     * The receiver group this [DataReceiver] belongs to. Receivers in the same group will not be updated simultaneously.
     */
    val receiverGroup: String

    /**
     * Downloads the weather data for the specified [dateTime] and saves it with the name [downloadedFileName] (or uses default name if null).
     * If no file is found for the specified time is found, the next-earliest file will be chosen instead. If there is no earlier file available, no file is downloaded.
     * Returns a percentage flow of the progress.
     */
    suspend fun downloadData(dateTime: ZonedDateTime, downloadedFileName: String? = null, autoDisconnect: Boolean = false): Flow<Int?>

    /**
     * Disconnects from the remote data source.
     */
    fun disconnect()

    /**
     * Determines if an update is necessary, assuming [dateTime].
     */
    fun updateNecessary(dateTime: ZonedDateTime): Boolean
}

/**
 * An FTP receiver that is capable of obtaining a single file. This is useful if all weather data is consolidated in one file.
 */
interface FtpSingleFileReceiver : DataReceiver

/**
 * Configuration class for a [DataReceiver].
 */
data class DataReceiverConfiguration(
    /**
     * The name of the weather model. Used for logging.
     */
    val modelName: String,

    /**
     * How often the weather model is updated at the source (interval between updates).
     */
    val updateFrequency: Duration,

    /**
     * The location where to store the weather data.
     */
    val storageLocation: Path,

    /**
     * The receiver group this [DataReceiver] belongs to. Receivers in the same group will not be updated simultaneously.
     */
    val receiverGroup: String,
    val ftpClientConfiguration: FtpClientConfiguration, // TODO: #20: support different configurations
    val subFolder: String
)