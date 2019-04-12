package com.thelastpickle.tlpcluster

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.netty.NettyDockerCmdExecFactory
import com.thelastpickle.tlpcluster.configuration.TFState
import com.thelastpickle.tlpcluster.configuration.User
import org.apache.logging.log4j.kotlin.logger
import com.thelastpickle.tlpcluster.configuration.Yaml
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

data class Context(val tlpclusterUserDirectory: File) {

    val cassandraBuildDir = File(tlpclusterUserDirectory, "builds")
    var profilesDir = File(tlpclusterUserDirectory, "profiles")
    var profileDir = File(profilesDir, "default")

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

        val dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock")
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
    val home = File(System.getProperty("user.home"))
    

    companion object {
        /**
         * Used only for testing
         */
        fun testContext() : Context {
            File("build/testContext")
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
