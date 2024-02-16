package com.luca009.imker.imkerserver.configuration

import com.luca009.imker.imkerserver.caching.model.WeatherRasterCompositeCacheConfiguration
import com.luca009.imker.imkerserver.configuration.model.WeatherModel
import com.luca009.imker.imkerserver.configuration.model.WeatherModelPropertyMapperService
import com.luca009.imker.imkerserver.configuration.model.WeatherVariableFileNameMapper
import com.luca009.imker.imkerserver.configuration.model.WeatherVariableUnitMapper
import com.luca009.imker.imkerserver.configuration.properties.ModelProperties
import com.luca009.imker.imkerserver.configuration.properties.RawWeatherModel
import com.luca009.imker.imkerserver.filemanager.model.DataFileNameManager
import com.luca009.imker.imkerserver.filemanager.model.LocalFileManagerService
import com.luca009.imker.imkerserver.parser.model.DynamicDataParser
import com.luca009.imker.imkerserver.parser.model.WeatherDataParser
import com.luca009.imker.imkerserver.parser.model.WeatherVariableType
import com.luca009.imker.imkerserver.receiver.model.DataReceiver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Path
import java.util.*

@Component
class WeatherModelPropertyMapperServiceImpl(
    private val dataReceiverFactory: (String, DataFileNameManager, Int, Path) -> DataReceiver?,
    private val weatherVariableFileNameMapperFactory: (File) -> WeatherVariableFileNameMapper,
    private val weatherVariableUnitMapperFactory: (File) -> WeatherVariableUnitMapper,
    private val weatherDataParserFactoryFactory: (String) -> ((String) -> WeatherDataParser)?,
    private val dynamicDataParserFactory: ((String) -> WeatherDataParser, String, DataFileNameManager) -> DynamicDataParser,
    private val dataFileNameManagerFactory: (String, String, String, Int) -> DataFileNameManager,
    private val fileManagerService: LocalFileManagerService,
    weatherModelProperties: ModelProperties
): WeatherModelPropertyMapperService {
    private val weatherModels: SortedMap<Int, WeatherModel>
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    init {
        weatherModels = weatherModelProperties.definedModels.mapNotNull {
            Pair(
                it.key,
                assembleWeatherModel(it.value) ?: return@mapNotNull null
            )
        }.toMap().toSortedMap()
    }

    fun assembleWeatherModel(rawWeatherModel: RawWeatherModel): WeatherModel? {
        // Get the factory for our desired DataParser
        val dataParserFactory = weatherDataParserFactoryFactory(rawWeatherModel.receiver.parserName)
        requireNotNull(dataParserFactory) {
            logger.error("Assembling ${rawWeatherModel.meta.name}: Could not resolve data parser \"${rawWeatherModel.receiver.parserName}\". Check if your weather models are configured correctly.")
            return null
        }

        // Instantiate a DynamicDataParser with the factory from before
        val fileNameManager = dataFileNameManagerFactory(rawWeatherModel.source.prefix, rawWeatherModel.source.postfix, rawWeatherModel.source.dateFormat, rawWeatherModel.source.updateFrequency)
        val storagePath = fileManagerService.getWeatherDataLocation(rawWeatherModel.storage.storageLocationName, rawWeatherModel.storage.subFolderName).toAbsolutePath()
        val dataParser = dynamicDataParserFactory(dataParserFactory, storagePath.toString(), fileNameManager)

        val dataReceiver = dataReceiverFactory(rawWeatherModel.receiver.receiverName, fileNameManager, rawWeatherModel.source.updateFrequency, storagePath)
        requireNotNull(dataReceiver) {
            logger.error("Assembling ${rawWeatherModel.meta.name}: Could not resolve data receiver \"${rawWeatherModel.receiver.receiverName}\". Check if your weather models are configured correctly.")
            return null
        }

        val variableMapperFile = File(rawWeatherModel.mapping.variableMapperFile)
        require(variableMapperFile.exists()) {
            logger.error("Assembling ${rawWeatherModel.meta.name}: Variable mapping file \"${rawWeatherModel.mapping.variableMapperFile}\" does not exist. Check if your weather models are configured correctly.")
            return null
        }
        val variableMapper = weatherVariableFileNameMapperFactory(variableMapperFile)

        val unitMapperFile = File(rawWeatherModel.mapping.unitMapperFile)
        require(unitMapperFile.exists()) {
            logger.error("Assembling ${rawWeatherModel.meta.name}: Unit mapping file \"${rawWeatherModel.mapping.unitMapperFile}\" does not exist. Check if your weather models are configured correctly.")
            return null
        }
        val unitMapper = weatherVariableUnitMapperFactory(unitMapperFile)

        val memoryVariables = mapVariableNames(rawWeatherModel.cache.variablesInMemory, rawWeatherModel.meta.name)
        val ignoredVariables = mapVariableNames(rawWeatherModel.cache.ignoredVariables, rawWeatherModel.meta.name)

        val cacheConfig = WeatherRasterCompositeCacheConfiguration(
            memoryVariables,
            ignoredVariables
        )

        return WeatherModel(
            rawWeatherModel.meta.name,
            rawWeatherModel.meta.friendlyName,
            rawWeatherModel.meta.copyright,
            dataReceiver,
            dataParser,
            variableMapper,
            unitMapper,
            cacheConfig
        )
    }

    fun mapVariableNames(names: Set<String>, weatherModelName: String): Set<WeatherVariableType> {
        return names.mapNotNull {
            val enum = WeatherVariableType.values().firstOrNull { enum -> it == enum.name } // get the first enum that matches the name
            requireNotNull(enum) {
                // Did not find an enum with the specified name
                logger.error("Assembling ${weatherModelName}: Could not find weather variable \"${it}\". Check if your weather models are configured correctly.")
                return@mapNotNull null
            }

            return@mapNotNull enum
        }.toSet()
    }

    override fun getWeatherModels(): SortedMap<Int, WeatherModel> {
        return weatherModels
    }
}