package com.thelastpickle.tlpcluster.instances.importers

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * Loads the instances.json file into objects
 *
 * Original data found here: https://github.com/powdahound/ec2instances.info
 * https://github.com/powdahound/ec2instances.info/blob/master/www/instances.json
 *
 * compressed with gzip -9 instances.json
 *
 */
class InstancesImporter(val instances : List<Instance>) : Iterable<InstancesImporter.Instance> {


    data class Instance(val instance_type: String,
                        val memory: Double,
                        val ebs_optimized: Boolean,
                        val family: String,
                        val network_performance: String,
                        val physical_processor: String,
                        val vCPU: Int,
                        val ebs_as_nvme: Boolean)




    companion object {
        val json = ObjectMapper().registerKotlinModule()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        fun loadFromCSV() : InstancesImporter {
            val log = logger()

            log.debug { "Loading instance data" }

            val url = GZIPInputStream(this::class.java.getResource("instances.json.gz").openStream())

            return InstancesImporter(json.readValue(url))
        }

    }

    override fun iterator(): Iterator<Instance> {
        return instances.iterator()
    }

}