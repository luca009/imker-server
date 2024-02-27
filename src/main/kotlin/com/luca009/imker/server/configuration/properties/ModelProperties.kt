package com.luca009.imker.server.configuration.properties

import com.luca009.imker.server.parser.model.WeatherVariableType
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "models")
class ModelProperties {
    var definedModels: Map<Int, RawWeatherModel> = mapOf()
}

data class RawWeatherModel(
    val meta: RawWeatherModelMetaInfo,
    val receiver: RawWeatherModelReceiverInfo,
    val source: RawWeatherModelSourceFileInfo,
    val mapping: RawWeatherModelMappingInfo,
    val storage: RawWeatherModelStorageInfo,
    val cache: RawWeatherModelCacheInfo
)

data class RawWeatherModelMetaInfo(
    val name: String,
    val friendlyName: String,
    val copyright: String
)

data class RawWeatherModelReceiverInfo(
    val receiverName: String,
    val parserName: String
)

data class RawWeatherModelSourceFileInfo(
    val prefix: String,
    val postfix: String,
    val dateFormat: String,
    val updateFrequency: Int
)

data class RawWeatherModelMappingInfo(
    val variableMapperFile: String,
    val unitMapperFile: String
)

data class RawWeatherModelStorageInfo(
    val storageLocationName: String,
    val subFolderName: String? = null
    // TODO: properties for auto-deletion/storage policy
)

data class RawWeatherModelCacheInfo(
    val variablesInMemory: Set<WeatherVariableType> = setOf(),
    val ignoredVariables: Set<WeatherVariableType> = setOf()
)