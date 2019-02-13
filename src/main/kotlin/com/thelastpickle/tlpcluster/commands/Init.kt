package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.github.dockerjava.api.async.ResultCallback
import com.thelastpickle.tlpcluster.Context
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import java.io.File
import java.io.Closeable
import org.apache.commons.io.FileUtils
import com.thelastpickle.tlpcluster.terraform.Configuration
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.command.BuildImageResultCallback
import com.github.dockerjava.core.command.AttachContainerResultCallback
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.model.*


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
        val terraform = File("terraform.tfvars")
        terraform.appendText("\n")

        terraform.appendText("client = \"$client\"\n")
        terraform.appendText("ticket = \"$ticket\"\n")
        terraform.appendText("purpose = \"$purpose\"\n")

        val dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://localhost:1234")
                .build()

        val dockerClient = DockerClientBuilder.getInstance(dockerConfig).build()
        val dockerBuildCallback = BuildImageResultCallback()
        val dockerImageName = "terraform"
        val dockerImageTag = "thelastpickle/tlp-cluster/$dockerImageName"

        println("Building Terraform image")

        dockerClient.buildImageCmd()
                .withDockerfile(File("build/resources/main/com/thelastpickle/tlpcluster/commands/origin/Dockerfile"))
                .withTags(hashSetOf(dockerImageTag))
                .exec(dockerBuildCallback)

        val imageId = dockerBuildCallback.awaitImageId()
        val volumeLocal = Volume("/local")

        println("Finished building Terraform image: $imageId")

        val cwdPath = System.getProperty("user.dir")

        println("working dir is: $cwdPath")

        println("Creating Terraform container")

        val dockerContainer = dockerClient.createContainerCmd(dockerImageTag)
                .withVolumes(volumeLocal)
                .withBinds(Bind(cwdPath, volumeLocal, AccessMode.rw))
                .withCmd(mutableListOf("init", "/local"))
                .exec()

        println("Starting Terraform container")

        dockerClient.startContainerCmd(dockerContainer.id).exec()

        var containerState : InspectContainerResponse.ContainerState

        do {
            Thread.sleep(5000)
            containerState = dockerClient.inspectContainerCmd(dockerContainer.id).exec().state
        } while (containerState.running == true)

        if (!containerState.status.equals("exited")) {
            println("Error in execution. Container exited with code : " + containerState.exitCode + ". " + containerState.error)
            return
        }

        println("Container execution completed")

        // clean up after ourselves
        dockerClient.removeContainerCmd(dockerContainer.id)
                .withRemoveVolumes(true)
                .exec()

//        val composeResult = DockerCompose().run("terraform", arrayOf("init", "/local"))
//
//        composeResult.fold(
//                {
//                    println(it.output)
//                    println(it.err)
//                    println("Your environment has been set up.  Please edit your terraform.tfvars then run 'tlp-cluster up' to start your AWS nodes.")
//
//                },
//                {
//                    println(it.message)
//                    System.exit(1)
//                }
//        )

        if(start) {
            Up(context).execute()
        }

    }


}