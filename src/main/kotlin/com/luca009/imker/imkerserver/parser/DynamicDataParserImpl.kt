package com.luca009.imker.imkerserver.parser

import com.luca009.imker.imkerserver.filemanager.model.BestFileSearchService
import com.luca009.imker.imkerserver.filemanager.model.DataFileNameManager
import com.luca009.imker.imkerserver.parser.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.time.ZonedDateTime

class DynamicDataParserImpl(
    private var weatherDataParser: WeatherDataParser?,
    private val dataParserFactory: (String) -> WeatherDataParser,
    filePath: String,
    private val bestFileSearchService: BestFileSearchService,
    private val fileNameManager: DataFileNameManager
) : DynamicDataParser {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val baseFolder: File

    init {
        val folder = File(filePath)

        if (!folder.isDirectory) {
            logger.warn("Given file path was not a directory. Using parent directory instead.")
            baseFolder = folder.parentFile
        }
        else {
            baseFolder = folder
        }
    }

    override fun updateParser(dateTime: ZonedDateTime): Boolean {
        // TODO: This should search the folders recursively at some point for future weather models
        val fileMap = baseFolder
            .listFiles()
            ?.associateWith { it.name }

        requireNotNull(fileMap) {
            logger.error("Files in base folder were null. Did not update parser.")
            return false
        }

        val bestFile = bestFileSearchService.getBestFile(fileMap, dateTime, fileNameManager)

        requireNotNull(bestFile) {
            logger.error("No new best file could be determined. Did not update parser.")
            return false
        }

        val oldWeatherParser = weatherDataParser // keep a copy of this to close, just to be on the safe side with thread-safety
        weatherDataParser = dataParserFactory(bestFile.path)
        oldWeatherParser?.close()

        return true
    }

    override fun getDataSources() = weatherDataParser?.getDataSources() ?: setOf()
    override fun getAvailableRawVariables() = weatherDataParser?.getAvailableRawVariables() ?: setOf()
    override fun getRawVariable(name: String) = weatherDataParser?.getRawVariable(name)
    override fun getGridEntireSlice(name: String) = weatherDataParser?.getGridEntireSlice(name)
    override fun getGridTimeSlice(name: String, timeIndex: Int) = weatherDataParser?.getGridTimeSlice(name, timeIndex)
    override fun getGridTimeAnd2dPositionSlice(
        name: String,
        timeIndex: Int,
        coordinate: WeatherVariable2dCoordinate
    ) = weatherDataParser?.getGridTimeAnd2dPositionSlice(name, timeIndex, coordinate)
    override fun getGridTimeAnd3dPositionSlice(
        name: String,
        timeIndex: Int,
        coordinate: WeatherVariable3dCoordinate
    ) = weatherDataParser?.getGridTimeAnd3dPositionSlice(name, timeIndex, coordinate)

    override fun getTimes(name: String) = weatherDataParser?.getTimes(name)

    override fun gridTimeSliceExists(name: String, timeIndex: Int) = weatherDataParser?.gridTimeSliceExists(name, timeIndex) ?: false
    override fun gridTimeAnd2dPositionSliceExists(
        name: String,
        timeIndex: Int,
        coordinate: WeatherVariable2dCoordinate
    ) = weatherDataParser?.gridTimeAnd2dPositionSliceExists(name, timeIndex, coordinate) ?: false
    override fun gridTimeAnd3dPositionSliceExists(
        name: String,
        timeIndex: Int,
        coordinate: WeatherVariable3dCoordinate
    ) = weatherDataParser?.gridTimeAnd3dPositionSliceExists(name, timeIndex, coordinate) ?: false

    override fun containsTime(name: String, time: ZonedDateTime) = weatherDataParser?.containsTime(name, time) ?: false

    override fun containsLatLon(name: String, latitude: Double, longitude: Double) = weatherDataParser?.containsLatLon(name, latitude, longitude) ?: false
    override fun latLonToCoordinates(name: String, latitude: Double, longitude: Double) = weatherDataParser?.latLonToCoordinates(name, latitude, longitude)
    override fun close() {
        weatherDataParser?.close()
    }
}