package com.thelastpickle.tlpcluster.instances.importers

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

data class RegionImporter(val regions: List<Region>) {

    data class Region(val name: String,
                      val full_name: String,
                      val code: String,
                      val public: Boolean,
                      val zones: List<String>)

    companion object {
        val json = ObjectMapper().registerKotlinModule()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        fun loadFromJson() : RegionImporter {
            val stream = this::class.java.getResourceAsStream("regions.json")
            return RegionImporter(json.readValue(stream))
        }
    }

}