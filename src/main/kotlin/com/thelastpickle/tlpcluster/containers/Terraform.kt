package com.thelastpickle.tlpcluster.containers

import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.model.AccessMode
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Volume
import com.github.dockerjava.core.command.BuildImageResultCallback
import com.thelastpickle.tlpcluster.Context
import java.io.File

class Terraform(val context: Context) {

    init {

    }

    fun init() {

        val dockerBuildCallback = BuildImageResultCallback()
        val dockerImageName = "terraform"
        val dockerImageTag = "thelastpickle/tlp-cluster/$dockerImageName"

        /// we can use the existing terraform image and ditch the dockerfile
        println("Building Terraform image")

        context.docker.buildImageCmd()
                .withDockerfile(File("build/resources/main/com/thelastpickle/tlpcluster/commands/origin/Dockerfile"))
                .withTags(hashSetOf(dockerImageTag))
                .exec(dockerBuildCallback)

        val imageId = dockerBuildCallback.awaitImageId()
        val volumeLocal = Volume("/local")

        println("Finished building Terraform image: $imageId")

        val cwdPath = System.getProperty("user.dir")

        println("working dir is: $cwdPath")

        println("Creating Terraform container")

        val dockerContainer = context.docker.createContainerCmd(dockerImageTag)
                .withVolumes(volumeLocal)
                .withBinds(Bind(cwdPath, volumeLocal, AccessMode.rw))
                .withCmd(mutableListOf("init", "/local"))
                .exec()

        println("Starting Terraform container")

        context.docker.startContainerCmd(dockerContainer.id).exec()

        var containerState : InspectContainerResponse.ContainerState

        do {
            Thread.sleep(1000)
            containerState = context.docker.inspectContainerCmd(dockerContainer.id).exec().state
        } while (containerState.running == true)

        if (!containerState.status.equals("exited")) {
            println("Error in execution. Container exited with code : " + containerState.exitCode + ". " + containerState.error)
            return
        }

        println("Container execution completed")

        // clean up after ourselves
        context.docker.removeContainerCmd(dockerContainer.id)
                .withRemoveVolumes(true)
                .exec()
    }
}