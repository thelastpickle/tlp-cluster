package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.DockerCompose

@Parameters(commandDescription = "Shut down a cluster")
class Down : ICommand {
    override fun execute() {
        println("Crushing dreams, terminating instances.")

        val docker = DockerCompose(inheritIO = true)
        docker.run("terraform", arrayOf("destroy", "/local" ))
        docker.down()
    }
}