package com.luca009.imker.server.parser

import com.luca009.imker.server.configuration.model.WeatherVariableFileNameMapper
import com.luca009.imker.server.configuration.model.WeatherVariableUnitMapper
import com.luca009.imker.server.parser.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ucar.nc2.Dimension
import ucar.nc2.constants.FeatureType
import ucar.nc2.dataset.NetcdfDataset
import ucar.nc2.dataset.NetcdfDatasets
import ucar.nc2.dt.GridDatatype
import ucar.nc2.dt.GridDataset
import ucar.nc2.ft.FeatureDataset
import ucar.nc2.ft.FeatureDatasetFactoryManager
import ucar.nc2.time.CalendarDate
import ucar.nc2.util.CancelTask
import java.nio.file.Path
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

class NetCdfParserImpl(
    netCdfFilePath: Path,
    private val variableMapper: WeatherVariableFileNameMapper,
    private val unitMapper: WeatherVariableUnitMapper
) : NetCdfParser {
    private val sourceFilePath: Path
    private val availableRawVariables: Set<RawWeatherVariable>
    private val availableVariables: Map<WeatherVariableType, WeatherVariable>
    private val dataset: NetcdfDataset
    private val wrappedDataset: FeatureDataset
    private val wrappedGridDataset: GridDataset?
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    init {
        val absoluteNetCdfFilePath = netCdfFilePath.toAbsolutePath()
        sourceFilePath = absoluteNetCdfFilePath

        dataset = openDataset(absoluteNetCdfFilePath.toString())
        wrappedDataset = wrapDataset(dataset)

        wrappedGridDataset =
            if (wrappedDataset.featureType == FeatureType.GRID)
                    (wrappedDataset as GridDataset)
            else
                null

        availableRawVariables = scanAvailableVariables().toSet()

        val weatherVariables = availableRawVariables.map {
            val variableTypes = variableMapper.getWeatherVariables(it.name)
            val unitEnum = unitMapper.getUnits(it.unitType)

            WeatherVariable(
                variableTypes,
                unitEnum,
                it.name,
                it.longName,
                it.dimensions
            )
        }

        availableVariables = weatherVariables.map { variable ->
            // Create a new map with each weather variable's variable types...
            variable.variableTypes.associateWith { variable }
        }.mergeMaps() // ...and merge them into one map. There should be no duplicate entries, assuming a correct configuration.
    }

    private fun <K, V> List<Map<K, V>>.mergeMaps(): Map<K, V> =
        mutableMapOf<K, V>().apply {
            for (innerMap in this@mergeMaps) putAll(innerMap)
        }

    private fun openDataset(netCdfFilePath: String): NetcdfDataset {
        return NetcdfDatasets.openDataset(netCdfFilePath)
    }

    private fun wrapDataset(dataset: NetcdfDataset): FeatureDataset {
        return FeatureDatasetFactoryManager.wrap(FeatureType.ANY, dataset, CancelTask.create(), null)
    }

    private fun scanAvailableVariables(): List<RawWeatherVariable> {
        return dataset.variables.mapNotNull {
            val name = it.name
            requireNotNull(name) { return@mapNotNull null }

            RawWeatherVariable(
                it.unitsString,
                name,
                it.fullName,
                it.dimensions.count(),
                it.dataType.name
            )
        }
    }

    private fun isInRange(dimension: Dimension?, index: Int): Boolean {
        requireNotNull(dimension) {
            return false
        }

        return index >= 0 && index < dimension.length
    }

    private fun isIn2dRange(weatherVariable: GridDatatype, timeIndex: Int, xIndex: Int, yIndex: Int): Boolean {
        return isInRange(weatherVariable.timeDimension, timeIndex) &&
                isInRange(weatherVariable.xDimension, xIndex) &&
                isInRange(weatherVariable.yDimension, yIndex)
    }

    private fun isIn3dRange(weatherVariable: GridDatatype, timeIndex: Int, xIndex: Int, yIndex: Int, zIndex: Int): Boolean {
        return isInRange(weatherVariable.timeDimension, timeIndex) &&
                isInRange(weatherVariable.xDimension, xIndex) &&
                isInRange(weatherVariable.yDimension, yIndex) &&
                isInRange(weatherVariable.zDimension, zIndex)
    }

    private fun getArrayFromAny(inputArray: Any?): Array<*>? {
        // why, Kotlin, why?!?!?!
        return when (inputArray) {
            is Array<*> -> inputArray
            is DoubleArray -> inputArray.toTypedArray()
            is FloatArray -> inputArray.toTypedArray()
            is IntArray -> inputArray.toTypedArray()
            is LongArray -> inputArray.toTypedArray()
            is ShortArray -> inputArray.toTypedArray()
            is ByteArray -> inputArray.toTypedArray()
            is BooleanArray -> inputArray.toTypedArray()
            else -> null
        }
    }

    private fun getGridset(variable: WeatherVariableType): GridDataset.Gridset? {
        val variableName = variableMapper.getWeatherVariableName(variable) ?: return null

        // Get the first gridset to contain a grid with our desired name
        return wrappedGridDataset?.gridsets?.find {
            it.grids?.find { it.name == variableName } != null
        }
    }

    private fun getGrid(variable: WeatherVariableType): GridDatatype? {
        val variableName = variableMapper.getWeatherVariableName(variable) ?: return null

        // Get the first gridset to contain a grid with our desired name
        return wrappedGridDataset?.grids?.find { it.name == variableName }
    }

    private fun getTimeSliceFromGrid(grid: GridDatatype, timeIndex: Int): WeatherVariableRasterSlice? {
        val volumeArray = grid.readVolumeData(timeIndex)

        val dataType = volumeArray.dataType
        val javaDataType = dataType.primitiveClassType.kotlin

        val uncheckedArray = volumeArray.get1DJavaArray(dataType)
        val castArray = getArrayFromAny(uncheckedArray)
        requireNotNull(castArray) {
            return null
        }

        return NetCdfWeatherVariableRasterSlice(
            javaDataType,
            castArray,
            grid.dimensions,
            grid.coordinateSystem.isLatLon
        )
    }

    override fun getAvailableRawVariables(): Set<RawWeatherVariable> {
        return availableRawVariables
    }

    override fun getDataSources(): Set<Path> {
        return setOf(sourceFilePath)
    }

    override fun getAvailableVariableTypes(): Set<WeatherVariableType> {
        return availableVariables.keys
    }

    override fun getAvailableVariables(): Set<WeatherVariable> {
        return availableVariables.values.toSet()
    }

    override fun getVariable(variableType: WeatherVariableType): WeatherVariable? {
        return availableVariables[variableType]
    }

    override fun getGridEntireSlice(variable: WeatherVariableType): WeatherVariableSlice? {
        val weatherVariable = getGrid(variable) ?: return null
        val allSlices: MutableList<WeatherVariableRasterSlice> = mutableListOf()

        for (x in 0 until weatherVariable.timeDimension.length) {
            val slice = getTimeSliceFromGrid(weatherVariable, x)
            requireNotNull(slice) {
                return null
            }

            allSlices.add(slice)
        }

        return WeatherVariableSlice(
            allSlices
        )
    }

    override fun getGridTimeSlice(variable: WeatherVariableType, timeIndex: Int): WeatherVariableRasterSlice? {
        val weatherVariable = getGrid(variable) ?: return null

        return if (isInRange(weatherVariable.timeDimension, timeIndex)) {
            getTimeSliceFromGrid(weatherVariable, timeIndex)
        } else {
            null
        }
    }

    override fun getGridTimeAnd2dPositionSlice(variable: WeatherVariableType, timeIndex: Int, coordinate: WeatherVariable2dCoordinate): Any? {
        val weatherVariable = getGrid(variable) ?: return null

        return if (isIn2dRange(weatherVariable, timeIndex, coordinate.xIndex, coordinate.yIndex)) {
            weatherVariable.readDataSlice(timeIndex, 0, coordinate.yIndex, coordinate.xIndex).getObject(0)
        } else {
            null
        }
    }

    override fun getGridTimeAnd3dPositionSlice(variable: WeatherVariableType, timeIndex: Int, coordinate: WeatherVariable3dCoordinate): Any? {
        val weatherVariable = getGrid(variable) ?: return null

        return if (isIn3dRange(weatherVariable, timeIndex, coordinate.xIndex, coordinate.yIndex, coordinate.zIndex)) {
            weatherVariable.readDataSlice(timeIndex, coordinate.zIndex, coordinate.yIndex, coordinate.xIndex).getObject(0)
        } else {
            null
        }
    }

    override fun getTimes(variable: WeatherVariableType): Set<Pair<Int, ZonedDateTime>>? {
        val gridset = getGridset(variable) ?: return null

        return gridset.geoCoordSystem.calendarDates.mapIndexed { index, date ->
            val utilDate = date.toDate()
            Pair(index, utilDate.toInstant().atZone(ZoneOffset.UTC))
        }.toSet()
    }

    override fun gridTimeSliceExists(variable: WeatherVariableType, timeIndex: Int): Boolean {
        val weatherVariable = getGrid(variable) ?: return false
        return isInRange(weatherVariable.timeDimension, timeIndex)
    }

    override fun gridTimeAnd2dPositionSliceExists(
        variable: WeatherVariableType,
        timeIndex: Int,
        coordinate: WeatherVariable2dCoordinate
    ): Boolean {
        val weatherVariable = getGrid(variable) ?: return false
        return isIn2dRange(weatherVariable, timeIndex, coordinate.xIndex, coordinate.yIndex)
    }

    override fun gridTimeAnd3dPositionSliceExists(
        variable: WeatherVariableType,
        timeIndex: Int,
        coordinate: WeatherVariable3dCoordinate
    ): Boolean {
        val weatherVariable = getGrid(variable) ?: return false
        return isIn3dRange(weatherVariable, timeIndex, coordinate.xIndex, coordinate.yIndex, coordinate.zIndex)
    }

    override fun containsTime(variable: WeatherVariableType, time: ZonedDateTime): Boolean {
        val gridset = getGridset(variable) ?: return false

        return try {
            val calendarDate = CalendarDate.of(Date.from(time.toInstant()))
            gridset.geoCoordSystem.timeAxis1D.hasCalendarDate(calendarDate)
        } catch (e: Exception) {
            logger.error("$sourceFilePath: $variable: could not determine if $time is contained in the gridset. ${e.message}")
            false
        }
    }

    override fun containsLatLon(variable: WeatherVariableType, latitude: Double, longitude: Double): Boolean {
        val gridset = getGridset(variable) ?: return false

        return try {
            val xy = gridset.geoCoordSystem.findXYindexFromLatLon(latitude, longitude, null)

            xy[0] >= 0 && xy[1] >= 0
        } catch (e: Exception) {
            logger.error("$sourceFilePath: $variable: could not project latlon coordinate to indices. ${e.message}")
            false
        }
    }

    override fun latLonToCoordinates(variable: WeatherVariableType, latitude: Double, longitude: Double): WeatherVariable2dCoordinate? {
        val gridset = getGridset(variable) ?: return null

        try {
            val xy = gridset.geoCoordSystem.findXYindexFromLatLon(latitude, longitude, null)

            if (xy[0] < 0 || xy[1] < 0) {
                logger.warn("$sourceFilePath: $variable: latlon coordinate (${xy[0]}, ${xy[1]}) was out of range.")
                return null
            }

            return WeatherVariable2dCoordinate(xy[0], xy[1])
        } catch (e: Exception) {
            logger.error("$sourceFilePath: $variable: could not project latlon coordinate to indices. ${e.message}")
            return null
        }
    }

    override fun close() {
        dataset.close()
    }
}