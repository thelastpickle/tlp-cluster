package com.thelastpickle.tlpcluster.containers

import com.github.dockerjava.api.model.AccessMode
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.Docker
import com.thelastpickle.tlpcluster.Utils
import com.thelastpickle.tlpcluster.VolumeMapping
import java.io.File


class CassandraBuildJava8(val context: Context) {

    private val docker = Docker(context)
    private val dockerImage = "thelastpickle/cassandra-build"
    private val dockerVersion = "1.0"
    private val dockerImageTag = "thelastpickle/cassandra-build:$dockerVersion"

    val home = File(System.getProperty("user.home"))
    val builds = File(home, ".tlp-cluster/builds/")
    val mavenCache = File(home, ".tlp-cluster/maven-cache/").toString()

    init {
        if(!docker.exists(dockerImage, dockerVersion))
            docker.pullImage(dockerImage, dockerVersion)
    }

    /**
     * name of the build in the end
     */
    fun runBuild(location: String, name: String) : Result<String> {

        return docker
                .addVolume(VolumeMapping(location, "/cassandra", AccessMode.ro))
                .addVolume(VolumeMapping(File(builds, name).toString(), "/builds/", AccessMode.rw))
                .addVolume(VolumeMapping(mavenCache, "/root/.m2/", AccessMode.rw))
                .runContainer(
                dockerImageTag,
                mutableListOf("/bin/sh", "/usr/local/bin/build_cassandra.sh"),
                "/cassandra"
        )
    }
}