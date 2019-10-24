package com.thelastpickle.tlpcluster.ubuntu

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.thelastpickle.tlpcluster.core.YamlDelegate
import org.apache.logging.log4j.kotlin.logger
import java.io.File

/**
 * Container and loader for region specific information
 */
data class Regions(val regions: Map<String, Region>) {

    companion object {
        val log = logger()
        val yaml : ObjectMapper by YamlDelegate()

        // using by lazy here because it might not exist yet
        // There's probably a better way of doing this, but for now, it's what we have.
        val url by lazy {
            try {
                val tmp = this::class.java.getResource("regions.yaml")
                tmp!!
            } catch (e: Exception) {
                println("regions does not exist $e")
                throw e
            }
        }

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

            val regions = mutableMapOf<String, Region>()
            val ubuntu = UbuntuImporter.loadFromResource()

            for(region in regionImporter.regions) {
                // get all the AZs - region.zones
                // for each instance type get the right ami
                // we're not launching in the government regions
                if(region.code.contains("gov"))
                    continue
                val amis = ubuntu.getAmis(region.code)

                val r = Region(region.zones)
                try {
                    r.ebs_ami = amis.filter { it.instance_type.contains("ebs") }.first().ami
                    r.instance_ami = amis.filter { it.instance_type.contains("instance-store") }.first().ami
                    regions[region.code] = r
                } catch (e: Exception) {
                    println("Could not load ami for ${region.code}")
                }
            }
            return Regions(regions)
        }
    }

    fun getAzs(region: String) : List<String> {
        return regions.getValue(region).azs
    }

    operator fun get(region: String) : Region? {
        return regions[region]
    }

    fun write() {
        val path = File("build/aws/com/thelastpickle/tlpcluster/ubuntu/")
        val fp = File(path, "regions.yaml")

        yaml.writeValue(fp, this)
    }
}

/**
 * convenience function to regenerate the regions.yaml
 */
fun main(args: Array<String>) {
    val regions = Regions.createRegions()
    regions.write()
}