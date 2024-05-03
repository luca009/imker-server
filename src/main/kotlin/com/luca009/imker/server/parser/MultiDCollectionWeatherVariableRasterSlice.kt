package com.luca009.imker.server.parser

import com.luca009.imker.server.parser.model.WeatherVariableRasterDimension
import com.luca009.imker.server.parser.model.WeatherVariableRasterDimensionType
import com.luca009.imker.server.parser.model.WeatherVariableRasterSlice
import com.luca009.imker.server.parser.model.WeatherVariableUnit
import kotlin.reflect.KClass

abstract class MultiDCollectionWeatherVariableRasterSlice(
    unit: WeatherVariableUnit?,
    dataType: KClass<*>?,
    override val dimensions: Map<WeatherVariableRasterDimensionType, WeatherVariableRasterDimension>
) : WeatherVariableRasterSlice(unit, dataType) {
    abstract val data: Collection<Collection<*>>
}

class TwoDCollectionWeatherVariableRasterSlice(
    unit: WeatherVariableUnit?,
    dataType: KClass<*>?,
    override val data: Collection<Collection<*>>,
    xDimension: WeatherVariableRasterDimension? = null,
    yDimension: WeatherVariableRasterDimension? = null
) : MultiDCollectionWeatherVariableRasterSlice(
    unit,
    dataType,
    mapOf(
        WeatherVariableRasterDimensionType.X to (xDimension ?: WeatherVariableRasterDimension(data.size)),
        WeatherVariableRasterDimensionType.Y to (yDimension ?: WeatherVariableRasterDimension(data.maxBy { it.size }.size))
    )
) {
    override val size: Int = data.sumOf { it.size }

    override fun get(vararg indices: Int): Any? {
        val x = indices[0]
        val y = indices[1]
        return data.elementAt(x).elementAt(y)
    }

    override fun getOrNull(vararg indices: Int): Any? {
        val x = indices.getOrNull(0) ?: return null
        val y = indices.getOrNull(1) ?: return null
        return data.elementAtOrNull(x)?.elementAtOrNull(y)
    }

    override fun map(transform: (Any?) -> Any?): TwoDCollectionWeatherVariableRasterSlice {
        return TwoDCollectionWeatherVariableRasterSlice(
            unit,
            dataType,
            data.map { it.map(transform) },
            dimensions[WeatherVariableRasterDimensionType.X],
            dimensions[WeatherVariableRasterDimensionType.Y]
        )
    }
}

class ThreeDCollectionWeatherVariableRasterSlice(
    unit: WeatherVariableUnit?,
    dataType: KClass<*>?,
    override val data: Collection<Collection<Collection<*>>>,
    xDimension: WeatherVariableRasterDimension? = null,
    yDimension: WeatherVariableRasterDimension? = null,
    zDimension: WeatherVariableRasterDimension? = null
) : MultiDCollectionWeatherVariableRasterSlice(
    unit,
    dataType,
    mapOf(
        WeatherVariableRasterDimensionType.X to (xDimension ?: WeatherVariableRasterDimension(data.size)),
        WeatherVariableRasterDimensionType.Y to (yDimension ?: WeatherVariableRasterDimension(data.maxBy { it.size }.size)),
        WeatherVariableRasterDimensionType.Z to (zDimension ?: WeatherVariableRasterDimension(data.maxOf { it.maxOf { it.size } }))
    )
) {
    override val size: Int = data.sumOf { it.sumOf { it.size } }

    override fun get(vararg indices: Int): Any? {
        val x = indices[0]
        val y = indices[1]
        val z = indices[2]
        return data.elementAt(x).elementAt(y).elementAt(z)
    }

    override fun getOrNull(vararg indices: Int): Any? {
        val x = indices.getOrNull(0) ?: return null
        val y = indices.getOrNull(1) ?: return null
        val z = indices.getOrNull(2) ?: return null
        return data.elementAtOrNull(x)?.elementAtOrNull(y)?.elementAtOrNull(z)
    }

    override fun map(transform: (Any?) -> Any?): ThreeDCollectionWeatherVariableRasterSlice {
        return ThreeDCollectionWeatherVariableRasterSlice(
            unit,
            dataType,
            data.map { it.map { it.map(transform) } },
            dimensions[WeatherVariableRasterDimensionType.X],
            dimensions[WeatherVariableRasterDimensionType.Y],
            dimensions[WeatherVariableRasterDimensionType.Z]
        )
    }
}