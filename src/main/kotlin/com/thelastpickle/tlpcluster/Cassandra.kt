package com.thelastpickle.tlpcluster

import java.io.File

/**
 * Manages a the Cassandra build process
 */
class Cassandra(val gitLocation: File) {

    // FIXME: un-hardcode
    val buildDir = File(System.getProperty("user.home"), "/.tlp-cluster/builds")


    fun listBuilds() : List<String> {
        return buildDir.listFiles().filter { it.isDirectory }.map { it.name }
    }



}