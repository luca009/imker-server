package com.luca009.imker.server.receiver

import com.luca009.imker.server.management.files.model.BestFileSearchService
import com.luca009.imker.server.management.files.model.DataFileNameManager
import com.luca009.imker.server.receiver.ftp.FtpClientImpl
import com.luca009.imker.server.receiver.ftp.FtpSingleFileReceiverImpl
import com.luca009.imker.server.receiver.model.DataReceiver
import com.luca009.imker.server.receiver.model.DataReceiverConfiguration
import com.luca009.imker.server.receiver.model.FtpClient
import com.luca009.imker.server.receiver.model.FtpSingleFileReceiver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
private class DataReceiverConfiguration(
    val bestFileSearchService: BestFileSearchService
) {
    val ftpClientsByHost: MutableMap<String, FtpClient> = mutableMapOf()

    @Bean
    fun dataReceiverFactory() = {
        receiverName: String, dataReceiverConfiguration: DataReceiverConfiguration, fileNameManager: DataFileNameManager -> dataReceiver(receiverName, dataReceiverConfiguration, fileNameManager)
    }

    fun dataReceiver(receiverName: String, dataReceiverConfiguration: DataReceiverConfiguration, fileNameManager: DataFileNameManager): DataReceiver? {
        return when (receiverName) {
            "ftpSingleFile" -> ftpSingleFileReceiver(dataReceiverConfiguration, fileNameManager) // TODO: add more receivers, including generic ones
            else -> null
        }
    }

    fun ftpSingleFileReceiver(dataReceiverConfiguration: DataReceiverConfiguration, fileNameManager: DataFileNameManager): FtpSingleFileReceiver {
        // Look up if we already have an FtpClient for this host, otherwise create a new one
        val ftpClient = ftpClientsByHost[dataReceiverConfiguration.ftpClientConfiguration.host] ?: ftpClient()

        return FtpSingleFileReceiverImpl(dataReceiverConfiguration, fileNameManager, bestFileSearchService, ftpClient)
    }

    @Bean
    fun ftpClient(): FtpClient {
        return FtpClientImpl()
    }
}