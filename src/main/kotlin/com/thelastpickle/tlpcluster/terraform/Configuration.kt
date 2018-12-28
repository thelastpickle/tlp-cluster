package com.thelastpickle.tlpcluster.terraform

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

class Configuration {
    val numCassandraInstances = 3
    val usEebs = false
    var email = ""


    val cassandraSpec = ServerTypeConfiguration("m5d.xlarge")
    val cassandraInstanceType = "m5d.xlarge"

    // stress
    val numStressInstances = 0
    var stressInstanceType = "c5d.2xlarge"

    val region = "us-west-2"

    // no way of enabling this right now
    val monitoring = false


    private val config  = TerraformConfig()
    val mapper = ObjectMapper()

    init {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
    }

    fun setVariable(key: String, default: String) : Configuration {
        config.variable[key] = Variable(default)
        return this
    }

    fun setVariable(key: String, variable: Variable) : Configuration {
        config.variable[key] = variable
        return this
    }


    private fun build() : Configuration {
        // set all the configuration variables

        setVariable("cassandra_instance_type", cassandraInstanceType)
        setVariable("stress_instance_type", stressInstanceType)
        setVariable("email", email)
        setVariable("security_groups", Variable(null, "list"))

        return this
    }

    fun toJSON() : String {
        build()
        return mapper.writeValueAsString(config)
    }


}

class TerraformConfig {
    var variable = mutableMapOf<String, Variable>()
    val provider = mapOf("aws" to Provider("\${var.region}", "/credentials", "\${var.profile}"))
}

data class ServerTypeConfiguration(val ami: String = "ami-5153702")

data class Provider(val region: String, val shared_credentials_file: String, val profile: String)

data class Variable(val default: Any?, val type: String? = null)

data class Resource(val ami: String = "ami-5153702",
                    val instance_type: String = "m5d.xlarge",
                    val tags: Map<String, String> = mapOf()
)


