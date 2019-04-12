package com.thelastpickle.tlpcluster.instances.importers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

//"us-east-1",
// "artful",
// "17.10",
// "amd64",
// "hvm:instance-store",
// "20180621",
// "<a href=\"https://console.aws.amazon.com/ec2/home?region=us-east-1#launchAmi=ami-71e2b40e\">ami-71e2b40e</a>",
// "hvm"

data class UbuntuImporter(val aaData: List<Ami>) {

    val image = "bionic"
    val targetReleaseDate = "20190320"

    @JsonDeserialize(using = AmiDeserializer::class)
    data class Ami(val region: String,
                   val release: String,
                   val releaseDate: String,
                   val instance_type: String,
                   val ami: String)


    class AmiDeserializer : JsonDeserializer<Ami>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Ami {
            val data = json.readValue(p, List::class.java).map { it.toString() }
            return Ami(data[0], data[1], data[5], data[4], extractAmi(data[6]))
        }

    }


    companion object {


        val json = ObjectMapper().registerKotlinModule()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        fun loadFromResource() : UbuntuImporter {
            val data = this::class.java.getResourceAsStream("ubuntu.json")
            return json.readValue(data)
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
                   && it.release == image
                   && it.releaseDate == targetReleaseDate
       }
    }


    fun getAmi(region: String) : Ami {
        val amis = getAmis(region)
        return amis.first()
    }


}