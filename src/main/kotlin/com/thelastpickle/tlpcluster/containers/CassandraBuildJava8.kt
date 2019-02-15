package com.thelastpickle.tlpcluster.containers

import com.github.dockerjava.api.model.AccessMode
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.Docker
import com.thelastpickle.tlpcluster.Utils
import com.thelastpickle.tlpcluster.VolumeMapping
import java.io.File

class CassandraBuildJava8(val context: Context) {

    private val docker = Docker(context)
    private val dockerImageTag = "thelastpickle/tlp-cluster-java8"

    val home = File(System.getProperty("user.home"))
    val builds = File(home, ".tlp-cluster/builds/")
    val mavenCache = File(home, ".tlp-cluster/maven-cache/").toString()

    fun buildContainer() : String {
        return docker.buildContainer("DockerfileCassandra", dockerImageTag)
    }

    /**
     * name of the build in the end
     */
    fun runBuild(location: String, name: String) : Result<String> {
        val scriptName = "build_cassandra.sh"
        val scriptFile = Utils.resourceToTempFile("containers/$scriptName", context.cwdPath)
        val scriptPathInContainer = "/local/$scriptName"

        val volumes = mutableListOf(
                VolumeMapping(location, "/cassandra", AccessMode.ro),
                VolumeMapping(scriptFile.absolutePath, scriptPathInContainer, AccessMode.rw),
                VolumeMapping(File(builds, name).toString(), "/builds/", AccessMode.rw),
                VolumeMapping(mavenCache, "/root/.m2/", AccessMode.rw)
        )

        println("Volumes: $volumes")

        return docker.runContainer(
                dockerImageTag,
                mutableListOf("/bin/sh", scriptPathInContainer),
                volumes,
                "/cassandra"
        )
    }
}