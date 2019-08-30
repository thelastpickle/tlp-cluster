package com.thelastpickle.tlpcluster.containers

import com.github.dockerjava.api.model.AccessMode
import com.thelastpickle.tlpcluster.*
import com.thelastpickle.tlpcluster.configuration.ServerType

import org.apache.logging.log4j.kotlin.logger


/**
 * This is currently flawed in that it only allows for SSH'ing to Cassandra
 */
class Pssh(val context: Context, val sshKey: String) {
    private val dockerImageTag = "thelastpickle/pssh"
    private val tag = "1.0"
    private val provisionCommand = "cd provisioning; chmod +x install.sh; sudo ./install.sh"
    private val postStartCommand = "cd provisioning; chmod +x post_start.sh; sudo ./post_start.sh"


    val log = logger()
    init {
        val docker = Docker(context)

        if(!docker.exists(dockerImageTag, tag)) {
            log.info("Pulling $dockerImageTag")
            docker.pullImage(dockerImageTag, tag)
        }
        else {
            log.info("Skipping pulling pssh, already local.")
        }
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
                "sudo service $serviceName $command && sleep 5 && sudo service $serviceName status && $postStartCommand ${nodeType.serverType}",
                nodeType)
    }

    private fun execute(scriptName: String, scriptCommand: String, nodeType: ServerType) : Result<String> {
        val docker = Docker(context)

        val hosts = "PSSH_HOSTNAMES=${context.tfstate.getHosts(nodeType).map { it.public }.joinToString(" ")}"
        log.info("Starting container with $hosts")

        val imageAndTag = "$dockerImageTag:$tag"

        return docker
                .addVolume(VolumeMapping(sshKey, "/root/.ssh/aws-private-key", AccessMode.ro))
                .also {
                    log.info("Added $sshKey to container")
                }
                .addVolume(VolumeMapping(context.cwdPath, "/local", AccessMode.rw))
                .also {
                    log.info("Added ${context.cwdPath} to /local")
                }
                .addEnv(hosts)
                .runContainer(imageAndTag, mutableListOf("/usr/local/bin/$scriptName", scriptCommand), "")
    }
}