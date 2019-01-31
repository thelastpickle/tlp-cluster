package com.thelastpickle.tlpcluster

import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File

class Stress(val location: File) {
    val repo: Repository

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
    }
}