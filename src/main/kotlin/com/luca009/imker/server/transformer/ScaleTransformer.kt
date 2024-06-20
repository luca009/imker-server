package com.luca009.imker.server.transformer

import com.luca009.imker.server.parser.model.WeatherVariableRasterSlice
import com.luca009.imker.server.parser.model.WeatherVariableTimeRasterSlice
import com.luca009.imker.server.parser.model.WeatherVariableTimeSlice
import com.luca009.imker.server.parser.model.WeatherVariableUnit
import com.luca009.imker.server.transformer.model.PointDataTransformer
import com.luca009.imker.server.transformer.model.TypeSensitiveDataTransformer

/**
 * A transformer for [Double] values which multiplies all value by the specified [scaleFactor].
 */
class ScaleTransformer(
    val scaleFactor: Double = 4.0
) : PointDataTransformer, TypeSensitiveDataTransformer(listOf(Double::class)) {
    override fun transformPoint(value: Any, unit: WeatherVariableUnit): Any? {
        return (value as? Double)?.times(scaleFactor)
    }

    override fun transformSlice(slice: WeatherVariableTimeRasterSlice): WeatherVariableTimeRasterSlice? {
        if (!supportsDataType(slice.dataType)) {
            return null
        }

        return slice.map {
            (it as Double) * scaleFactor
        }
    }

    override fun transformRaster(raster: WeatherVariableRasterSlice): WeatherVariableRasterSlice? {
        if (!supportsDataType(raster.dataType)) {
            return null
        }

        return raster.map {
            (it as Double) * scaleFactor
        }
    }

    override fun transformTimeSeries(series: WeatherVariableTimeSlice): WeatherVariableTimeSlice? {
        if (!supportsDataType(series.dataType)) {
            return null
        }

        return series.map {
            (it as Double) * scaleFactor
        }
    }

}