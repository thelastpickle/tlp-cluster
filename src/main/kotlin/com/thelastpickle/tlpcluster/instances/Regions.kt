package com.thelastpickle.tlpcluster.instances

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.thelastpickle.tlpcluster.YamlDelegate
import org.apache.logging.log4j.kotlin.logger


/**
 * Container and loader for region specific information
 * Here we can grab all the regions and their cooresponding amis and azs
 */
data class Regions(val regions: Map<String, Region>) {

    companion object {
        val log = logger()
        val yaml : ObjectMapper by YamlDelegate()

        fun load() : Regions {
            val url = this::class.java.getResource("regions.yaml")
            val regions = yaml.readValue<Regions>(url)
            return regions
        }


    }

}
