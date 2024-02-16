package com.luca009.imker.imkerserver.receiver

import com.luca009.imker.imkerserver.filemanager.model.BestFileSearchService
import com.luca009.imker.imkerserver.filemanager.model.DataFileNameManager
import com.luca009.imker.imkerserver.receiver.ftp.FtpClientImpl
import com.luca009.imker.imkerserver.receiver.inca.IncaReceiverImpl
import com.luca009.imker.imkerserver.receiver.model.DataReceiver
import com.luca009.imker.imkerserver.receiver.model.FtpClient
import com.luca009.imker.imkerserver.receiver.model.IncaReceiver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path

@Configuration
class DataReceiverConfiguration(
    val bestFileSearchService: BestFileSearchService
) {
    @Bean
    fun dataReceiverFactory() = {
            modelName: String, fileNameManager: DataFileNameManager, updateFrequencyMins: Int, storageLocation: Path -> dataReceiver(modelName, fileNameManager, updateFrequencyMins, storageLocation)
    }

    fun dataReceiver(name: String, fileNameManager: DataFileNameManager, updateFrequencyMins: Int, storageLocation: Path): DataReceiver? {
        return when (name) {
            "inca" -> incaReceiver(ftpClient(), fileNameManager, updateFrequencyMins, storageLocation) // TODO: add more receivers, including generic ones
            else -> null
        }
    }

    fun incaReceiver(ftpClient: FtpClient, fileNameManager: DataFileNameManager, updateFrequencyMins: Int, storageLocation: Path): IncaReceiver {
        return IncaReceiverImpl(fileNameManager, bestFileSearchService, ftpClient, updateFrequencyMins, storageLocation)
    }

    @Bean
    fun ftpClient(): FtpClient {
        return FtpClientImpl()
    }
}