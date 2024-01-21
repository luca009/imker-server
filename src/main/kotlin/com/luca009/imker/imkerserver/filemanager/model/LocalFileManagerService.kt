package com.luca009.imker.imkerserver.filemanager.model

import java.nio.file.Path

interface LocalFileManagerService {
    fun getWeatherDataLocation(storageLocation: String, subFolder: String? = null): Path
}