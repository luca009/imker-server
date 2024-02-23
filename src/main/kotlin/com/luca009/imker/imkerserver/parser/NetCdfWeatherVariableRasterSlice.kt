package com.luca009.imker.imkerserver.parser

import com.luca009.imker.imkerserver.parser.model.WeatherVariableRasterDimension
import com.luca009.imker.imkerserver.parser.model.WeatherVariableRasterDimensionType
import com.luca009.imker.imkerserver.parser.model.WeatherVariableRasterSlice
import ucar.nc2.Dimension
import kotlin.reflect.KClass

class NetCdfWeatherVariableRasterSlice(
    dataType: KClass<Any>,
    private val data: Array<*>,
    ucarDimensions: List<Dimension>
) : WeatherVariableRasterSlice(dataType) {
    override val size: Int
        get() = data.size

    override val dimensions: Map<WeatherVariableRasterDimensionType, WeatherVariableRasterDimension>
    private val splitFrequencies: List<Int>

    init {
        dimensions = ucarDimensions.mapNotNull {
            val enum = WeatherVariableRasterDimensionType.values().firstOrNull { enum -> it.name.lowercase() == enum.name }
            requireNotNull(enum) {
                return@mapNotNull null
            }

            Pair(
                enum,
                WeatherVariableRasterDimension(
                    it.length
                )
            )
        }.toMap()

        splitFrequencies = dimensions.values.reversed().toList().dropLast(1).map { it.size }
    }

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
        return data[index]
    }

    override fun getOrNull(vararg indices: Int): Any? {
        if (splitFrequencies.size + 1 != indices.size) {
            return null
        }

        val index = getIndex(indices)
        return data.getOrNull(index)
    }

}