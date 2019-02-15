package com.thelastpickle.tlpcluster.containers

import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.Docker
import com.github.dockerjava.api.model.AccessMode
import com.thelastpickle.tlpcluster.VolumeMapping


class Terraform(val context: Context) {
    private val docker = Docker(context)
    private val dockerImageTag = "hashicorp/terraform"
    private var volumeMapping = mutableListOf(VolumeMapping(context.cwdPath, "/local", AccessMode.rw))
    private var localDirectory = "/local"

    init {

    }

    fun init() : Result<String> {
        return execute("init")
    }

    fun up() : Result<String> {
        return execute("apply")
    }

    fun down() : Result<String> {
        return execute("destroy")
    }

    fun cassandraIps(): Result<String> {
        return execute("output", "cassandra_ips")
    }

    fun cassandraInternalIps(): Result<String> {
        return execute("output", "cassandra_internal_ips")
    }

    fun stressIps() : Result<String> {
        return execute("output", "stress_ips")
    }

    private fun execute(vararg command: String) : Result<String> {
        val args = command.toMutableList()
        return docker.runContainer(dockerImageTag, args, volumeMapping, localDirectory)

    }

}