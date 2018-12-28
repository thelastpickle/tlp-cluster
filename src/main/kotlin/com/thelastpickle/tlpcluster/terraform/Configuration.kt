package com.thelastpickle.tlpcluster.terraform

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

class Configuration(val tags: Map<String, String> = mapOf()) {
    val numCassandraInstances = 3
    val usEebs = false
    var email = ""


    val cassandraInstanceType = "m5d.xlarge"
    val cassandraAMI = "ami-51537029"

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

    fun setVariable(key: String, default: String?) : Configuration {
        config.variable[key] = Variable(default)
        return this
    }

    fun setVariable(key: String, variable: Variable) : Configuration {
        config.variable[key] = variable
        return this
    }

    fun setResource(key: String, ami: String, instanceType: String) : Configuration {
        val conf = Resource(ami, instanceType, tags)


        config.resource.aws_instance[key] = conf
        return this
    }

    private fun build() : Configuration {
        // set all the configuration variables

        setVariable("cassandra_instance_type", cassandraInstanceType)
        setVariable("stress_instance_type", stressInstanceType)
        setVariable("email", email)
        setVariable("security_groups", Variable(null, "list"))
        setVariable("purpose", null)
        setVariable("ticket", null)
        setVariable("client", null)
        setVariable("key_name", null)
        setVariable("cassandra_count", "$numCassandraInstances")
        setVariable("cassandra_instance_name", "cassandra-node")
        setVariable("stress_count", "$numStressInstances")
        setVariable("stress_instance_name", "stress-instance")
        setVariable("profile", null)
        setVariable("region", region)
        setVariable("zones", Variable(listOf("us-west-2a", "us-west-2b", "us-west-2c"), "list"))


        setResource("cassandra", cassandraAMI, cassandraInstanceType)

        return this
    }

    fun toJSON() : String {
        build()
        return mapper.writeValueAsString(config)
    }


}

class TerraformConfig {
    var variable = mutableMapOf<String, Variable>()
    val provider = mutableMapOf("aws" to Provider("\${var.region}", "/credentials", "\${var.profile}"))
    val resource = AWSResource()
}


data class Provider(val region: String, val shared_credentials_file: String, val profile: String)

data class Variable(val default: Any?, val type: String? = null)

data class Resource(val ami: String = "ami-5153702",
                    val instance_type: String = "m5d.xlarge",
                    val tags: Map<String, String> = mapOf(),
                    val security_groups : String = "\${var.security_groups}",
                    val key_name : String = "\${var.key_name}",
                    val count : String = "\${var.stress_count}"
)

data class AWSResource(var aws_instance : MutableMap<String, Resource> = mutableMapOf() )


