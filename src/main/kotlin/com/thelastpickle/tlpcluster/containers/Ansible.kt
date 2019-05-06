package com.thelastpickle.tlpcluster.containers

import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.Docker

class Ansible(context: Context) {
    private val docker = Docker(context)
    private val dockerImageTag = "ansible/ansible:latest"

    init {
        docker.pullImage(dockerImageTag, "latest")
    }

    fun command() : Result<String> {
        return Result.success("")
    }

}