package com.luca009.imker.server.configuration.properties

import com.luca009.imker.server.parser.model.WeatherVariableType
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@ConfigurationProperties(prefix = "models")
class ModelProperties {
    /**
     * The defined raw weather models.
     */
    var definedModels: Map<Int, RawWeatherModel> = mapOf()
}

/**
 * Represents a weather model in only basic data types. This is to be mapped to an instance of [com.luca009.imker.server.configuration.model.WeatherModel].
 */
data class RawWeatherModel(
    /**
     * Metadata about the weather model.
     */
    val meta: RawWeatherModelMetaInfo,

    /**
     * Information about the data receiver and parser.
     */
    val receiver: RawWeatherModelReceiverInfo,

    /**
     * Information about the data source.
     */
    val source: RawWeatherModelSourceFileInfo,

    /**
     * Information about how to map variable names/identifiers to [WeatherVariableType]s and unit strings to [com.luca009.imker.server.parser.model.WeatherVariableUnit]s.
     */
    val mapping: RawWeatherModelMappingInfo,

    /**
     * Transformers to use for modifying the weather data.
     */
    val transforming: RawWeatherModelTransformingInfo = RawWeatherModelTransformingInfo(),

    /**
     * Configuration about how to store the weather data files.
     */
    val storage: RawWeatherModelStorageInfo,

    /**
     * Configuration about how to cache the weather data files in memory.
     */
    val cache: RawWeatherModelCacheInfo = RawWeatherModelCacheInfo()
)

data class RawWeatherModelMetaInfo(
    val name: String,
    val friendlyName: String,
    val copyright: String
)

data class RawWeatherModelReceiverInfo(
    val receiverName: String,
    val receiverGroup: String = "default",
    val parserName: String
)

data class RawWeatherModelSourceFileInfo(
    val ftpHost: String,
    val ftpUsername: String = "anonymous",
    val ftpPassword: String = "",
    val ftpSubFolder: String,
    val prefix: String,
    val postfix: String,
    val dateFormat: String,
    val updateFrequency: Duration
)

data class RawWeatherModelMappingInfo(
    val variableMapping: Map<WeatherVariableType, String>,
    val unitMapperFile: String
)

data class RawWeatherModelTransformingInfo(
    val transformers: Map<WeatherVariableType, List<String>> = mapOf()
)

data class RawWeatherModelStorageInfo(
    val storageLocationName: String,
    val subFolderName: String? = null,
    val policy: RawWeatherModelStoragePolicyInfo = RawWeatherModelStoragePolicyInfo()
)

data class RawWeatherModelStoragePolicyInfo(
    val maxFiles: Int? = null,
    val maxAge: Duration? = null
)

data class RawWeatherModelCacheInfo(
    val variablesInMemory: Set<WeatherVariableType> = setOf(),
    val ignoredVariables: Set<WeatherVariableType> = setOf()
)