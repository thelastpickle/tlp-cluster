package com.thelastpickle.tlpcluster

import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File

/**
 * Manages a local Cassandra repo and build process
 */
class Cassandra(val gitLocation: File) {

    val repo: Repository

    // FIXME: un-hardcode
    val buildDir = File(System.getProperty("user.home"), "/.tlp-cluster/builds")


    init {
        if(!gitLocation.exists()) {
            println("Cloning cassandra repo")
            val result = Git.cloneRepository()
                    .setURI("https://github.com/apache/cassandra.git")
                    .setDirectory(gitLocation)
                    .call()
            println("Finished cloning")
        }

        val builder = FileRepositoryBuilder()
        repo = builder.setGitDir(gitLocation).findGitDir().build()

    }

    companion object {
        fun build(name: String, location: File): OutputResult {
            // i'm not sure how this works,but i'm leaving it here
            // I think we'll move to this eventually I just don't know the details on how it works
//            val docker = DefaultDockerClient.builder().build()
//            docker.pull("ubuntu")
//
//            val config = ContainerConfig.builder()
//
//
//            val container = docker.createContainer(config.build())
//            val containerId = container.id()
//            docker.copyToContainer()

//            docker.startContainer(container.id())
            val dc = DockerCompose(inheritIO = true)

            return dc
                    .setBuildName(name)
                    .setCassandraDir(location.absolutePath)
                    .run("build-cassandra", arrayOf())

        }
    }

    fun checkoutVersion(version: String) {

        val tag = "cassandra-$version"

        Git.open(gitLocation).checkout().setName(tag)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                .setStartPoint(tag)
                .call()
    }

    fun listBuilds() : List<String> {
        return buildDir.listFiles().filter { it.isDirectory }.map { it.name }
    }



}