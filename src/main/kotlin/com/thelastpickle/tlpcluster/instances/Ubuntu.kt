package com.thelastpickle.tlpcluster.instances

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

//"us-east-1",
// "artful",
// "17.10",
// "amd64",
// "hvm:instance-store",
// "20180621",
// "<a href=\"https://console.aws.amazon.com/ec2/home?region=us-east-1#launchAmi=ami-71e2b40e\">ami-71e2b40e</a>",
// "hvm"

data class Ubuntu(val aaData: List<Ami>) {
    data class Ami(val region: String, val release: String, val rootDevice: String, val aki: String)

    companion object {
        val json = ObjectMapper().registerKotlinModule()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    fun loadFromResource() {

    }
}