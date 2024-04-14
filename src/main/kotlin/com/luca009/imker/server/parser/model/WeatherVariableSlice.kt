package com.luca009.imker.server.parser.model

import kotlin.reflect.KClass

interface WeatherVariableSlice {
    val dataType: KClass<Any>?
    val unit: WeatherVariableUnit?

    fun isDouble(): Boolean
}

/**
 * A slice of a weather variable, containing multiple [variableSlices] at different time points.
 */
class WeatherVariableTimeRasterSlice(
    slices: List<WeatherVariableRasterSlice>,

) : WeatherVariableSlice {
    private val slices = slices.toMutableList()

    val variableSlices: List<WeatherVariableRasterSlice>
        get() = slices

    override val dataType: KClass<Any>?
    override val unit: WeatherVariableUnit?

    init {
        val firstDataType = slices.firstOrNull()?.dataType
        dataType = if (slices.all { it.dataType == firstDataType }) {
            firstDataType
        } else {
            null
        }

        val firstUnit = slices.firstOrNull()?.unit
        unit = if (slices.all { it.unit == firstUnit }) {
            firstUnit
        } else {
            null
        }
    }

    override fun isDouble(): Boolean = dataType == Double::class

    fun setSlice(timeIndex: Int, variableData: WeatherVariableRasterSlice) {
        if (timeIndex > slices.count())
            return

        slices[timeIndex] = variableData
    }
}

class WeatherVariableTimeSlice(
    private val values: List<Any>,
    override val dataType: KClass<Any>?,
    override val unit: WeatherVariableUnit?
) : WeatherVariableSlice {
    fun get(timeIndex: Int) = values[timeIndex]
    fun getOrNull(timeIndex: Int) = values.getOrNull(timeIndex)

    override fun isDouble(): Boolean = dataType == Double::class
}

/**
 * A slice of a weather variable at a specified time point.
 */
abstract class WeatherVariableRasterSlice(
    override val unit: WeatherVariableUnit?,
    override val dataType: KClass<Any>
) : WeatherVariableSlice {
    abstract val size: Int
    abstract val dimensions: Map<WeatherVariableRasterDimensionType, WeatherVariableRasterDimension>

    abstract operator fun get(vararg indices: Int): Any?
    abstract fun getOrNull(vararg indices: Int): Any?
    override fun isDouble(): Boolean = dataType == Double::class
    fun getDoubleOrNull(vararg indices: Int) = getOrNull(*indices) as? Double
}

data class WeatherVariableRasterDimension(
    val size: Int
)

enum class WeatherVariableRasterDimensionType {
    X, Y, Z
}