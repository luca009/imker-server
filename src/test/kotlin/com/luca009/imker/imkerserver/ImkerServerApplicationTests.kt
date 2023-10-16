package com.luca009.imker.imkerserver

import com.luca009.imker.imkerserver.filemanager.AromeFileNameManagerImpl
import com.luca009.imker.imkerserver.filemanager.BestFileSearchServiceImpl
import com.luca009.imker.imkerserver.filemanager.IncaFileNameManagerImpl
import com.luca009.imker.imkerserver.filemanager.model.AromeFileNameManager
import com.luca009.imker.imkerserver.filemanager.model.BestFileSearchService
import com.luca009.imker.imkerserver.filemanager.model.IncaFileNameManager
import com.luca009.imker.imkerserver.filemanager.model.LocalFileManagerService
import com.luca009.imker.imkerserver.parser.NetCdfParserImpl
import com.luca009.imker.imkerserver.parser.model.NetCdfParser
import com.luca009.imker.imkerserver.receiver.model.DownloadResult
import com.luca009.imker.imkerserver.receiver.ftp.FtpClientImpl
import com.luca009.imker.imkerserver.receiver.inca.IncaReceiverImpl
import com.luca009.imker.imkerserver.receiver.model.FtpClient
import com.luca009.imker.imkerserver.receiver.model.IncaReceiver
import org.apache.commons.net.ftp.FTPFile
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.InjectMocks
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever
import org.mockito.stubbing.Answer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.util.Assert
import ucar.nc2.constants.FeatureType
import ucar.nc2.dt.grid.GridDataset
import ucar.nc2.ft.FeatureDatasetFactoryManager
import ucar.nc2.util.CancelTask
import java.io.File
import java.nio.file.Path
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.io.path.Path

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImkerServerApplicationTests {
    private final val ZAMG_FTP_SERVER = "eaftp.zamg.ac.at"
    private final val EXECUTABLE_PATH = this::class.java.protectionDomain.codeSource.location.path.substring(1)
    private final val TEST_DATA_SUB_PATH = "TestData"
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

    @InjectMocks
    val ftpClient: FtpClient = FtpClientImpl()
    @InjectMocks
    val incaFileNameManager: IncaFileNameManager = IncaFileNameManagerImpl()
    @InjectMocks
    val aromeFileNameManager: AromeFileNameManager = AromeFileNameManagerImpl()
    @InjectMocks
    val bestFileSearchService: BestFileSearchService = BestFileSearchServiceImpl()
    @InjectMocks
    val netCdfParser: NetCdfParser = NetCdfParserImpl()

    val mockFtpClient: FtpClient = org.mockito.kotlin.mock()
    val mockLocalFileManagerService: LocalFileManagerService = org.mockito.kotlin.mock()

    @BeforeAll
    fun setupMockLocalFileManager() {
        whenever(mockLocalFileManagerService.getWeatherDataLocation(anyString())).thenAnswer(Answer {
            val subPath = it.arguments[0].toString()
            return@Answer Path(EXECUTABLE_PATH, TEST_DATA_SUB_PATH, subPath)
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
                && succeedingResult.fileLocation == Path(EXECUTABLE_PATH, TEST_DATA_SUB_PATH, IncaFileNameConstants.FOLDER_NAME, MOCK_INCA_FILES[1].name),
            "Download did not succeed for file that should exist.")
    }

    @Test
    fun latestIncaFileDownloads() {
        val incaReceiver: IncaReceiver = IncaReceiverImpl(mockLocalFileManagerService, incaFileNameManager, bestFileSearchService, mockFtpClient)

        val result = incaReceiver.downloadData(ZonedDateTime.now(), null)
        Assert.isTrue(result.successful
                && result.fileLocation == Path(EXECUTABLE_PATH, TEST_DATA_SUB_PATH, IncaFileNameConstants.FOLDER_NAME, MOCK_INCA_FILES[2].name),
            "Download did not succeed for file that should exist.")
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

