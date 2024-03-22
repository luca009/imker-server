package com.luca009.imker.server.receiver.model

import java.time.ZonedDateTime

interface DataReceiver {
    /**
     * Downloads the weather data for the specified [dateTime] and saves it with the name [downloadedFileName] (or uses default name if null).
     * If no file is found for the specified time is found, the next-earliest file will be chosen instead. If there is no earlier file available, no file is downloaded.
     * Returns a DownloadResult indicating whether the operation was successful, as well as the file path, if applicable.
     *
     * Note: This function is blocking. It will halt the current thread's execution until all data is obtained.
     */
    fun downloadData(dateTime: ZonedDateTime, downloadedFileName: String?): DownloadResult

    fun updateNecessary(dateTime: ZonedDateTime): Boolean
}

interface FtpSingleFileReceiver : DataReceiver