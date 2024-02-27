package com.luca009.imker.server.receiver.model

import java.nio.file.Path

data class DownloadResult(
    val successful: Boolean,
    val fileLocation: Path?
)