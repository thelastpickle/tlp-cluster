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

    fun setCassandraDir(path: String) : DockerCompose {
        cassandraDir = path
        return this
    }

    fun setBuildName(name: String) : DockerCompose {
        buildName = name
        return this
    }

    internal fun execute(service: String, command: Array<String>, dockerArg: String) : OutputResult {
        val composeFile = File("docker-compose.yml")
        if(!composeFile.exists()) {
            return OutputResult.failure(DockerComposeMissingException())
        }

        val commandToExecute = mutableListOf("docker-compose", "run", service)

        commandToExecute.addAll(command)

        println("Starting docker service $service $commandToExecute")

        var process = ProcessBuilder(*commandToExecute.toTypedArray())

        var env = process.environment()

        env.set("CASSANDRA_DIR", cassandraDir)
        env.set("BUILD_NAME", buildName)

//        println("Running with environment:")
//        println(env)

        if(inheritIO)
            process = process.inheritIO()

        var result = process.start()

        val output = IOUtils.toString(result.inputStream)
        val error = IOUtils.toString(result.errorStream)

        result.waitFor()

        return Result.success(Output(output, error))
    }
}