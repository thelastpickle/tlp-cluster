package com.thelastpickle.tlpcluster

import java.io.File

data class Context(val tlpclusterUserDirectory: File,
                   val cassandraRepo: Cassandra) {
    val cassandraBuildDir = File(tlpclusterUserDirectory, "builds")

    fun createBuildSkeleton(name: String) {

        val buildLocation = File(cassandraBuildDir, name)
        buildLocation.mkdirs()
        File(buildLocation, "conf").mkdirs()
        File(buildLocation, "deb").mkdirs()
    }

    val stress : Stress by lazy {
        val stressLocation = File(System.getProperty("user.home"), "/.tlp-cluster/tlp-stress")
        Stress(stressLocation)
    }

}
