package com.luca009.imker.imkerserver.receiver.arome

import com.luca009.imker.imkerserver.receiver.model.AromeReceiver
import com.luca009.imker.imkerserver.receiver.model.DownloadResult
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class AromeReceiverImpl : AromeReceiver {
    override fun downloadData(dateTime: ZonedDateTime, downloadedFileName: String?): DownloadResult {
        TODO("Not yet implemented")
    }

    override fun updateNecessary(dateTime: ZonedDateTime): Boolean {
        TODO("Not yet implemented")
    }

}