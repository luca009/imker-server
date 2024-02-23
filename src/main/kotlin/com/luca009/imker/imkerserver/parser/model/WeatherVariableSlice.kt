package com.luca009.imker.imkerserver.parser.model

import kotlin.reflect.KClass

/**
 * A slice of a weather variable, containing multiple [variableSlices] at different time points.
 */
class WeatherVariableSlice(
    slices: List<WeatherVariableRasterSlice>
) {
    private val slices: MutableList<WeatherVariableRasterSlice>
    val dataType: KClass<Any>?
    val variableSlices: List<WeatherVariableRasterSlice>
        get() = slices

    init {
        dataType = slices.firstOrNull()?.dataType

        this.slices = slices.filter {
            it.dataType == dataType
        }.toMutableList()
    }

    fun isDouble(): Boolean = dataType == Double::class

    fun setSlice(timeIndex: Int, variableData: WeatherVariableRasterSlice) {
        if (timeIndex > slices.count())
            return

        slices[timeIndex] = variableData
    }
}

/**
 * A slice of a weather variable at a specified time point.
 */
abstract class WeatherVariableRasterSlice(
    val dataType: KClass<Any>
) {
    abstract val size: Int
    abstract val dimensions: Map<WeatherVariableRasterDimensionType, WeatherVariableRasterDimension>

    abstract operator fun get(vararg indices: Int): Any?
    abstract fun getOrNull(vararg indices: Int): Any?
    fun isDouble(): Boolean = dataType == Double::class
    fun getDoubleOrNull(vararg indices: Int) = getOrNull(*indices) as? Double
}

data class WeatherVariableRasterDimension(
    val size: Int
)

enum class WeatherVariableRasterDimensionType {
    x, y, z
}