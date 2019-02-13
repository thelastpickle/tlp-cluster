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

        val volumeLocal = Volume("/local")

        val cwdPath = System.getProperty("user.dir")

        val dockerContainer = context.docker.createContainerCmd("hashicorp/terraform")
                .withVolumes(volumeLocal)
                .withBinds(Bind(cwdPath, volumeLocal, AccessMode.rw))
                .withWorkingDir("/local")
                .withCmd(mutableListOf("init", "/local"))
                .withAttachStdout(true)
                .exec()


        println("working dir is: $cwdPath")

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