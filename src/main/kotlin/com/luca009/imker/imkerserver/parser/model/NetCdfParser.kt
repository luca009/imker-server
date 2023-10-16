package com.luca009.imker.imkerserver.parser.model

import ucar.nc2.dataset.NetcdfDataset
import java.io.File

interface NetCdfParser {
    fun openLocalFile(file: File): NetcdfDataset?
}