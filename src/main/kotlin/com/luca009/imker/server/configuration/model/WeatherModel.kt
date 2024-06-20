package com.luca009.imker.server.configuration.model

import com.luca009.imker.server.caching.model.WeatherRasterCompositeCacheConfiguration
import com.luca009.imker.server.management.files.model.DataFileNameManager
import com.luca009.imker.server.management.files.model.LocalFileManagementConfiguration
import com.luca009.imker.server.parser.model.WeatherDataParser
import com.luca009.imker.server.receiver.model.DataReceiver

/**
 * Represents a weather model.
 */
data class WeatherModel(
    /**
     * The name used for the weather model. This is used when making requests to the endpoints and to represent the model in responses.
     */
    val name: String,

    /**
     * The friendly name of the weather model. Used in responses as optional additional information.
     */
    val friendlyName: String,

    /**
     * The copyright information for the weather model. Used in responses as optional additional information.
     */
    val copyright: String,

    /**
     * The [DataReceiver] used to obtain the weather data files.
     */
    val receiver: DataReceiver,

    /**
     * The [WeatherDataParser] used to parse the weather data files.
     */
    val parser: WeatherDataParser,

    /**
     * The [WeatherVariableTypeMapper] used to map the variable names/identifiers to [com.luca009.imker.server.parser.model.WeatherVariableType]s.
     */
    val mapper: WeatherVariableTypeMapper,

    /**
     * The [DataFileNameManager] used to manage the local files.
     */
    val fileNameManager: DataFileNameManager,

    /**
     * THe [WeatherVariableUnitMapper] used to map the unit strings to [com.luca009.imker.server.parser.model.WeatherVariableUnit]s.
     */
    val unitMapper: WeatherVariableUnitMapper,

    /**
     * The [WeatherRasterCompositeCacheConfiguration] that is to be used by any [com.luca009.imker.server.caching.model.WeatherRasterCompositeCache] storing this weather model.
     */
    val cacheConfiguration: WeatherRasterCompositeCacheConfiguration,

    /**
     * The [LocalFileManagementConfiguration] that is to be used by any [com.luca009.imker.server.management.files.model.LocalFileManagerService] managing the local files for this weather model.
     */
    val fileManagementConfiguration: LocalFileManagementConfiguration
)