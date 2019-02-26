package com.thelastpickle.tlpcluster.terraform

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.configuration.ServerType
import java.io.File

class Configuration(val tags: MutableMap<String, String> = mutableMapOf(),
                    var region: String = "us-west-2",
                    var context: Context) {

    var numCassandraInstances = 3
    val usEebs = false
    var email = context.userConfig.email


    var cassandraInstanceType = "m5d.xlarge"
    val cassandraAMI = "ami-51537029"

    // stress
    var numStressInstances = 0
    var stressAMI = "ami-51537029"
    var stressInstanceType = "c5d.2xlarge"

    //monitoring
    var monitoring = false
    var monitoringAMI = "ami-51537029"
    var monitoringInstanceType = "c5d.2xlarge"

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

    private fun setResource(key: String,
                            ami: String,
                            instanceType: String,
                            count: Int,
                            tags: MutableMap<String, String>) : Configuration {
        val conf = Resource(ami, instanceType, tags, count = count)
        config.resource.aws_instance[key] = conf
        return this
    }

    private fun setTagName(tags: MutableMap<String, String>, nodeType: ServerType) : MutableMap<String, String> {
        val newTags = HashMap<String, String>(tags).toMutableMap()
        val ticketTag by lazy {
            var tagValue = tags.getOrDefault("ticket", "")

            if (tagValue.isNotEmpty()) {
                tagValue = "${tagValue}_"
            }

            tagValue
        }

        newTags["Name"] = "${ticketTag}${nodeType.serverType}"
        return newTags
    }


    private fun build() : Configuration {
        // set all the configuration variables
        // update tags
        // Name = "${var.email} {$var.ticket} stress"

        setVariable("cassandra_instance_type", cassandraInstanceType)
        setVariable("stress_instance_type", stressInstanceType)
        setVariable("email", email)
        setVariable("security_groups", Variable(listOf(context.userConfig.securityGroup), "list"))
        setVariable("key_name", context.userConfig.keyName)
        setVariable("key_path", context.userConfig.sshKeyPath)
        setVariable("cassandra_instance_name", "cassandra-node")
        setVariable("stress_instance_name", "stress-instance")
        setVariable("region", region)
        setVariable("zones", Variable(listOf("us-west-2a", "us-west-2b", "us-west-2c"), "list"))

        setResource("cassandra", cassandraAMI, cassandraInstanceType, numCassandraInstances, setTagName(tags, ServerType.Cassandra))
        setResource("stress", stressAMI, stressInstanceType, numStressInstances, setTagName(tags, ServerType.Stress))
        setResource("monitoring", monitoringAMI, monitoringInstanceType, if (monitoring) 1 else 0, setTagName(tags, ServerType.Monitoring))

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


