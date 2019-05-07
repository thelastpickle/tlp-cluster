package com.thelastpickle.tlpcluster.containers

import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.Docker
import kotlin.concurrent.thread

/**
 * The behavior of this class might be a little surprising
 * Creating an instance of this class spins off a thread that
 */
class UbuntuSSHTestServer(val context: Context) {

    private val docker = Docker(context)
    private val dockerImageTag = "rastasheep/ubuntu-sshd"

    lateinit var id  : String

    init {
        docker.pullImage(dockerImageTag, "latest")
        val threadId = thread(start = true) {
            docker.runContainer(dockerImageTag, mutableListOf(), "")
        }
    }


    /**
     * Starts the container in a separate thread
     */
    fun stop() : String {
        return ""
    }
}