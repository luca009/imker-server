package com.luca009.imker.imkerserver.configuration

import com.luca009.imker.imkerserver.parser.model.WeatherVariableType
import com.luca009.imker.imkerserver.configuration.model.WeatherVariableFileNameMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class WeatherVariableFileNameMapperImpl(
    private val configurationFile: File
) : WeatherVariableFileNameMapper {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val map: Map<WeatherVariableType, Pair<String, String>>

    init {
        val reader = configurationFile.inputStream().bufferedReader()

        map = reader.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull {
                val (enumName, variableName, filePath) = it.split(',', ignoreCase = false, limit = 3)
                val enum = WeatherVariableType.values().find { it.toString() == enumName }

                if (enum == null) {
                    logger.warn("Unknown weather variable in configuration file ${configurationFile.name}: $enumName")
                    return@mapNotNull null
                }

                Pair(
                    enum,
                    Pair(
                        variableName,
                        filePath.removeSurrounding("\"")
                    )
                )
            }
            .toMap() // for any duplicate keys, only the last one in the list gets added
    }

    override fun getWeatherVariables(variableName: String, filePath: String): Set<WeatherVariableType> {
        val matches = map.filterValues { it == Pair(variableName, filePath) }

        if (matches.isEmpty()) {
            logger.warn("Could not find weather variable in configuration file ${configurationFile.name}: $variableName")
            return emptySet()
        }

        return matches.keys
    }

    override fun getWeatherVariableFile(variable: WeatherVariableType): String? {
        return map[variable]?.second
    }

    override fun getWeatherVariableName(variable: WeatherVariableType): String? {
        return map[variable]?.first
    }

    override fun containsWeatherVariable(variable: WeatherVariableType): Boolean {
        return map.containsKey(variable)
    }
}