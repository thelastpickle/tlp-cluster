package com.thelastpickle.tlpcluster

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.netty.NettyDockerCmdExecFactory
import com.thelastpickle.tlpcluster.configuration.TFState
import com.thelastpickle.tlpcluster.configuration.User
import com.thelastpickle.tlpcluster.core.YamlDelegate
import com.thelastpickle.tlpcluster.terraform.TFJson
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.nio.file.Files

data class Context(val tlpclusterUserDirectory: File) {

    val cassandraBuildDir = File(tlpclusterUserDirectory, "builds")
    var profilesDir = File(tlpclusterUserDirectory, "profiles")
    var profileDir = File(profilesDir, "default")
    val terraformCacheDir = File(tlpclusterUserDirectory, "terraform_cache").also { it.mkdirs() }

    var nettyInitialised = false
    val cassandraRepo = Cassandra()

    init {
        profileDir.mkdirs()
    }

    val log = logger()

    fun createBuildSkeleton(name: String) {

        val buildLocation = File(cassandraBuildDir, name)
        buildLocation.mkdirs()
        File(buildLocation, "conf").mkdirs()
        File(buildLocation, "deb").mkdirs()
    }

    fun shutdown() {
        if (nettyInitialised)
            netty.close()
    }

    /**
     * Please use this for reading and writing yaml to objects
     *
     * Example usage:
     *
     * val state = mapper.readValue<MyStateObject>(json)
     */
    val json = ObjectMapper()
    val yaml : ObjectMapper by YamlDelegate()

    private val userConfigFile = File(profileDir, "settings.yaml")

    // this will let us write out the yaml
    val userConfig by lazy {
        if(!userConfigFile.exists()) {
            log.debug { "$userConfigFile not found, going through interactive setup" }
            profilesDir.mkdirs()
            User.createInteractively(this, userConfigFile)
        }

        yaml.readValue<User>(userConfigFile)
    }

    val docker by lazy {
        nettyInitialised = true

        val socketLocation = "/var/run/docker.sock"
        val socket = "unix://$socketLocation"

        if(!File(socketLocation).exists())
            throw Exception("Could not find the docker socket, is docker running?  Using $socket")

        val dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(socket)
                .build()


        DockerClientBuilder.getInstance(dockerConfig)
                .withDockerCmdExecFactory(netty)
                .build()
    }

    private val netty by lazy  {
        NettyDockerCmdExecFactory()
    }

    val cwdPath = System.getProperty("user.dir")

    val tfstate by lazy { TFState.parse(this, File(cwdPath, "terraform.tfstate")) }
    val tfjson by lazy { TFJson.parse(this, File(cwdPath, "terraform.tf.json")) }
    val home = File(System.getProperty("user.home"))
    

    companion object {
        /**
         * Used only for testing
         */
        fun testContext() : Context {
            val tmpContentParent = File("test/contexts")
            tmpContentParent.mkdirs()

            val testTempDirectory = Files.createTempDirectory(tmpContentParent.toPath(), "tlpcluster")
            // create a default profile
            // generate a fake key
            val user = User("test@thelastpickle.com", "us-west-2", "test", "test", "test", "test", "test")

            val context = Context(testTempDirectory.toFile())
            context.yaml.writeValue(context.userConfigFile, user)
            return context
        }
    }
}
