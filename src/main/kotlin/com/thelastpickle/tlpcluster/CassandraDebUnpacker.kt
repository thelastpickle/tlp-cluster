package com.thelastpickle.tlpcluster

import org.apache.commons.io.FileUtils
import java.io.File
import java.net.URL
import java.nio.file.Path

/**
 * Utility class for fetching and extracting deb packages
 * Manipulates the docker image behind the scenes for deb extraction
 *
 */
class CassandraDebUnpacker(val version: String, val dest: Path) {

    fun download(version: String) {
        // example http://dl.bintray.com/apache/cassandra/pool/main/c/cassandra/cassandra_2.1.14_all.deb

        FileUtils.copyURLToFile(URL(getURL(version)), File(dest.toFile(), getFileName(version)))

    }

    fun getURL(version: String) = "http://dl.bintray.com/apache/cassandra/pool/main/c/cassandra/" + getFileName(version)
    fun getFileName(version: String) = "cassandra_${version}_all.deb"
}