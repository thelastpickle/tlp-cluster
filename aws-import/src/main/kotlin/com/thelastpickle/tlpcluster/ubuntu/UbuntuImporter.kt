package com.thelastpickle.tlpcluster.ubuntu

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.logging.log4j.kotlin.logger
import java.io.File

//"us-east-1",
// "artful",
// "17.10",
// "amd64",
// "hvm:instance-store",
// "20180621",
// "<a href=\"https://console.aws.amazon.com/ec2/home?region=us-east-1#launchAmi=ami-71e2b40e\">ami-71e2b40e</a>",
// "hvm"

data class UbuntuImporter(val aaData: List<Ami>) {
    @JsonDeserialize(using = AmiDeserializer::class)
    data class Ami(val region: String,
                   val release: String,
                   val releaseDate: String,
                   val instance_type: String, // hvm:instance-store or hvm:ebs or hvm:ebs-ssd
                   val ami: String) {

        val isInstanceRootVolume get() = this.instance_type.contains("instance-store")
    }


    class AmiDeserializer : JsonDeserializer<Ami>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Ami {
            val data = json.readValue(p, List::class.java).map { it.toString() }
            return Ami(data[0], data[1], data[5], data[4], extractAmi(data[6]))
        }
    }


    companion object {
        val json = ObjectMapper().registerKotlinModule()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        val image = "bionic"
        val targetReleaseDate = "20190814"

        val log = logger()


        fun loadFromResource() : UbuntuImporter {
            val data = File("data/ubuntu.json").inputStream()
            val tmp = json.readValue<UbuntuImporter>(data)

            // get rid of the images we don't use
            return UbuntuImporter(tmp.aaData.filter {
                it.release == image && it.releaseDate == targetReleaseDate
            })
        }


        fun extractAmi(link: String) : String {
            val regex = ">(ami-[^<]+)".toRegex()
            val match = regex.find(link)!!
            return match.groupValues[1]
        }
    }


    fun getAmis(region: String) : List<Ami> {
       return aaData.filter {
           it.region == region
       }
    }
}