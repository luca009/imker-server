 package com.luca009.imker.server.parser

import com.luca009.imker.server.parser.model.WeatherVariableRasterDimension
import com.luca009.imker.server.parser.model.WeatherVariableRasterDimensionType
import com.luca009.imker.server.parser.model.WeatherVariableRasterSlice
import com.luca009.imker.server.parser.model.WeatherVariableUnit
import kotlin.reflect.KClass

open class FlatCollectionWeatherVariableRasterSlice(
    unit: WeatherVariableUnit?,
    dataType: KClass<*>?,
    private val data: Collection<*>,
    final override val dimensions: Map<WeatherVariableRasterDimensionType, WeatherVariableRasterDimension>
): WeatherVariableRasterSlice(unit, dataType) {
    override val size: Int
        get() = data.size

    private val splitFrequencies: List<Int> = dimensions.values.reversed().toList().dropLast(1).map { it.size }

    private fun getIndex(indices: IntArray): Int {
        return indices.mapIndexed { index, value ->
            if (index == 0) {
                value
            } else {
                value * splitFrequencies[index - 1]
            }
        }.sum()
    }

    override fun get(vararg indices: Int): Any? {
        if (splitFrequencies.size + 1 != indices.size) {
            throw IllegalArgumentException("Number of indices was out of range (expected ${splitFrequencies.size + 1}, received ${indices.size})")
        }

        val index = getIndex(indices)
        return data.elementAt(index)
    }

    override fun getOrNull(vararg indices: Int): Any? {
        if (splitFrequencies.size + 1 != indices.size) {
            return null
        }

        val index = getIndex(indices)
        return data.elementAtOrNull(index)
    }

    override fun map(transform: (Any?) -> Any?): FlatCollectionWeatherVariableRasterSlice {
        return FlatCollectionWeatherVariableRasterSlice(
            unit,
            dataType,
            data.map(transform),
            dimensions
        )
    }
}

open class ArrayWeatherVariableRasterSlice(
    unit: WeatherVariableUnit?,
    dataType: KClass<*>,
    data: Array<*>,
    dimensions: Map<WeatherVariableRasterDimensionType, WeatherVariableRasterDimension>
): FlatCollectionWeatherVariableRasterSlice(unit, dataType, data.asList(), dimensions)