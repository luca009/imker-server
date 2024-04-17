package com.luca009.imker.server

import com.luca009.imker.server.caching.*
import com.luca009.imker.server.caching.model.*
import com.luca009.imker.server.configuration.WeatherVariableTypeMapperImpl
import com.luca009.imker.server.configuration.WeatherVariableUnitMapperImpl
import com.luca009.imker.server.configuration.model.WeatherModel
import com.luca009.imker.server.configuration.model.WeatherVariableTypeMapper
import com.luca009.imker.server.configuration.model.WeatherVariableUnitMapper
import com.luca009.imker.server.configuration.properties.QueryProperties
import com.luca009.imker.server.configuration.properties.StorageProperties
import com.luca009.imker.server.configuration.properties.UpdateProperties
import com.luca009.imker.server.management.files.BestFileSearchServiceImpl
import com.luca009.imker.server.management.files.DataFileNameManagerImpl
import com.luca009.imker.server.management.files.LocalFileManagerServiceImpl
import com.luca009.imker.server.management.files.model.*
import com.luca009.imker.server.management.models.WeatherModelManagerServiceImpl
import com.luca009.imker.server.management.models.WeatherModelUpdateJobEnabled
import com.luca009.imker.server.management.models.model.WeatherModelManagerService
import com.luca009.imker.server.parser.DynamicDataParserImpl
import com.luca009.imker.server.parser.NetCdfParserImpl
import com.luca009.imker.server.parser.model.*
import com.luca009.imker.server.queries.WeatherDataQueryServiceImpl
import com.luca009.imker.server.queries.model.PreferredWeatherModelMode
import com.luca009.imker.server.queries.model.WeatherDataQueryService
import com.luca009.imker.server.receiver.ftp.FtpClientImpl
import com.luca009.imker.server.receiver.ftp.FtpSingleFileReceiverImpl
import com.luca009.imker.server.receiver.model.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.runBlocking
import org.apache.commons.net.ftp.FTPFile
import org.junit.jupiter.api.*
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatusCode
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.util.Assert
import org.springframework.web.server.ResponseStatusException
import java.io.File
import java.nio.file.Path
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import kotlin.io.path.Path

