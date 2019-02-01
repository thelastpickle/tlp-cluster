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
        // doesn't create an image if it doesn't exist

        /*
        // Pull image
docker.pull("busybox:latest");

// Create container
final ContainerConfig config = ContainerConfig.builder()
    .image("busybox:latest")
    .build();
final String name = randomName();
final ContainerCreation creation = docker.createContainer(config, name);
final String id = creation.id();

final String tag = "foobar";
final ContainerCreation newContainer = docker.commitContainer(
    id, "mosheeshel/busybox", tag, config, "CommitedByTest-" + tag, "newContainer");

final ImageInfo imageInfo = docker.inspectImage(newContainer.id());
assertThat(imageInfo.author(), is("newContainer"));
assertThat(imageInfo.comment(), is("CommitedByTest-" + "foobar"));
         */


        val image = "gradle:jdk8-alpine"
        println("Pulling docker image: $image")
        docker.pull(image)

        val config = ContainerConfig.builder()
                .image(image)
                .build()

        val name = "tlp-stress-build-env"


        // does the container exist?

        val existingingContainer = docker.inspectImage(image).container()

        println("existing container: $existingingContainer")

        val creation = docker.createContainer(config, name)

        // execute the build
        // run fpm

        val id = creation.id()
        println("Created container $name with id $id")
    }

}