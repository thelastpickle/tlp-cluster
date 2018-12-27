package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.DockerCompose

@Parameters(commandDescription = "Shut down a cluster")
class Down : ICommand {
    override fun execute() {
        println("Crushing dreams, terminating instances.")

        val docker = DockerCompose(inheritIO = true)
        val output = docker.run("terraform", arrayOf("destroy", "-var-file=/user/user.tfvars", "/local" ))

    }
}