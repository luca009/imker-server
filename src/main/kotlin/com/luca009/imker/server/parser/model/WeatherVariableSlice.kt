package com.luca009.imker.server.parser.model

import com.luca009.imker.server.parser.*
import java.time.ZonedDateTime
import kotlin.reflect.KClass

interface WeatherVariableSlice {
    val dataType: KClass<*>?
    val unit: WeatherVariableUnit?

    fun isDouble(): Boolean
}

/**
 * A slice of a weather variable, containing multiple [variableSlices] at different time points.
 */
class WeatherVariableTimeRasterSlice(
    slices: Map<ZonedDateTime, WeatherVariableRasterSlice>
) : WeatherVariableSlice {
    private val slices = slices.toSortedMap()

    val variableSlices: Map<ZonedDateTime, WeatherVariableRasterSlice>
        get() = slices

    override val dataType: KClass<*>?
    override val unit: WeatherVariableUnit?
    val dimensions: Map<WeatherVariableRasterDimensionType, WeatherVariableRasterDimension>?

    init {
        val firstDataType = slices.values.firstOrNull()?.dataType
        dataType = if (slices.all { it.value.dataType == firstDataType }) {
            firstDataType
        } else {
            null
        }

        val firstUnit = slices.values.firstOrNull()?.unit
        unit = if (slices.all { it.value.unit == firstUnit }) {
            firstUnit
        } else {
            null
        }

        val firstDimensions = slices.values.firstOrNull()?.dimensions
        dimensions = if (slices.all { it.value.dimensions == firstDimensions }) {
            firstDimensions
        } else {
            null
        }
    }

    override fun isDouble(): Boolean = dataType == Double::class

    fun subMap(startIndex: Int, limit: Int): Map<ZonedDateTime, WeatherVariableRasterSlice>? {
        val firstKey = slices.keys.elementAtOrNull(startIndex) ?: return null

        return if (startIndex + limit in slices.keys.indices) {
            // startIndex and limit are within the entire range of the map, so only slice out a specific section
            val lastKey = slices.keys.elementAtOrNull(startIndex + limit + 1) ?: return null
            slices.subMap(firstKey, lastKey)
        } else {
            // startIndex + limit is outside the range of the map, so disregard the limit and instead use tailMap()
            slices.tailMap(firstKey)
        }

    }

    fun subSliceAt2dPosition(coordinate: WeatherVariable2dCoordinate): WeatherVariableTimeSlice {
        return WeatherVariableTimeSlice(
            variableSlices.mapValues {
                it.value.getOrNull(coordinate.xIndex, coordinate.yIndex)
            },
            dataType,
            unit
        )
    }

    fun subSliceAt2dPosition(startIndex: Int, limit: Int, coordinate: WeatherVariable2dCoordinate): WeatherVariableTimeSlice? {
        val fullSubMap = subMap(startIndex, limit) ?: return null

        return WeatherVariableTimeSlice(
            fullSubMap.mapValues {
                it.value.getOrNull(coordinate.xIndex, coordinate.yIndex)
            },
            dataType,
            unit
        )
    }

    fun subSliceAt3dPosition(coordinate: WeatherVariable3dCoordinate): WeatherVariableTimeSlice {
        return WeatherVariableTimeSlice(
            variableSlices.mapValues {
                it.value.getOrNull(coordinate.xIndex, coordinate.yIndex, coordinate.zIndex)
            },
            dataType,
            unit
        )
    }

    fun subSliceAt3dPosition(startIndex: Int, limit: Int, coordinate: WeatherVariable3dCoordinate): WeatherVariableTimeSlice? {
        val fullSubMap = subMap(startIndex, limit) ?: return null

        return WeatherVariableTimeSlice(
            fullSubMap.mapValues {
                it.value.getOrNull(coordinate.xIndex, coordinate.yIndex)
            },
            dataType,
            unit
        )
    }

    fun mapIndexedTimeSlices(transform: (x: Int, y: Int, z: Int?, WeatherVariableTimeSlice) -> WeatherVariableTimeSlice): WeatherVariableTimeRasterSlice? {
        // TODO: This function might have to get replaced with a more efficient method later

        requireNotNull(dimensions) {
            return null
        }

        val xDimension = dimensions[WeatherVariableRasterDimensionType.X] ?: return null
        val yDimension = dimensions[WeatherVariableRasterDimensionType.Y] ?: return null
        val zDimension = dimensions[WeatherVariableRasterDimensionType.Z]

        val rasterSlices = if (zDimension == null) {
            // There is no z dimension, so transform in 2d

            // First, get all time slices and transform them
            val slices = xDimension.indices.map { x ->
                yDimension.indices.map { y ->
                    // We don't have a z dimension, transform 2d slices
                    val timeSlice = subSliceAt2dPosition(WeatherVariable2dCoordinate(x, y))
                    transform(x, y, null, timeSlice)
                }
            }

            // We now have a List<List<WeatherVariableTimeSlice>>, but we need to convert it into multiple rasters
            // Iterate through all times
            variableSlices.keys.associateWith { time ->
                // Recreate the 2d slice at the specified time
                val twoDSlice = slices.map { x ->
                    x.map {
                        it[time]
                    }
                }

                // Instantiate a new raster slice for the slice above
                TwoDCollectionWeatherVariableRasterSlice(
                    unit,
                    dataType,
                    twoDSlice,
                    dimensions
                )
            }
        } else {
            // A z dimension is available, transform in 3d

            // First, get all time slices and transform them
            val slices = xDimension.indices.map { x ->
                yDimension.indices.map { y ->
                    // A z dimension is defined, transform the slices in there as well
                    zDimension.indices.map { z ->
                        val timeSlice = subSliceAt3dPosition(WeatherVariable3dCoordinate(x, y, z))
                        transform(x, y, z, timeSlice)
                    }
                }
            }

            // We now have a List<List<WeatherVariableTimeSlice>>, but we need to convert it into multiple rasters
            // Iterate through all times
            variableSlices.keys.associateWith { time ->
                // Recreate the 3d slice at the specified time
                val threeDSlice = slices.map { x ->
                    x.map { y ->
                        y.map {
                            it[time]
                        }
                    }
                }

                // Instantiate a new raster slice for the slice above
                ThreeDCollectionWeatherVariableRasterSlice(
                    unit,
                    dataType,
                    threeDSlice,
                    dimensions
                )
            }
        }

        return WeatherVariableTimeRasterSlice(
            rasterSlices
        )
    }

    fun setSlice(time: ZonedDateTime, variableData: WeatherVariableRasterSlice) {
        slices[time] = variableData
    }
}

