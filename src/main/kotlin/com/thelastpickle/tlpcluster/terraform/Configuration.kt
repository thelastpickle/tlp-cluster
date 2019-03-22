package com.thelastpickle.tlpcluster.terraform

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.configuration.ServerType
import java.io.File

class Configuration(val ticket: String,
                    val client: String,
                    val purpose: String,
                    var region: String = "us-west-2",
                    var context: Context) {

    var numCassandraInstances = 3
    var email = context.userConfig.email

    val tags = mutableMapOf("ticket" to ticket,
        "client" to client,
        "purpose" to purpose,
        "email" to email)

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

    private fun setInstanceResource(key: String,
                                    ami: String,
                                    instanceType: String,
                                    count: Int,
                                    securityGroups: List<String>,
                                    tags: MutableMap<String, String>) : Configuration {
        val conf = InstanceResource(ami, instanceType, tags, security_groups = securityGroups, count = count)
        config.resource.aws_instance[key] = conf
        return this
    }

    private fun setSecurityGroupResource(securityGroup: SecurityGroupResource) : Configuration {
        config.resource.aws_security_group[securityGroup.name] = securityGroup
        return this
    }

    private fun setTagName(tags: MutableMap<String, String>, nodeType: ServerType) : MutableMap<String, String> {
        val newTags = HashMap<String, String>(tags).toMutableMap()
        newTags["Name"] = "${ticket}_${nodeType.serverType}"
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

        val instanceSg = SecurityGroupResource.Builder()
            .newSecurityGroupResource("${ticket}_TlpClusterSG","tlp-cluster ${ticket} security group", tags)
            .withRule(0, 65535, "tcp", listOf("0.0.0.0/0"), "All traffic", SecurityGroupRule.Direction.Outbound)
            .withRule(22, 22, "tcp", listOf("0.0.0.0/0"), "SSH", SecurityGroupRule.Direction.Inbound)
            .withRule(7000, 7001, "tcp", listOf("172.31.0.0/16"), "Intra node", SecurityGroupRule.Direction.Inbound)
            .withRule(7199, 7199, "tcp", listOf("172.31.0.0/16"), "JMX", SecurityGroupRule.Direction.Inbound)
            .withRule(9042, 9042,"tcp", listOf("172.31.0.0/16"), "Native transport", SecurityGroupRule.Direction.Inbound)
            .withRule(9160, 9160,"tcp", listOf("172.31.0.0/16"), "Thrift", SecurityGroupRule.Direction.Inbound)
            .withRule(9090, 9090, "tcp", listOf("0.0.0.0/0"), "Prometheus GUI", SecurityGroupRule.Direction.Inbound)
            .withRule(3000, 3000, "tcp", listOf("0.0.0.0/0"), "Grafana GUI", SecurityGroupRule.Direction.Inbound)
            .withRule(9500, 9500,"tcp", listOf("172.31.0.0/16"), "Prometheus C* agent", SecurityGroupRule.Direction.Inbound)
            .withRule(9501, 9501,"tcp", listOf("172.31.0.0/16"), "Prometheus Stress agent", SecurityGroupRule.Direction.Inbound)
            .build()

        setSecurityGroupResource(instanceSg)

        setInstanceResource(
            "cassandra",
            cassandraAMI,
            cassandraInstanceType,
            numCassandraInstances,
            listOf(instanceSg.name),
            setTagName(tags, ServerType.Cassandra))
        setInstanceResource(
            "stress",
            stressAMI,
            stressInstanceType,
            numStressInstances,
            listOf(instanceSg.name),
            setTagName(tags, ServerType.Stress))
        setInstanceResource(
            "monitoring",
            monitoringAMI,
            monitoringInstanceType,
            if (monitoring) 1 else 0,
            listOf(instanceSg.name),
            setTagName(tags, ServerType.Monitoring))

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

data class InstanceResource(
    val ami: String = "ami-5153702",
    val instance_type: String = "m5d.xlarge",
    val tags: Map<String, String> = mapOf(),
    val security_groups : List<String> = listOf(),
    val key_name : String = "\${var.key_name}",
    val availability_zone: String = "\${element(var.zones, count.index)}",
    val count : Int
)

data class SecurityGroupRule(
    val from_port : Int,
    val to_port: Int,
    val protocol: String = "tcp",
    val cidr_blocks: List<String> = listOf(),
    val description : String,
    @JsonIgnore val direction: Direction
) {
    enum class Direction {
        Inbound,
        Outbound
    }
}

data class SecurityGroupResource(
    val name: String,
    val description : String,
    val tags: Map<String, String>,
    val ingress: List<SecurityGroupRule>,
    val egress: List<SecurityGroupRule>
) {
    class Builder {
        private var name: String = ""
        private var description: String = ""
        private var tags: Map<String, String> = mutableMapOf()
        private var ingress: MutableList<SecurityGroupRule> = mutableListOf()
        private var egress: MutableList<SecurityGroupRule> = mutableListOf()

        fun newSecurityGroupResource(name: String, description: String, tags: Map<String, String>) : Builder {
            this.name = name
            this.description = description
            this.tags = tags

            return this
        }

        fun withRule(
            from_port: Int,
            to_port: Int,
            protocol: String,
            cidr_blocks: List<String>,
            description: String,
            direction: SecurityGroupRule.Direction) : Builder {
            val sgRule = SecurityGroupRule(from_port, to_port, protocol, cidr_blocks, description, direction)

            return withRule(sgRule)
        }

        fun withRule(sgRule: SecurityGroupRule) : Builder {

            val ruleList by lazy {
                if (sgRule.direction == SecurityGroupRule.Direction.Inbound) {
                    this.ingress
                } else {
                    this.egress
                }
            }

            ruleList.add(sgRule)

            return this
        }

        fun build () = SecurityGroupResource(name, description, tags, ingress, egress)
    }
}

data class AWSResource(
    var aws_instance : MutableMap<String, InstanceResource> = mutableMapOf(),
    var aws_security_group : MutableMap<String, SecurityGroupResource> = mutableMapOf()
)


