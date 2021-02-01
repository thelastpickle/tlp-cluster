package com.thelastpickle.tlpcluster.containers

import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.Docker
import com.github.dockerjava.api.model.AccessMode
import com.thelastpickle.tlpcluster.Containers
import com.thelastpickle.tlpcluster.VolumeMapping


class Terraform(val context: Context) {
    private val docker = Docker(context)

    fun init() : Result<String> {
        return execute("init")
    }


    fun up(autoApprove : Boolean = false) : Result<String> {
        val commands = mutableListOf("apply")
        if(autoApprove) {
            commands.add("-auto-approve")
        }
        return execute(*commands.toTypedArray())
    }


    fun down(autoApprove: Boolean) : Result<String> {
        val commands = mutableListOf("destroy")
        if(autoApprove) {
            commands.add("-auto-approve")
        }
        return execute(*commands.toTypedArray())
    }


    private fun execute(vararg command: String) : Result<String> {
        val args = command.toMutableList()
        return docker
                .addVolume(VolumeMapping(context.cwdPath, "/local", AccessMode.rw))
                .addVolume(VolumeMapping(context.terraformCacheDir.absolutePath, "/tcache", AccessMode.rw))
                .addEnv("TF_PLUGIN_CACHE_DIR=/tcache")
                .addEnv("AWS_ACCESS_KEY_ID=${context.userConfig.awsAccessKey}")
                .addEnv("AWS_SECRET_ACCESS_KEY=${context.userConfig.awsSecret}")
                .addEnv("AWS_DEFAULT_REGION=${context.userConfig.region}")
                .runContainer(Containers.TERRAFORM, args, "/local")
    }
}