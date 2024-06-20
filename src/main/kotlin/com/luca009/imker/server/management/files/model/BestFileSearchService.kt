package com.luca009.imker.server.management.files.model

import org.springframework.context.annotation.Scope
import java.time.Duration
import java.time.ZonedDateTime

@Scope("singleton")
interface BestFileSearchService {
    /**
     * Get the closest (but earlier) file compared to the [referenceDateTime].
     * [files] represents a [Map] with any key and a string as value, representing the file name, which gets processed with the [fileNameManager].
     */
    fun <T> getBestFile(files: Map<T, String?>, referenceDateTime: ZonedDateTime, fileNameManager: DataFileNameManager): Triple<T, ZonedDateTime, Duration>?
}

class BestFileNotFoundException(message: String? = null) : IllegalArgumentException(message)