package com.thelastpickle.tlpcluster

import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.model.*
import com.github.dockerjava.core.command.AttachContainerResultCallback
import com.github.dockerjava.core.command.BuildImageResultCallback
import java.io.Closeable
import java.io.PipedOutputStream
import java.io.PipedInputStream
import kotlin.concurrent.thread


data class VolumeMapping(val source: String, val destination: String, val mode: AccessMode)


class Docker(val context: Context) {
    init {

    }

    fun buildContainer(dockerfileName : String, imageTag: String) : String {
        // The java-docker library we use can build an image from only a Dockerfile.
        // That is, there is no programmatic way to build an image using the API. So, we
        // need to copy the dockerfile in the JAR resources to a location it can read from.
        // To do this we will make a temporary file in the working directory that is
        // removed after the tlp-cluster command completes.
        val dockerfile = Utils.resourceToTempFile("containers/$dockerfileName", context.cwdPath)

        val dockerBuildCallback = object : BuildImageResultCallback() {
            override fun onStart(stream: Closeable?) {
                if(stream != null) {
                    println("Building container image $imageTag, this may take a minute...")
                }
            }

            override fun onComplete() {
                println("Finished building container image $imageTag")
                super.onComplete()
            }

            override fun onNext(item: BuildResponseItem?) {
                if(item != null) {
                    print(item.stream)
                }
                super.onNext(item)
            }
        }

        context.docker.buildImageCmd()
                .withDockerfile(dockerfile)
                .withTags(hashSetOf(imageTag))
                .exec(dockerBuildCallback)

        val imageId = dockerBuildCallback.awaitImageId()

        println("Container image id: ($imageId)")

        return imageId
    }

    fun runContainer(
            imageTag: String,
            command: MutableList<String>,
            volumes: MutableList<VolumeMapping>, // Triple: source, target , mode
            workingDirectory : String) : Result<String> {

        val capturedStdOut = StringBuilder()
        val dockerCommandBuilder = context.docker.createContainerCmd(imageTag)

        dockerCommandBuilder
                .withCmd(command)
                .withStdinOpen(true)

        if (volumes.isNotEmpty()) {
            val volumesList = mutableListOf<Volume>()
            val bindesList = mutableListOf<Bind>()

            volumes.forEach{
                val containerVolume = Volume(it.destination)
                volumesList.add(containerVolume)
                bindesList.add(Bind(it.source, containerVolume, it.mode))
            }

            dockerCommandBuilder
                    .withVolumes(volumesList)
                    .withBinds(bindesList)
        }

        if (workingDirectory.isNotEmpty()) {
            println("Setting working directory inside container to $workingDirectory")
            dockerCommandBuilder
                    .withWorkingDir(workingDirectory)
        }

        val dockerContainer = dockerCommandBuilder.exec()

        println("Starting $imageTag container")

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
            do {
                val message = stdOutReader.readLine()
                println(message)
                capturedStdOut.appendln(message)
            } while(true)
        }

        val redirectStdInputThread = thread {
            while(true) {
                val line = stdIn.readLine() + "\n"
                stdInputPipe.write(line.toByteArray())
            }
        }

        var framesRead = 0
        context.docker.attachContainerCmd(dockerContainer.id)
                .withStdIn(stdInputPipeToContainer)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .exec(object : AttachContainerResultCallback() {
                    override fun onNext(item: Frame?) {
                        // should only include standard out - please fix me
                        if(item != null && item.streamType.name.equals("STDOUT")) {
                            source.write(item.payload)
                            framesRead++
                        }
                    }

                    override fun onError(throwable: Throwable?) {
                        println(throwable.toString())
                        super.onError(throwable)
                    }
                })

        context.docker.startContainerCmd(dockerContainer.id).exec()
        
        // stay here till the container stops
        do {
            Thread.sleep(1000)
            containerState = context.docker.inspectContainerCmd(dockerContainer.id).exec().state
        } while (containerState.running == true)

        println("Container exited with exit code ${containerState.exitCode}, ${containerState.error}, frames read: $framesRead")

        // let the threads finish reading... this is a really bad way of solving the problem, ugly hack for now.
        Thread.sleep(1000)

        // clean up after ourselves
        context.docker.removeContainerCmd(dockerContainer.id)
                .withRemoveVolumes(true)
                .exec()

        val returnCode = containerState.exitCode ?: -1

        return if ( returnCode == 0) Result.success(capturedStdOut.toString()) else Result.failure(Exception("Non zero response returned."))
    }
}