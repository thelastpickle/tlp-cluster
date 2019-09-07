package com.thelastpickle.tlpcluster.containers

import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.Docker
import com.github.dockerjava.api.model.AccessMode
import com.thelastpickle.tlpcluster.VolumeMapping


class Terraform(val context: Context) {
    private val docker = Docker(context)
    private val dockerImage = "hashicorp/terraform"
    private val dockerTag = "0.11.14"
    private val dockerImageTag = "$dockerImage:${dockerTag}"

    private var localDirectory = "/local"

    init {
        if(!docker.exists("hashicorp/terraform", dockerTag))
            docker.pullImage(dockerImageTag, "0.11.14")
    }

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
                .runContainer(dockerImageTag, args, localDirectory)

    }

}