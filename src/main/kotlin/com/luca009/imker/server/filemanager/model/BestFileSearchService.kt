package com.luca009.imker.server.filemanager.model

import org.springframework.context.annotation.Scope
import java.time.ZonedDateTime

@Scope("singleton")
interface BestFileSearchService {
    fun <T> getBestFile(files: Map<T, String?>, referenceDateTime: ZonedDateTime, fileNameManager: DataFileNameManager): T?
}