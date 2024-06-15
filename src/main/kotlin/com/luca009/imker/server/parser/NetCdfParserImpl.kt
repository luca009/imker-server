package com.luca009.imker.server.parser

import com.luca009.imker.server.configuration.model.WeatherVariableTypeMapper
import com.luca009.imker.server.configuration.model.WeatherVariableUnitMapper
import com.luca009.imker.server.parser.model.*
import com.luca009.imker.server.transformer.DataTransformerHelper.chainedTransformPoint
import com.luca009.imker.server.transformer.DataTransformerHelper.chainedTransformRaster
import com.luca009.imker.server.transformer.DataTransformerHelper.chainedTransformSlice
import com.luca009.imker.server.transformer.DataTransformerHelper.chainedTransformTimeSeries
import com.luca009.imker.server.transformer.model.DataTransformer
import com.luca009.imker.server.transformer.model.PointDataTransformer
import com.luca009.imker.server.transformer.model.RasterDataTransformer
import com.luca009.imker.server.transformer.model.TimeDataTransformer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ucar.nc2.Dimension
import ucar.nc2.constants.FeatureType
import ucar.nc2.dataset.NetcdfDataset
import ucar.nc2.dataset.NetcdfDatasets
import ucar.nc2.dt.GridDatatype
import ucar.nc2.dt.GridDataset
import ucar.nc2.dt.GridDataset.Gridset
import ucar.nc2.ft.FeatureDataset
import ucar.nc2.ft.FeatureDatasetFactoryManager
import ucar.nc2.util.CancelTask
import java.nio.file.Path
import java.time.ZoneOffset
import java.time.ZonedDateTime

