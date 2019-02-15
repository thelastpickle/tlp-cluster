package com.thelastpickle.tlpcluster

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.netty.NettyDockerCmdExecFactory
import com.thelastpickle.tlpcluster.configuration.User
import java.io.File
import java.nio.file.Files

data class Context(val tlpclusterUserDirectory: File,
                   val cassandraRepo: Cassandra) {
    val cassandraBuildDir = File(tlpclusterUserDirectory, "builds")

    fun createBuildSkeleton(name: String) {

        val buildLocation = File(cassandraBuildDir, name)
        buildLocation.mkdirs()
        File(buildLocation, "conf").mkdirs()
        File(buildLocation, "deb").mkdirs()
    }

    /**
     * Please use this for reading and writing yaml to objects
     *
     * Example usage:
     *
     * val state = mapper.readValue<MyStateObject>(json)
     */
    val yaml = ObjectMapper(YAMLFactory()).registerKotlinModule()

    private val userConfigFile = File(System.getProperty("user.home"), ".tlp-cluster/user.yaml")

    // this will let us write out the yaml
    val userConfig by lazy {
        if(!userConfigFile.exists()) {
            User.createInteractively(this, userConfigFile)
        }

        yaml.readValue<User>(userConfigFile)
    }

    val docker by lazy {

        val dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock")
                .build()

        DockerClientBuilder.getInstance(dockerConfig)
                .withDockerCmdExecFactory(NettyDockerCmdExecFactory())
                .build()
    }

    val cwdPath = System.getProperty("user.dir")

    companion object {
        /**
         * Used only for testing
         */
        fun testContext() : Context {
            var testTempDirectory = Files.createTempDirectory("tlpcluster")
            var testTempDirectoryCassandra = Files.createTempDirectory("tlpcluster")
            return Context(testTempDirectory.toFile(), Cassandra(testTempDirectoryCassandra.toFile()))
        }
    }
}
