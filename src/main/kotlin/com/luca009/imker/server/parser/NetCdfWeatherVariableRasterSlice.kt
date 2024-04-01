package com.luca009.imker.server.parser

import com.luca009.imker.server.parser.model.WeatherVariableRasterDimension
import com.luca009.imker.server.parser.model.WeatherVariableRasterDimensionType
import com.luca009.imker.server.parser.model.WeatherVariableRasterSlice
import ucar.nc2.Dimension
import kotlin.reflect.KClass

class NetCdfWeatherVariableRasterSlice(
    dataType: KClass<Any>,
    private val data: Array<*>,
    ucarDimensions: List<Dimension>,
    isLatLon: Boolean
) : WeatherVariableRasterSlice(dataType) {
    override val size: Int
        get() = data.size

    override val dimensions: Map<WeatherVariableRasterDimensionType, WeatherVariableRasterDimension>
    private val splitFrequencies: List<Int>

    init {
        dimensions = ucarDimensions.mapNotNull {
            val enum = if (isLatLon) {
                // If the coordinate system is latlon, then map the latitude and longitude variables to X and Y respectively
                when (it.name.lowercase()) {
                    "latitude" -> WeatherVariableRasterDimensionType.X
                    "longitude" -> WeatherVariableRasterDimensionType.Y
                    else -> getDimensionTypeFromName(it.name)
                }
            } else {
                getDimensionTypeFromName(it.name)
            }

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

    private fun getDimensionTypeFromName(name: String): WeatherVariableRasterDimensionType? = WeatherVariableRasterDimensionType.values().firstOrNull { it.name.equals(name, true) }

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