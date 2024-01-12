package com.luca009.imker.imkerserver.management

import com.luca009.imker.imkerserver.configuration.model.WeatherModel
import com.luca009.imker.imkerserver.management.model.WeatherModelManagerService
import com.luca009.imker.imkerserver.parser.model.NetCdfParser
import com.luca009.imker.imkerserver.receiver.model.IncaReceiver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.SortedMap

@Configuration
class WeatherModelManagerServiceConfiguration(
    val netCdfParserFactory: (String) -> NetCdfParser
) {
    @Bean
    fun weatherModelManagerService(incaReceiver: IncaReceiver): WeatherModelManagerService {
        // TODO: this needs to be replaced with configuration files. For now, we only have one weather model

        val weatherModels: SortedMap<Int, WeatherModel> = sortedMapOf(
            0 to WeatherModel(
                "INCA",
                "INCA",
                "GeoSphere Austria under CC BY-SA 4.0",
                "src/test/resources/inca/inca_map.csv",
                incaReceiver,
                netCdfParserFactory("src/test/resources/inca/inca_nowcast.nc") // TODO: replace this with the actual files we have downloaded
            )
        )

        return WeatherModelManagerServiceImpl(
            weatherModels
        )
    }
}