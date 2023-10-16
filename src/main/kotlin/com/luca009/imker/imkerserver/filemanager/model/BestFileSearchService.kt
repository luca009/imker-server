package com.luca009.imker.imkerserver.filemanager.model

import org.apache.commons.net.ftp.FTPFile
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import java.time.ZonedDateTime

@Scope("singleton")
interface BestFileSearchService {
    fun getBestFile(files: Array<out FTPFile>, referenceDateTime: ZonedDateTime, fileNameManager: DataFileNameManager): FTPFile?
}