class NetCdfParserImpl(
    netCdfFilePath: Path,
    netCdfFileDate: ZonedDateTime,
    private val variableMapper: WeatherVariableTypeMapper,
    private val unitMapper: WeatherVariableUnitMapper,
    private val transformers: Map<WeatherVariableType, List<DataTransformer>> = mapOf()
) : NetCdfParser {
    private val sourceFilePath: Path
    private val sourceFileDate: ZonedDateTime = netCdfFileDate

    private val availableRawVariables: Set<RawWeatherVariable>
    private val availableVariables: Map<WeatherVariableType, WeatherVariable>
    private val availableTimes: Map<Gridset, List<ZonedDateTime>>

    private val dataset: NetcdfDataset
    private val wrappedDataset: FeatureDataset
    private val wrappedGridDataset: GridDataset?

    private val rasterTransformers = transformers.mapValues { it.value.filterIsInstance<RasterDataTransformer>() }
    private val timeTransformer = transformers.mapValues { it.value.filterIsInstance<TimeDataTransformer>() }
    private val pointTransformers = transformers.mapValues { it.value.filterIsInstance<PointDataTransformer>() }

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

        availableTimes = getAllTimes()
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

    private fun getAllTimes(): Map<Gridset, List<ZonedDateTime>> {
        return wrappedGridDataset!!.gridsets!!.associateWith {
            val rawDates = it.geoCoordSystem.calendarDates

            rawDates.mapNotNull { date ->
                val utilDate = date?.toDate()
                utilDate?.toInstant()?.atZone(ZoneOffset.UTC)
            }
        }
    }

    private fun isInRange(dimension: Dimension?, index: Int): Boolean {
        requireNotNull(dimension) {
            return false
        }

        return index in 0..dimension.length
    }

    private fun isIn2dRange(gridset: Gridset, grid: GridDatatype, time: ZonedDateTime, coordinate: WeatherVariable2dCoordinate): Boolean {
        val times = availableTimes[gridset] ?: return false
        return times.contains(time) &&
                isInRange(grid.xDimension, coordinate.xIndex) &&
                isInRange(grid.yDimension, coordinate.yIndex)
    }

    private fun isIn2dRange(grid: GridDatatype, timeIndex: Int, coordinate: WeatherVariable2dCoordinate): Boolean {
        return isInRange(grid.timeDimension, timeIndex) &&
                isInRange(grid.xDimension, coordinate.xIndex) &&
                isInRange(grid.yDimension, coordinate.yIndex)
    }

    private fun isIn3dRange(gridset: Gridset, grid: GridDatatype, time: ZonedDateTime, coordinate: WeatherVariable3dCoordinate): Boolean {
        val times = availableTimes[gridset] ?: return false
        return times.contains(time) &&
                isInRange(grid.xDimension, coordinate.xIndex) &&
                isInRange(grid.yDimension, coordinate.yIndex) &&
                isInRange(grid.zDimension, coordinate.zIndex)
    }

    private fun isIn3dRange(grid: GridDatatype, timeIndex: Int, coordinate: WeatherVariable3dCoordinate): Boolean {
        return isInRange(grid.timeDimension, timeIndex) &&
                isInRange(grid.xDimension, coordinate.xIndex) &&
                isInRange(grid.yDimension, coordinate.yIndex) &&
                isInRange(grid.zDimension, coordinate.zIndex)
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

    private fun getGridsetAndGrid(variable: WeatherVariableType): Pair<Gridset, GridDatatype>? {
        val variableName = variableMapper.getWeatherVariableName(variable) ?: return null

        // Get the first gridset to contain a grid with our desired name
        wrappedGridDataset?.gridsets?.forEach {
            val grid = it.grids?.find { it.name == variableName } ?: return@forEach

            return Pair(
                it,
                grid
            )
        }

        return null
    }

    private fun getGridset(variable: WeatherVariableType): Gridset? {
        return getGridsetAndGrid(variable)?.first
    }

    private fun getTimeSliceFromGrid(grid: GridDatatype, timeIndex: Int, unit: WeatherVariableUnit?): WeatherVariableRasterSlice? {
        val volumeArray = grid.readVolumeData(timeIndex)

        val dataType = volumeArray.dataType
        val javaDataType = dataType.primitiveClassType.kotlin

        val uncheckedArray = volumeArray.get1DJavaArray(dataType)
        val castArray = getArrayFromAny(uncheckedArray)
        requireNotNull(castArray) {
            return null
        }

        return NetCdfWeatherVariableRasterSlice(
            unit,
            javaDataType,
            castArray,
            grid.dimensions,
            grid.coordinateSystem.isLatLon
        )
    }

    override fun getAvailableRawVariables(): Set<RawWeatherVariable> {
        return availableRawVariables
    }

    override fun getDataSources(): Map<Path, ZonedDateTime> {
        return mapOf(
            sourceFilePath to sourceFileDate
        )
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

    override fun getGridEntireSlice(variable: WeatherVariableType): WeatherVariableTimeRasterSlice? {
        val weatherVariable = availableVariables[variable] ?: return null
        val (variableGridset, variableGrid) = getGridsetAndGrid(variable) ?: return null
        val times = availableTimes[variableGridset] ?: return null

        val slices = (0 until variableGrid.timeDimension.length).map {
            val slice = getTimeSliceFromGrid(variableGrid, it, weatherVariable.unitType) ?: return null

            Pair(
                times[it],
                slice
            )
        }.toMap()

        val mergedSlice = WeatherVariableTimeRasterSlice(slices)

        val transformers = transformers[variable]
        return if (transformers == null) {
            // No applicable transformers, return data as-is
            mergedSlice
        } else {
            // Transform our slice :D
            transformers.chainedTransformSlice(mergedSlice)
        }
    }

    override fun getGridRasterSlice(variable: WeatherVariableType, time: ZonedDateTime): WeatherVariableRasterSlice? {
        val weatherVariable = availableVariables[variable] ?: return null
        val (variableGridset, variableGrid) = getGridsetAndGrid(variable) ?: return null

        val timeIndex = availableTimes[variableGridset]?.indexOf(time)
        require(timeIndex != null && timeIndex >= 0) {
            return null
        }

        if (!isInRange(variableGrid.timeDimension, timeIndex)) {
            return null
        }

        val rasterSlice = getTimeSliceFromGrid(variableGrid, timeIndex, weatherVariable.unitType) ?: return null

        val transformers = rasterTransformers[variable]
        return if (transformers == null) {
            // No applicable transformers, return data as-is
            rasterSlice
        } else {
            // Transform our raster :D
            transformers.chainedTransformRaster(rasterSlice)
        }
    }

    override fun getGridTimeSeriesAt2dPosition(
        variable: WeatherVariableType,
        coordinate: WeatherVariable2dCoordinate,
        timeLimit: Int
    ): WeatherVariableTimeSlice? = getGridTimeSeriesAt3dPosition(
        variable,
        WeatherVariable3dCoordinate(
            coordinate.xIndex, coordinate.yIndex, 0
        ),
        timeLimit
    )

    override fun getGridTimeSeriesAt3dPosition(
        variable: WeatherVariableType,
        coordinate: WeatherVariable3dCoordinate,
        timeLimit: Int
    ): WeatherVariableTimeSlice? {
        val weatherVariable = availableVariables[variable] ?: return null
        val (variableGridset, variableGrid) = getGridsetAndGrid(variable) ?: return null

        val timeDepth = if (timeLimit < 0) {
            variableGrid.timeDimension.length
        } else {
            timeLimit.coerceAtMost(variableGrid.timeDimension.length)
        }

        val dataType = variableGrid.dataType.primitiveClassType.kotlin
        val values = (0 until timeDepth).map {
            Pair(
                variableGridset.getTime(it) ?: return null,
                variableGrid.readDataSlice(it, coordinate.zIndex, coordinate.yIndex, coordinate.xIndex).getObject(0)
            )
        }.toMap()

        val timeSlice = WeatherVariableTimeSlice(
            values,
            dataType,
            weatherVariable.unitType
        )

        val transformers = timeTransformer[variable]
        return if (transformers == null) {
            // No applicable transformers, return data as-is
            timeSlice
        } else {
            // Transform our series :D
            transformers.chainedTransformTimeSeries(timeSlice)
        }
    }

    override fun getGridTimeAnd2dPositionSlice(
        variable: WeatherVariableType,
        time: ZonedDateTime,
        coordinate: WeatherVariable2dCoordinate
    ): Any? {
        val (variableGridset, variableGrid) = getGridsetAndGrid(variable) ?: return null
        val timeIndex = availableTimes[variableGridset]?.indexOf(time) ?: return null

        if (!isIn2dRange(variableGrid, timeIndex, coordinate)) {
            return null
        }

        val value = variableGrid.readDataSlice(timeIndex, 0, coordinate.yIndex, coordinate.xIndex).getObject(0)

        val transformers = pointTransformers[variable]
        return if (transformers == null) {
            // No applicable transformers, return data as-is
            value
        } else {
            // Transform our value, but get the unit type first
            val unit = getVariable(variable)?.unitType ?: return null
            transformers.chainedTransformPoint(value, unit)
        }
    }

    override fun getGridTimeAnd3dPositionSlice(
        variable: WeatherVariableType,
        time: ZonedDateTime,
        coordinate: WeatherVariable3dCoordinate
    ): Any? {
        val (variableGridset, variableGrid) = getGridsetAndGrid(variable) ?: return null
        val timeIndex = availableTimes[variableGridset]?.indexOf(time) ?: return null

        if (!isIn3dRange(variableGrid, timeIndex, coordinate)) {
            return null
        }

        val value = variableGrid.readDataSlice(timeIndex, coordinate.zIndex, coordinate.yIndex, coordinate.xIndex).getObject(0)

        val transformers = pointTransformers[variable]
        return if (transformers == null) {
            // No applicable transformers, return data as-is
            value
        } else {
            // Transform our value, but get the unit type first
            val unit = getVariable(variable)?.unitType ?: return null
            transformers.chainedTransformPoint(value, unit)
        }
    }

    private fun Gridset.getTime(timeIndex: Int): ZonedDateTime? {
        return this.geoCoordSystem.calendarDates.getOrNull(timeIndex).run {
            val utilDate = this?.toDate()
            utilDate?.toInstant()?.atZone(ZoneOffset.UTC)
        }
    }

    override fun getTimes(variable: WeatherVariableType): List<ZonedDateTime>? {
        val variableGridset = getGridset(variable)
        return availableTimes[variableGridset]
    }

    override fun gridTimeSliceExists(variable: WeatherVariableType, time: ZonedDateTime): Boolean {
        val variableGridset = getGridset(variable)
        return availableTimes[variableGridset]?.contains(time) ?: false
    }

    override fun gridTimeAnd2dPositionSliceExists(
        variable: WeatherVariableType,
        time: ZonedDateTime,
        coordinate: WeatherVariable2dCoordinate
    ): Boolean {
        val (variableGridset, variableGrid) = getGridsetAndGrid(variable) ?: return false
        return isIn2dRange(variableGridset, variableGrid, time, coordinate)
    }

    override fun gridTimeAnd3dPositionSliceExists(
        variable: WeatherVariableType,
        time: ZonedDateTime,
        coordinate: WeatherVariable3dCoordinate
    ): Boolean {
        val (variableGridset, variableGrid) = getGridsetAndGrid(variable) ?: return false
        return isIn3dRange(variableGridset, variableGrid, time, coordinate)
    }

    override fun containsLatLon(latitude: Double, longitude: Double, variable: WeatherVariableType?): Boolean {
        return if (variable == null) {
            wrappedGridDataset?.gridsets?.any {
                gridsetContainsLatLon(it, latitude, longitude)
            } == true
        } else {
            val gridset = getGridset(variable) ?: return false
            gridsetContainsLatLon(gridset, latitude, longitude)
        }
    }

    private fun gridsetContainsLatLon(gridset: Gridset, latitude: Double, longitude: Double): Boolean {
        return try {
            val xy = gridset.geoCoordSystem.findXYindexFromLatLon(latitude, longitude, null)

            xy[0] >= 0 && xy[1] >= 0
        } catch (e: Exception) {
            logger.error("$sourceFilePath: ${gridset.grids.joinToString()}: could not project latlon coordinate to indices. ${e.message}")
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