@SpringBootTest
@ContextConfiguration(classes = [UpdateProperties::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestPropertySource(locations = ["/test-application.yaml"])
final class ImkerServerApplicationTests {
    companion object {
        const val ZAMG_FTP_SERVER = "eaftp.zamg.ac.at"
        const val ZAMG_FTP_INCA_SUBFOLDER = "nowcast"
        const val ZAMG_FTP_AROME_SUBFOLDER = "nwp"

        val testResourcesPath: String = File("src/test/resources").absolutePath
        val testNetcdfFilesPath = Path(testResourcesPath, "inca")
        val testPrimaryNetcdfFilePath: Path = testNetcdfFilesPath.resolve("nowcast_202309091345.nc")
        val testUnitMapperConfigFilePath = Path(testResourcesPath, "unit_map.csv")

        val testIncaVariableNameMapping: Map<WeatherVariableType, String> = mapOf(
            WeatherVariableType.WindDirection10m to "DD",
            WeatherVariableType.WindSpeed10m to "FF",
            WeatherVariableType.GustSpeed10m to "FX",
            WeatherVariableType.RelativeHumidity2m to "RH",
            WeatherVariableType.PrecipitationSum to "RR",
            WeatherVariableType.DewPoint to "TD",
            WeatherVariableType.Temperature2m to "TT"
        )

        val testDates: Set<Pair<Int, ZonedDateTime>> = setOf(
            Pair(1, ZonedDateTime.of(2023, 12, 20, 12, 20, 0, 0, ZoneOffset.UTC)),
            Pair(2, ZonedDateTime.of(2023, 12, 20, 14, 0, 0, 0, ZoneOffset.UTC)),
            Pair(3, ZonedDateTime.of(2023, 12, 21, 12, 0, 0, 0, ZoneOffset.UTC))
        )
        val mockIncaFiles = arrayOf(
            MockFTPFile(true, "nowcast_202309091330.nc"),
            MockFTPFile(true, "nowcast_202309100915.nc"),
            MockFTPFile(true, "nowcast_202309181700.nc")
        )
        val mockAromeFiles = arrayOf(
            MockFTPFile(true, "nwp_2023091006.nc"),
            MockFTPFile(true, "nwp_2023093021.nc"),
            MockFTPFile(true, "nwp_2023100103.nc")
        )
        val compositeCacheConfig = WeatherRasterCompositeCacheConfiguration(
            setOf(WeatherVariableType.Temperature2m),
            setOf()
        )
        val queryProperties = QueryProperties()
    }

    val netCdfParserFactory = {
            netCdfFilePath: Path, variableMapper: WeatherVariableTypeMapper, unitMapper: WeatherVariableUnitMapper -> NetCdfParserImpl(netCdfFilePath, variableMapper, unitMapper)
    }
    val weatherVariableTypeMapperFactory = {
        weatherVariableMap: Map<WeatherVariableType, String> -> WeatherVariableTypeMapperImpl(weatherVariableMap)
    }
    val weatherVariableUnitMapperFactory = {
        weatherVariableMapFile: File -> WeatherVariableUnitMapperImpl(weatherVariableMapFile)
    }

    val ftpClient: FtpClient = FtpClientImpl()

    val incaFileNameManager: DataFileNameManager = DataFileNameManagerImpl(
        "nowcast_",
        ".nc",
        "yyyyMMddHHmm",
        Duration.ofMinutes(15)
    )
    val aromeFileNameManager: DataFileNameManager = DataFileNameManagerImpl(
        "nwp_",
        ".nc",
        "yyyyMMddHH",
        Duration.ofHours(3)
    )

    val bestFileSearchService: BestFileSearchService = BestFileSearchServiceImpl()
    val variableMapper: WeatherVariableTypeMapper = WeatherVariableTypeMapperImpl(testIncaVariableNameMapping)
    val unitMapper: WeatherVariableUnitMapper = WeatherVariableUnitMapperImpl(testUnitMapperConfigFilePath.toFile())
    val netCdfParser: NetCdfParser = netCdfParserFactory(testPrimaryNetcdfFilePath, variableMapper, unitMapper)
    val weatherRasterMemoryCache: WeatherRasterMemoryCache = WeatherRasterMemoryCacheImpl()
    val weatherRasterDiskCache: WeatherRasterDiskCache = WeatherRasterDiskCacheImpl(netCdfParser)
    val weatherRasterCompositeCache: WeatherRasterCompositeCache = WeatherRasterCompositeCacheImpl(
        compositeCacheConfig,
        netCdfParser,
        weatherRasterMemoryCache,
        weatherRasterDiskCache
    )
    val localFileManagerService: LocalFileManagerService = LocalFileManagerServiceImpl(
        StorageProperties().apply {
            this.storageLocations = mutableMapOf("default" to testResourcesPath.toString())
        }
    )

    val mockFtpClient: FtpClient = org.mockito.kotlin.mock()

    val incaReceiver: FtpSingleFileReceiver = FtpSingleFileReceiverImpl(
        DataReceiverConfiguration(
            "INCA",
            Duration.ofMinutes(15),
            testNetcdfFilesPath,
            "default",
            FtpClientConfiguration(
                Companion.ZAMG_FTP_SERVER
            ),
            ZAMG_FTP_INCA_SUBFOLDER,
        ),
        incaFileNameManager,
        bestFileSearchService,
        mockFtpClient
    )

    val incaModel = WeatherModel(
        "INCA",
        "INCA",
        "GeoSphere Austria under CC BY-SA 4.0",

        incaReceiver,
        DynamicDataParserImpl(netCdfParser, netCdfParserFactory, testNetcdfFilesPath, bestFileSearchService, incaFileNameManager, variableMapper, unitMapper),
        weatherVariableTypeMapperFactory(testIncaVariableNameMapping),
        incaFileNameManager,
        weatherVariableUnitMapperFactory(testUnitMapperConfigFilePath.toFile()),

        WeatherRasterCompositeCacheConfiguration(
            setOf(
                // variables in memory
                WeatherVariableType.Temperature2m
            ),
            setOf() // ignored variables
        ),
        LocalFileManagementConfiguration(
            testNetcdfFilesPath,
            null,
            null
        )
    )

    val weatherModels: SortedMap<Int, WeatherModel> = sortedMapOf(
        0 to incaModel
    )

    val weatherDataCompositeCacheFactory = {
            configuration: WeatherRasterCompositeCacheConfiguration, dataParser: WeatherDataParser -> WeatherRasterCompositeCacheImpl(configuration, dataParser, weatherRasterMemoryCache, weatherRasterDiskCache)
    }
    val weatherModelManagerService: WeatherModelManagerService = WeatherModelManagerServiceImpl(
        weatherModels,
        weatherDataCompositeCacheFactory,
        localFileManagerService
    )

    val weatherDataQueryService: WeatherDataQueryService = WeatherDataQueryServiceImpl(weatherModelManagerService, queryProperties)

    val dynamicNetCdfParser: DynamicDataParser = DynamicDataParserImpl(
        netCdfParser,
        netCdfParserFactory,
        testNetcdfFilesPath,
        bestFileSearchService,
        incaFileNameManager,
        variableMapper,
        unitMapper
    )

    @BeforeAll
    fun setupMockFtpClient(): Unit = runBlocking {
//        whenever(mockFtpClient.connect(org.mockito.kotlin.any<FtpClientConfiguration>()))
//        whenever(mockFtpClient.connect(anyString(), anyString(), anyString()))
        whenever(mockFtpClient.isConnected()).thenReturn(true)
        whenever(mockFtpClient.listFiles(ZAMG_FTP_INCA_SUBFOLDER)).thenReturn(mockIncaFiles)
        whenever(mockFtpClient.listFiles(ZAMG_FTP_AROME_SUBFOLDER)).thenReturn(mockAromeFiles)
        whenever(mockFtpClient.downloadFile(
            org.mockito.kotlin.any(),
            org.mockito.kotlin.any(),
            org.mockito.kotlin.anyOrNull()
        )).thenAnswer {
            val path = it.arguments[1]!! as Path
            var fileName = it.arguments[2]?.toString()

            if (fileName == null) {
                fileName = path.fileName.toString()
            }

            val location = Path(path.toString(), fileName)
            FtpClientProgress(
                flowOf(100),
                location
            )
        }
    }

    @BeforeAll
    fun setupModelManager() = runBlocking {
        weatherModelManagerService.beginUpdateWeatherModels(
            updateSource = WeatherModelUpdateJobEnabled.Disabled,
            updateParser = WeatherModelUpdateJobEnabled.Disabled,
            updateCache = WeatherModelUpdateJobEnabled.Forced,
            cleanupStorage = WeatherModelUpdateJobEnabled.Disabled
        )
    }

    @Test
    fun contextLoads() {
    }

    @Test
    @Disabled // This test pings an actual FTP server, not really the best look :(
    fun ftpClientConnects() {
        ftpClient.connect(Companion.ZAMG_FTP_SERVER, "anonymous", "")
        Assert.isTrue(ftpClient.isConnected(), "FtpClient did not connect successfully")
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
    fun specificIncaFileDownloads(): Unit = runBlocking {
        // 2020-10-14 at 16:43:10 CEST
        val failingDateTime = ZonedDateTime.of(2020, 10, 14, 16, 43, 10, 0, ZoneOffset.ofHours(2))

        try {
            incaReceiver.downloadData(failingDateTime).onCompletion {
                throw IllegalArgumentException("Download succeeded for file that does not exist.")
            }
        } catch (e: Exception) {
            if (e !is BestFileNotFoundException) {
                throw e
            }
        }


        // 2023-09-10 at 12:05:10 CEST
        val succeedingDateTime = ZonedDateTime.of(2023, 9, 10, 12, 5, 10, 0, ZoneOffset.ofHours(2))

        incaReceiver.downloadData(succeedingDateTime).catch {
            throw IllegalArgumentException("Download did not succeed for file that should exist.")
        }
    }

    @Test
    fun latestIncaFileDownloads(): Unit = runBlocking {
        incaReceiver.downloadData(ZonedDateTime.now()).catch {
            throw IllegalArgumentException("Download did not succeed for file that should exist.")
        }
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
    @Order(2)
    fun netCdfParserWorks() {
        // Raw variable count
        val rawVariables = netCdfParser.getAvailableRawVariables()
        Assert.isTrue(rawVariables.count() == 14, "Raw variable count in NetCDF file was not correct")

        // Parsed variable count
        val variables = netCdfParser.getAvailableVariables()
        Assert.isTrue(variables.count() == 7, "Raw variable count in NetCDF file was not correct")

        // Get variable info
        val temperatureVariable = netCdfParser.getVariable(WeatherVariableType.Temperature2m)
        Assert.notNull(temperatureVariable, "Temperature variable was null")

        // Get times
        val times = netCdfParser.getTimes(WeatherVariableType.Temperature2m)!!
        Assert.isTrue(times.count() == 13, "Times count was incorrect")

        // Get variable 2d slice
        val temperatureSlice = netCdfParser.getGridRasterSlice(WeatherVariableType.Temperature2m, times.first())
        val indirectTemperaturePoint = temperatureSlice?.getDoubleOrNull(50, 100) // coordinates insignificant
        Assert.isTrue(indirectTemperaturePoint is Double, "Temperature point was not a double")

        // Get variable slice at point
        val temperaturePoint = netCdfParser.getGridTimeAnd2dPositionSlice(WeatherVariableType.Temperature2m, times.first(), WeatherVariable2dCoordinate(50, 100)) // same coordinates as above
        Assert.isTrue(temperaturePoint is Double, "Temperature point was not a double")

        // Compare getting value directly at point and via the 2d slice
        Assert.isTrue(indirectTemperaturePoint == temperaturePoint, "Getting temperature value via 2d slice did not yield the same result as getting it directly")

        // Get if coordinates are in the dataset
        val correctCoordinatesInDataset = netCdfParser.containsLatLon(WeatherVariableType.Temperature2m, 48.20847274949422, 16.373155534546584) // Vienna
        Assert.isTrue(correctCoordinatesInDataset, "Correct coordinates were not contained in dataset")
        val incorrectCoordinatesInDataset = netCdfParser.containsLatLon(WeatherVariableType.Temperature2m, 47.500810753017205, 19.05394481893561) // Budapest
        Assert.isTrue(!incorrectCoordinatesInDataset, "Incorrect coordinates were contained in dataset")

        // Get coordinates from latlon
        val coordinates = netCdfParser.latLonToCoordinates(WeatherVariableType.Temperature2m, 48.20847274949422, 16.373155534546584) // Vienna
        requireNotNull(coordinates) { "Coordinates were null" }
        Assert.isTrue(coordinates.xIndex == 605 && coordinates.yIndex == 293, "Coordinates were incorrect")
    }

    @Test
    @Order(2)
    fun compositeWeatherDataCacheWorks(): Unit = runBlocking {
        // Testing composite cache
        val temperatureVariableExistsInCompositeCache = weatherRasterCompositeCache.variableExists(WeatherVariableType.Temperature2m)
        Assert.isTrue(temperatureVariableExistsInCompositeCache, "Temperature variable was not in the composite cache despite being configured to be so")


        // Get times
        val times = weatherRasterCompositeCache.getTimes(WeatherVariableType.Temperature2m)!!
        Assert.isTrue(times.count() == 13, "Time count was not correct when returned from the composite cache")


        // Testing memory cache directly
        val temperatureVariableExistsInMemoryCache = weatherRasterMemoryCache.variableExists(WeatherVariableType.Temperature2m)
        Assert.isTrue(temperatureVariableExistsInMemoryCache, "Temperature variable was not in the memory cache despite being configured to be so")

        val windSpeedVariableExistsInMemoryCache = weatherRasterMemoryCache.variableExists(WeatherVariableType.WindSpeed10m)
        Assert.isTrue(!windSpeedVariableExistsInMemoryCache, "Wind speed variable was in the memory cache despite being configured not to be so")

        val temperatureVariableExistsAtTimeInMemoryCache =
            weatherRasterMemoryCache.variableExistsAtTime(WeatherVariableType.Temperature2m, times[2])
        Assert.isTrue(temperatureVariableExistsAtTimeInMemoryCache, "Temperature variable did not exist at specified time in the memory cache despite being configured to be so")

        val temperatureVariableExistsAtPointInMemoryCache =
            weatherRasterMemoryCache.variableExistsAtTimeAndPosition(WeatherVariableType.Temperature2m, times[0], WeatherVariable2dCoordinate(700, 430)) // Note: these are the maximum indices of the x and y coordinates in the INCA dataset respectively
        Assert.isTrue(temperatureVariableExistsAtPointInMemoryCache, "Temperature variable did not exist at specified point in the memory cache despite being configured to be so")


        // Testing disk cache via composite cache
        val windSpeedVariableExistsInCompositeCache = weatherRasterCompositeCache.variableExists(WeatherVariableType.WindSpeed10m)
        Assert.isTrue(windSpeedVariableExistsInCompositeCache, "Wind speed variable was not in the composite cache despite being configured to be so")

        val windSpeedVariableExistsAtTimeInCompositeCache =
            weatherRasterCompositeCache.variableExistsAtTime(WeatherVariableType.WindSpeed10m, times[2])
        Assert.isTrue(windSpeedVariableExistsAtTimeInCompositeCache, "Wind speed variable did not exist at specified time in the composite cache despite being configured to be so")

        val windSpeedVariableExistsAtPointInCompositeCache =
            weatherRasterCompositeCache.variableExistsAtTimeAndPosition(WeatherVariableType.WindSpeed10m, times[0], WeatherVariable2dCoordinate(700, 430)) // Note: these are the maximum indices of the x and y coordinates in the INCA dataset respectively
        Assert.isTrue(windSpeedVariableExistsAtPointInCompositeCache, "Wind speed variable did not exist at specified point in the composite cache despite being configured to be so")


        // Testing negative indices
        val temperatureVariableExistsAtNegativePointInCompositeCache =
            weatherRasterCompositeCache.variableExistsAtTimeAndPosition(WeatherVariableType.Temperature2m, times[0], WeatherVariable2dCoordinate(-1, -1))
        Assert.isTrue(!temperatureVariableExistsAtNegativePointInCompositeCache, "Temperature variable existed at negative point in the composite cache")

        val windSpeedVariableExistsAtNegativePointInCompositeCache =
            weatherRasterCompositeCache.variableExistsAtTimeAndPosition(WeatherVariableType.WindSpeed10m, times[0], WeatherVariable2dCoordinate(-1, -1))
        Assert.isTrue(!windSpeedVariableExistsAtNegativePointInCompositeCache, "Wind speed variable existed at negative point in the composite cache")
    }

    @Test
    fun fileManagerDeletionWorks() {
        // Testing without config
        val noFilesForDeletion = localFileManagerService.getFilesForCleanup(incaModel)
        Assert.isTrue(noFilesForDeletion?.isEmpty() ?: false, "Cleanup policy with no files expected to be deleted returned files to be deleted or null")


        // Testing maximum file count
        val incaWithMaxCountConfig = incaModel.copy(
            fileManagementConfiguration = LocalFileManagementConfiguration(
                testNetcdfFilesPath,
                null,
                1u
            )
        )
        val maxCountFilesForDeletion = localFileManagerService.getFilesForCleanup(incaWithMaxCountConfig)
        Assert.isTrue(maxCountFilesForDeletion?.count() == 1, "Cleanup policy with a maximum file count of 1 returned ${maxCountFilesForDeletion?.count()} files instead")
        Assert.isTrue(maxCountFilesForDeletion?.firstOrNull()?.first?.toPath() == testPrimaryNetcdfFilePath, "Cleanup policy with a maximum file count of 1 returned the wrong file to be deleted")


        // Testing maximum age
        val incaWithMaxAgeConfig = incaModel.copy(
            fileManagementConfiguration = LocalFileManagementConfiguration(
                testNetcdfFilesPath,
                Duration.ofMinutes(15),
                null
            )
        )

        // 2023-09-09 at 14:10:00 UTC (10 minutes after the second test file)
        val referenceDateTime = ZonedDateTime.of(2023, 9, 9, 14, 10, 0, 0, ZoneOffset.UTC)
        val maxAgeFilesForDeletion = localFileManagerService.getFilesForCleanup(incaWithMaxAgeConfig, referenceDateTime)
        Assert.isTrue(maxAgeFilesForDeletion?.count() == 1, "Cleanup policy with a maximum age of 15 minutes returned ${maxAgeFilesForDeletion?.count()} files instead of the expected 1")
        Assert.isTrue(maxAgeFilesForDeletion?.firstOrNull()?.first?.toPath() == testPrimaryNetcdfFilePath, "Cleanup policy with a maximum age of 15 minutes returned the wrong file to be deleted")

        // 2023-09-09 at 14:15:00 UTC (15 minutes after the second test file)
        // Explanation: because the maximum age is set to be 15 minutes, files that are exactly 15 minutes old shouldn't be deleted because they are still within the maximum age
        val edgeCaseReferenceDateTime = ZonedDateTime.of(2023, 9, 9, 14, 15, 0, 0, ZoneOffset.UTC)
        val maxAgeEdgeCaseFilesForDeletion = localFileManagerService.getFilesForCleanup(incaWithMaxAgeConfig, edgeCaseReferenceDateTime)
        Assert.isTrue(maxAgeEdgeCaseFilesForDeletion?.count() == 1, "Cleanup policy with a maximum age of 15 minutes returned ${maxAgeEdgeCaseFilesForDeletion?.count()} files instead of the expected 1")
        Assert.isTrue(maxAgeEdgeCaseFilesForDeletion?.firstOrNull()?.first?.toPath() == testPrimaryNetcdfFilePath, "Cleanup policy with a maximum age of 15 minutes returned the wrong file to be deleted")

        // 2023-09-09 at 14:20:00 UTC (20 minutes after the second test file)
        val allFilesReferenceDateTime = ZonedDateTime.of(2023, 9, 9, 14, 20, 0, 0, ZoneOffset.UTC)
        val maxAgeAllFilesForDeletion = localFileManagerService.getFilesForCleanup(incaWithMaxAgeConfig, allFilesReferenceDateTime)
        Assert.isTrue(maxAgeAllFilesForDeletion?.count() == 2, "Cleanup policy with a maximum age of 15 minutes returned ${maxAgeAllFilesForDeletion?.count()} files instead of the expected 2")
        Assert.isTrue(maxAgeAllFilesForDeletion?.firstOrNull()?.first?.toPath() == testPrimaryNetcdfFilePath, "Cleanup policy with a maximum age of 15 minutes returned the wrong file to be deleted")
    }

    @Test
    fun weatherModelManagerSelectionWorks() {
        val preferredWeatherModel = weatherModelManagerService.getPreferredWeatherModelForLatLon(
            WeatherVariableType.Temperature2m,
            48.20847274949422,
            16.373155534546584
        ) // Vienna
        Assert.isTrue(preferredWeatherModel?.name == "INCA", "Preferred weather model was not INCA")
    }

    @Test
    @Order(3)
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
    @Order(2)
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
        } catch (ex: Exception) {
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
            setOf(WeatherVariableType.Temperature2m, WeatherVariableType.WindSpeed10m),
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

