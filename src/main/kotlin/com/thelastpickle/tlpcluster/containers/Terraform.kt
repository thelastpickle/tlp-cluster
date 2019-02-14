package com.thelastpickle.tlpcluster.containers

import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.model.AccessMode
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.Volume
import com.github.dockerjava.core.command.AttachContainerResultCallback
import com.thelastpickle.tlpcluster.Context
import java.io.*
import java.lang.StringBuilder
import kotlin.concurrent.thread

class Terraform(val context: Context) {

    init {

    }

    fun execute(command: MutableList<String>) : String {

        val result = StringBuilder()

        val volumeLocal = Volume("/local")

        val cwdPath = System.getProperty("user.dir")

        val dockerContainer = context.docker.createContainerCmd("hashicorp/terraform")
                .withVolumes(volumeLocal)
                .withBinds(Bind(cwdPath, volumeLocal, AccessMode.rw))
                .withWorkingDir("/local")
                .withCmd(command)
                .withStdinOpen(true)
                .exec()

        println("Starting Terraform container")

        context.docker.startContainerCmd(dockerContainer.id).exec()

        var containerState : InspectContainerResponse.ContainerState

        // first, handle stdin.  the PipedOutputStream will accept data and
        // feed it to PipedInputStream, which then goes to docker
        // it looks like this, essentially
        // stdInputPipe -> stdInputPipeToContainer -> terraform container
        val stdInputPipe = PipedOutputStream()
        val stdInputPipeToContainer = PipedInputStream(stdInputPipe)

        // now a means of reading from stdin
        val stdIn = System.`in`.bufferedReader()

        // dealing with standard output from the docker container
        // this works, don't fuck with it, Jon
        val source = PipedOutputStream() // we're going to feed the frames to here
        val stdOutReader = PipedInputStream(source).bufferedReader()

        // We put this on a different thread because I have no idea what input it's going to ask for
        // and the operations are blocking
        val outputThread = thread {
            println("Reading lines")
            do {
                val message = stdOutReader.readLine()
                println(message)
                result.appendln(message)
            } while(true)
        }


        val redirectStdInputThread = thread {
            while(true) {
                val line = stdIn.readLine() + "\n"
                println("Sending $line to container")
                stdInputPipe.write(line.toByteArray())
                
            }
        }

        context.docker.attachContainerCmd(dockerContainer.id)
                .withStdIn(stdInputPipeToContainer)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .exec(object : AttachContainerResultCallback() {
                    override fun onNext(item: Frame?) {
                        if(item != null) {
                            source.write(item.payload)
                        }
                    }

                    override fun onError(throwable: Throwable?) {
                        println(throwable.toString())
                        super.onError(throwable)
                    }
            })

        // stay here till the container stops
        do {
            Thread.sleep(1000)
            containerState = context.docker.inspectContainerCmd(dockerContainer.id).exec().state
        } while (containerState.running == true)

        println("Container exited with exit code ${containerState.exitCode}, ${containerState.error}")

        outputThread.stop()
        redirectStdInputThread.stop()

        // clean up after ourselves
        context.docker.removeContainerCmd(dockerContainer.id)
                .withRemoveVolumes(true)
                .exec()

        return result.toString()

    }

    fun init() : String {
        return execute(mutableListOf("init", "/local"))
    }

    fun up() : String {
        return execute(mutableListOf("apply", "/local"))

    }

    fun cassandraIps(): String {
        return execute(mutableListOf("output", "cassandra_ips") )
    }

    fun cassandraInternalIps(): String {
        return execute(mutableListOf("output", "cassandra_internal_ips") )
    }

    fun stressIps() : String {
        return execute(mutableListOf("output", "stress_ips") )
    }

}