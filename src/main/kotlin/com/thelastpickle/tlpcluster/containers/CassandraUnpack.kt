package com.thelastpickle.tlpcluster.containers

import com.github.dockerjava.api.model.AccessMode
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.Docker
import com.thelastpickle.tlpcluster.ResourceFile
import com.thelastpickle.tlpcluster.VolumeMapping
import org.apache.commons.io.FileUtils
import java.io.File
import java.net.URL
import java.nio.file.Path

class CassandraUnpack(val context: Context, val version: String, val dest: Path) {

    val docker = Docker(context)


    fun download() {
        // example http://dl.bintray.com/apache/cassandra/pool/main/c/cassandra/cassandra_2.1.14_all.deb

        FileUtils.copyURLToFile(URL(getURL()), File(dest.toFile(), getFileName()))
        File(dest.toFile(), "conf").mkdir()

    }

    fun extractConf(context: Context) : Result<String> {
        // required that the download have already run
        check(File(dest.toFile(), getFileName()).exists())

        val shellScript = ResourceFile(javaClass.getResource("unpack_cassandra.sh"))

        val volumes = mutableListOf(
                VolumeMapping(dest.toString(), "/working", AccessMode.rw),
                VolumeMapping(shellScript.path, "/unpack_cassandra.sh", AccessMode.ro)
        )

        return docker.runContainer("ubuntu",
                mutableListOf("sh", "/unpack_cassandra.sh", getFileName()),
                volumes,
                "/working/"
        )

    }

    fun getURL() = "http://dl.bintray.com/apache/cassandra/pool/main/c/cassandra/" + getFileName()
    fun getFileName() = "cassandra_${version}_all.deb"

}