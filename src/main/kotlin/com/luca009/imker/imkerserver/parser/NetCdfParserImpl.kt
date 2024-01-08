package com.luca009.imker.imkerserver.parser

import com.luca009.imker.imkerserver.parser.model.NetCdfParser
import com.luca009.imker.imkerserver.parser.model.RawWeatherVariable
import com.luca009.imker.imkerserver.parser.model.WeatherVariable2dCoordinate
import com.luca009.imker.imkerserver.parser.model.WeatherVariable3dCoordinate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ucar.nc2.constants.FeatureType
import ucar.nc2.dataset.NetcdfDataset
import ucar.nc2.dataset.NetcdfDatasets
import ucar.nc2.dt.grid.GridDataset
import ucar.nc2.ft.FeatureDataset
import ucar.nc2.ft.FeatureDatasetFactoryManager
import ucar.nc2.util.CancelTask

class NetCdfParserImpl(
    netCdfFilePath: String
) : NetCdfParser {
    private val sourceFilePath: String = netCdfFilePath
    private val availableVariables: Map<String, RawWeatherVariable>
    private val dataset: NetcdfDataset
    private val wrappedDataset: FeatureDataset
    private val wrappedGridDataset: GridDataset?
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    init {
        dataset = openDataset(netCdfFilePath)
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

    override fun getDataSources(): Set<String> {
        return setOf(sourceFilePath)
    }

    override fun getAvailableRawVariables(): Set<RawWeatherVariable> {
        return availableVariables.values.toSet()
    }

    override fun getRawVariable(name: String): RawWeatherVariable? {
        return availableVariables[name]
    }

    override fun getGridEntireSlice(name: String): List<Array<*>>? {
        val weatherVariable = wrappedGridDataset?.grids?.find { it.name == name }
        requireNotNull(weatherVariable) { return null }

        val allSlices: MutableList<Array<*>> = mutableListOf()

        for (x in 0 until weatherVariable.timeDimension.length) {
            allSlices.add(weatherVariable.readVolumeData(x).copyToNDJavaArray() as Array<*>)
        }

        return allSlices
    }

    override fun getGridTimeSlice(name: String, timeIndex: Int): Array<*>? {
        val weatherVariable = wrappedGridDataset?.grids?.find { it.name == name }
        requireNotNull(weatherVariable) { return null }

        return weatherVariable.readVolumeData(timeIndex).copyToNDJavaArray() as Array<*>
    }

    override fun getGridTimeAnd2dPositionSlice(name: String, timeIndex: Int, coordinate: WeatherVariable2dCoordinate): Any? {
        val weatherVariable = wrappedGridDataset?.grids?.find { it.name == name }
        requireNotNull(weatherVariable) { return null }

        return weatherVariable.readDataSlice(timeIndex, 0, coordinate.yIndex, coordinate.xIndex).getObject(0)
    }

    override fun getGridTimeAnd3dPositionSlice(name: String, timeIndex: Int, coordinate: WeatherVariable3dCoordinate): Any? {
        val weatherVariable = wrappedGridDataset?.grids?.find { it.name == name }
        requireNotNull(weatherVariable) { return null }

        return weatherVariable.readDataSlice(timeIndex, coordinate.zIndex, coordinate.yIndex, coordinate.xIndex).getObject(0)
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

        return try {
            timeIndex >= 0 && timeIndex < weatherVariable.timeDimension.length &&
                    coordinate.isInRange(weatherVariable.xDimension.length, weatherVariable.yDimension.length)
        } catch (e: Exception) {
            logger.error("$sourceFilePath: $name: could not determine if 2d position slice exists, defaulting to false. ${e.message}")
            false
        }
    }

    override fun gridTimeAnd3dPositionSliceExists(
        name: String,
        timeIndex: Int,
        coordinate: WeatherVariable3dCoordinate
    ): Boolean {
        val weatherVariable = wrappedGridDataset?.grids?.find { it.name == name }
        requireNotNull(weatherVariable) { return false }

        return try {
            timeIndex >= 0 && timeIndex < weatherVariable.timeDimension.length &&
                    coordinate.isInRange(weatherVariable.xDimension.length, weatherVariable.yDimension.length, weatherVariable.zDimension.length)
        } catch (e: Exception) {
            logger.error("$sourceFilePath: $name: could not determine if 3d position slice exists, defaulting to false. ${e.message}")
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
}