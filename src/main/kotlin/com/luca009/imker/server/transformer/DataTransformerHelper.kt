package com.luca009.imker.server.transformer

import com.luca009.imker.server.parser.model.WeatherVariableRasterSlice
import com.luca009.imker.server.parser.model.WeatherVariableTimeRasterSlice
import com.luca009.imker.server.parser.model.WeatherVariableTimeSlice
import com.luca009.imker.server.parser.model.WeatherVariableUnit
import com.luca009.imker.server.transformer.model.DataTransformer
import com.luca009.imker.server.transformer.model.PointDataTransformer
import com.luca009.imker.server.transformer.model.RasterDataTransformer
import com.luca009.imker.server.transformer.model.TimeDataTransformer

object DataTransformerHelper {
    fun List<DataTransformer>.chainedTransformSlice(slice: WeatherVariableTimeRasterSlice): WeatherVariableTimeRasterSlice? {
        return this.runningFold(slice) { acc, transformer ->
            transformer.transformSlice(acc) ?: return null
        }.last()
    }

    fun List<RasterDataTransformer>.chainedTransformRaster(slice: WeatherVariableRasterSlice): WeatherVariableRasterSlice? {
        return this.runningFold(slice) { acc, transformer ->
            transformer.transformRaster(acc) ?: return null
        }.last()
    }

    fun List<TimeDataTransformer>.chainedTransformTimeSeries(slice: WeatherVariableTimeSlice): WeatherVariableTimeSlice? {
        return this.runningFold(slice) { acc, transformer ->
            transformer.transformTimeSeries(acc) ?: return null
        }.last()
    }

    fun List<PointDataTransformer>.chainedTransformPoint(value: Any, unit: WeatherVariableUnit): Any? {
        return this.runningFold(value) { acc, transformer ->
            transformer.transformPoint(acc, unit) ?: return null
        }.last()
    }
}