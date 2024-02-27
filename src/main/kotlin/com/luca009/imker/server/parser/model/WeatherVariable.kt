package com.luca009.imker.server.parser.model

data class RawWeatherVariable(
    val unitType: String,
    val name: String,
    val longName: String?,
    val dimensions: Int,
    val type: String?
)

data class WeatherVariable(
    val variableType: WeatherVariableType,
    val unitType: WeatherVariableUnit,
    val name: String,
    val longName: String?,
    val dimensions: Int
)

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
    PrecipitationSum,
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

enum class WeatherVariableUnit {
    Degree,
    DegreeCelsius,
    Percent,
    MetersPerSecond,
    KilogramPerSquareMeter
}