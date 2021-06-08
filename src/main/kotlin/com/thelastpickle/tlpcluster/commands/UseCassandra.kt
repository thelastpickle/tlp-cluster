package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.github.ajalt.mordant.TermColors
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.core.YamlDelegate
import com.thelastpickle.tlpcluster.configuration.*
import com.thelastpickle.tlpcluster.containers.CassandraUnpack
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.io.FileFilter
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.*

@Parameters(commandDescription = "Use a Cassandra build")
class UseCassandra(val context: Context) : ICommand {
    @Parameter
    var cassandraBuild: String = ""

    val log = logger()

    @Parameter(description = "Configuration settings to change in the cassandra.yaml file specified in the format key:value,...", names = ["--config", "-c"])
    var configSettings = listOf<String>()

    val yaml by YamlDelegate()

    private fun writeCassandraYaml(cassandraHosts: HostList, clusterName: String) {
        val cassandraYamlLocation = "provisioning/cassandra/conf/cassandra.yaml"
        val cassandraYaml = CassandraYaml.create(File(cassandraYamlLocation))
        val seeds = cassandraHosts.take(3)

        cassandraYaml.setProperty("cluster_name", clusterName)
        cassandraYaml.setProperty("endpoint_snitch", "Ec2Snitch")
        cassandraYaml.setSeeds(seeds.map { it.private })

        configSettings.forEach {
            val keyValue = it.split(":")
            if (keyValue.count() > 1) {
                cassandraYaml.setProperty(keyValue[0], keyValue[1])
            }
        }

        log.debug { "Writing Cassandra YAML to $cassandraYamlLocation" }
        cassandraYaml.write(cassandraYamlLocation)
    }

    private fun updateCassandraEnv() {
        val cassandraEnvLocation = "provisioning/cassandra/conf/cassandra-env.sh"
        val env = File(cassandraEnvLocation)

        env.appendText("\nJVM_OPTS=\"\$JVM_OPTS -Dcassandra.consistent.rangemovement=false\"\n")
    }

    private fun writeStressEnvironmentSh(stressContactHost: String) {
        val stressEnvironmentVars = File("provisioning/stress/environment.sh").bufferedWriter()
        stressEnvironmentVars.write("#!/usr/bin/env bash")
        stressEnvironmentVars.newLine()

        stressEnvironmentVars.write("export TLP_STRESS_CASSANDRA_HOST=$stressContactHost")
        stressEnvironmentVars.newLine()
        stressEnvironmentVars.flush()
        stressEnvironmentVars.close()
    }

    private fun writePrometheusYaml(cassandraHosts: HostList, stressHosts: HostList) {
        val prometheusYamlLocation = "provisioning/monitoring/config/prometheus/prometheus.yml"
        val prometheusOutput = FileOutputStream(prometheusYamlLocation, true)

        val labelBaseLocation = "provisioning/monitoring/config/prometheus/"

        val stressLabelOutput = File(labelBaseLocation, "stress.yml").outputStream()

        val mcacTargetsJsonOutput = File(labelBaseLocation, "tg_mcac.json").outputStream()

        Prometheus.writeConfiguration(cassandraHosts.map {
            HostInfo(it.private, it.alias, rack = it.availabilityZone)
        }, stressHosts.map {
            HostInfo(it.private, it.alias, rack = it.availabilityZone)
        },
                "/etc/prometheus/", prometheusOutput, stressLabelOutput, mcacTargetsJsonOutput)
        log.debug { "Writing Prometheus YAML to $prometheusYamlLocation" }
    }

    private fun writeStargateEnvironmentSh(
            cassandraHost: String,
            stargateHost: String,
            clusterName: String,
            cassandraVersion: String)
    {
        val stargateEnvironmentVars = File("provisioning/stargate/environment.sh").bufferedWriter()
        stargateEnvironmentVars.write("#!/usr/bin/env bash")

        stargateEnvironmentVars.newLine()
        stargateEnvironmentVars.write("export CASSANDRA_SEED=$cassandraHost")

        stargateEnvironmentVars.newLine()
        stargateEnvironmentVars.write("export STARGATE_SEED=$stargateHost")

        stargateEnvironmentVars.newLine()
        stargateEnvironmentVars.write("export CLUSTER_NAME=$clusterName")

        stargateEnvironmentVars.newLine()
        stargateEnvironmentVars.write("export CASSANDRA_VERSION=$cassandraVersion")

        stargateEnvironmentVars.flush()
        stargateEnvironmentVars.close()
    }

