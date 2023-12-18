package com.luca009.imker.imkerserver.caching

import com.luca009.imker.imkerserver.caching.model.WeatherVariable2dRasterSlice
import com.luca009.imker.imkerserver.caching.model.WeatherVariableSlice
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class WeatherRasterCacheHelper {
    fun arraysToWeatherVariableSlice(input: List<Array<*>>): WeatherVariableSlice? {
        val filteredList = input.filter {
            it.isArrayOf<DoubleArray>()
        }

        return typedArraysToWeatherVariableSlice(filteredList as? List<Array<DoubleArray>> ?: return null)
    }
    fun typedArraysToWeatherVariableSlice(input: List<Array<DoubleArray>>): WeatherVariableSlice {
        val wrappedList = input.map {
            it.asList().map { it.asList() }
        } // Looks confusing, but this is just a 2d map() to deep wrap all the arrays

        return typedListToWeatherVariableSlice(wrappedList)
    }

    fun typedListToWeatherVariableSlice(input: List<List<List<Double>>>): WeatherVariableSlice {
        return WeatherVariableSlice(
            input.map {
                WeatherVariable2dRasterSlice(it)
            }
        )
    }
}