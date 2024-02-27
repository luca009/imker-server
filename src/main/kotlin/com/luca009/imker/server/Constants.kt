package com.luca009.imker.server

private object ZamgConstants {
    const val FTP_SERVER_ADDRESS = "eaftp.zamg.ac.at"
    const val FTP_SERVER_USERNAME = "anonymous"
    const val FTP_SERVER_PASSWORD = ""
}


// TODO: Replace the FtpServerConstants with a service
object IncaFtpServerConstants {
    const val ADDRESS = ZamgConstants.FTP_SERVER_ADDRESS
    const val USERNAME = ZamgConstants.FTP_SERVER_USERNAME
    const val PASSWORD = ZamgConstants.FTP_SERVER_PASSWORD
    const val SUB_FOLDER = "nowcast"
}

object AromeFtpServerConstants {
    const val ADDRESS = ZamgConstants.FTP_SERVER_ADDRESS
    const val USERNAME = ZamgConstants.FTP_SERVER_USERNAME
    const val PASSWORD = ZamgConstants.FTP_SERVER_PASSWORD
    const val SUB_FOLDER = "nwp"
}

object IncaFileNameConstants {
    const val FILE_NAME_PREFIX = "nowcast_"
    const val FILE_NAME_POSTFIX = ".nc"
    const val FILE_NAME_DATE_FORMAT = "yyyyMMddHHmm"
    const val DATA_UPDATE_FREQUENCY_MINS = 15
    const val FOLDER_NAME = "inca"
}

object AromeFileNameConstants {
    const val FILE_NAME_PREFIX = "nwp_"
    const val FILE_NAME_POSTFIX = ".nc"
    const val FILE_NAME_DATE_FORMAT = "yyyyMMddHH"
    const val DATA_UPDATE_FREQUENCY_HRS = 3
}