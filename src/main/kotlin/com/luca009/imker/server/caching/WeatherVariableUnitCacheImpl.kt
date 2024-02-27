package com.luca009.imker.server.caching

import com.luca009.imker.server.caching.model.WeatherVariableUnitCache
import com.luca009.imker.server.parser.model.WeatherVariableType
import com.luca009.imker.server.parser.model.WeatherVariableUnit
import org.springframework.stereotype.Component
import java.util.*

@Component
class WeatherVariableUnitCacheImpl : WeatherVariableUnitCache {
    val map: EnumMap<WeatherVariableType, WeatherVariableUnit> = EnumMap(WeatherVariableType::class.java)

    override fun getUnits(weatherVariableType: WeatherVariableType): WeatherVariableUnit? {
        return map[weatherVariableType]
    }

    override fun containsUnits(weatherVariableType: WeatherVariableType): Boolean {
        return map.containsKey(weatherVariableType)
    }

    override fun setUnits(weatherVariableType: WeatherVariableType, newUnits: WeatherVariableUnit) {
        map[weatherVariableType] = newUnits
    }
}