package com.luca009.imker.imkerserver.caching.model

interface GeoCoordinateCache {
    fun getClosestLatLonIndices(lat: Float, lon: Float): Pair<Int, Int>
    fun getNeighboringLatLonIndices(lat: Float, lon: Float): Array<Pair<Int, Int>>
    fun set1dCacheData(lat: FloatArray, lon: FloatArray)
    fun set2dCacheData(lat: Array<FloatArray>, lon: Array<FloatArray>)
}