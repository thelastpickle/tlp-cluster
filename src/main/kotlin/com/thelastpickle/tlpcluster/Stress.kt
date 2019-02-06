package com.thelastpickle.tlpcluster

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.ContainerConfig
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File

class Stress(val location: File) {
    val repo: Repository
    val docker: DockerClient

    val sourceImage = "ubuntu:bionic"

    init {
        if(!location.exists()) {
            Git.cloneRepository()
                    .setURI("https://github.com/thelastpickle/tlp-stress.git")
                    .setDirectory(location)
                    .call()
            println("Closed tlp-stress to ${location.absoluteFile}")


        }
        val builder = FileRepositoryBuilder()
        repo = builder.setGitDir(location).findGitDir().build()


        Git.open(location).checkout().setName("master")
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                .setStartPoint("master")
                .call()

        docker = DefaultDockerClient.fromEnv()
                .build()
    }

    fun maybeCreateImage() {
        // always rebuilds

        println("Pulling docker image: $sourceImage")
        docker.pull(sourceImage)

        val config = ContainerConfig.builder()
                .image(sourceImage)
                .build()

        val name = "tlp-stress-build-env"
        /*
        final AtomicReference<String> imageIdFromMessage = new AtomicReference<>();

final String returnedImageId = docker.build(
    Paths.get(dockerDirectory), "test", new ProgressHandler() {
      @Override
      public void progress(ProgressMessage message) throws DockerException {
        final String imageId = message.buildImageId();
        if (imageId != null) {
          imageIdFromMessage.set(imageId);
        }
      }
    });

         */
        docker.create()

//        val containers = docker.listContainers(DockerClient.ListContainersParam.allContainers())


//        val stressContainer = containers.filter{ it.names()?.contains("/$name") ?: false }
//
//        stressContainer.forEach {
//            println("Removing container ${it.id()}")
//            docker.removeContainer(it.id())
//        }
//
//
//
//        val creation = docker.createContainer(config, name)
//
//        // execute the build
//        // run fpm
//
//        val id = creation.id()
//        println("Created container $name with id $id")
//
//        docker.startContainer(id)
//
//        // do a build
//        println(docker.stats(id))
//
//        val execId = docker.execCreate(id, arrayOf("apt-get", "install", "-y", "gradle")).id()
//        docker.execStart(execId).readFully()

        // commit the container



    }



}