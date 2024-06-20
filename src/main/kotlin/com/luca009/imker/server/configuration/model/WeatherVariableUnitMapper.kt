package com.luca009.imker.server.configuration.model

import com.luca009.imker.server.parser.model.WeatherVariableUnit

/**
 * Mapper interface responsible for mapping unit strings to [WeatherVariableUnit]s.
 */
interface WeatherVariableUnitMapper {
    /**
     * Get the [WeatherVariableUnit] associated with the specified [unitString].
     */
    fun getUnits(unitString: String): WeatherVariableUnit?
}