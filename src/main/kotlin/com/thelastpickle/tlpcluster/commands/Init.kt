package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.DockerCompose
import com.thelastpickle.tlpcluster.Utils
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import java.io.File
import org.apache.commons.io.FileUtils
import com.thelastpickle.tlpcluster.terraform.Configuration

sealed class CopyResourceResult {
    class Created(val fp: File) : CopyResourceResult()
    class Existed(val fp: File) : CopyResourceResult()
}

@Parameters(commandDescription = "Initialize this directory for tlp-cluster")
class Init : ICommand {

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
        val config = Configuration(configTags)

        config.numCassandraInstances = cassandraInstances
        config.numStressInstances = stressInstances

        config.region = region
        config.stressAMI = "ami-51537029"
        config.stressInstanceType = instanceType

        val configOutput = File("terraform.tf.json")
        config.write(configOutput)


        // add required tags to variable file
        val terraform = File("terraform.tfvars")
        terraform.appendText("\n")

        terraform.appendText("client = \"$client\"\n")
        terraform.appendText("ticket = \"$ticket\"\n")
        terraform.appendText("purpose = \"$purpose\"\n")

        val composeResult = DockerCompose().run("terraform", arrayOf("init", "/local"))

        composeResult.fold(
                {
                    println(it.output)
                    println(it.err)
                    println("Your environment has been set up.  Please edit your terraform.tfvars then run 'tlp-cluster up' to start your AWS nodes.")

                },
                {
                    println(it.message)
                    System.exit(1)
                }
        )

        if(start) {
            Up().execute()
        }


    }


}