package com.luca009.imker.imkerserver.parser

import com.luca009.imker.imkerserver.parser.model.*
import com.luca009.imker.imkerserver.receiver.model.DataReceiver
import java.time.ZonedDateTime

class DynamicDataParserImpl(
    private val weatherDataParser: WeatherDataParser,
    private val dataReceiver: DataReceiver
) : DynamicDataParser {

    override suspend fun updateSources(dateTime: ZonedDateTime): Boolean {
        val result = dataReceiver.downloadData(dateTime, null) // TODO: dynamically get the downloadedFileName
        return result.successful
    }

    override fun getDataSources() = weatherDataParser.getDataSources()
    override fun getAvailableRawVariables() = weatherDataParser.getAvailableRawVariables()
    override fun getRawVariable(name: String) = weatherDataParser.getRawVariable(name)
    override fun getGridEntireSlice(name: String) = weatherDataParser.getGridEntireSlice(name)
    override fun getGridTimeSlice(name: String, timeIndex: Int) = weatherDataParser.getGridTimeSlice(name, timeIndex)
    override fun getGridTimeAnd2dPositionSlice(
        name: String,
        timeIndex: Int,
        coordinate: WeatherVariable2dCoordinate
    ) = weatherDataParser.getGridTimeAnd2dPositionSlice(name, timeIndex, coordinate)
    override fun getGridTimeAnd3dPositionSlice(
        name: String,
        timeIndex: Int,
        coordinate: WeatherVariable3dCoordinate
    ) = weatherDataParser.getGridTimeAnd3dPositionSlice(name, timeIndex, coordinate)
    override fun gridTimeSliceExists(name: String, timeIndex: Int) = weatherDataParser.gridTimeSliceExists(name, timeIndex)
    override fun gridTimeAnd2dPositionSliceExists(
        name: String,
        timeIndex: Int,
        coordinate: WeatherVariable2dCoordinate
    ) = weatherDataParser.gridTimeAnd2dPositionSliceExists(name, timeIndex, coordinate)
    override fun gridTimeAnd3dPositionSliceExists(
        name: String,
        timeIndex: Int,
        coordinate: WeatherVariable3dCoordinate
    ) = weatherDataParser.gridTimeAnd3dPositionSliceExists(name, timeIndex, coordinate)
    override fun containsLatLon(name: String, latitude: Double, longitude: Double) = weatherDataParser.containsLatLon(name, latitude, longitude)
    override fun latLonToCoordinates(name: String, latitude: Double, longitude: Double) = weatherDataParser.latLonToCoordinates(name, latitude, longitude)
}