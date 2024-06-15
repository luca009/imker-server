package com.luca009.imker.server.parser

import com.luca009.imker.server.configuration.model.WeatherVariableTypeMapper
import com.luca009.imker.server.configuration.model.WeatherVariableUnitMapper
import com.luca009.imker.server.management.files.model.BestFileSearchService
import com.luca009.imker.server.management.files.model.DataFileNameManager
import com.luca009.imker.server.parser.model.*
import com.luca009.imker.server.transformer.model.DataTransformer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.time.ZonedDateTime
import kotlin.io.path.Path

class DynamicDataParserImpl(
    private var weatherDataParser: WeatherDataParser?,
    private val dataParserFactory: (Path, ZonedDateTime, WeatherVariableTypeMapper, WeatherVariableUnitMapper, Map<WeatherVariableType, List<DataTransformer>>) -> WeatherDataParser,
    filePath: Path,
    private val bestFileSearchService: BestFileSearchService,
    private val fileNameManager: DataFileNameManager,
    private val variableMapper: WeatherVariableTypeMapper,
    private val unitMapper: WeatherVariableUnitMapper,
    private val transformers: Map<WeatherVariableType, List<DataTransformer>>
) : DynamicDataParser {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val baseFolder: File

    init {
        val folder = filePath.toFile()

        baseFolder = if (!folder.isDirectory) {
            logger.warn("Given file path was not a directory. Using parent directory instead.")
            folder.parentFile
        } else {
            folder
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
        weatherDataParser = dataParserFactory(Path(bestFile.first.path), bestFile.second, variableMapper, unitMapper, transformers)
        oldWeatherParser?.close()

        return true
    }

    override fun getDataSources(): Map<Path, ZonedDateTime> = weatherDataParser?.getDataSources() ?: mapOf()
    override fun getAvailableVariableTypes(): Set<WeatherVariableType> = weatherDataParser?.getAvailableVariableTypes() ?: setOf()

    override fun getAvailableVariables() = weatherDataParser?.getAvailableVariables() ?: setOf()
    override fun getVariable(variableType: WeatherVariableType) = weatherDataParser?.getVariable(variableType)
    override fun getGridEntireSlice(variable: WeatherVariableType) = weatherDataParser?.getGridEntireSlice(variable)
    override fun getGridRasterSlice(variable: WeatherVariableType, time: ZonedDateTime) = weatherDataParser?.getGridRasterSlice(variable, time)

    override fun getGridTimeSeriesAt2dPosition(
        variable: WeatherVariableType,
        coordinate: WeatherVariable2dCoordinate,
        timeLimit: Int
    ) = weatherDataParser?.getGridTimeSeriesAt2dPosition(variable, coordinate, timeLimit)

    override fun getGridTimeSeriesAt3dPosition(
        variable: WeatherVariableType,
        coordinate: WeatherVariable3dCoordinate,
        timeLimit: Int
    ) = weatherDataParser?.getGridTimeSeriesAt3dPosition(variable, coordinate, timeLimit)

    override fun getGridTimeAnd2dPositionSlice(
        variable: WeatherVariableType,
        time: ZonedDateTime,
        coordinate: WeatherVariable2dCoordinate
    ) = weatherDataParser?.getGridTimeAnd2dPositionSlice(
        variable,
        time,
        coordinate
    )

    override fun getGridTimeAnd3dPositionSlice(
        variable: WeatherVariableType,
        time: ZonedDateTime,
        coordinate: WeatherVariable3dCoordinate
    ) = weatherDataParser?.getGridTimeAnd3dPositionSlice(
        variable,
        time,
        coordinate
    )

    override fun getTimes(variable: WeatherVariableType): List<ZonedDateTime>? = weatherDataParser?.getTimes(variable)
    override fun gridTimeSliceExists(variable: WeatherVariableType, time: ZonedDateTime) = weatherDataParser?.gridTimeSliceExists(variable, time) ?: false

    override fun gridTimeAnd2dPositionSliceExists(
        variable: WeatherVariableType,
        time: ZonedDateTime,
        coordinate: WeatherVariable2dCoordinate
    ) = weatherDataParser?.gridTimeAnd2dPositionSliceExists(
        variable,
        time,
        coordinate
    ) ?: false

    override fun gridTimeAnd3dPositionSliceExists(
        variable: WeatherVariableType,
        time: ZonedDateTime,
        coordinate: WeatherVariable3dCoordinate
    ) = weatherDataParser?.gridTimeAnd3dPositionSliceExists(
        variable,
        time,
        coordinate
    ) ?: false

    override fun containsLatLon(latitude: Double, longitude: Double, variable: WeatherVariableType?): Boolean = weatherDataParser?.containsLatLon(
        latitude,
        longitude,
        variable
    ) ?: false

    override fun latLonToCoordinates(variable: WeatherVariableType, latitude: Double, longitude: Double) = weatherDataParser?.latLonToCoordinates(variable, latitude, longitude)

    override fun close() {
        weatherDataParser?.close()
    }
}