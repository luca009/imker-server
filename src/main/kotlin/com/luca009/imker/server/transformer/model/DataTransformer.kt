package com.luca009.imker.server.transformer.model

import com.luca009.imker.server.parser.model.WeatherVariableRasterSlice
import com.luca009.imker.server.parser.model.WeatherVariableTimeRasterSlice
import com.luca009.imker.server.parser.model.WeatherVariableTimeSlice
import com.luca009.imker.server.parser.model.WeatherVariableUnit
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * A transformer which supports transforming entire slices of data
 */
interface DataTransformer {
    /**
     * Checks if the DataTransformer supports the specified [type]
     */
    fun supportsDataType(type: KClass<*>?): Boolean

    /**
     * Transform an entire [slice] of data and return the transformed slice or null, if the data type is invalid
     */
    fun transformSlice(slice: WeatherVariableTimeRasterSlice): WeatherVariableTimeRasterSlice?
}

/**
 * A transformer which supports transforming a raster of data
 */
interface RasterDataTransformer : DataTransformer {
    /**
     * Transform an entire [raster] of data and return the transformed raster or null, if the data type is invalid
     */
    fun transformRaster(raster: WeatherVariableRasterSlice): WeatherVariableRasterSlice?
}

/**
 * A transformer which supports transforming a time series of data
 */
// TODO: reevaluate time transformers
interface TimeDataTransformer : DataTransformer {
    /**
     * Transform a [series] of values sampled at a single point, throughout time and return the transformed list or null, if the data type is invalid
     */
    fun transformTimeSeries(series: WeatherVariableTimeSlice): WeatherVariableTimeSlice?
}

/**
 * A transformer which supports transforming a specific point of data
 */
interface PointDataTransformer : DataTransformer, RasterDataTransformer, TimeDataTransformer {
    /**
     * Transform the [value] sampled at a specific point and return the transformed value or null, if the data type is invalid
     */
    fun transformPoint(value: Any, unit: WeatherVariableUnit): Any?
}

/**
 * A partial implementation of [DataTransformer] supporting validating data types
 */
abstract class TypeSensitiveDataTransformer(
    val compatibleDataTypes: List<KClass<*>>
) : DataTransformer {
    override fun supportsDataType(type: KClass<*>?): Boolean {
        return compatibleDataTypes.any {
            type?.isSubclassOf(it) ?: false
        }
    }
}

/**
 * A transformer for converting between different units
 */
abstract class UnitDataTransformer(
    compatibleDataTypes: List<KClass<out Any>>
) : TypeSensitiveDataTransformer(compatibleDataTypes), PointDataTransformer {
    protected abstract fun convertUnit(sourceUnit: WeatherVariableUnit, value: Double): Double?

    override fun transformPoint(value: Any, unit: WeatherVariableUnit): Double? {
        val castValue = value as? Double
        requireNotNull(castValue) {
            return null
        }

        return convertUnit(unit, castValue)
    }
}