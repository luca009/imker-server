package com.luca009.imker.imkerserver

import com.luca009.imker.imkerserver.caching.*
import com.luca009.imker.imkerserver.caching.model.*
import com.luca009.imker.imkerserver.configuration.WeatherVariableFileNameMapperImpl
import com.luca009.imker.imkerserver.configuration.WeatherVariableUnitMapperImpl
import com.luca009.imker.imkerserver.configuration.model.WeatherModel
import com.luca009.imker.imkerserver.configuration.model.WeatherVariableFileNameMapper
import com.luca009.imker.imkerserver.configuration.model.WeatherVariableUnitMapper
import com.luca009.imker.imkerserver.configuration.properties.QueryProperties
import com.luca009.imker.imkerserver.configuration.properties.UpdateProperties
import com.luca009.imker.imkerserver.filemanager.AromeFileNameManagerImpl
import com.luca009.imker.imkerserver.filemanager.BestFileSearchServiceImpl
import com.luca009.imker.imkerserver.filemanager.IncaFileNameManagerImpl
import com.luca009.imker.imkerserver.filemanager.model.AromeFileNameManager
import com.luca009.imker.imkerserver.filemanager.model.BestFileSearchService
import com.luca009.imker.imkerserver.filemanager.model.IncaFileNameManager
import com.luca009.imker.imkerserver.filemanager.model.LocalFileManagerService
import com.luca009.imker.imkerserver.management.WeatherModelManagerServiceImpl
import com.luca009.imker.imkerserver.management.model.WeatherModelManagerService
import com.luca009.imker.imkerserver.parser.DynamicDataParserImpl
import com.luca009.imker.imkerserver.parser.NetCdfParserImpl
import com.luca009.imker.imkerserver.parser.model.*
import com.luca009.imker.imkerserver.queries.WeatherDataQueryServiceImpl
import com.luca009.imker.imkerserver.queries.model.PreferredWeatherModelMode
import com.luca009.imker.imkerserver.queries.model.WeatherDataQueryService
import com.luca009.imker.imkerserver.receiver.model.DownloadResult
import com.luca009.imker.imkerserver.receiver.ftp.FtpClientImpl
import com.luca009.imker.imkerserver.receiver.inca.IncaReceiverImpl
import com.luca009.imker.imkerserver.receiver.model.FtpClient
import com.luca009.imker.imkerserver.receiver.model.IncaReceiver
import org.apache.commons.net.ftp.FTPFile
import org.junit.jupiter.api.*
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever
import org.mockito.stubbing.Answer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatusCode
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.util.Assert
import org.springframework.web.server.ResponseStatusException
import java.io.File
import java.nio.file.Path
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import kotlin.io.path.Path

