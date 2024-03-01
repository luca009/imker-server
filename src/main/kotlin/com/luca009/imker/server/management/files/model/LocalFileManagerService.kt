package com.luca009.imker.server.management.files.model

import java.nio.file.Path

interface LocalFileManagerService {
    fun getWeatherDataLocation(storageLocation: String, subFolder: String? = null): Path
}