package com.thelastpickle.tlpcluster.containers

import com.github.dockerjava.api.model.AccessMode
import com.thelastpickle.tlpcluster.*
import com.thelastpickle.tlpcluster.configuration.*

import org.apache.logging.log4j.kotlin.logger


/**
 * This is currently flawed in that it only allows for SSH'ing to Cassandra
 */
class Pssh(val context: Context, val sshKey: String) {
    private val provisionCommand = "cd provisioning; chmod +x install.sh; sudo ./install.sh"

    val log = logger()

    fun copyProvisioningResources(nodeType: ServerType) : Result<String> {
        return execute("copy_provisioning_resources.sh", "", nodeType)
    }

    fun provisionNode(nodeType: ServerType) : Result<String> {
        return execute("parallel_ssh.sh", "$provisionCommand ${nodeType.serverType}", nodeType)
    }

    fun startService(nodeType: ServerType, serviceName: String, nodeFilter: NodeFilter) : Result<String> {
        return serviceCommand(nodeType, serviceName, "start", nodeFilter)
    }

//    fun startStargateService(nodeFilter: NodeFilter) : Result<String> {
//        return stargateCommand(nodeFilter, if (nodeFilter == NodeFilter.FIRST) "CASSANDRA_SEED" else "STARGATE_SEED")
//    }

    fun stopService(nodeType: ServerType, serviceName: String) : Result<String> {
        return serviceCommand(nodeType, serviceName, "stop")
    }

    private fun serviceCommand(nodeType: ServerType, serviceName: String, command: String, nodeFilter: NodeFilter = NodeFilter.ALL) : Result<String> {
        return execute("parallel_ssh.sh",
                "sudo service $serviceName $command && sleep 5 && sudo service $serviceName status",
                nodeType,
                nodeFilter)
    }

//    private fun stargateCommand(nodeFilter: NodeFilter = NodeFilter.ALL, seedNode: String) : Result<String> {
//        return execute("parallel_ssh.sh",
//                "source /etc/profile.d/environment.sh && stargate \$$seedNode",
//                ServerType.Stargate,
//                nodeFilter)
//    }

    private fun execute(scriptName: String, scriptCommand: String, nodeType: ServerType, nodeFilter: NodeFilter = NodeFilter.ALL) : Result<String> {
        val docker = Docker(context)

        val hosts = "PSSH_HOSTNAMES=${
            when (nodeFilter) {
                NodeFilter.FIRST -> context.tfstate.getHosts(nodeType).map { it.public }.first()
                NodeFilter.ALL_BUT_FIRST -> context
                                                .tfstate
                                                .getHosts(nodeType)
                                                .filterIndexed { index, _ -> index > 0 }
                                                .joinToString(" ") { it.public }
                // Assume ALL by default for node filter
                else -> context.tfstate.getHosts(nodeType).joinToString(" ") { it.public }
        }}"

//        var hosts = "PSSH_HOSTNAMES=${context.tfstate.getHosts(nodeType).joinToString(" ") { it.public }}"
//        if (nodeFilter.equals(NodeFilter.FIRST)) {
//            hosts = "PSSH_HOSTNAMES=${context.tfstate.getHosts(nodeType).map { it.public }.first()}"
//        } else if (nodeFilter.equals(NodeFilter.ALL_BUT_FIRST)) {
//            hosts = "PSSH_HOSTNAMES=${context.tfstate.getHosts(nodeType).filterIndexed { index, _ -> index > 0 }.joinToString(" ") { it.public }}"
//        }
        log.info("Starting container with $hosts")

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
                .runContainer(Containers.PSSH, mutableListOf("/usr/local/bin/$scriptName", scriptCommand), "")
    }
}