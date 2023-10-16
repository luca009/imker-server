package com.luca009.imker.imkerserver.filemanager

import com.luca009.imker.imkerserver.filemanager.model.LocalFileManagerService
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import java.nio.file.Path
import kotlin.io.path.Path

@Service
@Scope("singleton")
class LocalFileManagerServiceImpl : LocalFileManagerService {
    override fun getWeatherDataLocation(subFolder: String): Path {
        return Path("C:\\Users\\reall\\Downloads\\incadata", subFolder)
    }
}