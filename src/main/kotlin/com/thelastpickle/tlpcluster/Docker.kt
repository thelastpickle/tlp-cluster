package com.thelastpickle.tlpcluster

import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.model.*
import com.github.dockerjava.core.command.AttachContainerResultCallback
import com.github.dockerjava.core.command.BuildImageResultCallback
import com.github.dockerjava.core.command.PullImageResultCallback
import org.apache.logging.log4j.kotlin.logger
import java.io.Closeable
import java.io.PipedOutputStream
import java.io.PipedInputStream
import kotlin.concurrent.thread


data class VolumeMapping(val source: String, val destination: String, val mode: AccessMode) {
    companion object {
        val log = logger()
    }
    init {
        log.info { "Creating volume mapping $source to $destination, mode: $mode" }
    }
}

class Docker(val context: Context) {

    companion object {
        val log = logger()
    }

    private val volumes = mutableListOf<VolumeMapping>()
    private val env = mutableListOf<String>()

    fun addVolume(vol: VolumeMapping) : Docker {
        log.info { "adding volume: $vol" }
        volumes.add(vol)
        return this
    }

    fun addEnv(envList: String) : Docker {
        env.add(envList)
        return this
    }


    /**
     * Tag is required here, otherwise we pull every tag
     * and that isn't fun
     */
    fun pullImage(name: String, tag: String) {
        log.debug { "Creating pull object" }

        var pullCommand = context.docker.pullImageCmd(name)

        if(tag.isNotBlank())
            pullCommand = pullCommand.withTag(tag)

        pullCommand.exec(
                object : PullImageResultCallback() {

                    override fun awaitStarted(): PullImageResultCallback {
                        log.info { "Pulling image $name" }
                        return super.awaitStarted()
                    }

                    override fun onNext(item: PullResponseItem?) {
                        if(item != null) {

                            item.progressDetail?.let {
                                if(it.current != null && it.total != null)
                                    println("Pulling: ${it.current} / ${it.total}")
                            }
                        }
                        return super.onNext(item)
                    }

                }

        ).awaitCompletion()

        log.info{"Finished pulling $name"}
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
            workingDirectory : String) : Result<String> {

        val capturedStdOut = StringBuilder()
        val dockerCommandBuilder = context.docker.createContainerCmd(imageTag)

        // this only runs on linux or mac, deeply sorry
        val idQuery = ProcessBuilder("id", System.getProperty("user.name")).start().inputStream.bufferedReader().readLine()
        val matches = "uid=(\\d*)".toRegex().find(idQuery)

        var userId = 0
        if(matches != null) {
            userId = matches.groupValues[1].toInt()
        }

        log.debug { "user id: $userId" }
        check(userId > 0)

        env.add("HOST_USER_ID=$userId")

        log.info{ "docker environment variables: $env"}

        dockerCommandBuilder
                .withCmd(command)
                .withEnv(*env.toTypedArray())
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

        // handle stdin.  the PipedOutputStream will accept data and
        // feed it to PipedInputStream, which then goes to docker
        // it looks like this, essentially
        // stdInputPipe -> PipedInputStream(stdInputPipe) -> terraform container
        val stdInputPipe = PipedOutputStream()

        // now a means of reading from stdin
        val stdIn = System.`in`.bufferedReader()

        val redirectStdInputThread = thread(isDaemon = true) {
            while(true) {
                val line = stdIn.readLine() + "\n"
                stdInputPipe.write(line.toByteArray())
            }
        }

        var framesRead = 0
        context.docker.attachContainerCmd(dockerContainer.id)
                .withStdIn(PipedInputStream(stdInputPipe))
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .exec(object : AttachContainerResultCallback() {
                    override fun onNext(item: Frame?) {
                        // should only include standard out - please fix me
                        if(item == null) return

                        framesRead++
                        val payloadStr = String(item.payload)

                        if(item.streamType.name.equals("STDOUT")) {
                            // no need to use println - payloadStr already has carriage returns
                            print(payloadStr)
                            capturedStdOut.append(payloadStr)
                        } else if(item.streamType.name.equals("STDERR")) {
                            log.error(payloadStr)
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


        val returnMessage: String by lazy {
            var errorMessage = ""

            if (!containerState.error.isNullOrEmpty()) {
                errorMessage = ", ${containerState.error}"
            }

            "Container exited with exit code ${containerState.exitCode}$errorMessage, frames read: $framesRead"
        }

        println(returnMessage)

        // close the stdin pipe otherwise we will never exit back to the commandline
        stdInputPipe.close()

        // clean up after ourselves
        context.docker.removeContainerCmd(dockerContainer.id)
                .withRemoveVolumes(true)
                .exec()

        val returnCode = containerState.exitCode ?: -1

        return if ( returnCode == 0) Result.success(capturedStdOut.toString()) else Result.failure(Exception("Non zero response returned."))
    }
}