    override fun execute() {
        check(cassandraBuild.isNotBlank())
        try {
            context.tfstate
            context.tfjson
        } catch (e: FileNotFoundException) {
            println("Error: terraform config file not found.  Please run tlp-cluster up first to establish IP addresses for seed listing.")
            System.exit(1)
        }

        val clusterName = context.tfjson.getClusterName()

        // setup the provisioning directory
        val artifactDest = File("provisioning/cassandra/")

        println("Destination artifacts: $artifactDest")
        artifactDest.mkdirs()

        // delete existing deb packages
        for(deb in artifactDest.listFiles(FileFilter { it.extension.equals("deb") })) {
            deb.delete()
        }

        // if we're been passed a version, use the debs we get from apache
        val versionRegex = """(\d+\.\d+)[\.~]\w+""".toRegex()

        var versionRegexMatch = versionRegex.find(cassandraBuild)

        if (versionRegexMatch != null) {
            val cacheLocation = File(System.getProperty("user.home"), ".tlp-cluster/cache")
            println("Using released version $cassandraBuild")
            val unpacker = CassandraUnpack(context, cassandraBuild, artifactDest.toPath(), Optional.of(cacheLocation.toPath()))
            log.info("Downloading")
            unpacker.download()
            log.info("Extracting conf")
            unpacker.extractConf()
        } else {
            // otherwise it's a custom build
            val buildDir = File(context.cassandraRepo.buildDir, cassandraBuild)

            if (!buildDir.exists()) {
                println("Unable to find build $cassandraBuild in the list of builds. Has it been built using the 'build' command?")
                return
            }

            val conf = File(buildDir, "conf")
            val debs = File(buildDir, "deb")

            for (deb in debs.listFiles().filter { it.isFile }.filter { it.extension == "deb" }) {
                println("Copying $deb")
                FileUtils.copyFileToDirectory(deb, artifactDest)
            }

            val reader = buildDir.listFiles().filter { it.isFile }.first { it.name == "cassandra.version" }.bufferedReader()
            versionRegexMatch = versionRegex.find(reader.readLine())

            val configDest = File(artifactDest, "conf")
            configDest.mkdir()

            for (config in conf.listFiles()) {
                println("Copying configuration $config")
                if (config.isDirectory) {
                    FileUtils.copyDirectory(config, configDest)
                } else {
                    FileUtils.copyFileToDirectory(config, configDest)
                }
            }
        }

        val cassandraHosts = context.tfstate.getHosts(ServerType.Cassandra)
        val stressHosts = context.tfstate.getHosts(ServerType.Stress)
        val stargateHosts = context.tfstate.getHosts(ServerType.Stargate)

        val cassandraContact = cassandraHosts.first().private
        val stargateContact:String
        var stressContact = cassandraContact

        writeCassandraYaml(cassandraHosts, clusterName)
        updateCassandraEnv()
        writePrometheusYaml(cassandraHosts, stressHosts)

        if (stargateHosts.count() > 0) {
            stargateContact = stargateHosts.first().private
            stressContact = stargateContact

            val majorVersion = versionRegexMatch!!.destructured.component1()

            // TODO: Change this to throw an error as stargate is unsupported for versions < 3.11
            if (majorVersion.toDouble() < 3.11) {
                with(TermColors()) {
                    println(yellow("WARNING: The Cassandra $majorVersion release series may be incompatible with Stargate!"))
                }
            }

            writeStargateEnvironmentSh(cassandraContact, stargateContact, clusterName, majorVersion)
        }

        if (stressHosts.count() > 0) {
            writeStressEnvironmentSh(stressContact)
        }

        with(TermColors()) {
            println("Cassandra deb and config copied to provisioning/.  Config files are located in provisioning/cassandra. \n Use ${green("tlp-cluster install")} to push the artifacts to the nodes.")
        }
    }
}
