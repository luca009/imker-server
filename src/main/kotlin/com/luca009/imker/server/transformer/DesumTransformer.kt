package com.luca009.imker.server.transformer

import com.luca009.imker.server.parser.model.WeatherVariableTimeRasterSlice
import com.luca009.imker.server.parser.model.WeatherVariableTimeSlice
import com.luca009.imker.server.transformer.model.TimeDataTransformer
import com.luca009.imker.server.transformer.model.TypeSensitiveDataTransformer

/**
 * A transformer for [Double] values which calculates the difference between a value and its previous counterpart.
 */
class DesumTransformer : TypeSensitiveDataTransformer(
    listOf(Double::class)
), TimeDataTransformer {
    private fun unsafeTransformTimeSeries(series: WeatherVariableTimeSlice): WeatherVariableTimeSlice {
        // a slightly inefficient way to desum values
        val castEntries = series
            .mapValues {
                it.value as Double // allowed unsafe cast, as the method calling this one should have already checked the type. if this fails, it's a bug
            }
            .entries

        val newEntries = castEntries
            .windowed(2)
            .associate {
                Pair(
                    it[1].key,
                    it[1].value - it[0].value
                )
            }
            .plus(castEntries.first().toPair()) // add back the first element since windowed() effectively gets rid of it

        return WeatherVariableTimeSlice(
            newEntries,
            series.dataType,
            series.unit
        )
    }

    override fun transformTimeSeries(series: WeatherVariableTimeSlice): WeatherVariableTimeSlice? {
        if (!supportsDataType(series.dataType)) {
            return null
        }

        return unsafeTransformTimeSeries(series)
    }

    override fun transformSlice(slice: WeatherVariableTimeRasterSlice): WeatherVariableTimeRasterSlice? {
        if (!supportsDataType(slice.dataType)) {
            return null
        }

        return slice.mapIndexedTimeSlices { _, _, _, timeSlice ->
            unsafeTransformTimeSeries(timeSlice)
        }
    }
}