class WeatherVariableTimeSlice(
    private val internalValues: Map<ZonedDateTime, Any?>,
    override val dataType: KClass<*>?,
    override val unit: WeatherVariableUnit?
) : WeatherVariableSlice, Map<ZonedDateTime, Any?> {
    override val entries: Set<Map.Entry<ZonedDateTime, Any?>>
        get() = internalValues.entries
    override val keys: Set<ZonedDateTime>
        get() = internalValues.keys
    override val size: Int
        get() = internalValues.size
    override val values: Collection<Any?>
        get() = internalValues.values

    override fun isEmpty() = internalValues.isEmpty()
    override fun get(key: ZonedDateTime) = internalValues[key]
    override fun containsValue(value: Any?) = internalValues.containsValue(value)
    override fun containsKey(key: ZonedDateTime) = internalValues.containsKey(key)

    override fun isDouble(): Boolean = dataType == Double::class
}

/**
 * A slice of a weather variable at a specified time point.
 */
abstract class WeatherVariableRasterSlice(
    override val unit: WeatherVariableUnit?,
    override val dataType: KClass<*>?
) : WeatherVariableSlice {
    abstract val dimensions: Map<WeatherVariableRasterDimensionType, WeatherVariableRasterDimension>
    abstract val size: Int

    abstract operator fun get(vararg indices: Int): Any?
    abstract fun getOrNull(vararg indices: Int): Any?
    override fun isDouble(): Boolean = dataType == Double::class
    fun getDoubleOrNull(vararg indices: Int) = getOrNull(*indices) as? Double
}

data class WeatherVariableRasterDimension(
    val size: Int,
    val indices: IntRange = 0 until size
)

enum class WeatherVariableRasterDimensionType {
    X, Y, Z
}