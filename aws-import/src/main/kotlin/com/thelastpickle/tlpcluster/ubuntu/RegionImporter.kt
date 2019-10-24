package com.thelastpickle.tlpcluster.ubuntu

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

/**
 * Pulled from https://raw.githubusercontent.com/jsonmaur/aws-regions/master/regions.json
 * Saved as regions.json
  */
//
data class RegionImporter(val regions: List<Region>) {

    /**
     * Example:
     * name = "N. Virginia"
     * full_name = "US East (N. Virginia)"
     * code = "us-east-1"
     * public = true
     * zones = {ArrayList@2691}  size = 6
     *  0 = "us-east-1a"
     *  1 = "us-east-1b"
     *  2 = "us-east-1c"
     *  3 = "us-east-1d"
     *  4 = "us-east-1e"
     *  5 = "us-east-1f"
     */
    data class Region(val name: String,
                      val full_name: String,
                      val code: String,
                      val public: Boolean,
                      val zones: List<String>)

    companion object {
        val json = ObjectMapper().registerKotlinModule()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        fun loadFromJson() : RegionImporter {
            val stream = File("data/regions.json").inputStream()
            return RegionImporter(json.readValue(stream))
        }
    }

    fun getRegion(shortname: String) : Region {
        //regions.
        return regions.filter {
            it.code == shortname
        }.first()
    }

}