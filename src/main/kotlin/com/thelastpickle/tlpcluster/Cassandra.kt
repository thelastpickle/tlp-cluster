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