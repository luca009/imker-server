package com.luca009.imker.imkerserver.configuration

import com.luca009.imker.imkerserver.configuration.model.WeatherVariableUnitMapper
import com.luca009.imker.imkerserver.parser.model.WeatherVariableUnit
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File

class WeatherVariableUnitMapperImpl(
    private val configurationFile: File
) : WeatherVariableUnitMapper {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val map: Map<Regex, WeatherVariableUnit>

    init {
        val reader = configurationFile.inputStream().bufferedReader()

        map = reader.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull {
                val (regexString, unitName) = it.split(',', ignoreCase = false, limit = 2)
                val enum = WeatherVariableUnit.values().find { it.toString() == unitName }

                requireNotNull(enum) {
                    logger.warn("Unknown unit type in configuration file ${configurationFile.name}: $unitName")
                    return@mapNotNull null
                }

                val regex = try {
                    Regex(regexString)
                } catch (e: Exception) {
                    logger.warn("Invalid regex in configuration file ${configurationFile.name}: $regexString")
                    return@mapNotNull null
                }

                Pair(
                    regex,
                    enum
                )
            }
            .toMap() // for any duplicate keys, only the last one in the list gets added
    }

    override fun getUnits(unitString: String): WeatherVariableUnit? {
        val matchingUnits = map.filter {
            it.key.matches(unitString)
        }.values

        if (matchingUnits.isEmpty()) {
            logger.warn("No matching units found for \"$unitString\". Defaulting to null.")
            return null
        }

        if (matchingUnits.count() > 1) {
            logger.error("Multiple units match for \"$unitString\": $matchingUnits. Defaulting to null.")
            return null
        }

        return matchingUnits.first()
    }
}