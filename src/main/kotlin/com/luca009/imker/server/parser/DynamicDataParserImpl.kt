package com.luca009.imker.server.parser

import com.luca009.imker.server.configuration.model.WeatherVariableTypeMapper
import com.luca009.imker.server.configuration.model.WeatherVariableUnitMapper
import com.luca009.imker.server.management.files.model.BestFileSearchService
import com.luca009.imker.server.management.files.model.DataFileNameManager
import com.luca009.imker.server.parser.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.time.ZonedDateTime
import kotlin.io.path.Path

class DynamicDataParserImpl(
    private var weatherDataParser: WeatherDataParser?,
    private val dataParserFactory: (Path, WeatherVariableTypeMapper, WeatherVariableUnitMapper) -> WeatherDataParser,
    filePath: Path,
    private val bestFileSearchService: BestFileSearchService,
    private val fileNameManager: DataFileNameManager,
    private val variableMapper: WeatherVariableTypeMapper,
    private val unitMapper: WeatherVariableUnitMapper
) : DynamicDataParser {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val baseFolder: File

    init {
        val folder = filePath.toFile()

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
        weatherDataParser = dataParserFactory(Path(bestFile.path), variableMapper, unitMapper)
        oldWeatherParser?.close()

        return true
    }

    override fun getDataSources(): Set<Path> = weatherDataParser?.getDataSources() ?: setOf()
    override fun getAvailableVariableTypes(): Set<WeatherVariableType> = weatherDataParser?.getAvailableVariableTypes() ?: setOf()

    override fun getAvailableVariables() = weatherDataParser?.getAvailableVariables() ?: setOf()
    override fun getVariable(variableType: WeatherVariableType) = weatherDataParser?.getVariable(variableType)
    override fun getGridEntireSlice(variable: WeatherVariableType) = weatherDataParser?.getGridEntireSlice(variable)
    override fun getGridTimeSlice(variable: WeatherVariableType, timeIndex: Int) = weatherDataParser?.getGridTimeSlice(variable, timeIndex)
    override fun getGridTimeAnd2dPositionSlice(
        variable: WeatherVariableType,
        timeIndex: Int,
        coordinate: WeatherVariable2dCoordinate
    ) = weatherDataParser?.getGridTimeAnd2dPositionSlice(variable, timeIndex, coordinate)
    override fun getGridTimeAnd3dPositionSlice(
        variable: WeatherVariableType,
        timeIndex: Int,
        coordinate: WeatherVariable3dCoordinate
    ) = weatherDataParser?.getGridTimeAnd3dPositionSlice(variable, timeIndex, coordinate)

    override fun getTimes(variable: WeatherVariableType) = weatherDataParser?.getTimes(variable)

    override fun gridTimeSliceExists(variable: WeatherVariableType, timeIndex: Int) = weatherDataParser?.gridTimeSliceExists(variable, timeIndex) ?: false
    override fun gridTimeAnd2dPositionSliceExists(
        variable: WeatherVariableType,
        timeIndex: Int,
        coordinate: WeatherVariable2dCoordinate
    ) = weatherDataParser?.gridTimeAnd2dPositionSliceExists(variable, timeIndex, coordinate) ?: false
    override fun gridTimeAnd3dPositionSliceExists(
        variable: WeatherVariableType,
        timeIndex: Int,
        coordinate: WeatherVariable3dCoordinate
    ) = weatherDataParser?.gridTimeAnd3dPositionSliceExists(variable, timeIndex, coordinate) ?: false

    override fun containsTime(variable: WeatherVariableType, time: ZonedDateTime) = weatherDataParser?.containsTime(variable, time) ?: false

    override fun containsLatLon(variable: WeatherVariableType, latitude: Double, longitude: Double) = weatherDataParser?.containsLatLon(variable, latitude, longitude) ?: false
    override fun latLonToCoordinates(variable: WeatherVariableType, latitude: Double, longitude: Double) = weatherDataParser?.latLonToCoordinates(variable, latitude, longitude)
    override fun close() {
        weatherDataParser?.close()
    }
}