package com.thelastpickle.tlpcluster.containers

import com.github.dockerjava.api.model.AccessMode
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.Docker
import com.thelastpickle.tlpcluster.VolumeMapping

class DebUnpack(val context: Context) {

    val docker = Docker(context)

    fun start(downloadDir: String) : Result<String> {
        // mkdir /out && dpkg-deb -xv cassandra_2.1.14_all.deb /out && cp /out/etc/cassandra/* conf/
        val command = ""

        val volumes = mutableListOf(
                VolumeMapping(downloadDir, "/working", AccessMode.rw)
        )

        return docker.runContainer("ubuntu",
                mutableListOf("mkdir /out && dpkg-deb -xv cassandra_2.1.14_all.deb /out && cp /out/etc/cassandra/* conf/"),
                volumes,
                "/working/"
                )
    }


}