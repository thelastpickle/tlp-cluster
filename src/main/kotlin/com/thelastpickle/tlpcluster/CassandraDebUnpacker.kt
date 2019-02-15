package com.thelastpickle.tlpcluster

import com.thelastpickle.tlpcluster.containers.DebUnpack
import org.apache.commons.io.FileUtils
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

/**
 * Utility class for fetching and extracting deb packages
 * Manipulates the docker image behind the scenes for deb extraction
 *
 *  Caches files locally
 *
 */
class CassandraDebUnpacker(val version: String, val dest: Path) {

    fun download() {
        // example http://dl.bintray.com/apache/cassandra/pool/main/c/cassandra/cassandra_2.1.14_all.deb

        FileUtils.copyURLToFile(URL(getURL()), File(dest.toFile(), getFileName()))
        File(dest.toFile(), "conf").mkdir()

    }

    fun extractConf(context: Context) {
        // required that the download have already run
        check(File(dest.toFile(), getFileName()).exists())


        val debUnpack = DebUnpack(context)
        debUnpack.start(dest.toString())
    }

    fun getURL() = "http://dl.bintray.com/apache/cassandra/pool/main/c/cassandra/" + getFileName()
    fun getFileName() = "cassandra_${version}_all.deb"
}