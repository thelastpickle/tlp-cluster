package com.thelastpickle.tlpcluster.containers

import com.github.dockerjava.api.model.AccessMode
import com.thelastpickle.tlpcluster.*
import com.thelastpickle.tlpcluster.configuration.ServerType

import org.apache.logging.log4j.kotlin.logger


/**
 * This is currently flawed in that it only allows for SSH'ing to Cassandra
 */
class Pssh(val context: Context, val sshKey: String) {
    private val dockerImageTag = "thelastpickle/tlp-cluster_pssh"

    private val provisionCommand = "cd provisioning; chmod +x install.sh; sudo ./install.sh"

    val log = logger()
    init {
        val docker = Docker(context)
        docker.pullImage("ubuntu:bionic", "bionic")
        docker.buildContainer("DockerfileSSH", dockerImageTag)
    }

    fun createGrafanaDashboard() : Result<String> {
        return execute("create_dashboard.sh", "", ServerType.Monitoring)
    }


    fun copyProvisioningResources(nodeType: ServerType) : Result<String> {
        return execute("copy_provisioning_resources.sh", "", nodeType)
    }

    fun provisionNode(nodeType: ServerType) : Result<String> {
        return execute("parallel_ssh.sh", "$provisionCommand ${nodeType.serverType}", nodeType)
    }

    fun startService(nodeType: ServerType, serviceName: String) : Result<String> {
        return serviceCommand(nodeType, serviceName, "start")
    }

    fun stopService(nodeType: ServerType, serviceName: String) : Result<String> {
        return serviceCommand(nodeType, serviceName, "stop")
    }

    private fun serviceCommand(nodeType: ServerType, serviceName: String, command: String) : Result<String> {
        return execute("parallel_ssh.sh",
                "sudo service $serviceName $command && sleep 5 && sudo service $serviceName status",
                nodeType)
    }

    private fun execute(scriptName: String, scriptCommand: String, nodeType: ServerType) : Result<String> {
        val docker = Docker(context)
        val script = javaClass.getResourceAsStream(scriptName)
        val scriptFile = ResourceFile(script)

        val scriptPathInContainer = "/scripts/$scriptName"
        val containerCommands = mutableListOf("/bin/sh", scriptPathInContainer)

        if (scriptCommand.isNotEmpty()) {
            containerCommands.add(scriptCommand)
        }

        val hosts = "PSSH_HOSTNAMES=${context.tfstate.getHosts(nodeType).map { it.public }.joinToString(" ")}"
        log.info("Starting container with $hosts")

        return docker
                .addVolume(VolumeMapping(sshKey, "/root/.ssh/aws-private-key", AccessMode.ro))
                .addVolume(VolumeMapping(context.cwdPath, "/local", AccessMode.rw))
                .addVolume(VolumeMapping(scriptFile.path, scriptPathInContainer, AccessMode.rw))
                .addEnv(hosts)
                .runContainer(dockerImageTag, containerCommands, "")
    }
}