@SpringBootTest
@ContextConfiguration(classes = [UpdateProperties::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestPropertySource(locations = ["/test-application.yaml"])
class ImkerServerApplicationTests {
    private final val ZAMG_FTP_SERVER = "eaftp.zamg.ac.at"
    private final val TEST_RESOURCES_PATH = File("src/test/resources").absolutePath
    private final val TEST_NETCDF_FILE_PATH = Path(TEST_RESOURCES_PATH, "inca", "nowcast_202309091345.nc").toString()
    private final val TEST_MAPPER_CONFIG_FILE_PATH = Path(TEST_RESOURCES_PATH, "inca", "inca_map.csv").toString()
    private final val TEST_UNIT_MAPPER_CONFIG_FILE_PATH = Path(TEST_RESOURCES_PATH, "unit_map.csv").toString()
    private final val TEST_DATES: Set<Pair<Int, ZonedDateTime>> = setOf(
        Pair(1, ZonedDateTime.of(2023, 12, 20, 12, 20, 0, 0, ZoneOffset.UTC)),
        Pair(2, ZonedDateTime.of(2023, 12, 20, 14, 0, 0, 0, ZoneOffset.UTC)),
        Pair(3, ZonedDateTime.of(2023, 12, 21, 12, 0, 0, 0, ZoneOffset.UTC))
    )
    private final val MOCK_INCA_FILES = arrayOf(
        MockFTPFile(true, "nowcast_202309091330.nc"),
        MockFTPFile(true, "nowcast_202309100915.nc"),
        MockFTPFile(true, "nowcast_202309181700.nc")
    )
    private final val MOCK_AROME_FILES = arrayOf(
        MockFTPFile(true, "nwp_2023091006.nc"),
        MockFTPFile(true, "nwp_2023093021.nc"),
        MockFTPFile(true, "nwp_2023100103.nc")
    )
    private final val COMPOSITE_CACHE_CONFIG = WeatherRasterCompositeCacheConfiguration(
        setOf(WeatherVariableType.Temperature2m),
        setOf()
    )
    private final val QUERY_PROPERTIES = QueryProperties()

    val netCdfParserFactory = {
            netCdfFilePath: String -> NetCdfParserImpl(netCdfFilePath)
    }
    val weatherVariableFileNameMapperFactory = {
        weatherVariableMapFile: File -> WeatherVariableFileNameMapperImpl(weatherVariableMapFile)
    }
    val weatherVariableUnitMapperFactory = {
        weatherVariableMapFile: File -> WeatherVariableUnitMapperImpl(weatherVariableMapFile)
    }

    val ftpClient: FtpClient = FtpClientImpl()
    val incaFileNameManager: IncaFileNameManager = IncaFileNameManagerImpl()
    val aromeFileNameManager: AromeFileNameManager = AromeFileNameManagerImpl()
    val bestFileSearchService: BestFileSearchService = BestFileSearchServiceImpl()
    val netCdfParser: NetCdfParser = netCdfParserFactory(TEST_NETCDF_FILE_PATH)
    val variableMapper: WeatherVariableFileNameMapper = WeatherVariableFileNameMapperImpl(File(TEST_MAPPER_CONFIG_FILE_PATH))
    val unitMapper: WeatherVariableUnitMapper = WeatherVariableUnitMapperImpl(File(TEST_UNIT_MAPPER_CONFIG_FILE_PATH))
    val weatherTimeCache: WeatherTimeCache = WeatherTimeCacheImpl()
    val weatherUnitCache: WeatherVariableUnitCache = WeatherVariableUnitCacheImpl()
    val weatherRasterMemoryCache: WeatherRasterMemoryCache = WeatherRasterMemoryCacheImpl()
    val weatherRasterDiskCache: WeatherRasterDiskCache = WeatherRasterDiskCacheImpl(
        netCdfParser,
        variableMapper,
        WeatherRasterCacheHelper()
    )
    val weatherRasterCompositeCache: WeatherRasterCompositeCache = WeatherRasterCompositeCacheImpl(
        COMPOSITE_CACHE_CONFIG,
        netCdfParser,
        variableMapper,
        unitMapper,
        weatherRasterMemoryCache,
        weatherRasterDiskCache,
        WeatherRasterCacheHelper(),
        weatherTimeCache,
        weatherUnitCache
    )

    val mockFtpClient: FtpClient = org.mockito.kotlin.mock()
    val mockLocalFileManagerService: LocalFileManagerService = org.mockito.kotlin.mock()

    val weatherModels: SortedMap<Int, WeatherModel> = sortedMapOf(
        0 to WeatherModel(
            "INCA",
            "INCA",
            "GeoSphere Austria under CC BY-SA 4.0",

            IncaReceiverImpl(
                mockLocalFileManagerService,
                incaFileNameManager,
                bestFileSearchService,
                mockFtpClient
            ),
            DynamicDataParserImpl(netCdfParser, netCdfParserFactory, TEST_NETCDF_FILE_PATH, bestFileSearchService, incaFileNameManager),
            weatherVariableFileNameMapperFactory(File(TEST_MAPPER_CONFIG_FILE_PATH)),
            weatherVariableUnitMapperFactory(File(TEST_UNIT_MAPPER_CONFIG_FILE_PATH)),

            WeatherRasterCompositeCacheConfiguration(
                setOf(
                    // variables in memory
                    WeatherVariableType.Temperature2m
                ),
                setOf() // ignored variables
            )
        )
    )
    val weatherDataCompositeCacheFactory = {
            configuration: WeatherRasterCompositeCacheConfiguration, dataParser: WeatherDataParser, variableMapper: WeatherVariableFileNameMapper, unitMapper: WeatherVariableUnitMapper -> WeatherRasterCompositeCacheImpl(configuration, dataParser, variableMapper, unitMapper, weatherRasterMemoryCache, weatherRasterDiskCache, WeatherRasterCacheHelper(), WeatherTimeCacheImpl(), WeatherVariableUnitCacheImpl())
    }
    val weatherModelManagerService: WeatherModelManagerService = WeatherModelManagerServiceImpl(
        weatherModels,
        weatherDataCompositeCacheFactory
    )

    val weatherDataQueryService: WeatherDataQueryService = WeatherDataQueryServiceImpl(weatherModelManagerService, QUERY_PROPERTIES)

    val dynamicNetCdfParser: DynamicDataParser = DynamicDataParserImpl(
        netCdfParser,
        netCdfParserFactory,
        TEST_NETCDF_FILE_PATH,
        bestFileSearchService,
        incaFileNameManager
    )

    @BeforeAll
    fun setupMockLocalFileManager() {
        whenever(mockLocalFileManagerService.getWeatherDataLocation(anyString(), anyString())).thenAnswer(Answer {
            val subPath = it.arguments[0].toString()
            return@Answer Path(TEST_RESOURCES_PATH, subPath)
        })
    }

    @BeforeAll
    fun setupMockFtpClient() {
        whenever(mockFtpClient.connect(anyString(), anyString(), anyString())).thenReturn(true)
        whenever(mockFtpClient.isConnected()).thenReturn(true)
        whenever(mockFtpClient.listFiles(IncaFtpServerConstants.SUB_FOLDER)).thenReturn(MOCK_INCA_FILES)
        whenever(mockFtpClient.listFiles(AromeFtpServerConstants.SUB_FOLDER)).thenReturn(MOCK_AROME_FILES)
        whenever(mockFtpClient.downloadFile(org.mockito.kotlin.any(), org.mockito.kotlin.any(), org.mockito.kotlin.anyOrNull())).thenAnswer {
            val path = it.arguments[1]!! as Path
            var fileName = it.arguments[2]?.toString()

            if (fileName == null) {
                fileName = path.fileName.toString()
            }

            val location = Path(path.toString(), fileName)
            DownloadResult(true, location)
        }
        whenever(mockFtpClient.disconnect()).thenReturn(true)
    }

    @BeforeAll
    fun setupTimeCache() {
        weatherTimeCache.setTimes(WeatherVariableType.Temperature2m, TEST_DATES)
    }
    @BeforeAll
    fun setupWeatherModelManager() {
        weatherModelManagerService.updateWeatherModelCaches()
    }

    @Test
    fun contextLoads() {
    }

    @Test
    fun ftpClientConnects() {
        requireNotNull(ftpClient)

        val connectionSuccess = ftpClient.connect(ZAMG_FTP_SERVER, "anonymous", "")
        Assert.isTrue(ftpClient.isConnected() && connectionSuccess, "FtpClient did not connect successfully")
        ftpClient.disconnect()
        Assert.isTrue(!ftpClient.isConnected(), "FtpClient did not disconnect successfully")
    }

    @Test
    fun incaFileNameGenerationWorks() {
        // 2020-10-14 at 16:43:10 CEST
        val dateTime = ZonedDateTime.of(2020, 10, 14, 16, 43, 10, 0, ZoneOffset.ofHours(2))

        // reference result: 2020-10-14 at 14:30:00 UTC
        val referenceDateTime = ZonedDateTime.of(2020, 10, 14, 14, 30, 0, 0, ZoneOffset.UTC)

        // reference filename (in UTC)
        val referenceFileName = "nowcast_202010141430.nc"

        val roundedDateTime = incaFileNameManager.roundDownToNearestValidDateTime(dateTime)
        Assert.isTrue(roundedDateTime == referenceDateTime, "Rounded DateTime was not equal to the reference DateTime")

        val fileName = incaFileNameManager.getFileNameForDateTime(roundedDateTime)
        Assert.isTrue(fileName == referenceFileName, "Resulting filename was not equal to the reference filename")
    }

    @Test
    fun aromeFileNameGenerationWorks() {
        // 2020-10-14 at 16:43:10 CEST
        val dateTime = ZonedDateTime.of(2020, 10, 14, 16, 43, 10, 0, ZoneOffset.ofHours(2))

        // reference result: 2020-10-14 at 12:00:00 UTC
        val referenceDateTime = ZonedDateTime.of(2020, 10, 14, 12, 0, 0, 0, ZoneOffset.UTC)

        // reference filename (in UTC)
        val referenceFileName = "nwp_2020101412.nc"

        val roundedDateTime = aromeFileNameManager.roundDownToNearestValidDateTime(dateTime)
        Assert.isTrue(roundedDateTime == referenceDateTime, "Rounded DateTime was not equal to the reference DateTime")

        val fileName = aromeFileNameManager.getFileNameForDateTime(roundedDateTime)
        Assert.isTrue(fileName == referenceFileName, "Resulting filename was not equal to the reference filename")
    }

    @Test
    fun specificIncaFileDownloads() {
        val incaReceiver: IncaReceiver = IncaReceiverImpl(mockLocalFileManagerService, incaFileNameManager, bestFileSearchService, mockFtpClient)

        // 2020-10-14 at 16:43:10 CEST
        val failingDateTime = ZonedDateTime.of(2020, 10, 14, 16, 43, 10, 0, ZoneOffset.ofHours(2))

        val failingResult = incaReceiver.downloadData(failingDateTime, null)
        Assert.isTrue(!failingResult.successful, "Download succeeded for file that does not exist.")

        // 2023-09-10 at 12:05:10 CEST
        val succeedingDateTime = ZonedDateTime.of(2023, 9, 10, 12, 5, 10, 0, ZoneOffset.ofHours(2))

        val succeedingResult = incaReceiver.downloadData(succeedingDateTime, null)
        Assert.isTrue(succeedingResult.successful
                && succeedingResult.fileLocation == Path(TEST_RESOURCES_PATH, IncaFileNameConstants.FOLDER_NAME, MOCK_INCA_FILES[1].name),
            "Download did not succeed for file that should exist.")
    }

    @Test
    fun latestIncaFileDownloads() {
        val incaReceiver: IncaReceiver = IncaReceiverImpl(mockLocalFileManagerService, incaFileNameManager, bestFileSearchService, mockFtpClient)

        val result = incaReceiver.downloadData(ZonedDateTime.now(), null)
        Assert.isTrue(result.successful
                && result.fileLocation == Path(TEST_RESOURCES_PATH, IncaFileNameConstants.FOLDER_NAME, MOCK_INCA_FILES[2].name),
            "Download did not succeed for file that should exist.")
    }

//    @Test
//    fun specificAromeFileDownloads() {
//        val incaReceiver: AromeReceiver = IncaReceiverImpl(mockLocalFileManagerService, incaFileNameManager, mockFtpClient)
//
//        // 2020-10-14 at 16:43:10 CEST
//        val failingDateTime = ZonedDateTime.of(2020, 10, 14, 16, 43, 10, 0, ZoneOffset.ofHours(2))
//
//        val failingResult = incaReceiver.downloadData(failingDateTime, null)
//        Assert.isTrue(!failingResult.successful, "Download succeeded for file that does not exist.")
//
//        // 2023-09-10 at 12:05:10 CEST
//        val succeedingDateTime = ZonedDateTime.of(2023, 9, 10, 12, 5, 10, 0, ZoneOffset.ofHours(2))
//
//        val succeedingResult = incaReceiver.downloadData(succeedingDateTime, null)
//        Assert.isTrue(succeedingResult.successful
//                && succeedingResult.fileLocation == Path(EXECUTABLE_PATH, TEST_DATA_SUB_PATH, IncaFileNameConstants.FOLDER_NAME, MOCK_INCA_FILES[1].name),
//            "Download did not succeed for file that should exist.")
//    }
//
//    @Test
//    fun latestAromeFileDownloads() {
//        val incaReceiver: IncaReceiver = IncaReceiverImpl(mockLocalFileManagerService, incaFileNameManager, mockFtpClient)
//
//        val result = incaReceiver.downloadData(ZonedDateTime.now(), null)
//        Assert.isTrue(result.successful
//                && result.fileLocation == Path(EXECUTABLE_PATH, TEST_DATA_SUB_PATH, IncaFileNameConstants.FOLDER_NAME, MOCK_INCA_FILES[2].name),
//            "Download did not succeed for file that should exist.")
//    }

    @Test
    @Order(1)
    fun netCdfParserWorks() {
        // Variable count
        val rawVariables = netCdfParser.getAvailableRawVariables()
        Assert.isTrue(rawVariables.count() == 14, "Variable count in NetCDF file was not correct")

        // Get variable info
        val temperatureVariable = netCdfParser.getRawVariable("TT")
        Assert.notNull(temperatureVariable, "Temperature variable was null")

        // Get variable 2d slice
        val temperatureSlice = netCdfParser.getGridTimeSlice("TT", 0)
        val temperatureRow = temperatureSlice?.firstOrNull()
        if (temperatureRow !is DoubleArray)
            throw IllegalArgumentException("Temperature slice was not a 2d double array")

        // Get variable slice at point
        val temperaturePoint = netCdfParser.getGridTimeAnd2dPositionSlice("TT", 0, WeatherVariable2dCoordinate(0, 0))
        Assert.isTrue(temperaturePoint is Double, "Temperature point was not a double")

        // Get if coordinates are in the dataset
        val correctCoordinatesInDataset = netCdfParser.containsLatLon("TT", 48.20847274949422, 16.373155534546584) // Vienna
        Assert.isTrue(correctCoordinatesInDataset, "Correct coordinates were not contained in dataset")
        val incorrectCoordinatesInDataset = netCdfParser.containsLatLon("TT", 47.500810753017205, 19.05394481893561) // Budapest
        Assert.isTrue(!incorrectCoordinatesInDataset, "Incorrect coordinates were contained in dataset")

        // Get coordinates from latlon
        val coordinates = netCdfParser.latLonToCoordinates("TT", 48.20847274949422, 16.373155534546584) // Vienna
        requireNotNull(coordinates) { "Coordinates were null" }
        Assert.isTrue(coordinates.xIndex == 605 && coordinates.yIndex == 293, "Coordinates were incorrect")
    }

    @Test
    fun compositeWeatherDataCacheWorks() {
        weatherRasterCompositeCache.updateCaches()

        // Testing composite cache
        val temperatureVariableExistsInCompositeCache = weatherRasterCompositeCache.variableExists(WeatherVariableType.Temperature2m)
        Assert.isTrue(temperatureVariableExistsInCompositeCache, "Temperature variable was not in the composite cache despite being configured to be so")


        // Testing memory cache directly
        val temperatureVariableExistsInMemoryCache = weatherRasterMemoryCache.variableExists(WeatherVariableType.Temperature2m)
        Assert.isTrue(temperatureVariableExistsInMemoryCache, "Temperature variable was not in the memory cache despite being configured to be so")

        val windSpeedVariableExistsInMemoryCache = weatherRasterMemoryCache.variableExists(WeatherVariableType.WindSpeed10m)
        Assert.isTrue(!windSpeedVariableExistsInMemoryCache, "Wind speed variable was in the memory cache despite being configured not to be so")

        val temperatureVariableExistsAtTimeInMemoryCache =
            weatherRasterMemoryCache.variableExistsAtTime(WeatherVariableType.Temperature2m, 2)
        Assert.isTrue(temperatureVariableExistsAtTimeInMemoryCache, "Temperature variable did not exist at specified time in the memory cache despite being configured to be so")

        val temperatureVariableExistsAtPointInMemoryCache =
            weatherRasterMemoryCache.variableExistsAtTimeAndPosition(WeatherVariableType.Temperature2m, 0, WeatherVariable2dCoordinate(700, 430)) // Note: these are the maximum indices of the x and y coordinates in the INCA dataset respectively
        Assert.isTrue(temperatureVariableExistsAtPointInMemoryCache, "Temperature variable did not exist at specified point in the memory cache despite being configured to be so")


        // Testing disk cache via composite cache
        val windSpeedVariableExistsInCompositeCache = weatherRasterCompositeCache.variableExists(WeatherVariableType.WindSpeed10m)
        Assert.isTrue(windSpeedVariableExistsInCompositeCache, "Wind speed variable was not in the composite cache despite being configured to be so")

        val windSpeedVariableExistsAtTimeInCompositeCache =
            weatherRasterCompositeCache.variableExistsAtTime(WeatherVariableType.WindSpeed10m, 2)
        Assert.isTrue(windSpeedVariableExistsAtTimeInCompositeCache, "Wind speed variable did not exist at specified time in the composite cache despite being configured to be so")

        val windSpeedVariableExistsAtPointInCompositeCache =
            weatherRasterCompositeCache.variableExistsAtTimeAndPosition(WeatherVariableType.WindSpeed10m, 0, WeatherVariable2dCoordinate(700, 430)) // Note: these are the maximum indices of the x and y coordinates in the INCA dataset respectively
        Assert.isTrue(windSpeedVariableExistsAtPointInCompositeCache, "Wind speed variable did not exist at specified point in the composite cache despite being configured to be so")


        // Testing negative indices
        val temperatureVariableExistsAtNegativePointInCompositeCache =
            weatherRasterCompositeCache.variableExistsAtTimeAndPosition(WeatherVariableType.Temperature2m, 0, WeatherVariable2dCoordinate(-1, -1))
        Assert.isTrue(!temperatureVariableExistsAtNegativePointInCompositeCache, "Temperature variable existed at negative point in the composite cache")

        val windSpeedVariableExistsAtNegativePointInCompositeCache =
            weatherRasterCompositeCache.variableExistsAtTimeAndPosition(WeatherVariableType.WindSpeed10m, 0, WeatherVariable2dCoordinate(-1, -1))
        Assert.isTrue(!windSpeedVariableExistsAtNegativePointInCompositeCache, "Wind speed variable existed at negative point in the composite cache")
    }

    @Test
    fun weatherModelManagerWorks() {
        val preferredWeatherModel = weatherModelManagerService.getPreferredWeatherModelForLatLon(
            WeatherVariableType.Temperature2m,
            48.20847274949422,
            16.373155534546584
        ) // Vienna
        Assert.isTrue(preferredWeatherModel?.name == "INCA", "Preferred weather model was not INCA")
    }

    @Test
    @Order(2)
    fun dynamicDataParserWorks() {
        // try to update at 2000-01-01 at 00:00:00 UTC (invalid as there is no relevant dataset available)
        val invalidUpdateSuccessful = dynamicNetCdfParser.updateParser(ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
        Assert.isTrue(!invalidUpdateSuccessful, "Invalid NetCDF parser update was successful")

        val firstDataSources = dynamicNetCdfParser.getDataSources()

        // try to update at 2023-09-09 at 14:00:00 UTC (valid, there should be a relevant dataset available)
        val validUpdateSuccessful = dynamicNetCdfParser.updateParser(ZonedDateTime.of(2023, 9, 9, 14, 0, 0, 0, ZoneOffset.UTC))
        Assert.isTrue(validUpdateSuccessful, "Valid NetCDF parser update was unsuccessful")

        val secondDataSource = dynamicNetCdfParser.getDataSources()
        Assert.isTrue(firstDataSources != secondDataSource, "Dynamic NetCDF parser was not updated, despite the update being reported as successful")
    }

    @Test
    fun weatherTimeCacheWorks() {
        // 2023-12-21 at 08:00:00 UTC
        // in comparison to the test dates (not to scale :D):
        // --1-----2-----3-- (indices 1, 2 and 3 in the test dataset)
        // -------------T--- (this test date)
        val testDate = ZonedDateTime.of(2023, 12, 21, 8, 0, 0, 0, ZoneOffset.UTC)

        val earliestIndex = weatherTimeCache.getEarliestIndex(WeatherVariableType.Temperature2m, testDate)
        Assert.isTrue(earliestIndex == 2, "WeatherTimeCache earliest date calculation was incorrect")

        val closestIndex = weatherTimeCache.getClosestIndex(WeatherVariableType.Temperature2m, testDate)
        Assert.isTrue(closestIndex == 3, "WeatherTimeCache closest date calculation was incorrect")

        val latestIndex = weatherTimeCache.getLatestIndex(WeatherVariableType.Temperature2m, testDate)
        Assert.isTrue(latestIndex == 3, "WeatherTimeCache latest date calculation was incorrect")
    }

    @Test
    fun queryServiceWorks() {
        // Testing date calculations
        // 2023-09-09 at 13:00:00 UTC
        // (out of range for first data set; too early)
        // TODO: maybe this should still return the next available results?
        val firstDate = ZonedDateTime.of(2023, 9, 9, 13, 0, 0, 0, ZoneOffset.UTC)
        try {
            val firstTemperatureForecast = weatherDataQueryService.getVariableForecast(
                WeatherVariableType.Temperature2m,
                48.20847274949422,
                16.373155534546584,
                firstDate,
                10,
                PreferredWeatherModelMode.Static
            )
        } catch(ex: Exception) {
            Assert.isTrue(ex is ResponseStatusException && ex.statusCode == HttpStatusCode.valueOf(400),
                "Query for an invalid time did not throw a ResponseStatusException with 400 BAD REQUEST")
        }

        // 2023-09-09 at 13:45:00 UTC
        // start time of the first dataset
        val secondDate = ZonedDateTime.of(2023, 9, 9, 13, 45, 0, 0, ZoneOffset.UTC)
        val secondTemperatureForecast = weatherDataQueryService.getVariableForecast(
            WeatherVariableType.Temperature2m,
            48.20847274949422,
            16.373155534546584,
            secondDate,
            15,
            PreferredWeatherModelMode.Static
        )
        Assert.isTrue(secondTemperatureForecast.values.count() == 13, "Query returned wrong amount of forecast results")

        // 2023-09-09 at 13:50:00 UTC
        val thirdDate = ZonedDateTime.of(2023, 9, 9, 13, 50, 0, 0, ZoneOffset.UTC)
        val thirdTemperatureForecast = weatherDataQueryService.getVariableForecast(
            WeatherVariableType.Temperature2m,
            48.20847274949422,
            16.373155534546584,
            thirdDate,
            15,
            PreferredWeatherModelMode.Static
        )
        Assert.isTrue(thirdTemperatureForecast == secondTemperatureForecast, "Query returned wrong forecast results (not equal to previous results despite being expected to be equal)")


        // Testing multi-variable forecasts
        // 2023-09-09 at 13:50:00 UTC
        val windTemperatureForecastDate = ZonedDateTime.of(2023, 9, 9, 13, 50, 0, 0, ZoneOffset.UTC)
        val windTemperatureForecast = weatherDataQueryService.getForecast(
            listOf(WeatherVariableType.Temperature2m, WeatherVariableType.WindSpeed10m),
            48.20847274949422,
            16.373155534546584,
            windTemperatureForecastDate,
            15,
            PreferredWeatherModelMode.Static
        )
        Assert.isTrue(windTemperatureForecast.variables.count() == 2, "Query returned wrong amount of variables")

        val windTemperatureForecastTemperatureVariable = requireNotNull(windTemperatureForecast.variables.firstOrNull { it.variableName == WeatherVariableType.Temperature2m.name }) {
            throw IllegalArgumentException("Temperature forecast was not contained in query result despite being requested")
        }
        Assert.isTrue(windTemperatureForecastTemperatureVariable == secondTemperatureForecast, "Query returned wrong forecast results (not equal to previous results despite being expected to be equal)")


        // Testing dynamic forecasts
        // 2023-09-09 at 13:50:00 UTC
        val dynamicDate = ZonedDateTime.of(2023, 9, 9, 13, 50, 0, 0, ZoneOffset.UTC)
        val dynamicTemperatureForecast = weatherDataQueryService.getVariableForecast(
            WeatherVariableType.Temperature2m,
            48.20847274949422,
            16.373155534546584,
            dynamicDate,
            15,
            PreferredWeatherModelMode.Dynamic
        )
        // TODO: update this once more weather models are introduced
        Assert.isTrue(dynamicTemperatureForecast == secondTemperatureForecast, "Query returned wrong forecast results (not equal to previous results despite being expected to be equal)")


        // Testing all forecasts
        // 2023-09-09 at 13:50:00 UTC
        val allDate = ZonedDateTime.of(2023, 9, 9, 13, 50, 0, 0, ZoneOffset.UTC)
        val allTemperatureForecast = weatherDataQueryService.getVariableForecast(
            WeatherVariableType.Temperature2m,
            48.20847274949422,
            16.373155534546584,
            allDate,
            15,
            PreferredWeatherModelMode.All
        )
        // TODO: update this once more weather models are introduced
        Assert.isTrue(allTemperatureForecast == secondTemperatureForecast, "Query returned wrong forecast results (not equal to previous results despite being expected to be equal)")

        // Testing all forecasts with limits (same settings as above)
        val allLimitedTemperatureForecast = weatherDataQueryService.getVariableForecast(
            WeatherVariableType.Temperature2m,
            48.20847274949422,
            16.373155534546584,
            allDate,
            5,
            PreferredWeatherModelMode.All
        )
        Assert.isTrue(allLimitedTemperatureForecast.values.count() == 5, "Query returned wrong amount of results")
        // TODO: update this once more weather models are introduced
        Assert.isTrue(allLimitedTemperatureForecast.values == secondTemperatureForecast.values.subList(0, 5), "Query returned wrong forecast results (not equal to previous results despite being expected to be equal)")
    }
}

class MockFTPFile : FTPFile {
    private val isFileOverride: Boolean

    constructor(isFile: Boolean, name: String) : super() {
        isFileOverride = isFile
        this.name = name
        this.rawListing = ""
    }

    override fun isFile(): Boolean {
        return isFileOverride
    }

    override fun isValid(): Boolean {
        return true
    }
}

