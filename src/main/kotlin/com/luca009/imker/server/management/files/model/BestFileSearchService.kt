package com.luca009.imker.server.management.files.model

import org.springframework.context.annotation.Scope
 import java.time.Duration
import java.time.ZonedDateTime

@Scope("singleton")
interface BestFileSearchService {
    fun <T> getBestFile(files: Map<T, String?>, referenceDateTime: ZonedDateTime, fileNameManager: DataFileNameManager): Triple<T, ZonedDateTime, Duration>?
}

class BestFileNotFoundException(message: String? = null) : IllegalArgumentException(message)