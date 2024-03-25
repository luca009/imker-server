package com.luca009.imker.server.receiver

import com.luca009.imker.server.management.files.model.BestFileSearchService
import com.luca009.imker.server.management.files.model.DataFileNameManager
import com.luca009.imker.server.receiver.ftp.FtpClientImpl
import com.luca009.imker.server.receiver.ftp.FtpSingleFileReceiverImpl
import com.luca009.imker.server.receiver.model.DataReceiver
import com.luca009.imker.server.receiver.model.FtpClient
import com.luca009.imker.server.receiver.model.FtpClientConfiguration
import com.luca009.imker.server.receiver.model.FtpSingleFileReceiver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path
import java.time.Duration

@Configuration
class DataReceiverConfiguration(
    val bestFileSearchService: BestFileSearchService
) {
    val ftpClientsByHost: MutableMap<String, FtpClient> = mutableMapOf()

    @Bean
    fun dataReceiverFactory() = {
            modelName: String, receiverName: String, ftpClientConfiguration: FtpClientConfiguration, subFolder: String, fileNameManager: DataFileNameManager, updateFrequency: Duration, storageLocation: Path -> dataReceiver(modelName, receiverName, ftpClientConfiguration, subFolder, fileNameManager, updateFrequency, storageLocation)
    }

    fun dataReceiver(modelName: String, receiverName: String, ftpClientConfiguration: FtpClientConfiguration, subFolder: String, fileNameManager: DataFileNameManager, updateFrequency: Duration, storageLocation: Path): DataReceiver? {
        return when (receiverName) {
            "ftpSingleFile" -> ftpSingleFileReceiver(modelName, ftpClientConfiguration, subFolder, fileNameManager, updateFrequency, storageLocation) // TODO: add more receivers, including generic ones
            else -> null
        }
    }

    fun ftpSingleFileReceiver(weatherModelName: String, ftpClientConfiguration: FtpClientConfiguration, subFolder: String, fileNameManager: DataFileNameManager, updateFrequency: Duration, storageLocation: Path): FtpSingleFileReceiver {
        // Look up if we already have an FtpClient for this host, otherwise create a new one
        val ftpClient = ftpClientsByHost[ftpClientConfiguration.host] ?: ftpClient()

        return FtpSingleFileReceiverImpl(weatherModelName, fileNameManager, bestFileSearchService, ftpClient, ftpClientConfiguration, subFolder, updateFrequency, storageLocation)
    }

    @Bean
    fun ftpClient(): FtpClient {
        return FtpClientImpl()
    }
}