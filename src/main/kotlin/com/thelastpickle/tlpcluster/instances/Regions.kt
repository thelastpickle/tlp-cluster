package com.thelastpickle.tlpcluster.instances

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.thelastpickle.tlpcluster.YamlDelegate
import org.apache.logging.log4j.kotlin.logger


/**
 * Container and loader for region specific information
 * Here we can grab all the regions and their cooresponding amis and azs
 *
 * This file is generated from other locations
 */
data class Regions(val regions: Map<String, Region>) {

    data class Ami(val ebs: String, val instance: String)
    data class Region(val azs: List<String>, val amis: Ami)

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

