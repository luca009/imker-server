package com.luca009.imker.server.filemanager

import com.luca009.imker.server.configuration.properties.StorageProperties
import com.luca009.imker.server.filemanager.model.LocalFileManagerService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import java.nio.file.Path
import kotlin.io.path.Path

@Service
@Scope("singleton")
class LocalFileManagerServiceImpl(
    val storageProperties: StorageProperties
) : LocalFileManagerService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun getWeatherDataLocation(storageLocation: String, subFolder: String?): Path {
        val preferredStorageLocationPath = storageProperties.storageLocations[storageLocation]

        val storageLocationPath =
            if (preferredStorageLocationPath == null) {
                logger.warn("$storageLocation is not a valid storage location. Defaulting to default storage location. Are the storage locations configured correctly?")
                storageProperties.storageLocations["default"]
            } else {
                preferredStorageLocationPath
            }

        requireNotNull(storageLocationPath) {
            throw IllegalArgumentException("There is no default storage location configured. Are the storage locations configured correctly?")
        }

        return if (subFolder == null) {
            Path(storageLocationPath)
        } else {
            Path(storageLocationPath, subFolder)
        }
    }
}