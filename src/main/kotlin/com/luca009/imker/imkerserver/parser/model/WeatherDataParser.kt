package com.luca009.imker.imkerserver.parser.model

interface WeatherDataParser {
    fun getDataSources(): Set<String>
    fun getAvailableRawVariables(): Set<RawWeatherVariable>
    fun getRawVariable(name: String): RawWeatherVariable?
    fun getGridEntireSlice(name: String): Array<Array<*>>?
    fun getGridTimeSlice(name: String, timeIndex: Int = 0): Array<*>?
    fun getGridTimeAndPositionSlice(name: String, timeIndex: Int = 0, xIndex: Int, yIndex: Int, zIndex: Int): Any?

    fun gridTimeSliceExists(name: String, timeIndex: Int): Boolean
    fun gridTimeAndPositionSliceExists(name: String, timeIndex: Int, xIndex: Int, yIndex: Int, zIndex: Int): Boolean
}

interface NetCdfParser : WeatherDataParser