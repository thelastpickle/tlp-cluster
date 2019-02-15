package com.thelastpickle.tlpcluster.terraform

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.thelastpickle.tlpcluster.Context
import java.io.File

class Configuration(val tags: MutableMap<String, String> = mutableMapOf(),
                    var region: String = "us-west-2",
                    var context: Context) {

    var numCassandraInstances = 3
    val usEebs = false
    var email = ""


    var cassandraInstanceType = "m5d.xlarge"
    val cassandraAMI = "ami-51537029"

    // stress
    var numStressInstances = 0
    var stressAMI = "ami-51537029"
    var stressInstanceType = "c5d.2xlarge"

    // no way of enabling this right now
    val monitoring = false

    private val config  = TerraformConfig(region, context.userConfig.awsAccessKey, context.userConfig.awsSecret)

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

    fun setResource(key: String, ami: String, instanceType: String, count: Int) : Configuration {
        val conf = Resource(ami, instanceType, tags, count = count)
        config.resource.aws_instance[key] = conf
        return this
    }

    fun setOutput(name: String, value: String) : Configuration {
        config.output[name] = Output.create(value)
        return this
    }

    private fun build() : Configuration {
        // set all the configuration variables
        // update tags
        // Name = "${var.email} {$var.ticket} stress"

        val name = "${tags.getOrDefault("email", "")} ${tags.getOrDefault("ticket", "")}"

        tags["Name"] = name

        setVariable("cassandra_instance_type", cassandraInstanceType)
        setVariable("stress_instance_type", stressInstanceType)
        setVariable("email", email)
        setVariable("security_groups", Variable(listOf(context.userConfig.securityGroup), "list"))
        setVariable("purpose", null)
        setVariable("ticket", null)
        setVariable("client", null)
        setVariable("key_name", context.userConfig.keyName)
        setVariable("cassandra_instance_name", "cassandra-node")
        setVariable("stress_instance_name", "stress-instance")
        setVariable("region", region)
        setVariable("zones", Variable(listOf("us-west-2a", "us-west-2b", "us-west-2c"), "list"))


        setResource("cassandra", cassandraAMI, cassandraInstanceType, numCassandraInstances)
        setResource("stress", stressAMI, stressInstanceType, numStressInstances)

        setOutput("cassandra_ips", "\${aws_instance.cassandra.*.public_ip}")
        setOutput("cassandra_internal_ips", "\${aws_instance.cassandra.*.private_ip}")
        setOutput("stress_ips", "\${aws_instance.stress.*.public_ip}")

        return this
    }

    fun toJSON() : String {
        build()
        return mapper.writeValueAsString(config)
    }

    fun write(f: File) {
        build()
        mapper.writeValue(f, config)
    }
}

class TerraformConfig(@JsonIgnore val region: String,
                      @JsonIgnore val accessKey: String,
                      @JsonIgnore val secret: String) {

    var variable = mutableMapOf<String, Variable>()
    val provider = mutableMapOf("aws" to Provider(region, accessKey, secret))
    val resource = AWSResource()
    val output = mutableMapOf<String, Output>()
}


data class Provider(val region: String,
                    val access_key: String,
                    val secret_key: String)

data class Variable(val default: Any?, val type: String? = null)

data class Resource(val ami: String = "ami-5153702",
                    val instance_type: String = "m5d.xlarge",
                    val tags: Map<String, String> = mapOf(),
                    val security_groups : String = "\${var.security_groups}",
                    val key_name : String = "\${var.key_name}",
                    val count : Int
)

data class AWSResource(var aws_instance : MutableMap<String, Resource> = mutableMapOf() )


data class Output(val value: List<String>) {
    companion object {
        fun create(value: String) : Output {
            return Output(listOf(value))
        }
    }
}