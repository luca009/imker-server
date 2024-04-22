package com.luca009.imker.server.parser

import com.luca009.imker.server.parser.model.WeatherVariableRasterDimension
import com.luca009.imker.server.parser.model.WeatherVariableRasterDimensionType
import com.luca009.imker.server.parser.model.WeatherVariableRasterSlice
import com.luca009.imker.server.parser.model.WeatherVariableUnit
import ucar.nc2.Dimension
import kotlin.reflect.KClass

class NetCdfWeatherVariableRasterSlice(
    unit: WeatherVariableUnit?,
    dataType: KClass<Any>,
    private val data: Array<*>,
    ucarDimensions: List<Dimension>,
    isLatLon: Boolean
) : ArrayWeatherVariableRasterSlice(
    unit,
    dataType,
    data,
    ucarDimensions.mapNotNull {
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
) {

    private companion object {
        fun getDimensionTypeFromName(name: String): WeatherVariableRasterDimensionType? = WeatherVariableRasterDimensionType.values().firstOrNull { it.name.equals(name, true) }
    }

    override val size: Int
        get() = data.size
}