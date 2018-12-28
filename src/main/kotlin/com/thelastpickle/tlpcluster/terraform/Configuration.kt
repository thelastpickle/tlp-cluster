package com.thelastpickle.tlpcluster.terraform

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

class Configuration {
    val numCassandraInstances = 3
    val usEebs = false

    val cassandraSpec = ServerTypeConfiguration()

    // stress
    val numStressInstances = 0

    val region = "us-west-2"

    // no way of enabling this right now
    val monitoring = false

    private val config  = TerraformConfig()
    val mapper = ObjectMapper()

    init {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    fun setVariable(key: String, default: String) : Configuration {
        config.variable[key] = Variable(default)
        return this
    }

    fun toJSON() : String {


        return mapper.writeValueAsString(config)
    }


}

class TerraformConfig {
    var provider = mapOf<String, Provider>()
    var variable = mutableMapOf<String, Variable>()
}

data class ServerTypeConfiguration(val ami: String = "ami-5153702")

data class Provider(val region: String)

data class Variable(val default: String)

data class Resource(val ami: String = "ami-5153702",
                    val instance_type: String = "m5d.xlarge",
                    val tags: Map<String, String> = mapOf()
)