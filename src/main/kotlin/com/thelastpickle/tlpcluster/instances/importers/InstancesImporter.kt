package com.thelastpickle.tlpcluster.instances.importers

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.thelastpickle.tlpcluster.YamlDelegate
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

    /**
     * "instance_type": "c5d.xlarge"
     */
    data class Instance(val instance_type: String,
                        val memory: Double,
                        val ebs_optimized: Boolean,
                        val family: String,
                        val network_performance: String,
                        val physical_processor: String,
                        val vCPU: Int,
                        val ebs_as_nvme: Boolean,
                        val storage: Storage?) {

        /**
         * This doesn't seem to work all the time
         */
        val isInstanceRootVolume get() = this.storage != null
    }


    /**
     * Storage
     * "storage": {
        "devices": 2,
        "includes_swap_partition": false,
        "nvme_ssd": true,
        "size": 1900,
        "ssd": true,
        "storage_needs_initialization": false,
        "trim_support": true
        },
     */
    data class Storage(val devices: Int,
                       val includes_swap_partition: Boolean,
                       val nvme_ssd: Boolean,
                       val size: Int,
                       val ssd: Boolean,
                       val storage_needs_initialization: Boolean,
                       val trim_support: Boolean)


    companion object {
        val json = ObjectMapper().registerKotlinModule()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        val yaml : ObjectMapper by YamlDelegate(true)
        /**
         * used for building out the regions.yaml and instances.yaml
         */
        fun loadFromCompressedCSV() : InstancesImporter {
            val log = logger()

            log.debug { "Loading instance data" }

            val url = GZIPInputStream(this::class.java.getResource("instances.json.gz").openStream())

            return InstancesImporter(json.readValue(url))
        }

        fun loadFromYaml() : InstancesImporter {
            val resource = this::class.java.getResourceAsStream("/com/thelastpickle/tlpcluster/instances/instances.yaml")
            return InstancesImporter(yaml.readValue(resource))
        }

    }


    override fun iterator(): Iterator<Instance> {
        return instances.iterator()
    }


    fun getInstance(name: String): Instance {
        return instances.filter {
            it.instance_type == name
        }.first()
    }


    fun write() {
        val fp = File("src/main/resources/com/thelastpickle/tlpcluster/instances/instances.yaml")
        yaml.writeValue(fp, this.instances)
    }
}


fun main() {
    val instances = InstancesImporter.loadFromCompressedCSV()
    instances.write()
}