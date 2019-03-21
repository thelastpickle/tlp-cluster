package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.Context
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import java.io.File
import org.apache.commons.io.FileUtils
import com.thelastpickle.tlpcluster.terraform.Configuration
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

    @Parameter(description = "Enable monitoring (beta)", names = ["--monitoring", "-m"])
    var monitoringEnabled = false

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

        // Added because if we're reusing a directory, we don't want any of the previous state
        Clean().execute()

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

        val config = Configuration(ticket, client, purpose, region = context.userConfig.region, context = context)

        config.numCassandraInstances = cassandraInstances
        config.numStressInstances = stressInstances
        config.monitoring = monitoringEnabled

        config.region = region
        config.stressAMI = "ami-51537029"
        config.stressInstanceType = instanceType
        config.cassandraInstanceType = instanceType
        config.monitoringInstanceType = instanceType

        config.setVariable("client", client)
        config.setVariable("ticket", ticket)
        config.setVariable("purpose", purpose)

        val configOutput = File("terraform.tf.json")
        config.write(configOutput)


        val terraform = Terraform(context)
        terraform.init()


        if(start) {
            Up(context).execute()
        }

    }


}