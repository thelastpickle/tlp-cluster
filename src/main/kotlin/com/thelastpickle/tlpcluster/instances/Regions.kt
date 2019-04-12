package com.thelastpickle.tlpcluster.instances

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.thelastpickle.tlpcluster.YamlDelegate
import com.thelastpickle.tlpcluster.instances.importers.InstancesImporter
import com.thelastpickle.tlpcluster.instances.importers.RegionImporter
import com.thelastpickle.tlpcluster.instances.importers.UbuntuImporter
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.util.NoSuchElementException


/**
 * Container and loader for region specific information
 * Here we can grab all the regions and their cooresponding amis and azs
 *
 * This file is generated from other locations
 */
data class Regions(val regions: Map<String, Region>) {

    // maps instance type to ami
    data class Region(val azs: List<String>, val amis: Map<String, String>)

    companion object {
        val log = logger()
        val yaml : ObjectMapper by YamlDelegate()
        val url = this::class.java.getResource("regions.yaml")!!

        fun load() : Regions {
            val regions = yaml.readValue<Regions>(url)
            return regions
        }

        /**
         * Regenerates the local file
         * This is done here to an OutputStream to facilitate testing
         */
        fun createRegions() : Regions {
            val regionImporter = RegionImporter.loadFromJson()
            val ubuntu = UbuntuImporter.loadFromResource()
            val instances = InstancesImporter.loadFromCompressedCSV()

            val regions = mutableMapOf<String, Region>()

            for(region in regionImporter.regions) {
                // get all the AZs - region.zones
                // for each instance type get the right ami
                // we're not launching in the government regions
                if(region.code.contains("gov"))
                    continue
                val amis = mutableMapOf<String, String>()

                for(instance in instances) {
                    // it's possible we don't have an instance for a region
                    // if not, we just skip it.
                    // cn-north-1 is an example as of this writing
                    try {
                        amis[instance.instance_type] = ubuntu.getAmi(region.code, instance.isInstanceRootVolume).ami
                    } catch (e: NoSuchElementException) {
                        continue
                    }
                }
                if(amis.size > 0) {
                    val r = Region(region.zones, amis)
                    regions[region.code] = r
                }

            }

            return Regions(regions)

        }




    }

    operator fun get(region: String) : Region? {
        return regions[region]
    }

    fun write() {
        val fp = File("src/main/resources/com/thelastpickle/tlpcluster/instances/regions.yaml")
        yaml.writeValue(fp, this)
    }
}

/**
 * convenience function to regenerate the regions.yaml
 */
fun main() {
    val regions = Regions.createRegions()
    regions.write()
}