package com.luca009.imker.server.parser.model

/**
 * Data class resembling an unparsed weather variable.
 */
data class RawWeatherVariable(
    /**
     * The string associated with the units of this weather variable.
     */
    val unitType: String,

    /**
     * The unique name/identifier associated with this weather variable.
     */
    val name: String,

    /**
     * The long name/description associated with this weather variable.
     */
    val longName: String?,

    /**
     * The number of dimensions this weather variable encompasses.
     */
    val dimensions: Int,

    /**
     * The data type of this weather variable, as defined by the source.
     */
    val type: String?
)

/**
 * Data class resembling a parsed weather variable, with associated [variableTypes] and [unitType].
 */
data class WeatherVariable(
    /**
     * All [WeatherVariableType]s that are associated with this [WeatherVariable].
     */
    val variableTypes: Set<WeatherVariableType>,

    /**
     * The units used by this [WeatherVariable].
     */
    val unitType: WeatherVariableUnit?,

    /**
     * The name/identifier of the [WeatherVariable], as defined by the source.
     */
    val name: String,

    /**
     * The long name/description of the [WeatherVariable], as defined by the source.
     */
    val longName: String?,

    /**
     * The number of dimensions this [WeatherVariable] encompasses.
     */
    val dimensions: Int
)

/**
 * Enum resembling different types of weather variables - this is so that variables (i.e. temperature) are unified across weather models.
 */
enum class WeatherVariableType {
    ConvectiveAvailablePotentialEnergy,
    ConvectiveInhibition,
    GlobalRadiation,
    DewPoint,
    LowCloudCoverage,
    MediumCloudCoverage,
    HighCloudCoverage,
    TotalCloudCoverage,
    Temperature2m,
    Minimum2mTemperature,
    Maximum2mTemperature,
    NetLongWaveRadiationFlux,
    NetShortWaveRadiationFlux,
    RainfallAmount,
    RelativeHumidity2m,
    ShowalterIndex,
    SnowfallAmount,
    Pressure,
    SnowAmount,
    ThermalRadiation,
    SunshineDuration,
    PrecipitationAmount,
    WindSpeedU10m,
    WindSpeedV10m,
    GustSpeedU10m,
    GustSpeedV10m,
    WindSpeed10m,
    WindDirection10m,
    GustSpeed10m,
    GustDirection10m,
    SurfaceGeopotential
}

/**
 * Enum representing different types of real-world units.
 */
enum class WeatherVariableUnit {
    Degree,
    DegreeCelsius,
    Percent,
    MetersPerSecond,
    KilogramPerSquareMeterPerHour
}