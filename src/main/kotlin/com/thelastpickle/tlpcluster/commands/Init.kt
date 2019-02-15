package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.Context
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import java.io.File
import org.apache.commons.io.FileUtils
import com.thelastpickle.tlpcluster.terraform.Configuration
import com.github.dockerjava.core.command.BuildImageResultCallback
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.model.*
import com.thelastpickle.tlpcluster.containers.Terraform


sealed class CopyResourceResult {
    class Created(val fp: File) : CopyResourceResult()
    class Existed(val fp: File) : CopyResourceResult()
}

@Parameters(commandDescription = "Initialize this directory for tlp-cluster")
class Init(val context: Context) : ICommand {

    @Parameter(description = "Client, Ticket, Purpose", required = true, arity = 3)
    var tags = mutableListOf<String>()

    @Parameter(description = "Number of Cassandra instances", names = ["--cassandra", "-c"])
    var cassandraInstances = 3

    @Parameter(description = "Number of stress instances", names = ["--stress", "-s"])
    var stressInstances = 0

    @Parameter(description = "Start instances automatically", names = ["--up"])
    var start = false

    @Parameter(description = "AMI", names = ["--ami"])
    var ami = "ami-51537029"

    @Parameter(description = "Region", names = ["--region"])
    var region = "us-west-2"

    @Parameter(description = "Instance Type", names = ["--instance"])
    var instanceType =  "c5d.2xlarge"

    override fun execute() {
        println("Initializing directory")

        val client = tags[0]
        val ticket = tags[1]
        val purpose = tags[2]

        check(client.isNotBlank())
        check(ticket.isNotBlank())
        check(purpose.isNotBlank())

        // copy provisioning over
        val reflections = Reflections("com.thelastpickle.tlpcluster.commands.origin", ResourcesScanner())

        val provisioning = reflections.getResources(".*".toPattern())

        println("Copying provisioning files")

        for (f in provisioning) {
            val input = this.javaClass.getResourceAsStream("/" + f)
            val outputFile = f.replace("com/thelastpickle/tlpcluster/commands/origin/", "")

            val output = File(outputFile)
            println("Writing ${output.absolutePath}")

            output.absoluteFile.parentFile.mkdirs()
            FileUtils.copyInputStreamToFile(input, output)
        }

        val configTags = mutableMapOf("ticket" to ticket,
                    "client" to client,
                    "purpose" to purpose)

        val config = Configuration(configTags, region = context.userConfig.region, context = context)

        config.numCassandraInstances = cassandraInstances
        config.numStressInstances = stressInstances

        config.region = region
        config.stressAMI = "ami-51537029"
        config.stressInstanceType = instanceType
        config.cassandraInstanceType = instanceType

        val configOutput = File("terraform.tf.json")
        config.write(configOutput)


        // add required tags to variable file
        val terraformVars = File("terraform.tfvars")
        terraformVars.appendText("\n")

        terraformVars.appendText("client = \"$client\"\n")
        terraformVars.appendText("ticket = \"$ticket\"\n")
        terraformVars.appendText("purpose = \"$purpose\"\n")

        val terraform = Terraform(context)
        terraform.init()


        if(start) {
            Up(context).execute()
        }

    }


}