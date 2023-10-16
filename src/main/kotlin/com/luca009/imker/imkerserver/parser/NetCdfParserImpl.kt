package com.luca009.imker.imkerserver.parser

import com.luca009.imker.imkerserver.parser.model.NetCdfParser
import org.springframework.stereotype.Component
import ucar.nc2.dataset.NetcdfDataset
import ucar.nc2.dataset.NetcdfDatasets
import java.io.File

@Component
class NetCdfParserImpl : NetCdfParser {
    override fun openLocalFile(file: File): NetcdfDataset? {
        return try {
            NetcdfDatasets.openDataset(file.toString())
        }
        catch (ex: Exception) {
            null
        }
    }
}