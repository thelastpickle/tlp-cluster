package com.thelastpickle.tlpcluster.containers

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.model.AccessMode
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.Volume
import com.github.dockerjava.core.command.AttachContainerResultCallback
import com.github.dockerjava.core.command.EventsResultCallback
import com.github.dockerjava.core.command.LogContainerResultCallback
import com.thelastpickle.tlpcluster.Context
import java.io.*
import kotlin.concurrent.thread

class Terraform(val context: Context) {

    init {

    }

    fun execute(command: MutableList<String>) {
        val volumeLocal = Volume("/local")

        val cwdPath = System.getProperty("user.dir")

        val dockerContainer = context.docker.createContainerCmd("hashicorp/terraform")
                .withVolumes(volumeLocal)
                .withBinds(Bind(cwdPath, volumeLocal, AccessMode.rw))
                .withWorkingDir("/local")
                .withCmd(command)
                .withAttachStdout(true)
                .withAttachStdin(true)
                .exec()



        println("working dir is: $cwdPath")

        println("Starting Terraform container")

        context.docker.startContainerCmd(dockerContainer.id).exec()

        var containerState : InspectContainerResponse.ContainerState


        /**
         * https://github.com/docker-java/docker-java/issues/941
         *
         * try (PipedOutputStream out = new PipedOutputStream();
                PipedInputStream in = new PipedInputStream(out);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {

            AttachContainerCmd attachContainerCmd = docker.attachContainerCmd(createContainerResponse.getId()).withStdIn(in)
                .withStdOut(true).withStdErr(true).withFollowStream(true);

            attachContainerCmd.exec(new AttachContainerResultCallback());

            String line = "Hello World!";
            while (!"q".equals(line)) {
            writer.write(line + "\n");

            writer.flush();

            line = reader.readLine();
            }
            } catch (Exception ex) {
            ex.printStackTrace();
        }

         */

        // first, handle stdin.  the PipedOutputStream will accept data and feed it to PipedInputStream, which then goes to docker
        // it looks like this, essentially
        // stdInputPipe -> stdInputPipeToContainer -> terraform container
        val stdInputPipe = PipedOutputStream()
        val stdInputPipeToContainer = PipedInputStream(stdInputPipe)

        // now I need a means of writing to the stdInputPipe, and it should be buffered
        val writer = stdInputPipe.bufferedWriter()

        // now a means of reading from stdin
        val stdIn = System.`in`.bufferedReader()

        // dealing with standard output from the docker container
        val source = PipedOutputStream() // we're going to feed the frames to here
        val stdOutReader = PipedInputStream(source).bufferedReader()

        // We put this on a different thread because I have no idea what input it's going to ask for
        // and the operations are blocking
        val outputThread = thread {
            println("Reading line")
            do {
                println(stdOutReader.readLine())
            } while(true)
        }


        val redirectStdInputThread = thread {
            while(true) {
                val line = stdIn.readLine()
                println("Sending $line to container")
                writer.appendln(line)
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
            Thread.sleep(500)
            containerState = context.docker.inspectContainerCmd(dockerContainer.id).exec().state
        } while (containerState.running == true)

        println("Container exited with exit code ${containerState.exitCode}, ${containerState.error}")

        // clean up after ourselves
        context.docker.removeContainerCmd(dockerContainer.id)
                .withRemoveVolumes(true)
                .exec()
    }

    fun init() {
        return execute(mutableListOf("init", "/local"))
    }

    fun up() {
        return execute(mutableListOf("apply", "/local"))

    }

}