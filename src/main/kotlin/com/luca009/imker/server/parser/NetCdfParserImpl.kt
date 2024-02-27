package com.luca009.imker.server.parser

import com.luca009.imker.server.parser.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ucar.nc2.Dimension
import ucar.nc2.constants.FeatureType
import ucar.nc2.dataset.NetcdfDataset
import ucar.nc2.dataset.NetcdfDatasets
import ucar.nc2.dt.GridDatatype
import ucar.nc2.dt.grid.GridDataset
import ucar.nc2.ft.FeatureDataset
import ucar.nc2.ft.FeatureDatasetFactoryManager
import ucar.nc2.time.CalendarDate
import ucar.nc2.util.CancelTask
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import kotlin.io.path.Path

class NetCdfParserImpl(
    netCdfFilePath: String
) : NetCdfParser {
    private val sourceFilePath: String
    private val availableVariables: Map<String, RawWeatherVariable>
    private val dataset: NetcdfDataset
    private val wrappedDataset: FeatureDataset
    private val wrappedGridDataset: GridDataset?
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    init {
        val absoluteNetCdfFilePath = Path(netCdfFilePath).toAbsolutePath().toString()
        sourceFilePath = absoluteNetCdfFilePath

        dataset = openDataset(absoluteNetCdfFilePath)
        wrappedDataset = wrapDataset(dataset)
        availableVariables = scanAvailableVariables()

        wrappedGridDataset =
            if (wrappedDataset.featureType == FeatureType.GRID)
                    (wrappedDataset as GridDataset)
            else
                null
    }

    private fun openDataset(netCdfFilePath: String): NetcdfDataset {
        return NetcdfDatasets.openDataset(netCdfFilePath)
    }

    private fun wrapDataset(dataset: NetcdfDataset): FeatureDataset {
        return FeatureDatasetFactoryManager.wrap(FeatureType.ANY, dataset, CancelTask.create(), null)
    }

    private fun scanAvailableVariables(): Map<String, RawWeatherVariable> {
        return dataset.variables.mapNotNull {
            val name = it.name
            requireNotNull(name) { return@mapNotNull null }

            Pair(
                name,
                RawWeatherVariable(
                    it.unitsString,
                    name,
                    it.fullName,
                    it.dimensions.count(),
                    it.dataType.name
                )
            )
        }.toMap()
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
            grid.dimensions
        )
    }

    override fun getDataSources(): Set<String> {
        return setOf(sourceFilePath)
    }

    override fun getAvailableRawVariables(): Set<RawWeatherVariable> {
        return availableVariables.values.toSet()
    }

    override fun getRawVariable(name: String): RawWeatherVariable? {
        return availableVariables[name]
    }

    override fun getGridEntireSlice(name: String): WeatherVariableSlice? {
        val weatherVariable = wrappedGridDataset?.grids?.find { it.name == name }
        requireNotNull(weatherVariable) { return null }

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

    override fun getGridTimeSlice(name: String, timeIndex: Int): WeatherVariableRasterSlice? {
        val weatherVariable = wrappedGridDataset?.grids?.find { it.name == name }
        requireNotNull(weatherVariable) { return null }

        return if (isInRange(weatherVariable.timeDimension, timeIndex)) {
            getTimeSliceFromGrid(weatherVariable, timeIndex)
        } else {
            null
        }
    }

    override fun getGridTimeAnd2dPositionSlice(name: String, timeIndex: Int, coordinate: WeatherVariable2dCoordinate): Any? {
        val weatherVariable = wrappedGridDataset?.grids?.find { it.name == name }
        requireNotNull(weatherVariable) { return null }

        return if (isIn2dRange(weatherVariable, timeIndex, coordinate.xIndex, coordinate.yIndex)) {
            weatherVariable.readDataSlice(timeIndex, 0, coordinate.yIndex, coordinate.xIndex).getObject(0)
        } else {
            null
        }
    }

    override fun getGridTimeAnd3dPositionSlice(name: String, timeIndex: Int, coordinate: WeatherVariable3dCoordinate): Any? {
        val weatherVariable = wrappedGridDataset?.grids?.find { it.name == name }
        requireNotNull(weatherVariable) { return null }

        return if (isIn3dRange(weatherVariable, timeIndex, coordinate.xIndex, coordinate.yIndex, coordinate.zIndex)) {
            weatherVariable.readDataSlice(timeIndex, coordinate.zIndex, coordinate.yIndex, coordinate.xIndex).getObject(0)
        } else {
            null
        }
    }

    override fun getTimes(name: String): Set<Pair<Int, ZonedDateTime>>? {
        // Get the first gridset to contain a grid with our desired name
        val gridset = wrappedGridDataset?.gridsets?.find {
            it.grids?.find { it.name == name } != null
        }
        requireNotNull(gridset) { return null }

        return gridset.geoCoordSystem.calendarDates.mapIndexed { index, date ->
            val utilDate = date.toDate()
            Pair(index, utilDate.toInstant().atZone(ZoneOffset.UTC))
        }.toSet()
    }

    override fun gridTimeSliceExists(name: String, timeIndex: Int): Boolean {
        val weatherVariable = wrappedGridDataset?.grids?.find { it.name == name }
        requireNotNull(weatherVariable) { return false }

        return timeIndex >= 0 &&
                timeIndex < weatherVariable.timeDimension.length
    }

    override fun gridTimeAnd2dPositionSliceExists(
        name: String,
        timeIndex: Int,
        coordinate: WeatherVariable2dCoordinate
    ): Boolean {
        val weatherVariable = wrappedGridDataset?.grids?.find { it.name == name }
        requireNotNull(weatherVariable) { return false }

        return isIn2dRange(weatherVariable, timeIndex, coordinate.xIndex, coordinate.yIndex)
    }

    override fun gridTimeAnd3dPositionSliceExists(
        name: String,
        timeIndex: Int,
        coordinate: WeatherVariable3dCoordinate
    ): Boolean {
        val weatherVariable = wrappedGridDataset?.grids?.find { it.name == name }
        requireNotNull(weatherVariable) { return false }

        return isIn3dRange(weatherVariable, timeIndex, coordinate.xIndex, coordinate.yIndex, coordinate.zIndex)
    }

    override fun containsTime(name: String, time: ZonedDateTime): Boolean {
        // Get the first gridset to contain a grid with our desired name
        val gridset = wrappedGridDataset?.gridsets?.find {
            it.grids?.find { it.name == name } != null
        }
        requireNotNull(gridset) { return false }

        return try {
            val calendarDate = CalendarDate.of(Date.from(time.toInstant()))
            gridset.geoCoordSystem.timeAxis1D.hasCalendarDate(calendarDate)
        } catch (e: Exception) {
            logger.error("$sourceFilePath: $name: could not determine if $time is contained in the gridset. ${e.message}")
            false
        }
    }

    override fun containsLatLon(name: String, latitude: Double, longitude: Double): Boolean {
        // Get the first gridset to contain a grid with our desired name
        val gridset = wrappedGridDataset?.gridsets?.find {
            it.grids?.find { it.name == name } != null
        }
        requireNotNull(gridset) { return false }

        return try {
            val xy = gridset.geoCoordSystem.findXYindexFromLatLon(latitude, longitude, null)

            xy[0] >= 0 && xy[1] >= 0
        } catch (e: Exception) {
            logger.error("$sourceFilePath: $name: could not project latlon coordinate to indices. ${e.message}")
            false
        }
    }

    override fun latLonToCoordinates(name: String, latitude: Double, longitude: Double): WeatherVariable2dCoordinate? {
        // Get the first gridset to contain a grid with our desired name
        val gridset = wrappedGridDataset?.gridsets?.find {
            it.grids?.find { it.name == name } != null
        }
        requireNotNull(gridset) { return null }

        try {
            val xy = gridset.geoCoordSystem.findXYindexFromLatLon(latitude, longitude, null)

            if (xy[0] < 0 || xy[1] < 0) {
                logger.warn("$sourceFilePath: $name: latlon coordinate (${xy[0]}, ${xy[1]}) was out of range.")
                return null
            }

            return WeatherVariable2dCoordinate(xy[0], xy[1])
        } catch (e: Exception) {
            logger.error("$sourceFilePath: $name: could not project latlon coordinate to indices. ${e.message}")
            return null
        }
    }

    override fun close() {
        dataset.close()
    }
}