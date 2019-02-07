package com.thelastpickle.tlpcluster

import org.apache.commons.io.IOUtils
import java.io.File

//import com.spotify.docker.client.DefaultDockerClient


data class Output(val output: String, val err: String)

typealias OutputResult = Result<Output>

class DockerComposeMissingException : Exception()

class DockerCompose(val inheritIO : Boolean = false) {
    var cassandraDir = ""
    var buildName = ""
    var sshKeyPath = ""

    init {

    }

    /**
     * Runs a process and returns the stdout & stderr
     */
    fun run(service: String, command: Array<String>): OutputResult {
        return execute(service, command, "run")
    }

    fun up(service: String, command: Array<String>) : OutputResult {
        return execute(service, command, "up")
    }

    fun down() : OutputResult {
        return execute("", arrayOf(), "down")
    }

    fun setCassandraDir(path: String) : DockerCompose {
        cassandraDir = path
        return this
    }

    fun setBuildName(name: String) : DockerCompose {
        buildName = name
        return this
    }

    fun setSshKeyPath(path: String) : DockerCompose {
        sshKeyPath = path
        return this
    }

    internal fun execute(service: String, command: Array<String>, dockerArg: String) : OutputResult {
        val composeFile = File("docker-compose.yml")
        if(!composeFile.exists()) {
            return OutputResult.failure(DockerComposeMissingException())
        }

        val commandToExecute = mutableListOf("docker-compose", dockerArg)

        if (service.isNotEmpty()) {
            commandToExecute.add(service)
        }

        if (command.isNotEmpty()) {
            commandToExecute.addAll(command)
        }

        println("Starting docker service $service $commandToExecute")

        var process = ProcessBuilder(*commandToExecute.toTypedArray())

        val env = process.environment()

        env.set("CASSANDRA_DIR", cassandraDir)
        env.set("BUILD_NAME", buildName)
        env.set("SSH_KEY_PATH", sshKeyPath)

//        println("Running with environment:")
//        println(env)

        if(inheritIO)
            process = process.inheritIO()

        val result = process.start()

        val output = IOUtils.toString(result.inputStream)
        val error = IOUtils.toString(result.errorStream)

        val returnCode = result.waitFor()

        return if ( returnCode == 0) Result.success(Output(output, error)) else Result.failure(Exception("Non zero response returned."))
    }
}